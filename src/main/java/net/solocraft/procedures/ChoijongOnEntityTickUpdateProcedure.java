package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.FlameVortexPEntity;
import net.solocraft.entity.ChoijongEntity;
import net.solocraft.util.CombatRangeHelper;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class ChoijongOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		double dmg_modifier = 0;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == (null))) {
			Entity target = entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null;
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()),
					target.getY() + target.getBbHeight() * 0.6D, target.getZ()));
			CombatRangeHelper.maintainRangedBand(entity, target, 7.0D, 19.0D, 1.12D);
			if (entity instanceof ChoijongEntity _datEntSetI)
				_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, (int) ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_IA) : 0) + 1));
			int attackTimer = entity instanceof ChoijongEntity choi
					? choi.getEntityData().get(ChoijongEntity.DATA_IA) : 0;
			if (attackTimer % 24 == 0 && entity instanceof ChoijongEntity choi
					&& target instanceof net.minecraft.world.entity.LivingEntity livingTarget
					&& CombatRangeHelper.withinSurfaceRange(entity, target, 24.0D)
					&& choi.getSensing().hasLineOfSight(livingTarget)) {
				choi.performRangedAttack(livingTarget, 1.0F);
			}
			if ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_IA) : 0) == 60) {
				rand = Mth.nextInt(RandomSource.create(), 1, 3);
				if (rand == 1) {
					entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 0.5),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
					HeavyFlameCastProcedure.execute(entity);
				} else if (rand == 2) {
					entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 0.5),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
					{
						Entity _shootFrom = entity;
						Level projectileLevel = _shootFrom.level();
						if (!projectileLevel.isClientSide()) {
							Projectile _entityToSpawn = new Object() {
								public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
									AbstractArrow entityToSpawn = new FlameVortexPEntity(SololevelingModEntities.FLAME_VORTEX_P.get(), level);
									entityToSpawn.setOwner(shooter);
									entityToSpawn.setBaseDamage(damage);
									entityToSpawn.setKnockback(knockback);
									entityToSpawn.setSilent(true);
									return entityToSpawn;
								}
							}.getArrow(projectileLevel, entity, 5, 0);
							_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
							_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, (float) 1.5, 0);
							projectileLevel.addFreshEntity(_entityToSpawn);
						}
					}
				} else if (rand == 3) {
					entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 0.5),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
					FireTornadoShootProcedure.execute(world, entity);
				}
			} else if ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_IA) : 0) > 80) {
				if (entity instanceof ChoijongEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, 0);
			}
		} else {
			if (entity instanceof ChoijongEntity _datEntSetI)
				_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, 0);
		}
	}
}
