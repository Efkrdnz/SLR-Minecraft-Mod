package net.solocraft.procedures;

import net.solocraft.entity.BloodRedComIgrisEntity;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class BloodRedComIgrisOnInitialEntitySpawnProcedure {
	@SubscribeEvent
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof BloodRedComIgrisEntity) {
			if (entity instanceof BloodRedComIgrisEntity animatable)
				animatable.setTexture("igris_marcus");
		}
	}
}
