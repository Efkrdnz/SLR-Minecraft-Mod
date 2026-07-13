package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.ManaArrowEntity;
import net.solocraft.entity.IceElfEntity;
import net.solocraft.util.CombatRangeHelper;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class IceElfOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (!world.isClientSide()) {
			if (world.getLevelData().getGameTime() % 5 == 0) {
				if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
					if (entity instanceof IceElfEntity _datEntSetI)
						_datEntSetI.getEntityData().set(IceElfEntity.DATA_AI, (int) ((entity instanceof IceElfEntity _datEntI ? _datEntI.getEntityData().get(IceElfEntity.DATA_AI) : 0) + 1));
					if (entity instanceof IceElfEntity _datEntL6 && _datEntL6.getEntityData().get(IceElfEntity.DATA_canshoot)) {
						if (entity instanceof IceElfEntity _datEntSetI)
							_datEntSetI.getEntityData().set(IceElfEntity.DATA_MF, (int) ((entity instanceof IceElfEntity _datEntI ? _datEntI.getEntityData().get(IceElfEntity.DATA_MF) : 0) + 1));
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999, 90, false, false));
					} else {
						if (entity instanceof IceElfEntity _datEntSetI)
							_datEntSetI.getEntityData().set(IceElfEntity.DATA_MF, 0);
						if (entity instanceof LivingEntity _entity)
							_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
					}
					entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 1.2),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
					Entity target = entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null;
					CombatRangeHelper.maintainRangedBand(entity, target, 6.0D, 16.0D, 1.0D);
					if (CombatRangeHelper.withinSurfaceRange(entity, target, 20.0D)
							&& entity instanceof Mob mob && target instanceof LivingEntity livingTarget
							&& mob.getSensing().hasLineOfSight(livingTarget)) {
						if (entity instanceof IceElfEntity _datEntSetL)
							_datEntSetL.getEntityData().set(IceElfEntity.DATA_canshoot, true);
					} else {
						if (entity instanceof IceElfEntity _datEntSetL)
							_datEntSetL.getEntityData().set(IceElfEntity.DATA_canshoot, false);
					}
				} else {
					if (entity instanceof IceElfEntity _datEntSetI)
						_datEntSetI.getEntityData().set(IceElfEntity.DATA_MF, 0);
					if (entity instanceof IceElfEntity _datEntSetI)
						_datEntSetI.getEntityData().set(IceElfEntity.DATA_AI, 0);
				}
				if ((entity instanceof IceElfEntity _datEntI ? _datEntI.getEntityData().get(IceElfEntity.DATA_MF) : 0) == 2) {
					if (entity instanceof IceElfEntity) {
						((IceElfEntity) entity).setAnimation("empty");
					}
					if (entity instanceof IceElfEntity) {
						((IceElfEntity) entity).setAnimation("attack.bow");
					}
				}
				if ((entity instanceof IceElfEntity _datEntI ? _datEntI.getEntityData().get(IceElfEntity.DATA_MF) : 0) == 5) {
					{
						Entity _shootFrom = entity;
						Level projectileLevel = _shootFrom.level();
						if (!projectileLevel.isClientSide()) {
							Projectile _entityToSpawn = new Object() {
								public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
									AbstractArrow entityToSpawn = new ManaArrowEntity(SololevelingModEntities.MANA_ARROW.get(), level);
									entityToSpawn.setOwner(shooter);
									entityToSpawn.setBaseDamage(damage);
									entityToSpawn.setKnockback(knockback);
									entityToSpawn.setSilent(true);
									entityToSpawn.setCritArrow(true);
									return entityToSpawn;
								}
							}.getArrow(projectileLevel, entity, 5, (int) 0.1);
							_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
							_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 3, 5);
							projectileLevel.addFreshEntity(_entityToSpawn);
						}
					}
				}
				if ((entity instanceof IceElfEntity _datEntI ? _datEntI.getEntityData().get(IceElfEntity.DATA_MF) : 0) == 11) {
					if (entity instanceof IceElfEntity _datEntSetI)
						_datEntSetI.getEntityData().set(IceElfEntity.DATA_MF, 0);
				}
				if ((entity instanceof IceElfEntity _datEntI ? _datEntI.getEntityData().get(IceElfEntity.DATA_AI) : 0) == 12) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 120, 3, 1.8, 60, 0);
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.snow.step")), SoundSource.NEUTRAL, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.snow.step")), SoundSource.NEUTRAL, 1, 1, false);
						}
					}
					IceElfTeleportProcedure.execute(world, entity);
				}
				if ((entity instanceof IceElfEntity _datEntI ? _datEntI.getEntityData().get(IceElfEntity.DATA_AI) : 0) == 24) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 120, 3, 1.8, 60, 0);
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.snow.step")), SoundSource.NEUTRAL, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.snow.step")), SoundSource.NEUTRAL, 1, 1, false);
						}
					}
					IceElfTeleportProcedure.execute(world, entity);
				}
				if ((entity instanceof IceElfEntity _datEntI ? _datEntI.getEntityData().get(IceElfEntity.DATA_AI) : 0) == 64) {
					if (entity instanceof IceElfEntity _datEntSetI)
						_datEntSetI.getEntityData().set(IceElfEntity.DATA_AI, 0);
				}
			}
		}
	}
}
