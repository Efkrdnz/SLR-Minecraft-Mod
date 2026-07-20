package net.solocraft.client.gui;

import net.solocraft.client.renderer.shader.ShadowSummonBackgroundRenderTypes;
import net.solocraft.init.SololevelingModSounds;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

public abstract class ShadowStyledScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	protected static final int ACCENT = 0xFFB75CFF;
	protected static final int ACCENT_BLUE = 0xFF43C8FF;
	protected static final int ACCENT_DIM = 0xFF6A2D98;
	protected static final int TEXT_MAIN = 0xFFECEBFF;
	protected static final int TEXT_SUB = 0xFF9CA7D5;

	private static final long ANIM_MS = 190L;
	private static final float OPEN_SOUND_PITCH = 0.76F;
	private static final float CLOSE_SOUND_PITCH = 0.70F;
	private static final float SOUND_VOLUME = 0.46F;

	private State state = State.OPENING;
	private long animStart;
	private boolean closed;
	private float reveal;

	protected ShadowStyledScreen(T container, Inventory inventory, Component title, int imageWidth, int imageHeight) {
		super(container, inventory, title);
		this.imageWidth = imageWidth;
		this.imageHeight = imageHeight;
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
		playPanelSound(SololevelingModSounds.PANELOPEN.get(), OPEN_SOUND_PITCH);
		initShadowWidgets();
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		updateAnimation();
		if (closed)
			return;
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		int logicalMouseX = transform.logicalMouseX(mouseX);
		int logicalMouseY = transform.logicalMouseY(mouseY);
		this.renderBackground(guiGraphics);
		guiGraphics.flush();
		ResponsiveGuiScale.push(guiGraphics, transform);
		int centerY = topPos + imageHeight / 2;
		int halfH = Math.round((imageHeight / 2f + 4f) * reveal);
		int top = centerY - halfH;
		int bottom = centerY + halfH;
		int sx0 = leftPos - 3;
		int sx1 = leftPos + imageWidth + 3;
		ResponsiveGuiScale.enableScissor(guiGraphics, transform, sx0, top, sx1, bottom);
		super.render(guiGraphics, logicalMouseX, logicalMouseY, partialTicks);
		guiGraphics.disableScissor();
		if (reveal < 1.0f) {
			guiGraphics.fill(sx0, top, sx1, top + 1, ACCENT);
			guiGraphics.fill(sx0, bottom - 1, sx1, bottom, ACCENT);
			guiGraphics.fill(sx0, top + 1, sx1, top + 2, 0x77B75CFF);
			guiGraphics.fill(sx0, bottom - 2, sx1, bottom - 1, 0x7743C8FF);
		}
		if (state == State.OPEN) {
			renderShadowTooltips(guiGraphics, logicalMouseX, logicalMouseY);
		}
		ResponsiveGuiScale.pop(guiGraphics);
		if (state == State.OPEN)
			this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		renderAnimatedBackground(guiGraphics, mouseX, mouseY);
		renderFrame(guiGraphics);
		renderShadowSections(guiGraphics);
		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (key == 256 || (this.minecraft != null && this.minecraft.options.keyInventory.matches(key, scanCode))) {
			beginClose();
			return true;
		}
		return super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (state != State.OPEN)
			return true;
		return super.mouseClicked(logicalMouseX(mouseX), logicalMouseY(mouseY), button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return super.mouseReleased(logicalMouseX(mouseX), logicalMouseY(mouseY), button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		float scale = responsiveTransform().scale();
		return super.mouseDragged(logicalMouseX(mouseX), logicalMouseY(mouseY), button, dragX / scale, dragY / scale);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		return super.mouseScrolled(logicalMouseX(mouseX), logicalMouseY(mouseY), delta);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(logicalMouseX(mouseX), logicalMouseY(mouseY));
	}

	protected ResponsiveGuiScale.Transform responsiveTransform() {
		return ResponsiveGuiScale.fit(this.width, this.height, imageWidth + 8, imageHeight + 8);
	}

	protected double logicalMouseX(double mouseX) {
		return responsiveTransform().logicalX(mouseX);
	}

	protected double logicalMouseY(double mouseY) {
		return responsiveTransform().logicalY(mouseY);
	}

	@Override
	public void onClose() {
		beginClose();
	}

	protected abstract String shadowTitle();

	protected abstract void initShadowWidgets();

	protected abstract void renderShadowSections(GuiGraphics guiGraphics);

	protected void renderShadowTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
	}

	protected boolean isOpeningOrOpen() {
		return state == State.OPENING || state == State.OPEN;
	}

	private void beginClose() {
		if (state == State.CLOSING || closed)
			return;
		state = State.CLOSING;
		animStart = Util.getMillis();
		playPanelSound(SololevelingModSounds.PANELCLOSE.get(), CLOSE_SOUND_PITCH);
	}

	private void updateAnimation() {
		float raw = Math.min(1.0f, (float) (Util.getMillis() - animStart) / (float) ANIM_MS);
		float eased = raw * raw * (3.0f - 2.0f * raw);
		switch (state) {
			case OPENING -> {
				reveal = eased;
				if (raw >= 1.0f) {
					state = State.OPEN;
					reveal = 1.0f;
				}
			}
			case OPEN -> reveal = 1.0f;
			case CLOSING -> {
				reveal = 1.0f - eased;
				if (raw >= 1.0f && !closed) {
					closed = true;
					if (this.minecraft != null && this.minecraft.player != null)
						this.minecraft.player.closeContainer();
				}
			}
		}
	}

	private void playPanelSound(SoundEvent sound, float pitch) {
		Minecraft mc = Minecraft.getInstance();
		if (mc != null && mc.getSoundManager() != null)
			mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, SOUND_VOLUME));
	}

	private void renderFrame(GuiGraphics g) {
		int x = leftPos;
		int y = topPos;
		int w = imageWidth;
		int h = imageHeight;
		g.fill(x - 1, y - 1, x + w + 1, y, 0x8843C8FF);
		g.fill(x - 1, y + h, x + w + 1, y + h + 1, 0x8843C8FF);
		g.fill(x - 1, y, x, y + h, 0x8843C8FF);
		g.fill(x + w, y, x + w + 1, y + h, 0x8843C8FF);
		outline(g, x, y, w, h, ACCENT_DIM);
		g.fill(x, y, x + w, y + 22, 0x7A140921);
		g.fill(x, y + 22, x + w, y + 23, ACCENT);
		drawCornerBrackets(g, x, y, w, h);
		String title = "[ " + shadowTitle() + " ]";
		g.drawString(this.font, title, x + (w - this.font.width(title)) / 2, y + 7, ACCENT, false);
	}

	private void renderAnimatedBackground(GuiGraphics g, int mouseX, int mouseY) {
		float localX = clamp01((mouseX - leftPos) / (float) imageWidth);
		float localY = clamp01((mouseY - topPos) / (float) imageHeight);
		ShaderInstance shader = ShadowSummonBackgroundRenderTypes.get();
		if (shader == null) {
			renderFallbackBackground(g, localX, localY);
			return;
		}
		RenderSystem.setShader(ShadowSummonBackgroundRenderTypes::get);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableCull();
		AbstractUniform mouse = shader.safeGetUniform("MousePos");
		mouse.set(localX, localY);
		Matrix4f matrix = g.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, leftPos, topPos + imageHeight, 0).uv(0f, 1f).endVertex();
		buffer.vertex(matrix, leftPos + imageWidth, topPos + imageHeight, 0).uv(1f, 1f).endVertex();
		buffer.vertex(matrix, leftPos + imageWidth, topPos, 0).uv(1f, 0f).endVertex();
		buffer.vertex(matrix, leftPos, topPos, 0).uv(0f, 0f).endVertex();
		Tesselator.getInstance().end();
		RenderSystem.enableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void renderFallbackBackground(GuiGraphics g, float localX, float localY) {
		int x = leftPos;
		int y = topPos;
		g.fillGradient(x, y, x + imageWidth, y + imageHeight, 0xF0060712, 0xF014071D);
		float t = (float) (Util.getMillis() % 100000L) / 1000.0f;
		int mx = x + (int) (localX * imageWidth);
		int my = y + (int) (localY * imageHeight);
		for (int i = 0; i < 14; i++) {
			int lineY = my - 38 + i * 6;
			int shift = (int) (Math.sin(t * 12.0f + i * 1.7f) * 10.0f);
			int alpha = 35 + (int) (25 * (0.5f + 0.5f * Math.sin(t * 8.0f + i)));
			g.fill(mx - 54 + shift, lineY, mx + 54 + shift, lineY + 1, (alpha << 24) | 0xB75CFF);
			if (i % 3 == 0)
				g.fill(mx - 44 - shift, lineY + 2, mx + 42 - shift, lineY + 3, (alpha << 24) | 0x43C8FF);
		}
		g.fillGradient(x, y, x + imageWidth, y + imageHeight, 0x00000000, 0x66000000);
	}

	protected static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	protected static boolean isOver(double mouseX, double mouseY, Button button) {
		return button != null && mouseX >= button.getX() && mouseX < button.getX() + button.getWidth() && mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
	}

	private void drawCornerBrackets(GuiGraphics g, int x, int y, int w, int h) {
		int len = 15;
		g.fill(x - 1, y - 1, x + len, y + 1, ACCENT_BLUE);
		g.fill(x - 1, y - 1, x + 1, y + len, ACCENT_BLUE);
		g.fill(x + w - len, y - 1, x + w + 1, y + 1, ACCENT);
		g.fill(x + w - 1, y - 1, x + w + 1, y + len, ACCENT);
		g.fill(x - 1, y + h - 1, x + len, y + h + 1, ACCENT_BLUE);
		g.fill(x - 1, y + h - len, x + 1, y + h + 1, ACCENT_BLUE);
		g.fill(x + w - len, y + h - 1, x + w + 1, y + h + 1, ACCENT);
		g.fill(x + w - 1, y + h - len, x + w + 1, y + h + 1, ACCENT);
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : Math.min(v, 1f);
	}

	private enum State {
		OPENING, OPEN, CLOSING
	}

	protected static class ShadowButton extends Button {
		private final boolean purpleAccent;
		private final boolean compact;

		ShadowButton(int x, int y, int w, int h, Component label, boolean purpleAccent, OnPress onPress) {
			this(x, y, w, h, label, purpleAccent, false, onPress);
		}

		ShadowButton(int x, int y, int w, int h, Component label, boolean purpleAccent, boolean compact, OnPress onPress) {
			super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
			this.purpleAccent = purpleAccent;
			this.compact = compact;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
			boolean hovered = this.isHoveredOrFocused();
			int accent = purpleAccent ? ACCENT : ACCENT_BLUE;
			int border = !active ? 0x66545B74 : hovered ? 0xFFFFFFFF : accent;
			int fill = !active ? 0x3310131F : hovered ? (purpleAccent ? 0x884A1D68 : 0x8843C8FF) : 0x55102338;
			g.fill(getX(), getY(), getX() + width, getY() + height, fill);
			outline(g, getX(), getY(), width, height, border);
			Font font = Minecraft.getInstance().font;
			int color = !active ? 0xFF6D728C : hovered ? 0xFFFFFFFF : TEXT_MAIN;
			if (compact) {
				g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
				return;
			}
			String text = font.plainSubstrByWidth(getMessage().getString(), width - 14);
			g.drawString(font, text, getX() + 7, getY() + (height - 8) / 2, color, false);
		}
	}
}
