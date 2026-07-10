package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class ShadowEntityHurtOwnerOrEachotherProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity, event.getSource().getEntity());
		}
	}

	public static void execute(Entity entity, Entity sourceentity) {
		execute(null, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (ShadowMonarchManager.haveSameShadowOwner(sourceentity, entity)) {
			if (event != null && event.isCancelable()) {
				event.setCanceled(true);
			}
		}
		if (entity instanceof LivingEntity owner && ShadowMonarchManager.isOwnedShadow(sourceentity, owner)) {
			if (event != null && event.isCancelable()) {
				event.setCanceled(true);
			}
		}
	}
}
