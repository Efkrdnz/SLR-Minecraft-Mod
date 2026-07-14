package net.solocraft.client.screens;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.ArmorBarProcedure;
import net.solocraft.procedures.FatigueTextProcedure;
import net.solocraft.procedures.Health0Procedure;
import net.solocraft.procedures.Health100Procedure;
import net.solocraft.procedures.Health10Procedure;
import net.solocraft.procedures.Health20Procedure;
import net.solocraft.procedures.Health30Procedure;
import net.solocraft.procedures.Health40Procedure;
import net.solocraft.procedures.Health50Procedure;
import net.solocraft.procedures.Health60Procedure;
import net.solocraft.procedures.Health70Procedure;
import net.solocraft.procedures.Health80Procedure;
import net.solocraft.procedures.Health90Procedure;
import net.solocraft.procedures.HealthTextProcedure;
import net.solocraft.procedures.HungerBarProcedure;
import net.solocraft.procedures.IfInSurvivalProcedure;
import net.solocraft.procedures.LevelBarProcedure;
import net.solocraft.procedures.Mana0Procedure;
import net.solocraft.procedures.Mana100Procedure;
import net.solocraft.procedures.Mana10Procedure;
import net.solocraft.procedures.Mana20Procedure;
import net.solocraft.procedures.Mana30Procedure;
import net.solocraft.procedures.Mana40Procedure;
import net.solocraft.procedures.Mana50Procedure;
import net.solocraft.procedures.Mana60Procedure;
import net.solocraft.procedures.Mana70Procedure;
import net.solocraft.procedures.Mana80Procedure;
import net.solocraft.procedures.Mana90Procedure;
import net.solocraft.procedures.ManaTextProcedure;
import net.solocraft.procedures.TitleTextProcedure;
import net.solocraft.util.SystemClientConfig;
import net.solocraft.util.TitleManager;

@Mod.EventBusSubscriber({Dist.CLIENT})
public class MPOverlayOverlay {
	private static final int PANEL_X = 8;
	private static final int PANEL_Y = 8;
	private static final int PANEL_W = 166;
	private static final int PANEL_H = 64;

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void eventHandler(RenderGuiEvent.Pre event) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (player == null || mc.options.hideGui || mc.options.renderDebug || mc.screen != null)
			return;
		if (!IfInSurvivalProcedure.execute(player))
			return;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.setShader(SystemClientConfig.isLegacyOverlayEnabled() ? GameRenderer::getPositionTexShader : GameRenderer::getPositionColorShader);
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		RenderSystem.setShaderColor(1, 1, 1, 1);

