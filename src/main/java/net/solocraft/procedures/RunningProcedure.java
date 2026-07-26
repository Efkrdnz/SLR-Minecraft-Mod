package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.TickEvent;

import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class RunningProcedure {
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
		if (capability == null || !capability.ActiveDaily
				|| capability.RUN >= DailyQuestHelper.runTarget(entity))
			return;
		double dx = entity.getX() - capability.RX;
		double dz = entity.getZ() - capability.RZ;
		if (Math.sqrt(dx * dx + dz * dz) < 1)
			return;

		double previousValue = capability.RUN;
		double newValue = previousValue + 1;
		capability.RX = entity.getX();
		capability.RZ = entity.getZ();
		capability.RUN = newValue;
		capability.syncPlayerVariables(entity);
		DailyQuestHelper.checkSecretTransition(entity, previousValue, newValue,
				DailyQuestHelper.NORMAL_RUN_TARGET);
	}
}
