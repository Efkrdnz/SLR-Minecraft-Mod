package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.TitleManager;

import net.minecraft.world.entity.Entity;

public class ReturnTitleProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		int title = (int) (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).title;
		return "\u00A7f\u00A7l" + "Title: " + TitleManager.displayName(title);
	}
}
