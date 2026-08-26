package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

import javax.annotation.Nullable;

@EventBusSubscriber
public class AuraEffectGeneralProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.PHYSICAL_BUFF)) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.PHYSICAL_BUFF_PARTICLE.get()), (entity.getX()), (entity.getY() + entity.getBbHeight() / 2), (entity.getZ()), 5, (entity.getBbWidth() * 0.75),
						(entity.getBbHeight() / 2), (entity.getBbWidth() * 0.75), 1);
		}
		if (entity instanceof LivingEntity _livEnt9 && _livEnt9.hasEffect(SololevelingModMobEffects.HASTE_BUFF)) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.HASTE_BUFF_PARTICLE.get()), (entity.getX()), (entity.getY() + entity.getBbHeight() / 2), (entity.getZ()), 5, (entity.getBbWidth() * 0.75),
						(entity.getBbHeight() / 2), (entity.getBbWidth() * 0.75), 1);
		}
	}
}
