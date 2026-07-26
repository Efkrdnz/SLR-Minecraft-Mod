package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.RadiruMercyChoiceMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** Explicit Floor 15 choice shown after the player speaks to surrendered Esil. */
public final class RadiruMercyChoiceScreen extends SystemScreen {
	private boolean decisionSent;

	public RadiruMercyChoiceScreen() {
		super(Component.translatable("gui.sololeveling.radiru_mercy.title"));
		this.panelW = 310;
		this.panelH = 210;
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
		addRenderableWidget(new SystemButton(panelX + 27, buttonY, 122, 22,
				Component.translatable("gui.sololeveling.radiru_mercy.spare"), button -> choose(true)));
		addRenderableWidget(new SystemButton(panelX + panelW - 149, buttonY, 122, 22,
				Component.translatable("gui.sololeveling.radiru_mercy.later"), button -> choose(false)));
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int centerX = panelX + panelW / 2;
		graphics.drawCenteredString(font, Component.translatable("entity.sololeveling.esil_radiru"),
				centerX, panelY + 38, 0xFFD88CFF);
		graphics.drawCenteredString(font, Component.translatable("dialogue.sololeveling.esil.mercy.plea"),
				centerX, panelY + 59, TEXT_MAIN);
		graphics.drawCenteredString(font, Component.translatable("dialogue.sololeveling.esil.mercy.offer"),
				centerX, panelY + 76, TEXT_SUB);
		graphics.fill(panelX + 37, panelY + 98, panelX + panelW - 37, panelY + 99, ACCENT_DIM);
		graphics.drawCenteredString(font,
				Component.translatable("gui.sololeveling.radiru_mercy.instruction"),
				centerX, panelY + 112, TEXT_MAIN);
		graphics.drawCenteredString(font, Component.translatable("gui.sololeveling.radiru_mercy.warning"),
				centerX, panelY + 132, 0xFFFFA4B0);
	}

	@Override
	protected void beginClose() {
		choose(false);
	}

	@Override
	public void onClose() {
		if (!decisionSent)
			choose(false);
		super.onClose();
	}

	private void choose(boolean spare) {
		if (decisionSent)
			return;
		decisionSent = true;
		if (!spare)
			SystemGuiSounds.negativeNotification();
		SololevelingMod.PACKET_HANDLER.sendToServer(new RadiruMercyChoiceMessage(spare));
	}

	public static void handleServerState(boolean open) {
		Minecraft minecraft = Minecraft.getInstance();
		if (open) {
			if (!(minecraft.screen instanceof RadiruMercyChoiceScreen))
				minecraft.setScreen(new RadiruMercyChoiceScreen());
			return;
		}
		if (minecraft.screen instanceof RadiruMercyChoiceScreen screen) {
			screen.decisionSent = true;
			SystemGuiSounds.exit();
			minecraft.setScreen(null);
		}
	}
}
