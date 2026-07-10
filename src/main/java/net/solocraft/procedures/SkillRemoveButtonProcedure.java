package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class SkillRemoveButtonProcedure {
	public static void execute(Entity entity, int slot) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			SkillSlotHelper.setSlot(capability, slot, "");
			capability.syncPlayerVariables(entity);
		});
	}
}
