package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemQuestsScreen;
import net.solocraft.client.renderer.shader.DkcTowerBackgroundRenderTypes;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.network.PathButtonMessage;
import net.solocraft.world.inventory.PathMenu;

import net.minecraft.Util;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Scrollable, server-authored map of the Demon King's Castle.
 *
 * <p>Floor 1 lives at the bottom of the content and Floor 20 at the summit, so
 * the view opens scrolled down onto the floor the player currently occupies and
 * is climbed upward. The immutable state on {@link PathMenu} is only used for
 * presentation; every travel request is validated again against live DKC saved
 * data on the server.
 *
 * <p>The tower itself is drawn as a masonry shaft: each storey is a tapered
 * course of brickwork carrying a bolted iron plaque, joined by switchback
 * stairs. Only storeys inside the viewport are built each frame.
 */
public class PathScreen extends SystemContainerScreen<PathMenu> {
	private static final int PANEL_W = 360;
	private static final int PANEL_H = 294;

	private static final int HEADER_H = 26;
	private static final int VIEW_X = 9;
	private static final int VIEW_Y = 31;
	private static final int VIEW_W = 212;
	private static final int VIEW_H = 228;
	private static final int VIEW_HEADER_H = 16;
	/** Panel-relative top edge of the scrolling tower area. */
	private static final int TOWER_TOP = VIEW_Y + VIEW_HEADER_H;
	private static final int TOWER_H = VIEW_H - VIEW_HEADER_H - 1;
	/** Horizontal strip on the right of the viewport owned by the scrollbar. */
	private static final int SCROLLBAR_RESERVE = 12;

	private static final int DETAIL_X = 229;
	private static final int DETAIL_Y = 31;
	private static final int DETAIL_W = 122;
	private static final int DETAIL_H = 228;
	private static final int FOOTER_Y = 265;
	private static final int FOOTER_H = 20;

	private static final int FLOOR_STEP = 54;
	private static final int CONTENT_PAD = 30;
	private static final int FOUNDATION_H = 52;
	private static final int CONTENT_HEIGHT =
			CONTENT_PAD * 2 + (DkcFloorRegistry.LAST_FLOOR - 1) * FLOOR_STEP + FOUNDATION_H;

	/**
	 * Plaques stay narrower than the shaft at every height so a band of masonry
	 * and the stair flights always read on both sides; that band is what makes
	 * the column look like a tower rather than a list of cards.
	 */
	private static final int PLAQUE_W = 106;
	private static final int PLAQUE_H = 34;
	private static final int LANDMARK_W = 120;
	private static final int LANDMARK_H = 40;
	private static final int NUMERAL_W = 27;

	private static final int CRIMSON = 0xFFFF3A50;
	private static final int CRIMSON_HOT = 0xFFFF7A55;
	private static final int CRIMSON_DIM = 0xFF8E1F2F;
	private static final int CRIMSON_SOFT = 0x66FF354D;
	private static final int EMBER = 0xFFFF9A48;
	private static final int GOLD = 0xFFFFC463;
	private static final int GOLD_DIM = 0xFF9A6A25;
	private static final int VIOLET = 0xFFD48CFF;
	private static final int CLEARED = 0xFFCE6C78;
	private static final int LOCKED_BORDER = 0xFF5A4750;
	private static final int TEXT_MAIN = 0xFFFFEDF0;
	private static final int TEXT_SUB = 0xFFD5A8AF;
	private static final int TEXT_MUTED = 0xFF8E737A;
	private static final int VOID_INK = 0xFF060206;

	private DkcButton backButton;
	private DkcButton enterButton;
	private DkcButton focusButton;
	private DkcButton exitButton;

	private int selectedFloor;
	private int hoveredFloor;
	private float scroll;
	private float targetScroll;
	private boolean draggingTower;
	private boolean draggingScrollbar;
	private boolean returningToQuests;
	private boolean travelRequested;
	private int pendingFloor;
	private boolean pendingExit;

	/** Eased hover weight per floor; index 0 is unused so floors index directly. */
	private final float[] floorGlow = new float[DkcFloorRegistry.LAST_FLOOR + 1];
	private long selectionChangedAt;

	private float lastMouseX = 0.5F;
	private float lastMouseY = 0.5F;
	private float mouseVelocityX;
	private float mouseVelocityY;
	private boolean mouseSampled;
	private float focusRatio = -1.0F;

	public PathScreen(PathMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -PANEL_W / 2;
		this.pRelY = -PANEL_H / 2;
		this.pW = PANEL_W;
		this.pH = PANEL_H;
	}

	@Override
	protected void init() {
		this.returningToQuests = false;
		this.travelRequested = false;
		this.pendingFloor = 0;
		this.pendingExit = false;
		this.draggingTower = false;
		this.draggingScrollbar = false;
		this.mouseSampled = false;
		this.mouseVelocityX = 0.0F;
		this.mouseVelocityY = 0.0F;
		this.focusRatio = -1.0F;
		java.util.Arrays.fill(this.floorGlow, 0.0F);
		super.init();

		int left = panelLeft();
		int top = panelTop();
		// Opening inside the castle focuses the floor the player is standing on;
		// from outside it focuses the highest floor they have opened.
		int initialFloor = menu.insideDkc() && menu.currentFloor() > 0
				? menu.currentFloor()
				: menu.highestUnlockedFloor() > 0
						? menu.highestUnlockedFloor() : menu.currentFloor();
		this.selectedFloor = clampFloor(initialFloor <= 0 ? 1 : initialFloor);
		this.selectionChangedAt = Util.getMillis();
		this.targetScroll = focusOffset(selectedFloor);
		this.scroll = targetScroll;

		this.backButton = addRenderableWidget(new DkcButton(
				left + 6, top + 5, 46, 15, () -> Component.literal("< BACK"),
				() -> true, button -> returnToQuestHub()));
		boolean inside = menu.insideDkc();
		this.focusButton = addRenderableWidget(new DkcButton(
				left + VIEW_X, top + FOOTER_Y, inside ? 104 : VIEW_W, FOOTER_H,
				() -> Component.literal(menu.insideDkc() ? "FIND CURRENT" : "FIND HIGHEST"),
				() -> true, button -> focusProgressFloor()));
		if (inside) {
			this.exitButton = addRenderableWidget(new DkcButton(
					left + VIEW_X + 108, top + FOOTER_Y, 104, FOOTER_H,
					() -> Component.literal("EXIT CASTLE"),
					() -> !travelRequested, button -> exitCastle()));
		}
		this.enterButton = addRenderableWidget(new DkcButton(
				left + DETAIL_X + 4, top + FOOTER_Y, DETAIL_W - 8, FOOTER_H,
				this::enterButtonText, this::canEnterSelected,
				button -> enterSelectedFloor()));
	}

