package net.solocraft.client.gui;

import net.solocraft.network.DungeonBuilderStatusMessage;

/** Latest server snapshot for the client-only Dungeon Builder HUD. */
public final class DungeonBuilderClientState {
	private static final long STALE_AFTER_MS = 3_000L;
	private static DungeonBuilderStatusMessage.View view = DungeonBuilderStatusMessage.View.inactive();
	private static long receivedAt;

	private DungeonBuilderClientState() {
	}

	public static void update(DungeonBuilderStatusMessage.View next) {
		view = next == null ? DungeonBuilderStatusMessage.View.inactive() : next;
		receivedAt = System.currentTimeMillis();
	}

	public static DungeonBuilderStatusMessage.View view() {
		if (System.currentTimeMillis() - receivedAt > STALE_AFTER_MS)
			return DungeonBuilderStatusMessage.View.inactive();
		return view;
	}
}
