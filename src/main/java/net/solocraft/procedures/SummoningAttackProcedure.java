package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.SkeletonSummonerEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class SummoningAttackProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double attack_duration = 0;
		double rx = 0;
		double ry = 0;
		double rz = 0;
		if (entity.isAlive()) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 100, false, false));
			if (entity instanceof SkeletonSummonerEntity _datEntSetI)
				_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_AttackDuration, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_AttackDuration) : 0) + 1));
			attack_duration = entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_AttackDuration) : 0;
			if (attack_duration == 1) {
				if (entity instanceof SkeletonSummonerEntity) {
					((SkeletonSummonerEntity) entity).setAnimation("lantern_summoning");
				}
			}
			if (attack_duration >= 26 && attack_duration <= 80) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, (entity.getX()), (entity.getY()), (entity.getZ()), 5, 0.5, 2, 0.5, 0.01);
				if (world.dayTime() % 20 == 0) {
					rx = entity.getX() + Mth.nextDouble(RandomSource.create(), -6, 6);
					ry = entity.getY();
					rz = entity.getZ() + Mth.nextDouble(RandomSource.create(), -6, 6);
					if (world instanceof ServerLevel _level) {
						Entity _entityToSpawn = SololevelingModEntities.MAGICAL_SKULL.get().create(_level);
						_entityToSpawn.moveTo(rx, (ry + 3), rz, world.getRandom().nextFloat() * 360.0F, 0.0F);
						if (_entityToSpawn instanceof Mob _mobToSpawn) {
							_mobToSpawn.finalizeSpawn(_level, _level.getCurrentDifficultyAt(_entityToSpawn.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
						}
						if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == (null))) {
							if ((_entityToSpawn) instanceof Mob _entity && (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) instanceof LivingEntity _ent)
								_entity.setTarget(_ent);
						}
						_level.addFreshEntity(_entityToSpawn);
					}
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, rx, ry, rz, 10, 0.3, 0.75, 0.3, 0.01);
				}
			}
			if (attack_duration >= 104) {
				if (entity instanceof SkeletonSummonerEntity _datEntSetS)
					_datEntSetS.getEntityData().set(SkeletonSummonerEntity.DATA_State, "TARGETING");
				if (entity instanceof SkeletonSummonerEntity _datEntSetI)
					_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_SummoningCooldown, 100);
			}
		}
	}
}
