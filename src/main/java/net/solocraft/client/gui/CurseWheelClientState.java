package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModKeyMappings;
import net.solocraft.network.CurseSelectionMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.CurseMageSpellManager;
import net.solocraft.util.CurseType;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Client-only interaction state for the curse wheel.
 *
 * <p>Mirrors the Frozen Architecture radial: hold the ability key to open, drag
 * horizontally to rotate, release to commit. The one difference is that the wheel
 * is built from the curses this player has actually earned, so it grows from one
 * segment to six as their Hunter rank rises.
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class CurseWheelClientState {
	private static final double RADIANS_PER_PIXEL = 0.0105D;
	private static boolean active;
	private static double rotation;
	private static double lastMouseX;
	private static boolean mousePrimed;
	private static InputConstants.Key activationKey;
	private static float lockedYaw;
	private static float lockedPitch;

	private CurseWheelClientState() {
	}

	public static void begin(int hotbarSlot) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null)
			return;
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (!vars.combatmode
				|| !CurseMageSpellManager.CURSE_WEAVE.equals(vars.PselectedPower))
			return;
		if (!active) {
			active = true;
			rotation = 0.0D;
			mousePrimed = false;
			activationKey = hotbarKey(hotbarSlot).getKey();
			lockedYaw = player.getYRot();
			lockedPitch = player.getXRot();
			minecraft.setScreen(new CurseWheelPauseScreen());
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static double rotation() {
		return rotation;
	}

	/** The curses on the wheel: everything this player has unlocked. */
	public static List<CurseType> options() {
		LocalPlayer player = Minecraft.getInstance().player;
		return player == null ? List.of(CurseType.WITHERING)
				: CurseMageSpellManager.unlockedCurses(player);
	}

	public static CurseType selectedCurse() {
		List<CurseType> values = options();
		double step = Math.PI * 2.0D / values.size();
		int selected = Math.floorMod((int) Math.round(-rotation / step), values.size());
		return values.get(selected);
	}

	/** Called before the normal hotbar-release packet, preserving packet order. */
	public static boolean releaseAndSend() {
		if (!active)
			return false;
		CurseType selected = selectedCurse();
		SololevelingMod.PACKET_HANDLER.sendToServer(new CurseSelectionMessage(selected.id()));
		clear();
		return true;
	}

	public static void clear() {
		active = false;
		rotation = 0.0D;
		mousePrimed = false;
		activationKey = null;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen instanceof CurseWheelPauseScreen)
			minecraft.setScreen(null);
	}

	static void onPauseScreenRemoved(CurseWheelPauseScreen screen) {
		if (active) {
			active = false;
			rotation = 0.0D;
			mousePrimed = false;
			activationKey = null;
		}
	}

	static boolean isActivationKey(int keyCode, int scanCode) {
		return active && activationKey != null
				&& activationKey.equals(InputConstants.getKey(keyCode, scanCode));
	}

	/**
	 * Render-time sampling is required because a real integrated-server pause
	 * suppresses ordinary client ticks while GUI frames continue rendering.
	 */
	public static void updateMouseFromFrame() {
		if (!active)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null || !player.isAlive()) {
			clear();
			return;
		}
		double mouseX = minecraft.mouseHandler.xpos();
		if (!mousePrimed) {
			lastMouseX = mouseX;
			mousePrimed = true;
			return;
		}
		double delta = Mth.clamp(mouseX - lastMouseX, -80.0D, 80.0D);
		lastMouseX = mouseX;
		rotation = Mth.wrapDegrees(Math.toDegrees(rotation + delta * RADIANS_PER_PIXEL));
		rotation = Math.toRadians(rotation);

		player.setYRot(lockedYaw);
		player.setXRot(lockedPitch);
		player.yHeadRot = lockedYaw;
		player.yHeadRotO = lockedYaw;
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (!active)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null
				|| (minecraft.screen != null && !(minecraft.screen instanceof CurseWheelPauseScreen))
				|| !player.isAlive()) {
			clear();
			return;
		}
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (!vars.combatmode
				|| !CurseMageSpellManager.CURSE_WEAVE.equals(vars.PselectedPower)) {
			clear();
			return;
		}

		// Keep the world view steady between render frames as well.
		player.setYRot(lockedYaw);
		player.setXRot(lockedPitch);
		player.yHeadRot = lockedYaw;
		player.yHeadRotO = lockedYaw;
	}

	private static SololevelingModVariables.PlayerVariables variables(LocalPlayer player) {
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static KeyMapping hotbarKey(int slot) {
		// The skill slots ride the vanilla hotbar keys now, so hold-and-release
		// detection has to watch the key the player actually pressed rather than
		// a dedicated binding that no longer exists.
		KeyMapping[] slots = Minecraft.getInstance().options.keyHotbarSlots;
		int index = Math.max(0, Math.min(slots.length - 1, slot - 1));
		return slots[index];
	}
}
