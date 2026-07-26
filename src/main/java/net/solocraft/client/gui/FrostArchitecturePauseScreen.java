package net.solocraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Transparent screen that gives the radial the same single-player pause
 * semantics as the vanilla ESC menu and the other System screens.
 */
public final class FrostArchitecturePauseScreen extends Screen {
	public FrostArchitecturePauseScreen() {
		super(Component.translatable("gui.sololeveling.frost_architecture.title"));
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Intentionally transparent. FrostArchitectureRadialOverlay owns the visuals.
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (FrostArchitectureClientState.isActivationKey(keyCode, scanCode)) {
			FrostArchitectureClientState.releaseAndSend();
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		FrostArchitectureClientState.onPauseScreenRemoved(this);
		super.removed();
	}
}
