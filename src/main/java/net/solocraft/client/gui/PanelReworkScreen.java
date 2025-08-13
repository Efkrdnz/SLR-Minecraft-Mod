package net.solocraft.client.gui;

import net.solocraft.world.inventory.PanelReworkMenu;
import net.solocraft.procedures.VitalityTextProcedure;
import net.solocraft.procedures.TitleTextProcedure;
import net.solocraft.procedures.StrengthTextProcedure;
import net.solocraft.procedures.SpeedTextProcedure;
import net.solocraft.procedures.SkillPointsTextProcedure;
import net.solocraft.procedures.SenseTextProcedure;
import net.solocraft.procedures.ReturnRemainingXPProcedure;
import net.solocraft.procedures.ReturnNameAndGuildProcedure;
import net.solocraft.procedures.ManaTextProcedure;
import net.solocraft.procedures.LevelTextProcedure;
import net.solocraft.procedures.IntelligenceTextProcedure;
import net.solocraft.procedures.HealthTextProcedure;
import net.solocraft.procedures.FatigueTextProcedure;
import net.solocraft.network.PanelReworkButtonMessage;
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

public class PanelReworkScreen extends AbstractContainerScreen<PanelReworkMenu> {
	private final static HashMap<String, Object> guistate = PanelReworkMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_normal_quest;
	ImageButton imagebutton_normal_reward;
	ImageButton imagebutton_normal_store;
	ImageButton imagebutton_normal_abilities;
	ImageButton imagebutton_normal_training;
	ImageButton imagebutton_normal_craft;
	ImageButton imagebutton_invest2;
	ImageButton imagebutton_invest21;
	ImageButton imagebutton_invest22;
	ImageButton imagebutton_invest23;
	ImageButton imagebutton_invest24;

	public PanelReworkScreen(PanelReworkMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/panel_rework.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + 27 && mouseX < leftPos + 51 && mouseY > topPos + -52 && mouseY < topPos + -28)
			guiGraphics.renderTooltip(font, Component.literal(ReturnRemainingXPProcedure.execute(entity)), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_6.png"), this.leftPos + -113, this.topPos + -105, 0, 0, 225, 190, 225, 190);

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

				ReturnNameAndGuildProcedure.execute(entity), -56, -54, -1, false);
		guiGraphics.drawString(this.font,

				TitleTextProcedure.execute(entity), -56, -35, -1, false);
		guiGraphics.drawString(this.font,

				SkillPointsTextProcedure.execute(entity), 26, 33, -1, false);
		guiGraphics.drawString(this.font,

				IntelligenceTextProcedure.execute(entity), 43, 18, -1, false);
		guiGraphics.drawString(this.font,

				StrengthTextProcedure.execute(entity), -22, 2, -1, false);
		guiGraphics.drawString(this.font,

				VitalityTextProcedure.execute(entity), 43, 2, -1, false);
		guiGraphics.drawString(this.font,

				SpeedTextProcedure.execute(entity), -22, 19, -1, false);
		guiGraphics.drawString(this.font,

				SenseTextProcedure.execute(entity), -22, 34, -1, false);
		guiGraphics.drawString(this.font,

				LevelTextProcedure.execute(entity), 52, -44, -1, false);
		guiGraphics.drawString(this.font,

				FatigueTextProcedure.execute(entity), -5, -18, -1, false);
		guiGraphics.drawString(this.font,

				HealthTextProcedure.execute(entity), -61, -18, -39322, false);
		guiGraphics.drawString(this.font,

				ManaTextProcedure.execute(entity), 32, -18, -10040065, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_normal_quest = new ImageButton(this.leftPos + -144, this.topPos + 15, 32, 34, 0, 0, 34, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_normal_quest.png"), 32, 68, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(0, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_normal_quest", imagebutton_normal_quest);
		this.addRenderableWidget(imagebutton_normal_quest);
		imagebutton_normal_reward = new ImageButton(this.leftPos + -144, this.topPos + -25, 32, 34, 0, 0, 34, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_normal_reward.png"), 32, 68, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(1, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_normal_reward", imagebutton_normal_reward);
		this.addRenderableWidget(imagebutton_normal_reward);
		imagebutton_normal_store = new ImageButton(this.leftPos + -144, this.topPos + -66, 32, 34, 0, 0, 34, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_normal_store.png"), 32, 68, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(2, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_normal_store", imagebutton_normal_store);
		this.addRenderableWidget(imagebutton_normal_store);
		imagebutton_normal_abilities = new ImageButton(this.leftPos + 111, this.topPos + -66, 32, 34, 0, 0, 34, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_normal_abilities.png"), 32, 68, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(3, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		});
		guistate.put("button:imagebutton_normal_abilities", imagebutton_normal_abilities);
		this.addRenderableWidget(imagebutton_normal_abilities);
		imagebutton_normal_training = new ImageButton(this.leftPos + 112, this.topPos + -26, 32, 34, 0, 0, 34, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_normal_training.png"), 32, 68, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(4, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		});
		guistate.put("button:imagebutton_normal_training", imagebutton_normal_training);
		this.addRenderableWidget(imagebutton_normal_training);
		imagebutton_normal_craft = new ImageButton(this.leftPos + 111, this.topPos + 14, 32, 34, 0, 0, 34, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_normal_craft.png"), 32, 68, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(5, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_normal_craft", imagebutton_normal_craft);
		this.addRenderableWidget(imagebutton_normal_craft);
		imagebutton_invest2 = new ImageButton(this.leftPos + -87, this.topPos + 2, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest2.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(6, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest2", imagebutton_invest2);
		this.addRenderableWidget(imagebutton_invest2);
		imagebutton_invest21 = new ImageButton(this.leftPos + -87, this.topPos + 18, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest21.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(7, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest21", imagebutton_invest21);
		this.addRenderableWidget(imagebutton_invest21);
		imagebutton_invest22 = new ImageButton(this.leftPos + -87, this.topPos + 34, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest22.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(8, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest22", imagebutton_invest22);
		this.addRenderableWidget(imagebutton_invest22);
		imagebutton_invest23 = new ImageButton(this.leftPos + 79, this.topPos + 2, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest23.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(9, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest23", imagebutton_invest23);
		this.addRenderableWidget(imagebutton_invest23);
		imagebutton_invest24 = new ImageButton(this.leftPos + 79, this.topPos + 18, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest24.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelReworkButtonMessage(10, x, y, z));
				PanelReworkButtonMessage.handleButtonAction(entity, 10, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest24", imagebutton_invest24);
		this.addRenderableWidget(imagebutton_invest24);
	}
}
