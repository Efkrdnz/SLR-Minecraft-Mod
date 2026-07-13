package net.solocraft.procedures;

import net.solocraft.entity.ChaHaeInEntity;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.CombatRangeHelper;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class ChaHaeInOnEntityTickUpdateProcedure {
	private static final int SWORD_DANCE_CYCLE = 120;
	private static final int OVERHEAD_CYCLE = 80;
	private static final int DASH_CHARGE_TICKS = 12;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (!(entity instanceof ChaHaeInEntity cha))
			return;
		LivingEntity target = cha.getTarget();
		if (target == null || !target.isAlive())
			return;

		faceTarget(cha, target);
		double surfaceDistance = CombatRangeHelper.surfaceDistance(cha, target);
		tickSwordDance(cha);
		tickOverhead(world, cha, target, surfaceDistance);
		tickDash(cha, target, surfaceDistance);
	}

	private static void tickSwordDance(ChaHaeInEntity cha) {
		int timer = cha.getEntityData().get(ChaHaeInEntity.DATA_IA) + 1;
		if (timer == 60 && !cha.level().isClientSide()) {
			cha.addEffect(new MobEffectInstance(SololevelingModMobEffects.SWORD_DANCE.get(), 70, 2, false, false));
		}
		cha.getEntityData().set(ChaHaeInEntity.DATA_IA, timer >= SWORD_DANCE_CYCLE ? 0 : timer);
	}

	private static void tickOverhead(LevelAccessor world, ChaHaeInEntity cha,
			LivingEntity target, double surfaceDistance) {
		int timer = cha.getEntityData().get(ChaHaeInEntity.DATA_OverheadTimer) + 1;
		if (timer == 70)
			cha.setAnimation("overhead");
		if (timer >= OVERHEAD_CYCLE) {
			if (surfaceDistance <= 3.25D && !cha.level().isClientSide()) {
				target.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
						.getHolderOrThrow(DamageTypes.MOB_ATTACK), cha), 22.0F);
			}
			timer = 0;
		}
		cha.getEntityData().set(ChaHaeInEntity.DATA_OverheadTimer, timer);
	}

	private static void tickDash(ChaHaeInEntity cha, LivingEntity target, double surfaceDistance) {
		if (surfaceDistance <= 6.0D) {
			cha.getEntityData().set(ChaHaeInEntity.DATA_DashTimer, 0);
			return;
		}

		int timer = cha.getEntityData().get(ChaHaeInEntity.DATA_DashTimer) + 1;
		if (timer >= DASH_CHARGE_TICKS) {
			Vec3 direction = target.getBoundingBox().getCenter()
					.subtract(cha.getBoundingBox().getCenter());
			direction = new Vec3(direction.x, 0.0D, direction.z);
			if (direction.lengthSqr() > 1.0E-5D) {
				direction = direction.normalize();
				cha.setDeltaMovement(direction.x * 1.35D, 0.14D, direction.z * 1.35D);
				cha.hasImpulse = true;
			}
			timer = 0;
		}
		cha.getEntityData().set(ChaHaeInEntity.DATA_DashTimer, timer);
	}

	private static void faceTarget(Mob mob, LivingEntity target) {
		double deltaX = target.getX() - mob.getX();
		double deltaZ = target.getZ() - mob.getZ();
		float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
		mob.setYRot(yaw);
		mob.yRotO = yaw;
		mob.yBodyRot = yaw;
		mob.yHeadRot = yaw;
		mob.getLookControl().setLookAt(target, 35.0F, 35.0F);
	}
}
