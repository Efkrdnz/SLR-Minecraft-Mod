package net.solocraft.dungeon;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.data.DungeonDataManager;
import net.solocraft.dungeon.data.DungeonDataSnapshot;
import net.solocraft.dungeon.data.DungeonDataTypes.DungeonKind;
import net.solocraft.dungeon.runtime.DungeonEncounterRuntime;
import net.solocraft.dungeon.runtime.DungeonInstanceSavedData;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.dungeon.runtime.DungeonRuntimeGenerator;
import net.solocraft.entity.DatapackGateEntity;
import net.solocraft.entity.Portal1Entity;
import net.solocraft.guild.GuildGateHelper;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.MagicReadingHelper;
import net.solocraft.util.PlayerEntryGenerationGuard;
import net.solocraft.util.UrgentQuestManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative runtime for datapack-only dungeon gates.
 *
 * <p>No path in this class calls {@link ProceduralDungeonGenerator}. An absent,
 * invalid, rank-incompatible, or reloaded definition is an explicit failure and
 * never falls back to the built-in procedural dungeon system.</p>
 */
public final class DatapackDungeonGateHandler {
	private static final String GATE_MARKER = "slr_datapack_gate";
	private static final String MANUAL = "slr_datapack_gate_manual";
	private static final String AWAITING_SELECTION = "slr_datapack_gate_awaiting_selection";
	private static final String DUNGEON = "slr_datapack_gate_dungeon";
	private static final String RANK = "slr_datapack_gate_rank";
	private static final String BOUND_REVISION = "slr_datapack_gate_bound_revision";
	private static final String GENERATED = "slr_datapack_gate_generated";
	private static final String GENERATING = "slr_datapack_gate_generating";
	private static final String INSTANCE = "slr_datapack_gate_instance";
	private static final String START_X = "slr_datapack_gate_start_x";
	private static final String START_Y = "slr_datapack_gate_start_y";
	private static final String START_Z = "slr_datapack_gate_start_z";
	private static final String SAVE_STATE = "SLRDatapackGate";

	private static final String PLAYER_PROCEDURAL = "slr_procedural_dungeon";
	private static final String PLAYER_PROCEDURAL_RED = "slr_procedural_red_gate";
	private static final String LEGACY_RANK = "slr_procedural_rank";
	private static final double MAX_SELECTION_DISTANCE_SQR = 8.0D * 8.0D;
	private static final double PARTY_DISTANCE = 10.0D;

	private DatapackDungeonGateHandler() {
	}

	/** Immutable option payload suitable for GUI/network projection. */
	public record DungeonOption(ResourceLocation id, DungeonKind kind,
			Set<ProceduralDungeonRank> allowedRanks) {
		public DungeonOption {
			allowedRanks = Set.copyOf(allowedRanks);
		}
	}

	/** One atomic view of the current valid datapack dungeon catalog. */
	public record OptionSnapshot(long revision, List<DungeonOption> options) {
		public OptionSnapshot {
			options = List.copyOf(options);
		}
	}

	public record BindingResult(boolean success, String message) {
		private static BindingResult ok(String message) {
			return new BindingResult(true, message);
		}

		private static BindingResult failure(String message) {
			return new BindingResult(false, message);
		}
	}

	public static OptionSnapshot options() {
		DungeonDataSnapshot snapshot = DungeonDataManager.snapshot();
		List<DungeonOption> values = snapshot.dungeons().values().stream()
				.map(definition -> new DungeonOption(definition.id(),
						definition.kind(), definition.allowedRanks()))
				.toList();
		return new OptionSnapshot(snapshot.revision(), values);
	}

	public static List<DungeonOption> optionsForRank(ProceduralDungeonRank rank) {
		if (rank == null)
			return List.of();
		return options().options().stream()
				.filter(option -> option.allowedRanks().contains(rank))
				.toList();
	}

	public static boolean hasAvailableDungeons() {
		return !DungeonDataManager.snapshot().dungeons().isEmpty();
	}

	public static boolean isDatapackGate(Entity entity) {
		return entity instanceof DatapackGateEntity
				&& entity.getPersistentData().getBoolean(GATE_MARKER);
	}

	public static boolean isManual(DatapackGateEntity gate) {
		return gate != null && gate.getPersistentData().getBoolean(MANUAL);
	}

	public static boolean isAwaitingSelection(DatapackGateEntity gate) {
		return gate != null
				&& gate.getPersistentData().getBoolean(AWAITING_SELECTION)
				&& binding(gate).isEmpty()
				&& !isGenerated(gate);
	}

