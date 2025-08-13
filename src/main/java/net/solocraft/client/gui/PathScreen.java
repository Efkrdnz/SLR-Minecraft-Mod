package net.solocraft.client.gui;

import net.solocraft.world.inventory.PathMenu;
import net.solocraft.procedures.DkcFloorTextReturn9Procedure;
import net.solocraft.procedures.DkcFloorTextReturn8Procedure;
import net.solocraft.procedures.DkcFloorTextReturn7Procedure;
import net.solocraft.procedures.DkcFloorTextReturn6Procedure;
import net.solocraft.procedures.DkcFloorTextReturn5Procedure;
import net.solocraft.procedures.DkcFloorTextReturn4Procedure;
import net.solocraft.procedures.DkcFloorTextReturn3Procedure;
import net.solocraft.procedures.DkcFloorTextReturn2Procedure;
import net.solocraft.procedures.DkcFloorTextReturn20Procedure;
import net.solocraft.procedures.DkcFloorTextReturn1Procedure;
import net.solocraft.procedures.DkcFloorTextReturn19Procedure;
import net.solocraft.procedures.DkcFloorTextReturn18Procedure;
import net.solocraft.procedures.DkcFloorTextReturn17Procedure;
import net.solocraft.procedures.DkcFloorTextReturn16Procedure;
import net.solocraft.procedures.DkcFloorTextReturn15Procedure;
import net.solocraft.procedures.DkcFloorTextReturn14Procedure;
import net.solocraft.procedures.DkcFloorTextReturn13Procedure;
import net.solocraft.procedures.DkcFloorTextReturn12Procedure;
import net.solocraft.procedures.DkcFloorTextReturn11Procedure;
import net.solocraft.procedures.DkcFloorTextReturn10Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition9Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition8Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition7Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition6Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition5Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition4Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition3Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition2Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition20Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition1Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition19Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition18Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition17Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition16Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition15Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition14Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition13Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition12Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition11Procedure;
import net.solocraft.procedures.DkcButtonVisibilityCondition10Procedure;
import net.solocraft.network.PathButtonMessage;
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

public class PathScreen extends AbstractContainerScreen<PathMenu> {
	private final static HashMap<String, Object> guistate = PathMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_panel_button_dkc_1;
	ImageButton imagebutton_panel_button_dkc_2;
	ImageButton imagebutton_panel_button_dkc_3;
	ImageButton imagebutton_panel_button_dkc_4;
	ImageButton imagebutton_panel_button_dkc_5;
	ImageButton imagebutton_panel_button_dkc_6;
	ImageButton imagebutton_panel_button_dkc_7;
	ImageButton imagebutton_panel_button_dkc_8;
	ImageButton imagebutton_panel_button_dkc_9;
	ImageButton imagebutton_panel_button_dkc_10;
	ImageButton imagebutton_panel_button_dkc_11;
	ImageButton imagebutton_panel_button_dkc_12;
	ImageButton imagebutton_panel_button_dkc_13;
	ImageButton imagebutton_panel_button_dkc_14;
	ImageButton imagebutton_panel_button_dkc_15;
	ImageButton imagebutton_panel_button_dkc_16;
	ImageButton imagebutton_panel_button_dkc_17;
	ImageButton imagebutton_panel_button_dkc_18;
	ImageButton imagebutton_panel_button_dkc_19;
	ImageButton imagebutton_panel_button_dkc_20;

