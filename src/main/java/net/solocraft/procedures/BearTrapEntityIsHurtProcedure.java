package net.solocraft.procedures;

import net.solocraft.entity.BearTrapEntity;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class BearTrapEntityIsHurtProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity);
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		execute(null, world, x, y, z, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof BearTrapEntity) {
			if (entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
				if (world instanceof Level _level && !_level.isClientSide()) {
					_level.explode((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null), x, y, z, 3, false, Level.ExplosionInteraction.NONE);
				}
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
	}
}
