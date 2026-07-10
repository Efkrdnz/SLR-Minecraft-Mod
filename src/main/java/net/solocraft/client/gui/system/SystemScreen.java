package net.solocraft.client.gui.system;

import net.solocraft.client.renderer.shader.SystemBackgroundRenderTypes;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

import java.util.List;

/**
 * Reusable base for the reworked "Solo Leveling" System GUI.
 *
 * <p>Provides the shared chrome so concrete panels only place content:
 * <ul>
 *   <li>a <b>contained</b> animated void background (custom GLSL shader with a
 *       pure-Java fallback) — subtle descending static plus a compact glitch
 *       packet that tracks the cursor;</li>
 *   <li>a centred, glowing vertical "System window" frame with corner brackets
 *       and a title bar;</li>
 *   <li>an <b>open/close animation</b> that expands the window vertically from
 *       the middle outward (and collapses back on close).</li>
 * </ul>
 *
 * Subclasses override {@link #renderContent} and add widgets in {@link #init()}
 * (call {@code super.init()} first so the panel geometry is populated).
 */
public abstract class SystemScreen extends Screen {
	// Solo Leveling "System" palette
	protected static final int ACCENT = 0xFF3FC6FF; // bright azure
	protected static final int ACCENT_DIM = 0xFF2273A8;
	protected static final int ACCENT_SOFT = 0x553FC6FF;
	protected static final int TEXT_MAIN = 0xFFE8F6FF;
	protected static final int TEXT_SUB = 0xFF8FB8D8;
	protected static final int PANEL_FILL = 0xC2060A16;

	// portrait window
	protected int panelW = 196;
	protected int panelH = 300;
	protected int panelX;
	protected int panelY;

	// ── open/close animation ──────────────────────────────────────────────────
	private enum State { OPENING, OPEN, CLOSING }

	private static final long ANIM_MS = 180L;
	private State state = State.OPENING;
	private long animStart;
	private boolean closed;
	private float reveal; // 0..1 vertical expansion factor for this frame

	protected SystemScreen(Component title) {
		super(title);
	}

	@Override
	protected void init() {
		super.init();
		SystemGuiSounds.enter();
		this.panelX = (this.width - panelW) / 2;
		this.panelY = (this.height - panelH) / 2;
		this.state = State.OPENING;
		this.animStart = Util.getMillis();
		this.closed = false;
	}

	@Override
	public boolean isPauseScreen() {
		// keep the world (and thus GameTime) advancing so the background animates
		return false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false; // handled in keyPressed so the collapse animation can play
	}

