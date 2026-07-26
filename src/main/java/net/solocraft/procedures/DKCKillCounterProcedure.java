package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcSpatialLayout;
import net.solocraft.dkc.DkcRadiruManager;
import net.solocraft.entity.DemonEntity;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;

import java.util.UUID;

/** Server-authoritative kill progression for staged DKC floor waves. */
public class DKCKillCounterProcedure {
	public static void execute(LevelAccessor world, Entity entity, Entity sourceEntity) {
		if (!(world instanceof ServerLevel level) || (!(entity instanceof DemonEntity)
				&& !(entity instanceof DemonKnightEntity)))
			return;
		CompoundTag enemyData = entity.getPersistentData();
		if (!DKCDemonSpawnerProcedure.FLOOR_WAVE_ROLE.equals(
				enemyData.getString(DKCDemonSpawnerProcedure.ROLE_TAG)))
			return; // Baran summons and unrelated demons never advance a floor.
		int floor = (int) enemyData.getDouble("dkc_floor_number");
		if (floor < 2 || floor > 19)
			return;
		String ownerText = enemyData.getString("dkc_spawned_by");
		ServerPlayer player;
		UUID owner;
		try {
			owner = UUID.fromString(ownerText);
			player = level.getServer().getPlayerList().getPlayer(owner);
		} catch (IllegalArgumentException exception) {
			return;
		}
		if (player == null || !DkcSpatialLayout.isPlayerInFloor(player, floor)
				|| !DkcSpatialLayout.isEntityInOwnedFloor(entity, owner, floor))
			return;
		ServerPlayer creditedPlayer = ShadowKillCreditHelper.creditedServerPlayer(world, sourceEntity);
		if (creditedPlayer == null || !creditedPlayer.getUUID().equals(owner))
			return;
		double alreadyCleared = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).dkc_cleared;
		if (alreadyCleared >= floor)
			return;

		CompoundTag data = player.getPersistentData();
		String prefix = "dkc_floor_" + floor;
		if (enemyData.getInt(DKCDemonSpawnerProcedure.ATTEMPT_TAG)
				!= DKCDemonSpawnerProcedure.currentAttempt(player, floor))
			return;
		int required = DkcFloorRegistry.requiredKills(floor);
		if (required <= 0 || data.getBoolean(prefix + "_spawning") || data.getBoolean(prefix + "_complete"))
			return;
		int kills = Math.min(required, (int) data.getDouble(prefix + "_killed") + 1);
		data.putDouble(prefix + "_killed", kills);
		if (kills < required)
			return;

		data.putBoolean(prefix + "_complete", true);
		DKCDemonSpawnerProcedure.discardOwnedWave(level, player, floor);
		DKCDemonSpawnerProcedure.invalidateAttempt(player, floor);
		if (floor == 10) {
			DkcFloorBuilder.ensureBosses(player, floor);
			return;
		}
		if (floor == DkcRadiruManager.FLOOR) {
			DkcRadiruManager.onDefendersOverpowered(level, player);
			return;
		}

		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.dkc_cleared = Math.max(capability.dkc_cleared, floor);
			capability.syncPlayerVariables(player);
		});
		XPGainProcedure.awardBaseXp(world, player, floor * 100);
		givePermit(level, player);
	}

	private static void givePermit(ServerLevel level, ServerPlayer player) {
		ItemStack permit = new ItemStack(SololevelingModItems.ENTRY_PERMIT.get());
		if (!player.getInventory().add(permit))
			level.addFreshEntity(new ItemEntity(level, player.getX(), player.getY(), player.getZ(), permit));
	}

}
