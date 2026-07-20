package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.CartenonGateEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.CartenonAwakeningStateMessage;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class CartenonTempleManager {
	public static final ResourceKey<Level> CARTENON_DIMENSION = ResourceKey.create(Registries.DIMENSION,
			new ResourceLocation(SololevelingMod.MODID, "cartenon_temple"));

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

	private static final String INSTANCE_TAG = "slr_cartenon_instance";
	private static final String ENTRY_PROTECTION_TAG = "slr_cartenon_entry_protection";
	private static final String AWAKENING_PENDING_TAG = "slr_cartenon_awakening_pending";
	private static final String DECLINE_TICKS_TAG = "slr_cartenon_decline_ticks";
	private static final String DEATH_BYPASS_TAG = "slr_cartenon_death_bypass";
	private static final String PREVIOUS_INVULNERABLE_TAG = "slr_cartenon_previous_invulnerable";
	private static final String PREVIOUS_NO_GRAVITY_TAG = "slr_cartenon_previous_no_gravity";
	private static final String PROTECTION_STATE_SAVED_TAG = "slr_cartenon_protection_state_saved";

	private CartenonTempleManager() {
	}

	/** Called once from the common boss-credit path after a normal gate boss dies. */
	public static void onDungeonBossDefeated(LevelAccessor world, Entity boss, Entity creditedSource,
			String dungeonTag) {
		if (!(world instanceof ServerLevel level) || !(creditedSource instanceof ServerPlayer killer)
				|| boss == null || dungeonTag == null || dungeonTag.isBlank())
			return;
		if (!ELIGIBLE_DUNGEONS.contains(level.dimension().location().getPath()))
			return;

		List<ServerPlayer> participants = dungeonParticipants(killer, level).stream()
				.filter(participant -> !SystemPlayerAccess.hasSystem(participant))
				.toList();
		if (participants.isEmpty())
			return;
		CartenonProgressSavedData progressData = CartenonProgressSavedData.get(level);
		List<ServerPlayer> invitationOwners = new ArrayList<>();
		for (ServerPlayer participant : participants) {
			if (!progressData.isResolved(participant.getUUID())
					&& progressData.recordDungeonClear(participant.getUUID(), dungeonTag))
				invitationOwners.add(participant);
		}
		if (invitationOwners.isEmpty())
			return;

		int instanceId = progressData.allocateInstance();
		for (ServerPlayer owner : invitationOwners)
			progressData.markGateOffered(owner.getUUID(), instanceId);

		CartenonGateEntity gate = SololevelingModEntities.CARTENON_GATE.get().create(level);
		if (gate == null) {
			for (ServerPlayer owner : invitationOwners)
				progressData.cancelGateOffer(owner.getUUID());
			return;
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
			return;
		}

		level.playSound(null, gatePos, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.1F, 1.32F);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL,
				gate.getX(), gate.getY() + 1.2D, gate.getZ(), 90, 0.9D, 1.2D, 0.35D, 0.08D);
		for (ServerPlayer participant : participants) {
			SystemNotifications.showTitleUnder(participant, 0xFF4A84FF, 110,
					Component.literal("HIDDEN DUNGEON").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
					Component.literal("A strange gate has appeared.").withStyle(ChatFormatting.DARK_PURPLE));
		}
	}

	public static void enterGate(ServerPlayer player, CartenonGateEntity gate) {
		if (player == null || gate == null || gate.isRemoved() || SystemPlayerAccess.hasSystem(player))
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

	public static void resolveAwakeningChoice(ServerPlayer player, boolean accept) {
		if (player == null || !player.getPersistentData().getBoolean(AWAKENING_PENDING_TAG))
			return;
		if (player.serverLevel().dimension() != CARTENON_DIMENSION)
			return;

		player.getPersistentData().remove(AWAKENING_PENDING_TAG);
		CartenonProgressSavedData.get(player.serverLevel()).resolve(player.getUUID(), accept);
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
	public static void onLivingDamage(LivingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.serverLevel().dimension() != CARTENON_DIMENSION)
			return;
		if (player.getPersistentData().getBoolean(DEATH_BYPASS_TAG)
				|| CartenonProgressSavedData.get(player.serverLevel()).isResolved(player.getUUID()))
			return;
		if (player.getPersistentData().getBoolean(AWAKENING_PENDING_TAG)
				|| player.getPersistentData().getInt(DECLINE_TICKS_TAG) > 0) {
			event.setCanceled(true);
			return;
		}
		if (event.getAmount() + 0.001F < player.getHealth())
			return;
		event.setCanceled(true);
		beginAwakeningChoice(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingDeath(LivingDeathEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player) || player.serverLevel().dimension() != CARTENON_DIMENSION)
			return;
		if (player.getPersistentData().getBoolean(DEATH_BYPASS_TAG)
				|| CartenonProgressSavedData.get(player.serverLevel()).isResolved(player.getUUID()))
			return;
		event.setCanceled(true);
		beginAwakeningChoice(player);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player))
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
		CartenonProgressSavedData.get(templeLevel).associateInstance(player.getUUID(), instanceId);
		player.getPersistentData().putInt(INSTANCE_TAG, instanceId);
		player.getPersistentData().remove(AWAKENING_PENDING_TAG);
		player.getPersistentData().remove(DECLINE_TICKS_TAG);
		captureProtectionState(player);
		player.getPersistentData().putInt(ENTRY_PROTECTION_TAG, ENTRY_PROTECTION_TICKS);
		freezePlayer(player);
		player.stopRiding();
		player.teleportTo(templeLevel, entry.getX() + 0.5D, entry.getY(), entry.getZ() + 0.5D,
				0.0F, 0.0F);
		SystemNotifications.showTitleUnder(player, 0xFF597EFF, 120,
				Component.literal("HIDDEN DUNGEON").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD),
				Component.literal("Cartenon Temple").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD));
	}

	private static List<ServerPlayer> dungeonParticipants(ServerPlayer killer, ServerLevel level) {
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
			if (party.equals(candidateVars.party))
				participants.add(candidate);
		}
		if (!participants.contains(killer))
			participants.add(killer);
		return participants;
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