	@Override
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == 256) { // ESC
			beginClose();
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	/** Start the collapse animation; the real close happens when it finishes. */
	protected void beginClose() {
		if (state != State.CLOSING) {
			SystemGuiSounds.exit();
			state = State.CLOSING;
			animStart = Util.getMillis();
		}
	}

	protected boolean isFullyOpen() {
		return state == State.OPEN;
	}

	private void updateAnimation() {
		float raw = Math.min(1.0f, (float) (Util.getMillis() - animStart) / (float) ANIM_MS);
		float eased = raw * raw * (3.0f - 2.0f * raw); // smoothstep
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
					this.minecraft.setScreen(null);
				}
			}
		}
	}

	private void setWidgetsVisible(boolean visible) {
		for (GuiEventListener child : this.children()) {
			if (child instanceof AbstractWidget widget) {
				widget.visible = visible;
				widget.active = visible;
			}
		}
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		updateAnimation();
		if (closed)
			return;

		// widgets only interactive/visible when fully open
		setWidgetsVisible(state == State.OPEN);

		// full-screen dim, committed before the scissor so it isn't clipped
		this.renderBackground(guiGraphics);
		guiGraphics.flush();

		int centerY = panelY + panelH / 2;
		int halfH = Math.round((panelH / 2f + 4f) * reveal);
		int top = centerY - halfH;
		int bottom = centerY + halfH;
		int sx0 = panelX - 2;
		int sx1 = panelX + panelW + 2;

		guiGraphics.enableScissor(sx0, top, sx1, bottom);
		renderAnimatedBackground(guiGraphics, mouseX, mouseY);
		renderFrame(guiGraphics);
		renderContent(guiGraphics, mouseX, mouseY, partialTicks);
		super.render(guiGraphics, mouseX, mouseY, partialTicks); // widgets on top
		guiGraphics.disableScissor();

		// bright leading-edge seams while expanding/collapsing
		if (reveal < 1.0f) {
			guiGraphics.fill(sx0, top, sx1, top + 1, ACCENT);
			guiGraphics.fill(sx0, bottom - 1, sx1, bottom, ACCENT);
			guiGraphics.fill(sx0, top + 1, sx1, top + 2, ACCENT_SOFT);
			guiGraphics.fill(sx0, bottom - 2, sx1, bottom - 1, ACCENT_SOFT);
		}

		// System-styled tooltip (only when fully open; drawn on top, unclipped)
		if (state == State.OPEN) {
			List<Component> tip = getHoverTooltip(mouseX, mouseY);
			if (tip != null && !tip.isEmpty()) {
				SystemTooltip.render(guiGraphics, this.font, tip, mouseX, mouseY, this.width, this.height);
			}
		}
	}

	/** Concrete panels draw their labels/data here (over the frame). */
	protected abstract void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks);

	/** Override to supply System-styled tooltip lines for the hovered region ({@code null} = none). */
	protected List<Component> getHoverTooltip(int mouseX, int mouseY) {
		return null;
	}

	/** Swap to another System screen (used by nav / back buttons). */
	protected void openChild(Screen screen) {
		if (this.minecraft != null) {
			SystemGuiSounds.switchInsideSystem();
			this.minecraft.setScreen(screen);
		}
	}

	/** True if the cursor is over the axis-aligned text box at (x,y) sized (w,h). */
	protected static boolean isOver(int mouseX, int mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y - 1 && mouseY < y + h;
	}

	// ── Background (contained to the panel rect) ───────────────────────────────

	private void renderAnimatedBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		float localX = clamp01((mouseX - panelX) / (float) panelW);
		float localY = clamp01((mouseY - panelY) / (float) panelH);

		ShaderInstance shader = SystemBackgroundRenderTypes.get();
		if (shader == null) {
			renderJavaFallbackBackground(guiGraphics, localX, localY);
			return;
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(SystemBackgroundRenderTypes::get);
		AbstractUniform mouse = shader.safeGetUniform("MousePos");
		mouse.set(localX, localY);
		shader.safeGetUniform("MouseGlitch").set(1.0f);

		int x0 = panelX, y0 = panelY, x1 = panelX + panelW, y1 = panelY + panelH;
		Matrix4f matrix = guiGraphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, x0, y1, 0).uv(0f, 1f).endVertex();
		buffer.vertex(matrix, x1, y1, 0).uv(1f, 1f).endVertex();
		buffer.vertex(matrix, x1, y0, 0).uv(1f, 0f).endVertex();
		buffer.vertex(matrix, x0, y0, 0).uv(0f, 0f).endVertex();
		Tesselator.getInstance().end();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** Pure-Java animated background used when the shader is unavailable (contained). */
	private void renderJavaFallbackBackground(GuiGraphics g, float localX, float localY) {
		int x0 = panelX, y0 = panelY, w = panelW, h = panelH;
		g.fillGradient(x0, y0, x0 + w, y0 + h, 0xF0060B18, 0xF0010208);

		float t = (float) (Util.getMillis() % 100000L) / 1000.0f;

		// faint drifting motes
		for (int i = 0; i < 40; i++) {
			float seed = i * 12.9898f;
			float fx = frac((float) Math.sin(seed) * 43758.545f);
			float fy = frac((float) Math.sin(seed * 1.7f) * 43758.545f);
			float drift = (fy * h + t * (8.0f + fx * 24.0f)) % h;
			int px = x0 + (int) (fx * w);
			int py = y0 + (int) drift;
			int alpha = (int) (60 + 60 * Math.sin(t * 1.6f + i));
			alpha = Math.max(20, Math.min(160, alpha));
			g.fill(px, py, px + 1, py + 1, (alpha << 24) | 0x3FC6FF);
		}

		// descending static band
		int bandY = y0 + (int) ((t * 26.0f) % h);
		for (int sx = 0; sx < w; sx += 2) {
			float rnd = frac((float) Math.sin((sx * 3.1f) + Math.floor(t * 3.0f)) * 4517.3f);
			if (rnd > 0.5f) {
				int a = (int) (40 + 80 * rnd);
				g.fill(x0 + sx, bandY, x0 + sx + 1, bandY + 2, (a << 24) | 0x2E9BD6);
			}
		}

		// compact cursor glitch packet
		int mx = x0 + (int) (localX * w);
		int my = y0 + (int) (localY * h);
		long tick = (long) Math.floor(t * 12.0f);
		g.fill(mx - 3, my, mx + 4, my + 1, 0x883FC6FF);
		g.fill(mx, my - 3, mx + 1, my + 4, 0x663FC6FF);
		g.fill(mx - 6, my - 5, mx + 6, my + 5, 0x123FC6FF);
		for (int i = 0; i < 12; i++) {
			float seed = i * 31.37f + tick * 7.13f;
			float rx = frac((float) Math.sin(seed) * 43758.545f);
			float ry = frac((float) Math.sin(seed * 1.83f) * 24634.634f);
			float rw = frac((float) Math.sin(seed * 2.41f) * 18331.473f);
			int ox = (int) ((rx - 0.5f) * 48.0f);
			int oy = (int) ((ry - 0.5f) * 34.0f);
			int len = 4 + (int) (rw * 18.0f);
			int alpha = 36 + (int) (90.0f * rw);
			int y = my + oy;
			int x = mx + ox;
			g.fill(x, y, x + len, y + 1, (alpha << 24) | 0x3FC6FF);
			if (rw > 0.64f) {
				g.fill(x - 1, y, x + 1, y + 1, 0x44FF5B8E);
				g.fill(x + len, y, x + len + 2, y + 1, 0x55366CFF);
			}
		}

		// vignette
		g.fillGradient(x0, y0, x0 + w, y0 + h, 0x22000000, 0x77000000);
	}

	private static float frac(float v) {
		return v - (float) Math.floor(v);
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}

	// ── Frame chrome ──────────────────────────────────────────────────────────

	protected void renderFrame(GuiGraphics g) {
		int x = panelX, y = panelY, w = panelW, h = panelH;
		// outer glow border
		g.fill(x - 1, y - 1, x + w + 1, y, ACCENT_SOFT);
		g.fill(x - 1, y + h, x + w + 1, y + h + 1, ACCENT_SOFT);
		g.fill(x - 1, y, x, y + h, ACCENT_SOFT);
		g.fill(x + w, y, x + w + 1, y + h, ACCENT_SOFT);
		// crisp inner border
		drawRectOutline(g, x, y, w, h, ACCENT_DIM);
		// title bar
		g.fill(x, y, x + w, y + 18, 0x66102A3E);
		g.fill(x, y + 18, x + w, y + 19, ACCENT);
		// corner brackets
		drawCornerBrackets(g, x, y, w, h);
		// title text (font-safe glyphs only)
		Font font = Minecraft.getInstance().font;
		String title = "[ " + this.title.getString() + " ]";
		g.drawString(font, title, x + (w - font.width(title)) / 2, y + 5, ACCENT, false);
	}

	private void drawRectOutline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
	}

	private void drawCornerBrackets(GuiGraphics g, int x, int y, int w, int h) {
		int len = 12;
		g.fill(x - 1, y - 1, x + len, y + 1, ACCENT);
		g.fill(x - 1, y - 1, x + 1, y + len, ACCENT);
		g.fill(x + w - len, y - 1, x + w + 1, y + 1, ACCENT);
		g.fill(x + w - 1, y - 1, x + w + 1, y + len, ACCENT);
		g.fill(x - 1, y + h - 1, x + len, y + h + 1, ACCENT);
		g.fill(x - 1, y + h - len, x + 1, y + h + 1, ACCENT);
		g.fill(x + w - len, y + h - 1, x + w + 1, y + h + 1, ACCENT);
		g.fill(x + w - 1, y + h - len, x + w + 1, y + h + 1, ACCENT);
	}

	// ── Shared System-styled button widget ─────────────────────────────────────

	/** A flat, glowing System-style button drawn entirely in code (no textures). */
	public static class SystemButton extends Button {
		public SystemButton(int x, int y, int w, int h, Component label, OnPress onPress) {
			super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
			if (!this.visible)
				return;
			boolean hovered = isHoveredOrFocused();
			int fill = hovered ? 0x804FB8E8 : 0x55102338;
			int border = hovered ? 0xFF7FE4FF : ACCENT_DIM;
			int text = hovered ? 0xFFFFFFFF : TEXT_MAIN;
			g.fill(getX(), getY(), getX() + width, getY() + height, fill);
			g.fill(getX(), getY(), getX() + width, getY() + 1, border);
			g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
			g.fill(getX(), getY(), getX() + 1, getY() + height, border);
			g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
			Font font = Minecraft.getInstance().font;
			g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, text);
		}
	}

	/** A flat, glowing System-style slider (value 0..1). */
	public static class SystemSlider extends AbstractSliderButton {
		private final java.util.function.DoubleFunction<Component> labelFn;
		private final java.util.function.DoubleConsumer onApply;
		private final Runnable onRelease;

		public SystemSlider(int x, int y, int w, int h, double value, java.util.function.DoubleFunction<Component> labelFn,
				java.util.function.DoubleConsumer onApply, Runnable onRelease) {
			super(x, y, w, h, Component.empty(), value);
			this.labelFn = labelFn;
			this.onApply = onApply;
			this.onRelease = onRelease;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(labelFn.apply(this.value));
		}

		@Override
		protected void applyValue() {
			onApply.accept(this.value);
		}

		@Override
		public void onRelease(double mouseX, double mouseY) {
			if (onRelease != null)
				onRelease.run();
		}

		@Override
		public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
			if (!this.visible)
				return;
			boolean hovered = isHoveredOrFocused();
			int border = hovered ? 0xFF7FE4FF : ACCENT_DIM;
			g.fill(getX(), getY(), getX() + width, getY() + height, 0x55102338);
			g.fill(getX(), getY(), getX() + width, getY() + 1, border);
			g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
			g.fill(getX(), getY(), getX() + 1, getY() + height, border);
			g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);
			int fillW = (int) (this.value * (width - 2));
			g.fill(getX() + 1, getY() + 1, getX() + 1 + fillW, getY() + height - 1, ACCENT_SOFT);
			int knobX = getX() + (int) (this.value * (width - 4));
			g.fill(knobX, getY(), knobX + 4, getY() + height, hovered ? 0xFF7FE4FF : ACCENT);
			Font font = Minecraft.getInstance().font;
			g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, TEXT_MAIN);
		}
	}
}
