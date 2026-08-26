package net.solocraft.procedures;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

/**
 * Retired compatibility shell. Hunter Evaluation is now driven exclusively by
 * {@code HunterEvaluationManager}; this class deliberately subscribes to no
 * events and applies no legacy rewards.
 */
@Deprecated(forRemoval = false)
public final class EvaluationTimerProcedure {
	private EvaluationTimerProcedure() {
	}

	public static void onPlayerTick(PlayerTickEvent.Post event) {
		// No-op for binary/source compatibility with generated integrations.
	}

	public static void execute(LevelAccessor world, double x, double y,
			double z, Entity entity) {
		// No-op. Start or resume through an Evaluator crystal.
	}
}
