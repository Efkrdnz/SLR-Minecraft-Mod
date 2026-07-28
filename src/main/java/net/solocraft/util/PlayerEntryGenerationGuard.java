package net.solocraft.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Invalidates delayed player-entry work without retaining player objects in a
 * global collection.
 *
 * <p>The generation is stored on the player so every queued callback can carry
 * only a primitive token. Character reset must call {@link #invalidate} and
 * must preserve {@link #GENERATION_TAG} while clearing other mod-owned player
 * data.</p>
 */
public final class PlayerEntryGenerationGuard {
	public static final String GENERATION_TAG =
			"slr_player_entry_generation";

	private PlayerEntryGenerationGuard() {
	}

	/**
	 * Starts a new mutually-exclusive delayed entry for this player.
	 *
	 * @return the token that every callback belonging to the entry must verify
	 */
	public static long begin(ServerPlayer player) {
		invalidate(player);
		return capture(player);
	}

	public static long capture(ServerPlayer player) {
		return player == null
				? 0L
				: player.getPersistentData().getLong(GENERATION_TAG);
	}

	public static boolean isCurrent(ServerPlayer player, long generation) {
		if (player == null || !player.isAlive() || player.hasDisconnected())
			return false;
		MinecraftServer server = player.getServer();
		return server != null
				&& server.getPlayerList().getPlayer(player.getUUID()) == player
				&& capture(player) == generation;
	}

	public static void invalidate(ServerPlayer player) {
		if (player == null)
			return;
		long current = capture(player);
		long next = current == Long.MAX_VALUE ? 1L : current + 1L;
		if (next == 0L)
			next = 1L;
		player.getPersistentData().putLong(GENERATION_TAG, next);
	}
}
