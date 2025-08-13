package net.solocraft.client.gui;

import net.solocraft.world.inventory.PanelRework2Menu;
import net.solocraft.procedures.ReturnVitalityProcedure;
import net.solocraft.procedures.ReturnTitleProcedure;
import net.solocraft.procedures.ReturnStrengthProcedure;
import net.solocraft.procedures.ReturnSPProcedure;
import net.solocraft.procedures.ReturnRemainingXPProcedure;
import net.solocraft.procedures.ReturnPerceptionProcedure;
import net.solocraft.procedures.ReturnNameProcedure;
import net.solocraft.procedures.ReturnMPProcedure;
import net.solocraft.procedures.ReturnLevelProcedure;
import net.solocraft.procedures.ReturnJobProcedure;
import net.solocraft.procedures.ReturnIntelligenceProcedure;
import net.solocraft.procedures.ReturnHPProcedure;
import net.solocraft.procedures.ReturnFatigueProcedure;
import net.solocraft.procedures.ReturnAgilityProcedure;
import net.solocraft.network.PanelRework2ButtonMessage;
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

public class PanelRework2Screen extends AbstractContainerScreen<PanelRework2Menu> {
	private final static HashMap<String, Object> guistate = PanelRework2Menu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_invest2;
	ImageButton imagebutton_invest21;
	ImageButton imagebutton_invest22;
	ImageButton imagebutton_invest23;
	ImageButton imagebutton_invest24;
	ImageButton imagebutton_buttonshop;
	ImageButton imagebutton_buttonquests;
	ImageButton imagebutton_buttonrewards;
	ImageButton imagebutton_buttoncrafting;
	ImageButton imagebutton_buttontrain;
	ImageButton imagebutton_buttonabilities;

	public PanelRework2Screen(PanelRework2Menu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/panel_rework_2.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -50 && mouseX < leftPos + -26 && mouseY > topPos + -93 && mouseY < topPos + -69)
			guiGraphics.renderTooltip(font, Component.literal(ReturnRemainingXPProcedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -75 && mouseX < leftPos + -51 && mouseY > topPos + -93 && mouseY < topPos + -69)
			guiGraphics.renderTooltip(font, Component.literal(ReturnRemainingXPProcedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -155 && mouseX < leftPos + -131 && mouseY > topPos + -86 && mouseY < topPos + -62)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_rework_2.tooltip_open_system_shop"), mouseX, mouseY);
		if (mouseX > leftPos + -157 && mouseX < leftPos + -133 && mouseY > topPos + -19 && mouseY < topPos + 5)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_rework_2.tooltip_collectible_rewards"), mouseX, mouseY);
		if (mouseX > leftPos + -155 && mouseX < leftPos + -131 && mouseY > topPos + 48 && mouseY < topPos + 72)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_rework_2.tooltip_daily_quest"), mouseX, mouseY);
		if (mouseX > leftPos + 138 && mouseX < leftPos + 162 && mouseY > topPos + 60 && mouseY < topPos + 84)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_rework_2.tooltip_settings"), mouseX, mouseY);
		if (mouseX > leftPos + 140 && mouseX < leftPos + 164 && mouseY > topPos + -8 && mouseY < topPos + 16)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_rework_2.tooltip_crafting"), mouseX, mouseY);
		if (mouseX > leftPos + 138 && mouseX < leftPos + 162 && mouseY > topPos + -74 && mouseY < topPos + -50)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_rework_2.tooltip_training"), mouseX, mouseY);
		if (mouseX > leftPos + -75 && mouseX < leftPos + -51 && mouseY > topPos + -63 && mouseY < topPos + -39)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.panel_rework_2.tooltip_titles_wip"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_8.png"), this.leftPos + -149, this.topPos + -120, 0, 0, 300, 240, 300, 240);

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

