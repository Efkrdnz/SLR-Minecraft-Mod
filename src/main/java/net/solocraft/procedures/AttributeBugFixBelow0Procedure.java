package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class AttributeBugFixBelow0Procedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player);
		}
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			boolean changed = false;
			if (capability.Vitality < 0) {
				capability.Vitality = 0;
				changed = true;
			}
			if (capability.Strength < 0) {
				capability.Strength = 0;
				changed = true;
			}
			if (capability.Intelligence < 0) {
				capability.Intelligence = 0;
				changed = true;
			}
			if (capability.Speed < 0) {
				capability.Speed = 0;
				changed = true;
			}
			if (capability.Durability < 0) {
				capability.Durability = 0;
				changed = true;
			}
			if (capability.perception < 0) {
				capability.perception = 0;
				changed = true;
			}
			if (changed)
				capability.syncPlayerVariables(entity);
		});
	}
}
