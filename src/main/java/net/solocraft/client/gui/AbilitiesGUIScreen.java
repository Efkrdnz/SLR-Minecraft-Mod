package net.solocraft.client.gui;

import net.solocraft.world.inventory.AbilitiesGUIMenu;
import net.solocraft.procedures.TripleJumpButtonDisplayConProcedure;
import net.solocraft.procedures.SpeedPercent90Procedure;
import net.solocraft.procedures.SpeedPercent80Procedure;
import net.solocraft.procedures.SpeedPercent70Procedure;
import net.solocraft.procedures.SpeedPercent60Procedure;
import net.solocraft.procedures.SpeedPercent50Procedure;
import net.solocraft.procedures.SpeedPercent40Procedure;
import net.solocraft.procedures.SpeedPercent30Procedure;
import net.solocraft.procedures.SpeedPercent20Procedure;
import net.solocraft.procedures.SpeedPercent10Procedure;
import net.solocraft.procedures.SpeedPercent100Procedure;
import net.solocraft.procedures.SpeedPercent0Procedure;
import net.solocraft.procedures.SMonTextProcedure;
import net.solocraft.procedures.IsPlayerProcedure;
import net.solocraft.network.AbilitiesGUIButtonMessage;
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

public class AbilitiesGUIScreen extends AbstractContainerScreen<AbilitiesGUIMenu> {
	private final static HashMap<String, Object> guistate = AbilitiesGUIMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_back;
	ImageButton imagebutton_guiback;
	ImageButton imagebutton_guiforward;
	ImageButton imagebutton_guiabilitytj;
	ImageButton imagebutton_job1;
	ImageButton imagebutton_panel_rework_rewardbutton;

	public AbilitiesGUIScreen(AbilitiesGUIMenu container, Inventory inventory, Component text) {
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
		if (IsPlayerProcedure.execute(entity))
			if (mouseX > leftPos + -126 && mouseX < leftPos + -102 && mouseY > topPos + -23 && mouseY < topPos + 1)
				guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.abilities_gui.tooltip_back_to_main_panel"), mouseX, mouseY);
		if (mouseX > leftPos + -44 && mouseX < leftPos + -20 && mouseY > topPos + -11 && mouseY < topPos + 13)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.abilities_gui.tooltip_toggle_triple_jump"), mouseX, mouseY);
		if (mouseX > leftPos + 96 && mouseX < leftPos + 120 && mouseY > topPos + -116 && mouseY < topPos + -92)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.abilities_gui.tooltip_adjust_your_job_abilities"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_vertical2.png"), this.leftPos + -103, this.topPos + -114, 0, 0, 200, 225, 200, 225);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/guipercent.png"), this.leftPos + -17, this.topPos + -61, 0, 0, 16, 16, 16, 16);

		if (SpeedPercent10Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui10percent.png"), this.leftPos + -3, this.topPos + -61, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent20Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui20percent.png"), this.leftPos + -3, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent30Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui30percent.png"), this.leftPos + -3, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent40Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui40percent.png"), this.leftPos + -2, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent50Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui50percent.png"), this.leftPos + -3, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent60Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui60percent.png"), this.leftPos + -3, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent70Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui70percent.png"), this.leftPos + -3, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent80Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui80percent.png"), this.leftPos + -3, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent90Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui90percent.png"), this.leftPos + -3, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (SpeedPercent100Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui100percent.png"), this.leftPos + -4, this.topPos + -62, 0, 0, 16, 16, 16, 16);
		}
		if (TripleJumpButtonDisplayConProcedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/guiabilitytjon.png"), this.leftPos + -48, this.topPos + -15, 0, 0, 32, 32, 32, 32);
		}
		if (SpeedPercent0Procedure.execute(entity)) {
			guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/gui0percent.png"), this.leftPos + -8, this.topPos + -61, 0, 0, 16, 16, 16, 16);
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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.abilities_gui.label_speed_adjustment"), -42, -76, -6697729, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.abilities_gui.label_abilities"), -23, -35, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_back = new ImageButton(this.leftPos + -124, this.topPos + -21, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_back.png"), 20, 40, e -> {
			if (IsPlayerProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(0, x, y, z));
				AbilitiesGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (IsPlayerProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_back", imagebutton_back);
		this.addRenderableWidget(imagebutton_back);
		imagebutton_guiback = new ImageButton(this.leftPos + -36, this.topPos + -61, 16, 16, 0, 0, 16, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_guiback.png"), 16, 32, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(1, x, y, z));
				AbilitiesGUIButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_guiback", imagebutton_guiback);
		this.addRenderableWidget(imagebutton_guiback);
		imagebutton_guiforward = new ImageButton(this.leftPos + 18, this.topPos + -61, 16, 16, 0, 0, 16, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_guiforward.png"), 16, 32, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(2, x, y, z));
				AbilitiesGUIButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_guiforward", imagebutton_guiforward);
		this.addRenderableWidget(imagebutton_guiforward);
		imagebutton_guiabilitytj = new ImageButton(this.leftPos + -48, this.topPos + -15, 32, 32, 0, 0, 32, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_guiabilitytj.png"), 32, 64, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(3, x, y, z));
				AbilitiesGUIButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		});
		guistate.put("button:imagebutton_guiabilitytj", imagebutton_guiabilitytj);
		this.addRenderableWidget(imagebutton_guiabilitytj);
		imagebutton_job1 = new ImageButton(this.leftPos + 96, this.topPos + -117, 25, 25, 0, 0, 25, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_job1.png"), 25, 50, e -> {
			if (SMonTextProcedure.execute(entity)) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(4, x, y, z));
				AbilitiesGUIButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		}) {
			@Override
			public void render(GuiGraphics guiGraphics, int gx, int gy, float ticks) {
				if (SMonTextProcedure.execute(entity))
					super.render(guiGraphics, gx, gy, ticks);
			}
		};
		guistate.put("button:imagebutton_job1", imagebutton_job1);
		this.addRenderableWidget(imagebutton_job1);
		imagebutton_panel_rework_rewardbutton = new ImageButton(this.leftPos + -34, this.topPos + -41, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_rewardbutton.png"), 64, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new AbilitiesGUIButtonMessage(5, x, y, z));
				AbilitiesGUIButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_rework_rewardbutton", imagebutton_panel_rework_rewardbutton);
		this.addRenderableWidget(imagebutton_panel_rework_rewardbutton);
	}
}
