package net.solocraft.dungeon.data;

import net.solocraft.SololevelingMod;

import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge-bus registration for the server datapack snapshot. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
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
