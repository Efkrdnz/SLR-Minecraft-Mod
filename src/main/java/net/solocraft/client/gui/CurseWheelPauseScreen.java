package net.solocraft.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Transparent screen that gives the curse wheel the same single-player pause
 * semantics as the vanilla ESC menu and the other System screens.
 */
public final class CurseWheelPauseScreen extends Screen {
	public CurseWheelPauseScreen() {
		super(Component.translatable("gui.sololeveling.curse_wheel.title"));
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Intentionally transparent. CurseWheelRadialOverlay owns the visuals.
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY,
			float partialTick) {
		// Screen.render() invokes this automatically in 1.21. A no-op keeps the
		// wheel's world view genuinely transparent instead of applying menu blur.
	}

	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		if (CurseWheelClientState.isActivationKey(keyCode, scanCode)) {
			CurseWheelClientState.releaseAndSend();
			return true;
		}
		return super.keyReleased(keyCode, scanCode, modifiers);
	}

	@Override
	public void removed() {
		CurseWheelClientState.onPauseScreenRemoved(this);
		super.removed();
	}
}
