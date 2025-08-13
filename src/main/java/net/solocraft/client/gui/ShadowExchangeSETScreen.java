package net.solocraft.client.gui;

import net.solocraft.world.inventory.ShadowExchangeSETMenu;
import net.solocraft.procedures.ReturnExchange7Procedure;
import net.solocraft.procedures.ReturnExchange6Procedure;
import net.solocraft.procedures.ReturnExchange5Procedure;
import net.solocraft.procedures.ReturnExchange4Procedure;
import net.solocraft.procedures.ReturnExchange3Procedure;
import net.solocraft.procedures.ReturnExchange2Procedure;
import net.solocraft.procedures.ReturnExchange1Procedure;
import net.solocraft.procedures.ExchangeCon7Procedure;
import net.solocraft.procedures.ExchangeCon6Procedure;
import net.solocraft.procedures.ExchangeCon5Procedure;
import net.solocraft.procedures.ExchangeCon4Procedure;
import net.solocraft.procedures.ExchangeCon3Procedure;
import net.solocraft.procedures.ExchangeCon2Procedure;
import net.solocraft.procedures.ExchangeCon1Procedure;
import net.solocraft.network.ShadowExchangeSETButtonMessage;
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

public class ShadowExchangeSETScreen extends AbstractContainerScreen<ShadowExchangeSETMenu> {
	private final static HashMap<String, Object> guistate = ShadowExchangeSETMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl1;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl2;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl3;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl4;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl5;
	ImageButton imagebutton_panel_rework_rewardbutton2_purpl6;
	ImageButton imagebutton_purple_cancel;
	ImageButton imagebutton_purple_cancel1;
	ImageButton imagebutton_purple_cancel2;
	ImageButton imagebutton_purple_cancel3;
	ImageButton imagebutton_purple_cancel4;
	ImageButton imagebutton_purple_cancel5;
	ImageButton imagebutton_purple_cancel6;

