package net.solocraft.client.gui;

import net.solocraft.world.inventory.ChooseClassMenu;
import net.solocraft.network.ChooseClassButtonMessage;
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

public class ChooseClassScreen extends AbstractContainerScreen<ChooseClassMenu> {
	private final static HashMap<String, Object> guistate = ChooseClassMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_classchooseassasin;
	ImageButton imagebutton_classchoosemage;
	ImageButton imagebutton_classchooseknight;
	ImageButton imagebutton_classchoosetank;
	ImageButton imagebutton_classchoosehealer;
	ImageButton imagebutton_classchoosearcher;

	public ChooseClassScreen(ChooseClassMenu container, Inventory inventory, Component text) {
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
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -67 && mouseX < leftPos + -43 && mouseY > topPos + -65 && mouseY < topPos + -41)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_speed_and_strenght_fro"), mouseX, mouseY);
		if (mouseX > leftPos + -24 && mouseX < leftPos + 0 && mouseY > topPos + -65 && mouseY < topPos + -41)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_speed_and_strenght_fro1"), mouseX, mouseY);
		if (mouseX > leftPos + -45 && mouseX < leftPos + -21 && mouseY > topPos + -65 && mouseY < topPos + -41)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_speed_and_strenght_fro2"), mouseX, mouseY);
		if (mouseX > leftPos + 0 && mouseX < leftPos + 24 && mouseY > topPos + -65 && mouseY < topPos + -41)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_passive_mana_regenerat"), mouseX, mouseY);
		if (mouseX > leftPos + 24 && mouseX < leftPos + 48 && mouseY > topPos + -65 && mouseY < topPos + -41)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_passive_mana_regenerat1"), mouseX, mouseY);
		if (mouseX > leftPos + 43 && mouseX < leftPos + 67 && mouseY > topPos + -65 && mouseY < topPos + -41)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_passive_mana_regenerat2"), mouseX, mouseY);
		if (mouseX > leftPos + -67 && mouseX < leftPos + -43 && mouseY > topPos + -28 && mouseY < topPos + -4)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_damage_from_using_swor"), mouseX, mouseY);
		if (mouseX > leftPos + -45 && mouseX < leftPos + -21 && mouseY > topPos + -28 && mouseY < topPos + -4)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_damage_from_using_swor1"), mouseX, mouseY);
		if (mouseX > leftPos + -24 && mouseX < leftPos + 0 && mouseY > topPos + -28 && mouseY < topPos + -4)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_damage_from_using_swor2"), mouseX, mouseY);
		if (mouseX > leftPos + 0 && mouseX < leftPos + 24 && mouseY > topPos + -28 && mouseY < topPos + -4)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_strenght_and_defence_w"), mouseX, mouseY);
		if (mouseX > leftPos + 21 && mouseX < leftPos + 45 && mouseY > topPos + -28 && mouseY < topPos + -4)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_strenght_and_defence_w1"), mouseX, mouseY);
		if (mouseX > leftPos + 43 && mouseX < leftPos + 67 && mouseY > topPos + -28 && mouseY < topPos + -4)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_increased_strenght_and_defence_w2"), mouseX, mouseY);
		if (mouseX > leftPos + -45 && mouseX < leftPos + -21 && mouseY > topPos + 11 && mouseY < topPos + 35)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_passive_regeneration_effects_on"), mouseX, mouseY);
		if (mouseX > leftPos + -67 && mouseX < leftPos + -43 && mouseY > topPos + 10 && mouseY < topPos + 34)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_passive_regeneration_effects_on1"), mouseX, mouseY);
		if (mouseX > leftPos + -24 && mouseX < leftPos + 0 && mouseY > topPos + 11 && mouseY < topPos + 35)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_passive_regeneration_effects_on2"), mouseX, mouseY);
		if (mouseX > leftPos + 0 && mouseX < leftPos + 24 && mouseY > topPos + 10 && mouseY < topPos + 34)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_utilize_special_bows_and_use_man"), mouseX, mouseY);
		if (mouseX > leftPos + 21 && mouseX < leftPos + 45 && mouseY > topPos + 10 && mouseY < topPos + 34)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_utilize_special_bows_and_use_man1"), mouseX, mouseY);
		if (mouseX > leftPos + 42 && mouseX < leftPos + 66 && mouseY > topPos + 10 && mouseY < topPos + 34)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.choose_class.tooltip_utilize_special_bows_and_use_man2"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/techbase.png"), this.leftPos + -75, this.topPos + -73, 0, 0, 150, 150, 150, 150);

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
		imagebutton_classchooseassasin = new ImageButton(this.leftPos + -65, this.topPos + -63, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_classchooseassasin.png"), 64, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ChooseClassButtonMessage(0, x, y, z));
				ChooseClassButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_classchooseassasin", imagebutton_classchooseassasin);
		this.addRenderableWidget(imagebutton_classchooseassasin);
		imagebutton_classchoosemage = new ImageButton(this.leftPos + 1, this.topPos + -63, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_classchoosemage.png"), 64, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ChooseClassButtonMessage(1, x, y, z));
				ChooseClassButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_classchoosemage", imagebutton_classchoosemage);
		this.addRenderableWidget(imagebutton_classchoosemage);
		imagebutton_classchooseknight = new ImageButton(this.leftPos + -65, this.topPos + -26, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_classchooseknight.png"), 64, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ChooseClassButtonMessage(2, x, y, z));
				ChooseClassButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_classchooseknight", imagebutton_classchooseknight);
		this.addRenderableWidget(imagebutton_classchooseknight);
		imagebutton_classchoosetank = new ImageButton(this.leftPos + 1, this.topPos + -26, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_classchoosetank.png"), 64, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ChooseClassButtonMessage(3, x, y, z));
				ChooseClassButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		});
		guistate.put("button:imagebutton_classchoosetank", imagebutton_classchoosetank);
		this.addRenderableWidget(imagebutton_classchoosetank);
		imagebutton_classchoosehealer = new ImageButton(this.leftPos + -65, this.topPos + 11, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_classchoosehealer.png"), 64, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ChooseClassButtonMessage(4, x, y, z));
				ChooseClassButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		});
		guistate.put("button:imagebutton_classchoosehealer", imagebutton_classchoosehealer);
		this.addRenderableWidget(imagebutton_classchoosehealer);
		imagebutton_classchoosearcher = new ImageButton(this.leftPos + 1, this.topPos + 11, 64, 21, 0, 0, 21, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_classchoosearcher.png"), 64, 42, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ChooseClassButtonMessage(5, x, y, z));
				ChooseClassButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_classchoosearcher", imagebutton_classchoosearcher);
		this.addRenderableWidget(imagebutton_classchoosearcher);
	}
}
