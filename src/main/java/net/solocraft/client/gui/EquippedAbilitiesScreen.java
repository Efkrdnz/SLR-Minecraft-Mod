package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.client.gui.system.SystemTooltip;
import net.solocraft.network.EquippedAbilitiesButtonMessage;
import net.solocraft.procedures.ReturnAbilitySlotColorProcedure;
import net.solocraft.procedures.ReturnAbilitySlotProcedure;
import net.solocraft.world.inventory.EquippedAbilitiesMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;

public class EquippedAbilitiesScreen extends SystemContainerScreen<EquippedAbilitiesMenu> {
	private static final int ROWS = 8;
	private static final int ROW_H = 23;

	private final Level world;
	private final int x, y, z;
	private final Player entity;
	private int abilityPage = 1;

	private SystemScreen.SystemButton pageButton;
	private final SystemScreen.SystemButton[] changeButtons = new SystemScreen.SystemButton[ROWS];
	private final SystemScreen.SystemButton[] clearButtons = new SystemScreen.SystemButton[ROWS];

	public EquippedAbilitiesScreen(EquippedAbilitiesMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -118;
		this.pRelY = -126;
		this.pW = 236;
		this.pH = 252;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int gx, int gy) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ShopStyle.panel(g, leftPos + pRelX, topPos + pRelY, pW, pH);
		ShopStyle.titleBar(g, this.font, leftPos + pRelX, topPos + pRelY, pW, "SKILL SLOTS");

		int startY = topPos + pRelY + 34;
		for (int i = 0; i < ROWS; i++) {
			int y0 = startY + i * ROW_H;
			int fill = i % 2 == 0 ? 0x33102338 : 0x22102338;
			g.fill(leftPos + pRelX + 10, y0, leftPos + pRelX + pW - 10, y0 + 20, fill);
			g.fill(leftPos + pRelX + 10, y0 + 20, leftPos + pRelX + pW - 10, y0 + 21, ShopStyle.ACCENT_SOFT);
		}
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		int startY = pRelY + 39;
		String page = abilityPage == 1 ? "SLOTS 1-8" : "SLOTS 9-16";
		g.drawString(this.font, page, pRelX + 12, pRelY + 22, ShopStyle.ACCENT, false);

		for (int i = 0; i < ROWS; i++) {
			int slot = displaySlot(i + 1);
			int rowY = startY + i * ROW_H;
			int color = ReturnAbilitySlotColorProcedure.execute(entity, slot);
			String skill = ReturnAbilitySlotProcedure.execute(entity, slot);
			if (skill.length() > 22)
				skill = skill.substring(0, 21) + "...";
			g.drawString(this.font, slot < 10 ? "0" + slot : String.valueOf(slot), pRelX + 16, rowY, 0xFF8FB8D8, false);
			g.drawString(this.font, skill, pRelX + 44, rowY, color, false);
		}
	}

	@Override
	protected void renderExtras(GuiGraphics g, int mouseX, int mouseY) {
		for (int i = 0; i < ROWS; i++) {
			int slot = displaySlot(i + 1);
			int y0 = topPos + pRelY + 34 + i * ROW_H;
			if (mouseX >= leftPos + pRelX + 10 && mouseX < leftPos + pRelX + pW - 10 && mouseY >= y0 && mouseY < y0 + 20) {
				SystemTooltip.render(g, this.font, List.of(Component.literal("Slot " + slot), Component.literal(ReturnAbilitySlotProcedure.execute(entity, slot))), mouseX, mouseY, this.width, this.height);
				return;
			}
		}
	}

	@Override
	public void init() {
		super.init();
		this.addRenderableWidget(new SystemScreen.SystemButton(leftPos + pRelX + 3, topPos + pRelY + 2, 40, 12, Component.literal("< Back"), b -> {
			if (this.minecraft != null && this.minecraft.player != null) {
				this.minecraft.player.closeContainer();
				openSystemScreen(new net.solocraft.client.gui.system.SystemPanelScreen());
			}
		}));

		pageButton = new SystemScreen.SystemButton(leftPos + pRelX + pW - 72, topPos + pRelY + 20, 60, 16, Component.literal("Page 2"), b -> togglePage());
		this.addRenderableWidget(pageButton);

		int startY = topPos + pRelY + 35;
		for (int i = 0; i < ROWS; i++) {
			final int row = i + 1;
			int by = startY + i * ROW_H;
			changeButtons[i] = new SystemScreen.SystemButton(leftPos + pRelX + pW - 91, by, 48, 18, Component.literal("Change"), b -> sendButton(equipButtonId(row)));
			clearButtons[i] = new SystemScreen.SystemButton(leftPos + pRelX + pW - 39, by, 28, 18, Component.literal("X"), b -> sendButton(removeButtonId(row)));
			this.addRenderableWidget(changeButtons[i]);
			this.addRenderableWidget(clearButtons[i]);
		}
		refreshPageButton();
	}

	private int displaySlot(int row) {
		return abilityPage == 2 ? row + 8 : row;
	}

	private int equipButtonId(int row) {
		return abilityPage == 2 ? row + 15 : row - 1;
	}

	private int removeButtonId(int row) {
		return abilityPage == 2 ? row + 23 : row + 7;
	}

	private void togglePage() {
		abilityPage = abilityPage == 1 ? 2 : 1;
		refreshPageButton();
	}

	private void refreshPageButton() {
		if (pageButton != null)
			pageButton.setMessage(Component.literal(abilityPage == 1 ? "Page 2" : "Page 1"));
	}

	private void sendButton(int buttonId) {
		SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(buttonId, x, y, z));
	}
}
