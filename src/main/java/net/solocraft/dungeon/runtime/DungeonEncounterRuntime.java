package net.solocraft.dungeon.runtime;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.data.DungeonDataTypes.IntRange;
import net.solocraft.dungeon.data.MobPoolResolver;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Activates persisted encounter regions and completes instances from tracked boss deaths. */
@Mod.EventBusSubscriber
public final class DungeonEncounterRuntime {
	private static final int ACTIVATION_INTERVAL = 10;
	private static final int FAILED_ACTIVATION_RETRY_TICKS = 200;
	private static final int MISSING_MOB_RECOVERY_TICKS = 600;
	private static final Map<String, Long> RETRY_AFTER = new HashMap<>();
	private static final Map<String, Long> MISSING_SINCE = new HashMap<>();
	private static int tickCounter;

	private DungeonEncounterRuntime() {
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || ++tickCounter % ACTIVATION_INTERVAL != 0)
			return;
		MinecraftServer server = event.getServer();
		DungeonInstanceSavedData registry = DungeonInstanceSavedData.get(server);
		for (DungeonInstanceSavedData.Instance instance : registry.listInstances()) {
			if (instance.completed())
				continue;
			ServerLevel level = server.getLevel(instance.dimension());
			if (level == null)
				continue;
			for (DungeonInstanceSavedData.EncounterState encounter : instance.encounters()) {
				if (encounter.completed())
					continue;
				if (encounter.activated()) {
					reconcileActivatedEncounter(level, instance, encounter);
					continue;
				}
				if (encounter.sequenced()) {
					if (!sequencedWaveReady(level, instance, encounter))
						continue;
				} else if (encounter.triggerBounds().isPresent()) {
					if (!playerEntered(level, instance, encounter))
						continue;
				} else if (!markerChunksLoaded(level, encounter)) {
					continue;
				}
				String retryKey = retryKey(instance, encounter);
				if (RETRY_AFTER.getOrDefault(retryKey, 0L) > level.getGameTime())
					continue;
				if (activate(level, instance, encounter))
					RETRY_AFTER.remove(retryKey);
				else
					RETRY_AFTER.put(retryKey, level.getGameTime() + FAILED_ACTIVATION_RETRY_TICKS);
			}
		}
	}

	/**
	 * Latches a schema-v3 wave once its trigger/prerequisite is satisfied, then
	 * honors its persisted deadline. Legacy encounters never enter this path.
	 */
	private static boolean sequencedWaveReady(ServerLevel level,
			DungeonInstanceSavedData.Instance instance, DungeonInstanceSavedData.EncounterState encounter) {
		if (!sequencePrerequisitesCompleted(instance, encounter))
			return false;
		if (encounter.scheduledGameTime() < 0L) {
			if (encounter.sequenceOrder() == 0) {
				boolean triggerReady = encounter.triggerBounds().isPresent()
						? playerEntered(level, instance, encounter) : markerChunksLoaded(level, encounter);
				if (!triggerReady)
					return false;
			}
			encounter.scheduleActivation(activationTime(level.getGameTime(), encounter.delayTicks()));
		}
		return encounter.scheduledGameTime() >= 0L
				&& level.getGameTime() >= encounter.scheduledGameTime()
				&& markerChunksLoaded(level, encounter);
	}

	private static boolean sequencePrerequisitesCompleted(DungeonInstanceSavedData.Instance instance,
			DungeonInstanceSavedData.EncounterState encounter) {
		if (!encounter.sequenced() || encounter.sequenceOrder() == 0)
			return true;
		boolean foundImmediatePredecessor = false;
		for (DungeonInstanceSavedData.EncounterState candidate : instance.encounters()) {
			if (!candidate.sequenced() || !candidate.sequenceKey().equals(encounter.sequenceKey()))
				continue;
			if (candidate.sequenceOrder() == encounter.sequenceOrder() - 1)
				foundImmediatePredecessor = true;
			if (candidate.sequenceOrder() < encounter.sequenceOrder() && !candidate.completed())
				return false;
		}
		return foundImmediatePredecessor;
	}

	private static long activationTime(long gameTime, int delayTicks) {
		long safeTime = Math.max(0L, gameTime);
		return safeTime > Long.MAX_VALUE - delayTicks ? Long.MAX_VALUE : safeTime + delayTicks;
	}

	private static boolean playerEntered(ServerLevel level, DungeonInstanceSavedData.Instance instance,
			DungeonInstanceSavedData.EncounterState encounter) {
		Optional<DungeonInstanceSavedData.Bounds> trigger = encounter.triggerBounds();
		if (trigger.isEmpty())
			return false;
		for (ServerPlayer player : level.players()) {
			if (!instance.participants().isEmpty() && !instance.participants().contains(player.getUUID()))
				continue;
			if (trigger.get().contains(BlockPos.containing(player.position())))
				return true;
		}
		return false;
	}

	private static boolean markerChunksLoaded(ServerLevel level,
			DungeonInstanceSavedData.EncounterState encounter) {
		return !encounter.markers().isEmpty()
				&& encounter.markers().stream().allMatch(marker -> level.hasChunkAt(marker.position()));
	}

	private static boolean activate(ServerLevel level, DungeonInstanceSavedData.Instance instance,
			DungeonInstanceSavedData.EncounterState encounter) {
		if (!encounter.activate())
			return false;
		List<Mob> spawnedMobs = new ArrayList<>();
		String compatibilityTag = level.players().stream()
				.filter(player -> instance.participants().isEmpty() || instance.participants().contains(player.getUUID()))
				.map(player -> player.getPersistentData().getString(DungeonMobLevelAdapter.LEGACY_DUNGEON_TAG))
				.filter(value -> !value.isBlank()).findFirst().orElse(instance.id().toString());
		for (DungeonInstanceSavedData.EncounterMarker marker : encounter.markers()) {
			Optional<MobPoolResolver.Selection> selection = MobPoolResolver.select(level, encounter.poolId(),
					instance.effectiveLevel(), encounter.levelOverride()
							? Optional.of(new IntRange(encounter.minSpawnLevel(), encounter.maxSpawnLevel()))
							: Optional.empty(),
					level.random);
			if (selection.isEmpty()) {
				SololevelingMod.LOGGER.warn("Dungeon encounter {} in instance {} could not resolve mob pool {}",
						encounter.key(), instance.id(), encounter.poolId());
				continue;
			}
			DungeonMobLevelAdapter.MobRole role = DungeonMobLevelAdapter.MobRole.fromString(marker.role());
			DungeonMobLevelAdapter.SpawnSpec spec = new DungeonMobLevelAdapter.SpawnSpec(
					instance.id().toString(), encounter.key(), marker.id(), role, selection.get().level(),
					compatibilityTag, selection.get().baseXp().orElse(-1));
			DungeonMobLevelAdapter.SpawnResult result = DungeonMobLevelAdapter.spawnExact(level,
					selection.get().entityType(), marker.position(), level.random.nextFloat() * 360.0F, spec);
			if (result.succeeded() && result.mob() != null) {
				spawnedMobs.add(result.mob());
			} else {
				SololevelingMod.LOGGER.warn("Dungeon encounter {} could not spawn marker {}: {}",
						encounter.key(), marker.id(), result.message());
			}
		}

		if (spawnedMobs.size() != encounter.markers().size()) {
			// Commit waves atomically. A blocked marker or temporarily empty addon tag can recover later.
			spawnedMobs.forEach(Mob::discard);
			encounter.resetProgress();
			return false;
		}
		for (Mob mob : spawnedMobs) {
			if (!encounter.trackMob(mob.getUUID())) {
				spawnedMobs.forEach(Mob::discard);
				encounter.resetProgress();
				return false;
			}
			mob.getPersistentData().remove(DungeonMobLevelAdapter.PENDING_TRACK_TAG);
		}
		for (ServerPlayer player : level.players()) {
			if (instance.participants().isEmpty() || instance.participants().contains(player.getUUID()))
				player.displayClientMessage(Component.literal(encounter.boss()
						? "Boss encounter started." : "Dungeon encounter started.")
						.withStyle(encounter.boss() ? ChatFormatting.DARK_RED : ChatFormatting.RED), true);
		}
		return true;
	}

	private static void reconcileActivatedEncounter(ServerLevel level,
			DungeonInstanceSavedData.Instance instance, DungeonInstanceSavedData.EncounterState encounter) {
		String key = retryKey(instance, encounter);
		if (encounter.trackedMobs().isEmpty()) {
			encounter.resetProgress();
			RETRY_AFTER.put(key, level.getGameTime() + FAILED_ACTIVATION_RETRY_TICKS);
			MISSING_SINCE.remove(key);
			return;
		}
		boolean activationAreaReady = encounter.sequenced() || encounter.triggerBounds().isEmpty()
				? markerChunksLoaded(level, encounter) : playerEntered(level, instance, encounter);
		boolean markerChunksLoaded = markerChunksLoaded(level, encounter);
		boolean anyLoadedMob = encounter.trackedMobs().stream().anyMatch(id -> level.getEntity(id) != null);
		if (!activationAreaReady || !markerChunksLoaded || anyLoadedMob) {
			MISSING_SINCE.remove(key);
			return;
		}
		long missingSince = MISSING_SINCE.computeIfAbsent(key, ignored -> level.getGameTime());
		if (level.getGameTime() - missingSince < MISSING_MOB_RECOVERY_TICKS)
			return;
		SololevelingMod.LOGGER.warn("Resetting dungeon encounter {} in instance {} after tracked mobs were missing for {} ticks",
				encounter.key(), instance.id(), MISSING_MOB_RECOVERY_TICKS);
		encounter.resetProgress();
		MISSING_SINCE.remove(key);
		RETRY_AFTER.put(key, level.getGameTime() + FAILED_ACTIVATION_RETRY_TICKS);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		if (entity.level().isClientSide())
			return;
		CompoundTag data = entity.getPersistentData();
		String instanceText = data.getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		String encounterKey = data.getString(DungeonMobLevelAdapter.ENCOUNTER_TAG);
		if (instanceText.isBlank() || encounterKey.isBlank())
			return;
		UUID instanceId;
		try {
			instanceId = UUID.fromString(instanceText);
		} catch (IllegalArgumentException ignored) {
			return;
		}
		ServerLevel level = (ServerLevel) entity.level();
		Optional<DungeonInstanceSavedData.Instance> found = DungeonInstanceSavedData.get(level).getInstance(instanceId);
		if (found.isEmpty())
			return;
		DungeonInstanceSavedData.Instance instance = found.get();
		Optional<DungeonInstanceSavedData.EncounterState> state = instance.encounter(encounterKey);
		if (state.isEmpty() || !state.get().untrackMob(entity.getUUID()))
			return;
		DungeonInstanceSavedData.EncounterState encounter = state.get();
		RETRY_AFTER.remove(retryKey(instance, encounter));
		MISSING_SINCE.remove(retryKey(instance, encounter));
		if (!encounter.trackedMobs().isEmpty())
			return;
		encounter.markCompleted();
		if (encounter.sequenced())
			scheduleNextWave(level, instance, encounter);
		if (encounter.boss() && completionRequirementsMet(instance))
			completeInstance(level.getServer(), instance);
	}

	private static void scheduleNextWave(ServerLevel level, DungeonInstanceSavedData.Instance instance,
			DungeonInstanceSavedData.EncounterState completed) {
		instance.encounters().stream()
				.filter(candidate -> candidate.sequenced()
						&& candidate.sequenceKey().equals(completed.sequenceKey())
						&& candidate.sequenceOrder() == completed.sequenceOrder() + 1)
				.findFirst()
				.ifPresent(next -> next.scheduleActivation(activationTime(level.getGameTime(), next.delayTicks())));
	}

	private static boolean completionRequirementsMet(DungeonInstanceSavedData.Instance instance) {
		List<DungeonInstanceSavedData.EncounterState> encounters = instance.encounters();
		if (encounters.stream().filter(DungeonInstanceSavedData.EncounterState::boss)
				.anyMatch(encounter -> !encounter.completed()))
			return false;
		for (DungeonInstanceSavedData.EncounterState boss : encounters) {
			if (!boss.boss() || !boss.sequenced())
				continue;
			boolean terminal = encounters.stream().noneMatch(candidate -> candidate.sequenced()
					&& candidate.sequenceKey().equals(boss.sequenceKey())
					&& candidate.sequenceOrder() > boss.sequenceOrder());
			boolean predecessorsComplete = encounters.stream()
					.filter(candidate -> candidate.sequenced()
							&& candidate.sequenceKey().equals(boss.sequenceKey())
							&& candidate.sequenceOrder() < boss.sequenceOrder())
					.allMatch(DungeonInstanceSavedData.EncounterState::completed);
			if (!terminal || !predecessorsComplete)
				return false;
		}
		return true;
	}

	/** Removes persistent orphan mobs when an old instance/chunk is loaded again. */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity entity)
				|| !entity.getPersistentData().getBoolean(DungeonMobLevelAdapter.RUNTIME_SPAWN_TAG)
				|| !(event.getLevel() instanceof ServerLevel level))
			return;
		String instanceText = entity.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		String encounterKey = entity.getPersistentData().getString(DungeonMobLevelAdapter.ENCOUNTER_TAG);
		try {
			Optional<DungeonInstanceSavedData.Instance> found = DungeonInstanceSavedData.get(level)
					.getInstance(UUID.fromString(instanceText));
			boolean pendingTrack = entity.getPersistentData().getBoolean(DungeonMobLevelAdapter.PENDING_TRACK_TAG);
			boolean valid = found.isPresent() && !found.get().completed()
					&& found.get().encounter(encounterKey)
							.map(state -> !state.completed() && (pendingTrack || state.trackedMobs().contains(entity.getUUID())))
							.orElse(false);
			if (!valid) {
				event.setCanceled(true);
				entity.discard();
			}
		} catch (IllegalArgumentException ignored) {
			event.setCanceled(true);
			entity.discard();
		}
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		String currentInstance = player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG);
		boolean completedParticipant = !currentInstance.isBlank()
				&& DungeonInstanceSavedData.get(player.serverLevel()).listInstances().stream()
				.anyMatch(instance -> instance.completed() && instance.id().toString().equals(currentInstance)
						&& instance.participants().contains(player.getUUID()));
		if (completedParticipant)
			setBossKilledCompatibility(player);
	}

	/** Applies a completion receipt only for a participant bound to this exact instance. */
	public static boolean restoreCompletionFor(ServerPlayer player, DungeonInstanceSavedData.Instance instance) {
		if (player == null || instance == null || !instance.completed()
				|| !instance.participants().contains(player.getUUID()))
			return false;
		player.getPersistentData().putString(DungeonMobLevelAdapter.INSTANCE_TAG, instance.id().toString());
		setBossKilledCompatibility(player);
		return true;
	}

	private static void completeInstance(MinecraftServer server, DungeonInstanceSavedData.Instance instance) {
		if (!instance.setCompleted(true))
			return;
		ServerLevel level = server.getLevel(instance.dimension());
		for (DungeonInstanceSavedData.EncounterState encounter : instance.encounters()) {
			RETRY_AFTER.remove(retryKey(instance, encounter));
			MISSING_SINCE.remove(retryKey(instance, encounter));
			if (level != null)
				for (UUID mobId : encounter.trackedMobs()) {
					if (level.getEntity(mobId) != null)
						level.getEntity(mobId).discard();
				}
			encounter.clearTrackedMobs();
			encounter.markCompleted();
		}
		for (UUID participant : instance.participants()) {
			ServerPlayer player = server.getPlayerList().getPlayer(participant);
			if (player == null || !instance.id().toString().equals(
					player.getPersistentData().getString(DungeonMobLevelAdapter.INSTANCE_TAG)))
				continue;
			setBossKilledCompatibility(player);
			player.sendSystemMessage(Component.literal("Dungeon complete: " + instance.dungeonId())
					.withStyle(ChatFormatting.GOLD));
		}
		SololevelingMod.LOGGER.info("Completed dungeon instance {} ({})", instance.id(), instance.dungeonId());
	}

	private static void setBossKilledCompatibility(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(variables -> {
			variables.BossKilled = true;
			variables.syncPlayerVariables(player);
		});
	}

	private static String retryKey(DungeonInstanceSavedData.Instance instance,
			DungeonInstanceSavedData.EncounterState encounter) {
		return instance.id() + ":" + encounter.key();
	}
}
