package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.AbilitiesGUIButtonMessage;
import net.solocraft.network.PanelRework2ButtonMessage;
import net.solocraft.procedures.ReturnAgilityProcedure;
import net.solocraft.procedures.ReturnFatigueProcedure;
import net.solocraft.procedures.ReturnHPProcedure;
import net.solocraft.procedures.ReturnIntelligenceProcedure;
import net.solocraft.procedures.ReturnJobProcedure;
import net.solocraft.procedures.ReturnLevelProcedure;
import net.solocraft.procedures.ReturnMPProcedure;
import net.solocraft.procedures.ReturnNameProcedure;
import net.solocraft.procedures.ReturnPerceptionProcedure;
import net.solocraft.procedures.ReturnRemainingXPProcedure;
import net.solocraft.procedures.ReturnSPProcedure;
import net.solocraft.procedures.ReturnStrengthProcedure;
import net.solocraft.procedures.ReturnTitleProcedure;
import net.solocraft.procedures.ReturnVitalityProcedure;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class SystemPanelScreen extends SystemScreen {
	private static final int STAT_ROW0 = 136;
	private static final int STAT_STEP = 16;

	private static final List<List<Component>> STAT_TIPS = List.of(
			List.of(tipTitle("Strength"), tipText("Increases physical attack damage.")),
			List.of(tipTitle("Agility"), tipText("Increases movement speed and mobility.")),
			List.of(tipTitle("Perception"), tipText("Raises your chance to auto-dodge attacks.")),
			List.of(tipTitle("Vitality"), tipText("Increases your maximum health (HP).")),
			List.of(tipTitle("Intelligence"), tipText("Increases your maximum mana (MP).")));

	public SystemPanelScreen() {
		super(Component.literal("SYSTEM"));
		this.panelW = 196;
		this.panelH = 282;
	}

	@Override
	protected void init() {
		super.init();
		addRenderableWidget(new SystemButton(panelX + panelW - 15, panelY + 3, 12, 12, Component.literal("X"), b -> beginClose()));
		addRenderableWidget(new SystemButton(panelX + panelW - 29, panelY + 3, 12, 12, Component.literal("S"), b -> openChild(new SystemSettingsScreen())));
		addRenderableWidget(new InvisibleButton(panelX + 12, panelY + 46, 135, 14, b -> openChild(new SystemTitlesScreen())));

		for (int i = 0; i < 5; i++) {
			final int id = i;
			int y = panelY + STAT_ROW0 + i * STAT_STEP;
			addRenderableWidget(new SystemButton(panelX + panelW - 24, y - 2, 14, 12, Component.literal("+"), b -> sendPanelAction(id)));
		}

		Runnable[] navActions = {
				() -> sendPanelAction(5),
				() -> openChild(new SystemQuestsScreen()),
				() -> openChild(new SystemRewardsScreen()),
				() -> openChild(new SystemTrainScreen()),
				() -> sendPanelAction(8),
				this::openSkills
		};
		String[] navLabels = { "Shop", "Quests", "Rewards", "Train", "Craft", "Skills" };
		int navX0 = panelX + 12;
		int navY0 = panelY + 236;
		int navW = 53;
		int navH = 18;
		int gap = 6;
		for (int i = 0; i < navLabels.length; i++) {
			final Runnable action = navActions[i];
			int col = i % 3;
			int row = i / 3;
			int x = navX0 + col * (navW + gap);
			int y = navY0 + row * (navH + gap);
			addRenderableWidget(new SystemButton(x, y, navW, navH, Component.literal(navLabels[i]), b -> action.run()));
		}
	}

	private void openSkills() {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos bp = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(5, bp.getX(), bp.getY(), bp.getZ()));
	}

	private void sendPanelAction(int id) {
		Player player = Minecraft.getInstance().player;
		if (player == null)
			return;
		BlockPos bp = player.blockPosition();
		SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(id, bp.getX(), bp.getY(), bp.getZ()));
	}

	@Override
	protected void renderContent(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		Player entity = Minecraft.getInstance().player;
		if (entity == null)
			return;
		Font font = this.font;
		int lx = panelX + 12;

		g.drawString(font, ReturnLevelProcedure.execute(entity), lx, panelY + 24, TEXT_MAIN, false);
		g.drawString(font, ReturnNameProcedure.execute(entity), lx, panelY + 36, TEXT_MAIN, false);
		g.drawString(font, ReturnTitleProcedure.execute(entity), lx, panelY + 48, TEXT_SUB, false);
		g.drawString(font, ReturnJobProcedure.execute(entity), lx, panelY + 60, TEXT_SUB, false);
		g.drawString(font, ReturnFatigueProcedure.execute(entity), lx, panelY + 72, TEXT_SUB, false);

		divider(g, panelY + 86);
		g.drawString(font, ReturnHPProcedure.execute(entity), lx, panelY + 92, 0xFFFF6B6B, false);
		g.drawString(font, ReturnMPProcedure.execute(entity), lx, panelY + 104, 0xFF6BB8FF, false);

		divider(g, panelY + 118);
		g.drawString(font, "ATTRIBUTES", lx, panelY + 122, ACCENT, false);
		for (int i = 0; i < 5; i++)
			g.drawString(font, statString(entity, i), lx, panelY + STAT_ROW0 + i * STAT_STEP, TEXT_MAIN, false);

		g.drawString(font, ReturnSPProcedure.execute(entity), lx, panelY + 218, 0xFFFFD966, false);
		divider(g, panelY + 230);
	}

	@Override
	protected List<Component> getHoverTooltip(int mouseX, int mouseY) {
		Player entity = Minecraft.getInstance().player;
		if (entity == null)
			return null;
		Font font = this.font;
		int lx = panelX + 12;

		String levelStr = ReturnLevelProcedure.execute(entity);
		if (isOver(mouseX, mouseY, lx, panelY + 24, font.width(levelStr), 10))
			return List.of(tipTitle("Level Progress"), tipText(ReturnRemainingXPProcedure.execute(entity)));

		for (int i = 0; i < 5; i++) {
			int y = panelY + STAT_ROW0 + i * STAT_STEP;
			if (isOver(mouseX, mouseY, lx, y, font.width(statString(entity, i)), 10))
				return STAT_TIPS.get(i);
		}
		if (isOver(mouseX, mouseY, panelX + panelW - 29, panelY + 3, 12, 12))
			return List.of(tipTitle("Settings"), tipText("Open System settings."));
		return null;
	}

	private static Component tipTitle(String text) {
		return Component.literal(text).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
	}

	private static Component tipText(String text) {
		return Component.literal(text).withStyle(ChatFormatting.GRAY);
	}

	private static String statString(Player entity, int i) {
		return switch (i) {
			case 0 -> ReturnStrengthProcedure.execute(entity);
			case 1 -> ReturnAgilityProcedure.execute(entity);
			case 2 -> ReturnPerceptionProcedure.execute(entity);
			case 3 -> ReturnVitalityProcedure.execute(entity);
			default -> ReturnIntelligenceProcedure.execute(entity);
		};
	}

	private void divider(GuiGraphics g, int y) {
		g.fill(panelX + 10, y, panelX + panelW - 10, y + 1, ACCENT_SOFT);
	}

	private static class InvisibleButton extends Button {
		InvisibleButton(int x, int y, int w, int h, OnPress onPress) {
			super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		}
	}
}
