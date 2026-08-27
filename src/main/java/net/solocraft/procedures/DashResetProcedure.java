package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.TemporaryStatBonusManager;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import net.solocraft.util.CooldownManager;

@EventBusSubscriber
public class DashResetProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity());
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
			capability.MP -= Math.round(2 + TemporaryStatBonusManager.effectiveIntelligence(entity) / 30);
			capability.syncPlayerVariables(entity);
			CooldownManager.set(entity, "mana_refresh", 20);
		} else if (capability.dash == 1.5) {
			capability.MP -= Math.round(4 + TemporaryStatBonusManager.effectiveIntelligence(entity) / 30);
			capability.syncPlayerVariables(entity);
			CooldownManager.set(entity, "mana_refresh", 20);
		}
	}
}
