package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.RewardCollectProcedure;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class RewardManager {
	private static final String DELIMITER = "\\|";
	private static final String JOIN_DELIMITER = "|";

	private RewardManager() {
	}

	public static boolean hasRewards(Entity entity) {
		return !allRewards(entity).isEmpty();
	}

	public static List<String> allRewards(Entity entity) {
		List<String> rewards = new ArrayList<>();
		if (entity == null)
			return rewards;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		addIfPresent(rewards, vars.reward_1);
		addIfPresent(rewards, vars.reward_2);
		addIfPresent(rewards, vars.reward_3);
		for (String reward : parseExtra(vars.reward_extra))
			addIfPresent(rewards, reward);
		return rewards;
	}

	public static String rewardAt(Entity entity, int slot) {
		if (entity == null || slot < 1)
			return "";
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		if (slot == 1)
			return clean(vars.reward_1);
		if (slot == 2)
			return clean(vars.reward_2);
		if (slot == 3)
			return clean(vars.reward_3);
		List<String> extra = parseExtra(vars.reward_extra);
		int index = slot - 4;
		return index >= 0 && index < extra.size() ? extra.get(index) : "";
	}

	public static void appendReward(Entity entity, String reward) {
		if (entity == null || isEmptyReward(reward))
			return;
		String cleanReward = clean(reward);
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			if (isEmptyReward(vars.reward_1)) {
				vars.reward_1 = cleanReward;
			} else if (isEmptyReward(vars.reward_2)) {
				vars.reward_2 = cleanReward;
			} else if (isEmptyReward(vars.reward_3)) {
				vars.reward_3 = cleanReward;
			} else {
				List<String> extra = parseExtra(vars.reward_extra);
				extra.add(cleanReward);
				vars.reward_extra = String.join(JOIN_DELIMITER, extra);
			}
			vars.syncPlayerVariables(entity);
		});
	}

	public static boolean claimReward(Entity entity, int slot) {
		String reward = rewardAt(entity, slot);
		if (isEmptyReward(reward))
			return false;
		RewardCollectProcedure.execute(entity, reward);
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			if (slot == 1) {
				vars.reward_1 = "";
			} else if (slot == 2) {
				vars.reward_2 = "";
			} else if (slot == 3) {
				vars.reward_3 = "";
			} else {
				List<String> extra = parseExtra(vars.reward_extra);
				int index = slot - 4;
				if (index >= 0 && index < extra.size())
					extra.remove(index);
				vars.reward_extra = String.join(JOIN_DELIMITER, extra);
			}
			compactSlots(vars);
			vars.syncPlayerVariables(entity);
		});
		return true;
	}

	public static String displayName(Entity entity, int slot) {
		return displayName(rewardAt(entity, slot));
	}

	public static String displayName(String reward) {
		String cleanReward = clean(reward);
		if (cleanReward.startsWith("SP")) {
			try {
				int amount = Integer.parseInt(cleanReward.substring(2));
				return "\u00A7l" + amount + " Skill Points";
			} catch (NumberFormatException e) {
				return "\u00A7lInvalid Reward";
			}
		}
		if (cleanReward.startsWith("GOLD")) {
			try {
				int amount = Integer.parseInt(cleanReward.substring(4));
				return "\u00A7l" + amount + " System Golds";
			} catch (NumberFormatException e) {
				return "\u00A7lInvalid Reward";
			}
		}
		if (cleanReward.startsWith("XP")) {
			try {
				int amount = Integer.parseInt(cleanReward.substring(2));
				return "\u00A7l" + amount + " XP";
			} catch (NumberFormatException e) {
				return "\u00A7lInvalid Reward";
			}
		}
		if (cleanReward.startsWith("ITEM:")) {
			String itemResourceLocation = cleanReward.substring(5);
			try {
				Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemResourceLocation));
				if (item != null && item != Items.AIR)
					return "\u00A7lItem: " + new ItemStack(item).getDisplayName().getString();
				return "\u00A7lUnknown Item";
			} catch (Exception e) {
				return "\u00A7lInvalid Item";
			}
		}
		if ("FR".equals(cleanReward))
			return "\u00A7lFull Recovery";
		if ("ITEMBOX".equals(cleanReward))
			return "\u00A7lRandom Item";
		return "\u00A7lCollected!";
	}

	public static boolean isEmptyReward(String reward) {
		return clean(reward).isEmpty();
	}

	private static SololevelingModVariables.PlayerVariables vars(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static void addIfPresent(List<String> rewards, String reward) {
		String cleanReward = clean(reward);
		if (!cleanReward.isEmpty())
			rewards.add(cleanReward);
	}

	private static List<String> parseExtra(String encoded) {
		List<String> rewards = new ArrayList<>();
		if (encoded == null || encoded.isBlank() || "\"\"".equals(encoded.trim()))
			return rewards;
		for (String reward : encoded.split(DELIMITER))
			addIfPresent(rewards, reward);
		return rewards;
	}

	private static void compactSlots(SololevelingModVariables.PlayerVariables vars) {
		List<String> rewards = new ArrayList<>();
		addIfPresent(rewards, vars.reward_1);
		addIfPresent(rewards, vars.reward_2);
		addIfPresent(rewards, vars.reward_3);
		rewards.addAll(parseExtra(vars.reward_extra));
		vars.reward_1 = rewards.size() > 0 ? rewards.get(0) : "";
		vars.reward_2 = rewards.size() > 1 ? rewards.get(1) : "";
		vars.reward_3 = rewards.size() > 2 ? rewards.get(2) : "";
		vars.reward_extra = rewards.size() > 3 ? String.join(JOIN_DELIMITER, rewards.subList(3, rewards.size())) : "";
	}

	private static String clean(String reward) {
		if (reward == null)
			return "";
		String trimmed = reward.trim();
		return "\"\"".equals(trimmed) ? "" : trimmed;
	}
}
