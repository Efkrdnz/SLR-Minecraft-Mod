package net.solocraft.client.gui;

import net.solocraft.world.inventory.DailyQuestsMenu;
import net.solocraft.procedures.TimerTextProcedure;
import net.solocraft.procedures.SquatTextProcedure;
import net.solocraft.procedures.SitupTextProcedure;
import net.solocraft.procedures.RunTextProcedure;
import net.solocraft.procedures.PushUpTextProcedure;
import net.solocraft.network.DailyQuestsButtonMessage;
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

public class DailyQuestsScreen extends AbstractContainerScreen<DailyQuestsMenu> {
	private final static HashMap<String, Object> guistate = DailyQuestsMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_invest2;
	ImageButton imagebutton_invest21;
	ImageButton imagebutton_invest22;

	public DailyQuestsScreen(DailyQuestsMenu container, Inventory inventory, Component text) {
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

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_vertical2.png"), this.leftPos + -101, this.topPos + -112, 0, 0, 200, 225, 200, 225);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/notice.png"), this.leftPos + -12, this.topPos + -72, 0, 0, 16, 16, 16, 16);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.daily_quests.label_goals"), -17, -55, -13382656, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.daily_quests.label_pushups"), -55, -21, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.daily_quests.label_situps"), -56, -3, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.daily_quests.label_squats"), -55, 16, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.daily_quests.label_run"), -54, 34, -1, false);
		guiGraphics.drawString(this.font,

				PushUpTextProcedure.execute(entity), 13, -20, -1, false);
		guiGraphics.drawString(this.font,

				SitupTextProcedure.execute(entity), 13, -1, -1, false);
		guiGraphics.drawString(this.font,

				SquatTextProcedure.execute(entity), 13, 18, -1, false);
		guiGraphics.drawString(this.font,

				RunTextProcedure.execute(entity), 14, 37, -1, false);
		guiGraphics.drawString(this.font,

				TimerTextProcedure.execute(entity), -53, 66, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_invest2 = new ImageButton(this.leftPos + -3, this.topPos + -19, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest2.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new DailyQuestsButtonMessage(0, x, y, z));
				DailyQuestsButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest2", imagebutton_invest2);
		this.addRenderableWidget(imagebutton_invest2);
		imagebutton_invest21 = new ImageButton(this.leftPos + -8, this.topPos + -2, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest21.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new DailyQuestsButtonMessage(1, x, y, z));
				DailyQuestsButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest21", imagebutton_invest21);
		this.addRenderableWidget(imagebutton_invest21);
		imagebutton_invest22 = new ImageButton(this.leftPos + -13, this.topPos + 18, 7, 7, 0, 0, 7, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_invest22.png"), 7, 14, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new DailyQuestsButtonMessage(2, x, y, z));
				DailyQuestsButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_invest22", imagebutton_invest22);
		this.addRenderableWidget(imagebutton_invest22);
	}
}
