package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.SololevelingMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;

public class DKCPathTeleportProcedure {
	private static final ResourceKey<Level> DKC_DIMENSION = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling", "dungeon_dimension_dkc"));
	private static final TagKey<net.minecraft.world.entity.EntityType<?>> SHADOWS_TAG = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows"));

	public static boolean isFloorAvailable(Entity entity, int floor) {
		if (entity == null || floor < 1 || floor > 20)
			return false;
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (floor == 1)
			return vars.dkc_started || vars.dkc_cleared > 0;
		return vars.dkc_cleared >= floor - 1;
	}

	public static void execute(Player entity, int floor) {
		if (!(entity instanceof ServerPlayer player) || player.server == null)
			return;
		boolean alreadyInDkc = player.level().dimension().equals(DKC_DIMENSION);
		if (!alreadyInDkc) {
			if (!player.level().dimension().equals(Level.OVERWORLD)) {
				player.displayClientMessage(Component.literal("\u00A74The Demon King's Castle can only be entered from the Overworld."), true);
				return;
			}
			if (!DKCCombatTrackerProcedure.canEnterCastle(player)) {
				DKCCombatTrackerProcedure.sendCombatBlockedMessage(player);
				return;
			}
		}
		if (!isFloorAvailable(player, floor)) {
			player.displayClientMessage(Component.literal("\u00A74That floor is still sealed."), true);
			return;
		}
		PointSetProcedure.execute(player);
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (vars.dkc_cleared >= 20) {
			player.displayClientMessage(Component.literal("\u00A75The Demon King's Castle is already conquered. No path remains."), true);
			return;
		}
		ServerLevel dkcLevel = player.server.getLevel(DKC_DIMENSION);
		if (dkcLevel == null) {
			player.displayClientMessage(Component.literal("\u00A74The Demon King's Castle cannot be reached."), true);
			return;
		}
		if (player.level().dimension() != DKC_DIMENSION) {
			saveReturnPosition(player);
			discardOwnedShadows(player);
		}
		double targetX = vars.dkc_x + 100;
		double targetY = vars.dkc_y + 4;
		double targetZ = vars.dkc_z + ((floor - 1) * 200) + 4;
		player.getPersistentData().putDouble("dkc_current_floor", 0);
		player.getPersistentData().putBoolean("dkc_floor_just_changed", true);
		player.setNoGravity(true);
		dkcLevel.getChunk(BlockPos.containing(targetX, targetY, targetZ));
		if (player.level().dimension() != DKC_DIMENSION) {
			player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
			player.teleportTo(dkcLevel, player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
			player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
			for (MobEffectInstance effectInstance : player.getActiveEffects()) {
				player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), effectInstance));
			}
			player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
		}
		player.teleportTo(targetX, targetY, targetZ);
		player.connection.teleport(targetX, targetY, targetZ, player.getYRot(), player.getXRot());
		dkcLevel.playSound(null, BlockPos.containing(targetX, targetY, targetZ), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.45F, 0.7F);
		player.closeContainer();
		player.displayClientMessage(Component.literal("\u00A75Path opened: \u00A74Demon King's Castle \u00A78Floor " + floor), true);
		SololevelingMod.queueServerWork(10, () -> {
			if (player.isAlive()) {
				player.setNoGravity(false);
			}
		});
	}

	public static void returnToSavedOverworld(ServerPlayer player) {
		if (player == null || player.server == null)
			return;
		ServerLevel overworld = player.server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			player.displayClientMessage(Component.literal("\u00A74The way back to the Overworld is lost."), true);
			return;
		}
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		double targetX = vars.DunX;
		double targetY = vars.DunY;
		double targetZ = vars.DunZ;
		if (targetX == 0 && targetY == 0 && targetZ == 0) {
			BlockPos spawn = overworld.getSharedSpawnPos();
			targetX = spawn.getX() + 0.5D;
			targetY = spawn.getY() + 1.0D;
			targetZ = spawn.getZ() + 0.5D;
		}
		player.setNoGravity(true);
		overworld.getChunk(BlockPos.containing(targetX, targetY, targetZ));
		player.teleportTo(overworld, targetX, targetY, targetZ, player.getYRot(), player.getXRot());
		player.connection.teleport(targetX, targetY, targetZ, player.getYRot(), player.getXRot());
		overworld.playSound(null, BlockPos.containing(targetX, targetY, targetZ), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.45F, 1.15F);
		player.displayClientMessage(Component.literal("\u00A75The key drags you back to where the castle found you."), true);
		SololevelingMod.queueServerWork(10, () -> {
			if (player.isAlive()) {
				player.setNoGravity(false);
			}
		});
	}

	private static void saveReturnPosition(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.DunX = player.getX();
			capability.DunY = player.getY();
			capability.DunZ = player.getZ();
			capability.syncPlayerVariables(player);
		});
	}

	private static void discardOwnedShadows(ServerPlayer player) {
		Vec3 center = player.position();
		List<Entity> found = player.level().getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(250), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList();
		for (Entity foundEntity : found) {
			if (foundEntity.getType().is(SHADOWS_TAG) && foundEntity instanceof TamableAnimal tamable && tamable.isOwnedBy(player)) {
				foundEntity.discard();
			}
		}
	}
}
