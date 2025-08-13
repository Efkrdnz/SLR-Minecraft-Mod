package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.FxspikEntity;
import net.solocraft.entity.FxPuddleEntity;
import net.solocraft.entity.FangedKasakaEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;

public class FangedKasakaLongRangeProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		double particleNum = 0;
		double vX = 0;
		double vY = 0;
		double vZ = 0;
		double i = 0;
		double x_pos = 0;
		double z_pos = 0;
		double speed = 0;
		double arcAngle = 0;
		double radAngle = 0;
		double radYaw = 0;
		double radPitch = 0;
		double angle = 0;
		double y_pos = 0;
		double radius = 0;
		if (entity.getPersistentData().getDouble("IA") == 20) {
			entity.getPersistentData().putDouble("MODE", (Mth.nextInt(RandomSource.create(), 1, 2)));
		}
		if (entity.getPersistentData().getDouble("MODE") == 1) {
			if (entity.getPersistentData().getDouble("IA") == 21) {
				if (entity instanceof FangedKasakaEntity) {
					((FangedKasakaEntity) entity).setAnimation("tail_spike");
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 95) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FXSPIK.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 105) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FXSPIK.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 115) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FXSPIK.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 125) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FXSPIK.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 135) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FXSPIK.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 145) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FXSPIK.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 155) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FXSPIK.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 165) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FXSPIK.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 180) {
				entity.getPersistentData().putString("state", "");
				entity.getPersistentData().putDouble("IA", 0);
			}
		}
		if (entity.getPersistentData().getDouble("MODE") == 2) {
			if (entity.getPersistentData().getDouble("IA") == 21) {
				if (entity instanceof FangedKasakaEntity) {
					((FangedKasakaEntity) entity).setAnimation("breath_spit");
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 95) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.GLOW_SQUID_INK, (entity.getX()), (entity.getY() + 11), (entity.getZ()), 5, 0.5, 0.5, 0.5, 0);
			}
			if (entity.getPersistentData().getDouble("IA") == 130) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.GLOW_SQUID_INK, (entity.getX()), (entity.getY() + 11), (entity.getZ()), 5, 0.5, 0.5, 0.5, 0);
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FX_PUDDLE.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 150) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.GLOW_SQUID_INK, (entity.getX()), (entity.getY() + 11), (entity.getZ()), 5, 0.5, 0.5, 0.5, 0);
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FX_PUDDLE.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 170) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.GLOW_SQUID_INK, (entity.getX()), (entity.getY() + 9), (entity.getZ()), 5, 0.5, 0.5, 0.5, 0);
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FX_PUDDLE.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 190) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(96 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof FxspikEntity) && !(entityiterator instanceof ItemEntity) && !(entityiterator instanceof ItemFrame) && !(entityiterator instanceof ExperienceOrb)
								&& !(entityiterator instanceof FxPuddleEntity)) {
							if (world instanceof ServerLevel _level) {
								Entity entityToSpawn = SololevelingModEntities.FX_PUDDLE.get().spawn(_level, BlockPos.containing(entityiterator.getX(), entityiterator.getY(), entityiterator.getZ()), MobSpawnType.MOB_SUMMONED);
								if (entityToSpawn != null) {
									entityToSpawn.setDeltaMovement(0, 0, 0);
								}
							}
							if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 5, 99, false, false));
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IA") == 200) {
				entity.getPersistentData().putString("state", "");
				entity.getPersistentData().putDouble("IA", 0);
			}
		}
	}
}
