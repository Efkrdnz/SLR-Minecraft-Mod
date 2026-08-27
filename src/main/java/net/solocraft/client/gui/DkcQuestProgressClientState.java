package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.dkc.DkcSpatialLayout;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.entity.player.Player;

/** Latest compact, server-authoritative DKC objective shown by the hold-Tab HUD. */
@EventBusSubscriber(modid = SololevelingMod.MODID, value = Dist.CLIENT)
public final class DkcQuestProgressClientState {
	private static boolean active;
	private static int floor;
	private static int cleared;
	private static String floorName = "";
	private static String phase = "";
	private static String objective = "";
	private static String detail = "";
	private static int progress;
	private static int target;

	private DkcQuestProgressClientState() {
	}

	public static void update(boolean isActive, int currentFloor, int clearedFloors,
			String currentFloorName, String currentPhase, String currentObjective,
			String currentDetail, int currentProgress, int currentTarget) {
		active = isActive && currentFloor >= DkcFloorRegistry.FIRST_FLOOR
				&& currentFloor <= DkcFloorRegistry.LAST_FLOOR;
		floor = active ? currentFloor : 0;
		cleared = Math.max(0, Math.min(DkcFloorRegistry.LAST_FLOOR, clearedFloors));
		floorName = currentFloorName == null ? "" : currentFloorName;
		phase = currentPhase == null ? "" : currentPhase;
		objective = currentObjective == null ? "" : currentObjective;
		detail = currentDetail == null ? "" : currentDetail;
		progress = Math.max(0, currentProgress);
		target = Math.max(0, currentTarget);
	}

	public static void clear() {
		update(false, 0, 0, "", "", "", "", 0, 0);
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		clear();
	}

	/** Rejects a cached snapshot immediately after a floor or dimension transition. */
	public static boolean isActive(Player player) {
		return active && player != null && DkcFloorRegistry.isSharedDkc(player.level())
				&& DkcSpatialLayout.floorAt(player.blockPosition()) == floor;
	}

	public static int floor() {
		return floor;
	}

	public static int cleared() {
		return cleared;
	}

	public static String floorName() {
		return floorName;
	}

	public static String phase() {
		return phase;
	}

	public static String objective() {
		return objective;
	}

	public static String detail() {
		return detail;
	}

	public static int progress() {
		return progress;
	}

	public static int target() {
		return target;
	}
}
