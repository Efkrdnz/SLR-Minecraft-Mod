package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.network.FrostArchitectureSelectionMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.FrostArchitectureBlueprint;
import net.solocraft.util.FrostArchitectureManager;
import net.solocraft.util.CooldownManager;
import net.solocraft.init.SololevelingModKeyMappings;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.util.Mth;

/**
 * Client-only interaction state for the hold radial. Horizontal raw mouse
 * movement rotates the options while the player's view remains locked.
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class FrostArchitectureClientState {
	private static final double RADIANS_PER_PIXEL = 0.0105D;
	private static boolean active;
	private static double rotation;
	private static double lastMouseX;
	private static boolean mousePrimed;
	private static InputConstants.Key activationKey;
	private static float lockedYaw;
	private static float lockedPitch;

	private FrostArchitectureClientState() {
	}

	public static void begin(int hotbarSlot) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null)
			return;
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (!vars.combatmode || !FrostArchitectureManager.SKILL.equals(vars.PselectedPower))
			return;
		if (CooldownManager.isOnCooldown(player, FrostArchitectureManager.SKILL))
			return;
		if (!active) {
			active = true;
			rotation = 0.0D;
			mousePrimed = false;
			activationKey = hotbarKey(hotbarSlot).getKey();
			lockedYaw = player.getYRot();
			lockedPitch = player.getXRot();
			minecraft.setScreen(new FrostArchitecturePauseScreen());
		}
	}

	public static boolean isActive() {
		return active;
	}

	public static double rotation() {
		return rotation;
	}

	public static FrostArchitectureBlueprint selectedBlueprint() {
		FrostArchitectureBlueprint[] values = FrostArchitectureBlueprint.values();
		double step = Math.PI * 2.0D / values.length;
		int selected = Math.floorMod((int) Math.round(-rotation / step), values.length);
		return values[selected];
	}

	/**
	 * Called before the normal hotbar-release packet, preserving packet order.
	 */
	public static boolean releaseAndSend() {
		if (!active)
			return false;
		FrostArchitectureBlueprint selected = selectedBlueprint();
		SololevelingMod.PACKET_HANDLER.sendToServer(new FrostArchitectureSelectionMessage(selected.id()));
		clear();
		return true;
	}

	public static void clear() {
		active = false;
		rotation = 0.0D;
		mousePrimed = false;
		activationKey = null;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.screen instanceof FrostArchitecturePauseScreen)
			minecraft.setScreen(null);
	}

	static void onPauseScreenRemoved(FrostArchitecturePauseScreen screen) {
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
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !active)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.level == null
				|| (minecraft.screen != null && !(minecraft.screen instanceof FrostArchitecturePauseScreen))
				|| !player.isAlive()) {
			clear();
			return;
		}
		SololevelingModVariables.PlayerVariables vars = variables(player);
		if (!vars.combatmode || !FrostArchitectureManager.SKILL.equals(vars.PselectedPower)) {
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
		return switch (slot) {
			case 1 -> SololevelingModKeyMappings.AB_1;
			case 2 -> SololevelingModKeyMappings.AB_2;
			case 3 -> SololevelingModKeyMappings.AB_3;
			case 4 -> SololevelingModKeyMappings.AB_4;
			case 5 -> SololevelingModKeyMappings.AB_5;
			case 6 -> SololevelingModKeyMappings.AB_6;
			case 7 -> SololevelingModKeyMappings.AB_7;
			default -> SololevelingModKeyMappings.AB_8;
		};
	}
}
