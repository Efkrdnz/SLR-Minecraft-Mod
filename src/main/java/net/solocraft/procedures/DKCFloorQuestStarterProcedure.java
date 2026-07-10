package net.solocraft.procedures;

import net.solocraft.entity.KaiselinEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemNotifications;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.AABB;

public class DKCFloorQuestStarterProcedure {
	// main procedure that checks floor and starts quests
	public static void execute(LevelAccessor world, Entity entity) {
		if (world == null || entity == null || !(entity instanceof Player))
			return;
		Player player = (Player) entity;
		// only run in DKC dimension
		if ((player.level().dimension()) != (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_dkc")))) {
			return;
		}
		// only run every 20 ticks (1 second)
		if (!(world instanceof Level _level))
			return;
		if (_level.getGameTime() % 20 != 0)
			return;
		// only trigger for survival/adventure players
		if (player instanceof ServerPlayer serverPlayer) {
			net.minecraft.world.level.GameType gameMode = serverPlayer.gameMode.getGameModeForPlayer();
			if (gameMode == net.minecraft.world.level.GameType.SPECTATOR || gameMode == net.minecraft.world.level.GameType.CREATIVE)
				return;
		}
		// detect current floor
		DKCFloorDetectorProcedure.execute(entity);
		CompoundTag data = player.getPersistentData();
		int currentFloor = DKCFloorDetectorProcedure.getCurrentFloor(player);
		int previousFloor = (int) data.getDouble("dkc_previous_floor");
		boolean justChanged = data.getBoolean("dkc_floor_just_changed");
		if (currentFloor == 20 && !data.getBoolean("dkc_floor_20_boss_defeated")) {
			ensureFloor20Kaiselin(world, player);
		}
		// check if player entered a new floor
		if (justChanged && currentFloor != previousFloor) {
			startFloorQuest(world, player, currentFloor);
		}
	}

	// starts the quest/spawning for a specific floor
	private static void startFloorQuest(LevelAccessor world, Player player, int floor) {
		if (player == null)
			return;
		CompoundTag data = player.getPersistentData();
		// check if floor is unlocked
		int clearedFloors = (int) (player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared;
		if (floor > clearedFloors + 1) {
			notifyDkcNegative(player, 0xFFFF3D3D, "FLOOR LOCKED", "Complete Floor " + clearedFloors + " first.", 80);
			return;
		}
		// check if quest already started for this floor
		if (data.getBoolean("dkc_floor_" + floor + "_spawned")) {
			return;
		}
		// boss floors (1, 20) - just notify, bosses are pre-spawned
		if (floor == 1) {
			notifyDkc(player, 0xFFFF4C4C, "FLOOR 1", "Cerberus' Den\nDefeat the guardian Cerberus.", 110);
			data.putBoolean("dkc_floor_1_spawned", true);
			return;
		}
		if (floor == 20) {
			ensureFloor20Kaiselin(world, player);
			notifyDkc(player, 0xFFFF3D3D, "FLOOR 20", "Demon King's Throne\nDefeat Baran and Kaiselin.", 120);
			data.putBoolean("dkc_floor_20_spawned", true);
			return;
		}
		// floor 10 - auto-spawns Vulcan after 100 kills
		if (floor == 10) {
			data.putDouble("dkc_floor_10_required", 50);
			data.putDouble("dkc_floor_10_killed", 0);
			data.putBoolean("dkc_floor_10_complete", false);
			notifyDkc(player, 0xFFFF6A3D, "FLOOR 10", "The Demon Lord's Chamber\nDefeat 50 demons, then face Vulcan.", 120);
			// spawn demons
			DKCDemonSpawnerProcedure.execute(world, player);
			return;
		}
		// regular floors (2-9, 11-19)
		int requiredKills = switch (floor) {
			case 2  -> 10;
			case 3  -> 15;
			case 4  -> 20;
			case 5  -> 25;
			case 6  -> 28;
			case 7  -> 32;
			case 8  -> 36;
			case 9  -> 40;
			case 11 -> 25;
			case 12 -> 26;
			case 13 -> 27;
			case 14 -> 28;
			case 15 -> 29;
			case 16 -> 30;
			case 17 -> 32;
			case 18 -> 34;
			case 19 -> 38;
			default -> 10;
		};
		notifyDkc(player, 0xFFFFB83D, "FLOOR " + floor, "Defeat " + requiredKills + " demons to proceed.", 100);
		// spawn demons
		DKCDemonSpawnerProcedure.execute(world, player);
	}

	private static void notifyDkc(Player player, int accent, String title, String under, int durationTicks) {
		if (player instanceof ServerPlayer serverPlayer) {
			SystemNotifications.showTitleUnder(serverPlayer, accent, durationTicks,
					Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal(under).withStyle(ChatFormatting.GRAY));
		}
	}

	private static void notifyDkcNegative(Player player, int accent, String title, String under, int durationTicks) {
		if (player instanceof ServerPlayer serverPlayer) {
			SystemNotifications.showNegativeTitleUnder(serverPlayer, accent, durationTicks,
					Component.literal(title).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
					Component.literal(under).withStyle(ChatFormatting.RED));
		}
	}

	private static void ensureFloor20Kaiselin(LevelAccessor world, Player player) {
		if (!(world instanceof ServerLevel serverLevel) || player == null)
			return;
		double originX = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).dkc_x;
		double originY = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).dkc_y;
		double originZ = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).dkc_z;
		double floorStartZ = originZ + (19 * 200);
		String owner = player.getStringUUID();
		CompoundTag data = player.getPersistentData();
		if (data.getBoolean("dkc_floor_20_kaiselin_defeated"))
			return;
		AABB floorSearch = new AABB(originX - 160, originY - 96, floorStartZ - 80, originX + 220, originY + 180, floorStartZ + 240);
		for (KaiselinEntity existing : serverLevel.getEntitiesOfClass(KaiselinEntity.class, floorSearch)) {
			if ((int) existing.getPersistentData().getDouble("dkc_floor_number") == 20
					&& owner.equals(existing.getPersistentData().getString("dkc_spawned_by")))
				return;
		}
		if (data.getBoolean("dkc_floor_20_kaiselin_spawned"))
			return;
		BlockPos spawnPos = findSpawnPos(serverLevel, originX + 92, originY + 8, floorStartZ + 104);
		if (spawnPos == null)
			spawnPos = BlockPos.containing(originX + 92, originY + 8, floorStartZ + 104);
		KaiselinEntity kaiselin = SololevelingModEntities.KAISELIN.get().spawn(serverLevel, spawnPos, MobSpawnType.MOB_SUMMONED);
		if (kaiselin != null) {
			kaiselin.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 6.0D, spawnPos.getZ() + 0.5D, player.getYRot() + 180.0F, 0.0F);
			kaiselin.getPersistentData().putDouble("dkc_floor_number", 20);
			kaiselin.getPersistentData().putString("dkc_spawned_by", owner);
			kaiselin.setTarget(player);
			data.putBoolean("dkc_floor_20_kaiselin_spawned", true);
		}
	}

	private static BlockPos findSpawnPos(ServerLevel level, double x, double y, double z) {
		BlockPos pos = BlockPos.containing(x, y, z);
		for (int i = 0; i < 24; i++) {
			BlockPos below = pos.below();
			if (!level.isEmptyBlock(below) && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()))
				return pos;
			pos = pos.below();
		}
		pos = BlockPos.containing(x, y, z);
		for (int i = 0; i < 24; i++) {
			BlockPos below = pos.below();
			if (!level.isEmptyBlock(below) && level.isEmptyBlock(pos) && level.isEmptyBlock(pos.above()))
				return pos;
			pos = pos.above();
		}
		return null;
	}
}
