package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.entity.ChaHaeInEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Comparator;

public class ChaHaeInOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double distance = 0;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			distance = Math.round(Math.pow(entity.getX() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX(), 2) + Math.pow(entity.getY() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY(), 2)
					+ Math.pow(entity.getZ() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ(), 2));
			if (entity instanceof Mob _mob && _mob.getTarget() != null) {
				LivingEntity target = _mob.getTarget();
				double deltaX = target.getX() - entity.getX();
				double deltaZ = target.getZ() - entity.getZ();
				float targetYaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX))) - 90.0F;
				entity.setYRot(targetYaw);
				entity.yRotO = targetYaw;
				if (entity instanceof LivingEntity _livingEntity) {
					_livingEntity.yBodyRot = targetYaw;
					_livingEntity.yHeadRot = targetYaw;
				}
			}
			if ((entity instanceof ChaHaeInEntity _datEntI ? _datEntI.getEntityData().get(ChaHaeInEntity.DATA_IA) : 0) < 120) {
				if (entity instanceof ChaHaeInEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChaHaeInEntity.DATA_IA, (int) ((entity instanceof ChaHaeInEntity _datEntI ? _datEntI.getEntityData().get(ChaHaeInEntity.DATA_IA) : 0) + 1));
				if ((entity instanceof ChaHaeInEntity _datEntI ? _datEntI.getEntityData().get(ChaHaeInEntity.DATA_IA) : 0) == 60) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.SWORD_DANCE.get(), 60, 1, false, false));
				}
			} else {
				if (entity instanceof ChaHaeInEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChaHaeInEntity.DATA_IA, 0);
			}
			if ((entity instanceof ChaHaeInEntity _datEntI ? _datEntI.getEntityData().get(ChaHaeInEntity.DATA_OverheadTimer) : 0) < 80) {
				if (entity instanceof ChaHaeInEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChaHaeInEntity.DATA_OverheadTimer, (int) ((entity instanceof ChaHaeInEntity _datEntI ? _datEntI.getEntityData().get(ChaHaeInEntity.DATA_OverheadTimer) : 0) + 1));
				if ((entity instanceof ChaHaeInEntity _datEntI ? _datEntI.getEntityData().get(ChaHaeInEntity.DATA_OverheadTimer) : 0) == 72) {
					if (entity instanceof ChaHaeInEntity) {
						((ChaHaeInEntity) entity).setAnimation("overhead");
					}
				}
			} else {
				if (distance < 3) {
					{
						final Vec3 _center = new Vec3(x, y, z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator)) {
								entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK)), 15);
							}
						}
					}
				}
				if (entity instanceof ChaHaeInEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChaHaeInEntity.DATA_OverheadTimer, 0);
			}
			if (distance > 9) {
				if (entity instanceof ChaHaeInEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChaHaeInEntity.DATA_DashTimer, (int) ((entity instanceof ChaHaeInEntity _datEntI ? _datEntI.getEntityData().get(ChaHaeInEntity.DATA_DashTimer) : 0) + 1));
				if ((entity instanceof ChaHaeInEntity _datEntI ? _datEntI.getEntityData().get(ChaHaeInEntity.DATA_DashTimer) : 0) >= 10) {
					if (entity instanceof ChaHaeInEntity _datEntSetI)
						_datEntSetI.getEntityData().set(ChaHaeInEntity.DATA_DashTimer, 0);
					entity.setDeltaMovement(new Vec3((((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX() - entity.getX()) * (5 / distance)),
							(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() - entity.getY() + 0.2) * (5 / distance)),
							(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ() - entity.getZ()) * (5 / distance))));
				} else {
					if (entity instanceof ChaHaeInEntity _datEntSetI)
						_datEntSetI.getEntityData().set(ChaHaeInEntity.DATA_DashTimer, 0);
				}
			} else {
				if (entity instanceof ChaHaeInEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChaHaeInEntity.DATA_DashTimer, 0);
			}
		}
	}
}
