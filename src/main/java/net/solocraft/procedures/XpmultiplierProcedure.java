package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.guild.GuildBuffManager;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

@EventBusSubscriber
public class XpmultiplierProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (world.dayTime() % 100 != 0)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			double xpMultiplier = GuildBuffManager.xpMultiplier(entity);
			if (capability.GuildCode != 0 || capability.xpmultiplier != xpMultiplier
					|| capability.manaregen != 0) {
				capability.GuildCode = 0;
				capability.xpmultiplier = xpMultiplier;
				capability.manaregen = 0;
				capability.syncPlayerVariables(entity);
			}
		});
	}
}
