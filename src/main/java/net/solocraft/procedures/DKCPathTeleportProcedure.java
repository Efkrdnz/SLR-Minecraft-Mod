package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcRunSavedData;
import net.solocraft.dkc.DkcSpatialLayout;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class DKCPathTeleportProcedure {
	private static final String RETURN_DIMENSION = "dkc_return_dimension";
	private static final String RETURN_YAW = "dkc_return_yaw";
	private static final String RETURN_PITCH = "dkc_return_pitch";
	private static final TagKey<net.minecraft.world.entity.EntityType<?>> SHADOWS_TAG =
			TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows"));

	public static boolean isFloorAvailable(Entity entity, int floor) {
		if (entity == null || floor < 1 || floor > DkcFloorRegistry.LAST_FLOOR)
			return false;
		SololevelingModVariables.PlayerVariables vars = entity
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		if (entity instanceof ServerPlayer player && player.server != null)
			return (floor != 1 || vars.dkc_started || vars.dkc_cleared > 0)
					&& DkcRunSavedData.get(player.server).isUnlocked(player, floor);
		return floor == 1 ? vars.dkc_started || vars.dkc_cleared > 0 : vars.dkc_cleared >= floor - 1;
	}

	public static void execute(Player entity, int floor) {
		if (!(entity instanceof ServerPlayer player) || player.server == null)
			return;
		boolean alreadyInDkc = DkcFloorRegistry.isDkc(player.level());
		if (!canTravelToFloor(player, floor, true))
			return;
		PointSetProcedure.execute(player);
		if (!alreadyInDkc) {
			saveReturnPosition(player);
			discardOwnedShadows(player);
		}
		DkcFloorBuilder.teleportToFloor(player, floor);
	}

	/**
	 * Complete, side-effect-free path preflight shared by the menu packet and
	 * the actual teleport. Live progression is deliberately checked here rather
	 * than trusting the menu's opening snapshot.
	 */
	public static boolean canTravelToFloor(ServerPlayer player, int floor, boolean sendFeedback) {
		if (player == null || player.server == null
				|| floor < DkcFloorRegistry.FIRST_FLOOR
				|| floor > DkcFloorRegistry.LAST_FLOOR)
			return false;
		boolean alreadyInDkc = DkcFloorRegistry.isDkc(player.level());
		if (!alreadyInDkc && !player.level().dimension().equals(Level.OVERWORLD)) {
			if (sendFeedback)
				player.displayClientMessage(Component.literal(
						"\u00A74The Demon King's Castle can only be entered from the Overworld."), true);
			return false;
		}
		if (alreadyInDkc && DkcSpatialLayout.floor(player) == floor) {
			if (sendFeedback)
				player.displayClientMessage(Component.literal("\u00A74You are already on that floor."), true);
			return false;
		}
		if (!DKCCombatTrackerProcedure.canEnterCastle(player)) {
			if (sendFeedback)
				DKCCombatTrackerProcedure.sendCombatBlockedMessage(player);
			return false;
		}
		if (!isFloorAvailable(player, floor)) {
			if (sendFeedback)
				player.displayClientMessage(Component.literal("\u00A74That floor is still sealed."), true);
			return false;
		}
		SololevelingModVariables.PlayerVariables vars = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		if (vars.dkc_cleared >= DkcFloorRegistry.LAST_FLOOR) {
			if (sendFeedback)
				player.displayClientMessage(Component.literal(
						"\u00A75The Demon King's Castle is already conquered. No path remains."), true);
			return false;
		}
		return true;
	}

	/** Permanent post-conquest route granted by House Radiru's Floor 15 pact. */
	public static void enterRadiruCastle(ServerPlayer player) {
		if (player == null || player.server == null
				|| !net.solocraft.util.DkcQuestManager.hasRadiruCastleAccess(player))
			return;
		if (!player.level().dimension().equals(Level.OVERWORLD)) {
			player.displayClientMessage(Component.literal("\u00A74Radiru Castle can only be entered from the Overworld."), true);
			return;
		}
		if (!DKCCombatTrackerProcedure.canEnterCastle(player)) {
			DKCCombatTrackerProcedure.sendCombatBlockedMessage(player);
			return;
		}
		PointSetProcedure.execute(player);
		saveReturnPosition(player);
		discardOwnedShadows(player);
		DkcFloorBuilder.teleportToFloor(player, 15);
	}

	public static void returnToSavedOverworld(ServerPlayer player) {
		if (player == null || player.server == null)
			return;
		ServerLevel destination = resolveReturnLevel(player);
		if (destination == null) {
			player.displayClientMessage(Component.literal("\u00A74The way back from the castle is lost."), true);
			return;
		}
		SololevelingModVariables.PlayerVariables vars = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		double targetX = vars.DunX;
		double targetY = vars.DunY;
		double targetZ = vars.DunZ;
		if (targetX == 0.0D && targetY == 0.0D && targetZ == 0.0D) {
			BlockPos spawn = destination.getSharedSpawnPos();
			targetX = spawn.getX() + 0.5D;
			targetY = spawn.getY() + 1.0D;
			targetZ = spawn.getZ() + 0.5D;
		}
		float yaw = player.getPersistentData().contains(RETURN_YAW)
				? player.getPersistentData().getFloat(RETURN_YAW) : player.getYRot();
		float pitch = player.getPersistentData().contains(RETURN_PITCH)
				? player.getPersistentData().getFloat(RETURN_PITCH) : player.getXRot();
		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
		player.fallDistance = 0.0F;
		destination.getChunk(BlockPos.containing(targetX, targetY, targetZ));
		player.teleportTo(destination, targetX, targetY, targetZ, yaw, pitch);
		player.getPersistentData().putBoolean(DkcSpatialLayout.ACTIVE_RUN_TAG, false);
		destination.playSound(null, BlockPos.containing(targetX, targetY, targetZ),
				SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.45F, 1.15F);
		player.displayClientMessage(Component.literal("\u00A75The key drags you back to where the castle found you."), true);
	}

	private static ServerLevel resolveReturnLevel(ServerPlayer player) {
		String stored = player.getPersistentData().getString(RETURN_DIMENSION);
		ResourceLocation id = ResourceLocation.tryParse(stored);
		if (id != null) {
			ServerLevel level = player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
			if (level != null && !DkcFloorRegistry.isDkc(level))
				return level;
		}
		return player.server.getLevel(Level.OVERWORLD);
	}

	private static void saveReturnPosition(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = player.getX();
			capability.DunY = player.getY();
			capability.DunZ = player.getZ();
			capability.syncPlayerVariables(player);
		});
		player.getPersistentData().putString(RETURN_DIMENSION, player.level().dimension().location().toString());
		player.getPersistentData().putFloat(RETURN_YAW, player.getYRot());
		player.getPersistentData().putFloat(RETURN_PITCH, player.getXRot());
	}

	/** Lets the operator-only floor command preserve a safe exit from any non-DKC level. */
	public static void saveReturnPositionForDebug(ServerPlayer player) {
		if (player != null && !DkcFloorRegistry.isDkc(player.level()))
			saveReturnPosition(player);
	}

	private static void discardOwnedShadows(ServerPlayer player) {
		Vec3 center = player.position();
		for (Entity foundEntity : player.level().getEntitiesOfClass(Entity.class,
				new AABB(center, center).inflate(250))) {
			if (foundEntity.getType().is(SHADOWS_TAG) && foundEntity instanceof TamableAnimal tamable
					&& tamable.isOwnedBy(player))
				foundEntity.discard();
		}
	}
}
