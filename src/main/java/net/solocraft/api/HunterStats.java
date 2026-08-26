package net.solocraft.api;

import net.minecraft.world.entity.Entity;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.TemporaryStatBonusManager;

/**
 * Reads a hunter's stats, for scaling an ability's output.
 *
 * <p>These are the *effective* values: base investment plus equipment, effects,
 * and temporary buffs. Reading the raw fields on the player attachment instead
 * would silently ignore every buff in the game, so an ability scaled that way
 * would quietly stop responding to the systems built to empower it.
 *
 * <p>Deliberately flat static methods over primitives. Anything fluent or
 * generic here could not be expressed as an MCreator procedure block.
 */
public final class HunterStats {
	private HunterStats() {
	}

	public static double strength(Entity entity) {
		return entity == null ? 0.0D : TemporaryStatBonusManager.effectiveStrength(entity);
	}

	public static double agility(Entity entity) {
		return entity == null ? 0.0D : TemporaryStatBonusManager.effectiveAgility(entity);
	}

	public static double perception(Entity entity) {
		return entity == null ? 0.0D : TemporaryStatBonusManager.effectivePerception(entity);
	}

	public static double vitality(Entity entity) {
		return entity == null ? 0.0D : TemporaryStatBonusManager.effectiveVitality(entity);
	}

	public static double intelligence(Entity entity) {
		return entity == null ? 0.0D : TemporaryStatBonusManager.effectiveIntelligence(entity);
	}

	/** The hunter's System level. */
	public static double level(Entity entity) {
		SololevelingModVariables.PlayerVariables variables = variablesOf(entity);
		return variables == null ? 0.0D : variables.Level;
	}

	private static SololevelingModVariables.PlayerVariables variablesOf(Entity entity) {
		if (entity == null)
			return null;
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(null);
	}
}
