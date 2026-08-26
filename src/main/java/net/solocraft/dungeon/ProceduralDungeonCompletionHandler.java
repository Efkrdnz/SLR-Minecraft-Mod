package net.solocraft.dungeon;

import net.solocraft.dungeon.runtime.DungeonInstanceSavedData;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.dungeon.runtime.DungeonReturnPortalSpawner;
import net.solocraft.init.SololevelingModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Owns the optional alternate-exit choice for dungeons entered through a
 * Procedural Gate.
 *
 * <p>Normal gate dungeons reveal their entrance-side return portal only after
 * the boss is defeated. Procedural Red Gates alone keep that portal visible
 * and locked from the beginning. If Cartenon replaces either exit after
 * completion, this helper keeps that choice durable across chunk unloads.</p>
 */
public final class ProceduralDungeonCompletionHandler {
	public static final String PROCEDURAL_DUNGEON_TAG = "slr_procedural_dungeon";
	public static final String EXIT_HANDLED_TAG = "slr_procedural_exit_handled";

	private ProceduralDungeonCompletionHandler() {
	}

	public static boolean isProceduralCompletion(Entity boss, @Nullable Entity creditedSource) {
		return hasProceduralTag(boss) || hasProceduralTag(creditedSource);
	}

	public static void markProceduralMob(Entity entity) {
		if (entity != null)
			entity.getPersistentData().putBoolean(PROCEDURAL_DUNGEON_TAG, true);
	}

	public static void markExitHandled(Entity boss) {
		if (boss != null)
			boss.getPersistentData().putBoolean(EXIT_HANDLED_TAG, true);
	}

	public static boolean isExitHandled(Entity boss) {
		return boss != null && boss.getPersistentData().getBoolean(EXIT_HANDLED_TAG);
	}

	public static void recordUnscopedEntrant(ServerPlayer player, String dungeonTag,
			ResourceKey<Level> dungeonDimension) {
		if (player == null || dungeonDimension == null)
			return;
		ProceduralGateRunSavedData.get(player.server).recordEntrant(
				dungeonTag, player.getUUID(), dungeonDimension,
				player.serverLevel().getGameTime());
	}

	public static void removeUnscopedEntrant(ServerPlayer player,
			String dungeonTag) {
		if (player != null)
			ProceduralGateRunSavedData.get(player.server).removeEntrant(
					dungeonTag, player.getUUID());
	}

	public static void preserveLegacyUnscopedRoster(ServerPlayer player, String dungeonTag,
			ResourceKey<Level> dungeonDimension) {
		if (player == null || dungeonDimension == null)
			return;
		ProceduralGateRunSavedData.get(player.server).recordLegacyRun(
				dungeonTag, dungeonDimension, player.serverLevel().getGameTime());
	}

	public static void recordUnscopedReturnAnchor(ServerLevel level,
			String dungeonTag, BlockPos returnAnchor) {
		if (level == null || returnAnchor == null)
			return;
		ProceduralGateRunSavedData.get(level.getServer()).recordReturnAnchor(
				dungeonTag, level.dimension(), returnAnchor,
				level.getGameTime());
	}

	@Nullable
	public static BlockPos unscopedReturnAnchor(ServerLevel level,
			String dungeonTag) {
		if (level == null)
			return null;
		return ProceduralGateRunSavedData.get(level.getServer()).run(dungeonTag)
				.filter(run -> run.dimension().equals(level.dimension()))
				.map(ProceduralGateRunSavedData.RunView::returnAnchor)
				.orElse(null);
	}

	/**
	 * Resolves a clear portal footprint near the authored return marker. The
	 * participant fallback exists only for active saves made before anchors were
	 * persisted; it deliberately never uses the defeated boss's position.
	 */
	public static BlockPos resolveUnscopedReturnPosition(ServerLevel level,
			String dungeonTag) {
		if (level == null)
			return null;
		BlockPos desired = unscopedReturnAnchor(level, dungeonTag);
		if (desired == null) {
			desired = level.players().stream()
					.filter(player -> dungeonTag != null && dungeonTag.equals(
							player.getPersistentData().getString(
									DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG)))
					.map(Entity::blockPosition)
					.findFirst()
					.orElseGet(() -> level.getSharedSpawnPos().above());
		}
		return safeReturnPortalPosition(level, desired);
	}

