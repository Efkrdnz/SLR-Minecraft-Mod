package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import net.solocraft.util.CooldownManager;

@Mod.EventBusSubscriber
public class DashResetProcedure {
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
		SololevelingModVariables.PlayerVariables capability = entity.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(null);
		if (capability == null)
			return;
		if (capability.MP < 100 && capability.dash != 1) {
			capability.dash = 1;
			capability.syncPlayerVariables(entity);
		}
		if (capability.dash == 1.3) {
			capability.MP -= Math.round(2 + capability.Intelligence / 30);
			capability.syncPlayerVariables(entity);
			CooldownManager.set(entity, "mana_refresh", 20);
		} else if (capability.dash == 1.5) {
			capability.MP -= Math.round(4 + capability.Intelligence / 30);
			capability.syncPlayerVariables(entity);
			CooldownManager.set(entity, "mana_refresh", 20);
		}
	}
}
