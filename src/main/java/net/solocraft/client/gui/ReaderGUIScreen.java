package net.solocraft.client.gui;

import net.solocraft.world.inventory.ReaderGUIMenu;
import net.solocraft.procedures.ReturnClosestGateZProcedure;
import net.solocraft.procedures.ReturnClosestGateYProcedure;
import net.solocraft.procedures.ReturnClosestGateXProcedure;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class ReaderGUIScreen extends AbstractContainerScreen<ReaderGUIMenu> {
	private final static HashMap<String, Object> guistate = ReaderGUIMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;

	public ReaderGUIScreen(ReaderGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 119;
		this.imageHeight = 100;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/reader_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui_gate.png"), this.leftPos + 7, this.topPos + 10, 0, 0, 25, 25, 25, 25);

		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.reader_gui.label_gate"), 38, 17, -13421569, false);
		guiGraphics.drawString(this.font,

				ReturnClosestGateXProcedure.execute(world, x, y, z), 16, 43, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnClosestGateYProcedure.execute(world, x, y, z), 16, 60, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnClosestGateZProcedure.execute(world, x, y, z), 16, 76, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.reader_gui.label_x"), 6, 43, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.reader_gui.label_y"), 6, 60, -12829636, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.reader_gui.label_z"), 6, 77, -12829636, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
	}
}
