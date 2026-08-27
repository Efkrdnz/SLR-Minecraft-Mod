package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;

import javax.annotation.Nullable;

@EventBusSubscriber
public class MonarchOfWhiteFlamesPassiveProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, event.getSource(), entity);
		}
	}

	public static void execute(DamageSource damagesource, Entity entity) {
		execute(null, damagesource, entity);
	}

	private static void execute(@Nullable ICancellableEvent event, DamageSource damagesource, Entity entity) {
		if (damagesource == null || entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 4) {
			if ((damagesource).is(DamageTypes.IN_FIRE) || (damagesource).is(DamageTypes.ON_FIRE) || (damagesource).is(DamageTypes.LAVA)) {
				if (event != null) {
					event.setCanceled(true);
				}
			}
		}
	}
}
