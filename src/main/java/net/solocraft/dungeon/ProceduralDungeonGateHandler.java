package net.solocraft.dungeon;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.dungeon.runtime.SnowRedGateArenaManager;
import net.solocraft.entity.Portal1Entity;
import net.solocraft.guild.GuildGateHelper;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.MagicReadingHelper;
import net.solocraft.util.PlayerEntryGenerationGuard;
import net.solocraft.util.UrgentQuestManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
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
	private static final float RED_GATE_CHANCE = 0.02F;

	private ProceduralDungeonGateHandler() {
	}

	public static boolean isProceduralGate(Entity gate) {
		return gate != null && gate.getPersistentData().getBoolean(PROCEDURAL_GATE);
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
		boolean turnsRed = !gate.getPersistentData().getBoolean(GENERATED) && shouldTurnRed(world);
		if (turnsRed)
			turnRed(world, gate);
		List<ServerPlayer> entrants = turnsRed ? nearbyPartyMembers(world, gate, player) : List.of(player);
		if (isProceduralRedGate(gate)) {
			SnowRedGateArenaManager.enterProcedural(world, gate, player, entrants);
			return;
		}
		Map<UUID, Long> entryGenerations = new LinkedHashMap<>();
		for (ServerPlayer entrant : entrants) {
			prepareEntrant(world, x, y, z, gate, entrant);
			entryGenerations.put(entrant.getUUID(),
					PlayerEntryGenerationGuard.begin(entrant));
		}
		teleportEntrants(gate, entrants, entryGenerations);
	}

	private static boolean shouldTurnRed(LevelAccessor world) {
		return !SololevelingModVariables.MapVariables.get(world).RedGate && RandomSource.create().nextFloat() < RED_GATE_CHANCE;
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
		UrgentQuestManager.markDungeonId(entrant, isProceduralRedGate(gate) ? "red_gate" : "procedural");
		entrant.setNoGravity(true);
	}

	private static void teleportEntrants(Entity gate, List<ServerPlayer> entrants,
			Map<UUID, Long> entryGenerations) {
		ResourceKey<Level> destination = destinationFor(gate);
		ServerPlayer firstPlayer = entrants.stream().filter(entrant -> entrant != null && !entrant.level().isClientSide()).findFirst().orElse(null);
		if (firstPlayer == null)
			return;
		ServerLevel nextLevel = firstPlayer.server.getLevel(destination);
		if (nextLevel == null) {
			failEntrants(entrants, "The dungeon destination dimension is unavailable.");
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
		if (!gate.getPersistentData().getBoolean(GENERATED)) {
			for (ServerPlayer entrant : entrants)
				entrant.teleportTo(nextLevel, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, entrant.getYRot(), entrant.getXRot());
		}
		SololevelingMod.queueServerWork(5, () -> {
			String gateId = gate.getStringUUID();
			rollbackInvalidEntrants(gateId, entrants, entryGenerations);
			List<ServerPlayer> currentEntrants = currentPreparedEntrants(
					gateId, entrants, entryGenerations);
			if (currentEntrants.isEmpty())
				return;
			ServerPlayer currentFirst = currentEntrants.get(0);
			if (gate.isRemoved()) {
				failEntrants(currentEntrants,
						"The dungeon gate disappeared before entry finished.");
				return;
			}
			if (isCompletedGate(gate.level(), gate, currentFirst.server)) {
				failEntrants(currentEntrants, "This gate was cleared before entry finished.");
				return;
			}
			if (!gate.getPersistentData().getBoolean(GENERATED)) {
				ProceduralDungeonResult result = ProceduralDungeonGenerator.generate(nextLevel,
						targetPos, settingsFor(gate), currentFirst, false);
				gate.getPersistentData().putBoolean(GENERATED, true);
				gate.getPersistentData().putDouble(START_X, result.startPos.getX() + 0.5);
				gate.getPersistentData().putDouble(START_Y, result.startPos.getY());
				gate.getPersistentData().putDouble(START_Z, result.startPos.getZ() + 0.5);
			}
			double startX = gate.getPersistentData().getDouble(START_X);
			double startY = gate.getPersistentData().getDouble(START_Y);
			double startZ = gate.getPersistentData().getDouble(START_Z);
			// A normal procedural gate opens its return portal at the defeated
			// boss, never in the entry room. Remove portals left by older builds so
			// re-entering an unfinished run cannot bypass that lifecycle.
			ProceduralDungeonCompletionHandler.discardMatchingReturnPortals(
					nextLevel, null, gate.getStringUUID());
			for (ServerPlayer entrant : currentEntrants) {
				entrant.teleportTo(nextLevel, startX, startY, startZ, entrant.getYRot(), entrant.getXRot());
				entrant.setNoGravity(false);
				entrant.fallDistance = 0.0F;
			}
		});
	}

	private static List<ServerPlayer> currentPreparedEntrants(String gateId,
			List<ServerPlayer> entrants, Map<UUID, Long> entryGenerations) {
		return entrants.stream().filter(entrant -> {
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

	private static void failEntrants(List<ServerPlayer> entrants, String message) {
		for (ServerPlayer entrant : entrants) {
			entrant.setNoGravity(false);
			entrant.fallDistance = 0.0F;
			entrant.getPersistentData().putBoolean(PROCEDURAL_DUNGEON, false);
			entrant.getPersistentData().putBoolean(PROCEDURAL_RED, false);
			entrant.getPersistentData().remove("slr_dungeon_instance");
			entrant.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.dungeoning = false;
				capability.syncPlayerVariables(entrant);
			});
			entrant.sendSystemMessage(Component.literal(message));
		}
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
		final Vec3 center = new Vec3(x, y, z);
		List<Entity> found = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(250), e -> true).stream().sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center))).toList();
		for (Entity entity : found) {
			if (entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))
					&& (entity instanceof TamableAnimal tame && sourceentity instanceof LivingEntity owner && tame.isOwnedBy(owner))) {
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
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
			case E, D -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_d"));
			case C -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_c"));
			case B -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_b"));
			case A -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_a"));
			case S -> ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_s"));
		};
	}
}
