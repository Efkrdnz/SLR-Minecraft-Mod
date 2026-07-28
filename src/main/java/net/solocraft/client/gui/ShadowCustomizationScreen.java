package net.solocraft.client.gui;

import net.solocraft.client.renderer.shader.ShadowSummonBackgroundRenderTypes;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.world.inventory.ShadowCustomizationMenu;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
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
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

/**
 * Inventory-backed artifact configuration for Igris and Tusk.
 *
 * <p>This screen deliberately contains no entity preview. Equipping an artifact
 * changes combat behavior only; it does not alter a shadow's rendered model.</p>
 */
public class ShadowCustomizationScreen
		extends AbstractContainerScreen<ShadowCustomizationMenu> {
	private static final int PANEL_W = 420;
	private static final int PANEL_H = 270;
	private static final int ACCENT = 0xFFB75CFF;
	private static final int ACCENT_BLUE = 0xFF43C8FF;
	private static final int ACCENT_DIM = 0xFF6A2D98;
	private static final int TEXT_MAIN = 0xFFECEBFF;
	private static final int TEXT_SUB = 0xFF9CA7D5;
	private static final int GOLD = 0xFFFFC84A;
	private static final long ANIM_MS = 190L;
	private static final float OPEN_SOUND_PITCH = 0.78F;
	private static final float CLOSE_SOUND_PITCH = 0.72F;
	private static final float SOUND_VOLUME = 0.46F;

	private State state = State.OPENING;
	private long animStart;
	private boolean closed;
	private float reveal;

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
		playPanelSound(SololevelingModSounds.PANELOPEN.get(), OPEN_SOUND_PITCH);
	}

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY,
			float partialTicks) {
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
		int halfH = Math.round((imageHeight / 2.0F + 4.0F) * reveal);
		int top = centerY - halfH;
		int bottom = centerY + halfH;
		int sx0 = leftPos - 3;
		int sx1 = leftPos + imageWidth + 3;
		ResponsiveGuiScale.enableScissor(guiGraphics, transform, sx0, top, sx1,
				bottom);
		super.render(guiGraphics, logicalMouseX, logicalMouseY, partialTicks);
		guiGraphics.disableScissor();
		if (reveal < 1.0F) {
			guiGraphics.fill(sx0, top, sx1, top + 1, ACCENT);
			guiGraphics.fill(sx0, bottom - 1, sx1, bottom, ACCENT);
			guiGraphics.fill(sx0, top + 1, sx1, top + 2, 0x77B75CFF);
			guiGraphics.fill(sx0, bottom - 2, sx1, bottom - 1, 0x7743C8FF);
		}
		ResponsiveGuiScale.pop(guiGraphics);
		if (state == State.OPEN)
			this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks,
			int mouseX, int mouseY) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		renderAnimatedBackground(guiGraphics, mouseX, mouseY);
		renderFrame(guiGraphics);
		renderSections(guiGraphics);
		renderSlotBackgrounds(guiGraphics);
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX,
			int mouseY) {
		String boss = bossName();
		int rank = menu.shadowRank();
		int level = menu.shadowLevel();
		String rankName = ShadowMonarchManager.rankDisplayName(rank);
		guiGraphics.drawString(this.font, "BOSS ARTIFACT", 26, 38, ACCENT,
				false);
		guiGraphics.drawString(this.font, boss + "  |  Lv." + level, 26, 50,
				TEXT_MAIN, false);

		String rankContext;
		if (menu.isMaxRank()) {
			rankContext = rankName + "  |  MAX RANK";
		} else {
			rankContext = rankName + " -> "
					+ ShadowMonarchManager.rankDisplayName(menu.nextRank());
		}
		int rankWidth = this.font.width(rankContext);
		guiGraphics.drawString(this.font, rankContext,
				imageWidth - rankWidth - 26, 50,
				ShadowMonarchManager.rankColor(rank), false);

		String progressText;
		if (menu.isMaxRank()) {
			progressText = "MAX RANK";
		} else if (menu.isAtLevelCap()) {
			progressText = "LEVEL CAP  |  " + menu.rankXp() + " / "
					+ menu.rankXpNeeded() + " XP";
		} else {
			progressText = menu.rankXp() + " / " + menu.rankXpNeeded()
					+ " XP TO NEXT RANK";
		}
		guiGraphics.drawCenteredString(this.font, progressText, imageWidth / 2,
				71, menu.isAtLevelCap() && !menu.isMaxRank()
						? 0xFFFFC85C : TEXT_SUB);

		guiGraphics.drawString(this.font, "ARTIFACT SLOT", 123, 86,
				ACCENT_BLUE, false);
		boolean equipped = hasEquippedArtifact();
		guiGraphics.drawString(this.font, equipped ? "EQUIPPED" : "EMPTY",
				226, 86, equipped ? GOLD : TEXT_SUB, false);
		guiGraphics.drawString(this.font, "Allowed: " + allowedArtifact(), 38,
				108, TEXT_MAIN, false);
		guiGraphics.drawString(this.font, "Effect: " + artifactEffect(), 38,
				121, effectColor(), false);
		guiGraphics.drawCenteredString(this.font,
				"Insert or remove the artifact to update this shadow.",
				imageWidth / 2, 136, TEXT_SUB);

		guiGraphics.drawString(this.font, "INVENTORY",
				ShadowCustomizationMenu.PLAYER_INVENTORY_X, 160, ACCENT_BLUE,
				false);
	}

	@Override
	public boolean keyPressed(int key, int scanCode, int modifiers) {
		if (key == 256 || this.minecraft != null
				&& this.minecraft.options.keyInventory.matches(key, scanCode)) {
			beginClose();
			return true;
		}
		return state != State.OPEN
				|| super.keyPressed(key, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (state != State.OPEN)
			return true;
		return super.mouseClicked(logicalMouseX(mouseX), logicalMouseY(mouseY),
				button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return super.mouseReleased(logicalMouseX(mouseX),
				logicalMouseY(mouseY), button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button,
			double dragX, double dragY) {
		float scale = responsiveTransform().scale();
		return super.mouseDragged(logicalMouseX(mouseX),
				logicalMouseY(mouseY), button, dragX / scale, dragY / scale);
	}

	@Override
	public void mouseMoved(double mouseX, double mouseY) {
		super.mouseMoved(logicalMouseX(mouseX), logicalMouseY(mouseY));
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
		playPanelSound(SololevelingModSounds.PANELCLOSE.get(),
				CLOSE_SOUND_PITCH);
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
			minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound,
					pitch, SOUND_VOLUME));
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
		graphics.fill(x, y, x + imageWidth, y + 22, 0x7A140921);
		graphics.fill(x, y + 22, x + imageWidth, y + 23, ACCENT);
		drawCornerBrackets(graphics, x, y, imageWidth, imageHeight);
		String title = "[ SHADOW CUSTOMIZATION ]";
		graphics.drawString(this.font, title,
				x + (imageWidth - this.font.width(title)) / 2, y + 7, ACCENT,
				false);
	}

	private void renderSections(GuiGraphics graphics) {
		int x = leftPos;
		int y = topPos;
		outline(graphics, x + 18, y + 32, imageWidth - 36, 118,
				0x88B75CFF);
		graphics.fill(x + 19, y + 33, x + imageWidth - 19, y + 45,
				0x331D0B31);
		outline(graphics, x + 118, y + 155, 184, 101, 0x7743C8FF);
		graphics.fill(x + 119, y + 156, x + 301, y + 169, 0x33124B76);

		int barX = x + 26;
		int barY = y + 63;
		int barW = imageWidth - 52;
		int xp = menu.rankXp();
		int needed = Math.max(1, menu.rankXpNeeded());
		boolean maxRank = menu.isMaxRank();
		int fill = maxRank ? barW : (int) Math.round(barW
				* Math.min(1.0D, xp / (double) needed));
		graphics.fill(barX, barY, barX + barW, barY + 6, 0xCC080510);
		if (fill > 0)
			graphics.fill(barX, barY, barX + fill, barY + 6,
					maxRank ? GOLD : ACCENT);
		outline(graphics, barX, barY, barW, 6,
				maxRank ? GOLD : ACCENT_DIM);
	}

	private void renderSlotBackgrounds(GuiGraphics graphics) {
		int x = leftPos;
		int y = topPos;
		boolean equipped = hasEquippedArtifact();
		int equipmentX = x + ShadowCustomizationMenu.EQUIPMENT_SLOT_X - 1;
		int equipmentY = y + ShadowCustomizationMenu.EQUIPMENT_SLOT_Y - 1;
		graphics.fill(equipmentX, equipmentY, equipmentX + 18,
				equipmentY + 18, equipped ? 0x88452F0E : 0xAA080510);
		outline(graphics, equipmentX, equipmentY, 18, 18,
				equipped ? GOLD : ACCENT);

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				drawInventorySlot(graphics,
						x + ShadowCustomizationMenu.PLAYER_INVENTORY_X
								+ column * 18 - 1,
						y + ShadowCustomizationMenu.PLAYER_INVENTORY_Y
								+ row * 18 - 1);
			}
		}
		for (int column = 0; column < 9; column++) {
			drawInventorySlot(graphics,
					x + ShadowCustomizationMenu.PLAYER_INVENTORY_X
							+ column * 18 - 1,
					y + ShadowCustomizationMenu.PLAYER_HOTBAR_Y - 1);
		}
	}

	private static void drawInventorySlot(GuiGraphics graphics, int x, int y) {
		graphics.fill(x, y, x + 18, y + 18, 0xAA080510);
		outline(graphics, x, y, 18, 18, 0x88547BA8);
	}

	private boolean hasEquippedArtifact() {
		Slot slot = menu.get().get(0);
		return slot != null && slot.hasItem();
	}

	private String bossName() {
		return "tusk".equals(menu.shadowType()) ? "Tusk" : "Igris";
	}

	private String allowedArtifact() {
		return "tusk".equals(menu.shadowType()) ? "Orb of Avarice"
				: "Demon King's Long Sword";
	}

	private String artifactEffect() {
		return "tusk".equals(menu.shadowType()) ? "2x attack damage"
				: "Ally-safe area lightning strikes";
	}

	private int effectColor() {
		return "tusk".equals(menu.shadowType()) ? 0xFFFF6A8D : 0xFF79DFFF;
	}

	private void renderAnimatedBackground(GuiGraphics graphics, int mouseX,
			int mouseY) {
		float localX = clamp01((mouseX - leftPos) / (float) imageWidth);
		float localY = clamp01((mouseY - topPos) / (float) imageHeight);
		ShaderInstance shader = ShadowSummonBackgroundRenderTypes.get();
		if (shader == null) {
			renderFallbackBackground(graphics, localX, localY);
			return;
		}
		RenderSystem.setShader(ShadowSummonBackgroundRenderTypes::get);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.disableCull();
		AbstractUniform mouse = shader.safeGetUniform("MousePos");
		mouse.set(localX, localY);
		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().getBuilder();
		buffer.begin(VertexFormat.Mode.QUADS,
				DefaultVertexFormat.POSITION_TEX);
		buffer.vertex(matrix, leftPos, topPos + imageHeight, 0).uv(0.0F, 1.0F)
				.endVertex();
		buffer.vertex(matrix, leftPos + imageWidth, topPos + imageHeight, 0)
				.uv(1.0F, 1.0F).endVertex();
		buffer.vertex(matrix, leftPos + imageWidth, topPos, 0)
				.uv(1.0F, 0.0F).endVertex();
		buffer.vertex(matrix, leftPos, topPos, 0).uv(0.0F, 0.0F)
				.endVertex();
		Tesselator.getInstance().end();
		RenderSystem.enableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void renderFallbackBackground(GuiGraphics graphics, float localX,
			float localY) {
		int x = leftPos;
		int y = topPos;
		graphics.fillGradient(x, y, x + imageWidth, y + imageHeight,
				0xF0060712, 0xF014071D);
		float time = (float) (Util.getMillis() % 100000L) / 1000.0F;
		int mouseX = x + (int) (localX * imageWidth);
		int mouseY = y + (int) (localY * imageHeight);
		for (int i = 0; i < 14; i++) {
			int lineY = mouseY - 38 + i * 6;
			int shift = (int) (Math.sin(time * 12.0F + i * 1.7F) * 10.0F);
			int alpha = 35 + (int) (25 * (0.5F
					+ 0.5F * Math.sin(time * 8.0F + i)));
			graphics.fill(mouseX - 54 + shift, lineY, mouseX + 54 + shift,
					lineY + 1, alpha << 24 | 0xB75CFF);
			if (i % 3 == 0)
				graphics.fill(mouseX - 44 - shift, lineY + 2,
						mouseX + 42 - shift, lineY + 3,
						alpha << 24 | 0x43C8FF);
		}
		graphics.fillGradient(x, y, x + imageWidth, y + imageHeight,
				0x00000000, 0x66000000);
	}

	private static float clamp01(float value) {
		return value < 0.0F ? 0.0F : Math.min(value, 1.0F);
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
		int length = 15;
		graphics.fill(x - 1, y - 1, x + length, y + 1, ACCENT_BLUE);
		graphics.fill(x - 1, y - 1, x + 1, y + length, ACCENT_BLUE);
		graphics.fill(x + width - length, y - 1, x + width + 1, y + 1,
				ACCENT);
		graphics.fill(x + width - 1, y - 1, x + width + 1, y + length,
				ACCENT);
		graphics.fill(x - 1, y + height - 1, x + length, y + height + 1,
				ACCENT_BLUE);
		graphics.fill(x - 1, y + height - length, x + 1, y + height + 1,
				ACCENT_BLUE);
		graphics.fill(x + width - length, y + height - 1, x + width + 1,
				y + height + 1, ACCENT);
		graphics.fill(x + width - 1, y + height - length, x + width + 1,
				y + height + 1, ACCENT);
	}

	private enum State {
		OPENING,
		OPEN,
		CLOSING
	}
}
