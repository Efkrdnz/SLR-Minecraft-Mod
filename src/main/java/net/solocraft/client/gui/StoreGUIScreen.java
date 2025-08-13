package net.solocraft.client.gui;

import net.solocraft.world.inventory.StoreGUIMenu;
import net.solocraft.procedures.IsMiscEnabledProcedure;
import net.solocraft.network.StoreGUIButtonMessage;
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

public class StoreGUIScreen extends AbstractContainerScreen<StoreGUIMenu> {
	private final static HashMap<String, Object> guistate = StoreGUIMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_invb50;
	ImageButton imagebutton_invb501;
	ImageButton imagebutton_invb502;
	ImageButton imagebutton_storeaccecories;

	public StoreGUIScreen(StoreGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/store_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -44 && mouseX < leftPos + -20 && mouseY > topPos + -22 && mouseY < topPos + 2)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.store_gui.tooltip_weapons"), mouseX, mouseY);
		if (mouseX > leftPos + -1 && mouseX < leftPos + 23 && mouseY > topPos + -22 && mouseY < topPos + 2)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.store_gui.tooltip_potions"), mouseX, mouseY);
		if (mouseX > leftPos + -44 && mouseX < leftPos + -20 && mouseY > topPos + 23 && mouseY < topPos + 47)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.store_gui.tooltip_foods"), mouseX, mouseY);
		if (mouseX > leftPos + -1 && mouseX < leftPos + 23 && mouseY > topPos + 23 && mouseY < topPos + 47)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.store_gui.tooltip_misc"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_vertical2.png"), this.leftPos + -109, this.topPos + -114, 0, 0, 200, 225, 200, 225);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_gui.label_sslshop"), -23, -57, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_invb50 = new ImageButton(this.leftPos + -50, this.topPos + -28, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invb50.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreGUIButtonMessage(0, x, y, z));
				StoreGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invb50", imagebutton_invb50);
		this.addRenderableWidget(imagebutton_invb50);
		imagebutton_invb501 = new ImageButton(this.leftPos + -50, this.topPos + 17, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invb501.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreGUIButtonMessage(1, x, y, z));
				StoreGUIButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invb501", imagebutton_invb501);
		this.addRenderableWidget(imagebutton_invb501);
		imagebutton_invb502 = new ImageButton(this.leftPos + -7, this.topPos + -28, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invb502.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreGUIButtonMessage(2, x, y, z));
				StoreGUIButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invb502", imagebutton_invb502);
		this.addRenderableWidget(imagebutton_invb502);
		imagebutton_storeaccecories = new ImageButton(this.leftPos + -7, this.topPos + 17, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_storeaccecories.png"), 36, 72, e -> {
			if (IsMiscEnabledProcedure.execute(world)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreGUIButtonMessage(3, x, y, z));
				StoreGUIButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (IsMiscEnabledProcedure.execute(world))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_storeaccecories", imagebutton_storeaccecories);
		this.addRenderableWidget(imagebutton_storeaccecories);
	}
}
