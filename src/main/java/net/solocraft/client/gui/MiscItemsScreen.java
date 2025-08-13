package net.solocraft.client.gui;

import net.solocraft.world.inventory.MiscItemsMenu;
import net.solocraft.procedures.GoldTextProcedure;
import net.solocraft.network.MiscItemsButtonMessage;
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

public class MiscItemsScreen extends AbstractContainerScreen<MiscItemsMenu> {
	private final static HashMap<String, Object> guistate = MiscItemsMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_device;
	ImageButton imagebutton_speciallootbox;
	ImageButton imagebutton_chooseboxicon;

	public MiscItemsScreen(MiscItemsMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/misc_items.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -59 && mouseX < leftPos + -35 && mouseY > topPos + -78 && mouseY < topPos + -54)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.misc_items.tooltip_class_changer"), mouseX, mouseY);
		if (mouseX > leftPos + -60 && mouseX < leftPos + -36 && mouseY > topPos + -29 && mouseY < topPos + -5)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.misc_items.tooltip_special_random_box"), mouseX, mouseY);
		if (mouseX > leftPos + -60 && mouseX < leftPos + -36 && mouseY > topPos + 19 && mouseY < topPos + 43)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.misc_items.tooltip_special_selection_box"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/base.png"), this.leftPos + -77, this.topPos + -116, 0, 0, 150, 225, 150, 225);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.misc_items.label_1000_golds"), -18, -73, -26317, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.misc_items.label_5000_golds"), -18, -24, -26317, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.misc_items.label_5000_golds1"), -18, 25, -26317, false);
		guiGraphics.drawString(this.font,

				GoldTextProcedure.execute(entity), -67, -106, -26317, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_device = new ImageButton(this.leftPos + -63, this.topPos + -82, 32, 32, 0, 0, 32, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_device.png"), 32, 64, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new MiscItemsButtonMessage(0, x, y, z));
				MiscItemsButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_device", imagebutton_device);
		this.addRenderableWidget(imagebutton_device);
		imagebutton_speciallootbox = new ImageButton(this.leftPos + -60, this.topPos + -29, 24, 24, 0, 0, 24, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_speciallootbox.png"), 24, 48, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new MiscItemsButtonMessage(1, x, y, z));
				MiscItemsButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_speciallootbox", imagebutton_speciallootbox);
		this.addRenderableWidget(imagebutton_speciallootbox);
		imagebutton_chooseboxicon = new ImageButton(this.leftPos + -60, this.topPos + 19, 24, 24, 0, 0, 24, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_chooseboxicon.png"), 24, 48, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new MiscItemsButtonMessage(2, x, y, z));
				MiscItemsButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_chooseboxicon", imagebutton_chooseboxicon);
		this.addRenderableWidget(imagebutton_chooseboxicon);
	}
}
