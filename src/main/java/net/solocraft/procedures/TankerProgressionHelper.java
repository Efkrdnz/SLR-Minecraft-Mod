package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.CooldownManager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side bridge for the Tanker-owned progression entry points.
 *
 * <p>The combat manager can later delegate to these token-safe rules while it
 * owns login/respawn/class-change reconciliation and legacy combat-state cleanup.</p>
 */
public final class TankerProgressionHelper {
	public static final String TAUNT = TankerProgressionRules.TAUNT;
	public static final String REINFORCEMENT = TankerProgressionRules.REINFORCEMENT;
	public static final String TANK_LEAP = TankerProgressionRules.TANK_LEAP;
	public static final String SHIELD_BASH = TankerProgressionRules.SHIELD_BASH;
	public static final String WILLPOWER = TankerProgressionRules.WILLPOWER;
	public static final String PROTECTION_MARK = TankerProgressionRules.PROTECTION_MARK;

	private static final String RECONCILED_TAG = "slr_tanker_reconciled_v1";
	private static final String RECONCILED_RANK_TAG = "slr_tanker_reconciled_rank_v1";
	private static final String COOLDOWN_PREFIX = "cd_";
	private static final String FULL_COOLDOWN_PREFIX = "slr_cd_full_";
	private static final Map<String, Integer> MAX_COOLDOWN_TICKS = Map.of(
			TAUNT, 240,
			SHIELD_BASH, 160,
			TANK_LEAP, 280,
			REINFORCEMENT, 440,
			WILLPOWER, 900,
			PROTECTION_MARK, 1200);

	private TankerProgressionHelper() {
	}

