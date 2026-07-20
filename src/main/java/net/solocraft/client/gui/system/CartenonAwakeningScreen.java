package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.CartenonAwakeningChoiceMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Non-dismissible System decision shown when Cartenon would kill the player. */
public final class CartenonAwakeningScreen extends SystemScreen {
	private boolean decisionSent;

	public CartenonAwakeningScreen() {
		super(Component.literal("SYSTEM"));
		this.panelW = 286;
		this.panelH = 188;
	}

	@Override
	protected boolean allowsNonSystemAccess() {
		return true;
	}

	@Override
	protected boolean shouldPlaySystemSounds() {
		return true;
	}

	@Override
	protected void init() {
		super.init();
		int buttonY = panelY + panelH - 43;
		addRenderableWidget(new SystemButton(panelX + 31, buttonY, 100, 22,
				Component.literal("YES"), button -> choose(true)));
		addRenderableWidget(new SystemButton(panelX + panelW - 131, buttonY, 100, 22,
				Component.literal("NO"), button -> choose(false)));
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.drawCenteredString(font, Component.literal("WARNING"),
				panelX + panelW / 2, panelY + 39, 0xFFFF536A);
		graphics.drawCenteredString(font, Component.literal("Death is imminent."),
				panelX + panelW / 2, panelY + 60, 0xFFFFA4B0);
		graphics.drawCenteredString(font, Component.literal("The System has detected a compatible host."),
				panelX + panelW / 2, panelY + 82, TEXT_SUB);
		graphics.drawCenteredString(font, Component.literal("Would you like to become a Player?"),
				panelX + panelW / 2, panelY + 104, TEXT_MAIN);
		graphics.fill(panelX + 34, panelY + 122, panelX + panelW - 34, panelY + 123, ACCENT_DIM);
	}

	@Override
	protected void beginClose() {
		// The server is the only authority allowed to close this decision.
	}

	@Override
	public void onClose() {
		// Intentionally non-dismissible. A server state packet closes it.
	}

	private void choose(boolean accept) {
		if (decisionSent)
			return;
		decisionSent = true;
		if (!accept)
			SystemGuiSounds.negativeNotification();
		SololevelingMod.PACKET_HANDLER.sendToServer(new CartenonAwakeningChoiceMessage(accept));
	}

	public static void handleServerState(boolean open) {
		Minecraft minecraft = Minecraft.getInstance();
		if (open) {
			if (!(minecraft.screen instanceof CartenonAwakeningScreen))
				minecraft.setScreen(new CartenonAwakeningScreen());
			return;
		}
		if (minecraft.screen instanceof CartenonAwakeningScreen) {
			SystemGuiSounds.exit();
			minecraft.setScreen(null);
		}
	}
}