	public static boolean isGenerated(DatapackGateEntity gate) {
		return gate != null && gate.getPersistentData().getBoolean(GENERATED);
	}

	public static Optional<ResourceLocation> binding(DatapackGateEntity gate) {
		if (gate == null)
			return Optional.empty();
		String value = gate.getPersistentData().getString(DUNGEON);
		if (value.isBlank())
			return Optional.empty();
		return Optional.ofNullable(ResourceLocation.tryParse(value));
	}

	public static Optional<ProceduralDungeonRank> rank(DatapackGateEntity gate) {
		if (gate == null)
			return Optional.empty();
		return ProceduralDungeonRank.tryParse(
				gate.getPersistentData().getString(RANK));
	}

	/**
	 * Called after the shared portal spawn lifecycle. Spawn eggs remain
	 * configurable; non-egg gates must be bound immediately by the natural
	 * spawner and are discarded on the next tick if that does not happen.
	 */
	public static void initializeSpawn(DatapackGateEntity gate, MobSpawnType reason) {
		if (gate == null)
			return;
		CompoundTag data = gate.getPersistentData();
		data.putBoolean(GATE_MARKER, true);
		boolean manual = reason == MobSpawnType.SPAWN_EGG;
		data.putBoolean(MANUAL, manual);
		data.putBoolean(AWAITING_SELECTION, manual);
		data.putBoolean(GENERATED, false);
		data.putBoolean(GENERATING, false);
		data.remove(DUNGEON);
		data.remove(RANK);
		data.remove(BOUND_REVISION);
		data.remove(INSTANCE);
		data.remove(LEGACY_RANK);
		data.remove("slr_procedural_gate");
		data.remove("slr_procedural_red_gate");
		data.remove("slr_is_red_gate");
		gate.setTexture(DatapackGateEntity.TEXTURE_NAME);
	}

	/**
	 * Safety net for command/spawner-created entities that were never given a
	 * valid predetermined binding.
	 */
	public static void discardInvalidUnboundSpawn(DatapackGateEntity gate) {
		if (gate == null || gate.isRemoved() || binding(gate).isPresent())
			return;
		if (!isManual(gate) || !hasAvailableDungeons())
			gate.discard();
	}

	/**
	 * Natural-spawn binding API. The first successful binding is immutable.
	 */
	public static boolean bind(DatapackGateEntity gate, ResourceLocation dungeonId,
			ProceduralDungeonRank gateRank) {
		if (gate == null || gate.isRemoved() || dungeonId == null || gateRank == null
				|| isGenerated(gate) || binding(gate).isPresent())
			return false;
		DungeonDataSnapshot snapshot = DungeonDataManager.snapshot();
		var definition = snapshot.dungeon(dungeonId);
		if (definition.isEmpty() || !definition.get().supportsRank(gateRank))
			return false;

		CompoundTag data = gate.getPersistentData();
		data.putBoolean(GATE_MARKER, true);
		data.putString(DUNGEON, dungeonId.toString());
		data.putString(RANK, gateRank.name());
		// Compatibility for guild rank displays and the shared gate-break reading.
		data.putString(LEGACY_RANK, gateRank.name());
		data.putLong(BOUND_REVISION, snapshot.revision());
		data.putBoolean(AWAITING_SELECTION, false);
		data.putBoolean(GENERATING, false);
		gate.setTexture(DatapackGateEntity.TEXTURE_NAME);
		return true;
	}

	public static BindingResult bindSelection(ServerPlayer player,
			DatapackGateEntity gate, ResourceLocation dungeonId,
			ProceduralDungeonRank gateRank) {
		return bindSelection(player, gate, dungeonId, gateRank,
				DungeonDataManager.snapshot().revision());
	}

