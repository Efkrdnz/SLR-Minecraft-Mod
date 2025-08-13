package net.solocraft.client.gui;

import net.solocraft.world.inventory.ShadowExchangeMainGUIMenu;
import net.solocraft.network.ShadowExchangeMainGUIButtonMessage;
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

public class ShadowExchangeMainGUIScreen extends AbstractContainerScreen<ShadowExchangeMainGUIMenu> {
	private final static HashMap<String, Object> guistate = ShadowExchangeMainGUIMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_panel_rework_downt;
	ImageButton imagebutton_panel_rework_change;

	public ShadowExchangeMainGUIScreen(ShadowExchangeMainGUIMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/shadow_exchange_main_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -33 && mouseX < leftPos + -9 && mouseY > topPos + -9 && mouseY < topPos + 15)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.shadow_exchange_main_gui.tooltip_put_a_shadow_on_ground_to_save_t"), mouseX, mouseY);
		if (mouseX > leftPos + 7 && mouseX < leftPos + 31 && mouseY > topPos + -9 && mouseY < topPos + 15)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.shadow_exchange_main_gui.tooltip_change_locations_with_one_of_the"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_reworked_purple_small_horizontal.png"), this.leftPos + -98, this.topPos + -51, 0, 0, 200, 96, 200, 96);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.shadow_exchange_main_gui.label_sslsslshadow_exchange"), -49, -22, -39169, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_panel_rework_downt = new ImageButton(this.leftPos + -31, this.topPos + -7, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_downt.png"), 20, 40, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeMainGUIButtonMessage(0, x, y, z));
				ShadowExchangeMainGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_rework_downt", imagebutton_panel_rework_downt);
		this.addRenderableWidget(imagebutton_panel_rework_downt);
		imagebutton_panel_rework_change = new ImageButton(this.leftPos + 9, this.topPos + -7, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_change.png"), 20, 40, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShadowExchangeMainGUIButtonMessage(1, x, y, z));
				ShadowExchangeMainGUIButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_rework_change", imagebutton_panel_rework_change);
		this.addRenderableWidget(imagebutton_panel_rework_change);
	}
}
