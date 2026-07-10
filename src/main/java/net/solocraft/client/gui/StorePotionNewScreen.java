package net.solocraft.client.gui;

import net.solocraft.world.inventory.StorePotionNewMenu;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.network.StorePotionNewButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Potion shop — a 3×3 grid of potion slots (rows HP/MP/FTG, cols S/M/L) on the
 * animated System background. Click a slot to buy via the existing
 * {@link StorePotionNewButtonMessage} (id = slot index + 1).
 */
public class StorePotionNewScreen extends SystemContainerScreen<StorePotionNewMenu> {
	private final int x, y, z;
	private final Player entity;

	private static final int[] COLS = StorePotionNewMenu.COLS;
	private static final int[] ROWS = StorePotionNewMenu.ROWS;
	private static final String[] COL_PRICE = { "§b100g", "§b400g", "§b800g" };
	private static final String[] ROW_LABEL = { "§cHP", "§9MP", "§eFTG" };

	public StorePotionNewScreen(StorePotionNewMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -74;
		this.pRelY = -102;
		this.pW = 180;
		this.pH = 236;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int gx, int gy) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ShopStyle.panel(g, leftPos + pRelX, topPos + pRelY, pW, pH);
		ShopStyle.titleBar(g, this.font, leftPos + pRelX, topPos + pRelY, pW, "POTION SHOP");
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 3; c++)
				ShopStyle.slot(g, leftPos + COLS[c], topPos + ROWS[r]);
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				ShopStyle.slot(g, leftPos + -68 + sj * 18, topPos + 52 + si * 18);
		for (int si = 0; si < 9; ++si)
			ShopStyle.slot(g, leftPos + -68 + si * 18, topPos + 112);
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		ShopStyle.gold(g, this.font, entity, pRelX + 6, pRelY + 20);
		for (int c = 0; c < 3; c++) {
			int cx = COLS[c] + 8 - this.font.width(COL_PRICE[c]) / 2;
			g.drawString(this.font, COL_PRICE[c], cx, ROWS[0] - 13, ShopStyle.GOLD, false);
		}
		for (int r = 0; r < 3; r++)
			g.drawString(this.font, ROW_LABEL[r], COLS[0] - 30, ROWS[r] + 4, ShopStyle.TEXT_MAIN, false);
	}

	private int shopSlotAt(double mouseX, double mouseY) {
		for (int r = 0; r < 3; r++)
			for (int c = 0; c < 3; c++) {
				int sx = leftPos + COLS[c] - 1;
				int sy = topPos + ROWS[r] - 1;
				if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18)
					return r * 3 + c;
			}
		return -1;
	}

	@Override
	protected void renderExtras(GuiGraphics g, int mouseX, int mouseY) {
		int hovered = shopSlotAt(mouseX, mouseY);
		if (hovered >= 0)
			ShopStyle.slotHover(g, leftPos + COLS[hovered % 3], topPos + ROWS[hovered / 3]);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (isOpen() && button == 0) {
			int slot = shopSlotAt(mouseX, mouseY);
			if (slot >= 0) {
				int id = slot + 1;
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(id, x, y, z));
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public void init() {
		super.init();
		this.addRenderableWidget(new SystemScreen.SystemButton(leftPos + pRelX + 3, topPos + pRelY + 2, 40, 12, Component.literal("< Back"), b -> {
			SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(0, x, y, z));
		}));
	}
}
