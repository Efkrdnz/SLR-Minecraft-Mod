package net.solocraft.util;

import net.solocraft.network.RangerStateMessage;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/** Client snapshot for Mana Quiver charging, lock-on, and Ranger stances. */
@EventBusSubscriber(value = Dist.CLIENT)
public final class RangerClientState {
	public static volatile boolean quiverActive;
	public static volatile int chargeStage;
	public static volatile int maximumStage = 1;
	public static volatile float lockProgress;
	public static volatile boolean locked;
	public static volatile int fivefoldCharges;
	public static volatile boolean hawkeye;
	public static volatile boolean hyperFocus;

	private RangerClientState() {
	}

	public static void update(RangerStateMessage message) {
		quiverActive = message.quiverActive;
		chargeStage = message.chargeStage;
		maximumStage = Math.max(1, message.maximumStage);
		lockProgress = Math.max(0.0F, Math.min(1.0F, message.lockProgress));
		locked = message.locked;
		fivefoldCharges = Math.max(0, message.fivefoldCharges);
		hawkeye = message.hawkeye;
		hyperFocus = message.hyperFocus;
	}

	public static void clear() {
		quiverActive = false;
		chargeStage = 0;
		maximumStage = 1;
		lockProgress = 0.0F;
		locked = false;
		fivefoldCharges = 0;
		hawkeye = false;
		hyperFocus = false;
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		clear();
	}
}
