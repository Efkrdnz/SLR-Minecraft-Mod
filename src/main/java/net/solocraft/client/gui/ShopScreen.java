package net.solocraft.client.gui;

import net.solocraft.world.inventory.ShopMenu;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.procedures.ReturnShopSwords6Procedure;
import net.solocraft.procedures.ReturnShopSwords5Procedure;
import net.solocraft.procedures.ReturnShopSwords4Procedure;
import net.solocraft.procedures.ReturnShopSwords3Procedure;
import net.solocraft.procedures.ReturnShopSwords2Procedure;
import net.solocraft.procedures.ReturnShopSwords1Procedure;
import net.solocraft.procedures.ReturnRefreshButtonProcedure;
import net.solocraft.network.ShopButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;

public class ShopScreen extends SystemContainerScreen<ShopMenu> {
	private final int x, y, z;
	private final Player entity;

	private static final int SLOT_X = -30;
	private static final int[] SLOT_Y = { -60, -36, -12, 12, 36, 60 };

	private AbstractWidget refreshButton;

	public ShopScreen(ShopMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -54;
		this.pRelY = -96;
		this.pW = 172;
		this.pH = 192;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int gx, int gy) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ShopStyle.panel(g, leftPos + pRelX, topPos + pRelY, pW, pH);
		ShopStyle.titleBar(g, this.font, leftPos + pRelX, topPos + pRelY, pW, "WEAPON SHOP");
		for (int sy : SLOT_Y)
			ShopStyle.slot(g, leftPos + SLOT_X, topPos + sy);
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(this.font, ReturnShopSwords1Procedure.execute(entity), -6, SLOT_Y[0] + 4, ShopStyle.TEXT_MAIN, false);
		g.drawString(this.font, ReturnShopSwords2Procedure.execute(entity), -6, SLOT_Y[1] + 4, ShopStyle.TEXT_MAIN, false);
		g.drawString(this.font, ReturnShopSwords3Procedure.execute(entity), -6, SLOT_Y[2] + 4, ShopStyle.TEXT_MAIN, false);
		g.drawString(this.font, ReturnShopSwords4Procedure.execute(entity), -6, SLOT_Y[3] + 4, ShopStyle.TEXT_MAIN, false);
		g.drawString(this.font, ReturnShopSwords5Procedure.execute(entity), -6, SLOT_Y[4] + 4, ShopStyle.TEXT_MAIN, false);
		g.drawString(this.font, ReturnShopSwords6Procedure.execute(entity), -6, SLOT_Y[5] + 4, ShopStyle.TEXT_MAIN, false);
		ShopStyle.gold(g, this.font, entity, pRelX + 6, pRelY + pH - 12);
	}

	@Override
	protected void renderExtras(GuiGraphics g, int mouseX, int mouseY) {
		if (refreshButton != null && refreshButton.isMouseOver(mouseX, mouseY))
			g.renderTooltip(font, Component.literal(ReturnRefreshButtonProcedure.execute(entity)), mouseX, mouseY);
	}

	@Override
	public void init() {
		super.init();
		for (int i = 0; i < 6; i++) {
			final int id = i;
			PlainTextButton buy = new PlainTextButton(this.leftPos + SLOT_X - 1, this.topPos + SLOT_Y[i] - 1, 18, 18, Component.literal(""), e -> {
				SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(id, x, y, z));
			}, this.font);
			this.addRenderableWidget(buy);
		}
		this.addRenderableWidget(new SystemScreen.SystemButton(this.leftPos + pRelX + 3, this.topPos + pRelY + 2, 40, 12, Component.literal("< Back"), b -> {
			SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(6, x, y, z));
		}));
		refreshButton = new SystemScreen.SystemButton(this.leftPos + pRelX + pW - 51, this.topPos + pRelY + 2, 48, 12, Component.literal("Refresh"), b -> {
			SololevelingMod.PACKET_HANDLER.sendToServer(new ShopButtonMessage(7, x, y, z));
		});
		this.addRenderableWidget(refreshButton);
	}
}
