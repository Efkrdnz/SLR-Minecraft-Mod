package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

/**
 * Keeps the physical mouse position stable while the skill UI changes server
 * containers. Vanilla centers the pointer as a container is reopened; that is
 * particularly disruptive for the two-click slot -> skill -> slot workflow.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, value = Dist.CLIENT)
public final class SkillScreenCursorRestore {
	private static final int MAX_WAIT_TICKS = 40;

	private static TargetScreen pendingTarget;
	private static double savedX;
	private static double savedY;
	private static int remainingTicks;

	private SkillScreenCursorRestore() {
	}

	public static void preserveForSkillList() {
		preserve(TargetScreen.SKILL_LIST);
	}

	public static void preserveForSlots() {
		preserve(TargetScreen.SLOTS);
	}

	private static void preserve(TargetScreen target) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getWindow() == null)
			return;
		savedX = minecraft.mouseHandler.xpos();
		savedY = minecraft.mouseHandler.ypos();
		pendingTarget = target;
		remainingTicks = MAX_WAIT_TICKS;
	}

	@SubscribeEvent
	public static void restoreAfterContainerSwap(ClientTickEvent.Post event) {
		if (pendingTarget == null)
			return;
		if (--remainingTicks < 0) {
			pendingTarget = null;
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		if (!pendingTarget.matches(minecraft.screen) || minecraft.getWindow() == null)
			return;
		GLFW.glfwSetCursorPos(minecraft.getWindow().getWindow(), savedX, savedY);
		pendingTarget = null;
	}

	private enum TargetScreen {
		SKILL_LIST {
			@Override
			boolean matches(Object screen) {
				return screen instanceof UnlockedSkillsTab1Screen;
			}
		},
		SLOTS {
			@Override
			boolean matches(Object screen) {
				return screen instanceof EquippedAbilitiesScreen;
			}
		};

		abstract boolean matches(Object screen);
	}
}