	public static BindingResult bindSelection(ServerPlayer player,
			DatapackGateEntity gate, ResourceLocation dungeonId,
			ProceduralDungeonRank gateRank, long expectedRevision) {
		if (player == null || gate == null || dungeonId == null || gateRank == null)
			return BindingResult.failure("Invalid datapack gate selection.");
		if (gate.isRemoved() || player.level() != gate.level()
				|| player.distanceToSqr(gate) > MAX_SELECTION_DISTANCE_SQR)
			return BindingResult.failure("This gate is no longer close enough to configure.");
		if (!isManual(gate) || !isAwaitingSelection(gate))
			return BindingResult.failure("This gate is no longer awaiting a selection.");

		DungeonDataSnapshot snapshot = DungeonDataManager.snapshot();
		if (snapshot.revision() != expectedRevision)
			return BindingResult.failure(
					"Datapacks changed while this screen was open. Choose again.");
		var definition = snapshot.dungeon(dungeonId);
		if (definition.isEmpty())
			return BindingResult.failure(
					"That datapack dungeon is no longer loaded or valid.");
		if (!definition.get().supportsRank(gateRank))
			return BindingResult.failure("That dungeon does not support "
					+ gateRank.name() + "-rank gates.");
		if (!bind(gate, dungeonId, gateRank))
			return BindingResult.failure("The datapack gate could not be bound.");
		return BindingResult.ok("Configured " + gateRank.name() + "-rank gate for "
				+ dungeonId + ".");
	}

	/** Sole server-side interaction route used by DatapackGateEntity. */
	public static void interact(ServerPlayer player, DatapackGateEntity gate) {
		if (player == null || gate == null || gate.isRemoved())
			return;
		if (isAwaitingSelection(gate)) {
			DatapackGateSelectionService.requestOpen(player, gate);
			return;
		}
		Optional<ProceduralDungeonRank> gateRank = rank(gate);
		if (MagicReadingHelper.isHoldingMagicReader(player)) {
			if (gateRank.isPresent())
				MagicReadingHelper.showRankReading(player, gateRank.get());
			else
				player.displayClientMessage(
						Component.literal("This datapack gate is not configured."), true);
			return;
		}
		enter(player, gate);
	}

	public static void enter(ServerPlayer player, DatapackGateEntity gate) {
		if (player == null || gate == null || gate.isRemoved())
			return;
		Optional<ResourceLocation> dungeonId = binding(gate);
		Optional<ProceduralDungeonRank> gateRank = rank(gate);
		if (dungeonId.isEmpty() || gateRank.isEmpty()) {
			player.displayClientMessage(
					Component.literal("This datapack gate is not configured."), true);
			return;
		}

		var definition = DungeonDataManager.dungeon(dungeonId.get());
		if (definition.isEmpty()) {
			player.displayClientMessage(Component.literal(
					"This gate's datapack dungeon is no longer loaded or valid."), true);
			return;
		}
		if (!definition.get().supportsRank(gateRank.get())) {
			player.displayClientMessage(Component.literal("This "
					+ gateRank.get().name()
					+ "-rank gate is no longer supported by its datapack dungeon."), true);
			return;
		}
		if (isCompleted(gate, player.server)) {
			player.displayClientMessage(
					Component.literal("This gate has already been cleared."), true);
			return;
		}
		if (isDungeonBound(player)) {
			player.displayClientMessage(
					Component.literal("You are already bound to a dungeon."), true);
			return;
		}
		if (GuildGateHelper.prepareGateEntry(gate.level(), gate, player))
			return;

		List<ServerPlayer> entrants = nearbyPartyMembers(gate, player);
		Map<UUID, Long> entryGenerations = new LinkedHashMap<>();
		for (ServerPlayer entrant : entrants) {
			prepareEntrant(gate, entrant);
			entryGenerations.put(entrant.getUUID(),
					PlayerEntryGenerationGuard.begin(entrant));
		}
		teleportEntrants(gate, dungeonId.get(), gateRank.get(), entrants,
				entryGenerations);
	}

