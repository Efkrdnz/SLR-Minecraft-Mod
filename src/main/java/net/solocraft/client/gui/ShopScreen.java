package net.solocraft.client.gui;

import net.solocraft.world.inventory.ShopMenu;
import net.solocraft.procedures.ReturnShopSwords6Procedure;
import net.solocraft.procedures.ReturnShopSwords5Procedure;
import net.solocraft.procedures.ReturnShopSwords4Procedure;
import net.solocraft.procedures.ReturnShopSwords3Procedure;
import net.solocraft.procedures.ReturnShopSwords2Procedure;
import net.solocraft.procedures.ReturnShopSwords1Procedure;
import net.solocraft.procedures.ReturnRefreshButtonProcedure;
import net.solocraft.procedures.GoldTextProcedure;
import net.solocraft.network.ShopButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class ShopScreen extends AbstractContainerScreen<ShopMenu> {
	private final static HashMap<String, Object> guistate = ShopMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	Button button_empty;
	Button button_empty1;
	Button button_empty2;
	Button button_empty3;
	Button button_empty4;
	Button button_empty5;
	ImageButton imagebutton_backbutton;
	ImageButton imagebutton_storerefresh;

	public ShopScreen(ShopMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/shop.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -96 && mouseX < leftPos + -72 && mouseY > topPos + -85 && mouseY < topPos + -61)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.shop.tooltip_empty"), mouseX, mouseY);
		if (mouseX > leftPos + -96 && mouseX < leftPos + -72 && mouseY > topPos + -57 && mouseY < topPos + -33)
			guiGraphics.renderTooltip(font, Component.literal(ReturnRefreshButtonProcedure.execute(entity)), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_vertical1.png"), this.leftPos + -78, this.topPos + -113, 0, 0, 150, 225, 150, 225);

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
		guiGraphics.drawString(this.font,

				GoldTextProcedure.execute(entity), 77, -106, -26266, false);
		guiGraphics.drawString(this.font,

				ReturnShopSwords1Procedure.execute(entity), -10, -76, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnShopSwords2Procedure.execute(entity), -10, -49, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnShopSwords3Procedure.execute(entity), -10, -22, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnShopSwords4Procedure.execute(entity), -10, 14, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnShopSwords5Procedure.execute(entity), -10, 41, -12829636, false);
		guiGraphics.drawString(this.font,

				ReturnShopSwords6Procedure.execute(entity), -10, 68, -12829636, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_empty = new PlainTextButton(this.leftPos + -34, this.topPos + -80, 25, 20, Component.translatable("gui.sololeveling.shop.button_empty"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(0, x, y, z));
				ShopButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty", button_empty);
		this.addRenderableWidget(button_empty);
		button_empty1 = new PlainTextButton(this.leftPos + -34, this.topPos + -53, 25, 20, Component.translatable("gui.sololeveling.shop.button_empty1"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(1, x, y, z));
				ShopButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty1", button_empty1);
		this.addRenderableWidget(button_empty1);
		button_empty2 = new PlainTextButton(this.leftPos + -34, this.topPos + -26, 25, 20, Component.translatable("gui.sololeveling.shop.button_empty2"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(2, x, y, z));
				ShopButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty2", button_empty2);
		this.addRenderableWidget(button_empty2);
		button_empty3 = new PlainTextButton(this.leftPos + -34, this.topPos + 10, 25, 20, Component.translatable("gui.sololeveling.shop.button_empty3"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(3, x, y, z));
				ShopButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty3", button_empty3);
		this.addRenderableWidget(button_empty3);
		button_empty4 = new PlainTextButton(this.leftPos + -34, this.topPos + 37, 25, 20, Component.translatable("gui.sololeveling.shop.button_empty4"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(4, x, y, z));
				ShopButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty4", button_empty4);
		this.addRenderableWidget(button_empty4);
		button_empty5 = new PlainTextButton(this.leftPos + -34, this.topPos + 64, 25, 20, Component.translatable("gui.sololeveling.shop.button_empty5"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(5, x, y, z));
				ShopButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty5", button_empty5);
		this.addRenderableWidget(button_empty5);
		imagebutton_backbutton = new ImageButton(this.leftPos + -94, this.topPos + -83, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_backbutton.png"), 20, 40, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(6, x, y, z));
				ShopButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		});
		guistate.put("button:imagebutton_backbutton", imagebutton_backbutton);
		this.addRenderableWidget(imagebutton_backbutton);
		imagebutton_storerefresh = new ImageButton(this.leftPos + -94, this.topPos + -55, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_storerefresh.png"), 20, 40, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(7, x, y, z));
				ShopButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		});
		guistate.put("button:imagebutton_storerefresh", imagebutton_storerefresh);
		this.addRenderableWidget(imagebutton_storerefresh);
	}
}
