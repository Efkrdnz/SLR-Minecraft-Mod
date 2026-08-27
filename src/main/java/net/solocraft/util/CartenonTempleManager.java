package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.CartenonGateEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.CartenonAwakeningStateMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.DungeonDimensionPlayerLeavesDimensionProcedure;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** Discovery, instancing, entry safety, and awakening choice for Cartenon. */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class CartenonTempleManager {
	public static final ResourceKey<Level> CARTENON_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "cartenon_temple"));

	private static final Set<String> ELIGIBLE_DUNGEONS = Set.of(
			"dungeon_dimension_d",
			"dungeon_dimension_c",
			"dungeon_dimension_b",
			"dungeon_dimension_a",
			"dungeon_dimension_s",
			"dungeon_dimension_snow");
	private static final int INSTANCE_SPACING = 512;
	private static final int INSTANCE_COLUMNS = 32;
	private static final int TEMPLE_FLOOR_Y = 64;
	private static final int ENTRY_PROTECTION_TICKS = 40;
	private static final int DECLINE_DELAY_TICKS = 4;
	private static final Map<MinecraftServer, Map<Integer, LinkedHashSet<UUID>>> WAITING_PLAYERS = new WeakHashMap<>();

	/** Where the player stood when they entered, so they can be put back. */
	private static final String RETURN_DIM_TAG = "slr_cartenon_return_dim";
	private static final String RETURN_X_TAG = "slr_cartenon_return_x";
	private static final String RETURN_Y_TAG = "slr_cartenon_return_y";
	private static final String RETURN_Z_TAG = "slr_cartenon_return_z";

	private static final String INSTANCE_TAG = "slr_cartenon_instance";
	private static final String ENTRY_PROTECTION_TAG = "slr_cartenon_entry_protection";
	private static final String AWAKENING_PENDING_TAG = "slr_cartenon_awakening_pending";
	private static final String DECLINE_TICKS_TAG = "slr_cartenon_decline_ticks";
	private static final String FINALE_GATE_TAG = "slr_cartenon_finale_gate";
	private static final String KNEEL_TICKS_TAG = "slr_temple_kneel_ticks";
	/** Two seconds of held sneak. Long enough that it is never accidental. */
	private static final int KNEEL_REQUIRED_TICKS = 40;
	private static final String DEATH_BYPASS_TAG = "slr_cartenon_death_bypass";
	private static final String PREVIOUS_INVULNERABLE_TAG = "slr_cartenon_previous_invulnerable";
	private static final String PREVIOUS_NO_GRAVITY_TAG = "slr_cartenon_previous_no_gravity";
	private static final String PROTECTION_STATE_SAVED_TAG = "slr_cartenon_protection_state_saved";

	private CartenonTempleManager() {
	}

	/** Called once from the common boss-credit path after a normal gate boss dies. */
	public static boolean onDungeonBossDefeated(LevelAccessor world, Entity boss, Entity creditedSource,
			String dungeonTag) {
		if (!(world instanceof ServerLevel level) || !(creditedSource instanceof ServerPlayer killer)
				|| boss == null || dungeonTag == null || dungeonTag.isBlank())
			return false;
		return onDungeonBossDefeated(level, boss, killer, dungeonTag,
				dungeonParticipants(killer, level, dungeonTag));
	}

	/**
	 * Scoped variant used by runtime dungeons. The supplied collection must be the
	 * complete authoritative participant set; otherwise the normal return portal
	 * remains available.
	 */
	public static boolean onDungeonBossDefeated(LevelAccessor world, Entity boss, Entity creditedSource,
			String dungeonTag, Collection<ServerPlayer> suppliedParticipants) {
		if (!(world instanceof ServerLevel level) || !(creditedSource instanceof ServerPlayer killer)
				|| boss == null || dungeonTag == null || dungeonTag.isBlank()
				|| suppliedParticipants == null)
			return false;
		if (!ELIGIBLE_DUNGEONS.contains(level.dimension().location().getPath()))
			return false;
		// A hidden gate is not a usable alternate exit when its destination is
		// unavailable. Keep the normal return portal in that case.
		if (killer.server.getLevel(CARTENON_DIMENSION) == null)
			return false;

		if (StoryModeIntroManager.isHandledStoryBoss(killer, boss))
			return true;
		boolean storyIntro = StoryModeIntroManager.isStoryBoss(killer, boss);
		LinkedHashSet<ServerPlayer> uniqueParticipants = new LinkedHashSet<>();
		if (storyIntro) {
			uniqueParticipants.add(killer);
		} else {
			for (ServerPlayer participant : suppliedParticipants) {
				if (participant == null || participant.serverLevel() != level)
					return false;
				uniqueParticipants.add(participant);
			}
		}
		List<ServerPlayer> participants = List.copyOf(uniqueParticipants);
		CartenonProgressSavedData progressData = CartenonProgressSavedData.get(level);
		if (participants.isEmpty() || !participants.contains(killer))
			return false;
		List<ServerPlayer> invitationOwners = new ArrayList<>();
		if (storyIntro) {
			if (!progressData.isResolved(killer.getUUID()))
				invitationOwners.add(killer);
		} else {
			for (ServerPlayer participant : participants) {
				if (!SystemPlayerAccess.hasSystem(participant)
						&& !progressData.isResolved(participant.getUUID())
						&& progressData.recordDungeonClear(participant.getUUID(), dungeonTag))
					invitationOwners.add(participant);
			}
		}
		// Record eligible members' clear progress even when a shared alternate exit
		// would strand somebody. The normal return portal remains in that case.
		if (!storyIntro && participants.stream().anyMatch(participant ->
				SystemPlayerAccess.hasSystem(participant)
						|| progressData.isResolved(participant.getUUID())))
			return false;
		if (invitationOwners.isEmpty())
			return false;

		int instanceId = progressData.allocateInstance();
		for (ServerPlayer owner : invitationOwners)
			progressData.markGateOffered(owner.getUUID(), instanceId);

		CartenonGateEntity gate = SololevelingModEntities.CARTENON_GATE.get().create(level);
		if (gate == null) {
			for (ServerPlayer owner : invitationOwners)
				progressData.cancelGateOffer(owner.getUUID());
			return false;
		}

		LinkedHashSet<UUID> allowedPlayers = new LinkedHashSet<>();
		for (ServerPlayer participant : participants)
			allowedPlayers.add(participant.getUUID());
		gate.configure(invitationOwners.get(0).getUUID(), allowedPlayers, instanceId);
		BlockPos gatePos = findGatePosition(level, boss.blockPosition());
		gate.moveTo(gatePos.getX() + 0.5D, gatePos.getY(), gatePos.getZ() + 0.5D,
				killer.getYRot() + 180.0F, 0.0F);
		if (!level.addFreshEntity(gate)) {
			for (ServerPlayer owner : invitationOwners)
				progressData.cancelGateOffer(owner.getUUID());
			return false;
		}
		if (storyIntro)
			StoryModeIntroManager.onCartenonGateCreated(killer, gate, instanceId);

		level.playSound(null, gatePos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.1F, 1.32F);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
				gate.getX(), gate.getY() + 1.2D, gate.getZ(), 90, 0.9D, 1.2D, 0.35D, 0.08D);
		for (ServerPlayer participant : participants) {
			SystemNotifications.showTitleUnder(participant, 0xFF4A84FF, 110,
					Component.literal("HIDDEN DUNGEON").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
					Component.literal("A strange gate has appeared.").withStyle(ChatFormatting.DARK_PURPLE));
		}
		return true;
	}

	public static void enterGate(ServerPlayer player, CartenonGateEntity gate) {
		if (player == null || gate == null || gate.isRemoved())
			return;
		// The return is taken by a player who already has the System, which the
		// awakening path below explicitly refuses. Route it first.
		if (gate.getPersistentData().getBoolean(FINALE_GATE_TAG)) {
			enterFinaleGate(player, gate);
			return;
		}
		if (SystemPlayerAccess.hasSystem(player))
			return;
		if (!gate.isAllowed(player.getUUID())) {
			player.sendSystemMessage(Component.literal("This hidden gate does not respond to you.")
					.withStyle(ChatFormatting.RED));
			return;
		}

		CartenonProgressSavedData progressData = CartenonProgressSavedData.get(player.serverLevel());
		if (progressData.isResolved(player.getUUID())) {
			player.sendSystemMessage(Component.literal("The System has already recorded your decision.")
					.withStyle(ChatFormatting.DARK_GRAY));
			return;
		}

		int instanceId = Math.max(1, gate.getInstanceId());
		progressData.associateInstance(player.getUUID(), instanceId);
		queuePlayer(player.server, instanceId, player.getUUID());

		ServerLevel templeLevel = player.server.getLevel(CARTENON_DIMENSION);
		if (templeLevel == null) {
			removeWaitingPlayer(player.server, instanceId, player.getUUID());
			player.sendSystemMessage(Component.literal("The Cartenon Temple dimension is unavailable.")
					.withStyle(ChatFormatting.RED));
			return;
		}

		BlockPos origin = instanceOrigin(instanceId);
		boolean completionMarker = templeLevel.getBlockState(origin.below(2)).is(Blocks.LODESTONE);
		if (progressData.isInstanceBuilt(instanceId) && completionMarker) {
			teleportWaitingPlayers(player.server, instanceId);
			return;
		}
		if (completionMarker) {
			progressData.markInstanceBuilt(instanceId);
			teleportWaitingPlayers(player.server, instanceId);
			return;
		}

		SystemNotifications.showTitleUnder(player, 0xFF3FC6FF, 100,
				Component.literal("CARTENON TEMPLE").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.literal("The hidden dungeon is stabilizing...").withStyle(ChatFormatting.GRAY));
		if (CartenonTempleGenerator.isBuildingAt(templeLevel, origin))
			return;

		MinecraftServer server = player.server;
		boolean started = CartenonTempleGenerator.startAt(templeLevel, origin, Direction.SOUTH,
				player.getUUID(), player.getGameProfile().getName(), true, () -> {
					CartenonProgressSavedData.get(templeLevel).markInstanceBuilt(instanceId);
					teleportWaitingPlayers(server, instanceId);
				});
		if (!started && !CartenonTempleGenerator.isBuildingAt(templeLevel, origin)) {
			removeWaitingPlayer(player.server, instanceId, player.getUUID());
			player.sendSystemMessage(Component.literal("The hidden dungeon failed to stabilize.")
					.withStyle(ChatFormatting.RED));
		}
	}

	/**
	 * Spawns the return gate beside a player who has reached the peak.
	 *
	 * <p>Offered once and remembered in saved data, so it does not reappear every
	 * tick. Declining or walking out cancels the offer, which lets it be made
	 * again later -- the ending is never missable.
	 */
	/**
	 * Issues the standing summons once the player reaches the peak.
	 *
	 * <p>The System asks rather than dropping a portal at their feet. Opening the
	 * way is then the player's own deliberate act -- see {@link #tickKneel}.
	 */
	public static void offerFinaleSummons(ServerPlayer player) {
		if (player == null || !SystemAuthorityManager.isFinaleAvailable(player))
			return;
		ServerLevel level = player.serverLevel();
		if (level.dimension() == CARTENON_DIMENSION
				|| player.server.getLevel(CARTENON_DIMENSION) == null)
			return;
		CartenonProgressSavedData progressData = CartenonProgressSavedData.get(level);
		if (progressData.isFinaleOffered(player.getUUID())
				|| progressData.isFinaleResolved(player.getUUID()))
			return;
		UrgentQuestManager.startCartenonSummons(player);
	}

	/**
	 * Kneeling answers the summons and opens the gate.
	 *
	 * <p>A held action rather than a tap so it cannot fire from an incidental
	 * crouch, and it only counts while the summons quest is actually active.
	 */
	private static void tickKneel(ServerPlayer player) {
		if (!UrgentQuestManager.hasCartenonSummons(player))
			return;
		if (!player.isShiftKeyDown() || !player.onGround()) {
			player.getPersistentData().remove(KNEEL_TICKS_TAG);
			return;
		}
		int kneeling = player.getPersistentData().getInt(KNEEL_TICKS_TAG) + 1;
		if (kneeling < KNEEL_REQUIRED_TICKS) {
			player.getPersistentData().putInt(KNEEL_TICKS_TAG, kneeling);
			if (kneeling % 10 == 0)
				player.serverLevel().sendParticles(
						net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
						player.getX(), player.getY() + 0.2D, player.getZ(),
						12, 0.4D, 0.1D, 0.4D, 0.02D);
			return;
		}
		player.getPersistentData().remove(KNEEL_TICKS_TAG);
		if (openFinaleGate(player))
			UrgentQuestManager.clearCartenonSummons(player);
	}

	private static boolean openFinaleGate(ServerPlayer player) {
		if (!SystemAuthorityManager.isFinaleAvailable(player))
			return false;
		ServerLevel level = player.serverLevel();
		CartenonProgressSavedData progressData = CartenonProgressSavedData.get(level);
		if (progressData.isFinaleOffered(player.getUUID())
				|| progressData.isFinaleResolved(player.getUUID()))
			return false;

		CartenonGateEntity gate = SololevelingModEntities.CARTENON_GATE.get().create(level);
		if (gate == null)
			return false;
		int instanceId = progressData.allocateInstance();
		gate.configure(player.getUUID(), new LinkedHashSet<>(List.of(player.getUUID())), instanceId);
		gate.getPersistentData().putBoolean(FINALE_GATE_TAG, true);
		BlockPos gatePos = findGatePosition(level, player.blockPosition());
		gate.moveTo(gatePos.getX() + 0.5D, gatePos.getY(), gatePos.getZ() + 0.5D,
				player.getYRot() + 180.0F, 0.0F);
		if (!level.addFreshEntity(gate))
			return false;
		progressData.markFinaleOffered(player.getUUID(), instanceId);
		level.playSound(null, gatePos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.2F, 0.7F);
		SystemNotifications.showTitleUnder(player, 0xFF9B5CFF, 140,
				Component.literal("THE WAY BACK").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
				Component.literal("The temple that measured you has opened again.")
						.withStyle(ChatFormatting.GRAY));
		return true;
	}

	private static void enterFinaleGate(ServerPlayer player, CartenonGateEntity gate) {
		if (!gate.isAllowed(player.getUUID()))
			return;
		CartenonProgressSavedData progressData = CartenonProgressSavedData.get(player.serverLevel());
		if (progressData.isFinaleResolved(player.getUUID()))
			return;
		ServerLevel templeLevel = player.server.getLevel(CARTENON_DIMENSION);
		if (templeLevel == null) {
			player.sendSystemMessage(Component.literal("The Cartenon Temple dimension is unavailable.")
					.withStyle(ChatFormatting.RED));
			return;
		}
		int instanceId = Math.max(1, gate.getInstanceId());
		queuePlayer(player.server, instanceId, player.getUUID());
		// One use. The return gate is a door that closes behind you; leaving it
		// standing in the overworld is both wrong and confusing.
		gate.discard();
		BlockPos origin = instanceOrigin(instanceId);
		if (progressData.isInstanceBuilt(instanceId)
				|| templeLevel.getBlockState(origin.below(2)).is(Blocks.LODESTONE)) {
			progressData.markInstanceBuilt(instanceId);
			teleportWaitingPlayers(player.server, instanceId);
			return;
		}
		SystemNotifications.showTitleUnder(player, 0xFF9B5CFF, 100,
				Component.literal("CARTENON TEMPLE").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
				Component.literal("The way back is stabilizing...").withStyle(ChatFormatting.GRAY));
		if (CartenonTempleGenerator.isBuildingAt(templeLevel, origin))
			return;
		MinecraftServer server = player.server;
		CartenonTempleGenerator.startAt(templeLevel, origin, Direction.SOUTH,
				player.getUUID(), player.getGameProfile().getName(), true, () -> {
					CartenonProgressSavedData.get(templeLevel).markInstanceBuilt(instanceId);
					teleportWaitingPlayers(server, instanceId);
				});
	}

	public static void resolveAwakeningChoice(ServerPlayer player, boolean accept) {
		if (player == null || !player.getPersistentData().getBoolean(AWAKENING_PENDING_TAG))
			return;
		if (player.serverLevel().dimension() != CARTENON_DIMENSION)
			return;
		if (!StoryModeIntroManager.canResolveAwakening(player)) {
			player.getPersistentData().remove(AWAKENING_PENDING_TAG);
			player.getPersistentData().remove(DECLINE_TICKS_TAG);
			restoreProtectionState(player);
			player.setHealth(Math.max(1.0F, player.getHealth()));
			sendAwakeningState(player, false);
			return;
		}

		player.getPersistentData().remove(AWAKENING_PENDING_TAG);
		CartenonProgressSavedData.get(player.serverLevel()).resolve(player.getUUID(), accept);
		StoryModeIntroManager.onAwakeningResolved(player, accept);
		sendAwakeningState(player, false);

		if (!accept) {
			player.getPersistentData().putInt(DECLINE_TICKS_TAG, DECLINE_DELAY_TICKS);
			freezePlayer(player);
			return;
		}

		player.getPersistentData().remove(DECLINE_TICKS_TAG);
		player.getPersistentData().remove(ENTRY_PROTECTION_TAG);
		restoreProtectionState(player);
		player.setHealth(player.getMaxHealth());
		player.fallDistance = 0.0F;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.Player = true;
			capability.syncPlayerVariables(player);
		});
		VesselProgressionManager.sync(player);

		ServerLevel overworld = player.server.overworld();
		BlockPos spawn = findSafeOverworldSpawn(overworld);
		player.stopRiding();
		player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
				overworld.getSharedSpawnAngle(), 0.0F);
		SystemNotifications.showTitleUnder(player, 0xFF3FC6FF, 140,
				Component.literal("WELCOME").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				Component.literal("You have become a Player.").withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingDamage(LivingDamageEvent.Pre event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.serverLevel().dimension() != CARTENON_DIMENSION)
			return;
		// Same reasoning as onLivingDeath: without this the return visit makes the
		// player effectively immortal, because every lethal hit is zeroed out and
		// converted into an awakening offer they already accepted long ago.
		if (player.getPersistentData().getBoolean(DEATH_BYPASS_TAG)
				|| SystemPlayerAccess.hasSystem(player)
				|| CartenonFinaleManager.isActive(player)
				|| CartenonProgressSavedData.get(player.serverLevel()).isResolved(player.getUUID()))
			return;
		if (player.getPersistentData().getBoolean(AWAKENING_PENDING_TAG)
				|| player.getPersistentData().getInt(DECLINE_TICKS_TAG) > 0) {
			event.setNewDamage(0.0F);
			return;
		}
		if (event.getNewDamage() + 0.001F < player.getHealth())
			return;
		event.setNewDamage(0.0F);
		if (!StoryModeIntroManager.canTriggerAwakening(player,
				event.getSource()))
			return;
		beginAwakeningChoice(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.serverLevel().dimension() != CARTENON_DIMENSION)
			return;
		// The whole first-visit script -- cancelling death, the awakening offer --
		// belongs to a player who does not have the System yet. On the return the
		// player already has it, so dying here is an ordinary death. Checking
		// hasSystem as well as isResolved matters because a player can hold the
		// System without ever having resolved a Cartenon visit.
		if (player.getPersistentData().getBoolean(DEATH_BYPASS_TAG)
				|| SystemPlayerAccess.hasSystem(player)
				|| CartenonFinaleManager.isActive(player)
				|| CartenonProgressSavedData.get(player.serverLevel()).isResolved(player.getUUID()))
			return;
		event.setCanceled(true);
		if (!StoryModeIntroManager.canTriggerAwakening(player,
				event.getSource())) {
			player.setHealth(Math.max(1.0F, player.getHealth()));
			return;
		}
		beginAwakeningChoice(player);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false || !(event.getEntity() instanceof ServerPlayer player))
			return;

		int declineTicks = player.getPersistentData().getInt(DECLINE_TICKS_TAG);
		if (declineTicks > 0) {
			freezePlayer(player);
			declineTicks--;
			if (declineTicks > 0) {
				player.getPersistentData().putInt(DECLINE_TICKS_TAG, declineTicks);
			} else {
				player.getPersistentData().remove(DECLINE_TICKS_TAG);
				restoreProtectionState(player);
				player.getPersistentData().putBoolean(DEATH_BYPASS_TAG, true);
				player.kill();
				SololevelingMod.queueServerWork(1,
						() -> player.getPersistentData().remove(DEATH_BYPASS_TAG));
			}
			return;
		}

		if (player.getPersistentData().getBoolean(AWAKENING_PENDING_TAG)) {
			freezePlayer(player);
			if (player.tickCount % 20 == 0)
				sendAwakeningState(player, true);
			return;
		}

		// Offered on a slow cadence rather than every tick: reaching the peak is a
		// permanent state, so this would otherwise re-check for the rest of the run.
		if (player.tickCount % 100 == 0)
			offerFinaleSummons(player);
		tickKneel(player);

		int protectionTicks = player.getPersistentData().getInt(ENTRY_PROTECTION_TAG);
		if (protectionTicks <= 0)
			return;
		freezePlayer(player);
		if (protectionTicks > 1) {
			player.getPersistentData().putInt(ENTRY_PROTECTION_TAG, protectionTicks - 1);
		} else {
			player.getPersistentData().remove(ENTRY_PROTECTION_TAG);
			restoreProtectionState(player);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (player.getPersistentData().getBoolean(AWAKENING_PENDING_TAG)) {
			captureProtectionState(player);
			freezePlayer(player);
			sendAwakeningState(player, true);
		}
	}

	private static void beginAwakeningChoice(ServerPlayer player) {
		if (player.getPersistentData().getBoolean(AWAKENING_PENDING_TAG))
			return;
		captureProtectionState(player);
		player.getPersistentData().putBoolean(AWAKENING_PENDING_TAG, true);
		player.getPersistentData().remove(ENTRY_PROTECTION_TAG);
		player.setHealth(Math.max(1.0F, player.getHealth()));
		freezePlayer(player);
		sendAwakeningState(player, true);
	}

	private static void sendAwakeningState(ServerPlayer player, boolean open) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new CartenonAwakeningStateMessage(open));
	}

	private static void teleportWaitingPlayers(MinecraftServer server, int instanceId) {
		Map<Integer, LinkedHashSet<UUID>> serverQueue = WAITING_PLAYERS.get(server);
		if (serverQueue == null)
			return;
		LinkedHashSet<UUID> waiting = serverQueue.remove(instanceId);
		if (waiting == null || waiting.isEmpty())
			return;
		if (serverQueue.isEmpty())
			WAITING_PLAYERS.remove(server);

		ServerLevel templeLevel = server.getLevel(CARTENON_DIMENSION);
		if (templeLevel == null)
			return;
		int index = 0;
		for (UUID playerId : waiting) {
			ServerPlayer player = server.getPlayerList().getPlayer(playerId);
			if (player == null)
				continue;
			teleportToTemple(player, templeLevel, instanceId, index++);
		}
	}

	private static void teleportToTemple(ServerPlayer player, ServerLevel templeLevel, int instanceId,
			int partyIndex) {
		BlockPos origin = instanceOrigin(instanceId);
		int lateralOffset = (partyIndex % 5 - 2) * 2;
		BlockPos entry = origin.relative(Direction.SOUTH, 8)
				.relative(Direction.SOUTH.getClockWise(), lateralOffset).above();
		templeLevel.getChunk(entry);
		DungeonDimensionPlayerLeavesDimensionProcedure.completeAlternateExit(player);
		CartenonProgressSavedData.get(templeLevel).associateInstance(player.getUUID(), instanceId);
		player.getPersistentData().putInt(INSTANCE_TAG, instanceId);
		player.getPersistentData().remove(AWAKENING_PENDING_TAG);
		player.getPersistentData().remove(DECLINE_TICKS_TAG);
		rememberReturnPoint(player);
		captureProtectionState(player);
		player.getPersistentData().putInt(ENTRY_PROTECTION_TAG, ENTRY_PROTECTION_TICKS);
		freezePlayer(player);
		player.stopRiding();
		player.teleportTo(templeLevel, entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D,
				0.0F, 0.0F);
		// A player who already has the System is here for the return, not the
		// awakening. The intro hooks and the arrival banner both belong to the
		// first visit only.
		if (CartenonProgressSavedData.get(templeLevel).isFinaleOffered(player.getUUID())
				&& SystemPlayerAccess.hasSystem(player)) {
			CartenonFinaleManager.begin(player, instanceId);
			return;
		}
		StoryModeIntroManager.onPlayerEnteredTemple(player, templeLevel, instanceId);
		SystemNotifications.showTitleUnder(player, 0xFF597EFF, 120,
				Component.literal("HIDDEN DUNGEON").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD),
				Component.literal("Cartenon Temple").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
	}

	private static List<ServerPlayer> dungeonParticipants(ServerPlayer killer, ServerLevel level,
			String dungeonTag) {
		SololevelingModVariables.PlayerVariables killerVars = killer.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		String party = killerVars.party == null ? "" : killerVars.party.trim();
		if (party.isEmpty())
			return List.of(killer);

		List<ServerPlayer> participants = new ArrayList<>();
		for (ServerPlayer candidate : level.players()) {
			SololevelingModVariables.PlayerVariables candidateVars = candidate.getCapability(
					SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.orElse(new SololevelingModVariables.PlayerVariables());
			if (party.equals(candidateVars.party)
					&& dungeonTag.equals(candidate.getPersistentData().getString(
							"dungeon_tag")))
				participants.add(candidate);
		}
		if (!participants.contains(killer))
			participants.add(killer);
		return participants;
	}

	/**
	 * The whole footprint of a temple instance.
	 *
	 * <p>The temple runs 154 blocks deep and its statues sit as far back as the
	 * dais, so anything hunting for them has to search the instance rather than a
	 * radius around the player, who arrives eight blocks inside the entrance.
	 */
	public static AABB instanceBounds(int instanceId) {
		BlockPos origin = instanceOrigin(instanceId);
		return AABB.encapsulatingFullBlocks(origin.offset(-96, -16, -8),
				origin.offset(96, 72, 168));
	}

	/**
	 * Sends a player home from the temple.
	 *
	 * <p>The Cartenon dimension has no return portal of its own -- the awakening
	 * teleports the player out as part of resolving, and anything else that puts
	 * someone in here has to do the same or they are stranded in a sealed
	 * instance with no exit.
	 */
	public static void returnToOverworld(ServerPlayer player) {
		if (player == null)
			return;
		player.stopRiding();
		player.fallDistance = 0.0F;

		// Back to where they walked in, not to the world spawn. Sending everyone
		// to spawn stranded anyone who had not slept in a bed yet -- which, this
		// early in the game, is most of them.
		ServerLevel origin = returnLevel(player);
		if (origin != null) {
			BlockPos remembered = returnPosition(player);
			if (remembered != null) {
				BlockPos safe = findSafeNear(origin, remembered);
				if (safe != null) {
					clearReturnPoint(player);
					player.teleportTo(origin, safe.getX() + 0.5D, safe.getY(),
							safe.getZ() + 0.5D, player.getYRot(), 0.0F);
					return;
				}
			}
		}

		// No remembered point: a save from before this was recorded, a dimension
		// that no longer loads, or ground that is no longer safe. World spawn is
		// the old behaviour and still better than the temple.
		clearReturnPoint(player);
		ServerLevel overworld = player.server.overworld();
		BlockPos spawn = findSafeOverworldSpawn(overworld);
		player.teleportTo(overworld, spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D,
				overworld.getSharedSpawnAngle(), 0.0F);
	}

	private static void rememberReturnPoint(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.putString(RETURN_DIM_TAG, player.level().dimension().location().toString());
		data.putInt(RETURN_X_TAG, player.blockPosition().getX());
		data.putInt(RETURN_Y_TAG, player.blockPosition().getY());
		data.putInt(RETURN_Z_TAG, player.blockPosition().getZ());
	}

	private static void clearReturnPoint(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.remove(RETURN_DIM_TAG);
		data.remove(RETURN_X_TAG);
		data.remove(RETURN_Y_TAG);
		data.remove(RETURN_Z_TAG);
	}

	private static ServerLevel returnLevel(ServerPlayer player) {
		String id = player.getPersistentData().getString(RETURN_DIM_TAG);
		if (id.isEmpty())
			return null;
		ResourceLocation parsed = ResourceLocation.tryParse(id);
		if (parsed == null)
			return null;
		ServerLevel level = player.server.getLevel(
				ResourceKey.create(Registries.DIMENSION, parsed));
		// Never back into the temple, whatever the tag says.
		return level == null || level.dimension().equals(player.level().dimension())
				? null : level;
	}

	private static BlockPos returnPosition(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		if (!data.contains(RETURN_X_TAG))
			return null;
		return new BlockPos(data.getInt(RETURN_X_TAG), data.getInt(RETURN_Y_TAG),
				data.getInt(RETURN_Z_TAG));
	}

	/** The remembered spot, or the nearest standable one to it. */
	private static BlockPos findSafeNear(ServerLevel level, BlockPos wanted) {
		level.getChunk(wanted);
		for (int radius = 0; radius <= 6; radius++)
			for (int dx = -radius; dx <= radius; dx++)
				for (int dz = -radius; dz <= radius; dz++) {
					if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius)
						continue;
					for (int dy = 4; dy >= -6; dy--) {
						BlockPos candidate = wanted.offset(dx, dy, dz);
						if (isSafePlayerSpace(level, candidate))
							return candidate;
					}
				}
		return null;
	}

	private static BlockPos instanceOrigin(int instanceId) {
		int zeroBased = Math.max(0, instanceId - 1);
		int column = zeroBased % INSTANCE_COLUMNS;
		int row = zeroBased / INSTANCE_COLUMNS;
		return new BlockPos(column * INSTANCE_SPACING, TEMPLE_FLOOR_Y, row * INSTANCE_SPACING);
	}

	private static BlockPos findGatePosition(ServerLevel level, BlockPos center) {
		int[][] offsets = {
				{3, 0}, {-3, 0}, {0, 3}, {0, -3}, {4, 4}, {-4, 4}, {4, -4}, {-4, -4}, {0, 0}
		};
		for (int[] offset : offsets) {
			for (int dy = 3; dy >= -3; dy--) {
				BlockPos candidate = center.offset(offset[0], dy, offset[1]);
				if (isGateSpace(level, candidate))
					return candidate;
			}
		}
		return center.above();
	}

	private static boolean isGateSpace(ServerLevel level, BlockPos pos) {
		return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
				&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
				&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
				&& level.getBlockState(pos.above(2)).getCollisionShape(level, pos.above(2)).isEmpty();
	}

	private static BlockPos findSafeOverworldSpawn(ServerLevel overworld) {
		BlockPos shared = overworld.getSharedSpawnPos();
		for (int radius = 0; radius <= 8; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (radius > 0 && Math.abs(dx) != radius && Math.abs(dz) != radius)
						continue;
					for (int dy = 6; dy >= -4; dy--) {
						BlockPos candidate = shared.offset(dx, dy, dz);
						if (isSafePlayerSpace(overworld, candidate))
							return candidate;
					}
				}
			}
		}
		return shared.above();
	}

	private static boolean isSafePlayerSpace(ServerLevel level, BlockPos pos) {
		BlockState below = level.getBlockState(pos.below());
		return below.isFaceSturdy(level, pos.below(), Direction.UP)
				&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
				&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
	}

	private static void queuePlayer(MinecraftServer server, int instanceId, UUID playerId) {
		WAITING_PLAYERS.computeIfAbsent(server, ignored -> new java.util.LinkedHashMap<>())
				.computeIfAbsent(instanceId, ignored -> new LinkedHashSet<>()).add(playerId);
	}

	private static void removeWaitingPlayer(MinecraftServer server, int instanceId, UUID playerId) {
		Map<Integer, LinkedHashSet<UUID>> serverQueue = WAITING_PLAYERS.get(server);
		if (serverQueue == null)
			return;
		Collection<UUID> waiting = serverQueue.get(instanceId);
		if (waiting != null) {
			waiting.remove(playerId);
			if (waiting.isEmpty())
				serverQueue.remove(instanceId);
		}
		if (serverQueue.isEmpty())
			WAITING_PLAYERS.remove(server);
	}

	private static void captureProtectionState(ServerPlayer player) {
		if (player.getPersistentData().getBoolean(PROTECTION_STATE_SAVED_TAG))
			return;
		player.getPersistentData().putBoolean(PROTECTION_STATE_SAVED_TAG, true);
		player.getPersistentData().putBoolean(PREVIOUS_INVULNERABLE_TAG, player.isInvulnerable());
		player.getPersistentData().putBoolean(PREVIOUS_NO_GRAVITY_TAG, player.isNoGravity());
	}

	private static void restoreProtectionState(ServerPlayer player) {
		if (player.getPersistentData().getBoolean(PROTECTION_STATE_SAVED_TAG)) {
			player.setInvulnerable(player.getPersistentData().getBoolean(PREVIOUS_INVULNERABLE_TAG));
			player.setNoGravity(player.getPersistentData().getBoolean(PREVIOUS_NO_GRAVITY_TAG));
		}
		player.getPersistentData().remove(PROTECTION_STATE_SAVED_TAG);
		player.getPersistentData().remove(PREVIOUS_INVULNERABLE_TAG);
		player.getPersistentData().remove(PREVIOUS_NO_GRAVITY_TAG);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0F;
	}

	private static void freezePlayer(ServerPlayer player) {
		captureProtectionState(player);
		player.setInvulnerable(true);
		player.setNoGravity(true);
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0F;
	}
}
