package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.Entity;

/**
 * Keeps the legacy shadow-storage capability aligned with the advancement
 * milestones without synchronizing the capability every game tick.
 */
@Mod.EventBusSubscriber
public final class ShadowStorageTiersProcedure {
	private ShadowStorageTiersProcedure() {
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide()
				&& event.player.tickCount % 40 == Math.floorMod(event.player.getId(), 40))
			execute(event.player);
	}

	public static void execute(Entity entity) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = entity
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		if ((int) vars.JOB != 1)
			return;
		double target = vars.Level >= 120 ? 200
				: vars.Level >= 100 ? 150
				: vars.Level >= 90 ? 100
				: vars.Level >= 70 ? 40
				: 20;
		if (Double.compare(vars.shadowstorage, target) == 0)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.shadowstorage = target;
			capability.syncPlayerVariables(entity);
		});
	}
}
