package net.solocraft.client.gui;

import net.solocraft.world.inventory.QuestsMenu;
import net.solocraft.network.QuestsButtonMessage;
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

public class QuestsScreen extends AbstractContainerScreen<QuestsMenu> {
	private final static HashMap<String, Object> guistate = QuestsMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_panel_rework_quests_daily;
	ImageButton imagebutton_panel_rework_quests_path;

	public QuestsScreen(QuestsMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/quests.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + -43 && mouseY < topPos + -19)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.quests.tooltip_daily_quests"), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + -43 && mouseY < topPos + -19)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.quests.tooltip_daily_quests1"), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + -31 && mouseY < topPos + -7)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.quests.tooltip_daily_quests2"), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + -31 && mouseY < topPos + -7)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.quests.tooltip_daily_quests3"), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + 2 && mouseY < topPos + 26)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.quests.tooltip_path"), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + 2 && mouseY < topPos + 26)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.quests.tooltip_path1"), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + 14 && mouseY < topPos + 38)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.quests.tooltip_path2"), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + 14 && mouseY < topPos + 38)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.quests.tooltip_path3"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_empty_small.png"), this.leftPos + -101, this.topPos + -85, 0, 0, 200, 160, 200, 160);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.quests.label_sslquests_tab"), -29, -59, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_panel_rework_quests_daily = new ImageButton(this.leftPos + -19, this.topPos + -43, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_quests_daily.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new QuestsButtonMessage(0, x, y, z));
				QuestsButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_rework_quests_daily", imagebutton_panel_rework_quests_daily);
		this.addRenderableWidget(imagebutton_panel_rework_quests_daily);
		imagebutton_panel_rework_quests_path = new ImageButton(this.leftPos + -19, this.topPos + 2, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_quests_path.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new QuestsButtonMessage(1, x, y, z));
				QuestsButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_rework_quests_path", imagebutton_panel_rework_quests_path);
		this.addRenderableWidget(imagebutton_panel_rework_quests_path);
	}
}
