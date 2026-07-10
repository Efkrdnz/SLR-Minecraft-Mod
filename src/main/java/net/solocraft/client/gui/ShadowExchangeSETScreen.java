package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.network.ShadowExchangeSETButtonMessage;
import net.solocraft.util.ShadowExchangeManager;
import net.solocraft.world.inventory.ShadowExchangeSETMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class ShadowExchangeSETScreen extends ShadowStyledScreen<ShadowExchangeSETMenu> {
	private final static HashMap<String, Object> guistate = ShadowExchangeSETMenu.guistate;
	private static final int SLOT_COUNT = 7;

	private final Level world;
	private final int x;
	private final int y;
	private final int z;
	private final Player entity;
	private final ShadowButton[] teleportButtons = new ShadowButton[SLOT_COUNT];
	private final ShadowButton[] clearButtons = new ShadowButton[SLOT_COUNT];

	public ShadowExchangeSETScreen(ShadowExchangeSETMenu container, Inventory inventory, Component text) {
		super(container, inventory, text, 420, 252);
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
		for (int i = 0; i < SLOT_COUNT; i++) {
			int slot = i + 1;
			ShadowButton teleport = new ShadowButton(0, 0, 298, 22, Component.literal(""), i % 2 == 1, b -> sendTeleport(slot));
			ShadowButton clear = new ShadowButton(0, 0, 64, 22, Component.literal("Clear"), true, true, b -> sendClear(slot));
			teleportButtons[i] = teleport;
			clearButtons[i] = clear;
			guistate.put("button:shadow_exchange_tp_" + slot, teleport);
			guistate.put("button:shadow_exchange_clear_" + slot, clear);
			this.addRenderableWidget(teleport);
			this.addRenderableWidget(clear);
		}
		layoutButtons();
	}

	@Override
	public void containerTick() {
		super.containerTick();
		layoutButtons();
	}

	@Override
	protected void renderShadowSections(GuiGraphics guiGraphics) {
		int x = leftPos;
		int y = topPos;
		outline(guiGraphics, x + 12, y + 32, 396, 202, 0x7743C8FF);
		guiGraphics.fill(x + 13, y + 33, x + 407, y + 47, 0x33124B76);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, "SAVED POSITIONS", 18, 37, ACCENT_BLUE, false);
		if (visibleCount() == 0)
			guiGraphics.drawCenteredString(this.font, Component.literal("No exchange positions saved."), imageWidth / 2, 126, TEXT_SUB);
	}

	@Override
	protected void renderShadowTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		for (int i = 0; i < SLOT_COUNT; i++) {
			if (isOver(mouseX, mouseY, teleportButtons[i])) {
				guiGraphics.renderTooltip(this.font, Component.literal("Exchange to saved position " + (i + 1)), mouseX, mouseY);
				return;
			}
			if (isOver(mouseX, mouseY, clearButtons[i])) {
				guiGraphics.renderTooltip(this.font, Component.literal("Remove saved position " + (i + 1)), mouseX, mouseY);
				return;
			}
		}
	}

	private void layoutButtons() {
		int row = 0;
		for (int i = 0; i < SLOT_COUNT; i++) {
			boolean show = hasPosition(i + 1);
			ShadowButton teleport = teleportButtons[i];
			ShadowButton clear = clearButtons[i];
			if (teleport == null || clear == null)
				continue;
			teleport.visible = show;
			teleport.active = show;
			clear.visible = show;
			clear.active = show;
			if (!show)
				continue;
			int by = topPos + 56 + row * 25;
			teleport.setPosition(leftPos + 24, by);
			teleport.setMessage(Component.literal(positionText(i + 1)));
			clear.setPosition(leftPos + 330, by);
			row++;
		}
	}

	private int visibleCount() {
		int count = 0;
		for (int slot = 1; slot <= SLOT_COUNT; slot++) {
			if (hasPosition(slot))
				count++;
		}
		return count;
	}

	private boolean hasPosition(int slot) {
		return ShadowExchangeManager.hasAnchor(entity, slot);
	}

	private String positionText(int slot) {
		String value = ShadowExchangeManager.anchorDisplay(entity, slot);
		return value == null || value.isBlank() ? "Saved Position " + slot : value;
	}

	private void sendTeleport(int slot) {
		if (!hasPosition(slot))
			return;
		sendButton(slot - 1);
	}

	private void sendClear(int slot) {
		if (!hasPosition(slot))
			return;
		sendButton(6 + slot);
		layoutButtons();
	}

	private void sendButton(int buttonId) {
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(buttonId, x, y, z));
		ShadowExchangeSETButtonMessage.handleButtonAction(entity, buttonId, x, y, z);
	}
}
