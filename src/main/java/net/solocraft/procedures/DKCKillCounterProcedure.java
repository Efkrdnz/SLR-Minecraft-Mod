package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.entity.DemonEntity;
import net.solocraft.entity.VulcanEntity;
import net.solocraft.SololevelingMod;
import net.solocraft.util.SystemNotifications;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.UUID;

public class DKCKillCounterProcedure {
	// tracks when a demon dies and updates kill count
	public static void execute(LevelAccessor world, Entity entity, Entity sourceEntity) {
		if (world == null || entity == null)
			return;
		if (!(entity instanceof DemonEntity) && !(entity instanceof DemonKnightEntity))
			return;
		CompoundTag demonData = entity.getPersistentData();
		int floorNumber = (int) demonData.getDouble("dkc_floor_number");
		String spawnerUUID = demonData.getString("dkc_spawned_by");
		if (floorNumber == 0 || spawnerUUID == null || spawnerUUID.isEmpty())
			return;
		if (!(world instanceof ServerLevel serverLevel))
			return;
		Player player;
		try {
			UUID playerUUID = UUID.fromString(spawnerUUID);
			player = serverLevel.getPlayerByUUID(playerUUID);
		} catch (IllegalArgumentException e) {
			return;
		}
		if (player == null)
			return;
		CompoundTag playerData = player.getPersistentData();
		// do not count kills during spawn phase
		if (playerData.getBoolean("dkc_floor_" + floorNumber + "_spawning")) {
			return;
		}
		double currentKills = playerData.getDouble("dkc_floor_" + floorNumber + "_killed");
		double required = playerData.getDouble("dkc_floor_" + floorNumber + "_required");
		playerData.putDouble("dkc_floor_" + floorNumber + "_killed", currentKills + 1);
		System.out.println("[DKC KILL] Floor " + floorNumber + " - " + player.getName().getString() + " killed demon (" + (int) (currentKills + 1) + "/" + (int) required + ")");
		// complete floor when requirement reached
		if ((currentKills + 1) >= required && !playerData.getBoolean("dkc_floor_" + floorNumber + "_complete")) {
			playerData.putBoolean("dkc_floor_" + floorNumber + "_complete", true);
			System.out.println("[DKC] Floor " + floorNumber + " marked as complete for " + player.getName().getString() + " (" + (int) (currentKills + 1) + "/" + (int) required + ")");
			// despawn remaining demons and knights from this floor, but only those owned by this player
			for (DemonEntity demon : serverLevel.getEntitiesOfClass(DemonEntity.class, player.getBoundingBox().inflate(300))) {
				int demonFloor = (int) demon.getPersistentData().getDouble("dkc_floor_number");
				String demonOwner = demon.getPersistentData().getString("dkc_spawned_by");
				if (demonFloor == floorNumber && player.getStringUUID().equals(demonOwner)) {
					demon.discard();
				}
			}
			for (DemonKnightEntity knight : serverLevel.getEntitiesOfClass(DemonKnightEntity.class, player.getBoundingBox().inflate(300))) {
				int knightFloor = (int) knight.getPersistentData().getDouble("dkc_floor_number");
				String knightOwner = knight.getPersistentData().getString("dkc_spawned_by");
				if (knightFloor == floorNumber && player.getStringUUID().equals(knightOwner)) {
					knight.discard();
				}
			}
			notifyDkc(player, 0xFF4DFF88, "OBJECTIVES COMPLETE", "Floor " + floorNumber + " cleared.", 90);
			// floor 10 is special: auto-spawn Vulcan at the middle of the arena
			if (floorNumber == 10) {
				double originX = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x;
				double originY = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y;
				double originZ = (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z;
				double bossX = originX;
				double bossY = originY + 3;
				double bossZ = originZ + 56 + (floorNumber - 1) * 200;
				// dramatic title announcement
				notifyDkc(player, 0xFFFF3D3D, "BOSS DETECTED", "A powerful presence stirs...", 90);
				// spawn Vulcan after a 3-second delay so the last demon dies first
				final String playerUUID = player.getStringUUID();
				final int capturedFloor = floorNumber;
				SololevelingMod.queueServerWork(60, () -> {
					Player target = serverLevel.getPlayerByUUID(UUID.fromString(playerUUID));
					if (target == null) return;
					VulcanEntity vulcan = (VulcanEntity) SololevelingModEntities.VULCAN.get().spawn(serverLevel, BlockPos.containing(bossX, bossY, bossZ), MobSpawnType.SPAWNER);
					if (vulcan != null) {
						vulcan.getPersistentData().putDouble("dkc_floor_number", capturedFloor);
						vulcan.getPersistentData().putString("dkc_spawned_by", playerUUID);
						vulcan.setTarget(target);
					}
					notifyDkc(target, 0xFFFF3D3D, "VULCAN", "The demon lord has appeared.", 100);
				});
				return;
			}
			// increase cleared floors
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.dkc_cleared = capability.dkc_cleared + 1;
				capability.syncPlayerVariables(player);
			});
			XPGainProcedure.awardBaseXp(world, player, floorNumber * 100);
			// give entry permit
			ItemStack permit = new ItemStack(SololevelingModItems.ENTRY_PERMIT.get());
			if (!player.getInventory().add(permit)) {
				ItemEntity itemEntity = new ItemEntity(serverLevel, player.getX(), player.getY(), player.getZ(), permit);
				serverLevel.addFreshEntity(itemEntity);
			}
			// generate next floor layout
			if (floorNumber == 9) {
				FloorCreateNewProcedure.execute(world, player, "vulcan"); // creates floor 10 layout
			} else if (floorNumber == 19) {
				FloorCreateNewProcedure.execute(world, player, "baran"); // creates floor 20 layout
			} else {
				FloorCreateNewProcedure.execute(world, player); // regular layout
			}
			notifyDkc(player, 0xFF3FC6FF, "ENTRY PERMIT", "Received Entry Permit.\nNext floor generated.", 100);
		}
		DKCDemonSpawnerProcedure.checkWaveSpawn(world, player);
	}

	private static void notifyDkc(Player player, int accent, String title, String under, int durationTicks) {
		if (player instanceof ServerPlayer serverPlayer) {
			SystemNotifications.showTitleUnder(serverPlayer, accent, durationTicks,
					Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal(under).withStyle(ChatFormatting.GRAY));
		}
	}

}
