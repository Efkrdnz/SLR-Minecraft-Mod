package net.solocraft.client.gui;

import net.solocraft.world.inventory.StorePotionNewMenu;
import net.solocraft.procedures.ReturnPotionm3Procedure;
import net.solocraft.procedures.ReturnPotionm2Procedure;
import net.solocraft.procedures.ReturnPotionm1Procedure;
import net.solocraft.procedures.ReturnPotionh3Procedure;
import net.solocraft.procedures.ReturnPotionh2Procedure;
import net.solocraft.procedures.ReturnPotionh1Procedure;
import net.solocraft.procedures.ReturnPotionf3Procedure;
import net.solocraft.procedures.ReturnPotionf2Procedure;
import net.solocraft.procedures.ReturnPotionf1Procedure;
import net.solocraft.procedures.GoldTextProcedure;
import net.solocraft.network.StorePotionNewButtonMessage;
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

public class StorePotionNewScreen extends AbstractContainerScreen<StorePotionNewMenu> {
	private final static HashMap<String, Object> guistate = StorePotionNewMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_panel_rework_back;
	ImageButton imagebutton_panel_potion_health1;
	ImageButton imagebutton_panel_potion_health2;
	ImageButton imagebutton_panel_potion_health3;
	ImageButton imagebutton_panel_potion_mana1;
	ImageButton imagebutton_panel_potion_mana2;
	ImageButton imagebutton_panel_potion_mana3;
	ImageButton imagebutton_panel_potion_fatigue1;
	ImageButton imagebutton_panel_potion_fatigue2;
	ImageButton imagebutton_panel_potion_fatigue3;

