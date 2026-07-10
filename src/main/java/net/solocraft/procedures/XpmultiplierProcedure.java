package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.guild.GuildBuffManager;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class XpmultiplierProcedure {
	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase == TickEvent.Phase.END) {
			execute(event, event.player.level(), event.player);
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
			capability.GuildCode = 0;
			capability.xpmultiplier = GuildBuffManager.xpMultiplier(entity);
			capability.manaregen = 0;
			capability.syncPlayerVariables(entity);
		});
	}
}
