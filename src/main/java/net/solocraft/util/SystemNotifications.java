package net.solocraft.util;

import net.solocraft.network.ShowNotificationMessage;
import net.solocraft.SololevelingMod;

import net.minecraftforge.network.PacketDistributor;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side one-liners for showing a System notification on a player's HUD.
 * Sends a {@link ShowNotificationMessage} to that specific client, which the
 * client forwards to {@code SystemNotificationManager}.
 *
 * <p>Example:
 * {@code SystemNotifications.showTitleUnder(player, 0xFF3FC6FF, 80,
 *   Component.literal("§eLEVEL UP"), Component.literal("§7Lv 5 → 6"));}
 */
public final class SystemNotifications {
	public static final int ACCENT = 0xFF3FC6FF;

	private SystemNotifications() {
	}

	/** Big centered title only. */
	public static void showTitle(ServerPlayer player, int accentColor, int durationTicks, Component title) {
		send(player, new ShowNotificationMessage(accentColor, durationTicks, title, null));
	}

	/** Small centered undertext only. */
	public static void showUnder(ServerPlayer player, int accentColor, int durationTicks, Component undertext) {
		send(player, new ShowNotificationMessage(accentColor, durationTicks, null, undertext));
	}

	/** Big title on top, small undertext below. */
	public static void showTitleUnder(ServerPlayer player, int accentColor, int durationTicks, Component title, Component undertext) {
		send(player, new ShowNotificationMessage(accentColor, durationTicks, title, undertext));
	}

	/** Big title on top, small undertext below, using the negative System sound. */
	public static void showNegativeTitleUnder(ServerPlayer player, int accentColor, int durationTicks, Component title, Component undertext) {
		send(player, new ShowNotificationMessage(accentColor, durationTicks, title, undertext, true));
	}

	private static void send(ServerPlayer player, ShowNotificationMessage message) {
		if (player == null)
			return;
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), message);
	}
}