	public StorePotionNewScreen(StorePotionNewMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/store_potion_new.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + -91 && mouseX < leftPos + -67 && mouseY > topPos + 46 && mouseY < topPos + 70)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -79 && mouseX < leftPos + -55 && mouseY > topPos + 58 && mouseY < topPos + 82)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -91 && mouseX < leftPos + -67 && mouseY > topPos + 58 && mouseY < topPos + 82)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -79 && mouseX < leftPos + -55 && mouseY > topPos + 46 && mouseY < topPos + 70)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + 58 && mouseY < topPos + 82)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + 58 && mouseY < topPos + 82)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + 46 && mouseY < topPos + 70)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + 46 && mouseY < topPos + 70)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 53 && mouseX < leftPos + 77 && mouseY > topPos + 58 && mouseY < topPos + 82)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 65 && mouseX < leftPos + 89 && mouseY > topPos + 58 && mouseY < topPos + 82)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 53 && mouseX < leftPos + 77 && mouseY > topPos + 46 && mouseY < topPos + 70)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 65 && mouseX < leftPos + 89 && mouseY > topPos + 46 && mouseY < topPos + 70)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -91 && mouseX < leftPos + -67 && mouseY > topPos + -40 && mouseY < topPos + -16)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -79 && mouseX < leftPos + -55 && mouseY > topPos + -40 && mouseY < topPos + -16)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -79 && mouseX < leftPos + -55 && mouseY > topPos + -52 && mouseY < topPos + -28)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -91 && mouseX < leftPos + -67 && mouseY > topPos + -52 && mouseY < topPos + -28)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + -40 && mouseY < topPos + -16)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + -40 && mouseY < topPos + -16)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + -52 && mouseY < topPos + -28)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + -52 && mouseY < topPos + -28)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 53 && mouseX < leftPos + 77 && mouseY > topPos + -52 && mouseY < topPos + -28)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 65 && mouseX < leftPos + 89 && mouseY > topPos + -52 && mouseY < topPos + -28)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 53 && mouseX < leftPos + 77 && mouseY > topPos + -40 && mouseY < topPos + -16)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 65 && mouseX < leftPos + 89 && mouseY > topPos + -40 && mouseY < topPos + -16)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionh3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -91 && mouseX < leftPos + -67 && mouseY > topPos + -3 && mouseY < topPos + 21)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -79 && mouseX < leftPos + -55 && mouseY > topPos + -3 && mouseY < topPos + 21)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -91 && mouseX < leftPos + -67 && mouseY > topPos + 9 && mouseY < topPos + 33)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -79 && mouseX < leftPos + -55 && mouseY > topPos + 58 && mouseY < topPos + 82)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionf1Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + -3 && mouseY < topPos + 21)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + -3 && mouseY < topPos + 21)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -19 && mouseX < leftPos + 5 && mouseY > topPos + 9 && mouseY < topPos + 33)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -7 && mouseX < leftPos + 17 && mouseY > topPos + 9 && mouseY < topPos + 33)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm2Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 53 && mouseX < leftPos + 77 && mouseY > topPos + -3 && mouseY < topPos + 21)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 65 && mouseX < leftPos + 89 && mouseY > topPos + -3 && mouseY < topPos + 21)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 53 && mouseX < leftPos + 77 && mouseY > topPos + 9 && mouseY < topPos + 33)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + 65 && mouseX < leftPos + 89 && mouseY > topPos + 9 && mouseY < topPos + 33)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm3Procedure.execute(entity)), mouseX, mouseY);
		if (mouseX > leftPos + -166 && mouseX < leftPos + -142 && mouseY > topPos + -108 && mouseY < topPos + -84)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.store_potion_new.tooltip_go_to_previous_tab"), mouseX, mouseY);
		if (mouseX > leftPos + -79 && mouseX < leftPos + -55 && mouseY > topPos + 9 && mouseY < topPos + 33)
			guiGraphics.renderTooltip(font, Component.literal(ReturnPotionm1Procedure.execute(entity)), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/panel_rework_empty.png"), this.leftPos + -148, this.topPos + -120, 0, 0, 300, 240, 300, 240);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_potion_new.label_sslpotions"), -21, -88, -12793857, false);
		guiGraphics.drawString(this.font,

				GoldTextProcedure.execute(entity), -91, -70, -26317, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_panel_rework_back = new ImageButton(this.leftPos + -164, this.topPos + -106, 20, 20, 0, 0, 20, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_rework_back.png"), 20, 40, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(0, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_rework_back", imagebutton_panel_rework_back);
		this.addRenderableWidget(imagebutton_panel_rework_back);
		imagebutton_panel_potion_health1 = new ImageButton(this.leftPos + -91, this.topPos + -52, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_health1.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(1, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_health1", imagebutton_panel_potion_health1);
		this.addRenderableWidget(imagebutton_panel_potion_health1);
		imagebutton_panel_potion_health2 = new ImageButton(this.leftPos + -19, this.topPos + -52, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_health2.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(2, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_health2", imagebutton_panel_potion_health2);
		this.addRenderableWidget(imagebutton_panel_potion_health2);
		imagebutton_panel_potion_health3 = new ImageButton(this.leftPos + 53, this.topPos + -52, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_health3.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(3, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_health3", imagebutton_panel_potion_health3);
		this.addRenderableWidget(imagebutton_panel_potion_health3);
		imagebutton_panel_potion_mana1 = new ImageButton(this.leftPos + -91, this.topPos + -3, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_mana1.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(4, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_mana1", imagebutton_panel_potion_mana1);
		this.addRenderableWidget(imagebutton_panel_potion_mana1);
		imagebutton_panel_potion_mana2 = new ImageButton(this.leftPos + -19, this.topPos + -3, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_mana2.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(5, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_mana2", imagebutton_panel_potion_mana2);
		this.addRenderableWidget(imagebutton_panel_potion_mana2);
		imagebutton_panel_potion_mana3 = new ImageButton(this.leftPos + 53, this.topPos + -3, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_mana3.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(6, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_mana3", imagebutton_panel_potion_mana3);
		this.addRenderableWidget(imagebutton_panel_potion_mana3);
		imagebutton_panel_potion_fatigue1 = new ImageButton(this.leftPos + -91, this.topPos + 46, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_fatigue1.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(7, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_fatigue1", imagebutton_panel_potion_fatigue1);
		this.addRenderableWidget(imagebutton_panel_potion_fatigue1);
		imagebutton_panel_potion_fatigue2 = new ImageButton(this.leftPos + -19, this.topPos + 46, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_fatigue2.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(8, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_fatigue2", imagebutton_panel_potion_fatigue2);
		this.addRenderableWidget(imagebutton_panel_potion_fatigue2);
		imagebutton_panel_potion_fatigue3 = new ImageButton(this.leftPos + 53, this.topPos + 46, 36, 36, 0, 0, 36, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_panel_potion_fatigue3.png"), 36, 72, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StorePotionNewButtonMessage(9, x, y, z));
				StorePotionNewButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		});
		guistate.put("button:imagebutton_panel_potion_fatigue3", imagebutton_panel_potion_fatigue3);
		this.addRenderableWidget(imagebutton_panel_potion_fatigue3);
	}
}
