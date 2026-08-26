package net.solocraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.LivingEntity;

/**
 * Adapts the centered 1.20 inventory entity preview call to the bounded 1.21 API.
 */
public final class LegacyInventoryEntityRenderer {
	private LegacyInventoryEntityRenderer() {
	}

	public static void renderEntityInInventoryFollowsAngle(GuiGraphics graphics, int x, int y,
			int scale, float angleX, float angleY, LivingEntity entity) {
		int halfWidth = Math.max(48, scale + 16);
		int halfHeight = Math.max(64, scale * 2);
		InventoryScreen.renderEntityInInventoryFollowsAngle(graphics,
				x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight,
				scale, 0.0F, angleX, angleY, entity);
	}
}
