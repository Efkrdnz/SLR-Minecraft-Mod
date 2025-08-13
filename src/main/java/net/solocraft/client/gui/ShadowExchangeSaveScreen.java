package net.solocraft.client.gui;

import net.solocraft.world.inventory.ShadowExchangeSaveMenu;
import net.solocraft.procedures.DoesHaveWolfProcedure;
import net.solocraft.procedures.DoesHavePolarBearProcedure;
import net.solocraft.procedures.DoesHaveKnightsProcedure;
import net.solocraft.procedures.DoesHaveGoblinProcedure;
import net.solocraft.procedures.DoesHaveGoblinMageProcedure;
import net.solocraft.procedures.DoesHaveGoblinArcherProcedure;
import net.solocraft.network.ShadowExchangeSaveButtonMessage;
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

public class ShadowExchangeSaveScreen extends AbstractContainerScreen<ShadowExchangeSaveMenu> {
	private final static HashMap<String, Object> guistate = ShadowExchangeSaveMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_panel_rework_rewardbutton_purple;
	ImageButton imagebutton_panel_rework_rewardbutton_purple1;
	ImageButton imagebutton_panel_rework_rewardbutton_purple2;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl;
	ImageButton imagebutton_panel_rework_rewardbutton_purple3;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl1;

	public ShadowExchangeSaveScreen(ShadowExchangeSaveMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/shadow_exchange_save.png");

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

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_vertical2_purple.png"), this.leftPos + -100, this.topPos + -112, 0, 0, 200, 225, 200, 225);

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
		if (DoesHaveKnightsProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_exchange_save.label_knight"), -16, -65, -1, false);
		if (DoesHaveGoblinProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_exchange_save.label_goblin_club"), -28, -39, -1, false);
		if (DoesHaveGoblinArcherProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_exchange_save.label_goblin_archer"), -34, -15, -1, false);
		if (DoesHaveGoblinMageProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_exchange_save.label_goblin_mage"), -28, 11, -1, false);
		if (DoesHaveWolfProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_exchange_save.label_lycan"), -14, 36, -1, false);
		if (DoesHavePolarBearProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_exchange_save.label_polar_bear"), -26, 60, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_panel_rework_rewardbutton_purple = new ImageButton(this.leftPos + -32, this.topPos + -71, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton_purple.png"), 64, 42,
				e -> {
					if (DoesHaveKnightsProcedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSaveButtonMessage(0, x, y, z));
						ShadowExchangeSaveButtonMessage.handleButtonAction(entity, 0, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveKnightsProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton_purple", imagebutton_panel_rework_rewardbutton_purple);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton_purple);
		imagebutton_panel_rework_rewardbutton_purple1 = new ImageButton(this.leftPos + -32, this.topPos + -46, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton_purple1.png"), 64, 42,
				e -> {
					if (DoesHaveGoblinProcedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSaveButtonMessage(1, x, y, z));
						ShadowExchangeSaveButtonMessage.handleButtonAction(entity, 1, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveGoblinProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton_purple1", imagebutton_panel_rework_rewardbutton_purple1);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton_purple1);
		imagebutton_panel_rework_rewardbutton_purple2 = new ImageButton(this.leftPos + -48, this.topPos + -21, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton_purple2.png"), 96, 42,
				e -> {
					if (DoesHaveGoblinArcherProcedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSaveButtonMessage(2, x, y, z));
						ShadowExchangeSaveButtonMessage.handleButtonAction(entity, 2, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveGoblinArcherProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton_purple2", imagebutton_panel_rework_rewardbutton_purple2);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton_purple2);
		imagebutton_panel_rework_rewardbutton2_purpl = new ImageButton(this.leftPos + -32, this.topPos + 4, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl.png"), 64, 42, e -> {
			if (DoesHaveGoblinMageProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSaveButtonMessage(3, x, y, z));
				ShadowExchangeSaveButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveGoblinMageProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl", imagebutton_panel_rework_rewardbutton2_purpl);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl);
		imagebutton_panel_rework_rewardbutton_purple3 = new ImageButton(this.leftPos + -32, this.topPos + 29, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton_purple3.png"), 64, 42,
				e -> {
					if (DoesHaveWolfProcedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSaveButtonMessage(4, x, y, z));
						ShadowExchangeSaveButtonMessage.handleButtonAction(entity, 4, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveWolfProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton_purple3", imagebutton_panel_rework_rewardbutton_purple3);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton_purple3);
		imagebutton_panel_rework_rewardbutton2_purpl1 = new ImageButton(this.leftPos + -32, this.topPos + 53, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl1.png"), 64, 42,
				e -> {
					if (DoesHavePolarBearProcedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSaveButtonMessage(5, x, y, z));
						ShadowExchangeSaveButtonMessage.handleButtonAction(entity, 5, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHavePolarBearProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl1", imagebutton_panel_rework_rewardbutton2_purpl1);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl1);
	}
}
