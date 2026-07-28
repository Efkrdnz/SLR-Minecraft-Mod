package net.solocraft.util;

import java.util.List;
import java.util.Set;

/**
 * Identifies Solo Leveling runtime data stored outside PlayerVariables.
 *
 * <p>The player's Forge persistent-data compound is shared with every mod, so a
 * character reset must never clear it wholesale. Story/Cartenon state and
 * social reservations are intentionally retained because their authoritative
 * state lives outside the character progression capability.</p>
 */
public final class PlayerResetKeyPolicy {
	private static final String SHADOW_RESET_GENERATION = "sl_shadow_reset_generation";
	private static final String FORGIVING_DEATH_SNAPSHOT = "slr_forgiving_death_snapshot";
	private static final Set<String> CONSUMED_ENTITLEMENTS = Set.of(
			"slr_tanker_starter_redeemed_v1",
			"slr_instance_dungeon_key_claimed",
			"sl_urgent_pvp_first_reward_claimed",
			"SLRKangTaeshikAmbushCompleted");

	private static final List<String> PRESERVED_PREFIXES = List.of(
			"slr_story_intro_",
			"slr_cartenon_",
			"slr_party_",
			"slr_guild_gate_reserved",
			"slr_runestone_skill_");

	private static final List<String> RESET_PREFIXES = List.of(
			"slr_",
			"sl_",
			"sololeveling",
			"solocraft_",
			"cd_",
			"dkc_",
			"radiru_",
			"mowf_");

	private static final Set<String> RESET_EXACT_KEYS = Set.of(
			"dungeon_tag",
			"mage_casting",
			"mage_qte_zone_start",
			"Critical_Attack_Targetting",
			"CriticalAttackTarget",
			"Mutilation_Targetting",
			"MutilationTarget");

	private PlayerResetKeyPolicy() {
	}

	public static boolean shouldClear(String key) {
		if (key == null || key.isBlank() || SHADOW_RESET_GENERATION.equals(key)
				|| PlayerEntryGenerationGuard.GENERATION_TAG.equals(key)
				|| TemporaryStatBonusMigration.MIGRATION_RECEIPT.equals(key)
				|| TemporaryArmorSessionManager.GENERATION_TAG.equals(key)
				|| TemporaryArmorSessionManager.ACTIVE_ESCROW_TAG.equals(key)
				|| TemporaryArmorSessionManager.EQUIPPED_ESCROW_TAG.equals(key)
				|| FORGIVING_DEATH_SNAPSHOT.equals(key)
				|| CONSUMED_ENTITLEMENTS.contains(key))
			return false;
		for (String prefix : PRESERVED_PREFIXES)
			if (key.startsWith(prefix))
				return false;
		if (RESET_EXACT_KEYS.contains(key))
			return true;
		for (String prefix : RESET_PREFIXES)
			if (key.startsWith(prefix))
				return true;
		return false;
	}
}
