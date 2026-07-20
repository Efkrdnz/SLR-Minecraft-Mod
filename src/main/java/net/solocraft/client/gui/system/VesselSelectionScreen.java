package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.shader.VesselSelectionBackgroundRenderTypes;
import net.solocraft.network.VesselSelectionMessage;
import net.solocraft.util.VesselManager;
import net.solocraft.util.VesselManager.VesselDefinition;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/** Mandatory choice shown after the Job Change advancement trial. */
public final class VesselSelectionScreen extends SystemScreen {
	private static final int ROW_Y = 70;
	private static final int ROW_STEP = 34;
	private static final int ROW_W = 141;
	private static final int ROW_H = 29;

	private static final int THEME_SYSTEM = 0;
	private static final int THEME_RULER = 1;
	private static final int THEME_SHADOW = 2;
	private static final int THEME_FROST = 3;
	private static final int THEME_WHITE_FLAME = 4;
	private static final int THEME_BEAST = 5;
	private static final int THEME_MONARCH = 6;
	private static final int THEME_COUNT = 7;
	private static final float THEME_RESPONSE_SECONDS = 0.32F;

	private static final int[] THEME_ACCENTS = {
			0xFF3FC6FF, 0xFFFFC84A, 0xFFB664FF, 0xFFD6F7FF, 0xFFB9EAFF, 0xFFFF4938, 0xFFC684FF
	};
	private static final int[] THEME_TEXT = {
			0xFFE8F6FF, 0xFFFFF4CF, 0xFFF2E4FF, 0xFFF5FDFF, 0xFFF0FBFF, 0xFFFFE9E6, 0xFFF2E6FF
	};
	private static final int[] THEME_SUBTEXT = {
			0xFF8FB8D8, 0xFFE1BD6A, 0xFFC8A6E7, 0xFFAED3DF, 0xFF9DC5DE, 0xFFD79A92, 0xFFBE9DD8
	};
	private static final int[] FALLBACK_TOP = {
			0xFF07132B, 0xFF2D1A04, 0xFF21052F, 0xFF0A3A5B, 0xFF08244C, 0xFF340605, 0xFF260632
	};
	private static final int[] FALLBACK_BOTTOM = {
			0xFF01030A, 0xFF080300, 0xFF030006, 0xFF010B18, 0xFF010611, 0xFF080101, 0xFF040007
	};

	private final List<VesselButton> vesselButtons = new ArrayList<>();
	private final float[] themeWeights = new float[THEME_COUNT];
	private long lastThemeUpdate;
	private int advancementPoints;
	private int requiredPoints;
	private int vesselLimit;
	private int[] claimCounts;
	private int selectingIndex = -1;

	public VesselSelectionScreen(int advancementPoints, int requiredPoints, int vesselLimit, int[] claimCounts) {
		super(Component.literal("SELECT YOUR VESSEL"));
		this.panelW = 312;
		this.panelH = 292;
		this.themeWeights[THEME_SYSTEM] = 1.0F;
		updateState(advancementPoints, requiredPoints, vesselLimit, claimCounts);
	}

