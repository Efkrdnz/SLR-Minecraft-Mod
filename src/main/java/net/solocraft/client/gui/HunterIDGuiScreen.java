package net.solocraft.client.gui;

import net.solocraft.world.inventory.HunterIDGuiMenu;
import net.solocraft.procedures.ReturnIDRankProcedure;
import net.solocraft.procedures.ReturnIDPersonProcedure;
import net.solocraft.procedures.ReturnIDClassProcedure;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class HunterIDGuiScreen extends AbstractContainerScreen<HunterIDGuiMenu> {
	private final static HashMap<String, Object> guistate = HunterIDGuiMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;

	public HunterIDGuiScreen(HunterIDGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/hunter_id_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + 41 && mouseX < leftPos + 65 && mouseY > topPos + 25 && mouseY < topPos + 42)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.hunter_id_gui.tooltip_certified_hunter"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/idcard1.png"), this.leftPos + -88, this.topPos + -55, 0, 0, 176, 110, 176, 110);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.hunter_id_gui.label_hunters_association"), -55, -40, -13421569, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.hunter_id_gui.label_hunter_id_card"), -46, -31, -13421569, false);
		guiGraphics.drawString(this.font,

				ReturnIDPersonProcedure.execute(entity), -76, 0, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnIDRankProcedure.execute(entity), -76, 16, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnIDClassProcedure.execute(entity), -76, 33, -12829636, false);
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