	public ShadowExchangeSETScreen(ShadowExchangeSETMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/shadow_exchange_set.png");

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

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_vertical2_purple.png"), this.leftPos + -103, this.topPos + -114, 0, 0, 200, 225, 200, 225);

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
		if (ExchangeCon1Procedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnExchange1Procedure.execute(entity), -47, -72, -1, false);
		if (ExchangeCon2Procedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnExchange2Procedure.execute(entity), -47, -49, -1, false);
		if (ExchangeCon3Procedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnExchange3Procedure.execute(entity), -47, -26, -1, false);
		if (ExchangeCon4Procedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnExchange4Procedure.execute(entity), -47, -3, -1, false);
		if (ExchangeCon5Procedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnExchange5Procedure.execute(entity), -47, 20, -1, false);
		if (ExchangeCon6Procedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnExchange6Procedure.execute(entity), -47, 43, -1, false);
		if (ExchangeCon7Procedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnExchange7Procedure.execute(entity), -47, 66, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_panel_rework_rewardbutton2_purpl = new ImageButton(this.leftPos + -53, this.topPos + -80, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl.png"), 96, 42,
				e -> {
					if (ExchangeCon1Procedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(0, x, y, z));
						ShadowExchangeSETButtonMessage.handleButtonAction(entity, 0, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon1Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl", imagebutton_panel_rework_rewardbutton2_purpl);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl);
		imagebutton_panel_rework_rewardbutton2_purpl1 = new ImageButton(this.leftPos + -53, this.topPos + -57, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl1.png"), 96, 42,
				e -> {
					if (ExchangeCon2Procedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(1, x, y, z));
						ShadowExchangeSETButtonMessage.handleButtonAction(entity, 1, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon2Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl1", imagebutton_panel_rework_rewardbutton2_purpl1);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl1);
		imagebutton_panel_rework_rewardbutton2_purpl2 = new ImageButton(this.leftPos + -53, this.topPos + -34, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl2.png"), 96, 42,
				e -> {
					if (ExchangeCon3Procedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(2, x, y, z));
						ShadowExchangeSETButtonMessage.handleButtonAction(entity, 2, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon3Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl2", imagebutton_panel_rework_rewardbutton2_purpl2);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl2);
		imagebutton_panel_rework_rewardbutton2_purpl3 = new ImageButton(this.leftPos + -53, this.topPos + -11, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl3.png"), 96, 42,
				e -> {
					if (ExchangeCon4Procedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(3, x, y, z));
						ShadowExchangeSETButtonMessage.handleButtonAction(entity, 3, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon4Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl3", imagebutton_panel_rework_rewardbutton2_purpl3);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl3);
		imagebutton_panel_rework_rewardbutton2_purpl4 = new ImageButton(this.leftPos + -53, this.topPos + 12, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl4.png"), 96, 42,
				e -> {
					if (ExchangeCon5Procedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(4, x, y, z));
						ShadowExchangeSETButtonMessage.handleButtonAction(entity, 4, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon5Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl4", imagebutton_panel_rework_rewardbutton2_purpl4);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl4);
		imagebutton_panel_rework_rewardbutton2_purpl5 = new ImageButton(this.leftPos + -53, this.topPos + 35, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl5.png"), 96, 42,
				e -> {
					if (ExchangeCon6Procedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(5, x, y, z));
						ShadowExchangeSETButtonMessage.handleButtonAction(entity, 5, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon6Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl5", imagebutton_panel_rework_rewardbutton2_purpl5);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl5);
		imagebutton_panel_rework_rewardbutton2_purpl6 = new ImageButton(this.leftPos + -53, this.topPos + 58, 96, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton2_purpl6.png"), 96, 42,
				e -> {
					if (ExchangeCon7Procedure.execute(entity)) {
						SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(6, x, y, z));
						ShadowExchangeSETButtonMessage.handleButtonAction(entity, 6, x, y, z);
					}
				}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon7Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_rework_rewardbutton2_purpl6", imagebutton_panel_rework_rewardbutton2_purpl6);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton2_purpl6);
		imagebutton_purple_cancel = new ImageButton(this.leftPos + -78, this.topPos + -80, 22, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_purple_cancel.png"), 22, 44, e -> {
			if (ExchangeCon1Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(7, x, y, z));
				ShadowExchangeSETButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon1Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_purple_cancel", imagebutton_purple_cancel);
		this.addRenderableWidget(imagebutton_purple_cancel);
		imagebutton_purple_cancel1 = new ImageButton(this.leftPos + -78, this.topPos + -57, 22, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_purple_cancel1.png"), 22, 44, e -> {
			if (ExchangeCon2Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(8, x, y, z));
				ShadowExchangeSETButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon2Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_purple_cancel1", imagebutton_purple_cancel1);
		this.addRenderableWidget(imagebutton_purple_cancel1);
		imagebutton_purple_cancel2 = new ImageButton(this.leftPos + -78, this.topPos + -34, 22, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_purple_cancel2.png"), 22, 44, e -> {
			if (ExchangeCon3Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(9, x, y, z));
				ShadowExchangeSETButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon3Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_purple_cancel2", imagebutton_purple_cancel2);
		this.addRenderableWidget(imagebutton_purple_cancel2);
		imagebutton_purple_cancel3 = new ImageButton(this.leftPos + -78, this.topPos + -11, 22, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_purple_cancel3.png"), 22, 44, e -> {
			if (ExchangeCon4Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(10, x, y, z));
				ShadowExchangeSETButtonMessage.handleButtonAction(entity, 10, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon4Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_purple_cancel3", imagebutton_purple_cancel3);
		this.addRenderableWidget(imagebutton_purple_cancel3);
		imagebutton_purple_cancel4 = new ImageButton(this.leftPos + -78, this.topPos + 12, 22, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_purple_cancel4.png"), 22, 44, e -> {
			if (ExchangeCon5Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(11, x, y, z));
				ShadowExchangeSETButtonMessage.handleButtonAction(entity, 11, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon5Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_purple_cancel4", imagebutton_purple_cancel4);
		this.addRenderableWidget(imagebutton_purple_cancel4);
		imagebutton_purple_cancel5 = new ImageButton(this.leftPos + -78, this.topPos + 35, 22, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_purple_cancel5.png"), 22, 44, e -> {
			if (ExchangeCon6Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(12, x, y, z));
				ShadowExchangeSETButtonMessage.handleButtonAction(entity, 12, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon6Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_purple_cancel5", imagebutton_purple_cancel5);
		this.addRenderableWidget(imagebutton_purple_cancel5);
		imagebutton_purple_cancel6 = new ImageButton(this.leftPos + -78, this.topPos + 58, 22, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_purple_cancel6.png"), 22, 44, e -> {
			if (ExchangeCon7Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeSETButtonMessage(13, x, y, z));
				ShadowExchangeSETButtonMessage.handleButtonAction(entity, 13, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (ExchangeCon7Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_purple_cancel6", imagebutton_purple_cancel6);
		this.addRenderableWidget(imagebutton_purple_cancel6);
	}
}