	public static void handleServerState(boolean open, int advancementPoints, int requiredPoints, int vesselLimit, int[] claimCounts) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!open) {
			if (minecraft.screen instanceof VesselSelectionScreen) {
				SystemGuiSounds.exit();
				minecraft.setScreen(null);
			}
			return;
		}
		if (minecraft.screen instanceof VesselSelectionScreen screen) {
			screen.updateState(advancementPoints, requiredPoints, vesselLimit, claimCounts);
		} else {
			minecraft.setScreen(new VesselSelectionScreen(advancementPoints, requiredPoints, vesselLimit, claimCounts));
		}
	}

	@Override
	protected void init() {
		super.init();
		vesselButtons.clear();
		int rulerRow = 0;
		int monarchRow = 0;
		List<VesselDefinition> definitions = VesselManager.definitions();
		for (int i = 0; i < definitions.size(); i++) {
			VesselDefinition definition = definitions.get(i);
			boolean rulerColumn = isRulerColumn(definition);
			int row = rulerColumn ? rulerRow++ : monarchRow++;
			int x = panelX + (rulerColumn ? 10 : 161);
			int y = panelY + ROW_Y + row * ROW_STEP;
			VesselButton button = new VesselButton(this, i, definition, x, y, ROW_W, ROW_H);
			vesselButtons.add(button);
			addRenderableWidget(button);
		}
		lastThemeUpdate = Util.getMillis();
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (key == 256)
			return true;
		return super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	protected void beginClose() {
		// The server closes this screen only after it accepts a valid vessel.
	}

	@Override
	public void onClose() {
		// Reconnect/tick recovery also reopens the choice if another mod closes it.
	}

	@Override
	protected void renderAnimatedBackground(GuiGraphics graphics, int mouseX, int mouseY) {
		updateTheme(findHoveredTheme(mouseX, mouseY));
		float localX = clamp01((mouseX - panelX) / (float) panelW);
		float localY = clamp01((mouseY - panelY) / (float) panelH);
		ShaderInstance shader = VesselSelectionBackgroundRenderTypes.get();
		if (shader == null) {
			renderFallbackBackground(graphics);
			return;
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(VesselSelectionBackgroundRenderTypes::get);
		AbstractUniform mouse = shader.safeGetUniform("MousePos");
		mouse.set(localX, localY);
		shader.safeGetUniform("ThemeWeights0").set(themeWeights[0], themeWeights[1], themeWeights[2], themeWeights[3]);
		shader.safeGetUniform("ThemeWeights1").set(themeWeights[4], themeWeights[5], themeWeights[6], 0.0F);

		int x0 = panelX;
		int y0 = panelY;
		int x1 = panelX + panelW;
		int y1 = panelY + panelH;
		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, x0, y1, 0).uv(0.0F, 1.0F).endVertex();
		buffer.vertex(matrix, x1, y1, 0).uv(1.0F, 1.0F).endVertex();
		buffer.vertex(matrix, x1, y0, 0).uv(1.0F, 0.0F).endVertex();
		buffer.vertex(matrix, x0, y0, 0).uv(0.0F, 0.0F).endVertex();
		Tesselator.getInstance().end();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@Override
	protected void renderFrame(GuiGraphics graphics) {
		int accent = weightedColor(THEME_ACCENTS);
		int x = panelX;
		int y = panelY;
		int w = panelW;
		int h = panelH;
		int soft = withAlpha(accent, 0x56);
		int dim = mixRgb(0xFF07101A, accent, 0.62F);

		graphics.fill(x - 1, y - 1, x + w + 1, y, soft);
		graphics.fill(x - 1, y + h, x + w + 1, y + h + 1, soft);
		graphics.fill(x - 1, y, x, y + h, soft);
		graphics.fill(x + w, y, x + w + 1, y + h, soft);
		drawOutline(graphics, x, y, w, h, dim);
		graphics.fill(x, y, x + w, y + 18, withAlpha(mixRgb(0xFF020308, accent, 0.13F), 0xDD));
		graphics.fill(x, y + 18, x + w, y + 19, accent);
		drawCorners(graphics, x, y, w, h, accent);

		Font font = Minecraft.getInstance().font;
		String titleText = "[ " + this.title.getString() + " ]";
		graphics.drawString(font, titleText, x + (w - font.width(titleText)) / 2, y + 5, weightedColor(THEME_TEXT), false);
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int accent = weightedColor(THEME_ACCENTS);
		int mainText = weightedColor(THEME_TEXT);
		int subText = weightedColor(THEME_SUBTEXT);
		graphics.fill(panelX + 5, panelY + 22, panelX + panelW - 5, panelY + 68, 0x72000208);

		String instruction = "The choice is permanent. Select one vessel.";
		graphics.drawCenteredString(this.font, instruction, panelX + panelW / 2, panelY + 27, mainText);
		String progress = "ADVANCEMENT POINTS  " + advancementPoints + "/" + requiredPoints;
		graphics.drawCenteredString(this.font, Component.literal(progress).withStyle(ChatFormatting.BOLD), panelX + panelW / 2, panelY + 40,
				mixRgb(accent, 0xFFFFFFFF, 0.32F));
		graphics.fill(panelX + 10, panelY + 53, panelX + panelW - 10, panelY + 54, withAlpha(accent, 0x8A));
		graphics.fill(panelX + 155, panelY + 55, panelX + 156, panelY + 241, withAlpha(accent, 0x36));
		graphics.drawString(this.font, Component.literal("RULER VESSELS").withStyle(ChatFormatting.BOLD), panelX + 10, panelY + 58,
				mixRgb(0xFFFFC84A, mainText, themeWeights[THEME_SYSTEM] * 0.34F), false);
		graphics.drawString(this.font, Component.literal("MONARCH VESSELS").withStyle(ChatFormatting.BOLD), panelX + 161, panelY + 58,
				mixRgb(0xFFC684FF, mainText, themeWeights[THEME_SYSTEM] * 0.25F), false);

		boolean anyAvailable = false;
		for (int i = 0; i < VesselManager.definitions().size(); i++)
			anyAvailable |= isAvailable(i);
		graphics.fill(panelX + 5, panelY + panelH - 28, panelX + panelW - 5, panelY + panelH - 4, 0x82000105);
		if (anyAvailable) {
			graphics.drawCenteredString(this.font, "A claimed vessel cannot be selected.", panelX + panelW / 2, panelY + panelH - 18, subText);
		} else {
			graphics.drawCenteredString(this.font, "All vessels are claimed.", panelX + panelW / 2, panelY + panelH - 24, 0xFFFF6868);
			graphics.drawCenteredString(this.font, "Increase /gamerule soloLevelingMonarchLimit.", panelX + panelW / 2, panelY + panelH - 13, 0xFFFFA0A0);
		}
	}

	@Override
	protected List<Component> getHoverTooltip(int mouseX, int mouseY) {
		for (VesselButton button : vesselButtons) {
			if (!button.contains(mouseX, mouseY))
				continue;
			VesselDefinition definition = button.definition;
			List<Component> tooltip = new ArrayList<>();
			tooltip.add(Component.literal(displayName(definition)).withStyle(themeFormatting(definition), ChatFormatting.BOLD));
			tooltip.add(Component.literal(displayPower(definition)).withStyle(ChatFormatting.WHITE));
			tooltip.add(Component.literal(definition.description()).withStyle(ChatFormatting.GRAY));
			int count = claimCount(button.index);
			String capacity = vesselLimit <= 0 ? "Unlimited vessels" : count + "/" + vesselLimit + " claimed";
			tooltip.add(Component.literal(capacity).withStyle(isAvailable(button.index) ? ChatFormatting.GREEN : ChatFormatting.RED));
			return tooltip;
		}
		return null;
	}

	private void choose(int index, VesselDefinition definition) {
		if (!isAvailable(index) || selectingIndex >= 0)
			return;
		selectingIndex = index;
		SololevelingMod.PACKET_HANDLER.sendToServer(new VesselSelectionMessage(definition.type(), definition.identity()));
	}

	private void updateState(int advancementPoints, int requiredPoints, int vesselLimit, int[] claimCounts) {
		this.advancementPoints = Math.max(0, advancementPoints);
		this.requiredPoints = Math.max(1, requiredPoints);
		this.vesselLimit = vesselLimit;
		this.claimCounts = claimCounts == null ? new int[0] : claimCounts.clone();
		this.selectingIndex = -1;
	}

	private int claimCount(int index) {
		return index >= 0 && index < claimCounts.length ? claimCounts[index] : 0;
	}

	private boolean isAvailable(int index) {
		return vesselLimit <= 0 || claimCount(index) < vesselLimit;
	}

	private int findHoveredTheme(int mouseX, int mouseY) {
		for (VesselButton button : vesselButtons) {
			if (button.visible && button.contains(mouseX, mouseY))
				return themeFor(button.definition);
		}
		return THEME_SYSTEM;
	}

	private void updateTheme(int targetTheme) {
		long now = Util.getMillis();
		float elapsed = Math.min(0.1F, Math.max(0.0F, (now - lastThemeUpdate) / 1000.0F));
		lastThemeUpdate = now;
		float follow = 1.0F - (float) Math.exp(-elapsed / THEME_RESPONSE_SECONDS);
		for (int i = 0; i < THEME_COUNT; i++) {
			float target = i == targetTheme ? 1.0F : 0.0F;
			themeWeights[i] += (target - themeWeights[i]) * follow;
		}
		normalizeThemeWeights();
	}

	private void normalizeThemeWeights() {
		float total = 0.0F;
		for (float weight : themeWeights)
			total += weight;
		if (total <= 0.0001F) {
			themeWeights[THEME_SYSTEM] = 1.0F;
			return;
		}
		for (int i = 0; i < themeWeights.length; i++)
			themeWeights[i] /= total;
	}

	private void renderFallbackBackground(GuiGraphics graphics) {
		int top = weightedColor(FALLBACK_TOP);
		int bottom = weightedColor(FALLBACK_BOTTOM);
		int accent = weightedColor(THEME_ACCENTS);
		graphics.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, top, bottom);
		float time = (Util.getMillis() % 100000L) / 1000.0F;
		for (int i = 0; i < 36; i++) {
			float seed = i * 19.731F;
			float fx = frac((float) Math.sin(seed) * 43758.545F);
			float fy = frac((float) Math.sin(seed * 1.79F) * 24634.634F);
			int x = panelX + (int) (fx * panelW);
			int y = panelY + (int) ((fy * panelH + time * (5.0F + fx * 13.0F)) % panelH);
			graphics.fill(x, y, x + 1, y + 1, withAlpha(accent, 0x74));
		}
		graphics.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, 0x08000000, 0x76000000);
	}

	private int weightedColor(int[] palette) {
		float red = 0.0F;
		float green = 0.0F;
		float blue = 0.0F;
		for (int i = 0; i < THEME_COUNT; i++) {
			red += ((palette[i] >> 16) & 0xFF) * themeWeights[i];
			green += ((palette[i] >> 8) & 0xFF) * themeWeights[i];
			blue += (palette[i] & 0xFF) * themeWeights[i];
		}
		return 0xFF000000 | (Math.round(red) << 16) | (Math.round(green) << 8) | Math.round(blue);
	}

	private static int themeFor(VesselDefinition definition) {
		if (isRulerColumn(definition))
			return THEME_RULER;
		return switch (definition.identity()) {
			case "ashborn" -> THEME_SHADOW;
			case "sillad" -> THEME_FROST;
			case "baran" -> THEME_WHITE_FLAME;
			case "rakan" -> THEME_BEAST;
			default -> THEME_MONARCH;
		};
	}

	private static boolean isRulerColumn(VesselDefinition definition) {
		return VesselManager.RULER.equals(definition.type()) && !"ashborn".equals(definition.identity());
	}

	private static String displayName(VesselDefinition definition) {
		return switch (definition.identity()) {
			case "ashborn" -> "Ashborn";
			case "go_gunhee" -> "Brightest Fragment";
			case "liu_zhigang" -> "Sharpest Fragment";
			case "thomas_andre" -> "Adamant Fragment";
			case "christopher_reed" -> "Blazing Fragment";
			case "sung_il_hwan" -> "Silent Fragment";
			default -> definition.name();
		};
	}

	private static String displayPower(VesselDefinition definition) {
		if ("ashborn".equals(definition.identity()))
			return "Monarch of Shadows";
		return isRulerColumn(definition) ? definition.name() : definition.powerName();
	}

	private static ChatFormatting themeFormatting(VesselDefinition definition) {
		return switch (themeFor(definition)) {
			case THEME_RULER -> ChatFormatting.GOLD;
			case THEME_FROST -> ChatFormatting.AQUA;
			case THEME_WHITE_FLAME -> ChatFormatting.BLUE;
			case THEME_BEAST -> ChatFormatting.RED;
			default -> ChatFormatting.LIGHT_PURPLE;
		};
	}

	private static int withAlpha(int color, int alpha) {
		return (alpha << 24) | (color & 0x00FFFFFF);
	}

	private static int mixRgb(int from, int to, float amount) {
		float value = clamp01(amount);
		int red = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * value);
		int green = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * value);
		int blue = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * value);
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	private static float frac(float value) {
		return value - (float) Math.floor(value);
	}

	private static float clamp01(float value) {
		return Math.max(0.0F, Math.min(1.0F, value));
	}

	private static void drawOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	private static void drawCorners(GuiGraphics graphics, int x, int y, int width, int height, int color) {
		int length = 12;
		graphics.fill(x - 1, y - 1, x + length, y + 1, color);
		graphics.fill(x - 1, y - 1, x + 1, y + length, color);
		graphics.fill(x + width - length, y - 1, x + width + 1, y + 1, color);
		graphics.fill(x + width - 1, y - 1, x + width + 1, y + length, color);
		graphics.fill(x - 1, y + height - 1, x + length, y + height + 1, color);
		graphics.fill(x - 1, y + height - length, x + 1, y + height + 1, color);
		graphics.fill(x + width - length, y + height - 1, x + width + 1, y + height + 1, color);
		graphics.fill(x + width - 1, y + height - length, x + width + 1, y + height + 1, color);
	}

	private static final class VesselButton extends Button {
		private final VesselSelectionScreen screen;
		private final int index;
		private final VesselDefinition definition;

		private VesselButton(VesselSelectionScreen screen, int index, VesselDefinition definition, int x, int y, int width, int height) {
			super(x, y, width, height, Component.empty(), button -> screen.choose(index, definition), DEFAULT_NARRATION);
			this.screen = screen;
			this.index = index;
			this.definition = definition;
		}

		private boolean contains(int mouseX, int mouseY) {
			return mouseX >= getX() && mouseX < getX() + width && mouseY >= getY() && mouseY < getY() + height;
		}

		@Override
		protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
			if (!visible)
				return;
			boolean available = screen.isAvailable(index);
			boolean waiting = screen.selectingIndex == index;
			boolean hovered = contains(mouseX, mouseY) && available;
			int identityAccent = THEME_ACCENTS[themeFor(definition)];
			int activeAccent = hovered ? screen.weightedColor(THEME_ACCENTS) : identityAccent;
			int border = available ? (hovered ? activeAccent : mixRgb(0xFF07101A, identityAccent, 0.58F)) : 0xFF633A50;
			int fill = available ? withAlpha(mixRgb(0xFF02050A, activeAccent, hovered ? 0.24F : 0.10F), hovered ? 0xCE : 0xA8) : 0xA0150918;
			graphics.fill(getX(), getY(), getX() + width, getY() + height, fill);
			drawOutline(graphics, getX(), getY(), width, height, border);
			if (hovered)
				graphics.fill(getX() + 1, getY() + height - 3, getX() + width - 1, getY() + height - 1, withAlpha(activeAccent, 0x72));

			Font font = Minecraft.getInstance().font;
			String state = waiting ? "WAIT" : (available ? "OPEN" : "LOCKED");
			int stateColor = waiting ? 0xFFFFD66B : (available ? activeAccent : 0xFFFF6868);
			int nameColor = available ? (hovered ? screen.weightedColor(THEME_TEXT) : THEME_TEXT[themeFor(definition)]) : 0xFF967F8D;
			int subColor = available ? (hovered ? screen.weightedColor(THEME_SUBTEXT) : THEME_SUBTEXT[themeFor(definition)]) : 0xFF755E6B;
			graphics.drawString(font, fit(font, displayName(definition), width - 10), getX() + 5, getY() + 5, nameColor, false);
			int powerWidth = width - font.width(state) - 15;
			graphics.drawString(font, fit(font, displayPower(definition), powerWidth), getX() + 5, getY() + 17, subColor, false);
			graphics.drawString(font, state, getX() + width - font.width(state) - 5, getY() + 17, stateColor, false);
		}

		private static String fit(Font font, String text, int maxWidth) {
			if (font.width(text) <= maxWidth)
				return text;
			String value = text;
			while (!value.isEmpty() && font.width(value + "...") > maxWidth)
				value = value.substring(0, value.length() - 1);
			return value + "...";
		}
	}
}
