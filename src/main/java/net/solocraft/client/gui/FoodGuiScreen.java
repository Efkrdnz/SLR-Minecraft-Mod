package net.solocraft.client.gui;

import net.solocraft.world.inventory.FoodGuiMenu;
import net.solocraft.client.gui.system.SystemContainerScreen;
import net.solocraft.client.gui.system.SystemScreen;
import net.solocraft.network.FoodGuiButtonMessage;
import net.solocraft.SololevelingMod;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.GuiGraphics;

import com.mojang.blaze3d.systems.RenderSystem;

public class FoodGuiScreen extends SystemContainerScreen<FoodGuiMenu> {
	private final int x, y, z;
	private final Player entity;

	// shop slots mirror FoodGuiMenu: index 0 = top, 1 = mid, 2 = bottom
	private static final int SLOT_X = -53;
	private static final int[] SLOT_Y = { -84, -57, -30 };

	public FoodGuiScreen(FoodGuiMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.x = container.x;
		this.y = container.y;
		this.z = container.z;
		this.entity = container.entity;
		this.imageWidth = 0;
		this.imageHeight = 0;
		this.pRelX = -92;
		this.pRelY = -100;
		this.pW = 184;
		this.pH = 196;
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTicks, int gx, int gy) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		ShopStyle.panel(g, leftPos + pRelX, topPos + pRelY, pW, pH);
		ShopStyle.titleBar(g, this.font, leftPos + pRelX, topPos + pRelY, pW, "FOOD SHOP");
		for (int sy : SLOT_Y)
			ShopStyle.slot(g, leftPos + SLOT_X, topPos + sy);
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				ShopStyle.slot(g, leftPos + -79 + sj * 18, topPos + 1 + si * 18);
		for (int si = 0; si < 9; ++si)
			ShopStyle.slot(g, leftPos + -79 + si * 18, topPos + 59);
		RenderSystem.disableBlend();
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_25g_x16"), -30, SLOT_Y[0] + 1, ShopStyle.GOLD, false);
		g.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_x16"), -30, SLOT_Y[0] + 10, ShopStyle.TEXT_SUB, false);
		g.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_40g"), -30, SLOT_Y[1] + 1, ShopStyle.GOLD, false);
		g.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_x8"), -30, SLOT_Y[1] + 10, ShopStyle.TEXT_SUB, false);
		g.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_80g"), -30, SLOT_Y[2] + 1, ShopStyle.GOLD, false);
		g.drawString(this.font, Component.translatable("gui.sololeveling.food_gui.label_x1"), -30, SLOT_Y[2] + 10, ShopStyle.TEXT_SUB, false);
		ShopStyle.gold(g, this.font, entity, pRelX + 6, pRelY + pH - 12);
	}

	@Override
	public void init() {
		super.init();
		for (int i = 0; i < 3; i++) {
			final int id = i;
			PlainTextButton buy = new PlainTextButton(this.leftPos + SLOT_X - 1, this.topPos + SLOT_Y[i] - 1, 18, 18, Component.literal(""), e -> {
				SololevelingMod.PACKET_HANDLER.sendToServer(new FoodGuiButtonMessage(id, x, y, z));
			}, this.font);
			this.addRenderableWidget(buy);
		}
		this.addRenderableWidget(new SystemScreen.SystemButton(this.leftPos + pRelX + 3, this.topPos + pRelY + 2, 40, 12, Component.literal("< Back"), b -> {
			SololevelingMod.PACKET_HANDLER.sendToServer(new FoodGuiButtonMessage(3, x, y, z));
		}));
	}
}
