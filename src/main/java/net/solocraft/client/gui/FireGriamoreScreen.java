package net.solocraft.client.gui;

import net.solocraft.world.inventory.FireGriamoreMenu;
import net.solocraft.network.FireGriamoreButtonMessage;
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

public class FireGriamoreScreen extends AbstractContainerScreen<FireGriamoreMenu> {
	private final static HashMap<String, Object> guistate = FireGriamoreMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_griamorefireb1;
	ImageButton imagebutton_griamorefireb2;
	ImageButton imagebutton_griamorefireb3;

	public FireGriamoreScreen(FireGriamoreMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/fire_griamore.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -100 && mouseX < leftPos + -76 && mouseY > topPos + -66 && mouseY < topPos + -42)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.fire_griamore.tooltip_spread"), mouseX, mouseY);
		if (mouseX > leftPos + -75 && mouseX < leftPos + -51 && mouseY > topPos + -66 && mouseY < topPos + -42)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.fire_griamore.tooltip_spread1"), mouseX, mouseY);
		if (mouseX > leftPos + -100 && mouseX < leftPos + -76 && mouseY > topPos + -13 && mouseY < topPos + 11)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.fire_griamore.tooltip_aoe"), mouseX, mouseY);
		if (mouseX > leftPos + -75 && mouseX < leftPos + -51 && mouseY > topPos + -13 && mouseY < topPos + 11)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.fire_griamore.tooltip_aoe1"), mouseX, mouseY);
		if (mouseX > leftPos + -100 && mouseX < leftPos + -76 && mouseY > topPos + 44 && mouseY < topPos + 68)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.fire_griamore.tooltip_beam"), mouseX, mouseY);
		if (mouseX > leftPos + -75 && mouseX < leftPos + -51 && mouseY > topPos + 43 && mouseY < topPos + 67)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.fire_griamore.tooltip_beam1"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/griamorefire.png"), this.leftPos + -149, this.topPos + -111, 0, 0, 300, 225, 300, 225);

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
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_griamorefireb1 = new ImageButton(this.leftPos + -107, this.topPos + -69, 65, 30, 0, 0, 30, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_griamorefireb1.png"), 65, 60, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new FireGriamoreButtonMessage(0, x, y, z));
				FireGriamoreButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_griamorefireb1", imagebutton_griamorefireb1);
		this.addRenderableWidget(imagebutton_griamorefireb1);
		imagebutton_griamorefireb2 = new ImageButton(this.leftPos + -107, this.topPos + -16, 65, 30, 0, 0, 30, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_griamorefireb2.png"), 65, 60, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new FireGriamoreButtonMessage(1, x, y, z));
				FireGriamoreButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_griamorefireb2", imagebutton_griamorefireb2);
		this.addRenderableWidget(imagebutton_griamorefireb2);
		imagebutton_griamorefireb3 = new ImageButton(this.leftPos + -107, this.topPos + 41, 65, 30, 0, 0, 30, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_griamorefireb3.png"), 65, 60, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new FireGriamoreButtonMessage(2, x, y, z));
				FireGriamoreButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_griamorefireb3", imagebutton_griamorefireb3);
		this.addRenderableWidget(imagebutton_griamorefireb3);
	}
}
