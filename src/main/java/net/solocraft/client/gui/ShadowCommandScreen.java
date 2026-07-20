package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.shader.ShadowSummonBackgroundRenderTypes;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.network.ShadowCommandButtonMessage;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.world.inventory.ShadowCommandMenu;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

import java.util.HashMap;

public class ShadowCommandScreen extends AbstractContainerScreen<ShadowCommandMenu> {
	private final static HashMap<String, Object> guistate = ShadowCommandMenu.guistate;

	private static final int PANEL_W = 420;
	private static final int PANEL_H = 214;
	private static final int ACCENT = 0xFFB75CFF;
	private static final int ACCENT_BLUE = 0xFF43C8FF;
	private static final int ACCENT_DIM = 0xFF6A2D98;
	private static final int TEXT_MAIN = 0xFFECEBFF;
	private static final int TEXT_SUB = 0xFF9CA7D5;
	private static final long ANIM_MS = 190L;
	private static final float OPEN_SOUND_PITCH = 0.74F;
	private static final float CLOSE_SOUND_PITCH = 0.68F;
	private static final float SOUND_VOLUME = 0.46F;

	private final Level world;
	private final int x;
	private final int y;
	private final int z;
	private final Player entity;
	private CommandButton clearDungeonButton;
	private State state = State.OPENING;
	private long animStart;
	private boolean closed;
	private float reveal;

	public ShadowCommandScreen(ShadowCommandMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
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
		boolean unavailableHovered = state == State.OPEN && clearDungeonButton != null && !clearDungeonButton.active
				&& clearDungeonButton.isMouseOver(logicalMouseX, logicalMouseY);
		ResponsiveGuiScale.pop(guiGraphics);
		if (state == State.OPEN) {
			if (unavailableHovered)
				guiGraphics.renderTooltip(this.font, Component.literal("Only available inside a dungeon"), mouseX, mouseY);
			this.renderTooltip(guiGraphics, mouseX, mouseY);
		}
	}

	private ResponsiveGuiScale.Transform responsiveTransform() {
		return ResponsiveGuiScale.fit(this.width, this.height, PANEL_W + 8, PANEL_H + 8);
	}

	private double logicalMouseX(double mouseX) {
		return responsiveTransform().logicalX(mouseX);
	}

	private double logicalMouseY(double mouseY) {
		return responsiveTransform().logicalY(mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		renderAnimatedBackground(guiGraphics, mouseX, mouseY);
		renderFrame(guiGraphics);
		renderSections(guiGraphics);
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, "COMMAND MODES", 18, 37, ACCENT_BLUE, false);
		guiGraphics.drawString(this.font, "SPECIAL ORDER", 18, 154, ACCENT, false);
		guiGraphics.drawString(this.font, "ALL SUMMONED SHADOWS", 112, 154, TEXT_SUB, false);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (clearDungeonButton != null)
			clearDungeonButton.active = ShadowMonarchManager.isInDungeon(entity);
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

	@Override
	public void init() {
		super.init();
		this.state = State.OPENING;
		this.animStart = Util.getMillis();
		this.closed = false;
		this.reveal = 0.0F;
		playPanelSound(SololevelingModSounds.PANELOPEN.get(), OPEN_SOUND_PITCH);
		int left = this.leftPos + 18;
		int top = this.topPos + 54;
		int buttonW = 184;
		int buttonH = 27;
		addCommandButton("Default", 0, left, top, buttonW, buttonH, false);
		addCommandButton("Protect", 1, left + 202, top, buttonW, buttonH, false);
		addCommandButton("Berserk", 2, left, top + 37, buttonW, buttonH, true);
		addCommandButton("Follow", 3, left + 202, top + 37, buttonW, buttonH, false);
		clearDungeonButton = addCommandButton("Clear Dungeon", 4, left, top + 116, 386, buttonH, true);
		clearDungeonButton.active = ShadowMonarchManager.isInDungeon(entity);
	}

	@Override
	public void onClose() {
		beginClose();
	}

	private CommandButton addCommandButton(String label, int id, int x, int y, int w, int h, boolean bossAccent) {
		CommandButton button = new CommandButton(x, y, w, h, Component.literal(label), bossAccent, b -> sendCommand(id));
		guistate.put("button:shadow_command_" + id, button);
		this.addRenderableWidget(button);
		return button;
	}

	private void sendCommand(int buttonId) {
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowCommandButtonMessage(buttonId, x, y, z));
		beginClose();
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
		String title = "[ SHADOW COMMAND ]";
		g.drawString(this.font, title, x + (w - this.font.width(title)) / 2, y + 7, ACCENT, false);
	}

	private void renderSections(GuiGraphics g) {
		int x = leftPos;
		int y = topPos;
		outline(g, x + 12, y + 32, 396, 108, 0x7743C8FF);
		outline(g, x + 12, y + 151, 396, 55, 0x88B75CFF);
		g.fill(x + 13, y + 33, x + 407, y + 47, 0x33124B76);
		g.fill(x + 13, y + 152, x + 407, y + 166, 0x331D0B31);
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

	private static float clamp01(float v) {
		return v < 0f ? 0f : Math.min(v, 1f);
	}

	private static void outline(GuiGraphics g, int x, int y, int w, int h, int color) {
		g.fill(x, y, x + w, y + 1, color);
		g.fill(x, y + h - 1, x + w, y + h, color);
		g.fill(x, y, x + 1, y + h, color);
		g.fill(x + w - 1, y, x + w, y + h, color);
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

	private enum State {
		OPENING, OPEN, CLOSING
	}

	private static class CommandButton extends Button {
		private final boolean bossAccent;

		CommandButton(int x, int y, int w, int h, Component label, boolean bossAccent, OnPress onPress) {
			super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
			this.bossAccent = bossAccent;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
			boolean hovered = this.isHoveredOrFocused();
			int accent = bossAccent ? ACCENT : ACCENT_BLUE;
			int border = !active ? 0x66545B74 : hovered ? 0xFFFFFFFF : accent;
			int fill = !active ? 0x3310131F : hovered ? (bossAccent ? 0x884A1D68 : 0x8843C8FF) : 0x55102338;
			g.fill(getX(), getY(), getX() + width, getY() + height, fill);
			outline(g, getX(), getY(), width, height, border);
			Font font = Minecraft.getInstance().font;
			int color = !active ? 0xFF6D728C : hovered ? 0xFFFFFFFF : TEXT_MAIN;
			g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, color);
		}
	}
}
