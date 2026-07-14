package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public final class VesselManaScaling {
	private static final double REFERENCE_STRENGTH = 50.0D;
	private static final double CURVE_POWER = 0.9D;
	private static final double MAX_COST_BONUS = 4.0D;

	private VesselManaScaling() {
	}

	/**
	 * Raises an offensive vessel ability's resource cost with Strength while
	 * capping the result at five times its base cost.
	 */
	public static int strengthScaledCost(Entity entity, int baseCost, double scalingWeight) {
		double strength = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> Math.max(0.0D, variables.Strength)).orElse(0.0D);
		double pressure = Math.pow(strength / REFERENCE_STRENGTH, CURVE_POWER);
		double bonus = Math.min(MAX_COST_BONUS, pressure * Math.max(0.0D, scalingWeight));
		double scaled = Math.ceil(Math.max(1, baseCost) * (1.0D + bonus));
		return (int) Math.min(Integer.MAX_VALUE, scaled);
	}
}
