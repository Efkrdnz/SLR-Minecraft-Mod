package net.solocraft.procedures;

import net.solocraft.entity.SkeletonSummonerEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class Melee1Procedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double distance = 0;
		double attack_duration = 0;
		Entity target = null;
		Vec3 direction = Vec3.ZERO;
		if (entity.isAlive()) {
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3, 100, false, false));
			if (entity instanceof SkeletonSummonerEntity _datEntSetI)
				_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_AttackDuration, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_AttackDuration) : 0) + 1));
			attack_duration = entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_AttackDuration) : 0;
			target = entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null;
			distance = Math.sqrt(Math.pow(entity.getX() - target.getX(), 2) + Math.pow(entity.getY() - target.getY(), 2) + Math.pow(entity.getZ() - target.getZ(), 2));
			if (attack_duration == 1) {
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((target.getX()), (target.getY()), (target.getZ())));
				if (entity instanceof SkeletonSummonerEntity) {
					((SkeletonSummonerEntity) entity).setAnimation("lantern_attack");
				}
				direction = (new Vec3((entity.getLookAngle().x), 0, (entity.getLookAngle().z))).normalize();
			}
			if (attack_duration == 14) {
				entity.setDeltaMovement(entity.getDeltaMovement().add(direction.scale(1)));
				if (distance <= 6) {
					target.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)),
							(float) (entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_DamageMelee) : 0));
				}
			}
			if (attack_duration >= 18) {
				if (entity instanceof SkeletonSummonerEntity _datEntSetS)
					_datEntSetS.getEntityData().set(SkeletonSummonerEntity.DATA_State, "TARGETING");
				if (entity instanceof SkeletonSummonerEntity _datEntSetI)
					_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_MeleeCooldown, 24);
			}
		}
	}
}
