package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.FlameVortexPEntity;
import net.solocraft.entity.ChoijongEntity;

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
		double distance = 0;
		double dmg_modifier = 0;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == (null))) {
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()),
					((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + entity.getBbHeight() / 2), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
			if (entity instanceof ChoijongEntity _datEntSetI)
				_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, (int) ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_IA) : 0) + 1));
			distance = Math.sqrt(Math.pow(entity.getX() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX(), 2) + Math.pow(entity.getY() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY(), 2)
					+ Math.pow(entity.getZ() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ(), 2));
			if ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_backoff) : 0) > 0) {
				if (entity instanceof ChoijongEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChoijongEntity.DATA_backoff, (int) ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_backoff) : 0) - 1));
			}
			if (distance <= 5) {
				if ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_backoff) : 0) == 0) {
					entity.setDeltaMovement(new Vec3((entity.getLookAngle().x * (-2)), 0.3, (entity.getLookAngle().z * (-2))));
					if (entity instanceof ChoijongEntity _datEntSetI)
						_datEntSetI.getEntityData().set(ChoijongEntity.DATA_backoff, 50);
				}
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
			} else if ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_IA) : 0) > 60) {
				if (entity instanceof ChoijongEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, 0);
			}
		} else {
			if (entity instanceof ChoijongEntity _datEntSetI)
				_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, 0);
		}
	}
}
