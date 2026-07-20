package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.world.entity.Entity;

public class ReturnQuestNameProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (JobChangeQuestManager.isDungeonActive(entity) || JobChangeQuestManager.isAdvancementActive(entity)
				|| JobChangeQuestManager.isSelectionPending(entity) || JobChangeQuestManager.isShadowPresentation(entity))
			return "Job Change Quest";
		if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("")) {
			return (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest;
		}
		return (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest;
	}
}
