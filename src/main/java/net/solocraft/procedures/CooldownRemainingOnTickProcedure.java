package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.CooldownManager;
import net.solocraft.util.JobSkillManager;

import net.minecraft.world.entity.Entity;

/**
 * Returns the remaining cooldown seconds for the player's currently selected power,
 * or an empty string if the ability is ready.
 *
 * "Back Step" is a charge-based ability and uses a separate capability variable,
 * so it is handled separately here.
 */
public class CooldownRemainingOnTickProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";

		var cap = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		String power = normalizeCooldownKey(cap.PselectedPower);

		// Back Step uses a charge system, not a simple cooldown
		if (power.equals("Back Step")) {
			int charges = (int) cap.rangerleapnum;
			if (charges < 3) {
				return "Charges: " + charges + " / CD:" + (int) (cap.rangerleaptimer / 20);
			}
			return "Charges: " + charges;
		}

		int remaining = CooldownManager.getRemainingSeconds(entity, power);
		return remaining > 0 ? String.valueOf(remaining) : "";
	}

	private static String normalizeCooldownKey(String power) {
		if (JobSkillManager.isJobSkill(power))
			return JobSkillManager.cooldownKey(power);
		return power.equals("Critical Strike") ? "Cross Strike" : power;
	}
}
