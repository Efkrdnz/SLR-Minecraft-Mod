package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemPanelScreen;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.client.gui.system.SystemTooltip;
import net.solocraft.network.SpecialCraftingGUIButtonMessage;
import net.solocraft.world.inventory.SpecialCraftingGUIMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;

public class SpecialCraftingGUIScreen extends SystemContainerScreen<SpecialCraftingGUIMenu> {
	private static final int SLOT_0_X = -8;
	private static final int SLOT_0_Y = -120;
	private static final int SLOT_1_X = 42;
	private static final int SLOT_1_Y = -48;
	private static final int SLOT_2_X = -58;
	private static final int SLOT_2_Y = -48;
	private static final int OUTPUT_X = -8;
	private static final int OUTPUT_Y = -74;
	private static final int INV_X = -81;
	private static final int INV_Y = 28;
	private static final int HOTBAR_Y = 86;

	private final int x;
	private final int y;
	private final int z;
	private final Player entity;

	public SpecialCraftingGUIScreen(SpecialCraftingGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -113;
		this.pRelY = -140;
		this.pW = 226;
		this.pH = 286;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int gx, int gy) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		int ax = leftPos + pRelX;
		int ay = topPos + pRelY;
		ShopStyle.panel(g, ax, ay, pW, pH);
		ShopStyle.titleBar(g, this.font, ax, ay, pW, "SYSTEM CRAFTING");
		renderCraftingSigil(g);
		ShopStyle.slot(g, leftPos + SLOT_0_X, topPos + SLOT_0_Y);
		ShopStyle.slot(g, leftPos + SLOT_1_X, topPos + SLOT_1_Y);
		ShopStyle.slot(g, leftPos + SLOT_2_X, topPos + SLOT_2_Y);
		ShopStyle.slot(g, leftPos + OUTPUT_X, topPos + OUTPUT_Y);
		outlineSlot(g, leftPos + OUTPUT_X, topPos + OUTPUT_Y, 0xAA3FC6FF);
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				ShopStyle.slot(g, leftPos + INV_X + sj * 18, topPos + INV_Y + si * 18);
		for (int si = 0; si < 9; ++si)
			ShopStyle.slot(g, leftPos + INV_X + si * 18, topPos + HOTBAR_Y);
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(this.font, "INVENTORY", pRelX + 16, 14, ShopStyle.TEXT_SUB, false);
	}

	@Override
	protected void renderExtras(GuiGraphics g, int mouseX, int mouseY) {
		highlightHoveredSlot(g, mouseX, mouseY);
		List<Component> tip = slotTip(mouseX, mouseY);
		if (tip != null)
			SystemTooltip.render(g, this.font, tip, mouseX, mouseY, this.width, this.height);
	}

	@Override
	public void init() {
		super.init();
		this.addRenderableWidget(new SystemScreen.SystemButton(leftPos + pRelX + 4, topPos + pRelY + 3, 46, 13, Component.literal("< Back"), b -> {
			if (this.minecraft != null && this.minecraft.player != null) {
				this.minecraft.player.closeContainer();
				openSystemScreen(new SystemPanelScreen());
			}
		}));
		this.addRenderableWidget(new SystemScreen.SystemButton(leftPos - 34, topPos - 18, 68, 20, Component.literal("Craft"), b -> craft()));
	}

	private void craft() {
		SololevelingMod.PACKET_HANDLER.sendToServer(new SpecialCraftingGUIButtonMessage(0, x, y, z));
	}

	private void renderCraftingSigil(GuiGraphics g) {
		int cx = leftPos;
		int cy = topPos - 72;
		int glow = 0x553FC6FF;
		int topX = slotCenterX(SLOT_0_X);
		int topY = slotCenterY(SLOT_0_Y);
		int rightX = slotCenterX(SLOT_1_X);
		int rightY = slotCenterY(SLOT_1_Y);
		int leftX = slotCenterX(SLOT_2_X);
		int leftY = slotCenterY(SLOT_2_Y);
		drawLine(g, topX, topY, rightX, rightY, glow);
		drawLine(g, rightX, rightY, leftX, leftY, glow);
		drawLine(g, leftX, leftY, topX, topY, glow);
		drawRing(g, cx, cy, 54, glow);
		drawRing(g, cx, cy, 35, 0x334FB8E8);
	}

	private int slotCenterX(int slotX) {
		return leftPos + slotX + 8;
	}

	private int slotCenterY(int slotY) {
		return topPos + slotY + 8;
	}

	private void drawRing(GuiGraphics g, int cx, int cy, int r, int color) {
		for (int i = -r; i <= r; i += 3) {
			int h = (int) Math.sqrt(Math.max(0, r * r - i * i));
			if (Math.abs(i) % 9 == 0) {
				g.fill(cx + i, cy - h, cx + i + 1, cy - h + 2, color);
				g.fill(cx + i, cy + h - 1, cx + i + 1, cy + h + 1, color);
			}
			if (Math.abs(h) % 9 == 0) {
				g.fill(cx - h, cy + i, cx - h + 2, cy + i + 1, color);
				g.fill(cx + h - 1, cy + i, cx + h + 1, cy + i + 1, color);
			}
		}
	}

	private void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		while (true) {
			g.fill(x0, y0, x0 + 1, y0 + 1, color);
			if (x0 == x1 && y0 == y1)
				break;
			int e2 = err * 2;
			if (e2 > -dy) {
				err -= dy;
				x0 += sx;
			}
			if (e2 < dx) {
				err += dx;
				y0 += sy;
			}
		}
	}

	private List<Component> slotTip(int mouseX, int mouseY) {
		if (overSlot(mouseX, mouseY, SLOT_0_X, SLOT_0_Y))
			return List.of(tipTitle("Input I"), tipText("Blood, stick, or blaze rod."));
		if (overSlot(mouseX, mouseY, SLOT_1_X, SLOT_1_Y))
			return List.of(tipTitle("Input II"), tipText("World Tree fragment or Kamish tooth."));
		if (overSlot(mouseX, mouseY, SLOT_2_X, SLOT_2_Y))
			return List.of(tipTitle("Input III"), tipText("Spring water or S-rank mana crystal."));
		if (overSlot(mouseX, mouseY, OUTPUT_X, OUTPUT_Y))
			return List.of(tipTitle("Output"), tipText("Crafted result appears here."));
		return null;
	}

	private static Component tipTitle(String text) {
		return Component.literal(text).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
	}

	private static Component tipText(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GRAY);
	}

	private void highlightHoveredSlot(GuiGraphics g, int mouseX, int mouseY) {
		if (overSlot(mouseX, mouseY, SLOT_0_X, SLOT_0_Y))
			ShopStyle.slotHover(g, leftPos + SLOT_0_X, topPos + SLOT_0_Y);
		else if (overSlot(mouseX, mouseY, SLOT_1_X, SLOT_1_Y))
			ShopStyle.slotHover(g, leftPos + SLOT_1_X, topPos + SLOT_1_Y);
		else if (overSlot(mouseX, mouseY, SLOT_2_X, SLOT_2_Y))
			ShopStyle.slotHover(g, leftPos + SLOT_2_X, topPos + SLOT_2_Y);
		else if (overSlot(mouseX, mouseY, OUTPUT_X, OUTPUT_Y))
			ShopStyle.slotHover(g, leftPos + OUTPUT_X, topPos + OUTPUT_Y);
	}

	private void outlineSlot(GuiGraphics g, int slotX, int slotY, int color) {
		int x = slotX - 2;
		int y = slotY - 2;
		g.fill(x, y, x + 20, y + 1, color);
		g.fill(x, y + 19, x + 20, y + 20, color);
		g.fill(x, y, x + 1, y + 20, color);
		g.fill(x + 19, y, x + 20, y + 20, color);
	}

	private boolean overSlot(int mouseX, int mouseY, int slotX, int slotY) {
		int sx = leftPos + slotX - 1;
		int sy = topPos + slotY - 1;
		return mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18;
	}
}
