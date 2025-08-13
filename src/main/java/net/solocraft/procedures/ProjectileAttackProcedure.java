package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.SkeletonSummonerEntity;
import net.solocraft.entity.NecroBlastEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class ProjectileAttackProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double delay = 0;
		double attack_duration = 0;
		Entity target = null;
		if (entity.isAlive()) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 100, false, false));
			if (entity instanceof SkeletonSummonerEntity _datEntSetI)
				_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_AttackDuration, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_AttackDuration) : 0) + 1));
			target = entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null;
			attack_duration = entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_AttackDuration) : 0;
			if (attack_duration == 1) {
				if (entity instanceof SkeletonSummonerEntity) {
					((SkeletonSummonerEntity) entity).setAnimation("projectile");
				}
				for (int index0 = 0; index0 < 6; index0++) {
					delay = delay + 1;
					SololevelingMod.queueServerWork((int) delay, () -> {
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, (entity.getX() + 2 * entity.getLookAngle().x), (entity.getY() + 1.6 + 2 * entity.getLookAngle().y), (entity.getZ() + 2 * entity.getLookAngle().z), 10, 1, 1, 1, (-0.5));
					});
				}
			}
			if (attack_duration == 22) {
				if (world instanceof ServerLevel projectileLevel) {
					Projectile _entityToSpawn = new Object() {
						public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
							AbstractArrow entityToSpawn = new NecroBlastEntity(SololevelingModEntities.NECRO_BLAST.get(), level);
							entityToSpawn.setOwner(shooter);
							entityToSpawn.setBaseDamage(damage);
							entityToSpawn.setKnockback(knockback);
							entityToSpawn.setSilent(true);
							return entityToSpawn;
						}
					}.getArrow(projectileLevel, entity, (float) (entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_DamageProjectile) : 0), 0);
					_entityToSpawn.setPos((entity.getX()), (entity.getY() + 3), (entity.getZ()));
					_entityToSpawn.shoot((target.getX() - entity.getX()), ((target.getY() + target.getBbHeight() / 2) - (entity.getY() + 3)), (target.getZ() - entity.getZ()), (float) 2.5, 0);
					projectileLevel.addFreshEntity(_entityToSpawn);
				}
			}
			if (attack_duration >= 34) {
				if (entity instanceof SkeletonSummonerEntity _datEntSetS)
					_datEntSetS.getEntityData().set(SkeletonSummonerEntity.DATA_State, "TARGETING");
				if (entity instanceof SkeletonSummonerEntity _datEntSetI)
					_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_ProjectileCooldown, 60);
			}
		}
	}
}
