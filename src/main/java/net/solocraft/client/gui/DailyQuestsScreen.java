package net.solocraft.client.gui;

import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemQuestsScreen;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.client.gui.system.SystemTooltip;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.DailyQuestHelper;
import net.solocraft.world.inventory.DailyQuestsMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.List;
import java.util.Locale;

/**
 * Read-only System dashboard for the Minecraft-native Daily Quest objectives.
 * All objective progress is recorded automatically by the server.
 */
public class DailyQuestsScreen extends SystemContainerScreen<DailyQuestsMenu> {
	private static final int PANEL_W = 250;
	private static final int PANEL_H = 254;
	private static final int ROW_START = 51;
	private static final int ROW_HEIGHT = 50;
	private static final int ROW_STEP = 54;
	private static final int FOOTER_Y = 219;

	private static final int ROW_FILL = 0x55102338;
	private static final int ROW_COMPLETE = 0x5032A878;
	private static final int COMPLETE = 0xFF63E6A5;
	private static final int WARNING = 0xFFFFC65C;
	private static final int DANGER = 0xFFFF6B7D;
	private static final int SECRET_ACCENT = 0xFFFF354D;
	private static final int SECRET_ACCENT_DIM = 0xFF9E2635;
	private static final int SECRET_ACCENT_SOFT = 0x66FF354D;
	private static final int SECRET_PANEL_FILL = 0x99150912;
	private static final int SECRET_ROW_FILL = 0x66240A13;

	private final Player entity;
	private boolean returnToQuests;

	public DailyQuestsScreen(DailyQuestsMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -PANEL_W / 2;
		this.pRelY = -PANEL_H / 2;
		this.pW = PANEL_W;
		this.pH = PANEL_H;
	}

	@Override
	protected void init() {
		// A resize re-runs init(). Never carry a half-finished navigation action
		// into the newly laid-out screen.
		this.returnToQuests = false;
		super.init();
		int panelLeft = this.leftPos + pRelX;
		int panelTop = this.topPos + pRelY;
		this.addRenderableWidget(new SystemScreen.SystemButton(panelLeft + 3, panelTop + 2, 42, 12,
				Component.literal("< Back"), button -> returnToQuestHub()));
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		int panelLeft = leftPos + pRelX;
		int panelTop = topPos + pRelY;
		SololevelingModVariables.PlayerVariables vars = variables();
		boolean secretRevealed = isActiveSecretRevealed(vars);
		int accent = secretRevealed ? SECRET_ACCENT : ShopStyle.ACCENT;
		int accentDim = secretRevealed ? SECRET_ACCENT_DIM : ShopStyle.ACCENT_DIM;
		int accentSoft = secretRevealed ? SECRET_ACCENT_SOFT : ShopStyle.ACCENT_SOFT;

		if (secretRevealed) {
			drawSecretPanel(g, panelLeft, panelTop, pW, pH);
			drawSecretTitleBar(g, this.font, panelLeft, panelTop, pW);
		} else {
			ShopStyle.panel(g, panelLeft, panelTop, pW, pH);
			ShopStyle.titleBar(g, this.font, panelLeft, panelTop, pW, "DAILY QUEST");
		}

		for (int objective = 0; objective < 3; objective++) {
			int rowY = panelTop + ROW_START + objective * ROW_STEP;
			double progress = objectiveProgress(vars, objective);
			double target = objectiveTarget(objective);
			boolean complete = objectiveComplete(vars, objective, progress, target);

			drawObjectiveRow(g, panelLeft + 9, rowY, pW - 18, ROW_HEIGHT,
					complete ? ROW_COMPLETE : secretRevealed ? SECRET_ROW_FILL : ROW_FILL,
					accentDim, accentSoft);
			drawProgressBar(g, panelLeft + 14, rowY + 39, pW - 28, 6,
					objective == 1 && vars.dailyCombatWaived ? visibleTarget(objective) : progress,
					visibleTarget(objective),
					complete ? COMPLETE : accent, accentDim);
		}

		g.fill(panelLeft + 9, panelTop + FOOTER_Y, panelLeft + pW - 9,
				panelTop + FOOTER_Y + 1, accentSoft);
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		SololevelingModVariables.PlayerVariables vars = variables();
		boolean complete = allObjectivesComplete(vars);
		boolean secretRevealed = isActiveSecretRevealed(vars);
		int accent = secretRevealed ? SECRET_ACCENT : ShopStyle.ACCENT;

		String status = questStatus(vars, complete);
		g.drawString(this.font, status, pRelX + 12, pRelY + 24,
				statusColor(vars, complete), false);

		String timer = timerText(vars);
		g.drawString(this.font, timer, pRelX + pW - 12 - this.font.width(timer), pRelY + 24,
				timerColor(vars), false);
		g.drawString(this.font,
				secretRevealed ? "SECRET OBJECTIVES" : "AUTOMATIC OBJECTIVES",
				pRelX + 12, pRelY + 38,
				secretRevealed ? accent : ShopStyle.TEXT_SUB, false);

		for (int objective = 0; objective < 3; objective++) {
			int rowY = pRelY + ROW_START + objective * ROW_STEP;
			double progress = objectiveProgress(vars, objective);
			boolean objectiveComplete = objectiveComplete(vars, objective, progress,
					objectiveTarget(objective));

			g.drawString(this.font, objectiveName(objective), pRelX + 14, rowY + 7,
					objectiveComplete ? COMPLETE : ShopStyle.TEXT_MAIN, false);
			String progressText = objectiveProgressText(vars, objective, progress,
					objectiveComplete);
			g.drawString(this.font, progressText,
					pRelX + pW - 14 - this.font.width(progressText), rowY + 7,
					objectiveComplete ? COMPLETE : accent, false);
			drawLeftFitted(g, objectiveDescription(vars, objective),
					pRelX + 14, rowY + 22, pW - 28,
					secretRevealed ? 0xFFFFA8B2 : ShopStyle.TEXT_SUB);
		}

		String footer = !vars.ActiveDaily && complete
				? "Today's Daily Quest is complete."
				: !vars.ActiveDaily
						? "No active Daily Quest."
				: complete
						? "All objectives complete."
						: "Progress is tracked automatically.";
		drawCenteredFitted(g, footer, pRelY + 228,
				complete ? COMPLETE : !vars.ActiveDaily ? DANGER : ShopStyle.TEXT_MAIN);
		drawCenteredFitted(g, "Open this panel anytime to check your progress.",
				pRelY + 241, ShopStyle.TEXT_SUB);
	}

