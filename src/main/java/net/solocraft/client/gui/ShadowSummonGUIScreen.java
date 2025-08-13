package net.solocraft.client.gui;

import net.solocraft.world.inventory.ShadowSummonGUIMenu;
import net.solocraft.procedures.ReturnTuskProcedure;
import net.solocraft.procedures.ReturnShadowWolfProcedure;
import net.solocraft.procedures.ReturnPolarBearProcedure;
import net.solocraft.procedures.ReturnOrcProcedure;
import net.solocraft.procedures.ReturnKnightProcedure;
import net.solocraft.procedures.ReturnKamishProcedure;
import net.solocraft.procedures.ReturnIgrisProcedure;
import net.solocraft.procedures.ReturnHighOrcProcedure;
import net.solocraft.procedures.ReturnGoblinMageProcedure;
import net.solocraft.procedures.ReturnGoblinClubProcedure;
import net.solocraft.procedures.ReturnGoblinArcProcedure;
import net.solocraft.procedures.ReturnBeruProcedure;
import net.solocraft.procedures.DoesHaveWolfProcedure;
import net.solocraft.procedures.DoesHaveTuskProcedure;
import net.solocraft.procedures.DoesHavePolarBearProcedure;
import net.solocraft.procedures.DoesHaveOrcProcedure;
import net.solocraft.procedures.DoesHaveKnightsProcedure;
import net.solocraft.procedures.DoesHaveKamishProcedure;
import net.solocraft.procedures.DoesHaveIgrisProcedure;
import net.solocraft.procedures.DoesHaveHighOrcProcedure;
import net.solocraft.procedures.DoesHaveGoblinProcedure;
import net.solocraft.procedures.DoesHaveGoblinMageProcedure;
import net.solocraft.procedures.DoesHaveGoblinArcherProcedure;
import net.solocraft.procedures.DoesHaveBeruProcedure;
import net.solocraft.network.ShadowSummonGUIButtonMessage;
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

public class ShadowSummonGUIScreen extends AbstractContainerScreen<ShadowSummonGUIMenu> {
	private final static HashMap<String, Object> guistate = ShadowSummonGUIMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_summon_goblin;
	ImageButton imagebutton_summon_button;
	ImageButton imagebutton_summon_button1;
	ImageButton imagebutton_summon_button2;
	ImageButton imagebutton_summon_button3;
	ImageButton imagebutton_summon_button4;
	ImageButton imagebutton_summon_button5;
	ImageButton imagebutton_summon_button6;
	ImageButton imagebutton_summon_button7;
	ImageButton imagebutton_summon_button8;
	ImageButton imagebutton_button;
	ImageButton imagebutton_button1;

