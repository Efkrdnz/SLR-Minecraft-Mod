package net.solocraft.client.gui;

import net.solocraft.world.inventory.PanelEarlyMenu;
import net.solocraft.procedures.VitalityTextProcedure;
import net.solocraft.procedures.TitleTextProcedure;
import net.solocraft.procedures.TimerTextProcedure;
import net.solocraft.procedures.StrengthTextProcedure;
import net.solocraft.procedures.SpeedTextProcedure;
import net.solocraft.procedures.SkillPointsTextProcedure;
import net.solocraft.procedures.SenseTextProcedure;
import net.solocraft.procedures.ReturnPlayerProcedure;
import net.solocraft.procedures.ReturnNameAndGuildProcedure;
import net.solocraft.procedures.ReturnClassNameProcedure;
import net.solocraft.procedures.MaxXpTextProcedure;
import net.solocraft.procedures.ManaTextGUIProcedure;
import net.solocraft.procedures.LevelTextProcedure;
import net.solocraft.procedures.IntelligenceTextProcedure;
import net.solocraft.procedures.HealthTextProcedure;
import net.solocraft.procedures.HasSkillPointsProcedure;
import net.solocraft.procedures.FatigueTextProcedure;
import net.solocraft.network.PanelEarlyButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;

import com.mojang.blaze3d.systems.RenderSystem;

public class PanelEarlyScreen extends AbstractContainerScreen<PanelEarlyMenu> {
	private final static HashMap<String, Object> guistate = PanelEarlyMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_invest;
	ImageButton imagebutton_oie_transparent;
	ImageButton imagebutton_oie_transparent1;
	ImageButton imagebutton_oie_transparent2;
	ImageButton imagebutton_oie_transparent3;
	ImageButton imagebutton_oie_transparent5;
	ImageButton imagebutton_oie_transparent6;
	ImageButton imagebutton_store;
	ImageButton imagebutton_quests;
	ImageButton imagebutton_abilities;
	ImageButton imagebutton_craftingmenu;

