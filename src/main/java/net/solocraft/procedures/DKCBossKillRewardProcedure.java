package net.solocraft.procedures;

import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcSpatialLayout;
import net.solocraft.dkc.DkcRadiruManager;
import net.solocraft.entity.BaranEntity;
import net.solocraft.entity.CerberusEntity;
import net.solocraft.entity.KaiselinEntity;
import net.solocraft.entity.VulcanEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.RewardManager;
import net.solocraft.util.SystemNotifications;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;

import java.util.UUID;

/** Boss rewards, with strict dimension/owner checks and atomic floor progress. */
public class DKCBossKillRewardProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceEntity) {
		if (!(world instanceof ServerLevel level) || entity == null || !DkcFloorRegistry.isSharedDkc(level))
			return;
		boolean exactKaiselin = entity instanceof KaiselinEntity
				&& entity.getType() == SololevelingModEntities.KAISELIN.get();
		if (!(entity instanceof CerberusEntity) && !(entity instanceof VulcanEntity)
				&& !(entity instanceof BaranEntity) && !exactKaiselin)
			return;

		CompoundTag bossData = entity.getPersistentData();
		int floor = (int) bossData.getDouble("dkc_floor_number");
		if (!matchesBossFloor(entity, floor, exactKaiselin))
			return;
		String ownerText = bossData.getString("dkc_spawned_by");
		if (ownerText.isBlank())
			return;
		UUID owner;
		try {
			owner = UUID.fromString(ownerText);
		} catch (IllegalArgumentException ignored) {
			return;
		}
		ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
		if (player == null || !DkcSpatialLayout.isPlayerInFloor(player, floor)
				|| !DkcSpatialLayout.isEntityInOwnedFloor(entity, owner, floor))
			return;
		ServerPlayer creditedPlayer = ShadowKillCreditHelper.creditedServerPlayer(world, sourceEntity);
		if (creditedPlayer == null || !creditedPlayer.getUUID().equals(player.getUUID()))
			return;
		double alreadyCleared = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).dkc_cleared;
		if (alreadyCleared >= floor)
			return;

		CompoundTag data = player.getPersistentData();
		if (floor == 20) {
			if (entity instanceof BaranEntity) {
				data.putBoolean("dkc_floor_20_baran_defeated", true);
				BaranSummonProcedure.discardBossAdds(level, player, floor);
			}
			if (exactKaiselin)
				data.putBoolean("dkc_floor_20_kaiselin_defeated", true);
			boolean baranDown = data.getBoolean("dkc_floor_20_baran_defeated");
			boolean kaiselinDown = data.getBoolean("dkc_floor_20_kaiselin_defeated");
			if (!baranDown || !kaiselinDown)
				return;
			ensureKaiselSoul(level, player);
		}

		String defeatedKey = "dkc_floor_" + floor + "_boss_defeated";
		if (data.getBoolean(defeatedKey))
			return;
		data.putBoolean(defeatedKey, true);
		ServerPlayer rewardPlayer = player;
		rewardPlayer.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.dkc_cleared = Math.max(capability.dkc_cleared, floor);
			capability.syncPlayerVariables(rewardPlayer);
		});
		XPGainProcedure.awardBaseXp(world, player, floor * 100);
		notify(player, 0xFF4DFF88, "BOSS SLAIN",
				floor == DkcFloorRegistry.LAST_FLOOR
						? "Demon King's Castle conquered."
						: "Floor " + floor + " cleared.",
				100);
		grantBossRewards(level, player, floor);

		if (floor == DkcFloorRegistry.LAST_FLOOR)
			DkcRadiruManager.onCastleConquered(player);
	}

	private static boolean matchesBossFloor(Entity entity, int floor, boolean exactKaiselin) {
		return floor == 1 && entity instanceof CerberusEntity
				|| floor == 10 && entity instanceof VulcanEntity
				|| floor == 20 && (entity instanceof BaranEntity || exactKaiselin);
	}

	private static void ensureKaiselSoul(ServerLevel level, ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		if (data.getBoolean(KaiselinEntity.DKC_SOUL_SPAWNED))
			return;
		BlockPos ground = DkcFloorBuilder.bossPosition(player, 20);
		while (ground.getY() > level.getMinBuildHeight() + 1 && level.isEmptyBlock(ground.below()))
			ground = ground.below();
		while (ground.getY() < level.getMaxBuildHeight() - 2 && !level.isEmptyBlock(ground))
			ground = ground.above();
		Entity soul = SololevelingModEntities.SHADOW_SOUL.get().spawn(level, ground, MobSpawnType.MOB_SUMMONED);
		if (soul == null)
			return;
		soul.getPersistentData().putString("soultype", "kaisel");
		soul.getPersistentData().putDouble("dkc_floor_number", 20);
		soul.getPersistentData().putString("dkc_spawned_by", player.getStringUUID());
		data.putBoolean(KaiselinEntity.DKC_SOUL_SPAWNED, true);
	}

	private static void grantBossRewards(ServerLevel level, ServerPlayer player, int floor) {
		String name = player.getGameProfile().getName();
		try {
			if (floor == 1) {
				reward(level, name, "rewards set 1 Item sololeveling:entry_permit true");
				reward(level, name, "rewards set 2 Item sololeveling:world_trees_fragment true");
				reward(level, name, "rewards set 3 FullRecovery true");
			} else if (floor == 10) {
				reward(level, name, "rewards set 1 Item sololeveling:entry_permit true");
				reward(level, name, "rewards set 2 Item sololeveling:spring_water_of_the_echoing_forest true");
				reward(level, name, "rewards set 3 FullRecovery true");
				RewardManager.appendReward(player, "ITEM:sololeveling:orb_of_avarice");
				grantAdvancement(player, "monarchs_domain");
			} else if (floor == 20) {
				reward(level, name, "rewards set 1 Item sololeveling:purified_blood_of_the_demon_king true");
				reward(level, name, "rewards set 2 Item sololeveling:demon_kings_dagger true");
				reward(level, name, "rewards set 3 Item sololeveling:demon_kings_long_sword true");
			}
		} catch (RuntimeException ignored) {
		}
	}

	private static void reward(ServerLevel level, String playerName, String arguments) {
		level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),
				"slr " + playerName + " " + arguments);
	}

	private static void grantAdvancement(ServerPlayer player, String advancementId) {
		Advancement advancement = player.server.getAdvancements()
				.getAdvancement(new ResourceLocation("sololeveling", advancementId));
		if (advancement == null)
			return;
		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		if (!progress.isDone())
			for (String criterion : progress.getRemainingCriteria())
				player.getAdvancements().award(advancement, criterion);
	}

	private static void notify(ServerPlayer player, int accent, String title, String under, int duration) {
		SystemNotifications.showTitleUnder(player, accent, duration,
				Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.literal(under).withStyle(ChatFormatting.GRAY));
	}
}
