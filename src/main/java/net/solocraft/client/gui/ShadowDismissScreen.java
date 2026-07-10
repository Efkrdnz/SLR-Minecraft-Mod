package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.network.ShadowDismissButtonMessage;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.world.inventory.ShadowDismissMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShadowDismissScreen extends ShadowStyledScreen<ShadowDismissMenu> {
	private final static HashMap<String, Object> guistate = ShadowDismissMenu.guistate;
	private static final int PANEL_W = 360;
	private static final int PANEL_H = 236;
	private static final int LIST_X = 18;
	private static final int LIST_Y = 51;
	private static final int LIST_W = 324;
	private static final int LIST_H = 124;
	private static final int BUTTON_H = 27;
	private static final int BUTTON_GAP = 8;

	private final Level world;
	private final int x;
	private final int y;
	private final int z;
	private final Player entity;
	private final List<DismissEntry> entries = new ArrayList<>();
	private final List<DismissButton> dismissButtons = new ArrayList<>();
	private int scroll;

	public ShadowDismissScreen(ShadowDismissMenu container, Inventory inventory, Component text) {
		super(container, inventory, text, PANEL_W, PANEL_H);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
	}

	@Override
	protected String shadowTitle() {
		return "SHADOW DISMISS";
	}

	@Override
	public void containerTick() {
		super.containerTick();
		layoutDismissButtons();
	}

	@Override
	protected void initShadowWidgets() {
		entries.clear();
		dismissButtons.clear();
		addEntries();
		for (DismissEntry entry : entries)
			addDismissButton(entry);
		ShadowButton backButton = new ShadowButton(this.leftPos + 18, this.topPos + 198, 96, 20, Component.literal("Back"), false, true, b -> openSummon());
		guistate.put("button:button_back", backButton);
		this.addRenderableWidget(backButton);
		layoutDismissButtons();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, "NORMAL UNITS", 18, 37, ACCENT_BLUE, false);
		guiGraphics.drawString(this.font, "DISMISS", 18, 187, ACCENT_BLUE, false);
	}

	@Override
	protected void renderShadowSections(GuiGraphics g) {
		int x = leftPos;
		int y = topPos;
		outline(g, x + 12, y + 32, 336, 154, 0x7743C8FF);
		outline(g, x + 12, y + 192, 336, 32, 0x7743C8FF);
		g.fill(x + 13, y + 33, x + 347, y + 47, 0x33124B76);
		g.fill(x + 13, y + 193, x + 347, y + 199, 0x33124B76);
		drawScrollHint(g, x + 337, y + 52, y + 174, visibleEntries().size(), visibleRows(), scroll);
		if (visibleEntries().isEmpty())
			g.drawCenteredString(this.font, "No normal shadows available.", x + PANEL_W / 2, y + 104, TEXT_SUB);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (!isOpeningOrOpen())
			return true;
		if (isOverBox(mouseX, mouseY, leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H)) {
			int max = maxScroll(visibleEntries().size(), visibleRows());
			scroll = clamp(scroll - (int) Math.signum(delta), 0, max);
			layoutDismissButtons();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	private void addEntries() {
		entries.add(new DismissEntry(0, "Goblin Fighter", "goblin_club"));
		entries.add(new DismissEntry(1, "Goblin Archer", "goblin_archer"));
		entries.add(new DismissEntry(2, "Goblin Mage", "goblin_mage"));
		entries.add(new DismissEntry(3, "Lycan", "wolf"));
		entries.add(new DismissEntry(4, "Knight", "knight"));
		entries.add(new DismissEntry(5, "Polar Bear", "polar_bear"));
		entries.add(new DismissEntry(6, "Orc", "orc"));
		entries.add(new DismissEntry(7, "High Orc", "high_orc"));
	}

	private void addDismissButton(DismissEntry entry) {
		DismissButton button = new DismissButton(0, 0, 154, BUTTON_H, entry, b -> dismiss(entry));
		button.visible = false;
		dismissButtons.add(button);
		guistate.put("button:shadow_dismiss_" + entry.id, button);
		this.addRenderableWidget(button);
	}

	private void layoutDismissButtons() {
		List<DismissEntry> visible = visibleEntries();
		scroll = clamp(scroll, 0, maxScroll(visible.size(), visibleRows()));
		for (DismissButton button : dismissButtons) {
			button.visible = false;
			button.active = false;
		}
		int start = scroll * 2;
		int end = Math.min(visible.size(), start + visibleRows() * 2);
		for (int i = start; i < end; i++) {
			DismissButton button = buttonFor(visible.get(i));
			if (button == null)
				continue;
			int local = i - start;
			int col = local % 2;
			int row = local / 2;
			button.setPosition(leftPos + LIST_X + 6 + col * 160, topPos + LIST_Y + row * (BUTTON_H + BUTTON_GAP));
			button.setClip(leftPos + LIST_X, topPos + LIST_Y, LIST_W, LIST_H);
			button.visible = true;
			button.active = true;
		}
	}

	private DismissButton buttonFor(DismissEntry entry) {
		for (DismissButton button : dismissButtons) {
			if (button.entry == entry)
				return button;
		}
		return null;
	}

	private List<DismissEntry> visibleEntries() {
		List<DismissEntry> visible = new ArrayList<>();
		for (DismissEntry entry : entries) {
			if (ShadowMonarchManager.hasShadowForDisplay(entity, entry.type))
				visible.add(entry);
		}
		return visible;
	}

	private int visibleRows() {
		return Math.max(1, LIST_H / (BUTTON_H + BUTTON_GAP));
	}

	private int maxScroll(int entryCount, int visibleRows) {
		int rows = (int) Math.ceil(entryCount / 2.0D);
		return Math.max(0, rows - visibleRows);
	}

	private void dismiss(DismissEntry entry) {
		if (!ShadowMonarchManager.hasShadowForDisplay(entity, entry.type))
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowDismissButtonMessage(entry.id, x, y, z));
		ShadowDismissButtonMessage.handleButtonAction(entity, entry.id, x, y, z);
		layoutDismissButtons();
	}

	private void openSummon() {
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowDismissButtonMessage(100, x, y, z));
		ShadowDismissButtonMessage.handleButtonAction(entity, 100, x, y, z);
	}

	private void drawScrollHint(GuiGraphics g, int x, int y0, int y1, int count, int rows, int scroll) {
		int rowCount = (int) Math.ceil(count / 2.0D);
		if (rowCount <= rows)
			return;
		g.fill(x, y0, x + 2, y1, 0x443FC6FF);
		int trackH = y1 - y0;
		int knobH = Math.max(16, trackH * rows / rowCount);
		int knobY = y0 + (trackH - knobH) * scroll / Math.max(1, rowCount - rows);
		g.fill(x, knobY, x + 2, knobY + knobH, ACCENT_BLUE);
	}

	private static boolean isOverBox(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private record DismissEntry(int id, String name, String type) {
	}

	private static class DismissButton extends Button {
		private final DismissEntry entry;
		private int clipX;
		private int clipY;
		private int clipW;
		private int clipH;

		DismissButton(int x, int y, int w, int h, DismissEntry entry, OnPress onPress) {
			super(x, y, w, h, Component.literal(entry.name), onPress, DEFAULT_NARRATION);
			this.entry = entry;
		}

		void setClip(int x, int y, int w, int h) {
			this.clipX = x;
			this.clipY = y;
			this.clipW = w;
			this.clipH = h;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
			if (!visible)
				return;
			g.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
			boolean hovered = this.isHoveredOrFocused();
			g.fill(getX(), getY(), getX() + width, getY() + height, hovered ? 0x8843C8FF : 0x55102338);
			outline(g, getX(), getY(), width, height, hovered ? 0xFFFFFFFF : ACCENT_BLUE);
			Font font = Minecraft.getInstance().font;
			g.drawString(font, entry.name, getX() + 7, getY() + 5, TEXT_MAIN, false);
			Player player = Minecraft.getInstance().player;
			String count = player == null ? "0/0" : ShadowMonarchManager.shadowCountText(player, entry.type);
			g.drawString(font, count, getX() + width - font.width(count) - 7, getY() + 16, 0xFF9EDFFF, false);
			g.disableScissor();
		}
	}
}
