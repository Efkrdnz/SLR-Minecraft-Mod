package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.AbilitiesGUIButtonMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.SystemSettingsButtonMessage;
import net.solocraft.util.SystemClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Locale;

/**
 * Categorized System settings. Combat Mode remains keybind-only.
 *
 * <p>Popup controls live on a panel shifted to the right so their live
 * notification preview remains visible on the left side of the world view.</p>
 */
public class SystemSettingsScreen extends SystemScreen {
	private static final int CONTENT_TOP = 67;
	private static final int ROW_STEP = 46;
	private static final int TAB_GAP = 3;
	private static final int TAB_WIDTH = 59;

	private enum Category {
		GENERAL("General"),
		POPUPS("Popups"),
		HIGHLIGHTS("Highlights"),
		OVERLAY("Overlay");

		private final String label;

		Category(String label) {
			this.label = label;
		}
	}

	private final boolean returnToSkills;
	private Category category = Category.GENERAL;
	private SystemSlider popupPositionSlider;
	private SystemSlider popupLifetimeSlider;

	public SystemSettingsScreen() {
		this(false);
	}

	public SystemSettingsScreen(boolean returnToSkills) {
		super(Component.literal("SETTINGS"));
		this.returnToSkills = returnToSkills;
		this.panelW = 270;
		this.panelH = 322;
	}

	@Override
	protected void init() {
		super.init();
		positionPanel();
		buildWidgets();
	}

	@Override
	protected void rebuildWidgets() {
		clearWidgets();
		positionPanel();
		buildWidgets();
	}

	private void positionPanel() {
		this.panelX = (this.width - panelW) / 2;
		this.panelY = (this.height - panelH) / 2;
		if (category == Category.POPUPS) {
			int available = Math.max(0, (this.width - panelW) / 2 - 8);
			this.panelX += Math.min(82, available);
		}
	}

	private void buildWidgets() {
		addRenderableWidget(new SystemButton(panelX + 3, panelY + 3, 40, 12,
				Component.literal("< Back"), button -> {
					if (returnToSkills)
						openSkills();
					else
						openChild(new SystemPanelScreen());
				}));

		int tabsX = panelX + 10;
		for (Category value : Category.values()) {
			final Category selected = value;
			String label = value == category ? "[" + value.label + "]" : value.label;
			addRenderableWidget(new SystemButton(
					tabsX + value.ordinal() * (TAB_WIDTH + TAB_GAP), panelY + 25,
					TAB_WIDTH, 18, Component.literal(label), button -> selectCategory(selected)));
		}

		switch (category) {
			case GENERAL -> buildGeneralWidgets();
			case POPUPS -> buildPopupWidgets();
			case HIGHLIGHTS -> buildHighlightWidgets();
			case OVERLAY -> buildOverlayWidgets();
		}
	}

	private void selectCategory(Category selected) {
		if (selected == category)
			return;
		category = selected;
		SystemGuiSounds.switchInsideSystem();
		rebuildWidgets();
	}

	private void buildGeneralWidgets() {
		int buttonX = panelX + panelW - 76;
		int y = contentY(0);
		addRenderableWidget(new SystemButton(buttonX, y, 62, 18,
				Component.literal("Toggle"), button -> abilityAction(3)));

		y = contentY(1);
		addRenderableWidget(new SystemButton(panelX + panelW - 78, y, 30, 18,
				Component.literal("-"), button -> abilityAction(1)));
		addRenderableWidget(new SystemButton(panelX + panelW - 44, y, 30, 18,
				Component.literal("+"), button -> abilityAction(2)));

		y = contentY(2);
		addRenderableWidget(new SystemButton(buttonX, y, 62, 18,
				Component.literal("Toggle"), button -> SystemClientConfig.toggleDamageNumbers()));

		if (hasSystemPlayer()) {
			y = contentY(3);
			addRenderableWidget(new SystemButton(buttonX, y, 62, 18,
					Component.literal("Toggle"), button -> settingToggle(2)));
		}
	}

