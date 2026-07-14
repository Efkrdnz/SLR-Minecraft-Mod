package net.solocraft.client.screens;

import net.solocraft.client.LiuCombatClientEvents;
import net.solocraft.util.LiuZhigangCombatManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class LiuChargeOverlay {
	private LiuChargeOverlay() {
	}

	@SubscribeEvent(priority = EventPriority.NORMAL)
	public static void render(RenderGuiEvent.Post event) {
		if (!LiuCombatClientEvents.isCharging() || Minecraft.getInstance().options.hideGui)
			return;
		long ticks = LiuCombatClientEvents.getChargeTicks(0.0F);
		GuiGraphics graphics = event.getGuiGraphics();
		int centerX = event.getWindow().getGuiScaledWidth() / 2;
		int y = event.getWindow().getGuiScaledHeight() / 2 + 15;
		int width = 76;
		int x = centerX - width / 2;
		int tier = LiuZhigangCombatManager.beamChargeTier(ticks);
		float progress = Mth.clamp(ticks / (float) LiuZhigangCombatManager.BEAM_TIER_ONE_TICKS,
				0.0F, 1.0F);
		int fill = Math.round((width - 4) * progress);
		int color = switch (tier) {
			case 1 -> 0xFFFFD34E;
			case 2 -> 0xFFFFF0A0;
			case 3 -> 0xFFF2FCFF;
			default -> 0xFFDDA927;
		};

		graphics.fill(x - 2, y - 2, x + width + 2, y + 7, 0xA004080D);
		graphics.fill(x - 1, y - 1, x + width + 1, y + 6, tier >= 3 ? 0xD8FFF3B0 : 0xC7785617);
		graphics.fill(x, y, x + width, y + 5, 0xE00A0D13);
		if (fill > 0)
			graphics.fill(x + 2, y + 1, x + 2 + fill, y + 4, color);
		if (tier >= 1) {
			int shimmer = (int) ((Minecraft.getInstance().level.getGameTime() * (tier + 1L)) % (width - 8));
			graphics.fill(x + 3 + shimmer, y, x + 6 + shimmer, y + 5,
					tier >= 3 ? 0xEFFFFFFF : 0xC0FFF6C2);
		}
		if (tier >= 2) {
			int pulse = (int) (Minecraft.getInstance().level.getGameTime() % 8L);
			graphics.fill(x - 5 - pulse / 3, y + 1, x - 2, y + 4, 0xA0FFF1A0);
			graphics.fill(x + width + 2, y + 1, x + width + 5 + pulse / 3, y + 4, 0xA0FFF1A0);
		}
		if (tier >= 3) {
			graphics.fill(centerX - 1, y - 5, centerX + 1, y - 2, 0xD0FFFFFF);
			graphics.fill(centerX - 1, y + 7, centerX + 1, y + 10, 0xD0FFFFFF);
		}
	}
}
