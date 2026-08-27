package net.solocraft.util;

/**
 * Central raw-damage tuning for the Demon Castle's named bosses.
 *
 * <p>These values are applied before armor and class mitigation. Keeping the
 * phase variants here makes it possible to balance a complete attack sequence
 * instead of allowing individually reasonable hits to combine into a one-shot.</p>
 */
public final class DemonCastleBossDamageRules {
	public static final double VULCAN_MELEE = 32.0D;
	public static final float VULCAN_HAMMER_SLAM = 34.0F;
	public static final float VULCAN_CHARGE = 22.0F;
	public static final float VULCAN_MOLTEN_BURST = 25.0F;

	public static final double BARAN_MELEE = 16.0D;
	public static final float BARAN_GROUND_THIRD_WAVE = 6.0F;
	public static final float BARAN_MAGIC_PRE_SHOT = 8.0F;
	public static final float BARAN_MAGIC_SHOCKWAVE = 8.0F;
	public static final float BARAN_LIGHTNING_DIRECT = 5.0F;

	private DemonCastleBossDamageRules() {
	}

	public static float baranGroundSlam(boolean phaseTwo) {
		return phaseTwo ? 22.0F : 17.0F;
	}

	public static float baranGroundRipple(boolean phaseTwo) {
		return phaseTwo ? 10.0F : 8.0F;
	}

	public static float baranChargeCollision(boolean phaseTwo) {
		return phaseTwo ? 16.0F : 12.0F;
	}

	public static float baranChargeImpact(boolean phaseTwo) {
		return phaseTwo ? 21.0F : 15.0F;
	}

	public static float baranMagicBlast(boolean phaseTwo) {
		return phaseTwo ? 24.0F : 18.0F;
	}
}
