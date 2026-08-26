package net.solocraft.util;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.item.HunterIDItem;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.PowerAppendAssassinProcedure;
import net.solocraft.procedures.PowerAppendFighterProcedure;
import net.solocraft.procedures.PowerAppendHealerProcedure;
import net.solocraft.procedures.PowerAppendMageProcedure;
import net.solocraft.procedures.PowerAppendRangerProcedure;
import net.solocraft.procedures.PowerAppendTankerProcedure;

import net.neoforged.neoforge.items.ItemHandlerHelper;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Applies the original evaluation stat package exactly once. The receipt is
 * written before additive mutations, making repeated packets and reconnect
 * recovery unable to duplicate stats or starter packs.
 */
public final class HunterEvaluationRewardService {
	public static final String REWARDS_APPLIED = "RewardsApplied";

	/**
	 * Scales every awakening stat grant. 1.25 exactly cancels the 20% that was
	 * shaved off the skill-point exchange rate, so a fresh hunter ends up with the
	 * same total early power as before that change.
	 */
	public static final double STARTING_STAT_MULTIPLIER = 1.25D;

	private HunterEvaluationRewardService() {
	}

	public static boolean applyInitialRewards(ServerPlayer player, int classId,
			int rank, CompoundTag evaluationData) {
		if (player == null || evaluationData.getBoolean(REWARDS_APPLIED))
			return false;

		evaluationData.putBoolean(REWARDS_APPLIED, true);
		applyStats(player, classId, rank);
		giveStarterPack(player, classId);
		grantClassPowers(player, classId);
		ItemHandlerHelper.giveItemToPlayer(player,
				HunterIDItem.createBoundCard(player));
		return true;
	}

	public static void markLegacyRewardsApplied(CompoundTag evaluationData) {
		evaluationData.putBoolean(REWARDS_APPLIED, true);
	}

	private static void applyStats(ServerPlayer player, int classId, int rank) {
		player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					double lore = capability.LoreAccurateRankStart;
					// Skill points are scarcer than they were, so the awakening
					// hands over more of a hunter's power up front to keep total
					// early progression where it was.
					double curve = Math.pow(rank, lore) * STARTING_STAT_MULTIPLIER;
					double flat = rank * lore * STARTING_STAT_MULTIPLIER;
					switch (classId) {
						case 1 -> {
							capability.Speed += curve * 5.0D;
							capability.Strength += curve * 2.0D;
							capability.Intelligence += curve * 3.0D;
							capability.Vitality += curve;
							capability.perception += curve * 4.0D;
						}
						case 2 -> {
							capability.Speed += curve;
							capability.Vitality += curve;
							capability.Intelligence +=
									AwakeningStatCurves.intelligenceBonus(2, rank)
									* STARTING_STAT_MULTIPLIER;
							capability.perception += curve * 2.0D;
							capability.Strength += flat * 2.0D;
						}
						case 3 -> {
							capability.Vitality += curve * 2.0D;
							capability.Strength += curve * 4.0D;
							capability.perception += curve;
							capability.Intelligence += curve;
							capability.Speed += curve;
						}
						case 4 -> {
							capability.Vitality += curve * 4.0D;
							capability.Strength += curve * 2.0D;
							capability.Intelligence += flat;
							capability.Speed += curve;
							capability.perception += flat * 2.0D;
						}
						case 5 -> {
							capability.Intelligence +=
									AwakeningStatCurves.intelligenceBonus(5, rank)
									* STARTING_STAT_MULTIPLIER;
							capability.Vitality += curve * 5.0D;
							capability.perception += curve * 3.0D;
							capability.Speed += curve * 2.0D;
							capability.Strength += curve * 2.0D;
						}
						case 6 -> {
							capability.Intelligence +=
									AwakeningStatCurves.intelligenceBonus(6, rank)
									* STARTING_STAT_MULTIPLIER;
							capability.Vitality += curve * 4.0D;
							capability.perception += curve * 4.0D;
							capability.Speed += curve * 3.0D;
							capability.Strength += curve;
						}
						default -> {
							return;
						}
					}

					if (rank >= 1 && rank <= 4) {
						capability.Strength =
								Math.ceil(capability.Strength * 1.15D);
						capability.Speed =
								Math.ceil(capability.Speed * 1.15D);
						capability.Intelligence =
								Math.ceil(capability.Intelligence * 1.15D);
						capability.Vitality =
								Math.ceil(capability.Vitality * 1.15D);
						capability.perception =
								Math.ceil(capability.perception * 1.15D);
					}
					capability.syncPlayerVariables(player);
				});
	}

	private static void giveStarterPack(ServerPlayer player, int classId) {
		Item item = switch (classId) {
			case 2 -> SololevelingModItems.MAGE_STARTERPACK.get();
			case 3 -> SololevelingModItems.FIGHTER_STARTERPACK.get();
			case 4 -> SololevelingModItems.TANKER_STARTERPACK.get();
			case 5 -> SololevelingModItems.HEALER_STARTERPACK.get();
			case 6 -> SololevelingModItems.RANGER_STARTERPACK.get();
			default -> null;
		};
		if (item != null)
			ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(item));
	}

	private static void grantClassPowers(ServerPlayer player, int classId) {
		switch (classId) {
			case 1 -> PowerAppendAssassinProcedure.execute(player);
			case 2 -> PowerAppendMageProcedure.execute(player);
			case 3 -> PowerAppendFighterProcedure.execute(player);
			case 4 -> PowerAppendTankerProcedure.execute(player);
			case 5 -> PowerAppendHealerProcedure.execute(player);
			case 6 -> PowerAppendRangerProcedure.execute(player);
			default -> {
			}
		}
	}
}