	public PathScreen(PathMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/path.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (DkcButtonVisibilityCondition1Procedure.execute(entity))
			if (mouseX > leftPos + -69 && mouseX < leftPos + -45 && mouseY > topPos + -56 && mouseY < topPos + -32)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn1Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition2Procedure.execute(entity))
			if (mouseX > leftPos + -41 && mouseX < leftPos + -17 && mouseY > topPos + -56 && mouseY < topPos + -32)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn2Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition3Procedure.execute(entity))
			if (mouseX > leftPos + -12 && mouseX < leftPos + 12 && mouseY > topPos + -56 && mouseY < topPos + -32)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn3Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition4Procedure.execute(entity))
			if (mouseX > leftPos + 15 && mouseX < leftPos + 39 && mouseY > topPos + -56 && mouseY < topPos + -32)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn4Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition5Procedure.execute(entity))
			if (mouseX > leftPos + 42 && mouseX < leftPos + 66 && mouseY > topPos + -56 && mouseY < topPos + -32)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn5Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition6Procedure.execute(entity))
			if (mouseX > leftPos + -69 && mouseX < leftPos + -45 && mouseY > topPos + -30 && mouseY < topPos + -6)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn6Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition7Procedure.execute(entity))
			if (mouseX > leftPos + -41 && mouseX < leftPos + -17 && mouseY > topPos + -30 && mouseY < topPos + -6)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn7Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition8Procedure.execute(entity))
			if (mouseX > leftPos + -12 && mouseX < leftPos + 12 && mouseY > topPos + -30 && mouseY < topPos + -6)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn8Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition9Procedure.execute(entity))
			if (mouseX > leftPos + 15 && mouseX < leftPos + 39 && mouseY > topPos + -30 && mouseY < topPos + -6)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn9Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition10Procedure.execute(entity))
			if (mouseX > leftPos + 42 && mouseX < leftPos + 66 && mouseY > topPos + -30 && mouseY < topPos + -6)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn10Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition11Procedure.execute(entity))
			if (mouseX > leftPos + -69 && mouseX < leftPos + -45 && mouseY > topPos + -4 && mouseY < topPos + 20)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn11Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition12Procedure.execute(entity))
			if (mouseX > leftPos + -41 && mouseX < leftPos + -17 && mouseY > topPos + -4 && mouseY < topPos + 20)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn12Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition13Procedure.execute(entity))
			if (mouseX > leftPos + -12 && mouseX < leftPos + 12 && mouseY > topPos + -4 && mouseY < topPos + 20)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn13Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition14Procedure.execute(entity))
			if (mouseX > leftPos + 15 && mouseX < leftPos + 39 && mouseY > topPos + -4 && mouseY < topPos + 20)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn14Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition15Procedure.execute(entity))
			if (mouseX > leftPos + 42 && mouseX < leftPos + 66 && mouseY > topPos + -4 && mouseY < topPos + 20)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn15Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition16Procedure.execute(entity))
			if (mouseX > leftPos + -69 && mouseX < leftPos + -45 && mouseY > topPos + 22 && mouseY < topPos + 46)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn16Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition17Procedure.execute(entity))
			if (mouseX > leftPos + -41 && mouseX < leftPos + -17 && mouseY > topPos + 22 && mouseY < topPos + 46)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn17Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition18Procedure.execute(entity))
			if (mouseX > leftPos + -12 && mouseX < leftPos + 12 && mouseY > topPos + 22 && mouseY < topPos + 46)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn18Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition19Procedure.execute(entity))
			if (mouseX > leftPos + 15 && mouseX < leftPos + 39 && mouseY > topPos + 22 && mouseY < topPos + 46)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn19Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition20Procedure.execute(entity))
			if (mouseX > leftPos + 58 && mouseX < leftPos + 82 && mouseY > topPos + 38 && mouseY < topPos + 62)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn20Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition20Procedure.execute(entity))
			if (mouseX > leftPos + 42 && mouseX < leftPos + 66 && mouseY > topPos + 38 && mouseY < topPos + 62)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn20Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition20Procedure.execute(entity))
			if (mouseX > leftPos + 42 && mouseX < leftPos + 66 && mouseY > topPos + 22 && mouseY < topPos + 46)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn20Procedure.execute(entity)), mouseX, mouseY);
		if (DkcButtonVisibilityCondition20Procedure.execute(entity))
			if (mouseX > leftPos + 58 && mouseX < leftPos + 82 && mouseY > topPos + 22 && mouseY < topPos + 46)
				guiGraphics.renderTooltip(font, Component.literal(DkcFloorTextReturn20Procedure.execute(entity)), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_path.png"), this.leftPos + -100, this.topPos + -80, 0, 0, 200, 160, 200, 160);

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
		imagebutton_panel_button_dkc_1 = new ImageButton(this.leftPos + -67, this.topPos + -54, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_1.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition1Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(0, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition1Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_1", imagebutton_panel_button_dkc_1);
		this.addRenderableWidget(imagebutton_panel_button_dkc_1);
		imagebutton_panel_button_dkc_2 = new ImageButton(this.leftPos + -39, this.topPos + -54, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_2.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition2Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(1, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition2Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_2", imagebutton_panel_button_dkc_2);
		this.addRenderableWidget(imagebutton_panel_button_dkc_2);
		imagebutton_panel_button_dkc_3 = new ImageButton(this.leftPos + -10, this.topPos + -54, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_3.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition3Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(2, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition3Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_3", imagebutton_panel_button_dkc_3);
		this.addRenderableWidget(imagebutton_panel_button_dkc_3);
		imagebutton_panel_button_dkc_4 = new ImageButton(this.leftPos + 17, this.topPos + -54, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_4.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition4Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(3, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition4Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_4", imagebutton_panel_button_dkc_4);
		this.addRenderableWidget(imagebutton_panel_button_dkc_4);
		imagebutton_panel_button_dkc_5 = new ImageButton(this.leftPos + 44, this.topPos + -54, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_5.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition5Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(4, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition5Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_5", imagebutton_panel_button_dkc_5);
		this.addRenderableWidget(imagebutton_panel_button_dkc_5);
		imagebutton_panel_button_dkc_6 = new ImageButton(this.leftPos + -67, this.topPos + -28, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_6.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition6Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(5, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition6Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_6", imagebutton_panel_button_dkc_6);
		this.addRenderableWidget(imagebutton_panel_button_dkc_6);
		imagebutton_panel_button_dkc_7 = new ImageButton(this.leftPos + -39, this.topPos + -28, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_7.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition7Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(6, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition7Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_7", imagebutton_panel_button_dkc_7);
		this.addRenderableWidget(imagebutton_panel_button_dkc_7);
		imagebutton_panel_button_dkc_8 = new ImageButton(this.leftPos + -10, this.topPos + -28, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_8.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition8Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(7, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition8Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_8", imagebutton_panel_button_dkc_8);
		this.addRenderableWidget(imagebutton_panel_button_dkc_8);
		imagebutton_panel_button_dkc_9 = new ImageButton(this.leftPos + 17, this.topPos + -28, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_9.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition9Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(8, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition9Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_9", imagebutton_panel_button_dkc_9);
		this.addRenderableWidget(imagebutton_panel_button_dkc_9);
		imagebutton_panel_button_dkc_10 = new ImageButton(this.leftPos + 44, this.topPos + -28, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_10.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition10Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(9, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition10Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_10", imagebutton_panel_button_dkc_10);
		this.addRenderableWidget(imagebutton_panel_button_dkc_10);
		imagebutton_panel_button_dkc_11 = new ImageButton(this.leftPos + -67, this.topPos + -2, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_11.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition11Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(10, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 10, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition11Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_11", imagebutton_panel_button_dkc_11);
		this.addRenderableWidget(imagebutton_panel_button_dkc_11);
		imagebutton_panel_button_dkc_12 = new ImageButton(this.leftPos + -39, this.topPos + -2, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_12.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition12Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(11, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 11, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition12Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_12", imagebutton_panel_button_dkc_12);
		this.addRenderableWidget(imagebutton_panel_button_dkc_12);
		imagebutton_panel_button_dkc_13 = new ImageButton(this.leftPos + -10, this.topPos + -2, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_13.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition13Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(12, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 12, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition13Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_13", imagebutton_panel_button_dkc_13);
		this.addRenderableWidget(imagebutton_panel_button_dkc_13);
		imagebutton_panel_button_dkc_14 = new ImageButton(this.leftPos + 17, this.topPos + -2, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_14.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition14Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(13, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 13, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition14Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_14", imagebutton_panel_button_dkc_14);
		this.addRenderableWidget(imagebutton_panel_button_dkc_14);
		imagebutton_panel_button_dkc_15 = new ImageButton(this.leftPos + 44, this.topPos + -2, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_15.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition15Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(14, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 14, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition15Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_15", imagebutton_panel_button_dkc_15);
		this.addRenderableWidget(imagebutton_panel_button_dkc_15);
		imagebutton_panel_button_dkc_16 = new ImageButton(this.leftPos + -67, this.topPos + 24, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_16.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition16Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(15, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 15, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition16Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_16", imagebutton_panel_button_dkc_16);
		this.addRenderableWidget(imagebutton_panel_button_dkc_16);
		imagebutton_panel_button_dkc_17 = new ImageButton(this.leftPos + -39, this.topPos + 24, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_17.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition17Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(16, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 16, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition17Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_17", imagebutton_panel_button_dkc_17);
		this.addRenderableWidget(imagebutton_panel_button_dkc_17);
		imagebutton_panel_button_dkc_18 = new ImageButton(this.leftPos + -10, this.topPos + 24, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_18.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition18Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(17, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 17, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition18Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_18", imagebutton_panel_button_dkc_18);
		this.addRenderableWidget(imagebutton_panel_button_dkc_18);
		imagebutton_panel_button_dkc_19 = new ImageButton(this.leftPos + 17, this.topPos + 24, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_19.png"), 20, 40, e -> {
			if (DkcButtonVisibilityCondition19Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(18, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 18, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition19Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_19", imagebutton_panel_button_dkc_19);
		this.addRenderableWidget(imagebutton_panel_button_dkc_19);
		imagebutton_panel_button_dkc_20 = new ImageButton(this.leftPos + 44, this.topPos + 24, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_button_dkc_20.png"), 36, 72, e -> {
			if (DkcButtonVisibilityCondition20Procedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PathButtonMessage(19, x, y, z));
				PathButtonMessage.handleButtonAction(entity, 19, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DkcButtonVisibilityCondition20Procedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_panel_button_dkc_20", imagebutton_panel_button_dkc_20);
		this.addRenderableWidget(imagebutton_panel_button_dkc_20);
	}
}
