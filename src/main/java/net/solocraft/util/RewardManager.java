package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.RewardCollectProcedure;

import net.minecraft.core.registries.BuiltInRegistries;

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
	private static final String FULL_RECOVERY_REWARD = "FR";

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
		reconcileFullRecoveryRewards(entity);
		if (isFullRecovery(cleanReward) && hasPendingFullRecovery(entity)) {
			applyFullRecovery(entity);
			return;
		}
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

	public static boolean hasPendingFullRecovery(Entity entity) {
		if (entity == null)
			return false;
		return allRewards(entity).stream().anyMatch(RewardManager::isFullRecovery);
	}

	/**
	 * Assigns a Full Recovery to one of the three legacy command slots while
	 * enforcing the same single-pending-reward rule as {@link #appendReward}.
	 */
	public static void setFullRecoveryReward(Entity entity, int slot,
			boolean preservePreviousReward) {
		if (entity == null || slot < 1 || slot > 3)
			return;
		reconcileFullRecoveryRewards(entity);
		if (hasPendingFullRecovery(entity)) {
			applyFullRecovery(entity);
			return;
		}

		String previous = rewardAt(entity, slot);
		if (preservePreviousReward && !isEmptyReward(previous))
			appendReward(entity, previous);
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			if (slot == 1)
				vars.reward_1 = FULL_RECOVERY_REWARD;
			else if (slot == 2)
				vars.reward_2 = FULL_RECOVERY_REWARD;
			else
				vars.reward_3 = FULL_RECOVERY_REWARD;
			vars.syncPlayerVariables(entity);
		});
	}

	/**
	 * Migrates an inbox that already contains duplicate Full Recoveries. The
	 * first remains pending and the overflow is immediately used once, which is
	 * sufficient because Full Recovery restores every affected value to max.
	 */
	public static boolean reconcileFullRecoveryRewards(Entity entity) {
		if (entity == null || entity.level().isClientSide())
			return false;
		List<String> rewards = allRewards(entity);
		boolean found = false;
		boolean duplicate = false;
		List<String> normalized = new ArrayList<>(rewards.size());
		for (String reward : rewards) {
			if (isFullRecovery(reward)) {
				if (found) {
					duplicate = true;
					continue;
				}
				found = true;
			}
			normalized.add(reward);
		}
		if (!duplicate)
			return false;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			writeRewards(vars, normalized);
			vars.syncPlayerVariables(entity);
		});
		applyFullRecovery(entity);
		return true;
	}

	public static boolean claimReward(Entity entity, int slot) {
		// The label the player sees is rendered from the cleaned value, so the
		// collector has to receive the same string. Comparing the raw one made a
		// stored reward with stray whitespace display correctly and then match no
		// branch at all when claimed.
		String reward = clean(rewardAt(entity, slot));
		if (isEmptyReward(reward))
			return false;
		if (reward.startsWith(DaggerThrowManager.RECOVERY_PREFIX)) {
			if (!(entity instanceof net.minecraft.server.level.ServerPlayer player)
					|| !DaggerThrowManager.claimRecovery(player, reward))
				return false;
		} else if (!RewardCollectProcedure.execute(entity, reward))
			return false;
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
		if (cleanReward.startsWith(DaggerThrowManager.RECOVERY_PREFIX))
			return "\u00A7l" + DaggerThrowManager.displayRecoveryName(cleanReward);
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
				Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(itemResourceLocation));
				if (item != null && item != Items.AIR)
					return "\u00A7lItem: " + new ItemStack(item).getDisplayName().getString();
				return "\u00A7lUnknown Item";
			} catch (Exception e) {
				return "\u00A7lInvalid Item";
			}
		}
		if (isFullRecovery(cleanReward))
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
		writeRewards(vars, rewards);
	}

	private static void writeRewards(SololevelingModVariables.PlayerVariables vars,
			List<String> rewards) {
		vars.reward_1 = rewards.size() > 0 ? rewards.get(0) : "";
		vars.reward_2 = rewards.size() > 1 ? rewards.get(1) : "";
		vars.reward_3 = rewards.size() > 2 ? rewards.get(2) : "";
		vars.reward_extra = rewards.size() > 3 ? String.join(JOIN_DELIMITER, rewards.subList(3, rewards.size())) : "";
	}

	private static boolean isFullRecovery(String reward) {
		return FULL_RECOVERY_REWARD.equals(clean(reward));
	}

	private static void applyFullRecovery(Entity entity) {
		if (entity != null && !entity.level().isClientSide())
			RewardCollectProcedure.execute(entity, FULL_RECOVERY_REWARD);
	}

	/** Removes one exact pending reward, used when an escrowed dagger returns normally. */
	public static void removeReward(Entity entity, String reward) {
		String target = clean(reward);
		if (entity == null || target.isEmpty())
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			List<String> rewards = new ArrayList<>();
			addIfPresent(rewards, vars.reward_1);
			addIfPresent(rewards, vars.reward_2);
			addIfPresent(rewards, vars.reward_3);
			rewards.addAll(parseExtra(vars.reward_extra));
			if (!rewards.remove(target))
				return;
			writeRewards(vars, rewards);
			vars.syncPlayerVariables(entity);
		});
	}

	private static String clean(String reward) {
		if (reward == null)
			return "";
		String trimmed = reward.trim();
		if ("\"\"".equals(trimmed))
			return "";
		if (trimmed.startsWith("ITEM:")
				&& MageSpellProgression.isRetiredRunestoneId(trimmed.substring(5)))
			return "SP5";
		return trimmed;
	}
}
