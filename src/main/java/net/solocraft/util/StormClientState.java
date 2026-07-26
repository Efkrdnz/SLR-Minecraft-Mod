package net.solocraft.util;

import net.solocraft.network.StormStateMessage;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Client-only snapshot used to render the server-authoritative Storm Current. */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class StormClientState {
	public static volatile boolean hasAccess;
	public static volatile int voltage;
	public static volatile int effectiveStage = 1;
	public static volatile boolean overcharged;
	public static volatile boolean rodActive;
	public static volatile boolean tempestActive;
	public static volatile boolean spiritualizationBonus;

	private StormClientState() {
	}

	public static void update(StormStateMessage message) {
		hasAccess = message.hasAccess;
		voltage = Math.max(0, Math.min(100, message.voltage));
		effectiveStage = Math.max(1, Math.min(6, message.effectiveStage));
		overcharged = message.overcharged;
		rodActive = message.rodActive;
		tempestActive = message.tempestActive;
		spiritualizationBonus = message.spiritualizationBonus;
	}

	public static void clear() {
		hasAccess = false;
		voltage = 0;
		effectiveStage = 1;
		overcharged = false;
		rodActive = false;
		tempestActive = false;
		spiritualizationBonus = false;
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		clear();
	}
}