	private void buildPopupWidgets() {
		int buttonX = panelX + panelW - 76;
		addRenderableWidget(new SystemButton(buttonX, contentY(0), 62, 18,
				Component.literal("Toggle"), button -> {
					SystemClientConfig.toggleDynamicNotifications();
					previewNotification();
					rebuildWidgets();
				}));

		float minScale = SystemClientConfig.MIN_SCALE;
		float maxScale = SystemClientConfig.MAX_SCALE;
		double scaleValue = normalize(SystemClientConfig.getNotificationScale(), minScale, maxScale);
		addRenderableWidget(new SystemSlider(panelX + 16, panelY + 119, panelW - 32, 16,
				scaleValue,
				value -> Component.literal("\u00A7bSize: "
						+ Math.round((minScale + value * (maxScale - minScale)) * 100.0D) + "%"),
				value -> SystemClientConfig.setNotificationScale(
						(float) (minScale + value * (maxScale - minScale))),
				this::previewNotification));

		float minPosition = SystemClientConfig.MIN_NOTIFICATION_POSITION;
		float maxPosition = SystemClientConfig.MAX_NOTIFICATION_POSITION;
		double positionValue = normalize(SystemClientConfig.getNotificationHorizontalOffset(),
				minPosition, maxPosition);
		popupPositionSlider = addRenderableWidget(new SystemSlider(
				panelX + 16, panelY + 169, panelW - 32, 16, positionValue,
				value -> Component.literal("\u00A7b" + positionLabel(
						(float) (minPosition + value * (maxPosition - minPosition)))),
				value -> SystemClientConfig.setNotificationHorizontalOffset(
						(float) (minPosition + value * (maxPosition - minPosition))),
				this::previewNotification));

		float minLifetime = SystemClientConfig.MIN_NOTIFICATION_LIFETIME;
		float maxLifetime = SystemClientConfig.MAX_NOTIFICATION_LIFETIME;
		double lifetimeValue = normalize(SystemClientConfig.getNotificationLifetimeSeconds(),
				minLifetime, maxLifetime);
		popupLifetimeSlider = addRenderableWidget(new SystemSlider(
				panelX + 16, panelY + 219, panelW - 32, 16, lifetimeValue,
				value -> Component.literal("\u00A7bLifetime: " + String.format(Locale.ROOT,
						"%.1fs", minLifetime + value * (maxLifetime - minLifetime))),
				value -> SystemClientConfig.setNotificationLifetimeSeconds(
						(float) (minLifetime + value * (maxLifetime - minLifetime))),
				this::previewNotification));

		boolean manual = !SystemClientConfig.isDynamicNotificationsEnabled();
		popupPositionSlider.active = manual;
		popupLifetimeSlider.active = manual;
	}

	private void buildHighlightWidgets() {
		int buttonX = panelX + panelW - 76;
		addRenderableWidget(new SystemButton(buttonX, contentY(0), 62, 18,
				Component.literal("Toggle"), button -> SystemClientConfig.toggleEntityOutlines()));
		addRenderableWidget(new SystemButton(buttonX, contentY(1), 62, 18,
				Component.literal("Change"), button -> SystemClientConfig.cycleOutlineDensity()));
		addRenderableWidget(new SystemButton(buttonX, contentY(2), 62, 18,
				Component.literal("Toggle"), button -> SystemClientConfig.togglePerceptionOutlines()));
		addRenderableWidget(new SystemButton(buttonX, contentY(3), 62, 18,
				Component.literal("Toggle"), button -> SystemClientConfig.toggleEncounterOutlines()));
	}

	private void buildOverlayWidgets() {
		int buttonX = panelX + panelW - 76;
		addRenderableWidget(new SystemButton(buttonX, contentY(0), 62, 18,
				Component.literal("Toggle"), button -> settingToggle(1)));
		addRenderableWidget(new SystemButton(buttonX, contentY(1), 62, 18,
				Component.literal("Toggle"), button -> SystemClientConfig.toggleLegacyOverlay()));
	}

