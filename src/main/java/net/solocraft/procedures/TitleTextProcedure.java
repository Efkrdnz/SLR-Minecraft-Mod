package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class TitleTextProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if ("thomas_andre".equals(vars.vesselIdentity)) {
			return "Goliath";
		}
		if (vars.JOB == 1) {
			return "Shadow Monarch";
		} else if (vars.JOB == 2) {
			return "Grand Mage";
		} else if (vars.JOB == 3) {
			return "Frost Monarch";
		} else if (vars.JOB == 3) {
			return "Demon Monarch";
		}
		return "none";
	}
}
