package net.solocraft.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;

/** Compatibility helpers for projectile knobs removed from AbstractArrow. */
public final class LegacyProjectileCompat {
	private static final String KNOCKBACK_TAG = "sololeveling_legacy_knockback";

	private LegacyProjectileCompat() {
	}

	public static void setKnockback(AbstractArrow arrow, int knockback) {
		int clamped = Math.max(0, knockback);
		if (arrow instanceof LegacyAbstractArrow legacyArrow) {
			legacyArrow.setKnockback(clamped);
		} else {
			arrow.getPersistentData().putInt(KNOCKBACK_TAG, clamped);
		}
	}

	public static void applyKnockback(AbstractArrow arrow, LivingEntity target) {
		int knockback = arrow.getPersistentData().getInt(KNOCKBACK_TAG);
		if (knockback <= 0) {
			return;
		}

		double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		Vec3 impulse = arrow.getDeltaMovement()
				.multiply(1.0, 0.0, 1.0)
				.normalize()
				.scale(knockback * 0.6 * resistance);
		if (impulse.lengthSqr() > 0.0) {
			target.push(impulse.x, 0.1, impulse.z);
		}
	}
}
