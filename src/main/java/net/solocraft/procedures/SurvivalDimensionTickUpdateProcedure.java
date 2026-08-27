package net.solocraft.procedures;

import net.solocraft.util.daily.DailyPunishmentManager;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/** Keeps the generated procedure entry point while delegating to safe logic. */
@EventBusSubscriber
public final class SurvivalDimensionTickUpdateProcedure {
	private SurvivalDimensionTickUpdateProcedure() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true
				&& event.getEntity() instanceof ServerPlayer player)
			DailyPunishmentManager.tick(player);
	}

	public static void execute(LevelAccessor world, double x, double y,
			double z, Entity entity) {
		if (entity instanceof ServerPlayer player && !world.isClientSide())
			DailyPunishmentManager.tick(player);
	}
}
