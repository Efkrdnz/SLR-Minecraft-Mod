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
	private static final Category MAGE = new Category("M", 0xC18CFF);
	private static final Category ARCANE_MAGE = new Category("AM", 0xB66CFF);
	private static final Category FIRE_MAGE = new Category("FM", 0xFF6A32);
	private static final Category BARRIER_MAGE = new Category("BM", 0x62DFFF);
	private static final Category SUMMONING_MAGE = new Category("SM", 0xE66DFF);
	private static final Category CURSE_MAGE = new Category("CM", 0x9255E8);

	private static final Map<String, Category> SKILLS = new HashMap<>();

	static {
		register(ASSASSIN, Set.of(
				"Shadowstep", "Backstab", "Stealth", "Quickslashes", "Dualwield",
				"Critical Attack", "Mutilation", "Murderious Intent"));
		register(FIGHTER, Set.of(
				"Ground Slam", "Slash Dash", "Cross Strike", "Critical Strike", "Slash Fury",
				"Sword Dance", "Sword of Light", "Sword Beam"));
		register(TANKER, Set.of(
				"Taunt", "Reinforcement", "Tank Leap", "Shield Bash", "Willpower",
				"Protection Mark"));
		register(HEALER, Set.of(
				"Heal Beam", "Haste Buff", "Purification", "Physical Buff", "Overheal",
				"Blessing Mark"));
		register(RANGER, Set.of(
				"Back Step", "Hawkeye", "Hyper Focus", "Sharpshooter", "High Value Target",
				"Proximity Trap", "Detection"));
		register(FIRE_MAGE, FireMageSpellManager.FIRE_SKILLS);
		register(BARRIER_MAGE, BarrierMageSpellManager.BARRIER_SKILLS);
		register(MAGE, Set.of("Magic Missiles", "Lightball", "Light Ball", "Water Slash"));
		register(ARCANE_MAGE, Set.of(
				"Aether Bolt", "Vector Step",
				"Polarity Sphere", "Runic Relay", "Astral Arsenal", "Dimensional Rend",
				"Grand Formula: Convergence"));
		register(SUMMONING_MAGE, Set.of("Light Golem"));
		register(CURSE_MAGE, Set.of("Curse Sphere", "Curse Smoke", "Curse Chains"));
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