		if (SystemClientConfig.isLegacyOverlayEnabled()) {
			renderLegacyHud(event.getGuiGraphics(), event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), player);
		} else {
			renderSystemHud(event.getGuiGraphics(), mc, player);
		}

		RenderSystem.depthMask(true);
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
	}

	private static void renderSystemHud(GuiGraphics graphics, Minecraft mc, Player player) {
		SololevelingModVariables.PlayerVariables vars = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		Font font = mc.font;

		drawPanel(graphics, PANEL_X, PANEL_Y, PANEL_W, PANEL_H, 0xD5060B16, 0xDD13B8FF);
		drawScanLines(graphics, PANEL_X + 3, PANEL_Y + 3, PANEL_W - 6, PANEL_H - 6, 0x1613B8FF);
		drawEnergyNoise(graphics, PANEL_X + 5, PANEL_Y + 5, PANEL_W - 10, PANEL_H - 10, 0x3213B8FF);

		String job = clean(TitleTextProcedure.execute(player));
		String clazz = className((int) Math.round(vars.Classes));
		String title = TitleManager.displayName((int) Math.round(vars.title));
		if ("None".equals(title))
			title = "No Title";

		int metaY = PANEL_Y + 7;
		String identity = "No Job".equals(job) ? clazz : job;
		if (font.width(identity) > PANEL_W - 16)
			identity = clazz;
		graphics.drawString(font, Component.literal(identity), PANEL_X + 8, metaY, 0xFFEAF8FF, false);
		graphics.drawString(font, Component.literal(title), PANEL_X + 8, metaY + 10, 0xFFB98CFF, false);

		float hp = player.getHealth();
		float maxHp = Math.max(1.0F, player.getMaxHealth());
		float mp = (float) Math.max(0.0D, vars.MP);
		float maxMp = (float) Math.max(1.0D, vars.Mana);

		int barX = PANEL_X + 8;
		int barY = PANEL_Y + 31;
		int barW = PANEL_W - 16;
		drawLabeledBar(graphics, font, "HP", hp, maxHp, barX, barY, barW, 11, 0xFF381018, 0xFFFF3159, 0xFFFF8A9D);
		drawLabeledBar(graphics, font, "MP", mp, maxMp, barX, barY + 15, barW, 11, 0xFF071B2C, 0xFF1CCBFF, 0xFF9DEAFF);

		renderSurvivalChips(graphics, font, player, vars, PANEL_X + PANEL_W + 5, PANEL_Y);
	}

	private static void renderSurvivalChips(GuiGraphics graphics, Font font, Player player, SololevelingModVariables.PlayerVariables vars, int x, int y) {
		int armor = player.getArmorValue();
		int food = player.getFoodData().getFoodLevel();
		int fatigue = (int) Math.round(vars.Fatigue / 10.0D);

		drawChip(graphics, font, "DEF", String.valueOf(armor), x, y, 46, 20, 0xD4071018, 0xFF7AA8FF);
		drawChip(graphics, font, "FOOD", food + "/20", x, y + 22, 46, 20, 0xD4071018, 0xFFFFC96B);
		drawChip(graphics, font, "FTG", String.valueOf(fatigue), x, y + 44, 46, 20, 0xD4071018, 0xFFFF7CCB);
	}

	private static void drawLabeledBar(GuiGraphics graphics, Font font, String label, float value, float max, int x, int y, int width, int height, int trackColor, int fillColor, int hotColor) {
		float ratio = clamp(value / max, 0.0F, 1.0F);
		int fillW = Math.round(width * ratio);
		int color = ratio <= 0.22F && "HP".equals(label) ? pulseColor(fillColor, hotColor) : fillColor;

		graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xAA071C2A);
		graphics.fill(x, y, x + width, y + height, trackColor);
		if (fillW > 0) {
			graphics.fill(x, y, x + fillW, y + height, color);
			graphics.fill(x, y, x + fillW, y + 1, hotColor);
			for (int sx = x + 9; sx < x + fillW; sx += 12) {
				graphics.fill(sx, y + 1, sx + 1, y + height - 1, 0x44FFFFFF);
			}
		}

		String text = label + " " + Math.round(value) + "/" + Math.round(max);
		int textY = y + Math.max(0, (height - font.lineHeight) / 2);
		int textW = font.width(text);
		graphics.fill(x + 2, textY, x + textW + 7, textY + font.lineHeight, 0x66000000);
		if ("MP".equals(label)) {
			drawManaFlow(graphics, x, y, fillW, height);
		} else if ("HP".equals(label)) {
			drawHealthFluctuation(graphics, x, y, fillW, height);
		}
		graphics.drawString(font, Component.literal(text), x + 4, textY, 0xFFFFFFFF, true);
	}

	private static void drawChip(GuiGraphics graphics, Font font, String label, String value, int x, int y, int w, int h, int bg, int accent) {
		drawPanel(graphics, x, y, w, h, bg, accent);
		graphics.drawString(font, Component.literal(label), x + 4, y + 3, 0xFF77D9FF, false);
		graphics.drawString(font, Component.literal(value), x + 4, y + 11, 0xFFEAF8FF, false);
	}

	private static void drawPanel(GuiGraphics graphics, int x, int y, int w, int h, int bg, int accent) {
		graphics.fill(x, y, x + w, y + h, bg);
		graphics.fill(x, y, x + w, y + 1, accent);
		graphics.fill(x, y + h - 1, x + w, y + h, 0x99356F91);
		graphics.fill(x, y, x + 1, y + h, 0x99356F91);
		graphics.fill(x + w - 1, y, x + w, y + h, accent);
		graphics.fill(x + 3, y + 3, x + 13, y + 4, 0x9913B8FF);
		graphics.fill(x + 3, y + 3, x + 4, y + 13, 0x9913B8FF);
		graphics.fill(x + w - 13, y + h - 4, x + w - 3, y + h - 3, 0x9913B8FF);
		graphics.fill(x + w - 4, y + h - 13, x + w - 3, y + h - 3, 0x9913B8FF);
	}

	private static void renderLegacyHud(GuiGraphics graphics, int w, int h, Player entity) {
		ResourceLocation empty = new ResourceLocation("sololeveling:textures/screens/bar1.png");
		int barX = w / 2 - 85;
		int manaY = h - 48;
		int healthY = h - 35;
		graphics.blit(manaTexture(entity, empty), barX, manaY, 0, 0, 90, 10, 90, 10);
		graphics.blit(healthTexture(entity, empty), barX, healthY, 0, 0, 90, 10, 90, 10);
		graphics.blit(new ResourceLocation("sololeveling:textures/screens/armorbar.png"), w / 2 + 6, h - 36, 0, 0, 12, 12, 12, 12);
		graphics.blit(new ResourceLocation("sololeveling:textures/screens/hungerbar.png"), w / 2 + 6, h - 49, 0, 0, 12, 12, 12, 12);
		graphics.blit(new ResourceLocation("sololeveling:textures/screens/levelbar.png"), w / 2 + 44, h - 49, 0, 0, 12, 12, 12, 12);
		graphics.blit(new ResourceLocation("sololeveling:textures/screens/fatiguebar.png"), w / 2 + 42, h - 38, 0, 0, 16, 16, 16, 16);

		Font font = Minecraft.getInstance().font;
		graphics.drawString(font, ManaTextProcedure.execute(entity), w / 2 - 76, h - 47, -1, false);
		graphics.drawString(font, HealthTextProcedure.execute(entity), w / 2 - 76, h - 34, -1, false);
		graphics.drawString(font, ArmorBarProcedure.execute(entity), w / 2 + 19, h - 35, -1, false);
		graphics.drawString(font, HungerBarProcedure.execute(entity), w / 2 + 19, h - 47, -1, false);
		graphics.drawString(font, LevelBarProcedure.execute(entity), w / 2 + 55, h - 47, -1, false);
		graphics.drawString(font, FatigueTextProcedure.execute(entity), w / 2 + 55, h - 34, -1, false);
	}

	private static ResourceLocation manaTexture(Player entity, ResourceLocation empty) {
		if (Mana100Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana100.png");
		if (Mana90Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana90.png");
		if (Mana80Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana80.png");
		if (Mana70Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana70.png");
		if (Mana60Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana60.png");
		if (Mana50Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana50.png");
		if (Mana40Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana40.png");
		if (Mana30Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana30.png");
		if (Mana20Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana20.png");
		if (Mana10Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barmana10.png");
		return Mana0Procedure.execute(entity) ? empty : empty;
	}

	private static ResourceLocation healthTexture(Player entity, ResourceLocation empty) {
		if (Health100Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth100.png");
		if (Health90Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth90.png");
		if (Health80Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth80.png");
		if (Health70Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth70.png");
		if (Health60Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth60.png");
		if (Health50Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth50.png");
		if (Health40Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth40.png");
		if (Health30Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth30.png");
		if (Health20Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth20.png");
		if (Health10Procedure.execute(entity))
			return new ResourceLocation("sololeveling:textures/screens/barhealth10.png");
		return Health0Procedure.execute(entity) ? empty : empty;
	}

	private static void drawScanLines(GuiGraphics graphics, int x, int y, int w, int h, int color) {
		for (int line = y + 3; line < y + h; line += 5) {
			graphics.fill(x, line, x + w, line + 1, color);
		}
	}

	private static void drawEnergyNoise(GuiGraphics graphics, int x, int y, int w, int h, int color) {
		long tick = gameTick();
		for (int i = 0; i < 6; i++) {
			int yy = y + (int) ((tick * (i + 2) + i * 11) % Math.max(1, h));
			int xx = x + 6 + i * 23;
			graphics.fill(xx, yy, Math.min(x + w, xx + 16), yy + 1, color);
		}
	}

	private static void drawManaFlow(GuiGraphics graphics, int x, int y, int fillW, int height) {
		if (fillW <= 3)
			return;
		int offset = (int) (gameTick() % 16);
		for (int sx = x - 16 + offset; sx < x + fillW; sx += 16) {
			int start = Math.max(x, sx);
			int end = Math.min(x + fillW, sx + 6);
			if (end > start)
				graphics.fill(start, y + 2, end, y + height - 2, 0x52BDF5FF);
		}
	}

	private static void drawHealthFluctuation(GuiGraphics graphics, int x, int y, int fillW, int height) {
		if (fillW <= 4)
			return;
		long tick = gameTick();
		for (int i = 0; i < 5; i++) {
			int sx = x + (int) ((tick * 3 + i * 27) % Math.max(1, fillW));
			int pulseH = 2 + (int) ((tick + i) % Math.max(1, height - 1));
			graphics.fill(sx, y + Math.max(1, height - pulseH), Math.min(x + fillW, sx + 1), y + height - 1, 0x66FFFFFF);
		}
	}

	private static long gameTick() {
		return Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
	}

	private static int pulseColor(int base, int hot) {
		long tick = gameTick();
		return (tick / 8L) % 2L == 0L ? hot : base;
	}

	private static String clean(String text) {
		if (text == null || text.isBlank() || "none".equalsIgnoreCase(text))
			return "No Job";
		return text.replaceAll("\\u00A7.", "");
	}

	private static String className(int cls) {
		return switch (cls) {
			case 1 -> "Assassin";
			case 2 -> "Combat Mage";
			case 3 -> "Fighter";
			case 4 -> "Tanker";
			case 5 -> "Support Mage";
			case 6 -> "Ranger";
			default -> "No Class";
		};
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}
}
