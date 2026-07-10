package net.solocraft.client.gui;

import net.solocraft.world.inventory.StoreGUIMenu;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemPanelScreen;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.network.StoreGUIButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;

/** System-themed shop category selector (Weapons / Foods / Potions). */
public class StoreGUIScreen extends SystemContainerScreen<StoreGUIMenu> {
	private final int x, y, z;
	private final Player entity;

	public StoreGUIScreen(StoreGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -74;
		this.pRelY = -78;
		this.pW = 148;
		this.pH = 156;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int gx, int gy) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ShopStyle.panel(g, leftPos + pRelX, topPos + pRelY, pW, pH);
		ShopStyle.titleBar(g, this.font, leftPos + pRelX, topPos + pRelY, pW, "SHOP");
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		ShopStyle.gold(g, this.font, entity, pRelX + 6, pRelY + pH - 12);
	}

	private void category(int id) {
		SololevelingMod.PACKET_HANDLER.sendToServer(new StoreGUIButtonMessage(id, x, y, z));
	}

	@Override
	public void init() {
		super.init();
		int bw = 116, bh = 26;
		int bx = this.leftPos + pRelX + (pW - bw) / 2;
		// StoreGUIButtonMessage: 0 = Weapons, 1 = Foods, 2 = Potions
		this.addRenderableWidget(new SystemScreen.SystemButton(bx, this.topPos + pRelY + 30, bw, bh, Component.literal("Weapons"), b -> category(0)));
		this.addRenderableWidget(new SystemScreen.SystemButton(bx, this.topPos + pRelY + 64, bw, bh, Component.literal("Foods"), b -> category(1)));
		this.addRenderableWidget(new SystemScreen.SystemButton(bx, this.topPos + pRelY + 98, bw, bh, Component.literal("Potions"), b -> category(2)));
		this.addRenderableWidget(new SystemScreen.SystemButton(this.leftPos + pRelX + 3, this.topPos + pRelY + 2, 40, 12, Component.literal("< Back"), b -> {
			if (this.minecraft != null && this.minecraft.player != null) {
				this.minecraft.player.closeContainer();
				openSystemScreen(new SystemPanelScreen());
			}
		}));
	}
}
