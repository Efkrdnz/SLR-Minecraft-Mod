package net.solocraft.client.gui;

import net.solocraft.world.inventory.SelectionBoxGUIMenu;
import net.solocraft.network.SelectionBoxGUIButtonMessage;
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

public class SelectionBoxGUIScreen extends AbstractContainerScreen<SelectionBoxGUIMenu> {
	private final static HashMap<String, Object> guistate = SelectionBoxGUIMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	Button button_empty;
	Button button_empty1;
	Button button_empty2;

	public SelectionBoxGUIScreen(SelectionBoxGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 96;
		this.imageHeight = 36;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/selection_box_gui.png");

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.selection_box_gui.label_get_whole_set"), 11, -13, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		button_empty = new PlainTextButton(this.leftPos + 5, this.topPos + 8, 25, 20, Component.translatable("gui.sololeveling.selection_box_gui.button_empty"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new SelectionBoxGUIButtonMessage(0, x, y, z));
				SelectionBoxGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty", button_empty);
		this.addRenderableWidget(button_empty);
		button_empty1 = new PlainTextButton(this.leftPos + 35, this.topPos + 8, 25, 20, Component.translatable("gui.sololeveling.selection_box_gui.button_empty1"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new SelectionBoxGUIButtonMessage(1, x, y, z));
				SelectionBoxGUIButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty1", button_empty1);
		this.addRenderableWidget(button_empty1);
		button_empty2 = new PlainTextButton(this.leftPos + 66, this.topPos + 8, 25, 20, Component.translatable("gui.sololeveling.selection_box_gui.button_empty2"), e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new SelectionBoxGUIButtonMessage(2, x, y, z));
				SelectionBoxGUIButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}, this.font);
		guistate.put("button:button_empty2", button_empty2);
		this.addRenderableWidget(button_empty2);
	}
}
