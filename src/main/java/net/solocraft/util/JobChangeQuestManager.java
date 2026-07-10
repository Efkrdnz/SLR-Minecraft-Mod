package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

public final class JobChangeQuestManager {
	public static final String QUEST_ID = "job_change";
	private static final String TOKEN = QUEST_ID + ",";
	private static final int ACCENT = 0xFF7A5CFF;

	private JobChangeQuestManager() {
	}

	public static boolean isUnlocked(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		return vars.JOB == 1 || contains(vars.unlocked_quests, QUEST_ID);
	}

	public static boolean isFinished(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		return vars.JOB == 1 || contains(vars.finished_quests, QUEST_ID);
	}

	public static boolean isVisible(Entity entity) {
		return isUnlocked(entity) && !isFinished(entity);
	}

	public static void unlock(Entity entity) {
		if (entity == null || isFinished(entity))
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (!contains(capability.unlocked_quests, QUEST_ID))
				capability.unlocked_quests = append(capability.unlocked_quests);
			capability.jobkey = true;
			capability.syncPlayerVariables(entity);
		});
		if (entity instanceof ServerPlayer player)
			SystemNotifications.showTitleUnder(player, ACCENT, 100, Component.literal("\u00A7b\u00A7lQUEST UNLOCKED"), Component.literal("\u00A75Job Change Quest"));
	}

	public static void finish(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (!contains(capability.unlocked_quests, QUEST_ID))
				capability.unlocked_quests = append(capability.unlocked_quests);
			if (!contains(capability.finished_quests, QUEST_ID))
				capability.finished_quests = append(capability.finished_quests);
			capability.syncPlayerVariables(entity);
		});
	}

	public static void unlockIfEligible(LevelAccessor world, Entity entity, int requiredLevel) {
		if (entity == null || world == null)
			return;
		SololevelingModVariables.PlayerVariables vars = vars(entity);
		if (vars.JOB == 1) {
			finish(entity);
			return;
		}
		if (vars.Player && vars.Level >= requiredLevel && vars.JOB == 0 && !contains(vars.finished_quests, QUEST_ID) && !contains(vars.unlocked_quests, QUEST_ID))
			unlock(entity);
	}

	private static SololevelingModVariables.PlayerVariables vars(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static boolean contains(String list, String id) {
		return list != null && list.contains(id + ",");
	}

	private static String append(String list) {
		if (list == null || list.equals("\"\""))
			return TOKEN;
		return list + TOKEN;
	}
}
