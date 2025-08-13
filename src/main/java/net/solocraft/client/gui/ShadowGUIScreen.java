package net.solocraft.client.gui;

import net.solocraft.world.inventory.ShadowGUIMenu;
import net.solocraft.procedures.ReturnShadowWolfProcedure;
import net.solocraft.procedures.ReturnShadowStorageLevelProcedure;
import net.solocraft.procedures.ReturnShadowSoldProcedure;
import net.solocraft.procedures.ReturnShadowIgrisProcedure;
import net.solocraft.procedures.ReturnShadowGobProcedure;
import net.solocraft.procedures.ReturnShadowGobMageProcedure;
import net.solocraft.procedures.ReturnShadowBeruProcedure;
import net.solocraft.procedures.ReturnGoblinArcherProcedure;
import net.solocraft.procedures.IgrisSpawnedProcedure;
import net.solocraft.procedures.IgrisNotSpawnedProcedure;
import net.solocraft.procedures.BeruSpawnedProcedure;
import net.solocraft.network.ShadowGUIButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class ShadowGUIScreen extends AbstractContainerScreen<ShadowGUIMenu> {
	private final static HashMap<String, Object> guistate = ShadowGUIMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	Button button_berserk;
	Button button_dismiss_soldiers;
	Button button_dismiss_goblin_club;
	Button button_dismiss_goblin_mage;
	Button button_dismisswolf;
	Button button_dismiss_goblin_club1;

	public ShadowGUIScreen(ShadowGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/shadow_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		if (ReturnShadowIgrisProcedure.execute(world, x, y, z, entity) instanceof LivingEntity livingEntity) {
			if (IgrisSpawnedProcedure.execute(entity))
				InventoryScreen.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + -103, this.topPos + 3, 30, 0f + (float) Math.atan((this.leftPos + -103 - mouseX) / 40.0), (float) Math.atan((this.topPos + -46 - mouseY) / 40.0),
						livingEntity);
		}
		if (ReturnShadowBeruProcedure.execute(world, x, y, z, entity) instanceof LivingEntity livingEntity) {
			if (BeruSpawnedProcedure.execute(entity))
				InventoryScreen.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + 101, this.topPos + 1, 30, 0f, 0, livingEntity);
		}
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/basewide2.png"), this.leftPos + -151, this.topPos + -119, 0, 0, 300, 225, 300, 225);

		if (IgrisNotSpawnedProcedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/shadowunlocked.png"), this.leftPos + -135, this.topPos + -98, 0, 0, 67, 105, 67, 105);
		}

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/shadowunlocked.png"), this.leftPos + 67, this.topPos + -100, 0, 0, 67, 105, 67, 105);

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
		if (IgrisNotSpawnedProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_gui.label_not_spawned"), -115, -53, -10092544, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_gui.label_available_orders"), -46, -94, -16777216, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_gui.label_beru"), 90, -55, -10092544, false);
		guiGraphics.drawString(this.font,

				ReturnShadowSoldProcedure.execute(entity), -37, 7, -16777216, false);
		guiGraphics.drawString(this.font,

				ReturnShadowGobProcedure.execute(entity), -21, 26, -16777216, false);
		guiGraphics.drawString(this.font,

				ReturnShadowWolfProcedure.execute(entity), -63, 82, -16777216, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_gui.label_shadow_storage"), 73, 18, -16777216, false);
		guiGraphics.drawString(this.font,

				ReturnShadowStorageLevelProcedure.execute(entity), 71, 32, -16777216, false);
		guiGraphics.drawString(this.font,

				ReturnGoblinArcherProcedure.execute(entity), -21, 44, -16777216, false);
		guiGraphics.drawString(this.font,

				ReturnShadowGobMageProcedure.execute(entity), -21, 64, -16777216, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_berserk = Button.builder(Component.translatable("gui.sololeveling.shadow_gui.button_berserk"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowGUIButtonMessage(0, x, y, z));
				ShadowGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}).bounds(this.leftPos + -37, this.topPos + -74, 61, 20).build();
		guistate.put("button:button_berserk", button_berserk);
		this.addRenderableWidget(button_berserk);
		button_dismiss_soldiers = Button.builder(Component.translatable("gui.sololeveling.shadow_gui.button_dismiss_soldiers"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowGUIButtonMessage(1, x, y, z));
				ShadowGUIButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}).bounds(this.leftPos + -146, this.topPos + 2, 108, 20).build();
		guistate.put("button:button_dismiss_soldiers", button_dismiss_soldiers);
		this.addRenderableWidget(button_dismiss_soldiers);
		button_dismiss_goblin_club = Button.builder(Component.translatable("gui.sololeveling.shadow_gui.button_dismiss_goblin_club"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowGUIButtonMessage(2, x, y, z));
				ShadowGUIButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}).bounds(this.leftPos + -146, this.topPos + 21, 124, 20).build();
		guistate.put("button:button_dismiss_goblin_club", button_dismiss_goblin_club);
		this.addRenderableWidget(button_dismiss_goblin_club);
		button_dismiss_goblin_mage = Button.builder(Component.translatable("gui.sololeveling.shadow_gui.button_dismiss_goblin_mage"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowGUIButtonMessage(3, x, y, z));
				ShadowGUIButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}).bounds(this.leftPos + -146, this.topPos + 59, 124, 20).build();
		guistate.put("button:button_dismiss_goblin_mage", button_dismiss_goblin_mage);
		this.addRenderableWidget(button_dismiss_goblin_mage);
		button_dismisswolf = Button.builder(Component.translatable("gui.sololeveling.shadow_gui.button_dismisswolf"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowGUIButtonMessage(4, x, y, z));
				ShadowGUIButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}).bounds(this.leftPos + -146, this.topPos + 77, 82, 20).build();
		guistate.put("button:button_dismisswolf", button_dismisswolf);
		this.addRenderableWidget(button_dismisswolf);
		button_dismiss_goblin_club1 = Button.builder(Component.translatable("gui.sololeveling.shadow_gui.button_dismiss_goblin_club1"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowGUIButtonMessage(5, x, y, z));
				ShadowGUIButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		}).bounds(this.leftPos + -146, this.topPos + 40, 124, 20).build();
		guistate.put("button:button_dismiss_goblin_club1", button_dismiss_goblin_club1);
		this.addRenderableWidget(button_dismiss_goblin_club1);
	}
}
