package net.solocraft.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.solocraft.network.SololevelingModVariables;

/**
 * Tanker-owned compatibility layer for the shared passive panel. It replaces
 * the legacy "Guard" title with the canonical passive name without modifying
 * the shared multi-class overlay.
 */
@EventBusSubscriber(value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public final class TankerHudOverlay {
	private static final int PANEL_X = 8;
	private static final int NORMAL_PANEL_Y = 76;
	private static final int CREATIVE_PANEL_Y = 8;

	private TankerHudOverlay() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onRenderGui(RenderGuiEvent.Pre event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.options.hideGui
				|| minecraft.getDebugOverlay().showDebugScreen() || minecraft.screen != null)
			return;
		SololevelingModVariables.PlayerVariables vars = minecraft.player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		if (!vars.CustomHUD || (int) Math.round(vars.Classes) != 4)
			return;

		GameType gameType = gameType(minecraft);
		if (gameType == GameType.SPECTATOR)
			return;
		int panelY = gameType == GameType.CREATIVE ? CREATIVE_PANEL_Y : NORMAL_PANEL_Y;
		GuiGraphics graphics = event.getGuiGraphics();

		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 220);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();

		// Cover only the legacy title area; the shared ten-segment bar and value
		// remain the authoritative display fed by ClassPassiveMessage.
		graphics.fill(PANEL_X + 5, panelY + 3, PANEL_X + 75, panelY + 14, 0xC9080D14);
		graphics.drawString(minecraft.font,
				Component.translatable("gui.sololeveling.tanker.iron_wall"),
				PANEL_X + 7, panelY + 4, 0xFFA5D8FF, false);

		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1, 1, 1, 1);
		graphics.pose().popPose();
	}

	private static GameType gameType(Minecraft minecraft) {
		if (minecraft.player == null || minecraft.getConnection() == null)
			return GameType.SURVIVAL;
		PlayerInfo info = minecraft.getConnection().getPlayerInfo(
				minecraft.player.getGameProfile().getId());
		return info == null ? GameType.SURVIVAL : info.getGameMode();
	}
}
