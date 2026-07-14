package net.solocraft.client.screens;

import net.solocraft.util.BeastHuntClientState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class BeastHuntOverlay {
	private static final int WIDTH = 184;
	private static final int HEIGHT = 35;

	private BeastHuntOverlay() {
	}

	@SubscribeEvent
	public static void render(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.player.isSpectator() || minecraft.options.hideGui
				|| minecraft.options.renderDebug || minecraft.screen != null || !BeastHuntClientState.isActive())
			return;

		GuiGraphics graphics = event.getGuiGraphics();
		Font font = minecraft.font;
		int x = (event.getWindow().getGuiScaledWidth() - WIDTH) / 2;
		int y = event.getWindow().getGuiScaledHeight() - 78;
		int border = BeastHuntClientState.isStance() ? 0xFFD32A31 : 0xFF5B3033;
		graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xD306070A);
		graphics.fill(x, y, x + WIDTH, y + 1, border);
		graphics.fill(x, y + HEIGHT - 1, x + WIDTH, y + HEIGHT, 0xAA3B0D11);
		graphics.fill(x, y, x + 1, y + HEIGHT, 0xAA3B0D11);
		graphics.fill(x + WIDTH - 1, y, x + WIDTH, y + HEIGHT, border);

		String quarry = BeastHuntClientState.quarryName();
		if (quarry.length() > 19)
			quarry = quarry.substring(0, 18) + ".";
		graphics.drawString(font, Component.literal("QUARRY  " + quarry), x + 7, y + 5,
				BeastHuntClientState.quarryName().equals("NO QUARRY") ? 0xFF8C797A : 0xFFFFE0C1, false);

		String cadence = BeastHuntClientState.combo() == 0 ? "FANG STANCE"
				: "CADENCE " + BeastHuntClientState.combo() + "/3";
		graphics.drawString(font, Component.literal(cadence), x + WIDTH - 7 - font.width(cadence), y + 5,
				BeastHuntClientState.isStance() ? 0xFFFF4A4F : 0xFF806E70, false);

		int segmentX = x + 7;
		int segmentY = y + 20;
		int gap = 2;
		int segmentWidth = 37;
		int hunt = BeastHuntClientState.hunt();
		for (int i = 0; i < 4; i++) {
			int start = i * 25;
			int amount = Math.max(0, Math.min(25, hunt - start));
			int sx = segmentX + i * (segmentWidth + gap);
			graphics.fill(sx, segmentY, sx + segmentWidth, segmentY + 8, 0xFF1A0B0D);
			if (amount > 0) {
				int filled = Math.round(segmentWidth * amount / 25.0F);
				int color = i >= 3 ? 0xFFFFD6A2 : 0xFFE12530;
				graphics.fill(sx, segmentY, sx + filled, segmentY + 8, color);
				graphics.fill(sx, segmentY, sx + filled, segmentY + 1, 0xB0FFFFFF);
			}
		}

		int pipX = x + WIDTH - 16;
		for (int i = 0; i < 2; i++) {
			int color = i < BeastHuntClientState.openings() ? 0xFFFFE2B8 : 0xFF371316;
			graphics.fill(pipX + i * 6, segmentY + 1, pipX + 4 + i * 6, segmentY + 7, color);
		}
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		BeastHuntClientState.clear();
	}
}
