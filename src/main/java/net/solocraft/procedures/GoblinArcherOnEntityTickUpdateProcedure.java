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
			}
			if (entity.getPersistentData().getDouble("MF") == 10) {
				if (entity instanceof GoblinArcherEntity) {
					((GoblinArcherEntity) entity).setAnimation("empty");
				}
				if (entity instanceof GoblinArcherEntity) {
					((GoblinArcherEntity) entity).setAnimation("shoot");
				}
			}
			if (entity.getPersistentData().getDouble("MF") == 18) {
				{
					Entity _shootFrom = entity;
					Level projectileLevel = _shootFrom.level();
					if (!projectileLevel.isClientSide()) {
						Projectile _entityToSpawn = new Object() {
							public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
								AbstractArrow entityToSpawn = new Arrow(EntityType.ARROW, level);
								entityToSpawn.setOwner(shooter);
								entityToSpawn.setBaseDamage(damage);
								entityToSpawn.setKnockback(knockback);
								entityToSpawn.setCritArrow(true);
								return entityToSpawn;
							}
						}.getArrow(projectileLevel, entity, 2, (int) 0.1);
						_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
						Entity target = entity instanceof Mob mob ? mob.getTarget() : null;
						if (target != null) {
							double dx = target.getX() - _shootFrom.getX();
							double dz = target.getZ() - _shootFrom.getZ();
							double horizontal = Math.sqrt(dx * dx + dz * dz);
							double dy = target.getY() + target.getBbHeight() * 0.5D - _entityToSpawn.getY();
							_entityToSpawn.shoot(dx, dy + horizontal * 0.12D, dz, 2.5F, 2.0F);
							projectileLevel.addFreshEntity(_entityToSpawn);
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("MF") == 60) {
				entity.getPersistentData().putDouble("MF", 0);
			}
		}
	}
}
