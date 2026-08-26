package net.solocraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Owns Baran's one-time personal rewards. The receipt lives in Forge's
 * persisted player compound so dying to Kaiselin and retrying floor 20 cannot
 * duplicate the rewards.
 */
public final class BaranVictoryRewards {
	public static final String SECOND_DAGGER_REWARD =
			"ITEM:sololeveling:demon_kings_dagger";
	public static final String SHADOW_EXCHANGE_RUNESTONE_REWARD =
			"ITEM:sololeveling:runestone_shadow_exchange";

	private static final String REWARDS_RESOLVED_TAG =
			"dkc_baran_victory_rewards_resolved_v1";

	private BaranVictoryRewards() {
	}

	/**
	 * Grants every legitimate victor the additional dagger and Shadow Exchange
	 * runestone. Runestones are classless ability unlocks.
	 */
	public static boolean grantIfNeeded(ServerPlayer player) {
		if (player == null)
			return false;
		CompoundTag data = persistedData(player);
		if (data.getBoolean(REWARDS_RESOLVED_TAG))
			return false;

		RewardManager.appendReward(player, SECOND_DAGGER_REWARD);
		RewardManager.appendReward(player, SHADOW_EXCHANGE_RUNESTONE_REWARD);
		data.putBoolean(REWARDS_RESOLVED_TAG, true);

		String detail = "Demon King's Dagger and Shadow Exchange Stone added to System Rewards.";
		player.displayClientMessage(Component.literal(detail)
				.withStyle(ChatFormatting.LIGHT_PURPLE), false);
		return true;
	}

	private static CompoundTag persistedData(Player player) {
		CompoundTag root = player.getPersistentData();
		CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
		if (!root.contains(Player.PERSISTED_NBT_TAG))
			root.put(Player.PERSISTED_NBT_TAG, persisted);
		return persisted;
	}
}
