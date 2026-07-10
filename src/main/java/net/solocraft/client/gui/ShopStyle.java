package net.solocraft.client.gui;

import net.solocraft.procedures.GoldTextProcedure;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;

/**
 * Shared "System"-themed drawing helpers for the shop container screens
 * (Weapon / Food / Potion) and the shop selector. Pure {@link GuiGraphics}
 * rendering — a framed azure panel, corner brackets, styled slot cells and a
 * gold header — so it composes cleanly behind live item slots.
 */
public final class ShopStyle {
	public static final int ACCENT = 0xFF3FC6FF;
	public static final int ACCENT_DIM = 0xFF2273A8;
	public static final int ACCENT_SOFT = 0x553FC6FF;
	public static final int PANEL_FILL = 0x99070C1A; // translucent so the shader shows through
	public static final int SLOT_FILL = 0xB00B1524;
	public static final int TEXT_MAIN = 0xFFE8F6FF;
	public static final int TEXT_SUB = 0xFF8FB8D8;
	public static final int GOLD = 0xFFFFD966;

	private ShopStyle() {
	}

	/** Framed panel with glow border and corner brackets. (x,y) = top-left. */
	public static void panel(GuiGraphics g, int x, int y, int w, int h) {
		g.fill(x, y, x + w, y + h, PANEL_FILL);
		// outer glow
		g.fill(x - 1, y - 1, x + w + 1, y, ACCENT_SOFT);
		g.fill(x - 1, y + h, x + w + 1, y + h + 1, ACCENT_SOFT);
		g.fill(x - 1, y, x, y + h, ACCENT_SOFT);
		g.fill(x + w, y, x + w + 1, y + h, ACCENT_SOFT);
		// inner border
		g.fill(x, y, x + w, y + 1, ACCENT_DIM);
		g.fill(x, y + h - 1, x + w, y + h, ACCENT_DIM);
		g.fill(x, y, x + 1, y + h, ACCENT_DIM);
		g.fill(x + w - 1, y, x + w, y + h, ACCENT_DIM);
		corner(g, x, y, w, h);
	}

	private static void corner(GuiGraphics g, int x, int y, int w, int h) {
		int l = 10;
		g.fill(x - 1, y - 1, x + l, y + 1, ACCENT);
		g.fill(x - 1, y - 1, x + 1, y + l, ACCENT);
		g.fill(x + w - l, y - 1, x + w + 1, y + 1, ACCENT);
		g.fill(x + w - 1, y - 1, x + w + 1, y + l, ACCENT);
		g.fill(x - 1, y + h - 1, x + l, y + h + 1, ACCENT);
		g.fill(x - 1, y + h - l, x + 1, y + h + 1, ACCENT);
		g.fill(x + w - l, y + h - 1, x + w + 1, y + h + 1, ACCENT);
		g.fill(x + w - 1, y + h - l, x + w + 1, y + h + 1, ACCENT);
	}

	/** Title bar strip across the top of a panel. */
	public static void titleBar(GuiGraphics g, Font font, int x, int y, int w, String title) {
		g.fill(x, y, x + w, y + 16, 0x66102A3E);
		g.fill(x, y + 16, x + w, y + 17, ACCENT);
		g.drawString(font, title, x + (w - font.width(title)) / 2, y + 4, ACCENT, false);
	}

	/**
	 * Draws a slot cell whose contained 16×16 item sits at (slotX, slotY) — i.e.
	 * the same coordinates passed to a vanilla {@code Slot}. Call from renderBg
	 * with absolute (leftPos+relX) coordinates.
	 */
	public static void slot(GuiGraphics g, int slotX, int slotY) {
		int x = slotX - 1, y = slotY - 1;
		g.fill(x, y, x + 18, y + 18, SLOT_FILL);
		g.fill(x, y, x + 18, y + 1, ACCENT_DIM);
		g.fill(x, y + 17, x + 18, y + 18, ACCENT_DIM);
		g.fill(x, y, x + 1, y + 18, ACCENT_DIM);
		g.fill(x + 17, y, x + 18, y + 18, ACCENT_DIM);
	}

	/** Highlight overlay for a hovered shop slot. */
	public static void slotHover(GuiGraphics g, int slotX, int slotY) {
		int x = slotX - 1, y = slotY - 1;
		g.fill(x, y, x + 18, y + 18, 0x503FC6FF);
	}

	/** Gold header, e.g. "§6Total Gold: 1234". */
	public static void gold(GuiGraphics g, Font font, Entity entity, int x, int y) {
		g.drawString(font, GoldTextProcedure.execute(entity), x, y, GOLD, false);
	}
}