	@Override
	protected boolean allowsNonSystemAccess() {
		return true;
	}

	private void openSkills() {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos position = player.blockPosition();
		if (this.minecraft != null)
			this.minecraft.setScreen(null);
		SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(
				5, position.getX(), position.getY(), position.getZ()));
	}

	private void settingToggle(int id) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos position = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new SystemSettingsButtonMessage(
				id, position.getX(), position.getY(), position.getZ()));
	}

	private void abilityAction(int id) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos position = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(
				id, position.getX(), position.getY(), position.getZ()));
	}

	private void previewNotification() {
		SystemNotificationManager.INSTANCE.push(0xFF3FC6FF, 80,
				Component.literal("\u00A7eSYSTEM PREVIEW"),
				Component.literal("\u00A77Position \u00B7 size \u00B7 lifetime"));
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		Player entity = Minecraft.getInstance().player;
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables variables = entity
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());

		switch (category) {
			case GENERAL -> renderGeneral(graphics, variables);
			case POPUPS -> renderPopups(graphics);
			case HIGHLIGHTS -> renderHighlights(graphics);
			case OVERLAY -> renderOverlay(graphics, variables);
		}
	}

	private void renderGeneral(GuiGraphics graphics,
			SololevelingModVariables.PlayerVariables variables) {
		drawRow(graphics, 0, "Triple Jump", onOff(variables.tjonoff));
		drawRow(graphics, 1, "Move Speed", "\u00A7b" + (int) Math.round(variables.speedpercent) + "%");
		drawRow(graphics, 2, "Damage Numbers",
				onOff(SystemClientConfig.isDamageNumbersEnabled()));
		if (variables.Player)
			drawRow(graphics, 3, "PvP Urgent Quests", onOff(variables.pvpUrgentQuests));
		else
			drawLockedNote(graphics, "Awaken the System for Player-only settings.");
	}

	private void renderPopups(GuiGraphics graphics) {
		boolean dynamic = SystemClientConfig.isDynamicNotificationsEnabled();
		drawRow(graphics, 0, "Dynamic Layout", onOff(dynamic));
		graphics.drawString(this.font, "Popup Size", panelX + 16, panelY + 106,
				TEXT_MAIN, false);
		graphics.drawString(this.font, dynamic ? "Horizontal Position (automatic)"
				: "Horizontal Position", panelX + 16, panelY + 156,
				dynamic ? 0xFF60788A : TEXT_MAIN, false);
		graphics.drawString(this.font, dynamic ? "Lifetime (automatic)"
				: "Lifetime", panelX + 16, panelY + 206,
				dynamic ? 0xFF60788A : TEXT_MAIN, false);
		String note = dynamic
				? "Dynamic mode keeps wide popups clear of the crosshair and times them by text length."
				: "Drag a slider, then release it to spawn a live preview on the left.";
		drawWrapped(graphics, note, panelX + 16, panelY + 248, panelW - 32,
				dynamic ? 0xFF7895AA : TEXT_SUB);
	}

	private void renderHighlights(GuiGraphics graphics) {
		drawRow(graphics, 0, "Target Highlights",
				onOff(SystemClientConfig.isEntityOutlinesEnabled()));
		drawRow(graphics, 1, "Highlight Density",
				"\u00A7b[" + SystemClientConfig.getOutlineDensityLabel() + "]");
		drawRow(graphics, 2, "Sense & Skills",
				onOff(SystemClientConfig.isPerceptionOutlinesEnabled()));
		drawRow(graphics, 3, "Dungeon Finder",
				onOff(SystemClientConfig.isEncounterOutlinesEnabled()));
	}

	private void renderOverlay(GuiGraphics graphics,
			SololevelingModVariables.PlayerVariables variables) {
		drawRow(graphics, 0, "Custom System Overlay", onOff(variables.CustomHUD));
		drawRow(graphics, 1, "Legacy Overlay",
				onOff(SystemClientConfig.isLegacyOverlayEnabled()));
		drawWrapped(graphics,
				"Choose the modern custom HUD or retain the original resource-based overlay.",
				panelX + 16, panelY + 174, panelW - 32, TEXT_SUB);
	}

	@Override
	protected List<Component> getHoverTooltip(int mouseX, int mouseY) {
		if (mouseX < panelX + 12 || mouseX > panelX + panelW - 12)
			return super.getHoverTooltip(mouseX, mouseY);
		int row = (mouseY - (panelY + CONTENT_TOP)) / ROW_STEP;
		if (mouseY < panelY + CONTENT_TOP)
			return super.getHoverTooltip(mouseX, mouseY);
		if (category == Category.POPUPS && row == 0)
			return List.of(Component.literal(
					"Automatically offsets each popup by its rendered width and chooses a readable lifetime."));
		if (category == Category.HIGHLIGHTS && row == 0)
			return List.of(Component.literal("Master switch for all private target highlights."));
		if (category == Category.HIGHLIGHTS && row == 1)
			return List.of(Component.literal(
					"Minimal favors essentials; Balanced prioritizes hidden and distant targets; High shows more."));
		if (category == Category.HIGHLIGHTS && row == 2)
			return List.of(Component.literal(
					"Perception, Detection Eye, Hyper Focus, and other skill highlights."));
		if (category == Category.HIGHLIGHTS && row == 3)
			return List.of(Component.literal(
					"Dungeon, Red Gate, and Demon King's Castle encounter highlights."));
		if (category == Category.OVERLAY && row == 0)
			return List.of(Component.literal("Toggle the modern custom System HUD."));
		if (category == Category.OVERLAY && row == 1)
			return List.of(Component.literal("Toggle the original legacy HUD presentation."));
		return super.getHoverTooltip(mouseX, mouseY);
	}

	private int contentY(int row) {
		return panelY + CONTENT_TOP + row * ROW_STEP;
	}

	private void drawRow(GuiGraphics graphics, int row, String label, String state) {
		int y = contentY(row);
		graphics.drawString(this.font, label, panelX + 16, y, TEXT_MAIN, false);
		graphics.drawString(this.font, state, panelX + 16, y + 12, TEXT_SUB, false);
		graphics.fill(panelX + 12, y + 29, panelX + panelW - 12, y + 30, ACCENT_SOFT);
	}

	private void drawLockedNote(GuiGraphics graphics, String text) {
		drawWrapped(graphics, text, panelX + 16, panelY + 220, panelW - 32,
				0xFF60788A);
	}

	private void drawWrapped(GuiGraphics graphics, String text, int x, int y,
			int width, int color) {
		int line = 0;
		for (net.minecraft.util.FormattedCharSequence sequence
				: this.font.split(Component.literal(text), width)) {
			graphics.drawString(this.font, sequence, x, y + line * 11, color, false);
			line++;
		}
	}

	private static boolean hasSystemPlayer() {
		Player player = Minecraft.getInstance().player;
		return player != null && player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.Player)
				.orElse(false);
	}

	private static String onOff(boolean on) {
		return on ? "\u00A7a[ON]" : "\u00A7c[OFF]";
	}

	private static double normalize(float value, float minimum, float maximum) {
		return Math.max(0.0D, Math.min(1.0D,
				(value - minimum) / (double) (maximum - minimum)));
	}

	private static String positionLabel(float offset) {
		if (Math.abs(offset) < 0.04F)
			return "Position: Original";
		int percent = Math.round(Math.abs(offset)
				/ SystemClientConfig.MAX_NOTIFICATION_POSITION * 100.0F);
		return "Position: " + (offset > 0.0F ? "Left " : "Right ") + percent + "%";
	}
}
