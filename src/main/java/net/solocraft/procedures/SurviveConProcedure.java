package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

public class SurviveConProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:survival_dimension")))) {
			return true;
		}
		return false;
	}
}
