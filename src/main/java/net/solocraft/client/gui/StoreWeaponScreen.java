package net.solocraft.client.gui;

import net.solocraft.world.inventory.StoreWeaponMenu;
import net.solocraft.procedures.GoldTextProcedure;
import net.solocraft.procedures.CoinTierSProcedure;
import net.solocraft.procedures.CoinTierEProcedure;
import net.solocraft.procedures.CoinTierDProcedure;
import net.solocraft.procedures.CoinTierCProcedure;
import net.solocraft.procedures.CoinTierBProcedure;
import net.solocraft.procedures.CoinTierAProcedure;
import net.solocraft.network.StoreWeaponButtonMessage;
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

public class StoreWeaponScreen extends AbstractContainerScreen<StoreWeaponMenu> {
	private final static HashMap<String, Object> guistate = StoreWeaponMenu.guistate;
	private final Level world;
	private final int x, y, z;
	private final Player entity;
	ImageButton imagebutton_swordsa;
	ImageButton imagebutton_swordse;
	ImageButton imagebutton_swordsd;
	ImageButton imagebutton_swordsc;
	ImageButton imagebutton_swordsb;
	ImageButton imagebutton_swordss;

	public StoreWeaponScreen(StoreWeaponMenu container, Inventory inventory, Component text) {
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
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		guiGraphics.blit(new ResourceLocation("sololeveling:textures/screens/basewide.png"), this.leftPos + -145, this.topPos + -114, 0, 0, 300, 225, 300, 225);

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
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_weapon.label_weapon_of_choices"), -39, -105, -26317, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_weapon.label_e_rank_5_dmg"), -63, -84, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_weapon.label_d_rank_6_dmg"), -62, -51, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_weapon.label_c_rank_7_dmg"), -62, -20, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_weapon.label_b_rank_9_dmg"), -62, 12, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_weapon.label_a_rank_9_dmg"), -62, 43, -1, false);
		guiGraphics.drawString(this.font, Component.translatable("gui.sololeveling.store_weapon.label_s_rank_11_dmg"), -62, 75, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierEProcedure.execute(entity), 69, -84, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierDProcedure.execute(entity), 69, -52, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierCProcedure.execute(entity), 69, -20, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierBProcedure.execute(entity), 69, 13, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierAProcedure.execute(entity), 69, 43, -1, false);
		guiGraphics.drawString(this.font,

				CoinTierSProcedure.execute(entity), 69, 75, -1, false);
		guiGraphics.drawString(this.font,

				GoldTextProcedure.execute(entity), 69, -105, -1, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	public void init() {
		super.init();
		imagebutton_swordsa = new ImageButton(this.leftPos + -119, this.topPos + 33, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_swordsa.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreWeaponButtonMessage(0, x, y, z));
				StoreWeaponButtonMessage.handleButtonAction(entity, 0, x, y, z);
			}
		});
		guistate.put("button:imagebutton_swordsa", imagebutton_swordsa);
		this.addRenderableWidget(imagebutton_swordsa);
		imagebutton_swordse = new ImageButton(this.leftPos + -119, this.topPos + -95, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_swordse.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreWeaponButtonMessage(1, x, y, z));
				StoreWeaponButtonMessage.handleButtonAction(entity, 1, x, y, z);
			}
		});
		guistate.put("button:imagebutton_swordse", imagebutton_swordse);
		this.addRenderableWidget(imagebutton_swordse);
		imagebutton_swordsd = new ImageButton(this.leftPos + -119, this.topPos + -63, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_swordsd.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreWeaponButtonMessage(2, x, y, z));
				StoreWeaponButtonMessage.handleButtonAction(entity, 2, x, y, z);
			}
		});
		guistate.put("button:imagebutton_swordsd", imagebutton_swordsd);
		this.addRenderableWidget(imagebutton_swordsd);
		imagebutton_swordsc = new ImageButton(this.leftPos + -119, this.topPos + -31, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_swordsc.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreWeaponButtonMessage(3, x, y, z));
				StoreWeaponButtonMessage.handleButtonAction(entity, 3, x, y, z);
			}
		});
		guistate.put("button:imagebutton_swordsc", imagebutton_swordsc);
		this.addRenderableWidget(imagebutton_swordsc);
		imagebutton_swordsb = new ImageButton(this.leftPos + -119, this.topPos + 1, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_swordsb.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreWeaponButtonMessage(4, x, y, z));
				StoreWeaponButtonMessage.handleButtonAction(entity, 4, x, y, z);
			}
		});
		guistate.put("button:imagebutton_swordsb", imagebutton_swordsb);
		this.addRenderableWidget(imagebutton_swordsb);
		imagebutton_swordss = new ImageButton(this.leftPos + -119, this.topPos + 65, 33, 33, 0, 0, 33, new ResourceLocation("sololeveling:textures/screens/atlas/imagebutton_swordss.png"), 33, 66, e -> {
			if (true) {
				SololevelingMod.PACKET_HANDLER.sendToServer(new StoreWeaponButtonMessage(5, x, y, z));
				StoreWeaponButtonMessage.handleButtonAction(entity, 5, x, y, z);
			}
		});
		guistate.put("button:imagebutton_swordss", imagebutton_swordss);
		this.addRenderableWidget(imagebutton_swordss);
	}
}