	private static void teleportEntrants(DatapackGateEntity gate,
			ResourceLocation dungeonId, ProceduralDungeonRank gateRank,
			List<ServerPlayer> entrants, Map<UUID, Long> entryGenerations) {
		ServerPlayer firstPlayer = entrants.stream().findFirst().orElse(null);
		if (firstPlayer == null)
			return;
		ServerLevel destination = firstPlayer.server.getLevel(destinationFor(gateRank));
		if (destination == null) {
			failEntrants(entrants,
					"The dungeon destination dimension is unavailable.");
			return;
		}
		BlockPos target = storedTarget(gate);

		boolean firstGeneration = !isGenerated(gate);
		if (firstGeneration) {
			if (gate.getPersistentData().getBoolean(GENERATING)) {
				failEntrants(entrants,
						"This datapack gate is already preparing its dungeon.");
				return;
			}
			gate.getPersistentData().putBoolean(GENERATING, true);
		}

		SololevelingMod.queueServerWork(firstGeneration ? 5 : 0, () -> {
			try {
				List<ServerPlayer> currentEntrants = currentPreparedEntrants(
						gate.getStringUUID(), entrants, entryGenerations);
				if (currentEntrants.isEmpty())
					return;
				ServerPlayer currentFirst = currentEntrants.get(0);
				if (gate.isRemoved()) {
					failEntrants(currentEntrants,
							"The datapack gate disappeared before entry finished.");
					return;
				}
				var liveDefinition = DungeonDataManager.dungeon(dungeonId);
				if (liveDefinition.isEmpty()
						|| !liveDefinition.get().supportsRank(gateRank)) {
					failEntrants(currentEntrants,
							"The gate's datapack dungeon changed or became unavailable.");
					return;
				}
				if (isCompleted(gate, currentFirst.server)) {
					failEntrants(currentEntrants,
							"This gate was cleared before entry finished.");
					return;
				}

				DungeonInstanceSavedData.Instance instance;
				if (!isGenerated(gate)) {
					long seed = gate.getUUID().getMostSignificantBits()
							^ gate.getUUID().getLeastSignificantBits()
							^ destination.getSeed();
					DungeonRuntimeGenerator.GenerationResult generated =
							DungeonRuntimeGenerator.generate(destination, dungeonId,
									target, seed, currentEntrants, null);
					if (!generated.success() || generated.instanceId() == null
							|| generated.playerStart() == null) {
						SololevelingMod.LOGGER.error(
								"Datapack gate {} failed to generate {}: {}",
								gate.getUUID(), dungeonId, generated.message());
						failEntrants(currentEntrants,
								"Datapack dungeon generation failed. The gate was not consumed; check the Builder validation or server log.");
						return;
					}
					gate.getPersistentData().putString(INSTANCE,
							generated.instanceId().toString());
					gate.getPersistentData().putBoolean(GENERATED, true);
					storeStart(gate, generated.playerStart());
					gate.getEntityData().set(
							Portal1Entity.DATA_usedbefore, true);
					instance = DungeonInstanceSavedData.get(destination)
							.getInstance(generated.instanceId()).orElse(null);
				} else {
					instance = boundInstance(gate, destination).orElse(null);
				}

				if (instance == null) {
					failEntrants(currentEntrants,
							"This gate no longer has valid dungeon instance state.");
					return;
				}
				// Normal datapack gates have no entry-room escape. The encounter
				// runtime persists the boss death position and opens the return
				// portal only after the instance is complete.
				instance.setReturnPortalDeferred(true);
				instance.setReturnPortalSuppressed(false);
				ProceduralDungeonCompletionHandler.discardMatchingReturnPortals(
						destination, instance.id(), gate.getStringUUID());

				BlockPos playerStart = instance.playerStart()
						.orElseGet(() -> storedStart(gate));
				for (ServerPlayer entrant : currentEntrants)
					enterInstance(destination, gate, instance, playerStart, entrant);
			} finally {
				gate.getPersistentData().putBoolean(GENERATING, false);
			}
		});
	}

