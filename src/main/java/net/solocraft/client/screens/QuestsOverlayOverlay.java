
package net.solocraft.client.screens;

import org.checkerframework.checker.units.qual.h;

import net.solocraft.procedures.ReturnQuestNameProcedure;
import net.solocraft.procedures.QuestLinesProcedure;
import net.solocraft.procedures.QuestInfoGetProcedure;
import net.solocraft.procedures.DungeoningProcedure;
import net.solocraft.client.gui.UrgentQuestClientState;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.ChatFormatting;

import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.platform.GlStateManager;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class QuestsOverlayOverlay {
	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		int w = event.getWindow().getGuiScaledWidth();
		int h = event.getWindow().getGuiScaledHeight();
		Level world = null;
		double x = 0;
		double y = 0;
		double z = 0;
		Player entity = Minecraft.getInstance().player;
		if (entity != null) {
			world = entity.level();
			x = entity.getX();
			y = entity.getY();
			z = entity.getZ();
		}
		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, 1);
		if (QuestInfoGetProcedure.execute(entity)) {
			if (UrgentQuestClientState.isActive()) {
				renderUrgentQuest(event.getGuiGraphics(), w);
			} else {
				renderStoryQuest(event.getGuiGraphics(), entity, w);
			}
		}
		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
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