	public static void reconcileRankEntitlements(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			if ((int) Math.round(vars.Classes) != 4)
				return;
			boolean changed = migrateVariables(vars);
			int rank = Math.max(1, Math.min(6, (int) Math.round(vars.HunterRank)));
			for (String skill : TankerProgressionRules.entitlementsForRank(rank))
				changed |= ensureSkill(vars, skill);
			if (changed)
				vars.syncPlayerVariables(player);
			CompoundTag persisted = persistedData(player);
			persisted.putBoolean(RECONCILED_TAG, true);
			persisted.putInt(RECONCILED_RANK_TAG, rank);
		});
		migrateCooldownAliases(player);
	}

	/**
	 * Canonicalizes owned save surfaces without adding rank entitlements. This is
	 * safe for a runestone that explicitly grants one named skill at any rank.
	 */
	public static void reconcileAliases(ServerPlayer player) {
		if (player == null)
			return;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			if (migrateVariables(vars))
				vars.syncPlayerVariables(player);
		});
		migrateCooldownAliases(player);
	}

	public static String grantNextMasterySkill(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return "";
		final String[] granted = {""};
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			boolean changed = migrateVariables(vars);
			String missing = TankerProgressionRules.firstMissingSkill(vars.Plist);
			if (!missing.isEmpty()) {
				changed |= ensureSkill(vars, missing);
				granted[0] = missing;
			}
			if (changed)
				vars.syncPlayerVariables(player);
		});
		migrateCooldownAliases(player);
		return granted[0];
	}

	public static void learnFromRunestone(Entity entity, ItemStack stack, String requestedSkill) {
		if (!(entity instanceof ServerPlayer player) || stack == null || stack.isEmpty())
			return;
		String skill = TankerProgressionRules.canonicalName(requestedSkill);
		if (!TankerProgressionRules.isTankerSkill(skill))
			return;

		final boolean[] learned = {false};
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			boolean changed = migrateVariables(vars);
			if (!TankerProgressionRules.hasSkill(vars.Plist, skill)) {
				changed |= ensureSkill(vars, skill);
				learned[0] = true;
			}
			if (changed)
				vars.syncPlayerVariables(player);
		});
		migrateCooldownAliases(player);

		if (!learned[0]) {
			player.displayClientMessage(Component.translatable(
					"message.sololeveling.tanker.skill_known", skill), false);
			return;
		}
		if (!player.isCreative())
			stack.shrink(1);
		player.displayClientMessage(Component.translatable(
				"message.sololeveling.tanker.skill_gained", skill), false);
	}

	public static boolean isTanker(ServerPlayer player) {
		if (player == null)
			return false;
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(vars -> (int) Math.round(vars.Classes) == 4)
				.orElse(false);
	}

	private static boolean migrateVariables(SololevelingModVariables.PlayerVariables vars) {
		boolean changed = false;
		String migratedList = TankerProgressionRules.canonicalizeSkillList(vars.Plist);
		if (!migratedList.equals(vars.Plist)) {
			vars.Plist = migratedList;
			changed = true;
		}

		String selected = TankerProgressionRules.canonicalizeReference(vars.PselectedPower);
		if (!selected.equals(vars.PselectedPower)) {
			vars.PselectedPower = selected;
			changed = true;
		}
		for (int slot = 1; slot <= 16; slot++) {
			String current = SkillSlotHelper.getSlot(vars, slot);
			String canonical = TankerProgressionRules.canonicalizeReference(current);
			if (!canonical.equals(current)) {
				SkillSlotHelper.setSlot(vars, slot, canonical);
				changed = true;
			}
		}
		return changed;
	}

	private static boolean ensureSkill(SololevelingModVariables.PlayerVariables vars, String skill) {
		String updated = TankerProgressionRules.ensureSkill(vars.Plist, skill);
		if (updated.equals(vars.Plist))
			return false;
		vars.Plist = updated;
		return true;
	}

	/**
	 * Preserves the greatest remaining alias/canonical cooldown and clamps it to
	 * the new maximum. Registry IDs are untouched; this only migrates NBT keys.
	 */
	private static void migrateCooldownAliases(ServerPlayer player) {
		Map<String, List<String>> storedKeys = new LinkedHashMap<>();
		for (String nbtKey : new ArrayList<>(player.getPersistentData().getAllKeys())) {
			String rawSkill = null;
			if (nbtKey.startsWith(COOLDOWN_PREFIX))
				rawSkill = nbtKey.substring(COOLDOWN_PREFIX.length());
			else if (nbtKey.startsWith(FULL_COOLDOWN_PREFIX))
				rawSkill = nbtKey.substring(FULL_COOLDOWN_PREFIX.length());
			if (rawSkill == null)
				continue;
			String canonical = TankerProgressionRules.canonicalName(rawSkill);
			if (!TankerProgressionRules.isTankerSkill(canonical))
				continue;
			storedKeys.computeIfAbsent(canonical, ignored -> new ArrayList<>());
			if (!storedKeys.get(canonical).contains(rawSkill))
				storedKeys.get(canonical).add(rawSkill);
		}

		for (Map.Entry<String, List<String>> entry : storedKeys.entrySet()) {
			String canonical = entry.getKey();
			int maximum = MAX_COOLDOWN_TICKS.getOrDefault(canonical, 0);
			int greatestRemaining = Math.min(maximum,
					CooldownManager.getRemainingTicks(player, canonical));
			boolean hasAlias = false;
			for (String storedKey : entry.getValue()) {
				greatestRemaining = Math.min(maximum, Math.max(greatestRemaining,
						CooldownManager.getRemainingTicks(player, storedKey)));
				if (!canonical.equals(storedKey)) {
					hasAlias = true;
					CooldownManager.clear(player, storedKey);
				}
			}
			int canonicalRemaining = CooldownManager.getRemainingTicks(player, canonical);
			if (hasAlias || canonicalRemaining > maximum) {
				if (greatestRemaining > 0)
					CooldownManager.setFullDuration(player, canonical, greatestRemaining);
				else
					CooldownManager.clear(player, canonical);
			}
		}
	}

	private static CompoundTag persistedData(Player player) {
		CompoundTag root = player.getPersistentData();
		if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
		return root.getCompound(Player.PERSISTED_NBT_TAG);
	}
}
