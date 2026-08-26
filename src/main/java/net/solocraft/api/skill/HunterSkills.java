package net.solocraft.api.skill;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;

import net.solocraft.network.SololevelingModVariables;

/**
 * The skills a hunter has learned.
 *
 * <p>Skills are stored as one comma-separated string of display names carrying a
 * leading {@code "."} sentinel -- {@code ".Skill A,Skill B,"} -- which is how
 * every runestone in the mod has always taught one, and what
 * {@code ClassProgressionRules.canonicalizeSkillList} rewrites the field back to.
 * A bare {@code "."} is the empty list, and is the field's default.
 *
 * <p>This wraps that format so an addon does not have to know it. Writing without
 * the sentinel looks fine until the next canonicalisation pass puts it back, at
 * which point the first skill reads as {@code ".Grave Spiritualization"} and the
 * hunter is told they never learned it.
 *
 * <p>One difference from the older inline code worth knowing: membership here is
 * an exact match on a whole entry, not a substring test. A skill called
 * {@code Rend} no longer counts as learned because {@code Night Rend} is in the
 * list.
 */
public final class HunterSkills {
	private HunterSkills() {
	}

	/** Every skill the hunter has learned, in the order they were learned. */
	public static List<String> learned(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		if (variables == null || variables.Plist == null || variables.Plist.isBlank())
			return List.of();

		List<String> skills = new ArrayList<>();
		for (String part : variables.Plist.split(",")) {
			String name = normalise(part);
			if (!name.isEmpty() && !skills.contains(name))
				skills.add(name);
		}
		return List.copyOf(skills);
	}

	/** Exact whole-entry match, so one skill name cannot shadow a longer one. */
	public static boolean hasLearned(Entity entity, String skill) {
		String name = normalise(skill);
		return !name.isEmpty() && learned(entity).contains(name);
	}

	/**
	 * Teaches a skill. Server-side.
	 *
	 * <p>The usual caller is a runestone's right-click handler, which should also
	 * consume the stone only when this returns true.
	 *
	 * @return false when the name is unusable or the hunter already knew it
	 */
	public static boolean learn(Entity entity, String skill) {
		String name = normalise(skill);
		if (entity == null || name.isEmpty() || variablesOf(entity) == null)
			return false;

		List<String> skills = new ArrayList<>(learned(entity));
		if (skills.contains(name))
			return false;
		skills.add(name);
		write(entity, skills);
		return true;
	}

	/** @return false when the hunter did not know it */
	public static boolean forget(Entity entity, String skill) {
		String name = normalise(skill);
		if (entity == null || name.isEmpty() || variablesOf(entity) == null)
			return false;

		List<String> skills = new ArrayList<>(learned(entity));
		if (!skills.remove(name))
			return false;
		write(entity, skills);
		return true;
	}

	private static void write(Entity entity, List<String> skills) {
		// Leading "." sentinel and a trailing comma, matching the format every
		// existing runestone writes and canonicalizeSkillList rewrites back to, so
		// old and new entries stay indistinguishable to the rest of the mod.
		StringBuilder builder = new StringBuilder(".");
		for (String skill : skills)
			builder.append(skill).append(',');
		String joined = builder.toString();

		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.Plist = joined;
					capability.syncPlayerVariables(entity);
				});
	}

	/**
	 * Commas separate entries, so one inside a name would split it in two, and
	 * the first entry carries the list's leading {@code "."} sentinel.
	 */
	private static String normalise(String skill) {
		if (skill == null)
			return "";
		String name = skill.replace(",", "").trim();
		while (name.startsWith("."))
			name = name.substring(1).trim();
		return name;
	}

	private static SololevelingModVariables.PlayerVariables variablesOf(Entity entity) {
		if (entity == null)
			return null;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
