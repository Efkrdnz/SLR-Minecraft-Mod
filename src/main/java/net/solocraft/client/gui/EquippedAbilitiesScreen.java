package net.solocraft.client.gui;

import net.solocraft.world.inventory.EquippedAbilitiesMenu;
import net.solocraft.procedures.ReturnAbilitySlot8Procedure;
import net.solocraft.procedures.ReturnAbilitySlot7Procedure;
import net.solocraft.procedures.ReturnAbilitySlot6Procedure;
import net.solocraft.procedures.ReturnAbilitySlot5Procedure;
import net.solocraft.procedures.ReturnAbilitySlot4Procedure;
import net.solocraft.procedures.ReturnAbilitySlot3Procedure;
import net.solocraft.procedures.ReturnAbilitySlot2Procedure;
import net.solocraft.procedures.ReturnAbilitySlot1Procedure;
import net.solocraft.network.EquippedAbilitiesButtonMessage;
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

public class EquippedAbilitiesScreen extends AbstractContainerScreen<EquippedAbilitiesMenu> {
	private final static HashMap<String, Object> guistate = EquippedAbilitiesMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_button1;
	ImageButton imagebutton_button11;
	ImageButton imagebutton_button12;
	ImageButton imagebutton_button13;
	ImageButton imagebutton_button14;
	ImageButton imagebutton_button15;
	ImageButton imagebutton_button16;
	ImageButton imagebutton_button17;
	ImageButton imagebutton_buttondeappend;
	ImageButton imagebutton_buttondeappend1;
	ImageButton imagebutton_buttondeappend2;
	ImageButton imagebutton_buttondeappend3;
	ImageButton imagebutton_buttondeappend4;
	ImageButton imagebutton_buttondeappend5;
	ImageButton imagebutton_buttondeappend6;
	ImageButton imagebutton_buttondeappend7;

	public EquippedAbilitiesScreen(EquippedAbilitiesMenu container, Inventory inventory, Component text) {
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

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/equipped_abilities.png");

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
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/big_frame2.png"), this.leftPos + -75, this.topPos + -108, 0, 0, 150, 200, 150, 200);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.equipped_abilities.label_1"), -48, -94, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.equipped_abilities.label_2"), -48, -71, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.equipped_abilities.label_3"), -48, -48, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.equipped_abilities.label_4"), -48, -25, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.equipped_abilities.label_5"), -48, -2, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.equipped_abilities.label_6"), -48, 21, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.equipped_abilities.label_7"), -48, 44, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.equipped_abilities.label_8"), -48, 67, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAbilitySlot1Procedure.execute(entity), -17, -96, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAbilitySlot2Procedure.execute(entity), -17, -73, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAbilitySlot3Procedure.execute(entity), -17, -50, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAbilitySlot4Procedure.execute(entity), -17, -27, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAbilitySlot5Procedure.execute(entity), -17, -5, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAbilitySlot6Procedure.execute(entity), -17, 19, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAbilitySlot7Procedure.execute(entity), -17, 42, -1, false);
		guiGraphics.drawString(this.font,

				ReturnAbilitySlot8Procedure.execute(entity), -17, 65, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_button1 = new ImageButton(this.leftPos + -68, this.topPos + -101, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button1.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(0, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button1", imagebutton_button1);
		this.addRenderableWidget(imagebutton_button1);
		imagebutton_button11 = new ImageButton(this.leftPos + -68, this.topPos + -78, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button11.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(1, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button11", imagebutton_button11);
		this.addRenderableWidget(imagebutton_button11);
		imagebutton_button12 = new ImageButton(this.leftPos + -68, this.topPos + -55, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button12.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(2, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button12", imagebutton_button12);
		this.addRenderableWidget(imagebutton_button12);
		imagebutton_button13 = new ImageButton(this.leftPos + -68, this.topPos + -32, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button13.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(3, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button13", imagebutton_button13);
		this.addRenderableWidget(imagebutton_button13);
		imagebutton_button14 = new ImageButton(this.leftPos + -68, this.topPos + -9, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button14.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(4, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button14", imagebutton_button14);
		this.addRenderableWidget(imagebutton_button14);
		imagebutton_button15 = new ImageButton(this.leftPos + -68, this.topPos + 14, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button15.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(5, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button15", imagebutton_button15);
		this.addRenderableWidget(imagebutton_button15);
		imagebutton_button16 = new ImageButton(this.leftPos + -68, this.topPos + 37, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button16.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(6, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 6, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button16", imagebutton_button16);
		this.addRenderableWidget(imagebutton_button16);
		imagebutton_button17 = new ImageButton(this.leftPos + -68, this.topPos + 60, 48, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_button17.png"), 48, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(7, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 7, x, y, z);
			}
		});
		guistate.put("button:imagebutton_button17", imagebutton_button17);
		this.addRenderableWidget(imagebutton_button17);
		imagebutton_buttondeappend = new ImageButton(this.leftPos + -105, this.topPos + -101, 27, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttondeappend.png"), 27, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(8, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 8, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttondeappend", imagebutton_buttondeappend);
		this.addRenderableWidget(imagebutton_buttondeappend);
		imagebutton_buttondeappend1 = new ImageButton(this.leftPos + -105, this.topPos + -78, 27, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttondeappend1.png"), 27, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(9, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 9, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttondeappend1", imagebutton_buttondeappend1);
		this.addRenderableWidget(imagebutton_buttondeappend1);
		imagebutton_buttondeappend2 = new ImageButton(this.leftPos + -105, this.topPos + -55, 27, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttondeappend2.png"), 27, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(10, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 10, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttondeappend2", imagebutton_buttondeappend2);
		this.addRenderableWidget(imagebutton_buttondeappend2);
		imagebutton_buttondeappend3 = new ImageButton(this.leftPos + -105, this.topPos + -32, 27, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttondeappend3.png"), 27, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(11, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 11, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttondeappend3", imagebutton_buttondeappend3);
		this.addRenderableWidget(imagebutton_buttondeappend3);
		imagebutton_buttondeappend4 = new ImageButton(this.leftPos + -105, this.topPos + -9, 27, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttondeappend4.png"), 27, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(12, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 12, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttondeappend4", imagebutton_buttondeappend4);
		this.addRenderableWidget(imagebutton_buttondeappend4);
		imagebutton_buttondeappend5 = new ImageButton(this.leftPos + -105, this.topPos + 14, 27, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttondeappend5.png"), 27, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(13, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 13, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttondeappend5", imagebutton_buttondeappend5);
		this.addRenderableWidget(imagebutton_buttondeappend5);
		imagebutton_buttondeappend6 = new ImageButton(this.leftPos + -105, this.topPos + 37, 27, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttondeappend6.png"), 27, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(14, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 14, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttondeappend6", imagebutton_buttondeappend6);
		this.addRenderableWidget(imagebutton_buttondeappend6);
		imagebutton_buttondeappend7 = new ImageButton(this.leftPos + -105, this.topPos + 60, 27, 22, 0, 0, 22, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_buttondeappend7.png"), 27, 44, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new EquippedAbilitiesButtonMessage(15, x, y, z));
				EquippedAbilitiesButtonMessage.handleButtonAction(entity, 15, x, y, z);
			}
		});
		guistate.put("button:imagebutton_buttondeappend7", imagebutton_buttondeappend7);
		this.addRenderableWidget(imagebutton_buttondeappend7);
	}
}
