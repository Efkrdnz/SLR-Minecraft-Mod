package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.CooldownManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class SwordDanceOnKillProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(Entity entity, Entity sourceentity) {
		execute(null, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double rand = 0;
		if (sourceentity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.SWORD_DANCE.get())) {
			CooldownManager.set(sourceentity, "dance",
					(int) ((entity instanceof LivingEntity _livEnt && _livEnt.hasEffect(SololevelingModMobEffects.SWORD_DANCE.get()) ? _livEnt.getEffect(SololevelingModMobEffects.SWORD_DANCE.get()).getDuration() : 0) + 20));
		}
	}
}
