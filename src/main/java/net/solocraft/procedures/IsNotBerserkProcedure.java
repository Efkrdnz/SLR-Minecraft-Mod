package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;

public class IsNotBerserkProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null || !ShadowMonarchManager.shouldFollowOwner(entity))
			return false;
		if (!(entity instanceof TamableAnimal tame) || !tame.isTame())
			return false;
		Entity owner = tame.getOwner();
		return owner != null && owner.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> !capability.berserk)
				.orElse(false);
	}
}
