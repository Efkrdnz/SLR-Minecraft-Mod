package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.TrueMonarchRules;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.entity.Entity;

/**
 * Keeps the legacy shadow-storage capability aligned with the advancement
 * milestones without synchronizing the capability every game tick.
 */
@EventBusSubscriber
public final class ShadowStorageTiersProcedure {
	private ShadowStorageTiersProcedure() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true && !event.getEntity().level().isClientSide()
				&& event.getEntity().tickCount % 40 == Math.floorMod(event.getEntity().getId(), 40))
			execute(event.getEntity());
	}

	public static void execute(Entity entity) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = entity
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		if ((int) vars.JOB != 1)
			return;
		// The Black Heart removes the ceiling. This runs every 40 ticks and would
		// otherwise stamp the tier back over it two seconds after it was granted.
		if (TrueMonarchRules.hasUnlimitedShadowStorage(vars.trueMonarchHeart))
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
