package net.solocraft.util.daily;

import net.minecraftforge.eventbus.api.Event;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;

/**
 * Fired once, on the logical server, when all objectives for the active Daily
 * Quest are complete. This event is observational: the existing quest/reward
 * owner remains responsible for rewards, secret-quest unlocks, timer cleanup,
 * and setting {@code ActiveDaily} to false.
 */
public final class DailyQuestObjectivesCompletedEvent extends Event {
	private final ServerPlayer player;
	private final DailyQuestObjectiveManager.ProgressSnapshot progress;

	public DailyQuestObjectivesCompletedEvent(ServerPlayer player,
			DailyQuestObjectiveManager.ProgressSnapshot progress) {
		this.player = Objects.requireNonNull(player, "player");
		this.progress = Objects.requireNonNull(progress, "progress");
	}

	public ServerPlayer getPlayer() {
		return player;
	}

	public DailyQuestObjectiveManager.ProgressSnapshot getProgress() {
		return progress;
	}
}
