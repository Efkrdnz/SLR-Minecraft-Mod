package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.network.ShadowExchangeMainGUIButtonMessage;
import net.solocraft.world.inventory.ShadowExchangeMainGUIMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class ShadowExchangeMainGUIScreen extends ShadowStyledScreen<ShadowExchangeMainGUIMenu> {
	private final static HashMap<String, Object> guistate = ShadowExchangeMainGUIMenu.guistate;
	private final Level world;
	private final int x;
	private final int y;
	private final int z;
	private final Player entity;
	private ShadowButton saveButton;
	private ShadowButton teleportButton;

	public ShadowExchangeMainGUIScreen(ShadowExchangeMainGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text, 360, 154);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
	}

	@Override
	protected String shadowTitle() {
		return "SHADOW EXCHANGE";
	}

	@Override
	protected void initShadowWidgets() {
		int bx = leftPos + 24;
		int by = topPos + 62;
		saveButton = new ShadowButton(bx, by, 146, 34, Component.literal("Set Shadow"), false, button -> sendButton(0));
		teleportButton = new ShadowButton(bx + 166, by, 146, 34, Component.literal("Exchange"), true, button -> sendButton(1));
		guistate.put("button:shadow_exchange_save_mode", saveButton);
		guistate.put("button:shadow_exchange_teleport_mode", teleportButton);
		this.addRenderableWidget(saveButton);
		this.addRenderableWidget(teleportButton);
	}

	@Override
	protected void renderShadowSections(GuiGraphics guiGraphics) {
		int x = leftPos;
		int y = topPos;
		outline(guiGraphics, x + 12, y + 32, 336, 94, 0x7743C8FF);
		guiGraphics.fill(x + 13, y + 33, x + 347, y + 47, 0x33124B76);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, "MODE", 18, 37, ACCENT_BLUE, false);
		guiGraphics.drawString(this.font, "SAVE", 31, 52, TEXT_SUB, false);
		guiGraphics.drawString(this.font, "MOVE", 197, 52, TEXT_SUB, false);
	}

	@Override
	protected void renderShadowTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		if (isOver(mouseX, mouseY, saveButton))
			guiGraphics.renderTooltip(this.font, Component.literal("Place a shadow on the ground to save this location."), mouseX, mouseY);
		if (isOver(mouseX, mouseY, teleportButton))
			guiGraphics.renderTooltip(this.font, Component.literal("Teleport to one of your saved exchange positions."), mouseX, mouseY);
	}

	private void sendButton(int buttonId) {
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeMainGUIButtonMessage(buttonId, x, y, z));
		ShadowExchangeMainGUIButtonMessage.handleButtonAction(entity, buttonId, x, y, z);
	}
}