	private static List<ServerPlayer> currentPreparedEntrants(String gateId,
			List<ServerPlayer> entrants, Map<UUID, Long> entryGenerations) {
		return entrants.stream().filter(entrant -> {
			Long generation = entryGenerations.get(entrant.getUUID());
			return generation != null
					&& PlayerEntryGenerationGuard.isCurrent(entrant, generation)
					&& gateId.equals(entrant.getPersistentData().getString("dungeon_tag"))
					&& entrant.getPersistentData().getBoolean(PLAYER_PROCEDURAL)
					&& entrant.getCapability(
							SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
							.map(capability -> capability.dungeoning)
							.orElse(false);
		}).toList();
	}

	private static void enterInstance(ServerLevel destination,
			DatapackGateEntity gate, DungeonInstanceSavedData.Instance instance,
			BlockPos playerStart, ServerPlayer entrant) {
		if (!instance.participants().contains(entrant.getUUID())
				&& !instance.addParticipant(entrant.getUUID())) {
			resetFailedEntrant(entrant);
			entrant.sendSystemMessage(Component.literal(
					"This dungeon already has the maximum number of participants."));
			return;
		}
		entrant.getPersistentData().putString(
				DungeonMobLevelAdapter.INSTANCE_TAG, instance.id().toString());
		UrgentQuestManager.markDungeonId(entrant,
				instance.dungeonId().toString());
		DungeonEncounterRuntime.restoreCompletionFor(entrant, instance);
		entrant.teleportTo(destination, playerStart.getX() + 0.5D,
				playerStart.getY(), playerStart.getZ() + 0.5D,
				entrant.getYRot(), entrant.getXRot());
		entrant.setNoGravity(false);
		entrant.fallDistance = 0.0F;
	}

	private static Optional<DungeonInstanceSavedData.Instance> boundInstance(
			DatapackGateEntity gate, ServerLevel level) {
		String value = gate.getPersistentData().getString(INSTANCE);
		if (value.isBlank())
			return Optional.empty();
		try {
			return DungeonInstanceSavedData.get(level)
					.getInstance(UUID.fromString(value));
		} catch (IllegalArgumentException ignored) {
			return Optional.empty();
		}
	}

	private static boolean isCompleted(DatapackGateEntity gate,
			MinecraftServer server) {
		String gateToken = gate.getStringUUID() + ",";
		if (SololevelingModVariables.MapVariables.get(gate.level())
				.GatesCleared.contains(gateToken))
			return true;
		return boundInstance(gate, server.overworld())
				.map(DungeonInstanceSavedData.Instance::completed)
				.orElse(false);
	}

	private static List<ServerPlayer> nearbyPartyMembers(
			DatapackGateEntity gate, ServerPlayer leader) {
		String party = partyOf(leader);
		if (party.isBlank())
			return List.of(leader);
		List<ServerPlayer> entrants = new ArrayList<>();
		entrants.add(leader);
		for (ServerPlayer candidate : leader.server.getPlayerList().getPlayers()) {
			if (entrants.size() >= DungeonInstanceSavedData.MAX_PARTICIPANTS)
				break;
			if (candidate == leader || candidate.level() != gate.level()
					|| candidate.distanceTo(gate) > PARTY_DISTANCE
					|| !party.equals(partyOf(candidate))
					|| isDungeonBound(candidate))
				continue;
			entrants.add(candidate);
		}
		return List.copyOf(entrants);
	}

	private static String partyOf(ServerPlayer player) {
		return player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> capability.party)
				.orElse("");
	}

