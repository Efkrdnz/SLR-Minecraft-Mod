package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

final class ShadowSummonProcedureHelper {
	private static final ResourceKey<Level> SURVIVAL_DIMENSION = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:survival_dimension"));

	private ShadowSummonProcedureHelper() {
	}

	static void execute(LevelAccessor world, double x, double y, double z, Entity entity, String type) {
		if (entity == null)
			return;
		if (SURVIVAL_DIMENSION.equals(entity.level().dimension())) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("Your Job Abilities are Disabled in this dimension!"), true);
			return;
		}
		SololevelingModVariables.PlayerVariables variables = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (variables.JOB != 1)
			return;
		ShadowMonarchManager.summonType(world, x, y, z, entity, type);
	}
}
