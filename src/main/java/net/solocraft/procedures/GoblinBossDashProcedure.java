package net.solocraft.procedures;

import net.solocraft.entity.GoblinKingEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Comparator;

public class GoblinBossDashProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 1) {
			if (entity instanceof GoblinKingEntity) {
				((GoblinKingEntity) entity).setAnimation("precharge");
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 11) {
			if (entity instanceof GoblinKingEntity) {
				((GoblinKingEntity) entity).setAnimation("dash");
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 15) {
			entity.setDeltaMovement(new Vec3((4 * entity.getLookAngle().x), 0, (4 * entity.getLookAngle().z)));
			{
				final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator)) {
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 6);
					}
				}
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 17) {
			entity.setDeltaMovement(new Vec3((2.5 * entity.getLookAngle().x), 0, (2.5 * entity.getLookAngle().z)));
			{
				final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator)) {
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 6);
					}
				}
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 19) {
			entity.setDeltaMovement(new Vec3((1.2 * entity.getLookAngle().x), 0, (1.2 * entity.getLookAngle().z)));
			{
				final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator)) {
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 6);
					}
				}
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 21) {
			entity.setDeltaMovement(new Vec3((1.2 * entity.getLookAngle().x), 0, (1.2 * entity.getLookAngle().z)));
			{
				final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator)) {
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 6);
					}
				}
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 23) {
			entity.setDeltaMovement(new Vec3((1.2 * entity.getLookAngle().x), 0, (1.2 * entity.getLookAngle().z)));
			{
				final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator)) {
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 6);
					}
				}
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 25) {
			entity.setDeltaMovement(new Vec3((1.2 * entity.getLookAngle().x), 0, (1.2 * entity.getLookAngle().z)));
			{
				final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator)) {
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC)), 6);
					}
				}
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 26) {
			entity.setDeltaMovement(new Vec3(0, 0, 0));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, false));
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 70) {
			GoblinBossstatechangerProcedure.execute(entity);
		}
	}
}