	public PanelEarlyScreen(PanelEarlyMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/panel_early.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		if (ReturnPlayerProcedure.execute(entity) instanceof LivingEntity livingEntity) {
			InventoryScreen.renderEntityInInventoryFollowsAngle(guiGraphics, this.leftPos + 109, this.topPos + -54, 30, 0f + (float) Math.atan((this.leftPos + 109 - mouseX) / 40.0), (float) Math.atan((this.topPos + -103 - mouseY) / 40.0),
					livingEntity);
		}
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -110 && mouseX < leftPos + -86 && mouseY > topPos + 16 && mouseY < topPos + 40)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_early.tooltip_active_quests"), mouseX, mouseY);
		if (mouseX > leftPos + 91 && mouseX < leftPos + 115 && mouseY > topPos + -49 && mouseY < topPos + -25)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_early.tooltip_training"), mouseX, mouseY);
		if (mouseX > leftPos + -109 && mouseX < leftPos + -85 && mouseY > topPos + -16 && mouseY < topPos + 8)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_early.tooltip_daily_rewards"), mouseX, mouseY);
		if (mouseX > leftPos + -109 && mouseX < leftPos + -85 && mouseY > topPos + -49 && mouseY < topPos + -25)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_early.tooltip_shop"), mouseX, mouseY);
		if (mouseX > leftPos + 91 && mouseX < leftPos + 115 && mouseY > topPos + 17 && mouseY < topPos + 41)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_early.tooltip_adjust_abilities"), mouseX, mouseY);
		if (mouseX > leftPos + 91 && mouseX < leftPos + 115 && mouseY > topPos + -16 && mouseY < topPos + 8)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_early.tooltip_craft_special_items"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel3.png"), this.leftPos + -86, this.topPos + -117, 0, 0, 181, 242, 181, 242);

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

				VitalityTextProcedure.execute(entity), 59, 41, -1, false);
		guiGraphics.drawString(this.font,

				StrengthTextProcedure.execute(entity), -7, 42, -1, false);
		guiGraphics.drawString(this.font,

				IntelligenceTextProcedure.execute(entity), 4, 74, -1, false);
		guiGraphics.drawString(this.font,

				SpeedTextProcedure.execute(entity), -19, 57, -1, false);
		guiGraphics.drawString(this.font,

				LevelTextProcedure.execute(entity), 51, -82, -1, false);
		guiGraphics.drawString(this.font,

				SkillPointsTextProcedure.execute(entity), 32, 98, -1, false);
		guiGraphics.drawString(this.font,

				MaxXpTextProcedure.execute(entity), 40, -60, -1, false);
		guiGraphics.drawString(this.font,

				FatigueTextProcedure.execute(entity), 59, -36, -1, false);
		guiGraphics.drawString(this.font,

				HealthTextProcedure.execute(entity), -47, -16, -1, false);
		guiGraphics.drawString(this.font,

				ManaTextGUIProcedure.execute(entity), -48, 6, -1, false);
		guiGraphics.drawString(this.font,

				TitleTextProcedure.execute(entity), -44, -38, -13312, false);
		guiGraphics.drawString(this.font,

				ReturnNameAndGuildProcedure.execute(entity), -40, -83, -3355444, false);
		guiGraphics.drawString(this.font,

				ReturnClassNameProcedure.execute(entity), -46, -61, -3355444, false);
		guiGraphics.drawString(this.font,

				SenseTextProcedure.execute(entity), 49, 57, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.panel_early.label_welcome_player"), -33, -106, -6684673, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.panel_early.label_daily"), -65, 20, -1, false);
		guiGraphics.drawString(this.font,

				TimerTextProcedure.execute(entity), -33, 21, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_invest = new ImageButton(this.leftPos + -64, this.topPos + 42, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest.png"), 7, 14, e -> {
			if (HasSkillPointsProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(0, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (HasSkillPointsProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_invest", imagebutton_invest);
		this.addRenderableWidget(imagebutton_invest);
		imagebutton_oie_transparent = new ImageButton(this.leftPos + -64, this.topPos + 57, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_oie_transparent.png"), 7, 14, e -> {
			if (HasSkillPointsProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(1, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (HasSkillPointsProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_oie_transparent", imagebutton_oie_transparent);
		this.addRenderableWidget(imagebutton_oie_transparent);
		imagebutton_oie_transparent1 = new ImageButton(this.leftPos + 11, this.topPos + 57, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_oie_transparent1.png"), 7, 14, e -> {
			if (HasSkillPointsProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(2, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (HasSkillPointsProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_oie_transparent1", imagebutton_oie_transparent1);
		this.addRenderableWidget(imagebutton_oie_transparent1);
		imagebutton_oie_transparent2 = new ImageButton(this.leftPos + 11, this.topPos + 42, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_oie_transparent2.png"), 7, 14, e -> {
			if (HasSkillPointsProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(3, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (HasSkillPointsProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_oie_transparent2", imagebutton_oie_transparent2);
		this.addRenderableWidget(imagebutton_oie_transparent2);
		imagebutton_oie_transparent3 = new ImageButton(this.leftPos + -64, this.topPos + 74, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_oie_transparent3.png"), 7, 14, e -> {
			if (HasSkillPointsProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(4, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (HasSkillPointsProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_oie_transparent3", imagebutton_oie_transparent3);
		this.addRenderableWidget(imagebutton_oie_transparent3);
		imagebutton_oie_transparent5 = new ImageButton(this.leftPos + -106, this.topPos + -11, 17, 15, 0, 0, 15, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_oie_transparent5.png"), 17, 30, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(5, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_oie_transparent5", imagebutton_oie_transparent5);
		this.addRenderableWidget(imagebutton_oie_transparent5);
		imagebutton_oie_transparent6 = new ImageButton(this.leftPos + 94, this.topPos + -44, 17, 15, 0, 0, 15, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_oie_transparent6.png"), 17, 30, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(6, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		});
		guistate.put("button:imagebutton_oie_transparent6", imagebutton_oie_transparent6);
		this.addRenderableWidget(imagebutton_oie_transparent6);
		imagebutton_store = new ImageButton(this.leftPos + -106, this.topPos + -44, 17, 15, 0, 0, 15, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_store.png"), 17, 30, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(7, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		});
		guistate.put("button:imagebutton_store", imagebutton_store);
		this.addRenderableWidget(imagebutton_store);
		imagebutton_quests = new ImageButton(this.leftPos + -106, this.topPos + 22, 10, 10, 0, 0, 10, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_quests.png"), 10, 20, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(8, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		});
		guistate.put("button:imagebutton_quests", imagebutton_quests);
		this.addRenderableWidget(imagebutton_quests);
		imagebutton_abilities = new ImageButton(this.leftPos + 94, this.topPos + 22, 17, 15, 0, 0, 15, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_abilities.png"), 17, 30, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(9, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		});
		guistate.put("button:imagebutton_abilities", imagebutton_abilities);
		this.addRenderableWidget(imagebutton_abilities);
		imagebutton_craftingmenu = new ImageButton(this.leftPos + 94, this.topPos + -11, 17, 15, 0, 0, 15, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_craftingmenu.png"), 17, 30, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelEarlyButtonMessage(10, x, y, z));
				PanelEarlyButtonMessage.handleButtonAction(entity, 10, x, y, z);
			}
		});
		guistate.put("button:imagebutton_craftingmenu", imagebutton_craftingmenu);
		this.addRenderableWidget(imagebutton_craftingmenu);
	}
}
