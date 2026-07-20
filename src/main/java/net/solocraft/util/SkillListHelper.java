package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public class SkillListHelper {
	private static final List<String> JOB_SKILL_ORDER = List.of(
			JobSkillManager.ARISE,
			JobSkillManager.SHADOW_SUMMON,
			JobSkillManager.DISMISS_SHADOWS,
			JobSkillManager.SHADOW_COMMAND,
			JobSkillManager.SHADOW_EXCHANGE,
			JobSkillManager.SHADOW_MANIFESTATION,
			JobSkillManager.FIRE_CHARGE,
			JobSkillManager.METEOR_RAIN,
			JobSkillManager.FIREFLIES,
			JobSkillManager.ICE_SPEAR,
			JobSkillManager.FLASH_FREEZE,
			JobSkillManager.FROZEN_PATH,
			JobSkillManager.FROST_COUNTER,
			JobSkillManager.ABSOLUTE_ZERO,
			JobSkillManager.FROST_SPIRITUALIZATION,
			JobSkillManager.THOMAS_CAPTURE,
			JobSkillManager.THOMAS_POWER_SMASH,
			JobSkillManager.THOMAS_COLLAPSE,
			JobSkillManager.THOMAS_MANIFESTATION,
			JobSkillManager.LIU_HEAVENLY_COUNTER,
			JobSkillManager.LIU_GOLDEN_DRAGON_DANCE,
			JobSkillManager.LIU_SOVEREIGN_SWORD_DOMAIN,
			JobSkillManager.LIU_MANIFESTATION,
			JobSkillManager.LIGHTNING_BREATH,
			JobSkillManager.HELLSTORM_DOMINION,
			JobSkillManager.RADIRU_BLOOD_SPEAR,
			JobSkillManager.DOPPELGANGER,
			JobSkillManager.HELLS_ARMY,
			JobSkillManager.WHITE_FLAME_SPIRITUALIZATION,
			JobSkillManager.BEAST_CLAW_RIFT,
			JobSkillManager.BEAST_RUBBLE_JAW,
			JobSkillManager.BEAST_KINGS_MAUL,
			JobSkillManager.BEAST_RECONSTITUTION,
			JobSkillManager.BEAST_WHITE_FANG,
			JobSkillManager.MONARCH_BEAM,
			JobSkillManager.LIGHTNING_STORM,
			JobSkillManager.STORM_BURST);

	private SkillListHelper() {
	}

	public static String rawSkillAt(Entity entity, int position) {
		if (entity == null || position < 1)
			return "empty";
		List<String> skills = skills(entity);
		if (position > skills.size())
			return "empty";
		return skills.get(position - 1);
	}

	public static String displaySkillAt(Entity entity, int position) {
		String raw = rawSkillAt(entity, position);
		return "empty".equals(raw) ? "empty" : ShadowMonarchManager.displaySkillName(entity, raw);
	}

	public static int colorAt(Entity entity, int position) {
		String raw = rawSkillAt(entity, position);
		return ShadowMonarchManager.skillColor(entity, raw);
	}

	public static int skillCount(Entity entity) {
		return skills(entity).size();
	}

	public static int pageCount(Entity entity, int perPage) {
		return Math.max(1, (int) Math.ceil(skillCount(entity) / (double) perPage));
	}

	public static List<String> skills(Entity entity) {
		ArrayList<String> result = new ArrayList<>();
		if (entity == null)
			return result;
		String plistOriginal = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).Plist;
		if (plistOriginal == null || plistOriginal.isEmpty())
			return result;
		for (String item : plistOriginal.split(",")) {
			String skill = cleanSkill(item);
			if (!skill.isEmpty())
				result.add(skill);
		}
		return orderedForEquipList(result);
	}

	private static List<String> orderedForEquipList(List<String> skills) {
		ArrayList<String> result = new ArrayList<>();
		for (String jobSkill : JOB_SKILL_ORDER) {
			if (skills.contains(jobSkill))
				result.add(jobSkill);
		}
		for (String skill : skills) {
			if (!JobSkillManager.isJobSkill(skill))
				result.add(skill);
		}
		return result;
	}

	private static String cleanSkill(String item) {
		if (item == null)
			return "";
		String skill = item.trim();
		if (skill.startsWith("."))
			skill = skill.substring(1);
		return skill;
	}

}
