package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.TitleSelectionMessage;
import net.solocraft.util.TitleManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class SystemTitlesScreen extends SystemScreen {
	private static final int NONE_ROW_Y = 56;
	private static final int WOLF_ROW_Y = 94;
	private static final int ROW_X = 18;
	private static final int ROW_W = 184;
	private static final int ROW_H = 28;

	public SystemTitlesScreen() {
		super(Component.literal("TITLES"));
		this.panelW = 220;
		this.panelH = 180;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new SystemButton(panelX + 3, panelY + 3, 40, 12, Component.literal("< Back"), b -> openChild(new SystemPanelScreen())));
		addTitleButton(TitleManager.NONE, NONE_ROW_Y);
		addTitleButton(TitleManager.WOLF_ASSASSIN, WOLF_ROW_Y);
	}

	private void addTitleButton(int titleId, int rowY) {
		addRenderableWidget(new SystemButton(panelX + ROW_X + ROW_W - 54, panelY + rowY + 15, 46, 10, Component.literal("Select"), b -> selectTitle(titleId)));
	}

	private void selectTitle(int titleId) {
		SololevelingMod.PACKET_HANDLER.sendToServer(new TitleSelectionMessage(titleId));
	}

	@Override
	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		SololevelingModVariables.PlayerVariables vars = vars(player);
		g.drawString(this.font, "AVAILABLE TITLES", panelX + 18, panelY + 30, ACCENT, false);
		drawTitleRow(g, vars, TitleManager.NONE, NONE_ROW_Y);
		drawTitleRow(g, vars, TitleManager.WOLF_ASSASSIN, WOLF_ROW_Y);
	}

	private void drawTitleRow(GuiGraphics g, SololevelingModVariables.PlayerVariables vars, int titleId, int rowY) {
		int x = panelX + ROW_X;
		int y = panelY + rowY;
		boolean unlocked = TitleManager.isUnlocked(vars, titleId);
		boolean equipped = (int) vars.title == titleId;
		int border = equipped ? 0xFFFFD966 : (unlocked ? ACCENT_DIM : 0xFF4B5160);
		int fill = equipped ? 0x443FC6FF : 0x33102338;
		g.fill(x, y, x + ROW_W, y + ROW_H, fill);
		g.fill(x, y, x + ROW_W, y + 1, border);
		g.fill(x, y + ROW_H - 1, x + ROW_W, y + ROW_H, border);
		g.fill(x, y, x + 1, y + ROW_H, border);
		g.fill(x + ROW_W - 1, y, x + ROW_W, y + ROW_H, border);
		String name = TitleManager.displayName(titleId);
		String state = equipped ? "EQUIPPED" : (unlocked ? "UNLOCKED" : "LOCKED");
		int stateColor = equipped ? 0xFFFFD966 : (unlocked ? 0xFF7FE4FF : 0xFFFF6B6B);
		g.drawString(this.font, name, x + 8, y + 6, unlocked ? TEXT_MAIN : TEXT_SUB, false);
		g.drawString(this.font, state, x + ROW_W - this.font.width(state) - 8, y + 4, stateColor, false);
		if (titleId == TitleManager.WOLF_ASSASSIN && !unlocked) {
			String progress = (int) Math.min(vars.wolfAssassinKills, TitleManager.WOLF_ASSASSIN_REQUIRED_KILLS) + "/" + TitleManager.WOLF_ASSASSIN_REQUIRED_KILLS;
			g.drawString(this.font, progress, x + 8, y + 17, TEXT_SUB, false);
		}
	}

	@Override
	protected List<Component> getHoverTooltip(int mouseX, int mouseY) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return null;
		SololevelingModVariables.PlayerVariables vars = vars(player);
		if (isOver(mouseX, mouseY, panelX + ROW_X, panelY + NONE_ROW_Y, ROW_W, ROW_H)) {
			return TitleManager.tooltip(vars, TitleManager.NONE);
		}
		if (isOver(mouseX, mouseY, panelX + ROW_X, panelY + WOLF_ROW_Y, ROW_W, ROW_H)) {
			return TitleManager.tooltip(vars, TitleManager.WOLF_ASSASSIN);
		}
		return null;
	}

	private static SololevelingModVariables.PlayerVariables vars(Player player) {
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}
}
