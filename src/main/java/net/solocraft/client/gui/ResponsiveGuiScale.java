package net.solocraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shrinks oversized custom screens around the viewport centre while preserving
 * their existing layout at normal GUI scales.
 */
public final class ResponsiveGuiScale {
	private static final float VIEWPORT_MARGIN = 8.0F;

	private ResponsiveGuiScale() {
	}

	public static Transform fit(int viewportWidth, int viewportHeight, int contentWidth, int contentHeight) {
		float availableWidth = Math.max(1.0F, viewportWidth - VIEWPORT_MARGIN);
		float availableHeight = Math.max(1.0F, viewportHeight - VIEWPORT_MARGIN);
		float widthScale = availableWidth / Math.max(1.0F, contentWidth);
		float heightScale = availableHeight / Math.max(1.0F, contentHeight);
		float scale = Math.max(0.05F, Math.min(1.0F, Math.min(widthScale, heightScale)));
		return new Transform(viewportWidth, viewportHeight, scale);
	}

	public static void push(GuiGraphics graphics, Transform transform) {
		graphics.pose().pushPose();
		graphics.pose().translate(transform.centerX(), transform.centerY(), 0.0F);
		graphics.pose().scale(transform.scale(), transform.scale(), 1.0F);
		graphics.pose().translate(-transform.centerX(), -transform.centerY(), 0.0F);
	}

	public static void pop(GuiGraphics graphics) {
		graphics.pose().popPose();
	}

	/** GuiGraphics scissors ignore the pose stack, so transform them explicitly. */
	public static void enableScissor(GuiGraphics graphics, Transform transform, int minX, int minY, int maxX, int maxY) {
		int screenMinX = clamp((int) Math.floor(transform.screenX(minX)), 0, transform.viewportWidth());
		int screenMinY = clamp((int) Math.floor(transform.screenY(minY)), 0, transform.viewportHeight());
		int screenMaxX = clamp((int) Math.ceil(transform.screenX(maxX)), 0, transform.viewportWidth());
		int screenMaxY = clamp((int) Math.ceil(transform.screenY(maxY)), 0, transform.viewportHeight());
		graphics.enableScissor(screenMinX, screenMinY, Math.max(screenMinX, screenMaxX), Math.max(screenMinY, screenMaxY));
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	public record Transform(int viewportWidth, int viewportHeight, float scale) {
		public float centerX() {
			return viewportWidth * 0.5F;
		}

		public float centerY() {
			return viewportHeight * 0.5F;
		}

		public double logicalX(double screenX) {
			return centerX() + (screenX - centerX()) / scale;
		}

		public double logicalY(double screenY) {
			return centerY() + (screenY - centerY()) / scale;
		}

		public double screenX(double logicalX) {
			return centerX() + (logicalX - centerX()) * scale;
		}

		public double screenY(double logicalY) {
			return centerY() + (logicalY - centerY()) * scale;
		}

		public int logicalMouseX(double screenX) {
			return (int) Math.round(logicalX(screenX));
		}

		public int logicalMouseY(double screenY) {
			return (int) Math.round(logicalY(screenY));
		}
	}
}
