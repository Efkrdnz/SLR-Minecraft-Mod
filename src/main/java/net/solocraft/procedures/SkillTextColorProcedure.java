package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.entity.Entity;

public class SkillTextColorProcedure {
	public static int execute(Entity entity) {
		if (entity == null)
			return -26266;
		String selectedPower = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).PselectedPower;
		int skillColor = ShadowMonarchManager.skillColor(entity, selectedPower);
		return skillColor == 0xFFFFFF ? -26266 : skillColor;
	}
}
