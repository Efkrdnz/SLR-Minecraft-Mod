package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class AuraEffectGeneralProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player);
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.PHYSICAL_BUFF.get())) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.PHYSICAL_BUFF_PARTICLE.get()), (entity.getX()), (entity.getY() + entity.getBbHeight() / 2), (entity.getZ()), 5, (entity.getBbWidth() * 0.75),
						(entity.getBbHeight() / 2), (entity.getBbWidth() * 0.75), 1);
		}
		if (entity instanceof LivingEntity _livEnt9 && _livEnt9.hasEffect(SololevelingModMobEffects.HASTE_BUFF.get())) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.HASTE_BUFF_PARTICLE.get()), (entity.getX()), (entity.getY() + entity.getBbHeight() / 2), (entity.getZ()), 5, (entity.getBbWidth() * 0.75),
						(entity.getBbHeight() / 2), (entity.getBbWidth() * 0.75), 1);
		}
	}
}
