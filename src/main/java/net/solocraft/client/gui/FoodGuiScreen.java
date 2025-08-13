package net.solocraft.client.gui;

import net.solocraft.world.inventory.FoodGuiMenu;
import net.solocraft.procedures.GoldTextProcedure;
import net.solocraft.network.FoodGuiButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class FoodGuiScreen extends AbstractContainerScreen<FoodGuiMenu> {
	private final static HashMap<String, Object> guistate = FoodGuiMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	Button button_empty;
	Button button_empty1;
	Button button_empty2;

	public FoodGuiScreen(FoodGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 1;
		this.imageHeight = 1;
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

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_vertical2.png"), this.leftPos + -105, this.topPos + -119, 0, 0, 200, 225, 200, 225);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_25g_x16"), -27, -85, -26266, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_x16"), -27, -76, -26266, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_40g"), -27, -58, -26266, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_x8"), -27, -49, -26266, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_80g"), -27, -31, -26266, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_x1"), -27, -22, -26266, false);
		guiGraphics.drawString(this.font,

				GoldTextProcedure.execute(entity), 96, -108, -26317, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_empty = new PlainTextButton(this.leftPos + -54, this.topPos + -85, 25, 20, Component.translatable("gui.sololeveling.food_gui.button_empty"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new FoodGuiButtonMessage(0, x, y, z));
				FoodGuiButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty", button_empty);
		this.addRenderableWidget(button_empty);
		button_empty1 = new PlainTextButton(this.leftPos + -54, this.topPos + -58, 25, 20, Component.translatable("gui.sololeveling.food_gui.button_empty1"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new FoodGuiButtonMessage(1, x, y, z));
				FoodGuiButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty1", button_empty1);
		this.addRenderableWidget(button_empty1);
		button_empty2 = new PlainTextButton(this.leftPos + -54, this.topPos + -31, 25, 20, Component.translatable("gui.sololeveling.food_gui.button_empty2"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new FoodGuiButtonMessage(2, x, y, z));
				FoodGuiButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty2", button_empty2);
		this.addRenderableWidget(button_empty2);
	}
}
