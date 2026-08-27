package net.solocraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Preserves the pre-1.21 ImageButton atlas constructor used by the generated
 * screens. The second state remains a vertical UV offset in the same texture.
 */
public class LegacyAtlasImageButton extends ImageButton {
	private final ResourceLocation texture;
	private final int u;
	private final int v;
	private final int hoverOffset;
	private final int textureWidth;
	private final int textureHeight;

	public LegacyAtlasImageButton(int x, int y, int width, int height, int u, int v,
			int hoverOffset, ResourceLocation texture, int textureWidth, int textureHeight,
			Button.OnPress onPress) {
		this(x, y, width, height, u, v, hoverOffset, texture, textureWidth,
				textureHeight, onPress, Component.empty());
	}

	public LegacyAtlasImageButton(int x, int y, int width, int height, int u, int v,
			int hoverOffset, ResourceLocation texture, int textureWidth, int textureHeight,
			Button.OnPress onPress, Component message) {
		super(x, y, width, height, new WidgetSprites(texture, texture), onPress, message);
		this.texture = texture;
		this.u = u;
		this.v = v;
		this.hoverOffset = hoverOffset;
		this.textureWidth = textureWidth;
		this.textureHeight = textureHeight;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
			float partialTick) {
		int stateV = v + (isHoveredOrFocused() ? hoverOffset : 0);
		graphics.blit(texture, getX(), getY(), u, stateV, getWidth(), getHeight(),
				textureWidth, textureHeight);
	}
}
