package net.solocraft.client.gui;

import net.solocraft.util.OverlayLayoutConfig;

import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Applies a customized overlay's saved offset and scale.
 *
 * <p>Every movable overlay wraps its own rendering in {@link #push} and
 * {@link #pop}. An element left at defaults produces an identity transform, so
 * an untouched install renders exactly where it always did.</p>
 */
public final class OverlayTransform {
	private OverlayTransform() {
	}

	public static void push(GuiGraphics graphics, int element) {
		PoseStack pose = graphics.pose();
		pose.pushPose();
		pose.translate(OverlayLayoutConfig.getOffsetX(element),
				OverlayLayoutConfig.getOffsetY(element), 0.0F);
		float scale = OverlayLayoutConfig.getScale(element);
		if (Math.abs(scale - 1.0F) < 0.001F)
			return;
		// Pivot on the element's own anchor so resizing grows it away from its
		// corner rather than sliding it across the screen.
		float anchorX = OverlayLayoutConfig.anchorX(element);
		float anchorY = OverlayLayoutConfig.anchorY(element);
		pose.translate(anchorX, anchorY, 0.0F);
		pose.scale(scale, scale, 1.0F);
		pose.translate(-anchorX, -anchorY, 0.0F);
	}

	public static void pop(GuiGraphics graphics) {
		graphics.pose().popPose();
	}

	/**
	 * True when an overlay that normally hides behind an open screen should
	 * still render, because the player is arranging the HUD right now.
	 */
	public static boolean isArranging() {
		return net.minecraft.client.Minecraft.getInstance()
				.screen instanceof net.solocraft.client.gui.system.OverlayLayoutScreen;
	}
}
