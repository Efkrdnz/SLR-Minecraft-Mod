package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class PortalSpawnProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:survival_dimension")))) {
			if (!entity.level().isClientSide())
				entity.discard();
		}
		if ((entity.level().dimension()) == Level.OVERWORLD) {
			if (world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_GATE_NOTIFICATION)) {
				if (!world.isClientSide() && world.getServer() != null)
					world.getServer().getPlayerList().broadcastSystemMessage(Component.literal(("\u00A7c" + "A portal appeared at X: \"" + "\u00A7f" + Math.round(entity.getX()) + "\" Y:\" " + "\u00A7f" + Math.round(entity.getY()) + "\" Z:\" "
							+ "\u00A7f" + Math.round(entity.getZ()) + "\u00A7c" + " \"It will Disappear in 1 DAY!")), false);
			}
			SololevelingModVariables.MapVariables.get(world).gatetimer = 0;
			SololevelingModVariables.MapVariables.get(world).syncData(world);
			entity.getPersistentData().putDouble("PortalLife", 0);
			entity.getPersistentData().putDouble("tpx", (Mth.nextInt(RandomSource.create(), -299999, 299999)));
			entity.getPersistentData().putDouble("tpy", (Mth.nextInt(RandomSource.create(), 60, 120)));
			entity.getPersistentData().putDouble("tpz", (Mth.nextInt(RandomSource.create(), -299999, 299999)));
		} else {
			if (!entity.level().isClientSide())
				entity.discard();
		}
	}
}