	/**
	 * Finds room for the portal without overlapping a player or a wall. Completion
	 * is a rare bounded operation, so the authored anchor chunk is loaded
	 * synchronously instead of making the player walk back toward the entrance to
	 * trigger the retry path.
	 */
	public static BlockPos safeReturnPortalPosition(ServerLevel level,
			BlockPos desired) {
		if (level == null || desired == null)
			return null;
		if (!loadReturnPortalChunk(level, desired))
			return desired.immutable();
		int[] verticalOffsets = {0, 1, -1, 2, -2};
		for (int radius = 0; radius <= 6; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius)
						continue;
					for (int dy : verticalOffsets) {
						BlockPos candidate = desired.offset(dx, dy, dz);
						if (isSafeReturnPortalPosition(level, candidate))
							return candidate.immutable();
					}
				}
			}
		}
		return desired.immutable();
	}

	/** Loads exactly the entrance-side chunk needed for a completion portal. */
	public static boolean loadReturnPortalChunk(ServerLevel level,
			BlockPos position) {
		if (level == null || position == null)
			return false;
		level.getChunkAt(position);
		return level.hasChunkAt(position);
	}

	private static boolean isSafeReturnPortalPosition(ServerLevel level,
			BlockPos position) {
		if (!level.hasChunkAt(position))
			return false;
		BlockPos floorPos = position.below();
		BlockState floor = level.getBlockState(floorPos);
		if (!floor.isFaceSturdy(level, floorPos, Direction.UP)
				|| !floor.getFluidState().isEmpty())
			return false;
		AABB footprint = new AABB(position.getX() - 0.10D, position.getY(),
				position.getZ() - 0.10D, position.getX() + 1.10D,
				position.getY() + 3.60D, position.getZ() + 1.10D);
		if (!level.noCollision(null, footprint))
			return false;
		return level.getEntitiesOfClass(LivingEntity.class,
				footprint.inflate(0.35D),
				entity -> entity.isAlive()
						&& entity.getType() != SololevelingModEntities.PORTAL_12.get())
				.isEmpty();
	}

	/**
	 * Resolves the complete built-in-gate entrant set. An empty result means its
	 * completeness cannot be proven, so Cartenon must not replace the normal exit.
	 */
	public static Optional<List<ServerPlayer>> activeUnscopedParticipants(ServerLevel level,
			String dungeonTag) {
		if (level == null || dungeonTag == null || dungeonTag.isBlank())
			return Optional.empty();
		ProceduralGateRunSavedData.RunView run = ProceduralGateRunSavedData
				.get(level.getServer()).run(dungeonTag).orElse(null);
		if (run == null || !run.authoritativeRoster() || run.participants().isEmpty()
				|| !run.dimension().equals(level.dimension()))
			return Optional.empty();
		List<ServerPlayer> participants = new ArrayList<>();
		for (UUID participantId : run.participants()) {
			ServerPlayer participant = level.getServer().getPlayerList().getPlayer(participantId);
			if (participant == null || participant.serverLevel() != level
					|| !dungeonTag.equals(participant.getPersistentData().getString(
							DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG))
					|| !participant.getPersistentData().getBoolean(PROCEDURAL_DUNGEON_TAG))
				return Optional.empty();
			participants.add(participant);
		}
		return Optional.of(List.copyOf(participants));
	}

	public static void migrateLegacyUnscopedRuns(MinecraftServer server) {
		if (server == null)
			return;
		ProceduralGateRunSavedData runs = ProceduralGateRunSavedData.get(server);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			String dungeonTag = player.getPersistentData().getString(
					DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG);
			if (player.serverLevel().dimension().equals(Level.OVERWORLD)
					|| !player.getPersistentData().getBoolean(PROCEDURAL_DUNGEON_TAG)
					|| !player.getPersistentData().getString(
							DungeonMobLevelAdapter.INSTANCE_TAG).isBlank()
					|| dungeonTag.isBlank())
				continue;
			if (runs.run(dungeonTag).isPresent())
				continue;
			runs.recordLegacyRun(dungeonTag, player.serverLevel().dimension(),
					player.serverLevel().getGameTime());
		}
	}

	public static boolean isUnscopedRunDecided(MinecraftServer server, String dungeonTag) {
		if (server == null)
			return false;
		return ProceduralGateRunSavedData.get(server).run(dungeonTag)
				.map(run -> run.decision()
						!= ProceduralGateRunSavedData.ExitDecision.UNDECIDED)
				.orElse(false);
	}

	public static void chooseUnscopedReturnPortal(ServerLevel level, String dungeonTag,
			BlockPos exit) {
		if (level != null && exit != null)
			ProceduralGateRunSavedData.get(level.getServer()).chooseReturnPortal(
					dungeonTag, level.dimension(), exit, level.getGameTime());
	}

	public static void chooseCartenonExit(ServerLevel level, String dungeonTag) {
		if (level != null)
			ProceduralGateRunSavedData.get(level.getServer()).chooseCartenon(
					dungeonTag, level.dimension(), level.getGameTime());
	}

	/** Retries durable built-in entrance-side exits while an entrant is present. */
	public static void ensureUnscopedReturnPortals(MinecraftServer server) {
		if (server == null)
			return;
		for (ProceduralGateRunSavedData.ReturnRequest request :
				ProceduralGateRunSavedData.get(server).returnRequests()) {
			ServerLevel level = server.getLevel(request.dimension());
			if (level == null)
				continue;
			boolean entrantPresent = level.players().stream().anyMatch(player ->
					request.dungeonTag().equals(player.getPersistentData().getString(
							DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG))
							&& player.getPersistentData().getBoolean(PROCEDURAL_DUNGEON_TAG));
			if (!entrantPresent
					|| !loadReturnPortalChunk(level, request.exit())
					|| reconcileReturnPortal(
					level, null, request.dungeonTag(), request.exit()))
				continue;
			spawnUnscopedReturnPortal(level, request.exit(), request.dungeonTag());
		}
	}

	/**
	 * Spawns the unscoped return portal used by the built-in procedural generator.
	 * It must not receive a fake runtime-instance tag.
	 */
	public static boolean spawnUnscopedReturnPortal(ServerLevel level, BlockPos position,
			String dungeonTag) {
		if (level == null || position == null
				|| !loadReturnPortalChunk(level, position))
			return false;
		discardMatchingReturnPortals(level, null, dungeonTag);
		discardReturnPortalsAt(level, position);
		return DungeonReturnPortalSpawner.spawnUnscoped(
				level, position, dungeonTag) != null;
	}

	public static boolean spawnScopedReturnPortal(ServerLevel level, BlockPos position,
			@Nullable Direction facing, DungeonInstanceSavedData.Instance instance,
			String dungeonTag) {
		if (level == null || position == null || instance == null
				|| !loadReturnPortalChunk(level, position))
			return false;
		discardMatchingReturnPortals(level, instance.id(), dungeonTag);
		discardReturnPortalsAt(level, position);
		return DungeonReturnPortalSpawner.spawn(level, position, facing, instance.id(),
				dungeonTag) != null;
	}

	/**
	 * Repairs an already-loaded exit pile and reports whether one usable portal is
	 * left at the persisted exit. This also migrates untagged portals created by
	 * older post-join tagging code.
	 */
	public static boolean reconcileReturnPortal(ServerLevel level,
			@Nullable UUID instanceId, String dungeonTag, BlockPos expectedPosition) {
		if (level == null || expectedPosition == null)
			return false;
		String instanceText = instanceId == null ? "" : instanceId.toString();
		String cleanDungeonTag = dungeonTag == null ? "" : dungeonTag.trim();
		Entity kept = null;
		List<Entity> duplicates = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity.getType() != SololevelingModEntities.PORTAL_12.get())
				continue;
			boolean atExpectedPosition = expectedPosition.equals(entity.blockPosition());
			boolean matchingInstance = !instanceText.isEmpty() && instanceText.equals(
					entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG));
			boolean matchingDungeon = !cleanDungeonTag.isEmpty() && cleanDungeonTag.equals(
					entity.getPersistentData().getString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG));
			if (!atExpectedPosition && !matchingInstance && !matchingDungeon)
				continue;
			if (atExpectedPosition && kept == null) {
				kept = entity;
				continue;
			}
			duplicates.add(entity);
		}
		duplicates.forEach(Entity::discard);
		if (kept == null)
			return false;
		if (!instanceText.isEmpty())
			kept.getPersistentData().putString(DungeonMobLevelAdapter.INSTANCE_TAG,
					instanceText);
		if (!cleanDungeonTag.isEmpty())
			kept.getPersistentData().putString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG,
					cleanDungeonTag);
		return true;
	}

	/**
	 * Removes only loaded return portals belonging to this dungeon. Matching both
	 * identifiers migrates old gate-created runtime portals whose legacy tag is the
	 * overworld gate UUID while their scoped tag is the generated instance UUID.
	 */
	public static void discardMatchingReturnPortals(ServerLevel level,
			@Nullable UUID instanceId, String dungeonTag) {
		if (level == null)
			return;
		String instanceText = instanceId == null ? "" : instanceId.toString();
		String cleanDungeonTag = dungeonTag == null ? "" : dungeonTag.trim();
		List<Entity> matches = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity.getType() != SololevelingModEntities.PORTAL_12.get())
				continue;
			boolean matchingInstance = !instanceText.isEmpty() && instanceText.equals(
					entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG));
			boolean matchingDungeon = !cleanDungeonTag.isEmpty() && cleanDungeonTag.equals(
					entity.getPersistentData().getString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG));
			if (matchingInstance || matchingDungeon)
				matches.add(entity);
		}
		matches.forEach(Entity::discard);
	}

	private static void discardReturnPortalsAt(ServerLevel level, BlockPos position) {
		if (level == null || position == null)
			return;
		List<Entity> matches = new ArrayList<>();
		for (Entity entity : level.getAllEntities())
			if (entity.getType() == SololevelingModEntities.PORTAL_12.get()
					&& position.equals(entity.blockPosition()))
				matches.add(entity);
		matches.forEach(Entity::discard);
	}

	public static boolean hasMatchingReturnPortal(ServerLevel level,
			@Nullable UUID instanceId, String dungeonTag) {
		if (level == null)
			return false;
		String instanceText = instanceId == null ? "" : instanceId.toString();
		String cleanDungeonTag = dungeonTag == null ? "" : dungeonTag.trim();
		for (Entity entity : level.getAllEntities()) {
			if (entity.getType() != SololevelingModEntities.PORTAL_12.get())
				continue;
			if (!instanceText.isEmpty() && instanceText.equals(
					entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG)))
				return true;
			if (!cleanDungeonTag.isEmpty() && cleanDungeonTag.equals(
					entity.getPersistentData().getString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG)))
				return true;
		}
		return false;
	}

	/** Rejects a second return-portal entity occupying the same block. */
	public static boolean isDuplicateReturnPortal(ServerLevel level, Entity candidate) {
		if (level == null || candidate == null
				|| candidate.getType() != SololevelingModEntities.PORTAL_12.get())
			return false;
		BlockPos position = candidate.blockPosition();
		for (Entity entity : level.getAllEntities())
			if (entity != candidate
					&& entity.getType() == SololevelingModEntities.PORTAL_12.get()
					&& position.equals(entity.blockPosition()))
				return true;
		return false;
	}

	/**
	 * Rejects stale scoped portals when their chunk is loaded later. This makes
	 * deferred and Cartenon-only exits durable even when the original entry chunk
	 * was unloaded during boss completion.
	 */
	public static boolean isObsoleteScopedReturnPortal(ServerLevel level, Entity entity) {
		if (level == null || entity == null
				|| entity.getType() != SololevelingModEntities.PORTAL_12.get())
			return false;
		String dungeonTag = entity.getPersistentData().getString(
				DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG);
		ProceduralGateRunSavedData.RunView unscopedRun = ProceduralGateRunSavedData
				.get(level.getServer()).run(dungeonTag).orElse(null);
		if (unscopedRun != null) {
			if (unscopedRun.decision()
					== ProceduralGateRunSavedData.ExitDecision.UNDECIDED)
				return true;
			if (unscopedRun.decision() == ProceduralGateRunSavedData.ExitDecision.CARTENON)
				return true;
			if (unscopedRun.decision() == ProceduralGateRunSavedData.ExitDecision.RETURN_PORTAL
					&& (unscopedRun.exit() == null
							|| !unscopedRun.exit().equals(entity.blockPosition())))
				return true;
		}
		String instanceText = entity.getPersistentData().getString(
				DungeonMobLevelAdapter.INSTANCE_TAG);
		if (instanceText.isBlank())
			return false;
		try {
			DungeonInstanceSavedData.Instance instance = DungeonInstanceSavedData.get(level)
					.getInstance(UUID.fromString(instanceText)).orElse(null);
			if (instance == null)
				return true;
			if (instance.returnPortalSuppressed())
				return true;
			if (!instance.returnPortalDeferred())
				return false;
			if (!instance.completed())
				return true;
			return instance.exit().map(exit -> !exit.equals(entity.blockPosition())).orElse(true);
		} catch (IllegalArgumentException ignored) {
			return true;
		}
	}

	private static boolean hasProceduralTag(@Nullable Entity entity) {
		return entity != null && entity.getPersistentData().getBoolean(PROCEDURAL_DUNGEON_TAG);
	}
}
