package net.solocraft.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Bridges the generated 1.20 projectile constructors and explicit knockback
 * setting to the 1.21 {@link AbstractArrow} API.
 */
public abstract class LegacyAbstractArrow extends AbstractArrow {
	private int legacyKnockback;

	protected LegacyAbstractArrow(EntityType<? extends AbstractArrow> type, Level level) {
		super(type, level);
	}

	protected LegacyAbstractArrow(EntityType<? extends AbstractArrow> type, double x, double y, double z, Level level) {
		this(type, level);
		this.setPos(x, y, z);
	}

	protected LegacyAbstractArrow(EntityType<? extends AbstractArrow> type, LivingEntity owner, Level level) {
		this(type, owner.getX(), owner.getEyeY() - 0.1F, owner.getZ(), level);
		this.setOwner(owner);
	}

	public void setKnockback(int knockback) {
		this.legacyKnockback = Math.max(0, knockback);
	}

	@Override
	protected void doKnockback(LivingEntity target, DamageSource damageSource) {
		super.doKnockback(target, damageSource);
		if (this.legacyKnockback <= 0) {
			return;
		}

		double resistance = Math.max(0.0, 1.0 - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
		Vec3 impulse = this.getDeltaMovement()
				.multiply(1.0, 0.0, 1.0)
				.normalize()
				.scale(this.legacyKnockback * 0.6 * resistance);
		if (impulse.lengthSqr() > 0.0) {
			target.push(impulse.x, 0.1, impulse.z);
		}
	}
}
