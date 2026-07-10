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

/**
 * System Settings. Combat Mode stays keybind-only and is not shown here.
 */
public class SystemSettingsScreen extends SystemScreen {

	private static final int ROW0 = 42;
	private static final int ROW_STEP = 40;

	public SystemSettingsScreen() {
		super(Component.literal("SETTINGS"));
		this.panelW = 220;
		this.panelH = 286;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new SystemButton(panelX + 3, panelY + 3, 40, 12, Component.literal("< Back"), b -> openChild(new SystemPanelScreen())));

		int toggleX = panelX + panelW - 70;
		addRenderableWidget(new SystemButton(toggleX, panelY + ROW0 - 2, 58, 18, Component.literal("Toggle"), b -> settingToggle(1)));
		addRenderableWidget(new SystemButton(toggleX, panelY + ROW0 + ROW_STEP - 2, 58, 18, Component.literal("Toggle"), b -> abilityAction(3)));

		int speedY = panelY + ROW0 + 2 * ROW_STEP - 2;
		addRenderableWidget(new SystemButton(panelX + panelW - 72, speedY, 30, 18, Component.literal("-"), b -> abilityAction(1)));
		addRenderableWidget(new SystemButton(panelX + panelW - 38, speedY, 30, 18, Component.literal("+"), b -> abilityAction(2)));
		addRenderableWidget(new SystemButton(toggleX, panelY + ROW0 + 3 * ROW_STEP - 2, 58, 18, Component.literal("Toggle"), b -> SystemClientConfig.toggleDamageNumbers()));
		addRenderableWidget(new SystemButton(toggleX, panelY + ROW0 + 4 * ROW_STEP - 2, 58, 18, Component.literal("Toggle"), b -> SystemClientConfig.toggleLegacyOverlay()));

		float min = SystemClientConfig.MIN_SCALE, max = SystemClientConfig.MAX_SCALE;
		double init01 = (SystemClientConfig.getNotificationScale() - min) / (max - min);
		int sliderY = panelY + ROW0 + 5 * ROW_STEP + 11;
		addRenderableWidget(new SystemSlider(panelX + 16, sliderY, panelW - 32, 14, init01,
				v -> Component.literal("\u00A7bSize: " + Math.round((min + v * (max - min)) * 100) + "%"),
				v -> SystemClientConfig.setNotificationScale((float) (min + v * (max - min))),
				() -> SystemNotificationManager.INSTANCE.push(0xFF3FC6FF, 40,
						Component.literal("\u00A7eSYSTEM"), Component.literal("\u00A77Notification preview"))));
	}

	private void settingToggle(int id) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos bp = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new SystemSettingsButtonMessage(id, bp.getX(), bp.getY(), bp.getZ()));
	}

	private void abilityAction(int id) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos bp = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(id, bp.getX(), bp.getY(), bp.getZ()));
	}

	@Override
	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		Player entity = Minecraft.getInstance().player;
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());

		drawRow(g, 0, "Custom HUD", onOff(vars.CustomHUD));
		drawRow(g, 1, "Triple Jump", onOff(vars.tjonoff));
		drawRow(g, 2, "Move Speed", "\u00A7b" + (int) Math.round(vars.speedpercent) + "%");
		drawRow(g, 3, "Damage Numbers", onOff(SystemClientConfig.isDamageNumbersEnabled()));
		drawRow(g, 4, "Legacy Overlay", onOff(SystemClientConfig.isLegacyOverlayEnabled()));
		g.drawString(this.font, "Notification Size", panelX + 16, panelY + ROW0 + 5 * ROW_STEP, TEXT_MAIN, false);
	}

	private static String onOff(boolean on) {
		return on ? "\u00A7a[ON]" : "\u00A7c[OFF]";
	}

	private void drawRow(GuiGraphics g, int i, String label, String state) {
		int y = panelY + ROW0 + i * ROW_STEP;
		g.drawString(this.font, label, panelX + 16, y, TEXT_MAIN, false);
		g.drawString(this.font, state, panelX + 16, y + 12, TEXT_SUB, false);
		g.fill(panelX + 12, y + 26, panelX + panelW - 12, y + 27, ACCENT_SOFT);
	}
}
