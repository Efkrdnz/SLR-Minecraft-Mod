package net.solocraft.util.daily;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemNotifications;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * Server-authoritative travel and timing for the failed-Daily punishment.
 *
 * <p>The destination chunk is generated synchronously before travel and the
 * requested height is accepted only when it has a sturdy floor plus two clear
 * body blocks. Empty or malformed terrain receives a small red-sandstone
 * emergency platform. A per-tick void check then recovers players if terrain
 * generation, another mod, or a broken old save still leaves them below the
 * dimension floor.</p>
 */
public final class DailyPunishmentManager {
	public static final double PUNISHMENT_SECONDS = 120.0D;
	public static final ResourceKey<Level> SURVIVAL_DIMENSION =
			ResourceKey.create(Registries.DIMENSION,
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
							"survival_dimension"));

	private static final String RETURN_X = "punX";
	private static final String RETURN_Y = "punY";
	private static final String RETURN_Z = "punZ";
	private static final String RETURN_YAW = "slrPunYaw";
	private static final String RETURN_PITCH = "slrPunPitch";
	private static final String RETURN_DIMENSION = "slrPunDimension";
	private static final String ANCHOR_X = "slrPunSafeX";
	private static final String ANCHOR_Y = "slrPunSafeY";
	private static final String ANCHOR_Z = "slrPunSafeZ";

	private static final int SURFACE_SEARCH_RADIUS = 12;
	private static final int EMERGENCY_PLATFORM_RADIUS = 4;
	private static final int VOID_RECOVERY_CLEARANCE = 4;

	private DailyPunishmentManager() {
	}

	/** Prepares a verified landing before starting the timer and changing level. */
	public static boolean enter(ServerPlayer player) {
		if (player == null || player.server == null)
			return false;
		ServerLevel punishmentLevel = player.server.getLevel(SURVIVAL_DIMENSION);
		if (punishmentLevel == null)
			return false;

		int targetX = Mth.floor(player.getX());
		int targetZ = Mth.floor(player.getZ());
		BlockPos arrival = prepareSafeArrival(punishmentLevel, targetX,
				targetZ);
		rememberReturnPoint(player);
		rememberAnchor(player, arrival);
		setPunishmentState(player, PUNISHMENT_SECONDS, false);
		teleport(player, punishmentLevel, arrival.getX() + 0.5D,
				arrival.getY(), arrival.getZ() + 0.5D, player.getYRot(),
				player.getXRot());

		SystemNotifications.showNegativeTitleUnder(player, 0xFFFF3D3D, 100,
				Component.literal("PENALTY QUEST")
						.withStyle(ChatFormatting.DARK_RED,
								ChatFormatting.BOLD),
				Component.literal("Survive for 120 seconds.")
						.withStyle(ChatFormatting.RED));
		return player.level().dimension() == SURVIVAL_DIMENSION;
	}

	/** Called on every server player tick by the legacy procedure event hook. */
	public static void tick(ServerPlayer player) {
		if (player == null || player.level().dimension() != SURVIVAL_DIMENSION)
			return;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		if (variables == null)
			return;

		if (variables.punishment > PUNISHMENT_SECONDS) {
			setPunishmentState(player, PUNISHMENT_SECONDS, variables.giftstatus);
			variables = variables(player);
			if (variables == null)
				return;
		}

		if (variables.punishment > 0.0D)
			rescueFromVoid(player);
		if (player.serverLevel().getGameTime() % 20L != 0L)
			return;

		if (variables.punishment <= 0.0D) {
			finish(player);
			return;
		}

		double remaining = Math.max(0.0D, variables.punishment - 1.0D);
		setPunishmentState(player, remaining, variables.giftstatus);
		int wholeSeconds = (int) Math.ceil(remaining);
		if (wholeSeconds == 110 || wholeSeconds == 90
				|| wholeSeconds == 70)
			spawnCentipede(player);
		if (remaining <= 0.0D)
			finish(player);
	}

	/** Returns true when an invalid below-bedrock position was recovered. */
	public static boolean rescueFromVoid(ServerPlayer player) {
		if (player == null || player.level().dimension() != SURVIVAL_DIMENSION)
			return false;
		ServerLevel level = player.serverLevel();
		double rescueY = level.getMinBuildHeight()
				+ VOID_RECOVERY_CLEARANCE;
		if (Double.isFinite(player.getX()) && Double.isFinite(player.getY())
				&& Double.isFinite(player.getZ()) && player.getY() >= rescueY)
			return false;

		BlockPos anchor = readAnchor(player);
		if (anchor == null || !isSafeStandingPosition(level, anchor)) {
			int targetX = anchor == null ? Mth.floor(player.getX())
					: anchor.getX();
			int targetZ = anchor == null ? Mth.floor(player.getZ())
					: anchor.getZ();
			if (!Double.isFinite(player.getX())
					|| !Double.isFinite(player.getZ())) {
				targetX = 0;
				targetZ = 0;
			}
			anchor = prepareSafeArrival(level, targetX, targetZ);
			rememberAnchor(player, anchor);
		}

		teleport(player, level, anchor.getX() + 0.5D, anchor.getY(),
				anchor.getZ() + 0.5D, player.getYRot(), player.getXRot());
		player.displayClientMessage(Component.literal(
				"The System restored you to the punishment arena.")
				.withStyle(ChatFormatting.RED), true);
		return true;
	}

	private static BlockPos prepareSafeArrival(ServerLevel level, int targetX,
			int targetZ) {
		// ServerLevel#getChunk is synchronous here: heightmaps and platform writes
		// must never run against an ungenerated destination chunk.
		level.getChunk(targetX >> 4, targetZ >> 4);
		BlockPos exact = safeSurfaceAt(level, targetX, targetZ);
		if (exact != null)
			return exact;

		for (int radius = 1; radius <= SURFACE_SEARCH_RADIUS; radius++) {
			for (int offset = -radius; offset <= radius; offset++) {
				BlockPos candidate = safeSurfaceAt(level,
						targetX + offset, targetZ - radius);
				if (candidate != null)
					return candidate;
				candidate = safeSurfaceAt(level, targetX + offset,
						targetZ + radius);
				if (candidate != null)
					return candidate;
			}
			for (int offset = -radius + 1; offset < radius; offset++) {
				BlockPos candidate = safeSurfaceAt(level, targetX - radius,
						targetZ + offset);
				if (candidate != null)
					return candidate;
				candidate = safeSurfaceAt(level, targetX + radius,
						targetZ + offset);
				if (candidate != null)
					return candidate;
			}
		}
		return createEmergencyPlatform(level, targetX, targetZ);
	}

	@Nullable
	private static BlockPos safeSurfaceAt(ServerLevel level, int x, int z) {
		int minimum = level.getMinBuildHeight() + 1;
		int maximum = level.getMaxBuildHeight() - 2;
		int surface = level.getHeight(
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		if (surface < minimum || surface > maximum)
			return null;

		for (int offset = 0; offset <= 4; offset++) {
			BlockPos candidate = new BlockPos(x, surface + offset, z);
			if (candidate.getY() <= maximum
					&& isSafeStandingPosition(level, candidate))
				return candidate;
		}
		for (int offset = 1; offset <= 12; offset++) {
			BlockPos candidate = new BlockPos(x, surface - offset, z);
			if (candidate.getY() >= minimum
					&& isSafeStandingPosition(level, candidate))
				return candidate;
		}
		return null;
	}

	private static boolean isSafeStandingPosition(ServerLevel level,
			BlockPos feet) {
		if (feet.getY() <= level.getMinBuildHeight()
				|| feet.getY() >= level.getMaxBuildHeight() - 1)
			return false;
		BlockPos floorPos = feet.below();
		BlockState floor = level.getBlockState(floorPos);
		return floor.isFaceSturdy(level, floorPos, Direction.UP)
				&& canOccupy(level, feet)
				&& canOccupy(level, feet.above());
	}

	private static boolean canOccupy(ServerLevel level, BlockPos position) {
		BlockState state = level.getBlockState(position);
		return state.getCollisionShape(level, position).isEmpty()
				&& state.getFluidState().isEmpty()
				&& !state.is(Blocks.POWDER_SNOW)
				&& !state.is(Blocks.FIRE)
				&& !state.is(Blocks.SOUL_FIRE);
	}

	private static BlockPos createEmergencyPlatform(ServerLevel level, int x,
			int z) {
		int minimumFloor = level.getMinBuildHeight() + 8;
		int maximumFloor = level.getMaxBuildHeight() - 4;
		int surface = level.getHeight(
				Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
		int preferredFloor = surface > level.getMinBuildHeight() + 1
				? surface - 1 : Math.max(64, level.getSeaLevel());
		int floorY = Mth.clamp(preferredFloor, minimumFloor, maximumFloor);

		for (int dx = -EMERGENCY_PLATFORM_RADIUS;
				dx <= EMERGENCY_PLATFORM_RADIUS; dx++) {
			for (int dz = -EMERGENCY_PLATFORM_RADIUS;
					dz <= EMERGENCY_PLATFORM_RADIUS; dz++) {
				level.setBlock(new BlockPos(x + dx, floorY, z + dz),
						Blocks.RED_SANDSTONE.defaultBlockState(),
						Block.UPDATE_ALL);
			}
		}
		for (int dx = -1; dx <= 1; dx++) {
			for (int dz = -1; dz <= 1; dz++) {
				for (int dy = 1; dy <= 3; dy++)
					level.setBlock(new BlockPos(x + dx, floorY + dy,
							z + dz), Blocks.AIR.defaultBlockState(),
							Block.UPDATE_ALL);
			}
		}
		return new BlockPos(x, floorY + 1, z);
	}

	private static void spawnCentipede(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		BlockPos position = player.blockPosition().offset(3, 0, 3);
		Entity spawned = SololevelingModEntities.CENTIPEDE.get().spawn(level,
				position, MobSpawnType.MOB_SUMMONED);
		if (spawned != null)
			spawned.fallDistance = 0.0F;
	}

	private static void finish(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		ServerLevel destination = returnLevel(player, data);
		double x;
		double y;
		double z;
		if (hasReturnCoordinates(data)) {
			x = data.getDouble(RETURN_X);
			y = data.getDouble(RETURN_Y);
			z = data.getDouble(RETURN_Z);
		} else {
			BlockPos spawn = destination.getSharedSpawnPos();
			x = spawn.getX() + 0.5D;
			y = destination.getHeight(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					spawn.getX(), spawn.getZ());
			z = spawn.getZ() + 0.5D;
		}
		if (!Double.isFinite(x) || !Double.isFinite(y)
				|| !Double.isFinite(z)) {
			BlockPos spawn = destination.getSharedSpawnPos();
			x = spawn.getX() + 0.5D;
			y = destination.getHeight(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					spawn.getX(), spawn.getZ());
			z = spawn.getZ() + 0.5D;
		}
		y = Mth.clamp(y, destination.getMinBuildHeight() + 1.0D,
				destination.getMaxBuildHeight() - 2.0D);
		destination.getChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4);
		float yaw = data.contains(RETURN_YAW, Tag.TAG_FLOAT)
				? data.getFloat(RETURN_YAW) : player.getYRot();
		float pitch = data.contains(RETURN_PITCH, Tag.TAG_FLOAT)
				? data.getFloat(RETURN_PITCH) : player.getXRot();
		teleport(player, destination, x, y, z, yaw, pitch);
		setPunishmentState(player, 0.0D, true);
		clearTravelData(data);
		SystemNotifications.showTitleUnder(player, 0xFF64FF8A, 80,
				Component.literal("SURVIVED!")
						.withStyle(ChatFormatting.GREEN,
								ChatFormatting.BOLD),
				Component.literal("Punishment complete.")
						.withStyle(ChatFormatting.GRAY));
	}

	private static ServerLevel returnLevel(ServerPlayer player,
			CompoundTag data) {
		if (data.contains(RETURN_DIMENSION, Tag.TAG_STRING)) {
			ResourceLocation location = ResourceLocation.tryParse(
					data.getString(RETURN_DIMENSION));
			if (location != null) {
				ServerLevel stored = player.server.getLevel(ResourceKey.create(
						Registries.DIMENSION, location));
				if (stored != null && stored.dimension() != SURVIVAL_DIMENSION)
					return stored;
			}
		}
		return player.server.overworld();
	}

	private static void rememberReturnPoint(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.putDouble(RETURN_X, player.getX());
		data.putDouble(RETURN_Y, player.getY());
		data.putDouble(RETURN_Z, player.getZ());
		data.putFloat(RETURN_YAW, player.getYRot());
		data.putFloat(RETURN_PITCH, player.getXRot());
		data.putString(RETURN_DIMENSION,
				player.level().dimension().location().toString());
	}

	private static boolean hasReturnCoordinates(CompoundTag data) {
		return data.contains(RETURN_X, Tag.TAG_DOUBLE)
				&& data.contains(RETURN_Y, Tag.TAG_DOUBLE)
				&& data.contains(RETURN_Z, Tag.TAG_DOUBLE);
	}

	private static void rememberAnchor(ServerPlayer player, BlockPos anchor) {
		CompoundTag data = player.getPersistentData();
		data.putInt(ANCHOR_X, anchor.getX());
		data.putInt(ANCHOR_Y, anchor.getY());
		data.putInt(ANCHOR_Z, anchor.getZ());
	}

	@Nullable
	private static BlockPos readAnchor(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		if (!data.contains(ANCHOR_X, Tag.TAG_INT)
				|| !data.contains(ANCHOR_Y, Tag.TAG_INT)
				|| !data.contains(ANCHOR_Z, Tag.TAG_INT))
			return null;
		return new BlockPos(data.getInt(ANCHOR_X), data.getInt(ANCHOR_Y),
				data.getInt(ANCHOR_Z));
	}

	private static void clearTravelData(CompoundTag data) {
		for (String key : new String[] {RETURN_X, RETURN_Y, RETURN_Z,
				RETURN_YAW, RETURN_PITCH, RETURN_DIMENSION, ANCHOR_X,
				ANCHOR_Y, ANCHOR_Z})
			data.remove(key);
	}

	private static void setPunishmentState(ServerPlayer player,
			double remaining, boolean giftStatus) {
		player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.punishment = remaining;
					capability.giftstatus = giftStatus;
					capability.syncPlayerVariables(player);
				});
	}

	private static void teleport(ServerPlayer player, ServerLevel destination,
			double x, double y, double z, float yaw, float pitch) {
		boolean dimensionChange = player.serverLevel() != destination;
		player.stopRiding();
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0F;
		if (dimensionChange) {
			player.connection.send(new ClientboundGameEventPacket(
					ClientboundGameEventPacket.WIN_GAME, 0));
			player.teleportTo(destination, x, y, z, yaw, pitch);
			player.connection.send(new ClientboundPlayerAbilitiesPacket(
					player.getAbilities()));
			for (MobEffectInstance effect : player.getActiveEffects())
				player.connection.send(new ClientboundUpdateMobEffectPacket(
						player.getId(), effect, false));
			player.connection.send(new ClientboundLevelEventPacket(1032,
					BlockPos.ZERO, 0, false));
		} else {
			player.connection.teleport(x, y, z, yaw, pitch);
		}
		player.setDeltaMovement(Vec3.ZERO);
		player.fallDistance = 0.0F;
		player.setOnGround(true);
	}

	@Nullable
	private static SololevelingModVariables.PlayerVariables variables(
			ServerPlayer player) {
		return player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
