package net.solocraft.util;

import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

/**
 * Who, if anyone, is still administering the System.
 *
 * <p>The System is never destroyed. At the end of the run the player leaves it
 * running with nobody at the controls: it stops issuing quests, stops paying out
 * rewards and stops handing out levels, while everything already earned -- every
 * ability, stat, shadow and the shop -- keeps working exactly as before. This is
 * the single place that answers "is anyone still running this", so the systems
 * that shut down do not each invent their own check.
 *
 * <p>Read this rather than {@code vars.systemReleased} directly. The flag alone
 * does not carry the level-cap half of the story, and callers that reached past
 * a helper like this are how {@code SystemPlayerAccess} ended up needed in the
 * first place.
 */
public final class SystemAuthorityManager {
	private SystemAuthorityManager() {
	}

	/** True once the player has released the System at the Statue of God. */
	public static boolean isReleased(Entity entity) {
		if (entity == null)
			return false;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.systemReleased)
				.orElse(false);
	}

	/**
	 * True while the System is still handing out progression. Everything that
	 * awards XP, quests or skill points should gate on this.
	 */
	public static boolean isAttended(Entity entity) {
		return !isReleased(entity);
	}

	/** The configured peak for this world, already normalized. */
	public static int levelCap(LevelAccessor world) {
		if (world == null)
			return LevelCapRules.DEFAULT_LEVEL_CAP;
		return LevelCapRules.resolveCap(world.getLevelData().getGameRules()
				.getInt(SololevelingModGameRules.SOLO_LEVELING_LEVEL_CAP));
	}

	/** True when the player has reached this world's peak and can still act on it. */
	public static boolean hasReachedPeak(Entity entity) {
		if (entity == null || !SystemPlayerAccess.hasSystem(entity))
			return false;
		Level level = entity.level();
		int cap = levelCap(level);
		if (LevelCapRules.isUnlimited(cap))
			return false;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> LevelCapRules.isCapped((int) variables.Level, cap))
				.orElse(false);
	}

	/**
	 * True when the finale is available: the player is at the peak and has not
	 * already released the System.
	 */
	public static boolean isFinaleAvailable(Entity entity) {
		return hasReachedPeak(entity) && !isReleased(entity);
	}

	/** Records the release. One way -- there is no path back through this class. */
	public static void release(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					if (capability.systemReleased)
						return;
					capability.systemReleased = true;
					capability.syncPlayerVariables(entity);
				});
	}

	/**
	 * Clears the release. Only progress resets use this; releasing is otherwise
	 * permanent, and a reset that left the flag set would strand the player with
	 * a level-1 character that can never earn anything again.
	 */
	public static void reinstate(Entity entity) {
		if (entity == null)
			return;
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> {
					if (!capability.systemReleased)
						return;
					capability.systemReleased = false;
					capability.syncPlayerVariables(entity);
				});
	}
}
