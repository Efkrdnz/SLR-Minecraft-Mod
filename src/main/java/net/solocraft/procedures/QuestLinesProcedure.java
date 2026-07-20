package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.world.entity.Entity;

public class QuestLinesProcedure {
	public static String execute(Entity entity) {
		if (entity == null)
			return "";
		if (JobChangeQuestManager.isDungeonActive(entity))
			return "Defeat Igris the Blood-Red.";
		if (JobChangeQuestManager.isAdvancementActive(entity))
			return "Defeat summoned knights: " + JobChangeQuestManager.advancementPoints(entity) + "/" + JobChangeQuestManager.requiredPoints(entity);
		if (JobChangeQuestManager.isSelectionPending(entity))
			return "Select a Ruler or Monarch vessel.";
		if (JobChangeQuestManager.isShadowPresentation(entity))
			return "Job assignment in progress...";
		if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("Getting Stronger")
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).QuestProgression == 0) {
			return "Obtain the instance dungeon key";
		} else if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("Getting Stronger")
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).QuestProgression == 1) {
			return "Enter the instance dungeon";
		} else if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("Getting Stronger")
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).QuestProgression == 2) {
			return "Complete the instance dungeon!";
		} else if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("Something Is Weird")
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).QuestProgression == 0) {
			return "Work in progress...";
		} else if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("Something Is Weird")
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).QuestProgression == 1) {
			return "Work in progress...";
		} else if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("Something Is Weird")
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).QuestProgression == 2) {
			return "Work in progress...";
		}
		return "";
	}
}
