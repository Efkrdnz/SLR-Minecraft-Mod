package net.solocraft.procedures;

import net.solocraft.entity.GoblinArcherEntity;
import net.solocraft.util.CombatRangeHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class GoblinArcherOnEntityTickUpdateProcedure {
	private static final int DRAW_TICKS = 12;
	private static final int RELEASE_TICKS = 22;
	private static final int ATTACK_CYCLE_TICKS = 80;

	public static void execute(Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 0) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				entity.getPersistentData().putDouble("AL", (entity.getPersistentData().getDouble("AL") + 1));
				if (entity.getPersistentData().getBoolean("CanShoot")) {
					entity.getPersistentData().putDouble("MF", (entity.getPersistentData().getDouble("MF") + 1));
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999, 90, false, false));
				} else {
					entity.getPersistentData().putDouble("MF", 0);
					if (entity instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
				}
				Entity target = entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null;
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(target.getX(),
						target.getY() + target.getBbHeight() * 0.55D, target.getZ()));
				CombatRangeHelper.maintainRangedBand(entity, target, 5.5D, 21.0D, 1.0D);
				if (CombatRangeHelper.withinSurfaceRange(entity, target, 24.0D)
						&& entity instanceof Mob mob && target instanceof LivingEntity livingTarget
						&& mob.getSensing().hasLineOfSight(livingTarget)) {
					entity.getPersistentData().putBoolean("CanShoot", true);
				} else {
					entity.getPersistentData().putBoolean("CanShoot", false);
				}
			} else {
				entity.getPersistentData().putDouble("MF", 0);
				entity.getPersistentData().putBoolean("CanShoot", false);
				if (entity instanceof LivingEntity _entity)
					_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			}
			if (entity.getPersistentData().getDouble("MF") == DRAW_TICKS) {
				if (entity instanceof GoblinArcherEntity) {
					((GoblinArcherEntity) entity).setAnimation("empty");
				}
				if (entity instanceof GoblinArcherEntity) {
					((GoblinArcherEntity) entity).setAnimation("shoot");
				}
			}
			if (entity.getPersistentData().getDouble("MF") == RELEASE_TICKS) {
				if (entity instanceof Mob mob)
					fireArrow(mob);
			}
			if (entity.getPersistentData().getDouble("MF") >= ATTACK_CYCLE_TICKS) {
				entity.getPersistentData().putDouble("MF", 0);
			}
		}
	}

	private static void fireArrow(Mob shooter) {
		if (!shooter.isAlive() || shooter.level().isClientSide())
			return;
		LivingEntity target = shooter.getTarget();
		if (target == null || !target.isAlive() || target.level() != shooter.level()
				|| !shooter.getSensing().hasLineOfSight(target)
				|| !CombatRangeHelper.withinSurfaceRange(shooter, target, 24.0D))
			return;

		Level projectileLevel = shooter.level();
		AbstractArrow arrow = new Arrow(EntityType.ARROW, projectileLevel);
		arrow.setOwner(shooter);
		arrow.setBaseDamage(2);
		net.solocraft.entity.LegacyProjectileCompat.setKnockback(arrow, 0);
		arrow.setCritArrow(true);
		arrow.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
		double dx = target.getX() - shooter.getX();
		double dz = target.getZ() - shooter.getZ();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		double dy = target.getY() + target.getBbHeight() * 0.5D - arrow.getY();
		arrow.shoot(dx, dy + horizontal * 0.12D, dz, 2.5F, 2.0F);
		projectileLevel.addFreshEntity(arrow);
	}
}
