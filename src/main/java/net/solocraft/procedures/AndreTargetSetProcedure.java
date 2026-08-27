package net.solocraft.procedures;

import net.solocraft.entity.ThomasAndreEntity;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class AndreTargetSetProcedure {
	@SubscribeEvent
	public static void onEntitySetsAttackTarget(LivingChangeTargetEvent event) {
		execute(event, event.getOriginalAboutToBeSetTarget());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof ThomasAndreEntity) {
			entity.setSprinting(true);
		}
	}
}
