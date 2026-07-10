package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.network.ShadowExchangeSaveButtonMessage;
import net.solocraft.procedures.DoesHaveGoblinArcherProcedure;
import net.solocraft.procedures.DoesHaveGoblinMageProcedure;
import net.solocraft.procedures.DoesHaveGoblinProcedure;
import net.solocraft.procedures.DoesHaveKnightsProcedure;
import net.solocraft.procedures.DoesHavePolarBearProcedure;
import net.solocraft.procedures.DoesHaveWolfProcedure;
import net.solocraft.world.inventory.ShadowExchangeSaveMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class ShadowExchangeSaveScreen extends ShadowStyledScreen<ShadowExchangeSaveMenu> {
	private final static HashMap<String, Object> guistate = ShadowExchangeSaveMenu.guistate;
	private static final SaveEntry[] ENTRIES = {
			new SaveEntry(0, "Knight"),
			new SaveEntry(1, "Goblin Fighter"),
			new SaveEntry(2, "Goblin Archer"),
			new SaveEntry(3, "Goblin Mage"),
			new SaveEntry(4, "Lycan"),
			new SaveEntry(5, "Polar Bear")
	};

	private final Level world;
	private final int x;
	private final int y;
	private final int z;
	private final Player entity;
	private final ShadowButton[] buttons = new ShadowButton[ENTRIES.length];

	public ShadowExchangeSaveScreen(ShadowExchangeSaveMenu container, Inventory inventory, Component text) {
		super(container, inventory, text, 360, 212);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
	}

	@Override
	protected String shadowTitle() {
		return "SET EXCHANGE SHADOW";
	}

	@Override
	protected void initShadowWidgets() {
		for (int i = 0; i < ENTRIES.length; i++) {
			SaveEntry entry = ENTRIES[i];
			ShadowButton button = new ShadowButton(0, 0, 150, 27, Component.literal(entry.label), entry.id % 2 == 1, b -> sendButton(entry.id));
			buttons[i] = button;
			guistate.put("button:shadow_exchange_save_" + entry.id, button);
			this.addRenderableWidget(button);
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
		outline(guiGraphics, x + 12, y + 32, 336, 160, 0x7743C8FF);
		guiGraphics.fill(x + 13, y + 33, x + 347, y + 47, 0x33124B76);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, "AVAILABLE SHADOWS", 18, 37, ACCENT_BLUE, false);
		if (visibleCount() == 0)
			guiGraphics.drawCenteredString(this.font, Component.literal("No summonable shadows available."), imageWidth / 2, 105, TEXT_SUB);
	}

	private void layoutButtons() {
		int visible = 0;
		for (int i = 0; i < ENTRIES.length; i++) {
			ShadowButton button = buttons[i];
			if (button == null)
				continue;
			boolean show = hasShadow(ENTRIES[i].id);
			button.visible = show;
			button.active = show;
			if (!show)
				continue;
			int col = visible % 2;
			int row = visible / 2;
			button.setPosition(leftPos + 24 + col * 162, topPos + 58 + row * 38);
			visible++;
		}
	}

	private int visibleCount() {
		int count = 0;
		for (SaveEntry entry : ENTRIES) {
			if (hasShadow(entry.id))
				count++;
		}
		return count;
	}

	private boolean hasShadow(int id) {
		return switch (id) {
			case 0 -> DoesHaveKnightsProcedure.execute(entity);
			case 1 -> DoesHaveGoblinProcedure.execute(entity);
			case 2 -> DoesHaveGoblinArcherProcedure.execute(entity);
			case 3 -> DoesHaveGoblinMageProcedure.execute(entity);
			case 4 -> DoesHaveWolfProcedure.execute(entity);
			case 5 -> DoesHavePolarBearProcedure.execute(entity);
			default -> false;
		};
	}

	private void sendButton(int buttonId) {
		if (!hasShadow(buttonId))
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSaveButtonMessage(buttonId, x, y, z));
		ShadowExchangeSaveButtonMessage.handleButtonAction(entity, buttonId, x, y, z);
	}

	private record SaveEntry(int id, String label) {
	}
}
