package net.solocraft.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Classifies ordinary Hunter skills for compact, colored Skill List prefixes. */
public final class SkillCategoryRegistry {
	private static final Category ASSASSIN = new Category("A", 0x55DFFF);
	private static final Category FIGHTER = new Category("F", 0xFF5C5C);
	private static final Category TANKER = new Category("T", 0xEAF2FF);
	private static final Category HEALER = new Category("H", 0x58F28A);
	private static final Category RANGER = new Category("R", 0x71D83D);
	private static final Category ARCANE_MAGE = new Category("AM", 0xB66CFF);
	private static final Category FIRE_MAGE = new Category("FM", 0xFF6A32);
	private static final Category BARRIER_MAGE = new Category("BM", 0x62DFFF);
	private static final Category STORM_MAGE = new Category("ST", 0xFFD45A);
	private static final Category CURSE_MAGE = new Category("CM", 0xA05CFF);
	private static final Category FORBIDDEN = new Category("FB", 0xFF3434);

	private static final Map<String, Category> SKILLS = new HashMap<>();

	static {
		register(ASSASSIN, Set.of(
				AssassinSkillManager.GHOST_STEP, AssassinSkillManager.NIGHT_REND,
				AssassinSkillManager.STEALTH, AssassinSkillManager.FLASH_CUT, AssassinSkillManager.DUALWIELD,
				AssassinSkillManager.SHADOW_FEINT, AssassinSkillManager.SILENT_DOMAIN,
				AssassinSkillManager.ZERO_PRESENCE,
				"Critical Attack", "Mutilation", "Murderious Intent", "Dagger Throw", "Dagger Rush"));
		register(FIGHTER, Set.of(
				"Ground Slam", "Slash Dash", "Cross Strike", "Critical Strike", "Slash Fury",
				"Sword Dance", "Sword of Light", "Sword Beam",
				FighterSkillManager.IRON_KNUCKLE, FighterSkillManager.BREAKER_COMBO,
				FighterSkillManager.METEOR_FIST, FighterSkillManager.TITANS_BARRAGE,
				FighterSkillManager.RADIANT_EXECUTION, FighterSkillManager.MAGICAL_EYE,
				FighterSkillManager.CLAW_STRIKES, FighterSkillManager.BEAST_SENSE,
				FighterSkillManager.PARTIAL_TRANSFORMATION,
				FighterSkillManager.PREDATOR_RUSH,
				FighterSkillManager.FULL_BEAST_TRANSFORMATION));
		register(TANKER, Set.of(
				"Taunt", "Reinforcement", "Tank Leap", "Shield Bash", "Willpower",
				"Protection Mark",
				JuggernautSkillManager.HEAVY_BLOW, JuggernautSkillManager.IRON_BODY,
				JuggernautSkillManager.SEISMIC_GRAPPLE,
				JuggernautSkillManager.GIGANTIFICATION,
				JuggernautSkillManager.COLOSSUS_CHARGE,
				JuggernautSkillManager.MOUNTAIN_BREAKER));
		register(HEALER, Set.of(
				"Heal Beam", "Haste Buff", "Purification", "Physical Buff", "Overheal",
				"Blessing Mark",
				HealerSkillManager.HEALING_PULSE, HealerSkillManager.CAMOUFLAGE,
				HealerSkillManager.PURIFYING_WAVE, HealerSkillManager.GUARDIAN_STEP,
				HealerSkillManager.SANCTUARY, HealerSkillManager.SECOND_WIND,
				HealerSkillManager.GUARDIAN_WARD, HealerSkillManager.MANA_FONT,
				HealerSkillManager.VITALITY_SURGE, HealerSkillManager.DIVINE_FAVOR));
		register(RANGER, Set.of(
				RangerCombatManager.MANA_QUIVER, RangerCombatManager.BACK_STEP,
				RangerCombatManager.HAWKEYE, RangerCombatManager.RAPID_FIRE,
				RangerCombatManager.HYPER_FOCUS, RangerCombatManager.SHARPSHOOTER,
				RangerCombatManager.HIGH_VALUE_TARGET, RangerCombatManager.ARROW_SHOWER,
				RangerCombatManager.LEGACY_PROXIMITY_TRAP, "Detection"));
		register(FIRE_MAGE, FireMageSpellManager.FIRE_SKILLS);
		register(BARRIER_MAGE, BarrierMageSpellManager.BARRIER_SKILLS);
		register(ARCANE_MAGE, ArcaneMageSpellManager.ARCANE_SKILLS);
		register(STORM_MAGE, StormMageSpellManager.STORM_SKILLS);
		register(CURSE_MAGE, CurseMageSpellManager.CURSE_SKILLS);
		register(FORBIDDEN, Set.of("Cold Blood"));
	}

	private SkillCategoryRegistry() {
	}

	public static Component decorate(String rawSkill, String displayName) {
		if (rawSkill == null || displayName == null || JobSkillManager.isJobSkill(rawSkill)
				|| ShadowMonarchManager.isFormationSkill(rawSkill))
			return Component.literal(displayName == null ? "" : displayName);
		Category category = SKILLS.get(rawSkill);
		if (category == null)
			return Component.literal(displayName);

		MutableComponent result = Component.empty();
		result.append(Component.literal("("));
		result.append(Component.literal(category.code())
				.withStyle(style -> style.withColor(category.color()).withBold(true)));
		result.append(Component.literal(") "));
		result.append(Component.literal(displayName));
		return result;
	}

	private static void register(Category category, Set<String> skills) {
		for (String skill : skills)
			SKILLS.put(skill, category);
	}

	private record Category(String code, int color) {
	}
}
