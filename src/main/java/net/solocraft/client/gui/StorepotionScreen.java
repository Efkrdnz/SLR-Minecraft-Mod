package net.solocraft.client.gui;

import net.solocraft.world.inventory.StorepotionMenu;
import net.solocraft.procedures.GoldTextProcedure;
import net.solocraft.procedures.CoinTierPot3Procedure;
import net.solocraft.procedures.CoinTierPot2Procedure;
import net.solocraft.procedures.CoinTierPot1Procedure;
import net.solocraft.network.StorepotionButtonMessage;
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

public class StorepotionScreen extends AbstractContainerScreen<StorepotionMenu> {
	private final static HashMap<String, Object> guistate = StorepotionMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_potionmana;
	ImageButton imagebutton_potionhealth;
	ImageButton imagebutton_potionfatigue;
	ImageButton imagebutton_potionmana1;
	ImageButton imagebutton_potionmana2;
	ImageButton imagebutton_potionhealth1;
	ImageButton imagebutton_potionfatigue1;
	ImageButton imagebutton_potionhealth2;
	ImageButton imagebutton_potionfatigue2;

	public StorepotionScreen(StorepotionMenu container, Inventory inventory, Component text) {
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

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/basewide.png"), this.leftPos + -149, this.topPos + -115, 0, 0, 300, 225, 300, 225);

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

				GoldTextProcedure.execute(entity), -134, -102, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_1000_mana"), -88, -79, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_5000"), -1, -79, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_1000"), 89, -79, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_6_hp"), -87, -10, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_15_hp"), -1, -10, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_full_recovery"), 88, -10, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_20_fatigue"), -88, 56, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_50_ftg"), -1, 56, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.storepotion.label_100_ftg"), 88, 56, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot1Procedure.execute(entity), -88, -69, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot1Procedure.execute(entity), -88, 0, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot1Procedure.execute(entity), -88, 67, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot2Procedure.execute(entity), -2, -69, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot2Procedure.execute(entity), -3, 1, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot2Procedure.execute(entity), -2, 67, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot3Procedure.execute(entity), 89, -68, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot3Procedure.execute(entity), 88, 2, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierPot3Procedure.execute(entity), 88, 67, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_potionmana = new ImageButton(this.leftPos + -125, this.topPos + -83, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionmana.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(0, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionmana", imagebutton_potionmana);
		this.addRenderableWidget(imagebutton_potionmana);
		imagebutton_potionhealth = new ImageButton(this.leftPos + -125, this.topPos + -16, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionhealth.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(1, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionhealth", imagebutton_potionhealth);
		this.addRenderableWidget(imagebutton_potionhealth);
		imagebutton_potionfatigue = new ImageButton(this.leftPos + -125, this.topPos + 51, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionfatigue.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(2, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionfatigue", imagebutton_potionfatigue);
		this.addRenderableWidget(imagebutton_potionfatigue);
		imagebutton_potionmana1 = new ImageButton(this.leftPos + -41, this.topPos + -83, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionmana1.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(3, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionmana1", imagebutton_potionmana1);
		this.addRenderableWidget(imagebutton_potionmana1);
		imagebutton_potionmana2 = new ImageButton(this.leftPos + 50, this.topPos + -83, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionmana2.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(4, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionmana2", imagebutton_potionmana2);
		this.addRenderableWidget(imagebutton_potionmana2);
		imagebutton_potionhealth1 = new ImageButton(this.leftPos + -41, this.topPos + -16, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionhealth1.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(5, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionhealth1", imagebutton_potionhealth1);
		this.addRenderableWidget(imagebutton_potionhealth1);
		imagebutton_potionfatigue1 = new ImageButton(this.leftPos + -41, this.topPos + 51, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionfatigue1.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(6, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionfatigue1", imagebutton_potionfatigue1);
		this.addRenderableWidget(imagebutton_potionfatigue1);
		imagebutton_potionhealth2 = new ImageButton(this.leftPos + 50, this.topPos + -16, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionhealth2.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(7, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionhealth2", imagebutton_potionhealth2);
		this.addRenderableWidget(imagebutton_potionhealth2);
		imagebutton_potionfatigue2 = new ImageButton(this.leftPos + 50, this.topPos + 51, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_potionfatigue2.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorepotionButtonMessage(8, x, y, z));
				StorepotionButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		});
		guistate.put("button:imagebutton_potionfatigue2", imagebutton_potionfatigue2);
		this.addRenderableWidget(imagebutton_potionfatigue2);
	}
}
