package net.solocraft.procedures;

import net.solocraft.entity.VulcanEntity;
import net.solocraft.entity.DemonEntity;
import net.solocraft.entity.DemonKnightEntity;
import net.solocraft.entity.CerberusEntity;
import net.solocraft.entity.BaranEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DemonKilledDKCProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceEntity) {
		execute(null, world, x, y, z, entity, sourceEntity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceEntity) {
		if (entity == null)
			return;
		if (!world.isClientSide()) {
			if (entity instanceof DemonEntity || entity instanceof DemonKnightEntity) {
				DKCKillCounterProcedure.execute(world, entity, sourceEntity);
			}
			if (entity instanceof CerberusEntity) {
				DKCBossKillRewardProcedure.execute(world, x, y, z, entity, sourceEntity);
			}
			if (entity instanceof VulcanEntity) {
				DKCBossKillRewardProcedure.execute(world, x, y, z, entity, sourceEntity);
			}
			if (entity instanceof BaranEntity) {
				DKCBossKillRewardProcedure.execute(world, x, y, z, entity, sourceEntity);
			}
		}
	}
}