	public ShadowSummonGUIScreen(ShadowSummonGUIMenu container, Inventory inventory, Component text) {
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

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/big_frame.png"), this.leftPos + -76, this.topPos + -108, 0, 0, 150, 200, 150, 200);

		if (DoesHaveIgrisProcedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/frame.png"), this.leftPos + -160, this.topPos + -108, 0, 0, 75, 100, 75, 100);
		}
		if (DoesHaveBeruProcedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/frame.png"), this.leftPos + 84, this.topPos + -108, 0, 0, 75, 100, 75, 100);
		}
		if (DoesHaveKamishProcedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/frame.png"), this.leftPos + -159, this.topPos + -5, 0, 0, 75, 100, 75, 100);
		}
		if (DoesHaveTuskProcedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/frame.png"), this.leftPos + 84, this.topPos + -5, 0, 0, 75, 100, 75, 100);
		}
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
		if (DoesHaveGoblinProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_goblin"), -59, -99, -63257, false);
		if (DoesHaveGoblinProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_goblin_fighter"), -61, -90, -63257, false);
		if (DoesHaveGoblinArcherProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_goblin1"), -59, -64, -63257, false);
		if (DoesHaveGoblinArcherProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_archer"), -58, -54, -63257, false);
		if (DoesHaveGoblinMageProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_goblin2"), -59, -28, -63257, false);
		if (DoesHaveGoblinMageProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_mage"), -54, -19, -63257, false);
		if (DoesHaveWolfProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_lycan"), 27, -94, -63257, false);
		if (DoesHaveKnightsProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_knight"), 25, -58, -63257, false);
		if (DoesHavePolarBearProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_polar_bear"), 27, -28, -63257, false);
		if (DoesHavePolarBearProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_bear"), 30, -20, -63257, false);
		if (DoesHaveOrcProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_orc"), -52, 14, -63257, false);
		if (DoesHaveKnightsProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnKnightProcedure.execute(entity), 24, -42, -1, false);
		if (DoesHaveGoblinProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnGoblinClubProcedure.execute(entity), -62, -77, -1, false);
		if (DoesHaveWolfProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnShadowWolfProcedure.execute(entity), 24, -77, -1, false);
		if (DoesHaveGoblinArcherProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnGoblinArcProcedure.execute(entity), -62, -42, -1, false);
		if (DoesHaveGoblinMageProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnGoblinMageProcedure.execute(entity), -62, -5, -1, false);
		if (DoesHaveOrcProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnOrcProcedure.execute(entity), -62, 32, -1, false);
		if (DoesHavePolarBearProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnPolarBearProcedure.execute(entity), 23, -5, -1, false);
		if (DoesHaveIgrisProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_igris"), -136, -37, -6750055, false);
		if (DoesHaveBeruProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_beru"), 110, -37, -6750055, false);
		if (DoesHaveIgrisProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnIgrisProcedure.execute(entity), -136, -55, -1, false);
		if (DoesHaveBeruProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnBeruProcedure.execute(entity), 109, -54, -1, false);
		if (DoesHaveKamishProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_kamish"), -137, 66, -6750055, false);
		if (DoesHaveKamishProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnKamishProcedure.execute(entity), -136, 47, -1, false);
		if (DoesHaveHighOrcProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_high_orc"), 19, 14, -65281, false);
		if (DoesHaveHighOrcProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnHighOrcProcedure.execute(entity), 20, 32, -1, false);
		if (DoesHaveTuskProcedure.execute(entity))
			guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_summon_gui.label_tusk"), 110, 66, -6750055, false);
		if (DoesHaveTuskProcedure.execute(entity))
			guiGraphics.drawString(this.font,

					ReturnTuskProcedure.execute(entity), 109, 48, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_summon_goblin = new ImageButton(this.leftPos + -67, this.topPos + -100, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_goblin.png"), 48, 44, e -> {
			if (DoesHaveGoblinProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(0, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveGoblinProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_goblin", imagebutton_summon_goblin);
		this.addRenderableWidget(imagebutton_summon_goblin);
		imagebutton_summon_button = new ImageButton(this.leftPos + -67, this.topPos + -65, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button.png"), 48, 44, e -> {
			if (DoesHaveGoblinArcherProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(1, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveGoblinArcherProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button", imagebutton_summon_button);
		this.addRenderableWidget(imagebutton_summon_button);
		imagebutton_summon_button1 = new ImageButton(this.leftPos + -67, this.topPos + -29, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button1.png"), 48, 44, e -> {
			if (DoesHaveGoblinMageProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(2, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveGoblinMageProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button1", imagebutton_summon_button1);
		this.addRenderableWidget(imagebutton_summon_button1);
		imagebutton_summon_button2 = new ImageButton(this.leftPos + 16, this.topPos + -100, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button2.png"), 48, 44, e -> {
			if (DoesHaveWolfProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(3, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveWolfProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button2", imagebutton_summon_button2);
		this.addRenderableWidget(imagebutton_summon_button2);
		imagebutton_summon_button3 = new ImageButton(this.leftPos + 16, this.topPos + -65, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button3.png"), 48, 44, e -> {
			if (DoesHaveKnightsProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(4, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveKnightsProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button3", imagebutton_summon_button3);
		this.addRenderableWidget(imagebutton_summon_button3);
		imagebutton_summon_button4 = new ImageButton(this.leftPos + 16, this.topPos + -29, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button4.png"), 48, 44, e -> {
			if (DoesHavePolarBearProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(5, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHavePolarBearProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button4", imagebutton_summon_button4);
		this.addRenderableWidget(imagebutton_summon_button4);
		imagebutton_summon_button5 = new ImageButton(this.leftPos + -67, this.topPos + 8, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button5.png"), 48, 44, e -> {
			if (DoesHaveOrcProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(6, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveOrcProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button5", imagebutton_summon_button5);
		this.addRenderableWidget(imagebutton_summon_button5);
		imagebutton_summon_button6 = new ImageButton(this.leftPos + -146, this.topPos + -43, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button6.png"), 48, 44, e -> {
			if (DoesHaveIgrisProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(7, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveIgrisProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button6", imagebutton_summon_button6);
		this.addRenderableWidget(imagebutton_summon_button6);
		imagebutton_summon_button7 = new ImageButton(this.leftPos + 97, this.topPos + -43, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button7.png"), 48, 44, e -> {
			if (DoesHaveBeruProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(8, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveBeruProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button7", imagebutton_summon_button7);
		this.addRenderableWidget(imagebutton_summon_button7);
		imagebutton_summon_button8 = new ImageButton(this.leftPos + -146, this.topPos + 59, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_summon_button8.png"), 48, 44, e -> {
			if (DoesHaveKamishProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(9, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveKamishProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_summon_button8", imagebutton_summon_button8);
		this.addRenderableWidget(imagebutton_summon_button8);
		imagebutton_button = new ImageButton(this.leftPos + 16, this.topPos + 8, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button.png"), 48, 44, e -> {
			if (DoesHaveHighOrcProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(10, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 10, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveHighOrcProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_button", imagebutton_button);
		this.addRenderableWidget(imagebutton_button);
		imagebutton_button1 = new ImageButton(this.leftPos + 97, this.topPos + 59, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button1.png"), 48, 44, e -> {
			if (DoesHaveTuskProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowSummonGUIButtonMessage(11, x, y, z));
				ShadowSummonGUIButtonMessage.handleButtonAction(entity, 11, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (DoesHaveTuskProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_button1", imagebutton_button1);
		this.addRenderableWidget(imagebutton_button1);
	}
}
