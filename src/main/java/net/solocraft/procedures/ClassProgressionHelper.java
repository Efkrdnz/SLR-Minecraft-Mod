package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Server-side bridge between {@link ClassProgressionRules} and the capability.
 *
 * <p>It mirrors {@link TankerProgressionHelper}: canonicalize owned tokens
 * first, then add only the entitlements the hunter has earned, and never remove
 * a skill. Skills learned from a runestone or another class therefore survive
 * reconciliation untouched.</p>
 */
public final class ClassProgressionHelper {
	private ClassProgressionHelper() {
	}

	/**
	 * Grants every ability the hunter's rank entitles them to, deterministically.
	 * Safe to call repeatedly; it is a reconciliation, not an award.
	 */
	public static void reconcileRankEntitlements(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(vars -> {
					ClassProgressionRules rules = rulesFor(vars);
					if (rules == null)
						return;
					boolean changed = migrateVariables(vars, rules);
					int rank = boundedRank(vars.HunterRank);
					for (String skill : rules.entitlementsForRank(
							styleKey(vars), rank))
						changed |= ensureSkill(vars, rules, skill);
					if (changed)
						vars.syncPlayerVariables(entity);
				});
	}

	/**
	 * Grants the next missing ability in mastery order and returns its name, or
	 * "" when the tree is already complete. The random walk this replaces could
	 * spin for many iterations and had no defined award order.
	 */
	public static String grantNextMasterySkill(Entity entity) {
		if (entity == null)
			return "";
		final String[] granted = {""};
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(vars -> {
					ClassProgressionRules rules = rulesFor(vars);
					if (rules == null)
						return;
					boolean changed = migrateVariables(vars, rules);
					String missing = rules.firstMissingSkill(
							styleKey(vars), vars.Plist);
					if (!missing.isEmpty()) {
						changed |= ensureSkill(vars, rules, missing);
						granted[0] = missing;
					}
					if (changed)
						vars.syncPlayerVariables(entity);
				});
		if (!granted[0].isEmpty() && entity instanceof Player player
				&& !player.level().isClientSide())
			player.displayClientMessage(
					Component.literal("Gained skill: " + granted[0]), false);
		return granted[0];
	}

	/**
	 * Canonicalizes owned tokens without adding entitlements. Safe for a
	 * runestone that grants one named ability at any rank.
	 */
	public static void reconcileAliases(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(vars -> {
					ClassProgressionRules rules = rulesFor(vars);
					if (rules != null && migrateVariables(vars, rules))
						vars.syncPlayerVariables(entity);
				});
	}

	private static ClassProgressionRules rulesFor(
			SololevelingModVariables.PlayerVariables vars) {
		return ClassProgressionRules.forClassId((int) Math.round(vars.Classes));
	}

	/**
	 * A hunter with no accepted style uses the class default tree. Styles stay
	 * gated in {@code ClassStyleRules} until their abilities exist, so this is
	 * the path every Assassin, Fighter and Healer takes today.
	 */
	private static String styleKey(
			SololevelingModVariables.PlayerVariables vars) {
		return vars.classStyle == null ? "" : vars.classStyle;
	}

	private static int boundedRank(double rawRank) {
		return Math.max(1, Math.min(6, (int) Math.round(rawRank)));
	}

	private static boolean migrateVariables(
			SololevelingModVariables.PlayerVariables vars,
			ClassProgressionRules rules) {
		boolean changed = false;
		String migratedList = rules.canonicalizeSkillList(vars.Plist);
		if (!migratedList.equals(vars.Plist)) {
			vars.Plist = migratedList;
			changed = true;
		}

		String selected = rules.canonicalizeReference(vars.PselectedPower);
		if (!selected.equals(vars.PselectedPower)) {
			vars.PselectedPower = selected;
			changed = true;
		}
		for (int slot = 1; slot <= 16; slot++) {
			String current = SkillSlotHelper.getSlot(vars, slot);
			String canonical = rules.canonicalizeReference(current);
			if (!canonical.equals(current)) {
				SkillSlotHelper.setSlot(vars, slot, canonical);
				changed = true;
			}
		}
		return changed;
	}

	private static boolean ensureSkill(
			SololevelingModVariables.PlayerVariables vars,
			ClassProgressionRules rules, String skill) {
		String updated = rules.ensureSkill(vars.Plist, skill);
		if (updated.equals(vars.Plist))
			return false;
		vars.Plist = updated;
		return true;
	}
}
