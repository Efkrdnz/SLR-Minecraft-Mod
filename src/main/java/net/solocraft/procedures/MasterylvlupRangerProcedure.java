package net.solocraft.procedures;

import net.solocraft.util.RangerCombatManager;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import java.util.List;

/** Deterministic Ranger mastery progression; no random duplicate rolls. */
public final class MasterylvlupRangerProcedure {
	private static final List<String> ORDER = List.of(
			RangerCombatManager.BACK_STEP,
			RangerCombatManager.HAWKEYE,
			RangerCombatManager.RAPID_FIRE,
			RangerCombatManager.HIGH_VALUE_TARGET,
			RangerCombatManager.SHARPSHOOTER,
			RangerCombatManager.ARROW_SHOWER,
			RangerCombatManager.HYPER_FOCUS);

	private MasterylvlupRangerProcedure() {
	}

	public static void execute(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		RangerCombatManager.reconcileRanger(player);
		for (String skill : ORDER) {
			if (RangerCombatManager.hasSkill(player, skill))
				continue;
			if (RangerCombatManager.grantSkill(player, skill))
				player.displayClientMessage(Component.translatable(
						"message.sololeveling.ranger.skill_gained", skill), false);
			return;
		}
	}
}
