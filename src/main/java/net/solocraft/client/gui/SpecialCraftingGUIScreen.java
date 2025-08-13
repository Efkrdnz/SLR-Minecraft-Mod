package net.solocraft.client.gui;

import net.solocraft.world.inventory.SpecialCraftingGUIMenu;
import net.solocraft.network.SpecialCraftingGUIButtonMessage;
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

public class SpecialCraftingGUIScreen extends AbstractContainerScreen<SpecialCraftingGUIMenu> {
	private final static HashMap<String, Object> guistate = SpecialCraftingGUIMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_systemcraftbutton;

	public SpecialCraftingGUIScreen(SpecialCraftingGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 170;
		this.imageHeight = 158;
	}

	private static final ResourceLocation texture = new ResourceLocation("sololeveling:textures/screens/special_crafting_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
		if (mouseX > leftPos + 47 && mouseX < leftPos + 71 && mouseY > topPos + -18 && mouseY < topPos + 6)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.special_crafting_gui.tooltip_weapon_or_rune"), mouseX, mouseY);
		if (mouseX > leftPos + 111 && mouseX < leftPos + 135 && mouseY > topPos + -4 && mouseY < topPos + 20)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.special_crafting_gui.tooltip_potion_or_powerstone"), mouseX, mouseY);
		if (mouseX > leftPos + 65 && mouseX < leftPos + 89 && mouseY > topPos + 41 && mouseY < topPos + 65)
			guiGraphics.renderTooltip(font, Component.translatable("gui.sololeveling.special_crafting_gui.tooltip_manastone"), mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/systemcrafting.png"), this.leftPos + 10, this.topPos + -36, 0, 0, 150, 112, 150, 112);

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
		imagebutton_systemcraftbutton = new ImageButton(this.leftPos + 103, this.topPos + 33, 44, 39, 0, 0, 39, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_systemcraftbutton.png"), 44, 78, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new SpecialCraftingGUIButtonMessage(0, x, y, z));
				SpecialCraftingGUIButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_systemcraftbutton", imagebutton_systemcraftbutton);
		this.addRenderableWidget(imagebutton_systemcraftbutton);
	}
}
