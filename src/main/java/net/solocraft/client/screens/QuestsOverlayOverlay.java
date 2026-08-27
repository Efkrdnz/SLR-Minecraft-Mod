
package net.solocraft.client.screens;

import net.solocraft.procedures.ReturnQuestNameProcedure;
import net.solocraft.procedures.QuestLinesProcedure;
import net.solocraft.procedures.QuestInfoGetProcedure;
import net.solocraft.procedures.DungeoningProcedure;
import net.solocraft.client.gui.DkcQuestProgressClientState;
import net.solocraft.client.gui.UrgentQuestClientState;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@EventBusSubscriber({Dist.CLIENT})
public class QuestsOverlayOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		int w = event.getGuiGraphics().guiWidth();
		int h = event.getGuiGraphics().guiHeight();
		Player entity = Minecraft.getInstance().player;
		boolean visible = QuestInfoGetProcedure.execute(entity);
		if (!visible)
			return;
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		if (visible) {
			net.solocraft.client.gui.OverlayTransform.push(event.getGuiGraphics(),
					net.solocraft.util.OverlayLayoutConfig.QUESTS);
			if (DkcQuestProgressClientState.isActive(entity)) {
				renderDkcQuest(event.getGuiGraphics(), w, h);
			} else if (UrgentQuestClientState.isActive()) {
				renderUrgentQuest(event.getGuiGraphics(), w);
			} else {
				renderStoryQuest(event.getGuiGraphics(), entity, w);
			}
			net.solocraft.client.gui.OverlayTransform.pop(event.getGuiGraphics());
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private static void renderDkcQuest(GuiGraphics graphics, int screenWidth, int screenHeight) {
		Font font = Minecraft.getInstance().font;
		int x = 8;
		int width = Math.max(120, Math.min(230, screenWidth - 16));
		int accent = dkcAccent(DkcQuestProgressClientState.phase());
		List<net.minecraft.util.FormattedCharSequence> objectiveLines = font.split(
				Component.literal(DkcQuestProgressClientState.objective()), width - 12);
		List<net.minecraft.util.FormattedCharSequence> detailLines = DkcQuestProgressClientState.detail().isBlank()
				? List.of() : font.split(Component.literal(DkcQuestProgressClientState.detail()), width - 12);
		boolean objectiveProgress = DkcQuestProgressClientState.target() > 0;
		boolean urgent = UrgentQuestClientState.isActive();
		List<net.minecraft.util.FormattedCharSequence> urgentLines = urgent
				? font.split(Component.literal(UrgentQuestClientState.objective()), width - 12) : List.of();
		int urgentLineCount = Math.min(2, urgentLines.size());

		int height = 34 + objectiveLines.size() * 10 + detailLines.size() * 10
				+ (objectiveProgress ? 17 : 0);
		if (urgent)
			height += 25 + urgentLineCount * 10;
		int y = Math.min(134, Math.max(8, screenHeight - height - 8));

		drawSystemPanel(graphics, x, y, width, height, 0xD2080A13, accent);
		Component heading = Component.literal("DEMON KING'S CASTLE")
				.withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
		graphics.drawString(font, heading, x + 6, y + 5, 0xFFFFFFFF, false);
		String overall = DkcQuestProgressClientState.cleared() + "/" + 20;
		graphics.drawString(font, overall, x + width - 6 - font.width(overall), y + 5, 0xFFFFC766, false);

		String floorLine = "FLOOR " + DkcQuestProgressClientState.floor() + " - "
				+ DkcQuestProgressClientState.floorName().toUpperCase(java.util.Locale.ROOT);
		if (font.width(floorLine) > width - 12)
			floorLine = font.plainSubstrByWidth(floorLine, width - 24) + "...";
		graphics.drawString(font, floorLine, x + 6, y + 17, accent, false);
		int cursor = y + 29;
		for (net.minecraft.util.FormattedCharSequence line : objectiveLines) {
			graphics.drawString(font, line, x + 6, cursor, 0xFFF4F7FF, false);
			cursor += 10;
		}
		for (net.minecraft.util.FormattedCharSequence line : detailLines) {
			graphics.drawString(font, line, x + 6, cursor, 0xFFADB8C7, false);
			cursor += 10;
		}

		if (objectiveProgress) {
			int progress = Math.min(DkcQuestProgressClientState.progress(), DkcQuestProgressClientState.target());
			String progressText = "Objective: " + progress + "/" + DkcQuestProgressClientState.target();
			graphics.drawString(font, progressText, x + 6, cursor, 0xFFEAF8FF, false);
			cursor += 10;
			int barWidth = width - 12;
			int filled = (int) Math.round(barWidth * (progress / (double) DkcQuestProgressClientState.target()));
			graphics.fill(x + 6, cursor + 1, x + 6 + barWidth, cursor + 5, 0xAA1A2230);
			if (filled > 0)
				graphics.fill(x + 6, cursor + 1, x + 6 + filled, cursor + 5, accent);
			cursor += 7;
		}

		if (urgent) {
			graphics.fill(x + 6, cursor + 1, x + width - 6, cursor + 2, 0xAA7A1F2A);
			cursor += 6;
			Component urgentHeading = Component.literal("URGENT: ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
					.append(Component.literal(UrgentQuestClientState.title()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
			List<net.minecraft.util.FormattedCharSequence> headingLines = font.split(urgentHeading, width - 12);
			if (!headingLines.isEmpty())
				graphics.drawString(font, headingLines.get(0), x + 6, cursor, 0xFFFF6A6A, false);
			cursor += 10;
			for (int index = 0; index < urgentLineCount; index++) {
				graphics.drawString(font, urgentLines.get(index), x + 6, cursor, 0xFFD8C8D0, false);
				cursor += 10;
			}
			String urgentStatus = urgentStatus();
			graphics.drawString(font, urgentStatus, x + 6, cursor, 0xFFFFC766, false);
		}
	}

	private static String urgentStatus() {
		String status = switch (UrgentQuestClientState.kind()) {
			case "kill", "pvp", "kang" -> "Progress " + UrgentQuestClientState.progress() + "/" + UrgentQuestClientState.target();
			case "no_skills" -> "No skills used";
			default -> "Active";
		};
		int remaining = UrgentQuestClientState.remainingSeconds();
		return remaining < 0 ? status : status + String.format("  %02d:%02d", remaining / 60, remaining % 60);
	}

	private static int dkcAccent(String phase) {
		return switch (phase) {
			case "boss" -> 0xFFFF4D4D;
			case "radiru", "sanctuary" -> 0xFFD96CFF;
			case "ascent" -> 0xFF4FD7FF;
			case "permit" -> 0xFFFFC766;
			case "conquered" -> 0xFFFFB83D;
			default -> 0xFFFF7A45;
		};
	}

	private static void renderUrgentQuest(GuiGraphics graphics, int screenWidth) {
		Font font = Minecraft.getInstance().font;
		int x = 8;
		int y = 134;
		int width = Math.min(220, Math.max(128, screenWidth - 12));
		List<net.minecraft.util.FormattedCharSequence> objectiveLines = font.split(Component.literal(UrgentQuestClientState.objective()), width - 12);
		int height = 46 + objectiveLines.size() * 10;

		graphics.fill(x, y, x + width, y + height, 0xD20A0710);
		graphics.fill(x, y, x + width, y + 1, 0xFFFF3D3D);
		graphics.fill(x, y + height - 1, x + width, y + height, 0xFF7A1F2A);
		graphics.fill(x, y, x + 1, y + height, 0xFF7A1F2A);
		graphics.fill(x + width - 1, y, x + width, y + height, 0xFF7A1F2A);

		Component heading = Component.literal("URGENT QUEST: ").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
				.append(Component.literal(UrgentQuestClientState.title()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
		graphics.drawString(font, heading, x + 6, y + 5, 0xFFFFFFFF, false);
		int lineY = y + 17;
		for (net.minecraft.util.FormattedCharSequence line : objectiveLines) {
			graphics.drawString(font, line, x + 6, lineY, 0xFFD8C8D0, false);
			lineY += 10;
		}

		String status = switch (UrgentQuestClientState.kind()) {
			case "kill", "pvp", "kang" -> "Progress: " + UrgentQuestClientState.progress() + "/" + UrgentQuestClientState.target();
			case "no_skills" -> "Status: Active - No skills used";
			default -> "Status: Active";
		};
		int remaining = UrgentQuestClientState.remainingSeconds();
		String timer = remaining < 0 ? "Time: No limit" : String.format("Time: %02d:%02d", remaining / 60, remaining % 60);
		graphics.drawString(font, status, x + 6, y + height - 25, 0xFFFF8A8A, false);
		graphics.drawString(font, timer, x + 6, y + height - 15, 0xFFFFC766, false);
	}

	private static void renderStoryQuest(GuiGraphics graphics, Player entity, int screenWidth) {
		Font font = Minecraft.getInstance().font;
		int x = 8;
		int y = 134;
		int width = Math.min(220, Math.max(150, screenWidth - 16));
		boolean dungeon = DungeoningProcedure.execute(entity);
		Component title = Component.literal(ReturnQuestNameProcedure.execute(entity));
		Component body = Component.literal(QuestLinesProcedure.execute(entity));
		List<net.minecraft.util.FormattedCharSequence> bodyLines = font.split(body, width - 12);
		int height = 28 + bodyLines.size() * 10 + (dungeon ? 12 : 0);

		drawSystemPanel(graphics, x, y, width, height, dungeon ? 0xD2081018 : 0xCC060B16, dungeon ? 0xFFFFC766 : 0xDD13B8FF);
		graphics.drawString(font, title, x + 6, y + 5, dungeon ? 0xFFFFC766 : 0xFF9DEAFF, false);
		int lineY = y + 17;
		for (net.minecraft.util.FormattedCharSequence line : bodyLines) {
			graphics.drawString(font, line, x + 6, lineY, 0xFFEAF8FF, false);
			lineY += 10;
		}
		if (dungeon) {
			graphics.drawString(font, Component.translatable("gui.sololeveling.quests_overlay.label_clear_the_dungeon"), x + 6, y + height - 11, 0xFFFF8A8A, false);
		}
	}

	private static void drawSystemPanel(GuiGraphics graphics, int x, int y, int width, int height, int bg, int accent) {
		graphics.fill(x, y, x + width, y + height, bg);
		graphics.fill(x, y, x + width, y + 1, accent);
		graphics.fill(x, y + height - 1, x + width, y + height, 0x99356F91);
		graphics.fill(x, y, x + 1, y + height, 0x99356F91);
		graphics.fill(x + width - 1, y, x + width, y + height, accent);
		graphics.fill(x + 3, y + 3, x + 14, y + 4, 0x9913B8FF);
		graphics.fill(x + 3, y + 3, x + 4, y + 14, 0x9913B8FF);
	}
}