				ReturnStrengthProcedure.execute(entity), -40, 30, -1, false);
		guiGraphics.drawString(this.font,

				ReturnVitalityProcedure.execute(entity), 27, 30, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAgilityProcedure.execute(entity), -40, 45, -1, false);
		guiGraphics.drawString(this.font,

				ReturnIntelligenceProcedure.execute(entity), 28, 45, -1, false);
		guiGraphics.drawString(this.font,

				ReturnPerceptionProcedure.execute(entity), -40, 61, -1, false);
		guiGraphics.drawString(this.font,

				ReturnSPProcedure.execute(entity), 28, 61, -1, false);
		guiGraphics.drawString(this.font,

				ReturnNameProcedure.execute(entity), -74, -72, -1, false);
		guiGraphics.drawString(this.font,

				ReturnTitleProcedure.execute(entity), -74, -57, -1, false);
		guiGraphics.drawString(this.font,

				ReturnJobProcedure.execute(entity), -74, -41, -1, false);
		guiGraphics.drawString(this.font,

				ReturnFatigueProcedure.execute(entity), -74, -27, -1, false);
		guiGraphics.drawString(this.font,

				ReturnLevelProcedure.execute(entity), -74, -85, -1, false);
		guiGraphics.drawString(this.font,

				ReturnHPProcedure.execute(entity), -79, -9, -1, false);
		guiGraphics.drawString(this.font,

				ReturnMPProcedure.execute(entity), 3, -9, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_invest2 = new ImageButton(this.leftPos + -87, this.topPos + 29, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest2.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(0, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest2", imagebutton_invest2);
		this.addRenderableWidget(imagebutton_invest2);
		imagebutton_invest21 = new ImageButton(this.leftPos + -87, this.topPos + 45, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest21.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(1, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest21", imagebutton_invest21);
		this.addRenderableWidget(imagebutton_invest21);
		imagebutton_invest22 = new ImageButton(this.leftPos + -87, this.topPos + 61, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest22.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(2, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest22", imagebutton_invest22);
		this.addRenderableWidget(imagebutton_invest22);
		imagebutton_invest23 = new ImageButton(this.leftPos + 85, this.topPos + 29, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest23.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(3, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest23", imagebutton_invest23);
		this.addRenderableWidget(imagebutton_invest23);
		imagebutton_invest24 = new ImageButton(this.leftPos + 85, this.topPos + 45, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest24.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(4, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest24", imagebutton_invest24);
		this.addRenderableWidget(imagebutton_invest24);
		imagebutton_buttonshop = new ImageButton(this.leftPos + -159, this.topPos + -90, 32, 32, 0, 0, 32, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttonshop.png"), 32, 64, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(5, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttonshop", imagebutton_buttonshop);
		this.addRenderableWidget(imagebutton_buttonshop);
		imagebutton_buttonquests = new ImageButton(this.leftPos + -159, this.topPos + 44, 32, 32, 0, 0, 32, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttonquests.png"), 32, 64, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(6, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttonquests", imagebutton_buttonquests);
		this.addRenderableWidget(imagebutton_buttonquests);
		imagebutton_buttonrewards = new ImageButton(this.leftPos + -161, this.topPos + -23, 32, 32, 0, 0, 32, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttonrewards.png"), 32, 64, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(7, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttonrewards", imagebutton_buttonrewards);
		this.addRenderableWidget(imagebutton_buttonrewards);
		imagebutton_buttoncrafting = new ImageButton(this.leftPos + 136, this.topPos + -12, 32, 32, 0, 0, 32, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttoncrafting.png"), 32, 64, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(8, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttoncrafting", imagebutton_buttoncrafting);
		this.addRenderableWidget(imagebutton_buttoncrafting);
		imagebutton_buttontrain = new ImageButton(this.leftPos + 134, this.topPos + -78, 32, 32, 0, 0, 32, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttontrain.png"), 32, 64, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(9, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttontrain", imagebutton_buttontrain);
		this.addRenderableWidget(imagebutton_buttontrain);
		imagebutton_buttonabilities = new ImageButton(this.leftPos + 134, this.topPos + 56, 32, 32, 0, 0, 32, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttonabilities.png"), 32, 64, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new PanelRework2ButtonMessage(10, x, y, z));
				PanelRework2ButtonMessage.handleButtonAction(entity, 10, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttonabilities", imagebutton_buttonabilities);
		this.addRenderableWidget(imagebutton_buttonabilities);
	}
}
