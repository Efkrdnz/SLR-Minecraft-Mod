package net.solocraft.api.skill;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;

import net.solocraft.network.SololevelingModVariables;

/**
 * The skills a hunter has learned.
 *
 * <p>Skills are stored as one comma-separated string of display names, which is
 * how every runestone in the mod has always taught one. This wraps that format so
 * an addon does not have to know it -- and so the format stays free to change
 * without breaking every addon that learned to append a comma.
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
			String name = part.trim();
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
		// Trailing comma, matching the format every existing runestone writes, so
		// old and new entries stay indistinguishable to the rest of the mod.
		StringBuilder builder = new StringBuilder();
		for (String skill : skills)
			builder.append(skill).append(',');
		String joined = builder.toString();

		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.Plist = joined;
					capability.syncPlayerVariables(entity);
				});
	}

	/** Commas separate entries, so one inside a name would split it in two. */
	private static String normalise(String skill) {
		return skill == null ? "" : skill.replace(",", "").trim();
	}

	private static SololevelingModVariables.PlayerVariables variablesOf(Entity entity) {
		if (entity == null)
			return null;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
