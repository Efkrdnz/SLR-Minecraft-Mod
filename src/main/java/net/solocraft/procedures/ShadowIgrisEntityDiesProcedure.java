package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;

public class ShadowIgrisEntityDiesProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if (!(entity instanceof TamableAnimal tame) || !tame.isTame())
			return;
		Entity owner = tame.getOwner();
		if (owner == null)
			return;
		owner.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					capability.IgrisSpawned = 0;
					capability.syncPlayerVariables(owner);
				});
	}
}
