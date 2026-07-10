package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.VulcanEntity;
import net.solocraft.entity.CerberusEntity;
import net.solocraft.entity.BaranEntity;
import net.solocraft.util.SystemNotifications;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.ChatFormatting;

import java.util.UUID;

public class DKCBossKillRewardProcedure {
	// gives entry permit, increases dkc_cleared, and generates next floor when boss is killed
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceEntity) {
		if (world == null || entity == null)
			return;
		boolean isBoss = entity instanceof CerberusEntity || entity instanceof VulcanEntity || entity instanceof BaranEntity;
		if (!isBoss)
			return;
		// Baran only counts in the DKC dimension
		if (entity instanceof BaranEntity) {
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dkc =
				net.minecraft.resources.ResourceKey.create(
					net.minecraft.core.registries.Registries.DIMENSION,
					new net.minecraft.resources.ResourceLocation("sololeveling", "dungeon_dimension_dkc"));
			if (!entity.level().dimension().equals(dkc))
				return;
		}
		CompoundTag bossData = entity.getPersistentData();
		int floorNumber = (int) bossData.getDouble("dkc_floor_number");
		String spawnerUUID = bossData.getString("dkc_spawned_by");
		// determine the actual killer (player, pet owner, or shadow owner)
		Entity killer = ShadowKillCreditHelper.creditedSource(world, sourceEntity);
		// if no spawner uuid, use the killer
		if (spawnerUUID.isEmpty() && killer != null) {
			spawnerUUID = killer.getStringUUID();
			if (killer instanceof Player) {
				floorNumber = DKCFloorDetectorProcedure.getCurrentFloor(killer);
			}
		}
		// if still no killer, try to find nearest player (for pre-spawned bosses)
		if (spawnerUUID.isEmpty() && world instanceof ServerLevel serverLevel) {
			Player nearestPlayer = serverLevel.getNearestPlayer(x, y, z, 100, false);
			if (nearestPlayer != null) {
				spawnerUUID = nearestPlayer.getStringUUID();
				floorNumber = DKCFloorDetectorProcedure.getCurrentFloor(nearestPlayer);
			}
		}
		if (floorNumber == 0 || spawnerUUID.isEmpty())
			return;
		// find the player
		if (world instanceof ServerLevel serverLevel) {
			try {
				UUID playerUUID = UUID.fromString(spawnerUUID);
				Player player = serverLevel.getPlayerByUUID(playerUUID);
				if (player != null) {
					CompoundTag playerData = player.getPersistentData();
					// guard against double-firing (death event can fire more than once)
					if (playerData.getBoolean("dkc_floor_" + floorNumber + "_boss_defeated")) {
						return;
					}
					playerData.putBoolean("dkc_floor_" + floorNumber + "_boss_defeated", true);
					// increase dkc_cleared variable
					player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.dkc_cleared = capability.dkc_cleared + 1;
						capability.syncPlayerVariables(player);
					});
					XPGainProcedure.awardBaseXp(world, player, floorNumber * 100);
					notifyDkc(player, 0xFF4DFF88, "FLOOR COMPLETE", "Floor " + floorNumber + " has been cleared.", 100);
					// give boss-specific rewards
					if (floorNumber == 1) {
						// cerberus rewards
						try {
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(),
									"slr " + player.getGameProfile().getName() + " rewards set 1 Item sololeveling:entry_permit true");
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(),
									"slr " + player.getGameProfile().getName() + " rewards set 2 Item sololeveling:world_trees_fragment true");
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(), "slr " + player.getGameProfile().getName() + " rewards set 3 FullRecovery true");
						} catch (Exception e) {
						}
					} else if (floorNumber == 10) {
						// vulcan rewards
						try {
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(),
									"slr " + player.getGameProfile().getName() + " rewards set 1 Item sololeveling:entry_permit true");
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(),
									"slr " + player.getGameProfile().getName() + " rewards set 2 Item sololeveling:spring_water_of_the_echoing_forest true");
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(), "slr " + player.getGameProfile().getName() + " rewards set 3 FullRecovery true");
						} catch (Exception e) {
						}
						grantAdvancement(player, "monarchs_domain");
					} else if (floorNumber == 20) {
						// baran rewards (final boss - no entry permit)
						try {
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(),
									"slr " + player.getGameProfile().getName() + " rewards set 1 Item sololeveling:purified_blood_of_the_demon_king true");
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(),
									"slr " + player.getGameProfile().getName() + " rewards set 2 Item " + new net.minecraft.resources.ResourceLocation("sololeveling", "demon_kings_dagger").toString() + " true");
							serverLevel.getServer().getCommands().performPrefixedCommand(serverLevel.getServer().createCommandSourceStack(),
									"slr " + player.getGameProfile().getName() + " rewards set 3 Item " + new net.minecraft.resources.ResourceLocation("sololeveling", "demon_kings_long_sword").toString() + " true");
						} catch (Exception e) {
						}
					}
					// generate next floor based on which floor was just completed
					if (floorNumber == 1) {
						// floor 1 complete, generate floor 2 (regular floor)
						FloorCreateNewProcedure.execute(world, player);
						notifyDkc(player, 0xFFFFB83D, "FLOOR UNLOCKED", "Floor 2 has been unlocked.", 90);
					} else if (floorNumber == 10) {
						// floor 10 complete, generate floor 11 (regular floor)
						FloorCreateNewProcedure.execute(world, player);
						notifyDkc(player, 0xFFFFB83D, "FLOOR UNLOCKED", "Floor 11 has been unlocked.", 90);
					} else if (floorNumber == 20) {
						// final boss defeated
						notifyDkc(player, 0xFFFFB83D, "DKC CONQUERED", "You have conquered the Demon King's Castle.", 140);
					}
				}
			} catch (IllegalArgumentException e) {
				// invalid uuid
			}
		}
	}

	private static void grantAdvancement(Player player, String advancementId) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return;
		Advancement advancement = serverPlayer.server.getAdvancements().getAdvancement(new ResourceLocation("sololeveling", advancementId));
		if (advancement == null)
			return;
		AdvancementProgress progress = serverPlayer.getAdvancements().getOrStartProgress(advancement);
		if (!progress.isDone()) {
			for (String criteria : progress.getRemainingCriteria()) {
				serverPlayer.getAdvancements().award(advancement, criteria);
			}
		}
	}

	private static void notifyDkc(Player player, int accent, String title, String under, int durationTicks) {
		if (player instanceof ServerPlayer serverPlayer) {
			SystemNotifications.showTitleUnder(serverPlayer, accent, durationTicks,
					Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.literal(under).withStyle(ChatFormatting.GRAY));
		}
	}

}