	private static boolean isDungeonBound(ServerPlayer player) {
		if (!player.getPersistentData()
				.getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank())
			return true;
		return player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> capability.dungeoning)
				.orElse(false);
	}

	private static void prepareEntrant(DatapackGateEntity gate,
			ServerPlayer entrant) {
		dismissOwnedShadows(gate, entrant);
		entrant.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.DunX = entrant.getX();
					capability.DunY = entrant.getY();
					capability.DunZ = entrant.getZ();
					capability.dungeoning = true;
					capability.BossKilled = false;
					capability.syncPlayerVariables(entrant);
				});
		entrant.getPersistentData().putString(
				"dungeon_tag", gate.getStringUUID());
		entrant.getPersistentData().putBoolean(PLAYER_PROCEDURAL, true);
		entrant.getPersistentData().putBoolean(PLAYER_PROCEDURAL_RED, false);
		entrant.getPersistentData().remove(DungeonMobLevelAdapter.INSTANCE_TAG);
	}

	private static void dismissOwnedShadows(DatapackGateEntity gate,
			ServerPlayer owner) {
		Vec3 center = gate.position();
		AABB bounds = new AABB(center, center).inflate(250.0D);
		List<Entity> nearby = gate.level().getEntitiesOfClass(
				Entity.class, bounds, entity -> true).stream()
				.sorted(Comparator.comparingDouble(
						entity -> entity.distanceToSqr(center)))
				.toList();
		TagKey<net.minecraft.world.entity.EntityType<?>> shadows =
				TagKey.create(Registries.ENTITY_TYPE,
						ResourceLocation.parse("shadows"));
		for (Entity entity : nearby) {
			if (entity.getType().is(shadows)
					&& entity instanceof TamableAnimal tame
					&& owner instanceof LivingEntity
					&& tame.isOwnedBy(owner))
				entity.discard();
		}
	}

	private static void failEntrants(List<ServerPlayer> entrants,
			String message) {
		for (ServerPlayer entrant : entrants) {
			resetFailedEntrant(entrant);
			entrant.sendSystemMessage(Component.literal(message));
		}
	}

	private static void resetFailedEntrant(ServerPlayer entrant) {
		entrant.setNoGravity(false);
		entrant.fallDistance = 0.0F;
		entrant.getPersistentData().putBoolean(PLAYER_PROCEDURAL, false);
		entrant.getPersistentData().putBoolean(PLAYER_PROCEDURAL_RED, false);
		entrant.getPersistentData().remove("dungeon_tag");
		entrant.getPersistentData().remove(DungeonMobLevelAdapter.INSTANCE_TAG);
		entrant.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.dungeoning = false;
					capability.BossKilled = false;
					capability.syncPlayerVariables(entrant);
				});
	}

	private static BlockPos storedTarget(DatapackGateEntity gate) {
		return BlockPos.containing(
				gate.getPersistentData().getDouble("tpx"),
				gate.getPersistentData().getDouble("tpy"),
				gate.getPersistentData().getDouble("tpz"));
	}

	private static void storeStart(DatapackGateEntity gate, BlockPos start) {
		gate.getPersistentData().putDouble(START_X, start.getX());
		gate.getPersistentData().putDouble(START_Y, start.getY());
		gate.getPersistentData().putDouble(START_Z, start.getZ());
	}

	private static BlockPos storedStart(DatapackGateEntity gate) {
		return BlockPos.containing(
				gate.getPersistentData().getDouble(START_X),
				gate.getPersistentData().getDouble(START_Y),
				gate.getPersistentData().getDouble(START_Z));
	}

	private static ResourceKey<Level> destinationFor(
			ProceduralDungeonRank rank) {
		String path = switch (rank) {
			case E, D -> "dungeon_dimension_d";
			case C -> "dungeon_dimension_c";
			case B -> "dungeon_dimension_b";
			case A -> "dungeon_dimension_a";
			case S -> "dungeon_dimension_s";
		};
		return ResourceKey.create(Registries.DIMENSION,
				ResourceLocation.fromNamespaceAndPath("sololeveling", path));
	}

	/**
	 * Forge persistent data is already durable; this explicit copy also gives the
	 * entity a stable, inspectable save payload if ForgeData is ever sanitized.
	 */
	public static void writeAdditionalSaveData(DatapackGateEntity gate,
			CompoundTag root) {
		if (gate == null || root == null)
			return;
		CompoundTag source = gate.getPersistentData();
		CompoundTag state = new CompoundTag();
		copyBoolean(source, state, GATE_MARKER);
		copyBoolean(source, state, MANUAL);
		copyBoolean(source, state, AWAITING_SELECTION);
		copyBoolean(source, state, GENERATED);
		copyBoolean(source, state, GENERATING);
		copyString(source, state, DUNGEON);
		copyString(source, state, RANK);
		copyLong(source, state, BOUND_REVISION);
		copyString(source, state, INSTANCE);
		copyDouble(source, state, START_X);
		copyDouble(source, state, START_Y);
		copyDouble(source, state, START_Z);
		root.put(SAVE_STATE, state);
	}

	public static void readAdditionalSaveData(DatapackGateEntity gate,
			CompoundTag root) {
		if (gate == null || root == null
				|| !root.contains(SAVE_STATE, Tag.TAG_COMPOUND))
			return;
		CompoundTag state = root.getCompound(SAVE_STATE);
		CompoundTag target = gate.getPersistentData();
		copyBoolean(state, target, GATE_MARKER);
		copyBoolean(state, target, MANUAL);
		copyBoolean(state, target, AWAITING_SELECTION);
		copyBoolean(state, target, GENERATED);
		copyBoolean(state, target, GENERATING);
		copyString(state, target, DUNGEON);
		copyString(state, target, RANK);
		copyLong(state, target, BOUND_REVISION);
		copyString(state, target, INSTANCE);
		copyDouble(state, target, START_X);
		copyDouble(state, target, START_Y);
		copyDouble(state, target, START_Z);
		rank(gate).ifPresent(value ->
				target.putString(LEGACY_RANK, value.name()));
		target.remove("slr_procedural_gate");
		target.remove("slr_procedural_red_gate");
		target.remove("slr_is_red_gate");
		target.putBoolean(GENERATING, false);
		gate.setTexture(DatapackGateEntity.TEXTURE_NAME);
	}

	private static void copyBoolean(CompoundTag from, CompoundTag to,
			String key) {
		if (from.contains(key, Tag.TAG_BYTE))
			to.putBoolean(key, from.getBoolean(key));
	}

	private static void copyString(CompoundTag from, CompoundTag to,
			String key) {
		if (from.contains(key, Tag.TAG_STRING))
			to.putString(key, from.getString(key));
	}

	private static void copyLong(CompoundTag from, CompoundTag to,
			String key) {
		if (from.contains(key, Tag.TAG_LONG))
			to.putLong(key, from.getLong(key));
	}

	private static void copyDouble(CompoundTag from, CompoundTag to,
			String key) {
		if (from.contains(key, Tag.TAG_DOUBLE))
			to.putDouble(key, from.getDouble(key));
	}
}
