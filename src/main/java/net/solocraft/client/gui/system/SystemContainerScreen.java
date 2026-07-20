package net.solocraft.client.gui.system;

import net.solocraft.client.gui.ResponsiveGuiScale;
import net.solocraft.client.renderer.shader.SystemBackgroundRenderTypes;
import net.solocraft.util.SystemPlayerAccess;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

/**
 * Container-screen counterpart to {@link SystemScreen}. Gives the shop screens
 * the same animated System background (custom shader with Java fallback, cursor
 * glitch via MousePos) and a vertical open/close reveal animation, while still
 * hosting real item slots.
 *
 * Subclasses set the panel rect ({@link #pRelX}/{@link #pRelY}/{@link #pW}/{@link #pH},
 * relative to leftPos/topPos) and draw their frame + slot cells in {@code renderBg}.
 */
public abstract class SystemContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
	protected static final int ACCENT = 0xFF3FC6FF;
	protected static final int ACCENT_SOFT = 0x553FC6FF;

	protected int pRelX = -80, pRelY = -100, pW = 160, pH = 200;

	private enum State { OPENING, OPEN, CLOSING }

	private static final long ANIM_MS = 170L;
	private State state = State.OPENING;
	private long animStart;
	private boolean closed;
	private boolean accessDenied;
	private float reveal;

	protected SystemContainerScreen(T menu, Inventory inv, Component title) {
		super(menu, inv, title);
	}

	@Override
	protected void init() {
		super.init();
		this.accessDenied = !SystemPlayerAccess.hasSystem(this.minecraft == null ? null : this.minecraft.player)
				&& !allowsNonSystemAccess();
		if (accessDenied) {
			if (this.minecraft != null && this.minecraft.player != null)
				this.minecraft.player.closeContainer();
			return;
		}
		if (shouldPlaySystemSounds())
			SystemGuiSounds.enter();
		// Center the panel on screen. The screens use imageWidth/Height = 0, so
		// vanilla puts the origin at the screen centre; shift it so the panel's
		// own centre lands on the screen centre. Slots share this origin, so they
		// stay aligned. Must run before subclasses add widgets in their init().
		this.leftPos = (this.width - pW) / 2 - pRelX;
		this.topPos = (this.height - pH) / 2 - pRelY;
		this.state = State.OPENING;
		this.animStart = Util.getMillis();
		this.closed = false;
	}

	protected boolean isOpen() {
		return state == State.OPEN;
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
	public boolean keyPressed(int key, int scan, int mods) {
		if (key == 256 || (this.minecraft != null && this.minecraft.options.keyInventory.matches(key, scan))) {
			beginClose();
			return true;
		}
		return super.keyPressed(key, scan, mods);
	}

	protected void beginClose() {
		if (state != State.CLOSING) {
			if (shouldPlaySystemSounds())
				SystemGuiSounds.exit();
			state = State.CLOSING;
			animStart = Util.getMillis();
		}
	}

	protected void openSystemScreen(Screen screen) {
		if (this.minecraft != null) {
			if (shouldPlaySystemSounds())
				SystemGuiSounds.switchInsideSystem();
			this.minecraft.setScreen(screen);
		}
	}

	protected boolean allowsNonSystemAccess() {
		return false;
	}

	protected boolean shouldPlaySystemSounds() {
		return SystemPlayerAccess.hasSystem(this.minecraft == null ? null : this.minecraft.player);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!isOpen())
			return true; // swallow clicks during the animation
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

	private void setWidgetsVisible(boolean visible) {
		for (GuiEventListener child : this.children()) {
			if (child instanceof AbstractWidget widget) {
				widget.visible = visible;
				widget.active = visible;
			}
		}
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
		if (accessDenied)
			return;
		updateAnimation();
		if (closed)
			return;
		setWidgetsVisible(state == State.OPEN);
		ResponsiveGuiScale.Transform transform = responsiveTransform();
		int logicalMouseX = transform.logicalMouseX(mouseX);
		int logicalMouseY = transform.logicalMouseY(mouseY);

		this.renderBackground(g);
		g.flush();
		ResponsiveGuiScale.push(g, transform);

		int ax = leftPos + pRelX, ay = topPos + pRelY;
		int centerY = ay + pH / 2;
		int halfH = Math.round((pH / 2f + 4f) * reveal);
		int top = centerY - halfH, bottom = centerY + halfH;
		int sx0 = ax - 3, sx1 = ax + pW + 3;

		ResponsiveGuiScale.enableScissor(g, transform, sx0, top, sx1, bottom);
		drawShaderBackground(g, ax, ay, logicalMouseX, logicalMouseY);
		super.render(g, logicalMouseX, logicalMouseY, partialTicks); // renderBg (panel+cells) + slots/items + labels + widgets
		g.disableScissor();

		if (reveal < 1.0f) {
			g.fill(sx0, top, sx1, top + 1, ACCENT);
			g.fill(sx0, bottom - 1, sx1, bottom, ACCENT);
			g.fill(sx0, top + 1, sx1, top + 2, ACCENT_SOFT);
			g.fill(sx0, bottom - 2, sx1, bottom - 1, ACCENT_SOFT);
		}

		if (state == State.OPEN) {
			renderExtras(g, logicalMouseX, logicalMouseY);
		}
		ResponsiveGuiScale.pop(g);

		if (state == State.OPEN)
			this.renderTooltip(g, mouseX, mouseY);
	}

	protected ResponsiveGuiScale.Transform responsiveTransform() {
		return ResponsiveGuiScale.fit(this.width, this.height, pW + 8, pH + 8);
	}

	protected double logicalMouseX(double mouseX) {
		return responsiveTransform().logicalX(mouseX);
	}

	protected double logicalMouseY(double mouseY) {
		return responsiveTransform().logicalY(mouseY);
	}

	/** Subclass hook for hover highlights / extra tooltips, drawn on top when fully open. */
	protected void renderExtras(GuiGraphics g, int mouseX, int mouseY) {
	}

	// ── background ─────────────────────────────────────────────────────────────

	private void drawShaderBackground(GuiGraphics g, int ax, int ay, int mouseX, int mouseY) {
		float localX = clamp01((mouseX - ax) / (float) pW);
		float localY = clamp01((mouseY - ay) / (float) pH);

		ShaderInstance shader = SystemBackgroundRenderTypes.get();
		if (shader == null) {
			g.fillGradient(ax, ay, ax + pW, ay + pH, 0xF0060D1F, 0xF0010209);
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

		Matrix4f matrix = g.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, ax, ay + pH, 0).uv(0f, 1f).endVertex();
		buffer.vertex(matrix, ax + pW, ay + pH, 0).uv(1f, 1f).endVertex();
		buffer.vertex(matrix, ax + pW, ay, 0).uv(1f, 0f).endVertex();
		buffer.vertex(matrix, ax, ay, 0).uv(0f, 0f).endVertex();
		Tesselator.getInstance().end();

		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static float clamp01(float v) {
		return v < 0f ? 0f : (v > 1f ? 1f : v);
	}
}
