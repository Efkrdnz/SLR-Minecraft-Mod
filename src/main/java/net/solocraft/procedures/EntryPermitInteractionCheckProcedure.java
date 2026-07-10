package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;

public class EntryPermitInteractionCheckProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_dkc")))
				&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared < 20) {
			FloorCreateNewProcedure.execute(world, entity);
		}
	}
}