	@Override
	protected void renderExtras(GuiGraphics g, int mouseX, int mouseY) {
		int panelLeft = leftPos + pRelX;
		int panelTop = topPos + pRelY;

		for (int objective = 0; objective < 3; objective++) {
			int rowY = panelTop + ROW_START + objective * ROW_STEP;
			if (!isOver(mouseX, mouseY, panelLeft + 9, rowY, pW - 18, ROW_HEIGHT))
				continue;

			SystemTooltip.render(g, this.font, objectiveTooltip(objective),
					mouseX, mouseY, this.width, this.height);
			return;
		}
	}

	@Override
	protected void onCloseAnimationFinished() {
		if (returnToQuests) {
			returnToQuests = false;
			if (this.minecraft != null && this.minecraft.player != null
					&& this.minecraft.getConnection() != null)
				this.minecraft.setScreen(new SystemQuestsScreen());
		}
	}

	private void returnToQuestHub() {
		returnToQuests = true;
		beginClose();
	}

	private String questStatus(SololevelingModVariables.PlayerVariables vars, boolean complete) {
		if (!vars.ActiveDaily && complete)
			return "COMPLETED";
		if (!vars.ActiveDaily)
			return "INACTIVE";
		if (complete)
			return "ALL OBJECTIVES COMPLETE";
		if (DailyQuestHelper.isSecretQuestRevealed(entity))
			return "SECRET OBJECTIVES ACTIVE";
		return "QUEST IN PROGRESS";
	}

	private int statusColor(SololevelingModVariables.PlayerVariables vars, boolean complete) {
		if (complete)
			return COMPLETE;
		if (!vars.ActiveDaily)
			return DANGER;
		return DailyQuestHelper.isSecretQuestRevealed(entity)
				? SECRET_ACCENT
				: ShopStyle.ACCENT;
	}

	private boolean allObjectivesComplete(SololevelingModVariables.PlayerVariables vars) {
		return vars.dailyMinedBlocks >= objectiveTarget(0)
				&& (vars.dailyCombatWaived
						|| vars.dailyThreatPoints >= objectiveTarget(1))
				&& vars.RUN >= objectiveTarget(2);
	}

	private boolean isActiveSecretRevealed(
			SololevelingModVariables.PlayerVariables vars) {
		return vars.ActiveDaily && DailyQuestHelper.isSecretQuestRevealed(entity);
	}

