package net.solocraft.client.gui;

import net.solocraft.world.inventory.QuestsMenu;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.QuestsButtonMessage;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.util.DkcQuestManager;
import net.solocraft.dkc.DkcFloorRegistry;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Button;
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
	Button jobChangeButton;

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

	private static final ResourceLocation texture = ResourceLocation.parse("sololeveling:textures/screens/quests.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
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
		if (imagebutton_panel_rework_quests_path != null && imagebutton_panel_rework_quests_path.visible
				&& mouseX > leftPos + -19 && mouseX < leftPos + 17 && mouseY > topPos + 2 && mouseY < topPos + 38)
			guiGraphics.renderTooltip(font, dkcButtonTooltip(), mouseX, mouseY);
		if (jobChangeButton != null && jobChangeButton.visible && jobChangeButton.isHovered())
			guiGraphics.renderTooltip(font, Component.literal("Enter the Job Change Quest"), mouseX, mouseY);
	}

	private Component dkcButtonTooltip() {
		if (entity != null && DkcFloorRegistry.isDkc(entity.level())) {
			return Component.literal("Open the Castle Tower");
		}
		if (DkcQuestManager.hasRadiruCastleAccess(entity)) {
			return Component.literal("Travel to Radiru Castle");
		}
		double cleared = entity == null ? 0 : entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).dkc_cleared;
		if (cleared >= 20) {
			return Component.literal("Demon King's Castle conquered");
		}
		return Component.literal("Demon King's Castle Path");
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(ResourceLocation.parse("sololeveling:textures/screens/panel_rework_empty_small.png"), this.leftPos + -101, this.topPos + -85, 0, 0, 200, 160, 200, 160);

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
		updateJobChangeButton();
		updateDkcButton();
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
		imagebutton_panel_rework_quests_daily = new LegacyAtlasImageButton(this.leftPos + -19, this.topPos + -43, 36, 36, 0, 0, 36, ResourceLocation.parse("sololeveling:textures/screens/atlas/imagebutton_panel_rework_quests_daily.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new QuestsButtonMessage(0, x, y, z));
				QuestsButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_rework_quests_daily", imagebutton_panel_rework_quests_daily);
		this.addRenderableWidget(imagebutton_panel_rework_quests_daily);
		imagebutton_panel_rework_quests_path = new LegacyAtlasImageButton(this.leftPos + -19, this.topPos + 2, 36, 36, 0, 0, 36, ResourceLocation.parse("sololeveling:textures/screens/atlas/imagebutton_panel_rework_quests_path.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new QuestsButtonMessage(1, x, y, z));
				QuestsButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_rework_quests_path", imagebutton_panel_rework_quests_path);
		this.addRenderableWidget(imagebutton_panel_rework_quests_path);
		updateDkcButton();
		jobChangeButton = Button.builder(Component.literal("Job Change"), e -> {
			SololevelingMod.PACKET_HANDLER.sendToServer(new QuestsButtonMessage(2, x, y, z));
			QuestsButtonMessage.handleButtonAction(entity, 2, x, y, z);
		}).bounds(this.leftPos + -47, this.topPos + 47, 94, 20).build();
		guistate.put("button:job_change_quest", jobChangeButton);
		this.addRenderableWidget(jobChangeButton);
		updateJobChangeButton();
	}

	private void updateJobChangeButton() {
		if (jobChangeButton == null)
			return;
		boolean show = JobChangeQuestManager.isVisible(entity);
		jobChangeButton.visible = show;
		jobChangeButton.active = show;
	}

	private void updateDkcButton() {
		if (imagebutton_panel_rework_quests_path == null)
			return;
		boolean show = DkcQuestManager.isVisible(entity);
		imagebutton_panel_rework_quests_path.visible = show;
		imagebutton_panel_rework_quests_path.active = show;
	}
}
