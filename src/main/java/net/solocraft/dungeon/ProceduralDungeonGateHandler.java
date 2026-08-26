package net.solocraft.dungeon;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.dungeon.runtime.SnowRedGateArenaManager;
import net.solocraft.entity.Portal1Entity;
import net.solocraft.guild.GuildGateHelper;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.MagicReadingHelper;
import net.solocraft.util.PlayerEntryGenerationGuard;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.UrgentQuestManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProceduralDungeonGateHandler {
	private static final String PROCEDURAL_GATE = "slr_procedural_gate";
	private static final String PROCEDURAL_RED = "slr_procedural_red_gate";
	private static final String RANK = "slr_procedural_rank";
	private static final String THEME = "slr_procedural_theme";
	private static final String COMPLEXITY = "slr_procedural_complexity";
	private static final String GENERATED = "slr_procedural_generated";
	private static final String START_X = "slr_procedural_start_x";
	private static final String START_Y = "slr_procedural_start_y";
	private static final String START_Z = "slr_procedural_start_z";
	private static final String PROCEDURAL_DUNGEON = "slr_procedural_dungeon";
	private static final String PREPARING = "slr_procedural_preparing";
	private static final String PREPARING_SINCE = "slr_procedural_preparing_since";
	private static final long STALE_PREPARATION_TICKS = 200L;
	private static final float RED_GATE_CHANCE = 0.02F;

	private ProceduralDungeonGateHandler() {
	}

	public static boolean isProceduralGate(Entity gate) {
		// Portal1 is the built-in unnamed/procedural gate. Treat legacy saves that
		// predate the marker as procedural too; their old fallback interaction only
		// disabled player gravity and never loaded a dungeon.
		return gate instanceof Portal1Entity
				|| gate != null
						&& gate.getPersistentData().getBoolean(PROCEDURAL_GATE);
	}

	public static boolean isProceduralRedGate(Entity gate) {
		return gate != null && gate.getPersistentData().getBoolean(PROCEDURAL_RED);
	}

	public static boolean isGenerated(Entity gate) {
		return isProceduralGate(gate) && gate.getPersistentData().getBoolean(GENERATED);
	}

	public static void enter(LevelAccessor world, double x, double y, double z, Entity gate, Entity sourceentity) {
		if (gate == null || sourceentity == null)
			return;
		if (MagicReadingHelper.isHoldingMagicReader(sourceentity)) {
			showMagicReading(gate, sourceentity);
			return;
		}
		if (!(sourceentity instanceof ServerPlayer player))
			return;
		ensureProceduralMetadata(gate);
		recoverInterruptedBinding(player);
		if (isCompletedGate(world, gate, player.server)) {
			player.displayClientMessage(Component.literal("This gate has already been cleared."), true);
			return;
		}
		// Transformed Red Gates are one-way. Their used flag remains the silent
		// entry lock even after the active arena is torn down.
		if (isProceduralRedGate(gate) && isLocked(gate))
			return;
		if (isDungeonBound(player)) {
			player.displayClientMessage(Component.literal("You are already bound to a dungeon."), true);
			return;
		}
		if (GuildGateHelper.prepareGateEntry(world, gate, sourceentity))
			return;
		if (isProceduralRedGate(gate)) {
			SnowRedGateArenaManager.enterProcedural(world, gate, player, nearbyPartyMembers(world, gate, player));
			return;
		}
		boolean turnsRed = !gate.getPersistentData().getBoolean(GENERATED)
				&& shouldTurnRed(world, gate);
		if (turnsRed)
			turnRed(world, gate);
		List<ServerPlayer> entrants = turnsRed ? nearbyPartyMembers(world, gate, player) : List.of(player);
		if (isProceduralRedGate(gate)) {
			SnowRedGateArenaManager.enterProcedural(world, gate, player, entrants);
			return;
		}
		if (isPreparationActive(gate)) {
			player.displayClientMessage(Component.literal(
					"This gate is already stabilizing."), true);
			return;
		}
		markPreparing(gate);
		// First entry generates the whole dungeon synchronously, which can take
		// seconds. Without acknowledging the click the gate looks unresponsive and
		// players spam right-click, so say something immediately.
		for (ServerPlayer entrant : entrants)
			entrant.displayClientMessage(Component.literal(
					gate.getPersistentData().getBoolean(GENERATED)
							? "Opening the gate..."
							: "Stabilizing the gate... this may take a moment."), true);
		Map<UUID, Long> entryGenerations = new LinkedHashMap<>();
		try {
			for (ServerPlayer entrant : entrants) {
				prepareEntrant(world, x, y, z, gate, entrant);
				entryGenerations.put(entrant.getUUID(),
						PlayerEntryGenerationGuard.begin(entrant));
			}
			teleportEntrants(gate, entrants, entryGenerations);
		} catch (RuntimeException exception) {
			clearPreparing(gate);
			failEntrants(gate.getStringUUID(), entrants,
					"The dungeon entry failed before it could stabilize.",
					gate.level() instanceof ServerLevel sourceLevel
							? sourceLevel : player.serverLevel());
			SololevelingMod.LOGGER.error(
					"Failed to begin procedural gate entry {}",
					gate.getStringUUID(), exception);
		}
	}

	private static boolean shouldTurnRed(LevelAccessor world, Entity gate) {
		// Dedicated natural Red Gates already unlock at B rank. Apply the same
		// floor to the surprise transformation so an introductory E/D/C gate
		// cannot become a Monarch gauntlet immediately after Goblin Sewers.
		return rankFor(gate).numericRank
					>= ProceduralDungeonRank.B.numericRank
				&& !SololevelingModVariables.MapVariables.get(world).RedGate
				&& RandomSource.create().nextFloat() < RED_GATE_CHANCE;
	}

	private static void ensureProceduralMetadata(Entity gate) {
		if (gate == null || gate.level().isClientSide())
			return;
		var data = gate.getPersistentData();
		data.putBoolean(PROCEDURAL_GATE, true);
		if (data.getString(RANK).isBlank())
			data.putString(RANK, ProceduralDungeonRank.D.name());
		if (data.getString(THEME).isBlank()) {
			DungeonTheme[] themes = DungeonTheme.values();
			int themeIndex = Math.floorMod(gate.getUUID().hashCode(),
					themes.length);
			data.putString(THEME, themes[themeIndex].name());
		}
		if (data.getInt(COMPLEXITY) <= 0)
			data.putInt(COMPLEXITY, 3);
		if (!data.contains("tpx") || !data.contains("tpy")
				|| !data.contains("tpz")) {
			long seed = gate.getUUID().getMostSignificantBits()
					^ gate.getUUID().getLeastSignificantBits();
			RandomSource random = RandomSource.create(seed);
			data.putDouble("tpx", random.nextInt(599999) - 299999);
			data.putDouble("tpy", 60 + random.nextInt(61));
			data.putDouble("tpz", random.nextInt(599999) - 299999);
		}
	}

	private static boolean isLocked(Entity gate) {
		return gate instanceof Portal1Entity portal && portal.getEntityData().get(Portal1Entity.DATA_usedbefore);
	}

	private static boolean isCompletedGate(LevelAccessor world, Entity gate, MinecraftServer server) {
		String gateTag = gate.getStringUUID();
		return SololevelingModVariables.MapVariables.get(world).GatesCleared
				.contains(gateTag + ",")
				|| ProceduralDungeonCompletionHandler.isUnscopedRunDecided(server, gateTag);
	}

	private static boolean isDungeonBound(ServerPlayer player) {
		if (!player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank())
			return true;
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> capability.dungeoning)
				.orElse(false);
	}

	private static void recoverInterruptedBinding(ServerPlayer player) {
		if (player == null || player.level().dimension() != Level.OVERWORLD
				|| !player.getPersistentData().getString(
						DungeonMobLevelAdapter.INSTANCE_TAG).isBlank()
				|| !player.getPersistentData().getBoolean(
						PROCEDURAL_DUNGEON))
			return;
		String oldGateId = player.getPersistentData().getString("dungeon_tag");
		Entity oldGate = findLoadedGate(player.server, oldGateId);
		if (oldGate != null && isPreparationActive(oldGate))
			return;
		if (!oldGateId.isBlank())
			ProceduralDungeonCompletionHandler.removeUnscopedEntrant(
					player, oldGateId);
		clearEntrantBinding(player);
	}

	private static Entity findLoadedGate(MinecraftServer server,
			String gateId) {
		if (server == null || gateId == null || gateId.isBlank())
			return null;
		try {
			UUID uuid = UUID.fromString(gateId);
			for (ServerLevel level : server.getAllLevels()) {
				Entity gate = level.getEntity(uuid);
				if (gate != null)
					return gate;
			}
		} catch (IllegalArgumentException ignored) {
			// A malformed legacy tag is stale by definition.
		}
		return null;
	}

	private static boolean isPreparationActive(Entity gate) {
		if (gate == null || !gate.getPersistentData().getBoolean(PREPARING))
			return false;
		long since = gate.getPersistentData().getLong(PREPARING_SINCE);
		long age = Math.max(0L, gate.level().getGameTime() - since);
		if (age <= STALE_PREPARATION_TICKS)
			return true;
		clearPreparing(gate);
		return false;
	}

	private static void markPreparing(Entity gate) {
		gate.getPersistentData().putBoolean(PREPARING, true);
		gate.getPersistentData().putLong(PREPARING_SINCE,
				gate.level().getGameTime());
	}

	private static void clearPreparing(Entity gate) {
		if (gate == null)
			return;
		gate.getPersistentData().remove(PREPARING);
		gate.getPersistentData().remove(PREPARING_SINCE);
	}

	private static void turnRed(LevelAccessor world, Entity gate) {
		SnowRedGateArenaManager.assignTerritoryIfMissing(gate);
		gate.getPersistentData().putBoolean(PROCEDURAL_RED, true);
		gate.getPersistentData().putBoolean("slr_is_red_gate", true);
		if (gate instanceof Portal1Entity portal) {
			portal.getEntityData().set(Portal1Entity.DATA_usedbefore, true);
			portal.setTexture("21");
		}
		SololevelingModVariables.MapVariables.get(world).RedGate = true;
		SololevelingModVariables.MapVariables.get(world).syncData(world);
	}

	private static List<ServerPlayer> nearbyPartyMembers(LevelAccessor world, Entity gate, ServerPlayer player) {
		String party = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).party;
		if (party.equals(""))
			return List.of(player);
		List<ServerPlayer> entrants = new ArrayList<>();
		entrants.add(player);
		for (Entity candidate : new ArrayList<>(world.players())) {
			if (!(candidate instanceof ServerPlayer partyMember))
				continue;
			if (partyMember.getUUID().equals(player.getUUID()))
				continue;
			String candidateParty = partyMember.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).party;
			if (party.equals(candidateParty) && partyMember.distanceTo(gate) <= 10)
				entrants.add(partyMember);
		}
		return entrants;
	}

	private static void prepareEntrant(LevelAccessor world, double x, double y, double z, Entity gate, ServerPlayer entrant) {
		dismissOwnedShadows(world, x, y, z, entrant);
		saveReturnPosition(entrant);
		entrant.getPersistentData().putString("dungeon_tag", gate.getStringUUID());
		entrant.getPersistentData().putBoolean(PROCEDURAL_DUNGEON, true);
		entrant.getPersistentData().putBoolean(PROCEDURAL_RED, isProceduralRedGate(gate));
		entrant.getPersistentData().remove("slr_dungeon_instance");
	}

	private static void teleportEntrants(Entity gate, List<ServerPlayer> entrants,
			Map<UUID, Long> entryGenerations) {
		ResourceKey<Level> destination = destinationFor(gate);
		ServerPlayer firstPlayer = entrants.stream().filter(entrant -> entrant != null && !entrant.level().isClientSide()).findFirst().orElse(null);
		if (firstPlayer == null) {
			clearPreparing(gate);
			return;
		}
		ServerLevel sourceLevel = gate.level() instanceof ServerLevel level
				? level : firstPlayer.serverLevel();
		ServerLevel nextLevel = firstPlayer.server.getLevel(destination);
		if (nextLevel == null) {
			failEntrants(gate.getStringUUID(), entrants,
					"The dungeon destination dimension is unavailable.",
					sourceLevel);
			clearPreparing(gate);
			return;
		}

		BlockPos targetPos = storedTarget(gate);
		// A generated pre-feature gate may have offline entrants that cannot be
		// reconstructed. Keep that roster non-authoritative so Cartenon never
		// replaces their fallback return portal.
		if (gate.getPersistentData().getBoolean(GENERATED))
			ProceduralDungeonCompletionHandler.preserveLegacyUnscopedRoster(
					firstPlayer, gate.getStringUUID(), nextLevel.dimension());
		for (ServerPlayer entrant : entrants)
			ProceduralDungeonCompletionHandler.recordUnscopedEntrant(
					entrant, gate.getStringUUID(), nextLevel.dimension());
		// Keep players safely beside the overworld gate while the destination is
		// generated. Moving them to the empty build anchor with no gravity made a
		// gate removed by a guild team strand them hovering over the void.
		SololevelingMod.queueServerWork(5, () -> {
			String gateId = gate.getStringUUID();
			List<ServerPlayer> currentEntrants = List.of();
			try {
				rollbackInvalidEntrants(gateId, entrants, entryGenerations);
				currentEntrants = currentPreparedEntrants(
						gateId, entrants, entryGenerations);
				if (currentEntrants.isEmpty())
					return;
				ServerPlayer currentFirst = currentEntrants.get(0);
				if (gate.isRemoved()) {
					failEntrants(gateId, currentEntrants,
							"The dungeon gate disappeared before entry finished.",
							sourceLevel);
					return;
				}
				if (isCompletedGate(gate.level(), gate,
						currentFirst.server)) {
					failEntrants(gateId, currentEntrants,
							"This gate was cleared before entry finished.",
							sourceLevel);
					return;
				}

				// The player no longer preloads this remote build anchor by being
				// teleported into empty space, so load it explicitly before any
				// block placement.
				nextLevel.getChunk(targetPos.getX() >> 4,
						targetPos.getZ() >> 4);
				if (!gate.getPersistentData().getBoolean(GENERATED)
						|| !hasStoredStart(gate)) {
					ProceduralDungeonResult result =
							ProceduralDungeonGenerator.generate(nextLevel,
									targetPos, settingsFor(gate), currentFirst, false);
					gate.getPersistentData().putBoolean(GENERATED, true);
					gate.getPersistentData().putDouble(START_X,
							result.startPos.getX() + 0.5D);
					gate.getPersistentData().putDouble(START_Y,
							result.startPos.getY());
					gate.getPersistentData().putDouble(START_Z,
							result.startPos.getZ() + 0.5D);
					ProceduralDungeonCompletionHandler
							.recordUnscopedReturnAnchor(nextLevel, gateId,
									result.returnPortalPos);
				}
				double startX = gate.getPersistentData().getDouble(START_X);
				double startY = gate.getPersistentData().getDouble(START_Y);
				double startZ = gate.getPersistentData().getDouble(START_Z);
				BlockPos startPos = BlockPos.containing(startX, startY,
						startZ);
				nextLevel.getChunk(startPos.getX() >> 4,
						startPos.getZ() >> 4);
				// Reconstruct the entrance anchor for unfinished saves created
				// before it was persisted separately. This does not reveal the
				// portal early.
				ProceduralDungeonCompletionHandler.recordUnscopedReturnAnchor(
						nextLevel, gateId,
						BlockPos.containing(startX - 2.0D, startY, startZ));
				// Normal gates reveal this authored return point only after
				// completion. Remove an old loaded copy so re-entry cannot bypass
				// that lifecycle.
				ProceduralDungeonCompletionHandler.discardMatchingReturnPortals(
						nextLevel, null, gateId);
				for (ServerPlayer entrant : currentEntrants) {
					entrant.setNoGravity(false);
					entrant.setDeltaMovement(Vec3.ZERO);
					entrant.fallDistance = 0.0F;
					entrant.teleportTo(nextLevel, startX, startY, startZ,
							entrant.getYRot(), entrant.getXRot());
					entrant.setNoGravity(false);
					entrant.setDeltaMovement(Vec3.ZERO);
					entrant.fallDistance = 0.0F;
				}
				for (ServerPlayer entrant : currentEntrants)
					UrgentQuestManager.markDungeonId(entrant, "procedural");
			} catch (RuntimeException exception) {
				if (currentEntrants.isEmpty())
					currentEntrants = currentPreparedEntrants(
							gateId, entrants, entryGenerations);
				if (!currentEntrants.isEmpty())
					failEntrants(gateId, currentEntrants,
							"The dungeon could not finish generating. "
									+ "You were returned safely.",
							sourceLevel);
				SololevelingMod.LOGGER.error(
						"Procedural gate {} ({}) failed at target {}",
						gateId, rankFor(gate), targetPos, exception);
			} finally {
				clearPreparing(gate);
			}
		});
	}

	private static List<ServerPlayer> currentPreparedEntrants(String gateId,
			List<ServerPlayer> entrants, Map<UUID, Long> entryGenerations) {
		return entrants.stream().filter(entrant -> {
			if (entrant == null || entrant.hasDisconnected())
				return false;
			Long generation = entryGenerations.get(entrant.getUUID());
			return generation != null
					&& PlayerEntryGenerationGuard.isCurrent(entrant, generation)
					&& hasPreparedBinding(entrant, gateId);
		}).toList();
	}

	private static void rollbackInvalidEntrants(String gateId,
			List<ServerPlayer> entrants, Map<UUID, Long> entryGenerations) {
		for (ServerPlayer entrant : entrants) {
			Long generation = entryGenerations.get(entrant.getUUID());
			boolean current = generation != null
					&& PlayerEntryGenerationGuard.isCurrent(entrant, generation)
					&& hasPreparedBinding(entrant, gateId);
			// A newer entry to this same gate owns the shared UUID roster entry.
			// Do not let an older callback remove that replacement session.
			if (!current && !hasPreparedBinding(entrant, gateId))
				ProceduralDungeonCompletionHandler.removeUnscopedEntrant(
						entrant, gateId);
		}
	}

	private static boolean hasPreparedBinding(ServerPlayer entrant,
			String gateId) {
		return entrant != null
				&& gateId.equals(entrant.getPersistentData().getString(
						"dungeon_tag"))
				&& entrant.getPersistentData().getBoolean(PROCEDURAL_DUNGEON)
				&& entrant.getCapability(
						SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY,
						null)
						.map(capability -> capability.dungeoning)
						.orElse(false);
	}

	private static boolean hasStoredStart(Entity gate) {
		return gate != null
				&& gate.getPersistentData().contains(START_X)
				&& gate.getPersistentData().contains(START_Y)
				&& gate.getPersistentData().contains(START_Z)
				&& Double.isFinite(
						gate.getPersistentData().getDouble(START_X))
				&& Double.isFinite(
						gate.getPersistentData().getDouble(START_Y))
				&& Double.isFinite(
						gate.getPersistentData().getDouble(START_Z));
	}

	private static void failEntrants(String gateId,
			List<ServerPlayer> entrants, String message,
			ServerLevel sourceLevel) {
		for (ServerPlayer entrant : entrants) {
			if (entrant == null)
				continue;
			ProceduralDungeonCompletionHandler.removeUnscopedEntrant(
					entrant, gateId);
			entrant.setNoGravity(false);
			entrant.setDeltaMovement(Vec3.ZERO);
			entrant.fallDistance = 0.0F;
			SololevelingModVariables.PlayerVariables variables = entrant
					.getCapability(
							SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY,
							null)
					.orElse(null);
			if (sourceLevel != null && entrant.serverLevel() != sourceLevel
					&& variables != null
					&& Double.isFinite(variables.DunX)
					&& Double.isFinite(variables.DunY)
					&& Double.isFinite(variables.DunZ)) {
				try {
					entrant.teleportTo(sourceLevel, variables.DunX,
							variables.DunY, variables.DunZ,
							entrant.getYRot(), entrant.getXRot());
				} catch (RuntimeException exception) {
					SololevelingMod.LOGGER.error(
							"Could not return {} after procedural gate {} failed",
							entrant.getGameProfile().getName(), gateId,
							exception);
				}
			}
			clearEntrantBinding(entrant);
			if (!entrant.hasDisconnected())
				entrant.sendSystemMessage(Component.literal(message));
		}
	}

	private static void clearEntrantBinding(ServerPlayer entrant) {
		entrant.setNoGravity(false);
		entrant.setDeltaMovement(Vec3.ZERO);
		entrant.fallDistance = 0.0F;
		entrant.getPersistentData().putBoolean(PROCEDURAL_DUNGEON, false);
		entrant.getPersistentData().putBoolean(PROCEDURAL_RED, false);
		entrant.getPersistentData().remove("dungeon_tag");
		entrant.getPersistentData().remove(
				DungeonMobLevelAdapter.INSTANCE_TAG);
		entrant.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.dungeoning = false;
					capability.BossKilled = false;
					capability.syncPlayerVariables(entrant);
				});
	}

	private static void showMagicReading(Entity gate, Entity sourceentity) {
		if (!(sourceentity instanceof Player player) || player.level().isClientSide())
			return;
		if (isProceduralRedGate(gate)) {
			MagicReadingHelper.showUnreadableReading(player);
			return;
		}
		MagicReadingHelper.showRankReading(player, rankFor(gate));
	}

	private static void dismissOwnedShadows(LevelAccessor world, double x, double y, double z, Entity sourceentity) {
		if (sourceentity instanceof ServerPlayer owner)
			ShadowMonarchManager.dismissLoadedOwnedShadows(owner,
					owner.level().dimension());
	}

	private static void saveReturnPosition(Entity entity) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = entity.getX();
			capability.DunY = entity.getY();
			capability.DunZ = entity.getZ();
			capability.dungeoning = true;
			capability.BossKilled = false;
			capability.syncPlayerVariables(entity);
		});
	}

	private static BlockPos storedTarget(Entity gate) {
		return BlockPos.containing(gate.getPersistentData().getDouble("tpx"), gate.getPersistentData().getDouble("tpy"), gate.getPersistentData().getDouble("tpz"));
	}

	private static ProceduralDungeonSettings settingsFor(Entity gate) {
		return new ProceduralDungeonSettings(rankFor(gate), DungeonTheme.fromString(gate.getPersistentData().getString(THEME)), gate.getPersistentData().getInt(COMPLEXITY));
	}

	public static ProceduralDungeonRank rankFor(Entity gate) {
		return ProceduralDungeonRank.fromString(gate.getPersistentData().getString(RANK));
	}

	private static ResourceKey<Level> destinationFor(Entity gate) {
		return switch (rankFor(gate)) {
			case E, D -> ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_d"));
			case C -> ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_c"));
			case B -> ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_b"));
			case A -> ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_a"));
			case S -> ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("sololeveling:dungeon_dimension_s"));
		};
	}
}
