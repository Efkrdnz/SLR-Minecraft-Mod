package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemQuestsScreen;
import net.solocraft.client.renderer.shader.DkcTowerBackgroundRenderTypes;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.network.PathButtonMessage;
import net.solocraft.world.inventory.PathMenu;

import net.minecraft.Util;
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

/**
 * Scrollable, server-authored map of the Demon King's Castle.
 *
 * <p>Floor 1 lives at the bottom of the content and Floor 20 at the summit.
 * The immutable state on {@link PathMenu} is only used for presentation; every
 * travel request is validated again against live DKC saved data on the server.
 */
public class PathScreen extends SystemContainerScreen<PathMenu> {
	private static final int PANEL_W = 322;
	private static final int PANEL_H = 286;

	private static final int VIEW_X = 8;
	private static final int VIEW_Y = 31;
	private static final int VIEW_W = 190;
	private static final int VIEW_H = 220;
	private static final int DETAIL_X = 205;
	private static final int DETAIL_Y = 31;
	private static final int DETAIL_W = 109;
	private static final int DETAIL_H = 220;
	private static final int FOOTER_Y = 259;

	private static final int FLOOR_STEP = 42;
	private static final int CONTENT_PAD = 22;
	private static final int CONTENT_HEIGHT =
			CONTENT_PAD * 2 + (DkcFloorRegistry.LAST_FLOOR - 1) * FLOOR_STEP + 30;

	private static final int CRIMSON = 0xFFFF354D;
	private static final int CRIMSON_HOT = 0xFFFF6A4D;
	private static final int CRIMSON_DIM = 0xFF8C1E2D;
	private static final int CRIMSON_SOFT = 0x66FF354D;
	private static final int BLOOD_FILL = 0xB50A0206;
	private static final int ROW_FILL = 0xD018060B;
	private static final int LOCKED_FILL = 0xD00B090D;
	private static final int LOCKED_BORDER = 0xFF54414A;
	private static final int CLEARED = 0xFFB9515D;
	private static final int GOLD = 0xFFFFB34C;
	private static final int TEXT_MAIN = 0xFFFFEDF0;
	private static final int TEXT_SUB = 0xFFD5A8AF;
	private static final int TEXT_MUTED = 0xFF8E737A;

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

	private float lastMouseX = 0.5F;
	private float lastMouseY = 0.5F;
	private float mouseVelocityX;
	private float mouseVelocityY;
	private boolean mouseSampled;

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
		super.init();

		int left = panelLeft();
		int top = panelTop();
		int initialFloor = menu.highestUnlockedFloor() > 0
				? menu.highestUnlockedFloor() : menu.currentFloor();
		this.selectedFloor = clampFloor(initialFloor <= 0 ? 1 : initialFloor);
		this.targetScroll = focusOffset(selectedFloor);
		this.scroll = targetScroll;