	private static boolean objectiveComplete(SololevelingModVariables.PlayerVariables vars,
			int objective, double progress, double target) {
		return (objective == 1 && vars.dailyCombatWaived) || progress >= target;
	}

	private String timerText(SololevelingModVariables.PlayerVariables vars) {
		if (!vars.ActiveDaily || vars.dailytimer <= 0)
			return "--:--";
		long seconds = Math.max(0L, (long) Math.ceil(vars.dailytimer / 20.0));
		return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
	}

	private int timerColor(SololevelingModVariables.PlayerVariables vars) {
		if (!vars.ActiveDaily || vars.dailytimer <= 0)
			return DANGER;
		if (vars.dailytimer <= 1200)
			return DANGER;
		if (vars.dailytimer <= 6000)
			return WARNING;
		return ShopStyle.TEXT_MAIN;
	}

	private SololevelingModVariables.PlayerVariables variables() {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private double objectiveTarget(int objective) {
		boolean secret = DailyQuestHelper.isSecretQuest(entity);
		return switch (objective) {
			case 0 -> secret ? DailyQuestHelper.SECRET_MINING_TARGET
					: DailyQuestHelper.NORMAL_MINING_TARGET;
			case 1 -> secret ? DailyQuestHelper.SECRET_THREAT_TARGET
					: DailyQuestHelper.NORMAL_THREAT_TARGET;
			case 2 -> secret ? DailyQuestHelper.SECRET_RUN_TARGET
					: DailyQuestHelper.NORMAL_RUN_TARGET;
			default -> 1;
		};
	}

	private static double visibleTarget(int objective) {
		return switch (objective) {
			case 0 -> DailyQuestHelper.NORMAL_MINING_TARGET;
			case 1 -> DailyQuestHelper.NORMAL_THREAT_TARGET;
			case 2 -> DailyQuestHelper.NORMAL_RUN_TARGET;
			default -> 1;
		};
	}

	private static double objectiveProgress(SololevelingModVariables.PlayerVariables vars, int objective) {
		return switch (objective) {
			case 0 -> vars.dailyMinedBlocks;
			case 1 -> vars.dailyThreatPoints;
			case 2 -> vars.RUN;
			default -> 0;
		};
	}

	private static String objectiveName(int objective) {
		return switch (objective) {
			case 0 -> "MINING DRILL";
			case 1 -> "COMBAT READINESS";
			case 2 -> "ENDURANCE";
			default -> "";
		};
	}

	private static String objectiveDescription(
			SololevelingModVariables.PlayerVariables vars, int objective) {
		return switch (objective) {
			case 0 -> "Stone/ores, Nether stone, End Stone, mana deposits";
			case 1 -> vars.dailyCombatWaived
					? "Waived for this quest (Peaceful difficulty)"
					: "Defeat hostiles or your System Training Bot";
			case 2 -> "Travel on foot";
			default -> "";
		};
	}

	private static List<Component> objectiveTooltip(int objective) {
		return switch (objective) {
			case 0 -> List.of(
					Component.literal("Counts: natural stone, deepslate and ores."),
					Component.literal("Also: netherrack, blackstone, basalt and end stone."),
					Component.literal("Mana Crystal Deposits count. Use the correct tool."));
			case 1 -> List.of(
					Component.literal("Defeat hostile creatures to earn combat points."),
					Component.literal("Stronger enemies can be worth more points."),
					Component.literal("Your System Training Bot is worth 8 points."));
			case 2 -> List.of(
					Component.literal("Walking and sprinting are tracked automatically."),
					Component.literal("Vehicles, flight and teleportation do not count."));
			default -> List.of();
		};
	}

	private String objectiveProgressText(SololevelingModVariables.PlayerVariables vars,
			int objective, double progress, boolean complete) {
		if (objective == 1 && vars.dailyCombatWaived)
			return "WAIVED";
		if (complete)
			return "COMPLETE";

		double visibleTarget = visibleTarget(objective);
		boolean hiddenRemainder = isActiveSecretRevealed(vars)
				&& progress >= visibleTarget;
		if (objective == 2) {
			if (hiddenRemainder)
				return formatDistance(visibleTarget) + "+ / ??? KM";
			return formatDistance(progress) + " / " + formatDistance(visibleTarget) + " KM";
		}
		if (hiddenRemainder)
			return Math.round(visibleTarget) + "+ / ???";
		return Math.round(progress) + " / " + Math.round(visibleTarget);
	}

	private void drawCenteredFitted(GuiGraphics g, String text, int y, int color) {
		int maxWidth = pW - 24;
		String fitted = text;
		if (this.font.width(fitted) > maxWidth) {
			String suffix = "...";
			fitted = this.font.plainSubstrByWidth(fitted, maxWidth - this.font.width(suffix)) + suffix;
		}
		g.drawString(this.font, fitted, pRelX + (pW - this.font.width(fitted)) / 2, y, color, false);
	}

	private void drawLeftFitted(GuiGraphics g, String text, int x, int y,
			int maxWidth, int color) {
		String fitted = this.font.width(text) <= maxWidth
				? text
				: this.font.plainSubstrByWidth(text, maxWidth - this.font.width("...")) + "...";
		g.drawString(this.font, fitted, x, y, color, false);
	}

	private static String formatDistance(double blocks) {
		double distance = blocks / 50.0;
		if (Math.abs(distance - Math.rint(distance)) < 0.001)
			return Long.toString(Math.round(distance));
		return String.format(Locale.ROOT, "%.1f", distance);
	}

	private static void drawObjectiveRow(GuiGraphics g, int x, int y, int width,
			int height, int fill, int accentDim, int accentSoft) {
		g.fill(x, y, x + width, y + height, fill);
		g.fill(x, y, x + width, y + 1, accentDim);
		g.fill(x, y + height - 1, x + width, y + height, accentSoft);
		g.fill(x, y, x + 1, y + height, accentDim);
		g.fill(x + width - 1, y, x + width, y + height, accentDim);
	}

	private static void drawProgressBar(GuiGraphics g, int x, int y, int width, int height,
			double progress, double target, int color, int accentDim) {
		g.fill(x, y, x + width, y + height, 0xAA050A12);
		g.fill(x, y, x + width, y + 1, accentDim);
		if (target <= 0)
			return;
		int filled = Math.max(0, Math.min(width, (int) Math.round(width * progress / target)));
		if (filled > 0)
			g.fill(x, y + 1, x + filled, y + height - 1, color);
	}

	private static void drawSecretPanel(GuiGraphics g, int x, int y, int width,
			int height) {
		g.fill(x, y, x + width, y + height, SECRET_PANEL_FILL);
		g.fill(x - 1, y - 1, x + width + 1, y, SECRET_ACCENT_SOFT);
		g.fill(x - 1, y + height, x + width + 1, y + height + 1,
				SECRET_ACCENT_SOFT);
		g.fill(x - 1, y, x, y + height, SECRET_ACCENT_SOFT);
		g.fill(x + width, y, x + width + 1, y + height, SECRET_ACCENT_SOFT);
		g.fill(x, y, x + width, y + 1, SECRET_ACCENT_DIM);
		g.fill(x, y + height - 1, x + width, y + height, SECRET_ACCENT_DIM);
		g.fill(x, y, x + 1, y + height, SECRET_ACCENT_DIM);
		g.fill(x + width - 1, y, x + width, y + height, SECRET_ACCENT_DIM);
		drawSecretCorners(g, x, y, width, height);
	}

	private static void drawSecretCorners(GuiGraphics g, int x, int y,
			int width, int height) {
		int length = 10;
		g.fill(x - 1, y - 1, x + length, y + 1, SECRET_ACCENT);
		g.fill(x - 1, y - 1, x + 1, y + length, SECRET_ACCENT);
		g.fill(x + width - length, y - 1, x + width + 1, y + 1, SECRET_ACCENT);
		g.fill(x + width - 1, y - 1, x + width + 1, y + length, SECRET_ACCENT);
		g.fill(x - 1, y + height - 1, x + length, y + height + 1, SECRET_ACCENT);
		g.fill(x - 1, y + height - length, x + 1, y + height + 1, SECRET_ACCENT);
		g.fill(x + width - length, y + height - 1, x + width + 1,
				y + height + 1, SECRET_ACCENT);
		g.fill(x + width - 1, y + height - length, x + width + 1,
				y + height + 1, SECRET_ACCENT);
	}

	private static void drawSecretTitleBar(GuiGraphics g,
			net.minecraft.client.gui.Font font, int x, int y, int width) {
		String title = "SECRET QUEST";
		g.fill(x, y, x + width, y + 16, 0x772B0910);
		g.fill(x, y + 16, x + width, y + 17, SECRET_ACCENT);
		g.drawString(font, title, x + (width - font.width(title)) / 2,
				y + 4, SECRET_ACCENT, false);
	}

	private static boolean isOver(int mouseX, int mouseY, int x, int y, int width, int height) {
		return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
	}
}
