package net.solocraft.client.gui;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.shader.ShadowSummonBackgroundRenderTypes;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.network.ShadowSummonGUIButtonMessage;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.world.inventory.ShadowSummonGUIMenu;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ShadowSummonGUIScreen extends AbstractContainerScreen<ShadowSummonGUIMenu> {
	private final static HashMap<String, Object> guistate = ShadowSummonGUIMenu.guistate;

	private static final int PANEL_W = 420;
	private static final int PANEL_H = 292;
	private static final int ACCENT = 0xFFB75CFF;
	private static final int ACCENT_BLUE = 0xFF43C8FF;
	private static final int ACCENT_DIM = 0xFF6A2D98;
	private static final int TEXT_MAIN = 0xFFECEBFF;
	private static final int TEXT_SUB = 0xFF9CA7D5;

	private static final int NORMAL_X = 18;
	private static final int NORMAL_Y = 51;
	private static final int NORMAL_W = 248;
	private static final int NORMAL_H = 142;
	private static final int BOSS_X = 284;
	private static final int BOSS_Y = 51;
	private static final int BOSS_W = 118;
	private static final int BOSS_H = 142;
	private static final int BUTTON_H = 27;
	private static final int BUTTON_GAP = 8;
	private static final long ANIM_MS = 190L;
	private static final float OPEN_SOUND_PITCH = 0.78F;
	private static final float CLOSE_SOUND_PITCH = 0.72F;
	private static final float SOUND_VOLUME = 0.46F;

	private final Level world;
	private final int x;
	private final int y;
	private final int z;
	private final Player entity;
	private final List<SummonEntry> normalEntries = new ArrayList<>();
	private final List<SummonEntry> bossEntries = new ArrayList<>();
	private final List<SummonButton> summonButtons = new ArrayList<>();

	private ControlButton formationModeButton;
	private ControlButton saveFormationButton;
	private ControlButton dismissButton;
	private EditBox formationNameBox;
	private int normalScroll;
	private int bossScroll;
	private boolean formationMode = false;
	private State state = State.OPENING;
	private long animStart;
	private boolean closed;
	private float reveal;

	public ShadowSummonGUIScreen(ShadowSummonGUIMenu container, Inventory inventory, Component text) {
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
		this.renderBackground(guiGraphics);
		layoutSummonButtons();
		guiGraphics.flush();
		int centerY = topPos + imageHeight / 2;
		int halfH = Math.round((imageHeight / 2f + 4f) * reveal);
		int top = centerY - halfH;
		int bottom = centerY + halfH;
		int sx0 = leftPos - 3;
		int sx1 = leftPos + imageWidth + 3;
		guiGraphics.enableScissor(sx0, top, sx1, bottom);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.disableScissor();
		if (reveal < 1.0f) {
			guiGraphics.fill(sx0, top, sx1, top + 1, ACCENT);
			guiGraphics.fill(sx0, bottom - 1, sx1, bottom, ACCENT);
			guiGraphics.fill(sx0, top + 1, sx1, top + 2, 0x77B75CFF);
			guiGraphics.fill(sx0, bottom - 2, sx1, bottom - 1, 0x7743C8FF);
		}
		if (state == State.OPEN)
			this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		layoutSummonButtons();
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
		guiGraphics.drawString(this.font, "NORMAL UNITS", 18, 37, ACCENT_BLUE, false);
		guiGraphics.drawString(this.font, "BOSS SHADOWS", 286, 37, ACCENT, false);
		guiGraphics.drawString(this.font, "FORMATION", 18, 207, ACCENT_BLUE, false);
		guiGraphics.drawString(this.font, formationMode ? "Name current layout." : "Toggle to save current layout.", 108, 207, TEXT_SUB, false);
		guiGraphics.drawString(this.font, "MANAGE", 18, 260, ACCENT_BLUE, false);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		if (state != State.OPEN)
			return true;
		if (isOverBox(mouseX, mouseY, leftPos + NORMAL_X, topPos + NORMAL_Y, NORMAL_W, NORMAL_H)) {
			int max = maxScroll(visibleEntries(normalEntries).size(), visibleNormalRows(), true);
			normalScroll = clamp(normalScroll - (int) Math.signum(delta), 0, max);
			layoutSummonButtons();
			return true;
		}
		if (isOverBox(mouseX, mouseY, leftPos + BOSS_X, topPos + BOSS_Y, BOSS_W, BOSS_H)) {
			int max = maxScroll(visibleEntries(bossEntries).size(), visibleBossRows(), false);
			bossScroll = clamp(bossScroll - (int) Math.signum(delta), 0, max);
			layoutSummonButtons();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (formationNameBox != null && formationNameBox.visible && formationNameBox.isFocused()) {
			if (key == 256) {
				beginClose();
				return true;
			}
			return formationNameBox.keyPressed(key, scanCode, modifiers);
		}
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
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		if (state != State.OPEN)
			return true;
		if (formationNameBox != null && formationNameBox.visible && formationNameBox.isFocused())
			return formationNameBox.charTyped(codePoint, modifiers);
		return super.charTyped(codePoint, modifiers);
	}

	@Override
	public void init() {
		super.init();
		this.state = State.OPENING;
		this.animStart = Util.getMillis();
		this.closed = false;
		this.reveal = 0.0F;
		playPanelSound(SololevelingModSounds.PANELOPEN.get(), OPEN_SOUND_PITCH);
		normalEntries.clear();
		bossEntries.clear();
		summonButtons.clear();
		addEntries();
		for (SummonEntry entry : normalEntries)
			addSummonButton(entry, false);
		for (SummonEntry entry : bossEntries)
			addSummonButton(entry, true);

		formationModeButton = new ControlButton(this.leftPos + 18, this.topPos + 226, 104, 20, Component.literal("Formation: OFF"), b -> toggleFormationMode());
		guistate.put("button:button_formation_mode", formationModeButton);
		this.addRenderableWidget(formationModeButton);

		formationNameBox = new EditBox(this.font, this.leftPos + 132, this.topPos + 227, 166, 18, Component.literal("Formation Name"));
		formationNameBox.setMaxLength(24);
		formationNameBox.setValue("Formation");
		formationNameBox.setTextColor(TEXT_MAIN);
		formationNameBox.setBordered(false);
		formationNameBox.visible = false;
		this.addRenderableWidget(formationNameBox);

		saveFormationButton = new ControlButton(this.leftPos + 306, this.topPos + 226, 96, 20, Component.literal("Save Formation"), b -> saveFormation());
		saveFormationButton.visible = false;
		guistate.put("button:button_save_formation", saveFormationButton);
		this.addRenderableWidget(saveFormationButton);

		dismissButton = new ControlButton(this.leftPos + 306, this.topPos + 258, 96, 20, Component.literal("Dismiss"), b -> openDismiss());
		guistate.put("button:button_shadow_dismiss", dismissButton);
		this.addRenderableWidget(dismissButton);
		layoutSummonButtons();
	}

	@Override
	public void onClose() {
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

	private void addEntries() {
		normalEntries.add(new SummonEntry(0, "Goblin Fighter", "goblin_club"));
		normalEntries.add(new SummonEntry(1, "Goblin Archer", "goblin_archer"));
		normalEntries.add(new SummonEntry(2, "Goblin Mage", "goblin_mage"));
		normalEntries.add(new SummonEntry(3, "Lycan", "wolf"));
		normalEntries.add(new SummonEntry(4, "Knight", "knight"));
		normalEntries.add(new SummonEntry(5, "Polar Bear", "polar_bear"));
		normalEntries.add(new SummonEntry(6, "Orc", "orc"));
		normalEntries.add(new SummonEntry(10, "High Orc", "high_orc"));
		bossEntries.add(new SummonEntry(7, "Igris", "igris"));
		bossEntries.add(new SummonEntry(8, "Beru", "beru"));
		bossEntries.add(new SummonEntry(9, "Kamish", "kamish"));
		bossEntries.add(new SummonEntry(11, "Tusk", "tusk"));
		bossEntries.add(new SummonEntry(12, "Kaisel", "kaisel"));
	}

	private void addSummonButton(SummonEntry entry, boolean boss) {
		SummonButton button = new SummonButton(0, 0, boss ? BOSS_W - 12 : 118, BUTTON_H, entry, boss, b -> summon(entry));
		button.visible = false;
		summonButtons.add(button);
		guistate.put("button:shadow_summon_" + entry.id, button);
		this.addRenderableWidget(button);
	}

	private void layoutSummonButtons() {
		List<SummonEntry> visibleNormals = visibleEntries(normalEntries);
		List<SummonEntry> visibleBosses = visibleEntries(bossEntries);
		normalScroll = clamp(normalScroll, 0, maxScroll(visibleNormals.size(), visibleNormalRows(), true));
		bossScroll = clamp(bossScroll, 0, maxScroll(visibleBosses.size(), visibleBossRows(), false));
		for (SummonButton button : summonButtons) {
			button.visible = false;
			button.active = false;
		}
		layoutNormalButtons(visibleNormals);
		layoutBossButtons(visibleBosses);
	}

	private void layoutNormalButtons(List<SummonEntry> entries) {
		int start = normalScroll * 2;
		int end = Math.min(entries.size(), start + visibleNormalRows() * 2);
		for (int i = start; i < end; i++) {
			SummonButton button = buttonFor(entries.get(i));
			if (button == null)
				continue;
			int local = i - start;
			int col = local % 2;
			int row = local / 2;
			int bx = leftPos + NORMAL_X + 6 + col * 124;
			int by = topPos + NORMAL_Y + row * (BUTTON_H + BUTTON_GAP);
			button.setPosition(bx, by);
			button.setClip(leftPos + NORMAL_X, topPos + NORMAL_Y, NORMAL_W, NORMAL_H);
			button.visible = true;
			button.active = true;
		}
	}

	private void layoutBossButtons(List<SummonEntry> entries) {
		int start = bossScroll;
		int end = Math.min(entries.size(), start + visibleBossRows());
		for (int i = start; i < end; i++) {
			SummonButton button = buttonFor(entries.get(i));
			if (button == null)
				continue;
			int row = i - start;
			button.setPosition(leftPos + BOSS_X + 6, topPos + BOSS_Y + row * (BUTTON_H + BUTTON_GAP));
			button.setClip(leftPos + BOSS_X, topPos + BOSS_Y, BOSS_W, BOSS_H);
			button.visible = true;
			button.active = true;
		}
	}

	private SummonButton buttonFor(SummonEntry entry) {
		for (SummonButton button : summonButtons) {
			if (button.entry == entry)
				return button;
		}
		return null;
	}

	private List<SummonEntry> visibleEntries(List<SummonEntry> entries) {
		List<SummonEntry> visible = new ArrayList<>();
		for (SummonEntry entry : entries) {
			if (ShadowMonarchManager.hasShadowForDisplay(entity, entry.type))
				visible.add(entry);
		}
		return visible;
	}

	private int visibleNormalRows() {
		return Math.max(1, NORMAL_H / (BUTTON_H + BUTTON_GAP));
	}

	private int visibleBossRows() {
		return Math.max(1, BOSS_H / (BUTTON_H + BUTTON_GAP));
	}

	private int maxScroll(int entryCount, int visibleRows, boolean twoColumns) {
		int rows = twoColumns ? (int) Math.ceil(entryCount / 2.0D) : entryCount;
		return Math.max(0, rows - visibleRows);
	}

	private void summon(SummonEntry entry) {
		if (!ShadowMonarchManager.hasShadowForDisplay(entity, entry.type))
			return;
		String payload = hasShiftDown() ? "all" : "";
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(entry.id, x, y, z, payload));
	}

	private void toggleFormationMode() {
		formationMode = !formationMode;
		formationModeButton.setMessage(Component.literal(formationMode ? "Formation: ON" : "Formation: OFF"));
		formationNameBox.visible = formationMode;
		formationNameBox.setFocused(formationMode);
		saveFormationButton.visible = formationMode;
		if (formationMode)
			this.setInitialFocus(formationNameBox);
	}

	private void saveFormation() {
		if (!formationMode)
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(100, x, y, z, formationNameBox.getValue()));
		ShadowSummonGUIButtonMessage.handleButtonAction(entity, 100, x, y, z, formationNameBox.getValue());
	}

	private void openDismiss() {
		SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(101, x, y, z));
		ShadowSummonGUIButtonMessage.handleButtonAction(entity, 101, x, y, z);
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
		String title = "[ SHADOW SUMMON ]";
		g.drawString(this.font, title, x + (w - this.font.width(title)) / 2, y + 7, ACCENT, false);
	}

	private void renderSections(GuiGraphics g) {
		int x = leftPos;
		int y = topPos;
		outline(g, x + 12, y + 32, 254, 170, 0x7743C8FF);
		outline(g, x + 278, y + 32, 130, 170, 0x88B75CFF);
		outline(g, x + 12, y + 218, 396, 32, 0x7743C8FF);
		outline(g, x + 12, y + 254, 396, 26, 0x7743C8FF);
		g.fill(x + 13, y + 33, x + 265, y + 47, 0x33124B76);
		g.fill(x + 279, y + 33, x + 407, y + 47, 0x331D0B31);
		g.fill(x + 13, y + 219, x + 407, y + 225, 0x33124B76);
		g.fill(x + 13, y + 255, x + 407, y + 261, 0x33124B76);
		drawScrollHint(g, x + 256, y + 52, y + 191, visibleEntries(normalEntries).size(), visibleNormalRows(), normalScroll, true);
		drawScrollHint(g, x + 397, y + 52, y + 191, visibleEntries(bossEntries).size(), visibleBossRows(), bossScroll, false);
		if (formationMode) {
			outline(g, x + 129, y + 224, 172, 24, ACCENT_DIM);
			g.fill(x + 130, y + 225, x + 300, y + 247, 0x55102338);
		}
	}

	private void drawScrollHint(GuiGraphics g, int x, int y0, int y1, int count, int rows, int scroll, boolean twoColumns) {
		int rowCount = twoColumns ? (int) Math.ceil(count / 2.0D) : count;
		if (rowCount <= rows)
			return;
		g.fill(x, y0, x + 2, y1, 0x443FC6FF);
		int trackH = y1 - y0;
		int knobH = Math.max(16, trackH * rows / rowCount);
		int knobY = y0 + (trackH - knobH) * scroll / Math.max(1, rowCount - rows);
		g.fill(x, knobY, x + 2, knobY + knobH, twoColumns ? ACCENT_BLUE : ACCENT);
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

	private static boolean isOverBox(double mouseX, double mouseY, int x, int y, int w, int h) {
		return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
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

	private record SummonEntry(int id, String name, String type) {
	}

	private static class SummonButton extends Button {
		private final SummonEntry entry;
		private final boolean boss;
		private int clipX;
		private int clipY;
		private int clipW;
		private int clipH;

		SummonButton(int x, int y, int w, int h, SummonEntry entry, boolean boss, OnPress onPress) {
			super(x, y, w, h, Component.literal(entry.name), onPress, DEFAULT_NARRATION);
			this.entry = entry;
			this.boss = boss;
		}

		void setClip(int x, int y, int w, int h) {
			this.clipX = x;
			this.clipY = y;
			this.clipW = w;
			this.clipH = h;
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
			if (!visible)
				return;
			g.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
			boolean hovered = this.isHoveredOrFocused();
			int border = hovered ? 0xFFFFFFFF : (boss ? ACCENT : ACCENT_BLUE);
			g.fill(getX(), getY(), getX() + width, getY() + height, hovered ? 0x884A1D68 : 0x55102338);
			outline(g, getX(), getY(), width, height, border);
			Font font = Minecraft.getInstance().font;
			g.drawString(font, entry.name, getX() + 7, getY() + 5, TEXT_MAIN, false);
			Player player = Minecraft.getInstance().player;
			String count = player == null ? "0/0" : ShadowMonarchManager.shadowCountText(player, entry.type);
			g.drawString(font, count, getX() + width - font.width(count) - 7, getY() + 16, boss ? 0xFFDBA6FF : 0xFF9EDFFF, false);
			g.disableScissor();
		}
	}

	private static class ControlButton extends Button {
		ControlButton(int x, int y, int w, int h, Component label, OnPress onPress) {
			super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
			boolean hovered = this.isHoveredOrFocused();
			int border = hovered ? 0xFFFFFFFF : ACCENT_BLUE;
			g.fill(getX(), getY(), getX() + width, getY() + height, hovered ? 0x8843C8FF : 0x55102338);
			outline(g, getX(), getY(), width, height, border);
			Font font = Minecraft.getInstance().font;
			g.drawCenteredString(font, getMessage(), getX() + width / 2, getY() + (height - 8) / 2, hovered ? 0xFFFFFFFF : TEXT_MAIN);
		}
	}
}