		this.backButton = addRenderableWidget(new DkcButton(
				left + 4, top + 3, 43, 12, () -> Component.literal("< Back"),
				() -> true, button -> returnToQuestHub()));
		this.focusButton = addRenderableWidget(new DkcButton(
				left + VIEW_X, top + FOOTER_Y, 92, 18,
				() -> Component.literal(menu.insideDkc() ? "FIND CURRENT" : "FIND HIGHEST"),
				() -> true, button -> focusProgressFloor()));
		if (menu.insideDkc()) {
			this.exitButton = addRenderableWidget(new DkcButton(
					left + VIEW_X + 98, top + FOOTER_Y, 92, 18,
					() -> Component.literal("EXIT CASTLE"),
					() -> !travelRequested, button -> exitCastle()));
		}
		this.enterButton = addRenderableWidget(new DkcButton(
				left + DETAIL_X + 5, top + FOOTER_Y, DETAIL_W - 10, 18,
				this::enterButtonText, this::canEnterSelected,
				button -> enterSelectedFloor()));
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		advanceScroll();
		int left = panelLeft();
		int top = panelTop();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0x4D050003);
		drawPanelFrame(graphics, left, top, PANEL_W, PANEL_H);
		graphics.fill(left, top, left + PANEL_W, top + 19, 0xB52B030B);
		graphics.fill(left, top + 18, left + PANEL_W, top + 19, CRIMSON);
		drawCornerBrackets(graphics, left, top, PANEL_W, PANEL_H, CRIMSON);

		String title = "DEMON KING'S CASTLE";
		graphics.drawString(this.font, title,
				left + (PANEL_W - this.font.width(title)) / 2, top + 6, CRIMSON_HOT, false);

		int viewLeft = left + VIEW_X;
		int viewTop = top + VIEW_Y;
		graphics.fill(viewLeft, viewTop, viewLeft + VIEW_W, viewTop + VIEW_H, 0x7A030104);
		drawOutline(graphics, viewLeft, viewTop, VIEW_W, VIEW_H, CRIMSON_DIM);
		graphics.drawString(this.font, "TOWER ASCENT", viewLeft + 5, viewTop + 4, TEXT_SUB, false);
		String scrollHint = "SCROLL / DRAG";
		graphics.drawString(this.font, scrollHint,
				viewLeft + VIEW_W - this.font.width(scrollHint) - 8, viewTop + 4,
				TEXT_MUTED, false);

		ResponsiveGuiScale.enableScissor(graphics, responsiveTransform(),
				viewLeft + 1, viewTop + 15, viewLeft + VIEW_W - 1, viewTop + VIEW_H - 1);
		renderTower(graphics, mouseX, mouseY, viewLeft, viewTop + 15, VIEW_W, VIEW_H - 16);
		graphics.disableScissor();

		int detailLeft = left + DETAIL_X;
		int detailTop = top + DETAIL_Y;
		graphics.fill(detailLeft, detailTop, detailLeft + DETAIL_W, detailTop + DETAIL_H, BLOOD_FILL);
		drawOutline(graphics, detailLeft, detailTop, DETAIL_W, DETAIL_H, CRIMSON_DIM);
		renderFloorDetails(graphics, detailLeft, detailTop);

		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		// All labels use absolute panel coordinates in renderBg.
	}

	private void renderTower(GuiGraphics graphics, int mouseX, int mouseY,
			int viewLeft, int viewTop, int viewWidth, int viewHeight) {
		int centerX = viewLeft + viewWidth / 2 - 2;
		this.hoveredFloor = floorAt(mouseX, mouseY, viewLeft, viewTop, viewWidth, viewHeight);
		int roundedScroll = Math.round(scroll);

		// A stepped obsidian body widens toward Floor 1.
		for (int floor = DkcFloorRegistry.LAST_FLOOR; floor >= DkcFloorRegistry.FIRST_FLOOR; floor--) {
			int y = viewTop + floorContentY(floor) - roundedScroll;
			int nextY = y + FLOOR_STEP;
			int descent = DkcFloorRegistry.LAST_FLOOR - floor;
			int halfWidth = 13 + descent * 2;
			int bodyTop = y;
			int bodyBottom = floor == 1 ? y + 25 : nextY + 1;
			graphics.fill(centerX - halfWidth, bodyTop, centerX + halfWidth, bodyBottom, 0xE3030104);
			graphics.fill(centerX - halfWidth, bodyTop, centerX - halfWidth + 1, bodyBottom,
					floor <= menu.highestUnlockedFloor() ? 0xAA8A1524 : 0x77452C34);
			graphics.fill(centerX + halfWidth - 1, bodyTop, centerX + halfWidth, bodyBottom,
					floor <= menu.highestUnlockedFloor() ? 0xAA8A1524 : 0x77452C34);
			if (floor % 2 == 0) {
				graphics.fill(centerX - halfWidth - 3, y + 17, centerX + halfWidth + 3, y + 19,
						floor <= menu.highestUnlockedFloor() ? 0x99271318 : 0x88402E35);
			}
		}

		// The central path and landing arms remain visible even when nodes are dim.
		for (int floor = DkcFloorRegistry.LAST_FLOOR; floor >= DkcFloorRegistry.FIRST_FLOOR; floor--) {
			int y = viewTop + floorContentY(floor) - roundedScroll;
			NodeBounds node = nodeBounds(floor, viewLeft, viewTop, roundedScroll);
			boolean energized = menu.isFloorUnlocked(floor);
			boolean highlighted = floor == hoveredFloor || floor == selectedFloor;
			int pathColor = highlighted ? CRIMSON_HOT : energized ? CRIMSON_DIM : 0xFF3F3036;
			graphics.fill(centerX - 1, y, centerX + 1,
					y + (floor == 1 ? 2 : FLOOR_STEP + 1), pathColor);
			int nodeCenter = node.x() + node.width() / 2;
			int x0 = Math.min(centerX, nodeCenter);
			int x1 = Math.max(centerX, nodeCenter);
			graphics.fill(x0, y - 1, x1 + 1, y + 1, pathColor);
			if (highlighted) {
				int pulseX = x0 + (int) ((x1 - x0) * pulse01());
				graphics.fill(pulseX - 1, y - 2, pulseX + 2, y + 2, GOLD);
			}
		}

		for (int floor = DkcFloorRegistry.LAST_FLOOR; floor >= DkcFloorRegistry.FIRST_FLOOR; floor--)
			renderFloorNode(graphics, floor, nodeBounds(floor, viewLeft, viewTop, roundedScroll));

		renderScrollbar(graphics, viewLeft, viewTop, viewWidth, viewHeight);
	}

	private void renderFloorNode(GuiGraphics graphics, int floor, NodeBounds node) {
		boolean unlocked = menu.isFloorUnlocked(floor);
		boolean cleared = menu.isFloorCleared(floor);
		boolean here = menu.insideDkc() && menu.currentFloor() == floor;
		boolean selected = selectedFloor == floor;
		boolean hovered = hoveredFloor == floor;
		boolean landmark = isLandmark(floor);

		int fill = cleared ? 0xD011080C : unlocked ? ROW_FILL : LOCKED_FILL;
		int border = here ? GOLD : cleared ? CLEARED : unlocked ? CRIMSON : LOCKED_BORDER;
		if (selected || hovered)
			border = selected ? 0xFFFFE0D4 : CRIMSON_HOT;
		if (here)
			fill = 0xD03B1009;

		if (selected) {
			drawOutline(graphics, node.x() - 2, node.y() - 2,
					node.width() + 4, node.height() + 4, CRIMSON_SOFT);
		}
		graphics.fill(node.x(), node.y(), node.x() + node.width(), node.y() + node.height(), fill);
		drawOutline(graphics, node.x(), node.y(), node.width(), node.height(), border);
		graphics.fill(node.x(), node.y(), node.x() + 3, node.y() + node.height(), border);

		String floorText = "FLOOR " + floor;
		graphics.drawString(this.font, floorText, node.x() + 6, node.y() + 4,
				unlocked ? TEXT_MAIN : TEXT_MUTED, false);
		String state = shortFloorState(floor);
		graphics.drawString(this.font, state, node.x() + 6, node.y() + node.height() - 10,
				floorStateColor(floor), false);

		if (landmark) {
			String badge = floor == 15 ? radiruBadge() : "BOSS";
			int badgeWidth = this.font.width(badge);
			graphics.drawString(this.font, badge,
					node.x() + node.width() - badgeWidth - 5, node.y() + 4,
					floor == 15 ? 0xFFD98CFF : GOLD, false);
		}
		if (here) {
			int pulseAlpha = 110 + (int) (100.0F * pulse01());
			graphics.fill(node.x() - 1, node.y() - 1, node.x() + node.width() + 1, node.y(),
					(pulseAlpha << 24) | 0xFFB34C);
		}
	}

	private void renderScrollbar(GuiGraphics graphics, int viewLeft, int viewTop,
			int viewWidth, int viewHeight) {
		int trackX = viewLeft + viewWidth - 5;
		graphics.fill(trackX, viewTop + 2, trackX + 2, viewTop + viewHeight - 2, 0xAA24090F);
		float maximum = maxScroll();
		int usable = viewHeight - 8;
		int thumbHeight = Math.max(22, Math.round(usable * (viewHeight / (float) CONTENT_HEIGHT)));
		int travel = Math.max(1, usable - thumbHeight);
		int thumbY = viewTop + 4 + (maximum <= 0.0F ? 0
				: Math.round((scroll / maximum) * travel));
		graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, CRIMSON_DIM);
		graphics.fill(trackX, thumbY + 1, trackX + 2, thumbY + thumbHeight - 1, CRIMSON_HOT);
	}

	private void renderFloorDetails(GuiGraphics graphics, int x, int y) {
		int floor = clampFloor(selectedFloor);
		String heading = "FLOOR " + floor;
		graphics.drawString(this.font, heading, x + 6, y + 7,
				isLandmark(floor) ? GOLD : CRIMSON_HOT, false);
		graphics.fill(x + 5, y + 19, x + DETAIL_W - 5, y + 20, CRIMSON_DIM);

		int cursor = y + 25;
		cursor = drawWrapped(graphics, DkcFloorRegistry.name(floor), x + 6, cursor,
				DETAIL_W - 12, TEXT_MAIN, 2);
		cursor += 3;
		graphics.drawString(this.font, longFloorState(floor), x + 6, cursor,
				floorStateColor(floor), false);
		cursor += 14;
		if (floor > 1 && menu.isTransitionArmed(floor - 1)) {
			graphics.drawString(this.font, "PERMIT ACCEPTED", x + 6, cursor, CLEARED, false);
			cursor += 12;
		}

		graphics.drawString(this.font, "OBJECTIVE", x + 6, cursor, CRIMSON_HOT, false);
		cursor += 11;
		cursor = drawWrapped(graphics, floorObjective(floor), x + 6, cursor,
				DETAIL_W - 12, TEXT_SUB, 4);
		cursor += 5;

		graphics.drawString(this.font, "REWARD", x + 6, cursor, CRIMSON_HOT, false);
		cursor += 11;
		cursor = drawWrapped(graphics, floorReward(floor), x + 6, cursor,
				DETAIL_W - 12, TEXT_SUB, 4);
		cursor += 5;

		String generation = menu.isFloorGenerated(floor) ? "FLOOR FORMED"
				: menu.isFloorUnlocked(floor) ? "FORMS ON ENTRY" : "UNFORMED";
		graphics.drawString(this.font, generation, x + 6,
				Math.min(y + DETAIL_H - 13, cursor),
				menu.isFloorGenerated(floor) ? CLEARED : TEXT_MUTED, false);
	}

	private int drawWrapped(GuiGraphics graphics, String text, int x, int y,
			int width, int color, int maxLines) {
		List<FormattedCharSequence> lines = this.font.split(Component.literal(text), width);
		int count = Math.min(maxLines, lines.size());
		for (int index = 0; index < count; index++) {
			FormattedCharSequence line = lines.get(index);
			graphics.drawString(this.font, line, x, y, color, false);
			y += 10;
		}
		if (lines.size() > maxLines)
			graphics.drawString(this.font, "...", x, y - 10, color, false);
		return y;
	}

	@Override
	protected ShaderInstance backgroundShader() {
		return DkcTowerBackgroundRenderTypes.get();
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

		shader.safeGetUniform("MousePos").set(localX, localY);
		shader.safeGetUniform("MouseVelocity").set(mouseVelocityX, mouseVelocityY);
		shader.safeGetUniform("ScrollOffset").set(scroll);
		shader.safeGetUniform("UnlockedRatio")
				.set(menu.highestUnlockedFloor() / (float) DkcFloorRegistry.LAST_FLOOR);
	}

	@Override
	protected void renderBackgroundFallback(GuiGraphics graphics, int x, int y,
			float localX, float localY) {
		graphics.fillGradient(x, y, x + pW, y + pH, 0xF02B0209, 0xF0010002);
		float time = (Util.getMillis() % 100000L) / 1000.0F;

		for (int band = 0; band < 7; band++) {
			int bandY = y + Math.floorMod((int) (time * (5 + band) + band * 47), pH);
			int alpha = 16 + band * 4;
			graphics.fill(x, bandY, x + pW, Math.min(y + pH, bandY + 6),
					(alpha << 24) | 0x6B0614);
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

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isOpen() || button != 0)
			return super.mouseClicked(mouseX, mouseY, button);
		int logicalX = (int) Math.round(logicalMouseX(mouseX));
		int logicalY = (int) Math.round(logicalMouseY(mouseY));
		int viewLeft = panelLeft() + VIEW_X;
		int viewTop = panelTop() + VIEW_Y + 15;
		int viewHeight = VIEW_H - 16;
		if (!inside(logicalX, logicalY, viewLeft, viewTop, VIEW_W, viewHeight))
			return super.mouseClicked(mouseX, mouseY, button);

		if (logicalX >= viewLeft + VIEW_W - 9) {
			draggingScrollbar = true;
			setScrollFromScrollbar(logicalY, viewTop, viewHeight);
			return true;
		}
		int floor = floorAt(logicalX, logicalY, viewLeft, viewTop, VIEW_W, viewHeight);
		if (floor > 0) {
			selectedFloor = floor;
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
				setScrollFromScrollbar(logicalY, panelTop() + VIEW_Y + 15, VIEW_H - 16);
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
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int logicalX = (int) Math.round(logicalMouseX(mouseX));
		int logicalY = (int) Math.round(logicalMouseY(mouseY));
		int viewLeft = panelLeft() + VIEW_X;
		int viewTop = panelTop() + VIEW_Y;
		if (inside(logicalX, logicalY, viewLeft, viewTop, VIEW_W, VIEW_H)) {
			targetScroll = clampScroll(targetScroll - (float) delta * FLOOR_STEP * 1.35F);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
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
		lines.add(Component.literal("Click to inspect"));
		drawDkcTooltip(graphics, lines, mouseX, mouseY);
	}

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

	private void selectAndFocus(int floor) {
		selectedFloor = clampFloor(floor);
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

	private String shortFloorState(int floor) {
		if (menu.insideDkc() && menu.currentFloor() == floor)
			return "YOU ARE HERE";
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

	private int floorAt(int mouseX, int mouseY, int viewLeft, int viewTop,
			int viewWidth, int viewHeight) {
		if (!inside(mouseX, mouseY, viewLeft, viewTop, viewWidth, viewHeight))
			return 0;
		int roundedScroll = Math.round(scroll);
		for (int floor = DkcFloorRegistry.FIRST_FLOOR;
				floor <= DkcFloorRegistry.LAST_FLOOR; floor++) {
			NodeBounds node = nodeBounds(floor, viewLeft, viewTop, roundedScroll);
			if (inside(mouseX, mouseY, node.x(), node.y(), node.width(), node.height()))
				return floor;
		}
		return 0;
	}

	private NodeBounds nodeBounds(int floor, int viewLeft, int viewTop, int roundedScroll) {
		boolean landmark = isLandmark(floor);
		int width = landmark ? 102 : 76;
		int height = landmark ? 30 : 25;
		int centerX = viewLeft + VIEW_W / 2 - 2;
		int x;
		if (landmark)
			x = centerX - width / 2;
		else if ((floor & 1) == 0)
			x = centerX + 11;
		else
			x = centerX - width - 11;
		int y = viewTop + floorContentY(floor) - roundedScroll - height / 2;
		return new NodeBounds(x, y, width, height);
	}

	private static boolean isLandmark(int floor) {
		return floor == 1 || floor == 10 || floor == 15 || floor == 20;
	}

	private static int floorContentY(int floor) {
		return CONTENT_PAD + (DkcFloorRegistry.LAST_FLOOR - floor) * FLOOR_STEP;
	}

	private float focusOffset(int floor) {
		float nodeY = floorContentY(clampFloor(floor));
		return clampScroll(nodeY - (VIEW_H - 16) * 0.64F);
	}

	private void advanceScroll() {
		targetScroll = clampScroll(targetScroll);
		scroll += (targetScroll - scroll) * 0.24F;
		if (Math.abs(targetScroll - scroll) < 0.08F)
			scroll = targetScroll;
		mouseVelocityX *= 0.94F;
		mouseVelocityY *= 0.94F;
	}

	private void setScrollFromScrollbar(int mouseY, int viewTop, int viewHeight) {
		int usable = viewHeight - 8;
		int thumbHeight = Math.max(22, Math.round(usable * (viewHeight / (float) CONTENT_HEIGHT)));
		int travel = Math.max(1, usable - thumbHeight);
		float fraction = (mouseY - viewTop - 4 - thumbHeight / 2.0F) / travel;
		targetScroll = clampScroll(fraction * maxScroll());
		scroll = targetScroll;
	}

	private static float maxScroll() {
		return Math.max(0, CONTENT_HEIGHT - (VIEW_H - 16));
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

	private static void drawPanelFrame(GuiGraphics graphics, int x, int y, int width, int height) {
		graphics.fill(x - 1, y - 1, x + width + 1, y, CRIMSON_SOFT);
		graphics.fill(x - 1, y + height, x + width + 1, y + height + 1, CRIMSON_SOFT);
		graphics.fill(x - 1, y, x, y + height, CRIMSON_SOFT);
		graphics.fill(x + width, y, x + width + 1, y + height, CRIMSON_SOFT);
		drawOutline(graphics, x, y, width, height, CRIMSON_DIM);
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
		int padding = 5;
		int textWidth = 0;
		for (Component line : lines)
			textWidth = Math.max(textWidth, this.font.width(line));
		int width = textWidth + padding * 2;
		int height = lines.size() * 10 + padding * 2 - 2;
		int x = mouseX + 12;
		int y = mouseY - 12;
		if (x + width > this.width - 2)
			x = mouseX - width - 12;
		if (y + height > this.height - 2)
			y = this.height - height - 2;
		x = Math.max(2, x);
		y = Math.max(2, y);

		graphics.fillGradient(x, y, x + width, y + height, 0xF032050E, 0xF0070104);
		drawOutline(graphics, x, y, width, height, CRIMSON_DIM);
		drawCornerBrackets(graphics, x, y, width, height, CRIMSON);
		int textY = y + padding;
		for (int index = 0; index < lines.size(); index++) {
			graphics.drawString(this.font, lines.get(index), x + padding, textY,
					index == 0 ? CRIMSON_HOT : TEXT_MAIN, false);
			textY += 10;
		}
	}

	private static final class DkcButton extends Button {
		private final java.util.function.Supplier<Component> label;
		private final BooleanSupplier enabled;

		private DkcButton(int x, int y, int width, int height,
				java.util.function.Supplier<Component> label, BooleanSupplier enabled,
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
			Component currentLabel = label.get();
			setMessage(currentLabel);
			boolean usable = enabled.getAsBoolean();
			boolean hovered = usable && isHoveredOrFocused();
			int fill = !usable ? 0x9910080B : hovered ? 0xB05A0D1B : 0x99240710;
			int border = !usable ? LOCKED_BORDER : hovered ? CRIMSON_HOT : CRIMSON_DIM;
			int text = !usable ? TEXT_MUTED : hovered ? 0xFFFFFFFF : TEXT_MAIN;
			graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
			drawOutline(graphics, getX(), getY(), width, height, border);
			Font font = Minecraft.getInstance().font;
			graphics.drawCenteredString(font, currentLabel, getX() + width / 2,
					getY() + (height - 8) / 2, text);
		}
	}

	private record NodeBounds(int x, int y, int width, int height) {
	}
}
