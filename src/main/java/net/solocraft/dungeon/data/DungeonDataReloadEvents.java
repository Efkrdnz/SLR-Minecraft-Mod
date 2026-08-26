package net.solocraft.dungeon.data;

import net.solocraft.SololevelingMod;

import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/** Forge-bus registration for the server datapack snapshot. */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class DungeonDataReloadEvents {
	private DungeonDataReloadEvents() {
	}

	@SubscribeEvent
	public static void addReloadListener(AddReloadListenerEvent event) {
		event.addListener(DungeonDataManager.reloadListener(event.getConditionContext()));
	}

	@SubscribeEvent
	public static void serverStopped(ServerStoppedEvent event) {
		DungeonDataManager.clear();
	}
}
