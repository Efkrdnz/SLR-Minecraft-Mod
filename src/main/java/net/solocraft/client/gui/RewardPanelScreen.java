package net.solocraft.client.gui;

import net.solocraft.world.inventory.RewardPanelMenu;
import net.solocraft.procedures.RewardNameReturnProcedure;
import net.solocraft.procedures.RewardNameReturn3Procedure;
import net.solocraft.procedures.RewardNameReturn2Procedure;
import net.solocraft.network.RewardPanelButtonMessage;
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

public class RewardPanelScreen extends AbstractContainerScreen<RewardPanelMenu> {
	private final static HashMap<String, Object> guistate = RewardPanelMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_fullrecovery;
	ImageButton imagebutton_lootbox;
	ImageButton imagebutton_skillpoints10;

	public RewardPanelScreen(RewardPanelMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/reward_panel.png");

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

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_vertical2.png"), this.leftPos + -99, this.topPos + -113, 0, 0, 200, 225, 200, 225);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.reward_panel.label_ssfrewards"), -23, -68, -1, false);
		guiGraphics.drawString(this.font,

				RewardNameReturnProcedure.execute(entity), -43, 4, -1, false);
		guiGraphics.drawString(this.font,

				RewardNameReturn2Procedure.execute(entity), -43, 30, -1, false);
		guiGraphics.drawString(this.font,

				RewardNameReturn3Procedure.execute(entity), -43, 56, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_fullrecovery = new ImageButton(this.leftPos + -48, this.topPos + 24, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_fullrecovery.png"), 96, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new RewardPanelButtonMessage(0, x, y, z));
				RewardPanelButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_fullrecovery", imagebutton_fullrecovery);
		this.addRenderableWidget(imagebutton_fullrecovery);
		imagebutton_lootbox = new ImageButton(this.leftPos + -48, this.topPos + -2, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_lootbox.png"), 96, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new RewardPanelButtonMessage(1, x, y, z));
				RewardPanelButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_lootbox", imagebutton_lootbox);
		this.addRenderableWidget(imagebutton_lootbox);
		imagebutton_skillpoints10 = new ImageButton(this.leftPos + -48, this.topPos + 50, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_skillpoints10.png"), 96, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new RewardPanelButtonMessage(2, x, y, z));
				RewardPanelButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_skillpoints10", imagebutton_skillpoints10);
		this.addRenderableWidget(imagebutton_skillpoints10);
	}
}
