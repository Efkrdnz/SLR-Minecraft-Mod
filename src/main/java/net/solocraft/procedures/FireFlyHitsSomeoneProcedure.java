package net.solocraft.procedures;

import net.solocraft.entity.FireFlyEntity;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FireFlyHitsSomeoneProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof FireFlyEntity) {
			entity.igniteForSeconds(5);
			if (!sourceentity.level().isClientSide())
				sourceentity.discard();
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.FLAME, x, y, z, 5, 3, 3, 3, 1);
			if (world instanceof Level _level && !_level.isClientSide())
				// The firefly, not nobody. It has already been discarded, which the
				// explosion does not mind, and attributing it is what lets the item
				// guard recognise this as one of ours.
				_level.explode(sourceentity, x, y, z, 1, Level.ExplosionInteraction.NONE);
		}
	}
}
