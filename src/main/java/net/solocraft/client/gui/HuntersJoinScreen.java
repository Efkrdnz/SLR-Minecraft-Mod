package net.solocraft.client.gui;

import net.solocraft.world.inventory.HuntersJoinMenu;
import net.solocraft.procedures.HuntersLevelReqProcedure;
import net.solocraft.network.HuntersJoinButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class HuntersJoinScreen extends AbstractContainerScreen<HuntersJoinMenu> {
	private final static HashMap<String, Object> guistate = HuntersJoinMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_guildjoin;

	public HuntersJoinScreen(HuntersJoinMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

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

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/base.png"), this.leftPos + -76, this.topPos + -113, 0, 0, 150, 225, 150, 225);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.hunters_join.label_are_you_sure_you_want"), -57, -101, -13210, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.hunters_join.label_to_join_ahjin_guild"), -58, -87, -13210, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.hunters_join.label_empty"), -16, -59, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.hunters_join.label_x2_xp_boost"), -46, -45, -10027162, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.hunters_join.label_major_speed_boost"), -54, -31, -10027162, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.hunters_join.label_requirements"), -34, -5, -1, false);
		guiGraphics.drawString(this.font,

				HuntersLevelReqProcedure.execute(entity), -27, 17, -12829636, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_guildjoin = new ImageButton(this.leftPos + -13, this.topPos + 73, 28, 12, 0, 0, 12, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_guildjoin.png"), 28, 24, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new HuntersJoinButtonMessage(0, x, y, z));
				HuntersJoinButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_guildjoin", imagebutton_guildjoin);
		this.addRenderableWidget(imagebutton_guildjoin);
	}
}
