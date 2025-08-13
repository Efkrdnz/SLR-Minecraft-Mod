package net.solocraft.procedures;

import net.solocraft.entity.AncientGolemEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;

public class AncientGolemOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity.isAlive()) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				if (!world.getEntitiesOfClass(Player.class, AABB.ofSize(new Vec3(x, y, z), 32, 32, 32), e -> true).isEmpty()) {
					if (entity instanceof AncientGolemEntity _datEntSetI)
						_datEntSetI.getEntityData().set(AncientGolemEntity.DATA_IA, (int) ((entity instanceof AncientGolemEntity _datEntI ? _datEntI.getEntityData().get(AncientGolemEntity.DATA_IA) : 0) + 1));
					if ((entity instanceof AncientGolemEntity _datEntI ? _datEntI.getEntityData().get(AncientGolemEntity.DATA_IA) : 0) == 100) {
						if (entity instanceof AncientGolemEntity) {
							((AncientGolemEntity) entity).setAnimation("slam");
						}
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 0, false, false));
					}
					if ((entity instanceof AncientGolemEntity _datEntI ? _datEntI.getEntityData().get(AncientGolemEntity.DATA_IA) : 0) == 115) {
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.HOSTILE, 3, 1);
							} else {
								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.HOSTILE, 3, 1, false);
							}
						}
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 32, 1, 1, 1, 1);
						{
							final Vec3 _center = new Vec3(x, y, z);
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(24 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
							for (Entity entityiterator : _entfound) {
								if (!(entityiterator instanceof LivingEntity _livEnt12 && _livEnt12.isBlocking())) {
									if (!(entityiterator == entity)) {
										entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK)), 7);
										entityiterator.setDeltaMovement(new Vec3(0, 1, 0));
										if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
											_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
									}
								}
							}
						}
					}
					if ((entity instanceof AncientGolemEntity _datEntI ? _datEntI.getEntityData().get(AncientGolemEntity.DATA_IA) : 0) == 122) {
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 128, 4, 1, 4, 1);
						{
							final Vec3 _center = new Vec3(x, y, z);
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(36 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
							for (Entity entityiterator : _entfound) {
								if (!(entityiterator instanceof LivingEntity _livEnt21 && _livEnt21.isBlocking())) {
									if (entityiterator.onGround()) {
										if (!(entityiterator == entity)) {
											entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK)), 7);
											entityiterator.setDeltaMovement(new Vec3(0, 1, 0));
											if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
												_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
										}
									}
								}
							}
						}
					}
					if ((entity instanceof AncientGolemEntity _datEntI ? _datEntI.getEntityData().get(AncientGolemEntity.DATA_IA) : 0) == 129) {
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 256, 8, 1, 8, 1);
						{
							final Vec3 _center = new Vec3(x, y, z);
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(48 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
							for (Entity entityiterator : _entfound) {
								if (!(entityiterator instanceof LivingEntity _livEnt31 && _livEnt31.isBlocking())) {
									if (entityiterator.onGround()) {
										if (!(entityiterator == entity)) {
											entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK)), 7);
											entityiterator.setDeltaMovement(new Vec3(0, 1, 0));
											if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
												_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
										}
									}
								}
							}
						}
					}
				} else {
					if (entity instanceof AncientGolemEntity _datEntSetI)
						_datEntSetI.getEntityData().set(AncientGolemEntity.DATA_IA, 0);
				}
			} else {
				if (entity instanceof AncientGolemEntity _datEntSetI)
					_datEntSetI.getEntityData().set(AncientGolemEntity.DATA_IA, 0);
			}
			if ((entity instanceof AncientGolemEntity _datEntI ? _datEntI.getEntityData().get(AncientGolemEntity.DATA_IA) : 0) >= 130) {
				if (entity instanceof AncientGolemEntity _datEntSetI)
					_datEntSetI.getEntityData().set(AncientGolemEntity.DATA_IA, 0);
			}
		}
	}
}
