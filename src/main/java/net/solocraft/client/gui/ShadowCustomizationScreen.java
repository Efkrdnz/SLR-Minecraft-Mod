package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.shader.ShadowSummonBackgroundRenderTypes;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.network.ShadowGlowColorMessage;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.world.inventory.ShadowCustomizationMenu;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

import java.util.List;

/**
 * Outline colour picker for the shadow army, with the boss artifact slot folded
 * in for the two shadows that have one.
 *
 * <p>The roster on the left names every shadow once; everything else on screen is
 * the colour being chosen. Prose that only restated what the controls already
 * show was removed deliberately — the swatch, the roster dot and the live preview
 * carry that information without a paragraph.
 */
public class ShadowCustomizationScreen
		extends AbstractContainerScreen<ShadowCustomizationMenu> {
	private static final int PANEL_W = 336;
	private static final int PANEL_H = 300;
	private static final int HEADER_H = 20;
	private static final int LIST_X = 10;
	private static final int LIST_Y = 28;
	private static final int LIST_W = 116;
	private static final int ROW_H = 12;
	private static final int EDITOR_X = 134;
	private static final int EDITOR_Y = 28;
	private static final int EDITOR_W = 192;

	private static final int ACCENT = 0xFFB75CFF;
	private static final int ACCENT_BLUE = 0xFF43C8FF;
	private static final int ACCENT_DIM = 0xFF6A2D98;
	private static final int TEXT_MAIN = 0xFFECEBFF;
	private static final int TEXT_SUB = 0xFF9CA7D5;
	private static final int INK = 0xCC080510;
	private static final int GOLD = 0xFFFFC84A;
	private static final long ANIM_MS = 190L;

	private static final int[] PRESETS = {
			0x3FC6FF, 0xB75CFF, 0xFFD966, 0xFF5B8E, 0x5BFF9E, 0xFFFFFF
	};

	private final List<String> types = ShadowMonarchManager.customizableTypes();
	private State state = State.OPENING;
	private long animStart;
	private boolean closed;
	private float reveal;
	private boolean suppressNestedBackground;

	private int selected;
	private int draftColor = ShadowMonarchManager.NO_GLOW;
	private boolean draftDirty;
	private int draftIdleTicks;

	private ColorSlider redSlider;
	private ColorSlider greenSlider;
	private ColorSlider blueSlider;
	private Button toggleButton;

	public ShadowCustomizationScreen(ShadowCustomizationMenu menu,
			Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = PANEL_W;
		this.imageHeight = PANEL_H;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public void init() {
		super.init();
		this.state = State.OPENING;
		this.animStart = Util.getMillis();
		this.closed = false;
		this.reveal = 0.0F;
		this.selected = Math.max(0, this.types.indexOf(menu.shadowType()));
		this.draftColor = menu.glowColor(this.selected);
		playPanelSound(SololevelingModSounds.PANELOPEN.get(), 0.78F);
		buildControls();
	}

	private void buildControls() {
		clearWidgets();
		int x = leftPos + EDITOR_X + 8;
		int y = topPos + EDITOR_Y + 74;
		this.redSlider = addRenderableWidget(new ColorSlider(x, y, "R", 16));
		this.greenSlider = addRenderableWidget(new ColorSlider(x, y + 18, "G", 8));
		this.blueSlider = addRenderableWidget(new ColorSlider(x, y + 36, "B", 0));
		this.toggleButton = addRenderableWidget(Button.builder(
						Component.empty(), button -> toggleOutline())
				.bounds(x, y + 58, EDITOR_W - 16, 18).build());
		refreshToggleLabel();
	}

	private void refreshToggleLabel() {
		if (this.toggleButton != null)
			this.toggleButton.setMessage(Component.literal(
					hasOutline() ? "Remove Outline" : "Apply Outline"));
	}

	private boolean hasOutline() {
		return this.draftColor != ShadowMonarchManager.NO_GLOW;
	}

	private int editableColor() {
		return hasOutline() ? this.draftColor : PRESETS[0];
	}

	private void select(int index) {
		if (index == this.selected || index < 0 || index >= this.types.size())
			return;
		commitDraft();
		this.selected = index;
		this.draftColor = menu.glowColor(index);
		syncSliders();
		refreshToggleLabel();
	}

	private void syncSliders() {
		if (this.redSlider != null)
			this.redSlider.pull();
		if (this.greenSlider != null)
			this.greenSlider.pull();
		if (this.blueSlider != null)
			this.blueSlider.pull();
	}

	private void toggleOutline() {
		this.draftColor = hasOutline()
				? ShadowMonarchManager.NO_GLOW : PRESETS[0];
		syncSliders();
		refreshToggleLabel();
		sendColor();
	}

	private void applyPreset(int rgb) {
		this.draftColor = rgb;
		syncSliders();
		refreshToggleLabel();
		sendColor();
	}

	/** Slider drags are batched so a drag does not flood the server. */
	private void commitDraft() {
		if (this.draftDirty)
			sendColor();
	}

	private void sendColor() {
		this.draftDirty = false;
		this.draftIdleTicks = 0;
		menu.setGlowColorLocal(this.selected, this.draftColor);
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowGlowColorMessage(
				this.types.get(this.selected), this.draftColor));
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		if (this.draftDirty && ++this.draftIdleTicks >= 5)
			commitDraft();
	}

	// ── rendering ─────────────────────────────────────────────────────────────

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY,
			float partialTicks) {
		updateAnimation();
		if (closed)
			return;
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		int logicalMouseX = transform.logicalMouseX(mouseX);
		int logicalMouseY = transform.logicalMouseY(mouseY);
		this.renderTransparentBackground(graphics);
		graphics.flush();
		ResponsiveGuiScale.push(graphics, transform);
		int centerY = topPos + imageHeight / 2;
		int halfH = Math.round((imageHeight / 2.0F + 4.0F) * reveal);
		int top = centerY - halfH;
		int bottom = centerY + halfH;
		int sx0 = leftPos - 3;
		int sx1 = leftPos + imageWidth + 3;
		ResponsiveGuiScale.enableScissor(graphics, transform, sx0, top, sx1, bottom);
		suppressNestedBackground = true;
		try {
			super.render(graphics, logicalMouseX, logicalMouseY, partialTicks);
		} finally {
			suppressNestedBackground = false;
		}
		graphics.disableScissor();
		if (reveal < 1.0F) {
			graphics.fill(sx0, top, sx1, top + 1, ACCENT);
			graphics.fill(sx0, bottom - 1, sx1, bottom, ACCENT_BLUE);
		}
		ResponsiveGuiScale.pop(graphics);
		if (state == State.OPEN)
			this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY,
			float partialTick) {
		if (suppressNestedBackground)
			renderBg(graphics, partialTick, mouseX, mouseY);
		else
			super.renderBackground(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX,
			int mouseY) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		renderAnimatedBackground(graphics, mouseX, mouseY);
		graphics.drawManaged(() -> {
			renderFrame(graphics);
			renderRoster(graphics, mouseX, mouseY);
			renderEditor(graphics);
		});
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		// Everything is drawn in panel space by renderBg.
	}

	private void renderFrame(GuiGraphics graphics) {
		int x = leftPos;
		int y = topPos;
		graphics.fill(x - 1, y - 1, x + imageWidth + 1, y, 0x8843C8FF);
		graphics.fill(x - 1, y + imageHeight, x + imageWidth + 1,
				y + imageHeight + 1, 0x8843C8FF);
		graphics.fill(x - 1, y, x, y + imageHeight, 0x8843C8FF);
		graphics.fill(x + imageWidth, y, x + imageWidth + 1, y + imageHeight,
				0x8843C8FF);
		outline(graphics, x, y, imageWidth, imageHeight, ACCENT_DIM);
		graphics.fillGradient(x, y, x + imageWidth, y + HEADER_H, 0xB2200A33,
				0x8C140921);
		graphics.fill(x, y + HEADER_H - 1, x + imageWidth, y + HEADER_H, ACCENT);
		drawCornerBrackets(graphics, x, y, imageWidth, imageHeight);
		String title = "SHADOW OUTLINES";
		graphics.drawString(this.font, title,
				x + (imageWidth - this.font.width(title)) / 2, y + 6, ACCENT,
				false);
	}

	/** One row per shadow: a colour dot, the name, and nothing else. */
	private void renderRoster(GuiGraphics graphics, int mouseX, int mouseY) {
		int x = leftPos + LIST_X;
		int y = topPos + LIST_Y;
		int height = this.types.size() * ROW_H + 4;
		graphics.fill(x, y, x + LIST_W, y + height, 0x66060310);
		outline(graphics, x, y, LIST_W, height, 0x5543C8FF);

		for (int index = 0; index < this.types.size(); index++) {
			int rowY = y + 2 + index * ROW_H;
			boolean active = index == this.selected;
			boolean hovered = mouseX >= x && mouseX < x + LIST_W
					&& mouseY >= rowY && mouseY < rowY + ROW_H;
			if (active)
				graphics.fill(x + 1, rowY, x + LIST_W - 1, rowY + ROW_H,
						0x66B75CFF);
			else if (hovered)
				graphics.fill(x + 1, rowY, x + LIST_W - 1, rowY + ROW_H,
						0x33FFFFFF);
			if (active)
				graphics.fill(x + 1, rowY, x + 3, rowY + ROW_H, ACCENT);

			int color = menu.glowColor(index);
			int dotX = x + 7;
			int dotY = rowY + 4;
			if (color != ShadowMonarchManager.NO_GLOW) {
				graphics.fill(dotX, dotY, dotX + 6, dotY + 6,
						0xFF000000 | color);
			} else {
				outline(graphics, dotX, dotY, 6, 6, 0xFF4A4560);
			}
			boolean owned = menu.ownsShadow(index);
			graphics.drawString(this.font, displayName(this.types.get(index)),
					x + 18, rowY + 3,
					active ? TEXT_MAIN : owned ? TEXT_SUB : 0xFF6A6480, false);
		}
	}

	private void renderEditor(GuiGraphics graphics) {
		int x = leftPos + EDITOR_X;
		int y = topPos + EDITOR_Y;
		int height = this.types.size() * ROW_H + 4;
		graphics.fill(x, y, x + EDITOR_W, y + height, 0x66060310);
		outline(graphics, x, y, EDITOR_W, height, 0x55B75CFF);

		graphics.drawString(this.font,
				displayName(this.types.get(this.selected)), x + 8, y + 7,
				TEXT_MAIN, false);

		// Live swatch: exactly what the outline will look like in the world.
		int swatchX = x + 8;
		int swatchY = y + 21;
		int swatchW = EDITOR_W - 16;
		graphics.fill(swatchX, swatchY, swatchX + swatchW, swatchY + 22, INK);
		if (hasOutline()) {
			int color = 0xFF000000 | this.draftColor;
			float pulse = 0.55F + 0.45F
					* (float) Math.sin(Util.getMillis() / 320.0D);
			graphics.fill(swatchX + 1, swatchY + 1, swatchX + swatchW - 1,
					swatchY + 21, withAlpha(this.draftColor,
							(int) (0x33 + 0x3C * pulse)));
			outline(graphics, swatchX, swatchY, swatchW, 22, color);
			String hex = String.format("#%06X", this.draftColor);
			graphics.drawCenteredString(this.font, hex, swatchX + swatchW / 2,
					swatchY + 8, color);
		} else {
			outline(graphics, swatchX, swatchY, swatchW, 22, 0xFF3A3550);
			graphics.drawCenteredString(this.font, "NO OUTLINE",
					swatchX + swatchW / 2, swatchY + 8, 0xFF6A6480);
		}

		// Preset swatches.
		int presetY = y + 49;
		int presetW = (swatchW - (PRESETS.length - 1) * 3) / PRESETS.length;
		for (int index = 0; index < PRESETS.length; index++) {
			int px = swatchX + index * (presetW + 3);
			graphics.fill(px, presetY, px + presetW, presetY + 14,
					0xFF000000 | PRESETS[index]);
			if (hasOutline() && this.draftColor == PRESETS[index])
				outline(graphics, px - 1, presetY - 1, presetW + 2, 16, TEXT_MAIN);
		}

		if (menu.supportsArtifact())
			renderArtifact(graphics, x, y + height + 4);
		renderSlotBackgrounds(graphics);
	}

	private void renderArtifact(GuiGraphics graphics, int x, int y) {
		boolean equipped = hasEquippedArtifact();
		graphics.drawString(this.font, "ARTIFACT", x + 8, y + 4, ACCENT_BLUE,
				false);
		graphics.drawString(this.font, equipped ? "EQUIPPED" : "EMPTY",
				x + EDITOR_W - 8 - this.font.width(equipped ? "EQUIPPED" : "EMPTY"),
				y + 4, equipped ? GOLD : TEXT_SUB, false);
	}

	private void renderSlotBackgrounds(GuiGraphics graphics) {
		if (menu.supportsArtifact()) {
			int equipmentX = leftPos + ShadowCustomizationMenu.EQUIPMENT_SLOT_X - 1;
			int equipmentY = topPos + ShadowCustomizationMenu.EQUIPMENT_SLOT_Y - 1;
			boolean equipped = hasEquippedArtifact();
			graphics.fill(equipmentX, equipmentY, equipmentX + 18, equipmentY + 18,
					equipped ? 0x88452F0E : INK);
			outline(graphics, equipmentX, equipmentY, 18, 18,
					equipped ? GOLD : ACCENT);
		}
		for (int row = 0; row < 3; row++)
			for (int column = 0; column < 9; column++)
				drawSlot(graphics,
						leftPos + ShadowCustomizationMenu.PLAYER_INVENTORY_X
								+ column * 18 - 1,
						topPos + ShadowCustomizationMenu.PLAYER_INVENTORY_Y
								+ row * 18 - 1);
		for (int column = 0; column < 9; column++)
			drawSlot(graphics,
					leftPos + ShadowCustomizationMenu.PLAYER_INVENTORY_X
							+ column * 18 - 1,
					topPos + ShadowCustomizationMenu.PLAYER_HOTBAR_Y - 1);
	}

	private static void drawSlot(GuiGraphics graphics, int x, int y) {
		graphics.fill(x, y, x + 18, y + 18, INK);
		outline(graphics, x, y, 18, 18, 0x88547BA8);
	}

	private boolean hasEquippedArtifact() {
		Slot slot = menu.get().get(0);
		return slot != null && slot.hasItem();
	}

	private static String displayName(String type) {
		return switch (type) {
			case "goblin_club" -> "Goblin Fighter";
			case "goblin_archer" -> "Goblin Archer";
			case "goblin_mage" -> "Goblin Mage";
			case "wolf" -> "Lycan";
			case "knight" -> "Knight";
			case "polar_bear" -> "Polar Bear";
			case "orc" -> "Orc";
			case "high_orc" -> "High Orc";
			case "igris" -> "Igris";
			case "beru" -> "Beru";
			case "kamish" -> "Kamish";
			case "tusk" -> "Tusk";
			case "kaisel" -> "Kaisel";
			case "iron" -> "Iron";
			default -> type;
		};
	}

	// ── input ─────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (state != State.OPEN)
			return true;
		double logicalX = logicalMouseX(mouseX);
		double logicalY = logicalMouseY(mouseY);

		int listX = leftPos + LIST_X;
		int listY = topPos + LIST_Y + 2;
		if (button == 0 && logicalX >= listX && logicalX < listX + LIST_W) {
			int row = (int) ((logicalY - listY) / ROW_H);
			if (row >= 0 && row < this.types.size() && logicalY >= listY) {
				select(row);
				return true;
			}
		}

		int swatchX = leftPos + EDITOR_X + 8;
		int presetY = topPos + EDITOR_Y + 49;
		int swatchW = EDITOR_W - 16;
		int presetW = (swatchW - (PRESETS.length - 1) * 3) / PRESETS.length;
		if (button == 0 && logicalY >= presetY && logicalY < presetY + 14) {
			for (int index = 0; index < PRESETS.length; index++) {
				int px = swatchX + index * (presetW + 3);
				if (logicalX >= px && logicalX < px + presetW) {
					applyPreset(PRESETS[index]);
					return true;
				}
			}
		}
		return super.mouseClicked(logicalX, logicalY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return super.mouseReleased(logicalMouseX(mouseX), logicalMouseY(mouseY),
				button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button,
			double dragX, double dragY) {
		float scale = responsiveTransform().scale();
		return super.mouseDragged(logicalMouseX(mouseX), logicalMouseY(mouseY),
				button, dragX / scale, dragY / scale);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(logicalMouseX(mouseX), logicalMouseY(mouseY));
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (key == 256 || this.minecraft != null
				&& this.minecraft.options.keyInventory.matches(key, scanCode)) {
			beginClose();
			return true;
		}
		return state != State.OPEN || super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	public void onClose() {
		beginClose();
	}

	private void beginClose() {
		if (state == State.CLOSING || closed)
			return;
		commitDraft();
		state = State.CLOSING;
		animStart = Util.getMillis();
		playPanelSound(SololevelingModSounds.PANELCLOSE.get(), 0.72F);
	}

	private void updateAnimation() {
		float raw = Math.min(1.0F,
				(float) (Util.getMillis() - animStart) / (float) ANIM_MS);
		float eased = raw * raw * (3.0F - 2.0F * raw);
		switch (state) {
			case OPENING -> {
				reveal = eased;
				if (raw >= 1.0F) {
					state = State.OPEN;
					reveal = 1.0F;
				}
			}
			case OPEN -> reveal = 1.0F;
			case CLOSING -> {
				reveal = 1.0F - eased;
				if (raw >= 1.0F && !closed) {
					closed = true;
					if (this.minecraft != null && this.minecraft.player != null)
						this.minecraft.player.closeContainer();
				}
			}
		}
	}

	private void playPanelSound(SoundEvent sound, float pitch) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft != null && minecraft.getSoundManager() != null)
			minecraft.getSoundManager().play(
					SimpleSoundInstance.forUI(sound, pitch, 0.46F));
	}

	private ResponsiveGuiScale.Transform responsiveTransform() {
		return ResponsiveGuiScale.fit(this.width, this.height, PANEL_W + 8,
				PANEL_H + 8);
	}

	private double logicalMouseX(double mouseX) {
		return responsiveTransform().logicalX(mouseX);
	}

	private double logicalMouseY(double mouseY) {
		return responsiveTransform().logicalY(mouseY);
	}

	private void renderAnimatedBackground(GuiGraphics graphics, int mouseX,
			int mouseY) {
		float localX = clamp01((mouseX - leftPos) / (float) imageWidth);
		float localY = clamp01((mouseY - topPos) / (float) imageHeight);
		ShaderInstance shader = ShadowSummonBackgroundRenderTypes.get();
		if (shader == null) {
			graphics.fillGradient(leftPos, topPos, leftPos + imageWidth,
					topPos + imageHeight, 0xF0060712, 0xF014071D);
			return;
		}
		RenderSystem.setShader(ShadowSummonBackgroundRenderTypes::get);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableCull();
		AbstractUniform mouse = shader.safeGetUniform("MousePos");
		mouse.set(localX, localY);
		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.addVertex(matrix, leftPos, topPos + imageHeight, 0).setUv(0.0F, 1.0F);
		buffer.addVertex(matrix, leftPos + imageWidth, topPos + imageHeight, 0)
				.setUv(1.0F, 1.0F);
		buffer.addVertex(matrix, leftPos + imageWidth, topPos, 0).setUv(1.0F, 0.0F);
		buffer.addVertex(matrix, leftPos, topPos, 0).setUv(0.0F, 0.0F);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.enableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static float clamp01(float value) {
		return value < 0.0F ? 0.0F : Math.min(value, 1.0F);
	}

	private static int withAlpha(int rgb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (rgb & 0xFFFFFF);
	}

	private static void outline(GuiGraphics graphics, int x, int y, int width,
			int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	private static void drawCornerBrackets(GuiGraphics graphics, int x, int y,
			int width, int height) {
		int length = 13;
		graphics.fill(x - 1, y - 1, x + length, y + 1, ACCENT_BLUE);
		graphics.fill(x - 1, y - 1, x + 1, y + length, ACCENT_BLUE);
		graphics.fill(x + width - length, y - 1, x + width + 1, y + 1, ACCENT);
		graphics.fill(x + width - 1, y - 1, x + width + 1, y + length, ACCENT);
		graphics.fill(x - 1, y + height - 1, x + length, y + height + 1,
				ACCENT_BLUE);
		graphics.fill(x - 1, y + height - length, x + 1, y + height + 1,
				ACCENT_BLUE);
		graphics.fill(x + width - length, y + height - 1, x + width + 1,
				y + height + 1, ACCENT);
		graphics.fill(x + width - 1, y + height - length, x + width + 1,
				y + height + 1, ACCENT);
	}

	private final class ColorSlider extends AbstractSliderButton {
		private final String channel;
		private final int shift;

		private ColorSlider(int x, int y, String channel, int shift) {
			super(x, y, EDITOR_W - 16, 16, Component.empty(),
					(ShadowCustomizationScreen.this.editableColor() >> shift & 0xFF)
							/ 255.0D);
			this.channel = channel;
			this.shift = shift;
			updateMessage();
		}

		private void pull() {
			this.value = (ShadowCustomizationScreen.this.editableColor()
					>> this.shift & 0xFF) / 255.0D;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Component.literal(this.channel + "  "
					+ (int) Math.round(this.value * 255.0D)));
		}

		@Override
		protected void applyValue() {
			int channelValue = (int) Math.round(this.value * 255.0D);
			int base = ShadowCustomizationScreen.this.editableColor();
			ShadowCustomizationScreen.this.draftColor =
					base & ~(0xFF << this.shift) | channelValue << this.shift;
			ShadowCustomizationScreen.this.draftDirty = true;
			ShadowCustomizationScreen.this.draftIdleTicks = 0;
			ShadowCustomizationScreen.this.refreshToggleLabel();
			updateMessage();
		}
	}

	private enum State {
		OPENING,
		OPEN,
		CLOSING
	}
}