	// ── frame ──────────────────────────────────────────────────────────────────

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		advanceAnimation();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		// GuiGraphics.fill/fillGradient/drawString each call flushIfUnmanaged(),
		// so outside a managed batch every one of them is its own GL draw call
		// wrapped in a depth-test toggle. This panel issues a few thousand per
		// frame, so batching them is the single largest cost here. Scissor
		// changes still flush internally, which keeps draw order correct.
		graphics.drawManaged(() -> renderPanel(graphics, mouseX, mouseY));
		RenderSystem.disableBlend();
	}

	private void renderPanel(GuiGraphics graphics, int mouseX, int mouseY) {
		int left = panelLeft();
		int top = panelTop();

		graphics.fillGradient(left, top, left + PANEL_W, top + PANEL_H, 0x59120309, 0x6E030004);
		drawPanelFrame(graphics, left, top, PANEL_W, PANEL_H);
		renderHeader(graphics, left, top);

		int viewLeft = left + VIEW_X;
		int viewTop = top + VIEW_Y;
		renderViewportChrome(graphics, viewLeft, viewTop);

		int towerTop = top + TOWER_TOP;
		ResponsiveGuiScale.enableScissor(graphics, responsiveTransform(),
				viewLeft + 1, towerTop, viewLeft + VIEW_W - 1, towerTop + TOWER_H);
		renderTower(graphics, mouseX, mouseY, viewLeft, towerTop);
		graphics.disableScissor();

		renderScrollbar(graphics, viewLeft, towerTop);

		int detailLeft = left + DETAIL_X;
		int detailTop = top + DETAIL_Y;
		renderSelectionLink(graphics, viewLeft, towerTop, detailLeft);
		renderDetailPanel(graphics, detailLeft, detailTop);
		renderFooterRail(graphics, left, top);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		// All labels use absolute panel coordinates in renderBg.
	}

	private void renderHeader(GuiGraphics graphics, int left, int top) {
		graphics.fillGradient(left + 1, top + 1, left + PANEL_W - 1, top + HEADER_H,
				0xE62A0611, 0xCC0F0208);
		// Brushed-iron banding keeps the plate from reading as a flat rectangle.
		graphics.fill(left + 1, top + 8, left + PANEL_W - 1, top + 9, 0x18FFC7C0);
		graphics.fill(left + 1, top + 13, left + PANEL_W - 1, top + 14, 0x10000000);
		graphics.fill(left + 1, top + HEADER_H - 1, left + PANEL_W - 1, top + HEADER_H, CRIMSON_DIM);
		graphics.fill(left + PANEL_W / 3, top + HEADER_H - 1,
				left + PANEL_W * 2 / 3, top + HEADER_H, CRIMSON);

		String title = "DEMON KING'S CASTLE";
		int titleWidth = trackedWidth(title, 1);
		int titleX = left + (PANEL_W - titleWidth) / 2;
		drawTracked(graphics, title, titleX + 1, top + 8, 0xCC1A0208, 1);
		drawTracked(graphics, title, titleX, top + 7, CRIMSON_HOT, 1);
		drawOrnament(graphics, titleX - 9, top + 10, CRIMSON_DIM);
		drawOrnament(graphics, titleX + titleWidth + 7, top + 10, CRIMSON_DIM);

		String tally = String.format("%02d", menu.clearedFloors()) + "/"
				+ DkcFloorRegistry.LAST_FLOOR;
		int tallyWidth = this.font.width(tally);
		int tallyX = left + PANEL_W - 7 - tallyWidth;
		graphics.drawString(this.font, tally, tallyX, top + 7,
				menu.conquered() ? GOLD : CLEARED, false);
		String tallyLabel = "CLEARED";
		int labelWidth = trackedWidth(tallyLabel, 1);
		drawTracked(graphics, tallyLabel, tallyX - 5 - labelWidth, top + 7, TEXT_MUTED, 1);

		renderConquestBar(graphics, left + 8, top + 19, PANEL_W - 16);
	}

	/** Twenty segments summarising the whole climb, mirroring the scrollbar notches. */
	private void renderConquestBar(GuiGraphics graphics, int x, int y, int width) {
		int floors = DkcFloorRegistry.LAST_FLOOR;
		graphics.fill(x - 1, y - 1, x + width + 1, y + 5, 0x99050103);
		for (int floor = DkcFloorRegistry.FIRST_FLOOR; floor <= floors; floor++) {
			int index = floor - DkcFloorRegistry.FIRST_FLOOR;
			int x0 = x + (width * index) / floors;
			int x1 = x + (width * (index + 1)) / floors - 1;
			boolean cleared = menu.isFloorCleared(floor);
			boolean unlocked = menu.isFloorUnlocked(floor);
			int fill = cleared ? 0xFFB43046 : unlocked ? 0xFF5E1522 : 0xFF1B1218;
			graphics.fill(x0, y, x1, y + 4, fill);
			if (cleared)
				graphics.fill(x0, y, x1, y + 1, CRIMSON);
			if (isLandmark(floor))
				graphics.fill(x0, y + 3, x1, y + 4, cleared || unlocked ? GOLD : GOLD_DIM);
			if (menu.insideDkc() && menu.currentFloor() == floor) {
				int alpha = 150 + (int) (105.0F * pulse01());
				graphics.fill(x0 - 1, y - 1, x1 + 1, y + 5, (alpha << 24) | 0xFFC463);
			}
		}
	}

	private void renderViewportChrome(GuiGraphics graphics, int viewLeft, int viewTop) {
		graphics.fillGradient(viewLeft, viewTop, viewLeft + VIEW_W, viewTop + VIEW_H,
				0x66030104, 0x8C020003);
		drawOutline(graphics, viewLeft, viewTop, VIEW_W, VIEW_H, CRIMSON_DIM);
		graphics.fill(viewLeft + 1, viewTop + 1, viewLeft + VIEW_W - 1, viewTop + VIEW_HEADER_H - 1,
				0xB2200610);
		graphics.fill(viewLeft + 1, viewTop + VIEW_HEADER_H - 1,
				viewLeft + VIEW_W - 1, viewTop + VIEW_HEADER_H, CRIMSON_DIM);

		drawTracked(graphics, "ASCENT", viewLeft + 6, viewTop + 4, TEXT_SUB, 1);

		String marker = menu.insideDkc() && menu.currentFloor() > 0
				? "F" + String.format("%02d", menu.currentFloor())
				: "F" + String.format("%02d", clampFloor(selectedFloor));
		String suffix = " / " + DkcFloorRegistry.LAST_FLOOR;
		int suffixWidth = this.font.width(suffix);
		int markerWidth = this.font.width(marker);
		int markerX = viewLeft + VIEW_W - 8 - suffixWidth - markerWidth;
		graphics.drawString(this.font, marker, markerX, viewTop + 4,
				menu.insideDkc() ? GOLD : CRIMSON_HOT, false);
		graphics.drawString(this.font, suffix, markerX + markerWidth, viewTop + 4,
				TEXT_MUTED, false);
	}

	/** Ties the highlighted plaque to the dossier so selection reads as one gesture. */
	private void renderSelectionLink(GuiGraphics graphics, int viewLeft, int towerTop,
			int detailLeft) {
		NodeBounds node = nodeBounds(clampFloor(selectedFloor), viewLeft, towerTop,
				Math.round(scroll));
		int linkY = node.y() + node.height() / 2;
		if (linkY < towerTop + 4 || linkY > towerTop + TOWER_H - 4)
			return;
		int x0 = viewLeft + VIEW_W;
		int x1 = detailLeft;
		// A short flash travels the link whenever the selection changes.
		float flash = Math.max(0.0F, 1.0F - (Util.getMillis() - selectionChangedAt) / 320.0F);
		graphics.fill(x0, linkY, x1, linkY + 1, flash > 0.0F ? CRIMSON_HOT : CRIMSON_DIM);
		graphics.fill(x1 - 3, linkY - 2, x1 - 2, linkY + 3, CRIMSON_HOT);
		graphics.fill(x1 - 2, linkY - 1, x1 - 1, linkY + 2, CRIMSON_HOT);
		int pulseX = x0 + Math.round((x1 - x0 - 2) * (flash > 0.0F ? 1.0F - flash : pulse01()));
		graphics.fill(pulseX, linkY - 1, pulseX + 2, linkY + 2, GOLD);
	}

	/** Hairline rail beneath the columns so the footer buttons sit on something. */
	private void renderFooterRail(GuiGraphics graphics, int left, int top) {
		int y = top + FOOTER_Y - 4;
		graphics.fill(left + VIEW_X, y, left + VIEW_X + VIEW_W, y + 1, 0x66571623);
		graphics.fill(left + DETAIL_X, y, left + DETAIL_X + DETAIL_W, y + 1, 0x66571623);
	}

	// ── tower ──────────────────────────────────────────────────────────────────

	private void renderTower(GuiGraphics graphics, int mouseX, int mouseY,
			int viewLeft, int towerTop) {
		int centerX = viewLeft + (VIEW_W - SCROLLBAR_RESERVE) / 2;
		int towerBottom = towerTop + TOWER_H;
		int roundedScroll = Math.round(scroll);
		this.hoveredFloor = floorAt(mouseX, mouseY, viewLeft, towerTop);
		float time = (Util.getMillis() % 600000L) / 1000.0F;

		renderHaze(graphics, viewLeft, towerTop, roundedScroll);

		int summitTop = towerTop + floorContentY(DkcFloorRegistry.LAST_FLOOR) - roundedScroll;
		if (summitTop > towerTop - 40)
			renderSummit(graphics, centerX, summitTop);

		for (int floor = DkcFloorRegistry.LAST_FLOOR; floor >= DkcFloorRegistry.FIRST_FLOOR; floor--) {
			int y = towerTop + floorContentY(floor) - roundedScroll;
			if (y > towerBottom + FLOOR_STEP || y + FLOOR_STEP < towerTop - FLOOR_STEP)
				continue;
			renderStorey(graphics, floor, centerX, y);
		}

		int baseBottom = towerTop + floorContentY(DkcFloorRegistry.FIRST_FLOOR)
				+ FLOOR_STEP - roundedScroll;
		if (baseBottom < towerBottom + 40)
			renderFoundation(graphics, centerX, baseBottom);

		for (int floor = DkcFloorRegistry.LAST_FLOOR; floor >= DkcFloorRegistry.FIRST_FLOOR; floor--) {
			NodeBounds node = nodeBounds(floor, viewLeft, towerTop, roundedScroll);
			if (node.y() > towerBottom + 8 || node.y() + node.height() < towerTop - 8)
				continue;
			renderFloorPlaque(graphics, floor, node);
		}

		renderEmbers(graphics, viewLeft + 1, towerTop, VIEW_W - 2, TOWER_H, time);
		// Soft cuts at both ends so the clipped content fades instead of shearing.
		graphics.fillGradient(viewLeft + 1, towerTop, viewLeft + VIEW_W - 1, towerTop + 20,
				0xE6060206, 0x00060206);
		graphics.fillGradient(viewLeft + 1, towerBottom - 24, viewLeft + VIEW_W - 1, towerBottom,
				0x00060206, 0xE6060206);
	}

	/** Distant fog shelves drifting behind the shaft at a slower parallax rate. */
	private void renderHaze(GuiGraphics graphics, int viewLeft, int towerTop, int roundedScroll) {
		int parallax = Math.round(roundedScroll * 0.34F);
		for (int band = 0; band < 9; band++) {
			int bandY = towerTop + Math.floorMod(band * 74 - parallax, TOWER_H + 150) - 60;
			if (bandY + 26 < towerTop || bandY > towerTop + TOWER_H)
				continue;
			int alpha = 22 + (band % 3) * 9;
			graphics.fillGradient(viewLeft + 1, bandY, viewLeft + VIEW_W - 1, bandY + 26,
					(alpha << 24) | 0x521022, 0x00320A15);
		}
	}

	private void renderStorey(GuiGraphics graphics, int floor, int centerX, int y) {
		int halfWidth = shaftHalfWidth(floor);
		int top = y;
		int bottom = y + FLOOR_STEP;
		boolean lit = menu.isFloorUnlocked(floor);
		boolean cleared = menu.isFloorCleared(floor);

		graphics.fill(centerX - halfWidth, top, centerX + halfWidth, bottom,
				lit ? 0xF21A0C12 : 0xF20E080E);
		renderMasonry(graphics, floor, centerX, halfWidth, top + 5, bottom);
		renderShaftShading(graphics, centerX, halfWidth, top, bottom);

		// Cornice: a wider slab that reads as the ceiling of the storey below.
		int overhang = 5;
		graphics.fill(centerX - halfWidth - overhang, top,
				centerX + halfWidth + overhang, top + 5, lit ? 0xFF20111A : 0xFF150E15);
		graphics.fill(centerX - halfWidth - overhang, top,
				centerX + halfWidth + overhang, top + 1, lit ? 0x99A03448 : 0x66463840);
		graphics.fill(centerX - halfWidth - overhang, top + 5,
				centerX + halfWidth + overhang, top + 6, 0x8C000000);

		// Pilasters plus an outer rim: without the rim the dark shaft dissolves
		// into the dark sky and the silhouette stops reading.
		graphics.fill(centerX - halfWidth, top, centerX - halfWidth + 2, bottom,
				cleared ? 0xCC9E3446 : lit ? 0xBB7E2434 : 0x88463840);
		graphics.fill(centerX + halfWidth - 2, top, centerX + halfWidth, bottom,
				cleared ? 0x996E2130 : lit ? 0x88501824 : 0x66302428);
		graphics.fill(centerX - halfWidth - 1, top, centerX - halfWidth, bottom, 0xCC000000);
		graphics.fill(centerX + halfWidth, top, centerX + halfWidth + 1, bottom, 0xCC000000);

		int selfHalf = (isLandmark(floor) ? LANDMARK_H : PLAQUE_H) / 2;
		int nextHalf = floor > DkcFloorRegistry.FIRST_FLOOR
				? (isLandmark(floor - 1) ? LANDMARK_H : PLAQUE_H) / 2 : 8;
		renderStairFlight(graphics, floor, centerX,
				top + selfHalf + 1, top + FLOOR_STEP - nextHalf - 1, lit);
		// Sconces sit in the gap below the plaque, where they are never covered.
		renderTorch(graphics, centerX - halfWidth + 7, top + 24, floor, lit);
		renderTorch(graphics, centerX + halfWidth - 9, top + 24, floor * 7 + 3, lit);
	}

	private void renderMasonry(GuiGraphics graphics, int floor, int centerX, int halfWidth,
			int top, int bottom) {
		int left = centerX - halfWidth + 2;
		int right = centerX + halfWidth - 2;
		// Course and block pitch are deliberately coarse: this is the densest
		// loop on the screen and larger blocks read better as castle stone.
		int courseHeight = 7;
		int course = 0;
		for (int cy = top; cy < bottom; cy += courseHeight, course++) {
			int courseBottom = Math.min(bottom, cy + courseHeight);
			graphics.fill(left, cy, right, cy + 1, 0x4D000000);
			graphics.fill(left, cy + 1, right, cy + 2, 0x14FFD2C4);
			int offset = (course & 1) == 0 ? 0 : 10;
			for (int bx = left + offset; bx < right; bx += 20) {
				float seed = hash01(floor * 37 + course, bx);
				int blockRight = Math.min(right, bx + 19);
				if (seed > 0.86F)
					graphics.fill(bx + 1, cy + 2, blockRight, courseBottom, 0x40000000);
				else if (seed < 0.14F)
					graphics.fill(bx + 1, cy + 2, blockRight, courseBottom, 0x1FFFD2C4);
				if (bx > left)
					graphics.fill(bx, cy + 1, bx + 1, courseBottom, 0x40000000);
			}
		}
	}

	/** Vertical strips fake a cylindrical shaft lit from the upper left. */
	private static void renderShaftShading(GuiGraphics graphics, int centerX, int halfWidth,
			int top, int bottom) {
		int strips = 8;
		for (int index = 0; index < strips; index++) {
			int x0 = centerX - halfWidth + (2 * halfWidth * index) / strips;
			int x1 = centerX - halfWidth + (2 * halfWidth * (index + 1)) / strips;
			float t = (index + 0.5F) / strips;
			float curvature = 1.0F - Math.abs(t * 2.0F - 1.0F);
			// Kept light enough that the masonry beside the plaques stays legible.
			int shadow = (int) (112 * (1.0F - curvature) * (t > 0.5F ? 1.0F : 0.45F));
			if (shadow > 3)
				graphics.fill(x0, top, x1, bottom, shadow << 24);
			float light = Math.max(0.0F, 1.0F - Math.abs(t - 0.34F) * 3.1F);
			int warm = (int) (52 * light);
			if (warm > 3)
				graphics.fill(x0, top, x1, bottom, (warm << 24) | 0xFF9A66);
		}
	}

	/** Switchback stairs in the gap under each plaque, alternating direction. */
	private void renderStairFlight(GuiGraphics graphics, int floor, int centerX,
			int top, int bottom, boolean lit) {
		int height = bottom - top;
		if (height < 8)
			return;
		int steps = 4;
		int run = 56;
		int stepWidth = run / steps;
		int stepHeight = Math.max(2, height / steps);
		boolean leftToRight = (floor & 1) == 0;
		int tread = lit ? 0xFF4E2A34 : 0xFF2A1F27;
		int nosing = lit ? 0xEEC24A5C : 0x99584850;
		for (int index = 0; index < steps; index++) {
			int sx = leftToRight
					? centerX - run / 2 + index * stepWidth
					: centerX + run / 2 - (index + 1) * stepWidth;
			int sy = bottom - (index + 1) * stepHeight;
			graphics.fill(sx, sy, sx + stepWidth, sy + stepHeight, tread);
			graphics.fill(sx, sy, sx + stepWidth, sy + 1, nosing);
			graphics.fill(sx, sy, sx + 1, sy + stepHeight, 0x66000000);
		}
		// Landing doorway at the head of the flight, sized to the visible gap.
		int doorX = leftToRight ? centerX + run / 2 : centerX - run / 2 - 11;
		int doorTop = bottom - steps * stepHeight;
		graphics.fill(doorX, doorTop, doorX + 11, bottom, 0xF2050203);
		graphics.fill(doorX, doorTop, doorX + 11, doorTop + 1, lit ? 0xCC912B3A : 0x66403138);
		if (lit) {
			int alpha = 80 + (int) (70.0F * pulse01());
			graphics.fill(doorX + 2, doorTop + 2, doorX + 9, bottom, (alpha << 24) | 0xFF5A2A);
		}
	}

	private void renderTorch(GuiGraphics graphics, int x, int y, int seed, boolean lit) {
		graphics.fill(x, y + 3, x + 2, y + 8, 0xFF2A1A1E);
		graphics.fill(x, y + 7, x + 2, y + 8, 0xFF120A0C);
		if (!lit)
			return;
		float flicker = 0.55F + 0.45F * (float) Math.sin(Util.getMillis() * 0.011 + seed * 1.7);
		int height = 3 + Math.round(flicker * 2.0F);
		graphics.fill(x, y + 3 - height, x + 2, y + 3, EMBER);
		graphics.fill(x, y + 2 - height, x + 2, y + 3 - height, GOLD);
		int glow = 34 + (int) (34 * flicker);
		graphics.fill(x - 3, y - 3, x + 5, y + 6, (glow << 24) | 0xFF7A30);
		graphics.fill(x - 5, y - 1, x + 7, y + 4, ((glow / 2) << 24) | 0xFF5A20);
	}

	/** Battlements and the throne spire capping Floor 20. */
	private void renderSummit(GuiGraphics graphics, int centerX, int summitTop) {
		int halfWidth = shaftHalfWidth(DkcFloorRegistry.LAST_FLOOR) + 5;
		boolean conquered = menu.conquered();
		int merlon = conquered ? 0xFF32161E : 0xFF1E1018;
		for (int x = centerX - halfWidth; x < centerX + halfWidth; x += 13) {
			int x1 = Math.min(centerX + halfWidth, x + 8);
			graphics.fill(x, summitTop - 9, x1, summitTop, merlon);
			graphics.fill(x, summitTop - 9, x1, summitTop - 8, conquered ? GOLD_DIM : 0xFF4A2C34);
		}
		graphics.fill(centerX - halfWidth, summitTop - 12, centerX + halfWidth, summitTop - 9,
				0xFF241219);

		// Tapered spire with a beacon that only burns once the summit is open.
		for (int step = 0; step < 11; step++) {
			int spireHalf = 11 - step;
			int y = summitTop - 13 - step * 3;
			graphics.fill(centerX - spireHalf, y - 3, centerX + spireHalf, y, 0xFF1B0E15);
			graphics.fill(centerX - spireHalf, y - 3, centerX - spireHalf + 1, y, 0xFF43222C);
		}
		boolean summitOpen = menu.isFloorUnlocked(DkcFloorRegistry.LAST_FLOOR);
		int beaconY = summitTop - 50;
		int alpha = summitOpen ? 150 + (int) (105.0F * pulse01()) : 60;
		graphics.fill(centerX - 2, beaconY, centerX + 2, beaconY + 5,
				(alpha << 24) | (summitOpen ? 0xFFC463 : 0x6B4A55));
		if (summitOpen) {
			graphics.fill(centerX - 5, beaconY + 1, centerX + 5, beaconY + 3, 0x55FFC463);
			graphics.fill(centerX - 1, beaconY - 6, centerX + 1, beaconY, 0x44FFC463);
		}
	}

	/** Widening plinth and the gatehouse arch beneath Floor 1. */
	private void renderFoundation(GuiGraphics graphics, int centerX, int baseBottom) {
		int halfWidth = shaftHalfWidth(DkcFloorRegistry.FIRST_FLOOR);
		for (int step = 0; step < 5; step++) {
			int stepHalf = halfWidth + 3 + step * 5;
			int y = baseBottom + step * 6;
			graphics.fill(centerX - stepHalf, y, centerX + stepHalf, y + 6, 0xFF150C13);
			graphics.fill(centerX - stepHalf, y, centerX + stepHalf, y + 1, 0xFF33202A);
		}
		int gateHalf = 15;
		int gateTop = baseBottom + 4;
		int gateBottom = baseBottom + 30;
		graphics.fill(centerX - gateHalf, gateTop, centerX + gateHalf, gateBottom, 0xFF070305);
		for (int step = 0; step < 4; step++)
			graphics.fill(centerX - gateHalf + step, gateTop - 4 + step,
					centerX + gateHalf - step, gateTop - 3 + step, 0xFF2A1620);
		int alpha = 90 + (int) (60.0F * pulse01());
		graphics.fill(centerX - gateHalf + 3, gateTop + 3, centerX + gateHalf - 3, gateBottom,
				(alpha << 24) | 0xC42A1E);
		graphics.fill(centerX - 1, gateTop + 3, centerX + 1, gateBottom, 0x77FF6A3C);
	}

	private void renderEmbers(GuiGraphics graphics, int left, int top, int width, int height,
			float time) {
		for (int mote = 0; mote < 34; mote++) {
			float speed = 8.0F + (mote % 11) * 3.4F;
			float sway = (float) Math.sin(time * 0.9 + mote * 1.37) * 5.0F;
			int x = left + (int) (hash01(mote, 3) * width + sway);
			int cycle = height + 60;
			int y = top + height - (int) ((time * speed + hash01(mote, 7) * cycle) % cycle) + 30;
			if (x < left || x >= left + width || y < top || y >= top + height)
				continue;
			boolean bright = mote % 6 == 0;
			int size = bright ? 2 : 1;
			graphics.fill(x, y, x + size, y + size + 1, bright ? 0xCCFFB055 : 0x99FF4436);
			if (bright)
				graphics.fill(x, y + 2, x + 1, y + 5, 0x44FF6A3C);
		}
	}

	// ── plaques ────────────────────────────────────────────────────────────────

	private void renderFloorPlaque(GuiGraphics graphics, int floor, NodeBounds node) {
		boolean unlocked = menu.isFloorUnlocked(floor);
		boolean cleared = menu.isFloorCleared(floor);
		boolean here = menu.insideDkc() && menu.currentFloor() == floor;
		boolean selected = selectedFloor == floor;
		boolean landmark = isLandmark(floor);
		float glow = floorGlow[floor];

		int x = node.x();
		int y = node.y();
		int width = node.width();
		int height = node.height();
		int cut = 4;

		fillChamfer(graphics, x + 2, y + 3, width, height, cut, 0x82000000);

		// Landmark descriptor rides a tab in the gap above the plaque, which keeps
		// the state row full width no matter how long the badge text is.
		if (landmark) {
			String badge = floor == 15 ? radiruBadge() : "BOSS";
			int badgeWidth = this.font.width(badge);
			int tabWidth = badgeWidth + 12;
			int tabX = x + width - 8 - tabWidth;
			int accent = floor == 15 ? VIOLET : GOLD;
			fillChamfer(graphics, tabX, y - 10, tabWidth, 13, 3,
					floor == 15 ? 0xE62A1040 : 0xE6432A0C);
			outlineChamfer(graphics, tabX, y - 10, tabWidth, 13, 3,
					unlocked ? accent : LOCKED_BORDER);
			graphics.drawString(this.font, badge, tabX + 6, y - 7,
					unlocked ? accent : TEXT_MUTED, false);
		}

		int topFill = here ? 0xF2551C13 : cleared ? 0xF2371A21 : unlocked ? 0xF23C1520 : 0xF21D1922;
		int bottomFill = here ? 0xF21F0906 : cleared ? 0xF2160A0F : unlocked ? 0xF2170810 : 0xF20D0A11;
		fillChamfer(graphics, x, y, width, height, cut, topFill);
		graphics.fillGradient(x + 1, y + cut, x + width - 1, y + height - cut, 0x00000000,
				bottomFill & 0xB2FFFFFF);
		// Speckled corrosion breaks up the flat plate without a texture atlas.
		for (int fleck = 0; fleck < 9; fleck++) {
			int fx = x + 4 + (int) (hash01(floor * 13 + fleck, 91) * (width - 9));
			int fy = y + 3 + (int) (hash01(fleck, floor * 5 + 41) * (height - 7));
			graphics.fill(fx, fy, fx + 1, fy + 1, fleck % 3 == 0 ? 0x22FFD8CC : 0x33000000);
		}

		int border = here ? GOLD : cleared ? CLEARED : unlocked ? CRIMSON : LOCKED_BORDER;
		if (selected)
			border = 0xFFFFE3D6;
		outlineChamfer(graphics, x, y, width, height, cut, border);
		graphics.fill(x + cut, y + 1, x + width - cut, y + 2, 0x33FFFFFF);
		graphics.fill(x + cut, y + height - 2, x + width - cut, y + height - 1, 0x44000000);

		int numeralRight = x + NUMERAL_W;
		graphics.fill(x + 1, y + cut, numeralRight, y + height - cut,
				unlocked ? 0x66120409 : 0x660A0810);
		graphics.fill(numeralRight, y + 3, numeralRight + 1, y + height - 3,
				unlocked ? 0x99862431 : 0x66443840);
		String numeral = String.valueOf(floor);
		int numeralColor = here ? GOLD : cleared ? CLEARED
				: unlocked ? 0xFFFFD9D2 : 0xFF6B565E;
		int numeralX = x + 1 + (NUMERAL_W - 1 - this.font.width(numeral) * 2) / 2;
		int numeralY = y + (height - 16) / 2;
		drawScaled(graphics, numeral, numeralX + 1, numeralY + 1, 2.0F, 0xCC000000);
		drawScaled(graphics, numeral, numeralX, numeralY, 2.0F, numeralColor);

		int textLeft = numeralRight + 5;
		int textWidth = x + width - 5 - textLeft;
		List<FormattedCharSequence> nameLines =
				this.font.split(Component.literal(DkcFloorRegistry.name(floor)), textWidth);
		int nameCount = Math.max(1, Math.min(2, nameLines.size()));
		// Centre the name block in the space left above the state row.
		int nameTop = y + 3 + Math.max(0, (height - 15 - nameCount * 9) / 2);
		for (int line = 0; line < nameCount; line++)
			graphics.drawString(this.font, nameLines.get(line), textLeft, nameTop + line * 9,
					unlocked ? TEXT_MAIN : TEXT_MUTED, false);

		int stateY = y + height - 12;
		int stateColor = floorStateColor(floor);
		graphics.fill(textLeft, stateY + 2, textLeft + 3, stateY + 5, stateColor);
		String state = shortFloorState(floor);
		graphics.drawString(this.font, state, textLeft + 6, stateY, stateColor, false);
		// Defender count fills the slack on the right of the state row when it fits.
		if (!DkcFloorRegistry.isBossFloor(floor)) {
			String count = "x" + DkcFloorRegistry.requiredKills(floor);
			int countX = x + width - 6 - this.font.width(count);
			if (countX > textLeft + 10 + this.font.width(state))
				graphics.drawString(this.font, count, countX, stateY,
						unlocked ? TEXT_MUTED : 0xFF5C4A52, false);
		}

		drawRivet(graphics, x + 4, y + 4, border);
		drawRivet(graphics, x + width - 6, y + 4, border);
		drawRivet(graphics, x + 4, y + height - 6, border);
		drawRivet(graphics, x + width - 6, y + height - 6, border);

		if (glow > 0.01F) {
			int alpha = (int) (58 * glow);
			fillChamfer(graphics, x, y, width, height, cut, (alpha << 24) | 0xFF7A55);
			int sweep = x + 2 + Math.round((width - 5) * pulse01());
			graphics.fill(sweep, y + 2, sweep + 1, y + height - 2, ((int) (70 * glow) << 24) | 0xFFFFFF);
		}
		if (selected) {
			outlineChamfer(graphics, x - 2, y - 2, width + 4, height + 4, cut + 1, CRIMSON_SOFT);
			int tick = 6;
			graphics.fill(x - 3, y - 3, x - 3 + tick, y - 2, CRIMSON_HOT);
			graphics.fill(x - 3, y - 3, x - 2, y - 3 + tick, CRIMSON_HOT);
			graphics.fill(x + width + 3 - tick, y - 3, x + width + 3, y - 2, CRIMSON_HOT);
			graphics.fill(x + width + 2, y - 3, x + width + 3, y - 3 + tick, CRIMSON_HOT);
			graphics.fill(x - 3, y + height + 2, x - 3 + tick, y + height + 3, CRIMSON_HOT);
			graphics.fill(x - 3, y + height + 3 - tick, x - 2, y + height + 3, CRIMSON_HOT);
			graphics.fill(x + width + 3 - tick, y + height + 2, x + width + 3, y + height + 3, CRIMSON_HOT);
			graphics.fill(x + width + 2, y + height + 3 - tick, x + width + 3, y + height + 3, CRIMSON_HOT);
		}
		if (here) {
			int alpha = 110 + (int) (110.0F * pulse01());
			outlineChamfer(graphics, x - 1, y - 1, width + 2, height + 2, cut, (alpha << 24) | 0xFFC463);
			// Pennant marking the floor the player physically stands on. It sits on
			// the left so it never fights the landmark tab on the right.
			graphics.fill(x + 9, y - 7, x + 15, y + 3, 0xFFB4243A);
			graphics.fill(x + 9, y - 7, x + 15, y - 6, GOLD);
			graphics.fill(x + 9, y + 3, x + 11, y + 5, 0xFFB4243A);
			graphics.fill(x + 13, y + 3, x + 15, y + 5, 0xFFB4243A);
		}
	}

	// ── scrollbar ──────────────────────────────────────────────────────────────

	private void renderScrollbar(GuiGraphics graphics, int viewLeft, int towerTop) {
		int trackX = viewLeft + VIEW_W - 8;
		int trackWidth = 5;
		int trackTop = towerTop + 3;
		int trackBottom = towerTop + TOWER_H - 3;
		graphics.fillGradient(trackX, trackTop, trackX + trackWidth, trackBottom,
				0xC0180409, 0xC00A0206);
		drawOutline(graphics, trackX, trackTop, trackWidth, trackBottom - trackTop, 0x88571422);

		// One notch per floor doubles as a progress read-out while dragging.
		int span = trackBottom - trackTop - 5;
		for (int floor = DkcFloorRegistry.FIRST_FLOOR; floor <= DkcFloorRegistry.LAST_FLOOR; floor++) {
			float t = (DkcFloorRegistry.LAST_FLOOR - floor)
					/ (float) (DkcFloorRegistry.LAST_FLOOR - DkcFloorRegistry.FIRST_FLOOR);
			int notchY = trackTop + 2 + Math.round(t * span);
			boolean landmark = isLandmark(floor);
			int color = menu.isFloorCleared(floor) ? CLEARED
					: menu.isFloorUnlocked(floor) ? CRIMSON_DIM : 0xFF2E2229;
			int inset = landmark ? 0 : 1;
			graphics.fill(trackX + inset, notchY, trackX + trackWidth - inset, notchY + 1,
					landmark && (menu.isFloorUnlocked(floor) || menu.isFloorCleared(floor))
							? GOLD : color);
			if (floor == clampFloor(selectedFloor)) {
				graphics.fill(trackX - 4, notchY - 1, trackX - 1, notchY + 2, CRIMSON_HOT);
				graphics.fill(trackX - 2, notchY, trackX, notchY + 1, TEXT_MAIN);
			}
			if (menu.insideDkc() && menu.currentFloor() == floor) {
				int alpha = 150 + (int) (105.0F * pulse01());
				graphics.fill(trackX + trackWidth + 1, notchY - 1,
						trackX + trackWidth + 4, notchY + 2, (alpha << 24) | 0xFFC463);
			}
		}

		float maximum = maxScroll();
		int usable = TOWER_H - 8;
		int thumbHeight = Math.max(24, Math.round(usable * (TOWER_H / (float) CONTENT_HEIGHT)));
		int travel = Math.max(1, usable - thumbHeight);
		int thumbY = towerTop + 4 + (maximum <= 0.0F ? 0 : Math.round((scroll / maximum) * travel));
		// Hollow thumb so the notches underneath stay legible.
		graphics.fill(trackX - 1, thumbY, trackX + trackWidth + 1, thumbY + thumbHeight, 0x44FF3A50);
		drawOutline(graphics, trackX - 1, thumbY, trackWidth + 2, thumbHeight, CRIMSON_HOT);
		graphics.fill(trackX - 1, thumbY, trackX + trackWidth + 1, thumbY + 2, CRIMSON);
		graphics.fill(trackX - 1, thumbY + thumbHeight - 2, trackX + trackWidth + 1,
				thumbY + thumbHeight, CRIMSON);
		int gripY = thumbY + thumbHeight / 2 - 3;
		for (int grip = 0; grip < 3; grip++)
			graphics.fill(trackX + 1, gripY + grip * 3, trackX + trackWidth - 1,
					gripY + grip * 3 + 1, 0x99FFC7C0);
	}

	// ── dossier ────────────────────────────────────────────────────────────────

	private void renderDetailPanel(GuiGraphics graphics, int x, int y) {
		int floor = clampFloor(selectedFloor);
		boolean landmark = isLandmark(floor);
		int inner = DETAIL_W - 12;

		graphics.fillGradient(x, y, x + DETAIL_W, y + DETAIL_H, 0xD9200711, 0xC4050103);
		drawOutline(graphics, x, y, DETAIL_W, DETAIL_H, CRIMSON_DIM);
		drawCornerBrackets(graphics, x, y, DETAIL_W, DETAIL_H, CRIMSON);
		graphics.fill(x + 1, y + 1, x + DETAIL_W - 1, y + 2, 0x22FFC7C0);

		// Header: the floor number carries the hierarchy, the sigil the identity.
		graphics.fillGradient(x + 1, y + 1, x + DETAIL_W - 1, y + 42, 0x8C3A0C1B, 0x00220611);
		drawTracked(graphics, "FLOOR", x + 8, y + 7, TEXT_MUTED, 1);
		String numeral = String.format("%02d", floor);
		drawScaled(graphics, numeral, x + 8, y + 18, 2.0F, 0xCC000000);
		drawScaled(graphics, numeral, x + 7, y + 17, 2.0F, landmark ? GOLD : CRIMSON_HOT);
		renderFloorSigil(graphics, x + DETAIL_W - 25, y + 21, floor);

		int cursor = y + 44;
		graphics.fill(x + 6, cursor, x + DETAIL_W - 6, cursor + 1, 0x772C0A14);
		drawOrnament(graphics, x + DETAIL_W / 2, cursor, CRIMSON_DIM);
		cursor += 6;

		List<FormattedCharSequence> nameLines =
				this.font.split(Component.literal(DkcFloorRegistry.name(floor)), inner);
		for (int line = 0; line < Math.min(2, nameLines.size()); line++) {
			graphics.drawString(this.font, nameLines.get(line), x + 6, cursor, TEXT_MAIN, false);
			cursor += 10;
		}
		cursor += 2;

		int stateColor = floorStateColor(floor);
		String state = longFloorState(floor);
		int chipWidth = Math.min(inner, this.font.width(state) + 12);
		fillChamfer(graphics, x + 6, cursor, chipWidth, 12, 3, 0x66000000);
		outlineChamfer(graphics, x + 6, cursor, chipWidth, 12, 3, stateColor);
		graphics.fill(x + 10, cursor + 4, x + 13, cursor + 7, stateColor);
		graphics.drawString(this.font, state, x + 16, cursor + 2, stateColor, false);
		// Permit rides alongside the chip rather than claiming a row of its own.
		if (floor > 1 && menu.isTransitionArmed(floor - 1)) {
			int permitX = x + 8 + chipWidth;
			if (permitX + this.font.width("PERMIT") + 6 <= x + DETAIL_W - 6) {
				graphics.fill(permitX, cursor + 1, permitX + 2, cursor + 11, CLEARED);
				graphics.drawString(this.font, "PERMIT", permitX + 4, cursor + 2, CLEARED, false);
			}
		}
		cursor += 16;

		cursor = drawSection(graphics, "OBJECTIVE", x + 6, cursor, inner);
		cursor = drawWrapped(graphics, floorObjective(floor), x + 6, cursor, inner, TEXT_SUB, 4);
		cursor += 4;

		cursor = drawSection(graphics, "GARRISON", x + 6, cursor, inner);
		cursor = renderGarrison(graphics, floor, x + 6, cursor, inner);
		cursor += 4;

		cursor = drawSection(graphics, "SPOILS", x + 6, cursor, inner);
		cursor = drawWrapped(graphics, floorReward(floor), x + 6, cursor, inner, TEXT_SUB, 4);

		// Formation state is pinned to the bottom so the dossier never reflows it.
		int footY = y + DETAIL_H - 13;
		graphics.fill(x + 6, footY - 4, x + DETAIL_W - 6, footY - 3, 0x552C0A14);
		boolean formed = menu.isFloorGenerated(floor);
		String generation = formed ? "FLOOR FORMED"
				: menu.isFloorUnlocked(floor) ? "FORMS ON ENTRY" : "UNFORMED";
		graphics.fill(x + 6, footY + 2, x + 9, footY + 5, formed ? CLEARED : TEXT_MUTED);
		graphics.drawString(this.font, generation, x + 12, footY,
				formed ? CLEARED : TEXT_MUTED, false);
	}

	/** Boss floors describe their champion; ordinary floors report the garrison. */
	private int renderGarrison(GuiGraphics graphics, int floor, int x, int y, int width) {
		int cursor = y;
		switch (floor) {
			case 1 -> {
				cursor = drawStat(graphics, "CHAMPION", "CERBERUS", x, cursor, width, GOLD);
				cursor = drawStat(graphics, "CLASS", "BEAST", x, cursor, width, TEXT_SUB);
			}
			case 20 -> {
				cursor = drawStat(graphics, "CHAMPION", "BARAN", x, cursor, width, GOLD);
				cursor = drawStat(graphics, "CONSORT", "KAISELIN", x, cursor, width, GOLD);
			}
			default -> {
				cursor = drawStat(graphics, "DEFENDERS",
						String.valueOf(DkcFloorRegistry.requiredKills(floor)), x, cursor, width,
						TEXT_MAIN);
				cursor = drawStat(graphics, "ENGAGED",
						String.valueOf(DkcFloorRegistry.activeEnemyCap(floor)), x, cursor, width,
						TEXT_SUB);
				int knights = Math.round(DkcFloorRegistry.knightShare(floor) * 100.0F);
				cursor = drawStat(graphics, "KNIGHTS", knights + "%", x, cursor, width,
						knights > 0 ? EMBER : TEXT_MUTED);
			}
		}
		if (DkcFloorRegistry.isBossFloor(floor) && floor != 1 && floor != 20)
			cursor = drawStat(graphics, "GUARDIAN", "VULCAN", x, cursor, width, GOLD);
		return cursor;
	}

	/**
	 * Each floor gets a deterministic rune inside a rotating ward, so the dossier
	 * has twenty distinct crests without any texture work.
	 */
	private void renderFloorSigil(GuiGraphics graphics, int centerX, int centerY, int floor) {
		boolean unlocked = menu.isFloorUnlocked(floor);
		int ring = floor == 15 ? VIOLET : isLandmark(floor) ? GOLD : unlocked ? CRIMSON : LOCKED_BORDER;
		int glyph = unlocked ? (floor == 15 ? VIOLET : CRIMSON_HOT) : 0xFF6B565E;

		drawDiamond(graphics, centerX, centerY, 15, (0x55000000) | (ring & 0xFFFFFF));
		drawDiamond(graphics, centerX, centerY, 13, ring);
		double spin = Util.getMillis() * 0.0006;
		for (int tick = 0; tick < 8; tick++) {
			double angle = spin + tick * Math.PI / 4.0;
			int tx = centerX + (int) Math.round(Math.cos(angle) * 9.0);
			int ty = centerY + (int) Math.round(Math.sin(angle) * 9.0);
			graphics.fill(tx, ty, tx + 1, ty + 1, unlocked ? ring : 0xFF453840);
		}
		if (floor == DkcFloorRegistry.LAST_FLOOR) {
			// Crown for the Tempest Throne.
			graphics.fill(centerX - 5, centerY + 2, centerX + 6, centerY + 4, glyph);
			graphics.fill(centerX - 5, centerY - 3, centerX - 4, centerY + 2, glyph);
			graphics.fill(centerX - 1, centerY - 5, centerX, centerY + 2, glyph);
			graphics.fill(centerX + 4, centerY - 3, centerX + 5, centerY + 2, glyph);
		} else {
			drawRune(graphics, centerX, centerY, 4, floor, glyph);
		}
	}

	private int drawSection(GuiGraphics graphics, String label, int x, int y, int width) {
		graphics.fill(x, y + 1, x + 2, y + 7, CRIMSON);
		int end = drawTracked(graphics, label, x + 5, y, EMBER, 1);
		for (int dot = end + 4; dot < x + width; dot += 3)
			graphics.fill(dot, y + 3, dot + 1, y + 4, 0x552C0A14);
		return y + 11;
	}

	/** Label/value row joined by a dotted leader, as on a printed ledger. */
	private int drawStat(GuiGraphics graphics, String label, String value, int x, int y,
			int width, int valueColor) {
		graphics.drawString(this.font, label, x, y, TEXT_MUTED, false);
		int valueWidth = this.font.width(value);
		int leaderStart = x + this.font.width(label) + 3;
		int leaderEnd = x + width - valueWidth - 3;
		for (int dot = leaderStart; dot < leaderEnd; dot += 3)
			graphics.fill(dot, y + 6, dot + 1, y + 7, 0x44A5646F);
		graphics.drawString(this.font, value, x + width - valueWidth, y, valueColor, false);
		return y + 10;
	}

	private int drawWrapped(GuiGraphics graphics, String text, int x, int y,
			int width, int color, int maxLines) {
		List<FormattedCharSequence> lines = this.font.split(Component.literal(text), width);
		int count = Math.min(maxLines, lines.size());
		for (int index = 0; index < count; index++) {
			graphics.drawString(this.font, lines.get(index), x, y, color, false);
			y += 10;
		}
		if (lines.size() > maxLines)
			graphics.drawString(this.font, "...", x, y - 10, color, false);
		return y;
	}

	// ── background ─────────────────────────────────────────────────────────────

	/**
	 * The tower backdrop is a full-panel procedural shader, and at GUI scale 3-4
	 * that panel covers close to a megapixel. Players who have already told the
	 * game they want cheap rendering get the far lighter Java fallback instead.
	 */
	private static boolean prefersCheapBackground() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft != null && minecraft.options != null
				&& minecraft.options.graphicsMode().get() == GraphicsStatus.FAST;
	}

	@Override
	protected ShaderInstance backgroundShader() {
		return prefersCheapBackground() ? null : DkcTowerBackgroundRenderTypes.get();
	}

	@Override
	protected void configureBackgroundShader(ShaderInstance shader, float localX, float localY) {
		if (!mouseSampled) {
			lastMouseX = localX;
			lastMouseY = localY;
			mouseSampled = true;
		}
		float rawVelocityX = (localX - lastMouseX) * 11.0F;
		float rawVelocityY = (localY - lastMouseY) * 11.0F;
		mouseVelocityX = mouseVelocityX * 0.72F + rawVelocityX * 0.28F;
		mouseVelocityY = mouseVelocityY * 0.72F + rawVelocityY * 0.28F;
		lastMouseX = localX;
		lastMouseY = localY;

		float target = (clampFloor(selectedFloor) - DkcFloorRegistry.FIRST_FLOOR)
				/ (float) (DkcFloorRegistry.LAST_FLOOR - DkcFloorRegistry.FIRST_FLOOR);
		focusRatio = focusRatio < 0.0F ? target : focusRatio + (target - focusRatio) * 0.11F;

		shader.safeGetUniform("MousePos").set(localX, localY);
		shader.safeGetUniform("MouseVelocity").set(mouseVelocityX, mouseVelocityY);
		shader.safeGetUniform("ScrollOffset").set(scroll);
		shader.safeGetUniform("UnlockedRatio")
				.set(menu.highestUnlockedFloor() / (float) DkcFloorRegistry.LAST_FLOOR);
		shader.safeGetUniform("FocusRatio").set(focusRatio);
	}

	@Override
	protected void renderBackgroundFallback(GuiGraphics graphics, int x, int y,
			float localX, float localY) {
		graphics.fillGradient(x, y, x + pW, y + pH, 0xF02B0209, 0xF0010002);
		float time = (Util.getMillis() % 600000L) / 1000.0F;

		// Storm shelves, then rain, then embers: back to front.
		for (int band = 0; band < 7; band++) {
			int bandY = y + Math.floorMod((int) (time * (5 + band) + band * 47), pH);
			int alpha = 16 + band * 4;
			graphics.fill(x, bandY, x + pW, Math.min(y + pH, bandY + 6),
					(alpha << 24) | 0x6B0614);
		}
		for (int streak = 0; streak < 40; streak++) {
			int sx = x + Math.floorMod(streak * 53 + streak * streak, pW);
			int sy = y + Math.floorMod((int) (time * (150 + streak % 9 * 22) + streak * 71), pH);
			graphics.fill(sx, sy, sx + 1, Math.min(y + pH, sy + 7), 0x33C08894);
		}
		for (int mote = 0; mote < 46; mote++) {
			int px = x + Math.floorMod(mote * 67 + mote * mote * 3, pW);
			int speed = 8 + mote % 17;
			int py = y + pH - Math.floorMod((int) (time * speed + mote * 41), pH);
			int color = mote % 7 == 0 ? 0xCCFF9A40 : 0x99FF293E;
			graphics.fill(px, py, px + (mote % 9 == 0 ? 2 : 1), py + 1, color);
		}

		int mouseX = x + Math.round(localX * pW);
		int mouseY = y + Math.round(localY * pH);
		int radius = 22;
		int[] px = new int[5];
		int[] py = new int[5];
		for (int point = 0; point < 5; point++) {
			double angle = -Math.PI / 2.0 + point * Math.PI * 2.0 / 5.0 + time * 0.25;
			px[point] = mouseX + (int) Math.round(Math.cos(angle) * radius);
			py[point] = mouseY + (int) Math.round(Math.sin(angle) * radius);
		}
		for (int point = 0; point < 5; point++)
			drawLine(graphics, px[point], py[point], px[(point + 2) % 5], py[(point + 2) % 5],
					0x88FF354D);
		drawDiamond(graphics, mouseX, mouseY, 29, 0x66FF6A4D);
	}

	@Override
	protected int revealAccent() {
		return CRIMSON_HOT;
	}

	@Override
	protected int revealAccentSoft() {
		return CRIMSON_SOFT;
	}

	// ── input ──────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isOpen() || button != 0)
			return super.mouseClicked(mouseX, mouseY, button);
		int logicalX = (int) Math.round(logicalMouseX(mouseX));
		int logicalY = (int) Math.round(logicalMouseY(mouseY));
		int viewLeft = panelLeft() + VIEW_X;
		int towerTop = panelTop() + TOWER_TOP;
		if (!inside(logicalX, logicalY, viewLeft, towerTop, VIEW_W, TOWER_H))
			return super.mouseClicked(mouseX, mouseY, button);

		if (logicalX >= viewLeft + VIEW_W - 11) {
			draggingScrollbar = true;
			setScrollFromScrollbar(logicalY, towerTop);
			return true;
		}
		int floor = floorAt(logicalX, logicalY, viewLeft, towerTop);
		if (floor > 0) {
			select(floor);
			return true;
		}
		draggingTower = true;
		return true;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button,
			double dragX, double dragY) {
		if (button == 0 && (draggingTower || draggingScrollbar)) {
			if (draggingScrollbar) {
				int logicalY = (int) Math.round(logicalMouseY(mouseY));
				setScrollFromScrollbar(logicalY, panelTop() + TOWER_TOP);
			} else {
				float scale = responsiveTransform().scale();
				targetScroll = clampScroll(targetScroll - (float) (dragY / scale));
			}
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = draggingTower || draggingScrollbar;
		draggingTower = false;
		draggingScrollbar = false;
		return handled || super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
		int logicalX = (int) Math.round(logicalMouseX(mouseX));
		int logicalY = (int) Math.round(logicalMouseY(mouseY));
		int viewLeft = panelLeft() + VIEW_X;
		int viewTop = panelTop() + VIEW_Y;
		if (inside(logicalX, logicalY, viewLeft, viewTop, VIEW_W, VIEW_H)) {
			targetScroll = clampScroll(targetScroll - (float) deltaY * FLOOR_STEP * 1.25F);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (isOpen()) {
			if (key == 265 || key == 87) { // Up / W
				selectAndFocus(selectedFloor + 1);
				return true;
			}
			if (key == 264 || key == 83) { // Down / S
				selectAndFocus(selectedFloor - 1);
				return true;
			}
			if (key == 266) { // Page Up
				selectAndFocus(selectedFloor + 5);
				return true;
			}
			if (key == 267) { // Page Down
				selectAndFocus(selectedFloor - 5);
				return true;
			}
			if (key == 268) { // Home = summit
				selectAndFocus(DkcFloorRegistry.LAST_FLOOR);
				return true;
			}
			if (key == 269) { // End = entrance
				selectAndFocus(DkcFloorRegistry.FIRST_FLOOR);
				return true;
			}
			if (key == 257 || key == 335) { // Enter / keypad Enter
				enterSelectedFloor();
				return true;
			}
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		if (hoveredFloor <= 0)
			return;
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("FLOOR " + hoveredFloor + " - "
				+ DkcFloorRegistry.name(hoveredFloor)));
		lines.add(Component.literal(longFloorState(hoveredFloor)));
		if (!DkcFloorRegistry.isBossFloor(hoveredFloor))
			lines.add(Component.literal(DkcFloorRegistry.requiredKills(hoveredFloor)
					+ " defenders garrisoned"));
		lines.add(Component.literal("Click to inspect"));
		drawDkcTooltip(graphics, lines, mouseX, mouseY);
	}

	// ── actions ────────────────────────────────────────────────────────────────

	@Override
	protected void onBeforeCloseAnimationFinished() {
		if (pendingExit) {
			pendingExit = false;
			SololevelingMod.PACKET_HANDLER.sendToServer(
					PathButtonMessage.exitCastle(menu.x, menu.y, menu.z));
		} else if (pendingFloor > 0) {
			int floor = pendingFloor;
			pendingFloor = 0;
			SololevelingMod.PACKET_HANDLER.sendToServer(
					PathButtonMessage.enterFloor(floor, menu.x, menu.y, menu.z));
		}
	}

	@Override
	protected void onCloseAnimationFinished() {
		if (returningToQuests) {
			returningToQuests = false;
			if (this.minecraft != null && this.minecraft.player != null
					&& this.minecraft.getConnection() != null)
				this.minecraft.setScreen(new SystemQuestsScreen());
		}
	}

	private void returnToQuestHub() {
		returningToQuests = true;
		beginClose();
	}

	private void focusProgressFloor() {
		int floor = menu.insideDkc() && menu.currentFloor() > 0
				? menu.currentFloor() : menu.highestUnlockedFloor();
		selectAndFocus(floor <= 0 ? 1 : floor);
	}

	private void select(int floor) {
		int clamped = clampFloor(floor);
		if (clamped != selectedFloor)
			selectionChangedAt = Util.getMillis();
		selectedFloor = clamped;
	}

	private void selectAndFocus(int floor) {
		select(floor);
		targetScroll = focusOffset(selectedFloor);
	}

	private void enterSelectedFloor() {
		if (!canEnterSelected())
			return;
		travelRequested = true;
		pendingFloor = selectedFloor;
		beginClose();
	}

	private void exitCastle() {
		if (!menu.insideDkc() || travelRequested)
			return;
		travelRequested = true;
		pendingExit = true;
		beginClose();
	}

	private boolean canEnterSelected() {
		if (travelRequested || menu.conquered() || !menu.isFloorUnlocked(selectedFloor))
			return false;
		if (menu.insideDkc())
			return menu.currentFloor() != selectedFloor;
		return menu.entity != null && menu.entity.level().dimension().equals(Level.OVERWORLD);
	}

	private Component enterButtonText() {
		if (travelRequested)
			return Component.literal("OPENING PATH...");
		if (menu.conquered())
			return Component.literal("CONQUERED");
		if (!menu.isFloorUnlocked(selectedFloor))
			return Component.literal("FLOOR SEALED");
		if (menu.insideDkc() && menu.currentFloor() == selectedFloor)
			return Component.literal("YOU ARE HERE");
		if (!menu.insideDkc() && (menu.entity == null
				|| !menu.entity.level().dimension().equals(Level.OVERWORLD)))
			return Component.literal("OVERWORLD ONLY");
		return Component.literal("ENTER FLOOR");
	}

	// ── floor state ────────────────────────────────────────────────────────────

	/** Plaque-width labels; the dossier and tooltip carry the long form. */
	private String shortFloorState(int floor) {
		if (menu.insideDkc() && menu.currentFloor() == floor)
			return "HERE";
		if (menu.isFloorCleared(floor))
			return "CLEARED";
		if (menu.isFloorUnlocked(floor))
			return floor == menu.highestUnlockedFloor() ? "CURRENT" : "OPEN";
		if (needsEntryPermit(floor))
			return "PERMIT";
		return "SEALED";
	}

	private String longFloorState(int floor) {
		if (menu.insideDkc() && menu.currentFloor() == floor)
			return "YOU ARE HERE";
		if (menu.isFloorCleared(floor))
			return "CLEARED";
		if (menu.isFloorUnlocked(floor))
			return "AVAILABLE";
		if (needsEntryPermit(floor))
			return "ENTRY PERMIT REQUIRED";
		return floor <= 1 ? "FIRST SEAL REQUIRED" : "CLEAR FLOOR " + (floor - 1);
	}

	private int floorStateColor(int floor) {
		if (menu.insideDkc() && menu.currentFloor() == floor)
			return GOLD;
		if (menu.isFloorCleared(floor))
			return CLEARED;
		if (menu.isFloorUnlocked(floor))
			return CRIMSON_HOT;
		return needsEntryPermit(floor) ? GOLD : TEXT_MUTED;
	}

	private boolean needsEntryPermit(int floor) {
		return floor > 1 && !menu.isFloorUnlocked(floor)
				&& menu.clearedFloors() >= floor - 1
				&& !menu.isTransitionArmed(floor - 1);
	}

	private String floorObjective(int floor) {
		return switch (floor) {
			case 1 -> "Defeat Cerberus at the Ashen Threshold.";
			case 10 -> "Purge 32 defenders, then defeat Vulcan.";
			case 15 -> menu.radiruPact()
					? "House Radiru stands as your sanctuary."
					: menu.radiruSlaughtered()
							? "House Radiru has fallen by your hand."
							: "Defeat 30 defenders and decide House Radiru's fate.";
			case 20 -> "Defeat Baran and Kaiselin at the Tempest Throne.";
			default -> "Defeat " + DkcFloorRegistry.requiredKills(floor)
					+ " castle defenders.";
		};
	}

	private String floorReward(int floor) {
		return switch (floor) {
			case 1 -> "Entry Permit / World Tree Fragment / Full Recovery";
			case 10 -> "Entry Permit / Spring Water / Orb of Avarice";
			case 15 -> menu.radiruPact()
					? "Radiru sanctuary / 1,500 XP"
					: menu.radiruSlaughtered()
							? "Cold Blood / 4,000 XP"
							: "Entry Permit / outcome reward";
			case 20 -> "Demon King weapons / Purified Blood / Kaisel";
			default -> "Entry Permit / " + (floor * 100) + " XP";
		};
	}

	private String radiruBadge() {
		if (menu.radiruPact())
			return "PACT";
		if (menu.radiruSlaughtered())
			return "FALLEN";
		return "RADIRU";
	}

	// ── geometry ───────────────────────────────────────────────────────────────

	private int floorAt(int mouseX, int mouseY, int viewLeft, int towerTop) {
		if (!inside(mouseX, mouseY, viewLeft, towerTop, VIEW_W - SCROLLBAR_RESERVE, TOWER_H))
			return 0;
		int roundedScroll = Math.round(scroll);
		for (int floor = DkcFloorRegistry.FIRST_FLOOR;
				floor <= DkcFloorRegistry.LAST_FLOOR; floor++) {
			NodeBounds node = nodeBounds(floor, viewLeft, towerTop, roundedScroll);
			if (inside(mouseX, mouseY, node.x(), node.y(), node.width(), node.height()))
				return floor;
		}
		return 0;
	}

	private NodeBounds nodeBounds(int floor, int viewLeft, int towerTop, int roundedScroll) {
		boolean landmark = isLandmark(floor);
		int width = landmark ? LANDMARK_W : PLAQUE_W;
		int height = landmark ? LANDMARK_H : PLAQUE_H;
		int centerX = viewLeft + (VIEW_W - SCROLLBAR_RESERVE) / 2;
		int y = towerTop + floorContentY(floor) - roundedScroll - height / 2;
		return new NodeBounds(centerX - width / 2, y, width, height);
	}

	/**
	 * The shaft tapers from a broad base up to the summit, but only gently: the
	 * batter has to stay wider than a plaque at every floor or the masonry band
	 * disappears at the top. The summit crown and foundation flare carry the
	 * rest of the perspective.
	 */
	private static int shaftHalfWidth(int floor) {
		int descent = DkcFloorRegistry.LAST_FLOOR - floor;
		return 64 + (descent * 14) / (DkcFloorRegistry.LAST_FLOOR - DkcFloorRegistry.FIRST_FLOOR);
	}

	private static boolean isLandmark(int floor) {
		return floor == 1 || floor == 10 || floor == 15 || floor == 20;
	}

	private static int floorContentY(int floor) {
		return CONTENT_PAD + (DkcFloorRegistry.LAST_FLOOR - floor) * FLOOR_STEP;
	}

	private float focusOffset(int floor) {
		float nodeY = floorContentY(clampFloor(floor));
		return clampScroll(nodeY - TOWER_H * 0.58F);
	}

	private void advanceAnimation() {
		targetScroll = clampScroll(targetScroll);
		scroll += (targetScroll - scroll) * 0.24F;
		if (Math.abs(targetScroll - scroll) < 0.08F)
			scroll = targetScroll;
		mouseVelocityX *= 0.94F;
		mouseVelocityY *= 0.94F;
		for (int floor = DkcFloorRegistry.FIRST_FLOOR;
				floor <= DkcFloorRegistry.LAST_FLOOR; floor++) {
			float target = floor == hoveredFloor ? 1.0F : 0.0F;
			floorGlow[floor] += (target - floorGlow[floor]) * 0.22F;
		}
	}

	private void setScrollFromScrollbar(int mouseY, int towerTop) {
		int usable = TOWER_H - 8;
		int thumbHeight = Math.max(24, Math.round(usable * (TOWER_H / (float) CONTENT_HEIGHT)));
		int travel = Math.max(1, usable - thumbHeight);
		float fraction = (mouseY - towerTop - 4 - thumbHeight / 2.0F) / travel;
		targetScroll = clampScroll(fraction * maxScroll());
		scroll = targetScroll;
	}

	private static float maxScroll() {
		return Math.max(0, CONTENT_HEIGHT - TOWER_H);
	}

	private static float clampScroll(float value) {
		return Math.max(0.0F, Math.min(maxScroll(), value));
	}

	private static int clampFloor(int floor) {
		return Math.max(DkcFloorRegistry.FIRST_FLOOR,
				Math.min(DkcFloorRegistry.LAST_FLOOR, floor));
	}

	private int panelLeft() {
		return this.leftPos + pRelX;
	}

	private int panelTop() {
		return this.topPos + pRelY;
	}

	private static float pulse01() {
		return 0.5F + 0.5F * (float) Math.sin(Util.getMillis() * 0.007);
	}

	private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}

	private static float hash01(int x, int y) {
		int hash = x * 374761393 + y * 668265263;
		hash = (hash ^ (hash >>> 13)) * 1274126177;
		return ((hash ^ (hash >>> 16)) & 0xFFFF) / 65535.0F;
	}

	// ── primitives ─────────────────────────────────────────────────────────────

	/** Letterspaced caps; the tracking is what separates a title from body text. */
	private int drawTracked(GuiGraphics graphics, String text, int x, int y, int color,
			int tracking) {
		int cursor = x;
		for (int index = 0; index < text.length(); index++) {
			String glyph = String.valueOf(text.charAt(index));
			graphics.drawString(this.font, glyph, cursor, y, color, false);
			cursor += this.font.width(glyph) + tracking;
		}
		return cursor - tracking;
	}

	private int trackedWidth(String text, int tracking) {
		if (text.isEmpty())
			return 0;
		int width = -tracking;
		for (int index = 0; index < text.length(); index++)
			width += this.font.width(String.valueOf(text.charAt(index))) + tracking;
		return width;
	}

	/** Integer-scaled text keeps the pixel grid crisp while adding hierarchy. */
	private void drawScaled(GuiGraphics graphics, String text, int x, int y, float scale,
			int color) {
		graphics.pose().pushPose();
		graphics.pose().translate(x, y, 0.0F);
		graphics.pose().scale(scale, scale, 1.0F);
		graphics.drawString(this.font, text, 0, 0, color, false);
		graphics.pose().popPose();
	}

	private static void fillChamfer(GuiGraphics graphics, int x, int y, int width, int height,
			int cut, int color) {
		graphics.fill(x, y + cut, x + width, y + height - cut, color);
		for (int step = 0; step < cut; step++) {
			graphics.fill(x + cut - step, y + step, x + width - cut + step, y + step + 1, color);
			graphics.fill(x + cut - step, y + height - step - 1, x + width - cut + step,
					y + height - step, color);
		}
	}

	private static void outlineChamfer(GuiGraphics graphics, int x, int y, int width, int height,
			int cut, int color) {
		graphics.fill(x + cut, y, x + width - cut, y + 1, color);
		graphics.fill(x + cut, y + height - 1, x + width - cut, y + height, color);
		graphics.fill(x, y + cut, x + 1, y + height - cut, color);
		graphics.fill(x + width - 1, y + cut, x + width, y + height - cut, color);
		for (int step = 0; step < cut; step++) {
			graphics.fill(x + cut - 1 - step, y + step, x + cut - step, y + step + 1, color);
			graphics.fill(x + width - cut + step, y + step, x + width - cut + step + 1,
					y + step + 1, color);
			graphics.fill(x + cut - 1 - step, y + height - step - 1, x + cut - step,
					y + height - step, color);
			graphics.fill(x + width - cut + step, y + height - step - 1,
					x + width - cut + step + 1, y + height - step, color);
		}
	}

	private static void drawRivet(GuiGraphics graphics, int x, int y, int tint) {
		graphics.fill(x, y, x + 2, y + 2, 0xCC0A0508);
		graphics.fill(x, y, x + 1, y + 1, (0x88000000) | (tint & 0xFFFFFF));
	}

	private static void drawOrnament(GuiGraphics graphics, int centerX, int centerY, int color) {
		graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, color);
		graphics.fill(centerX - 2, centerY, centerX + 3, centerY + 1, color);
		graphics.fill(centerX, centerY - 2, centerX + 1, centerY + 3, color);
	}

	/** Four chords across a 3x3 lattice, seeded by floor, give each floor a mark. */
	private static void drawRune(GuiGraphics graphics, int centerX, int centerY, int size,
			int seed, int color) {
		int[] pointX = new int[9];
		int[] pointY = new int[9];
		for (int index = 0; index < 9; index++) {
			pointX[index] = centerX + (index % 3 - 1) * size;
			pointY[index] = centerY + (index / 3 - 1) * size;
		}
		for (int chord = 0; chord < 4; chord++) {
			int from = (int) (hash01(seed, chord * 2) * 8.999F);
			int to = (int) (hash01(seed, chord * 2 + 1) * 8.999F);
			if (from == to)
				to = (to + 3) % 9;
			drawLine(graphics, pointX[from], pointY[from], pointX[to], pointY[to], color);
		}
	}

	private static void drawPanelFrame(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.fill(x - 2, y - 2, x + width + 2, y - 1, 0x33FF354D);
		graphics.fill(x - 2, y + height + 1, x + width + 2, y + height + 2, 0x33FF354D);
		graphics.fill(x - 2, y - 1, x - 1, y + height + 1, 0x33FF354D);
		graphics.fill(x + width + 1, y - 1, x + width + 2, y + height + 1, 0x33FF354D);
		graphics.fill(x - 1, y - 1, x + width + 1, y, CRIMSON_SOFT);
		graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, CRIMSON_SOFT);
		graphics.fill(x - 1, y, x, y + height, CRIMSON_SOFT);
		graphics.fill(x + width, y, x + width + 1, y + height, CRIMSON_SOFT);
		drawOutline(graphics, x, y, width, height, CRIMSON_DIM);
		drawCornerBrackets(graphics, x, y, width, height, CRIMSON);
	}

	private static void drawOutline(GuiGraphics graphics, int x, int y,
			int width, int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	private static void drawCornerBrackets(GuiGraphics graphics, int x, int y,
			int width, int height, int color) {
		int length = 12;
		int stub = 5;
		graphics.fill(x - 1, y - 1, x + length, y + 1, color);
		graphics.fill(x - 1, y - 1, x + 1, y + length, color);
		graphics.fill(x + width - length, y - 1, x + width + 1, y + 1, color);
		graphics.fill(x + width - 1, y - 1, x + width + 1, y + length, color);
		graphics.fill(x - 1, y + height - 1, x + length, y + height + 1, color);
		graphics.fill(x - 1, y + height - length, x + 1, y + height + 1, color);
		graphics.fill(x + width - length, y + height - 1, x + width + 1,
				y + height + 1, color);
		graphics.fill(x + width - 1, y + height - length, x + width + 1,
				y + height + 1, color);
		// Inner stubs echo the bracket a few pixels in, like a forged corner plate.
		int inner = 0x66000000 | (color & 0xFFFFFF);
		graphics.fill(x + 2, y + 2, x + 2 + stub, y + 3, inner);
		graphics.fill(x + 2, y + 2, x + 3, y + 2 + stub, inner);
		graphics.fill(x + width - 2 - stub, y + 2, x + width - 2, y + 3, inner);
		graphics.fill(x + width - 3, y + 2, x + width - 2, y + 2 + stub, inner);
		graphics.fill(x + 2, y + height - 3, x + 2 + stub, y + height - 2, inner);
		graphics.fill(x + 2, y + height - 2 - stub, x + 3, y + height - 2, inner);
		graphics.fill(x + width - 2 - stub, y + height - 3, x + width - 2, y + height - 2, inner);
		graphics.fill(x + width - 3, y + height - 2 - stub, x + width - 2, y + height - 2, inner);
	}

	private static void drawLine(GuiGraphics graphics, int x0, int y0,
			int x1, int y1, int color) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		int steps = Math.max(Math.abs(dx), Math.abs(dy));
		if (steps <= 0) {
			graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
			return;
		}
		for (int step = 0; step <= steps; step++) {
			int x = x0 + Math.round(dx * (step / (float) steps));
			int y = y0 + Math.round(dy * (step / (float) steps));
			graphics.fill(x, y, x + 1, y + 1, color);
		}
	}

	private static void drawDiamond(GuiGraphics graphics, int centerX, int centerY,
			int radius, int color) {
		drawLine(graphics, centerX, centerY - radius, centerX + radius, centerY, color);
		drawLine(graphics, centerX + radius, centerY, centerX, centerY + radius, color);
		drawLine(graphics, centerX, centerY + radius, centerX - radius, centerY, color);
		drawLine(graphics, centerX - radius, centerY, centerX, centerY - radius, color);
	}

	private void drawDkcTooltip(GuiGraphics graphics, List<Component> lines,
			int mouseX, int mouseY) {
		int padding = 6;
		int textWidth = 0;
		for (Component line : lines)
			textWidth = Math.max(textWidth, this.font.width(line));
		int width = textWidth + padding * 2 + 3;
		int height = lines.size() * 10 + padding * 2 - 1;
		int x = mouseX + 12;
		int y = mouseY - 12;
		if (x + width > this.width - 2)
			x = mouseX - width - 12;
		if (y + height > this.height - 2)
			y = this.height - height - 2;
		x = Math.max(2, x);
		y = Math.max(2, y);

		graphics.fill(x + 2, y + 3, x + width + 2, y + height + 3, 0x77000000);
		graphics.fillGradient(x, y, x + width, y + height, 0xF23A0716, VOID_INK);
		drawOutline(graphics, x, y, width, height, CRIMSON_DIM);
		drawCornerBrackets(graphics, x, y, width, height, CRIMSON);
		graphics.fill(x + 1, y + 1, x + 4, y + height - 1, CRIMSON_DIM);

		int textY = y + padding;
		for (int index = 0; index < lines.size(); index++) {
			graphics.drawString(this.font, lines.get(index), x + padding + 3, textY,
					index == 0 ? CRIMSON_HOT : index == lines.size() - 1 ? TEXT_MUTED : TEXT_MAIN,
					false);
			if (index == 0) {
				graphics.fill(x + padding + 3, textY + 10, x + width - padding, textY + 11,
						0x552C0A14);
				textY += 2;
			}
			textY += 10;
		}
	}

	/** Chamfered iron button matching the plaques, with a corroded disabled state. */
	private static final class DkcButton extends Button {
		private final Supplier<Component> label;
		private final BooleanSupplier enabled;

		private DkcButton(int x, int y, int width, int height,
				Supplier<Component> label, BooleanSupplier enabled,
				OnPress onPress) {
			super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
			this.label = label;
			this.enabled = enabled;
		}

		@Override
		public void onPress() {
			if (enabled.getAsBoolean())
				super.onPress();
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY,
				float partialTicks) {
			if (!this.visible)
				return;
			// Widgets render outside renderBg, so they need their own managed batch.
			graphics.drawManaged(() -> renderButton(graphics));
		}

		private void renderButton(GuiGraphics graphics) {
			Component currentLabel = label.get();
			setMessage(currentLabel);
			boolean usable = enabled.getAsBoolean();
			boolean hovered = usable && isHoveredOrFocused();
			int x = getX();
			int y = getY();
			int cut = 3;

			int top = !usable ? 0xCC17111A : hovered ? 0xE6721826 : 0xCC3A0D18;
			int bottom = !usable ? 0xCC0B080D : hovered ? 0xE6260710 : 0xCC160409;
			int border = !usable ? LOCKED_BORDER : hovered ? CRIMSON_HOT : CRIMSON_DIM;
			int text = !usable ? TEXT_MUTED : hovered ? 0xFFFFFFFF : TEXT_MAIN;

			fillChamfer(graphics, x + 1, y + 2, width, height, cut, 0x66000000);
			fillChamfer(graphics, x, y, width, height, cut, top);
			graphics.fillGradient(x + 1, y + cut, x + width - 1, y + height - cut,
					0x00000000, bottom);
			outlineChamfer(graphics, x, y, width, height, cut, border);
			graphics.fill(x + cut, y + 1, x + width - cut, y + 2,
					usable ? 0x44FFFFFF : 0x22FFFFFF);

			if (usable && hovered) {
				// Bracket ticks appear only on hover, so the row stays quiet at rest.
				graphics.fill(x + 2, y + height / 2 - 2, x + 4, y + height / 2 + 2, CRIMSON_HOT);
				graphics.fill(x + width - 4, y + height / 2 - 2, x + width - 2,
						y + height / 2 + 2, CRIMSON_HOT);
			} else if (!usable) {
				for (int fleck = 0; fleck < 10; fleck++) {
					int fx = x + 4 + (int) (hash01(fleck, y) * (width - 8));
					int fy = y + 3 + (int) (hash01(x, fleck) * (height - 6));
					graphics.fill(fx, fy, fx + 1, fy + 1, 0x33000000);
				}
			}

			Font font = Minecraft.getInstance().font;
			graphics.drawCenteredString(font, currentLabel, x + width / 2,
					y + (height - 8) / 2, text);
		}
	}

	private record NodeBounds(int x, int y, int width, int height) {
	}
}
