package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.ShamanMagicEntity;
import net.solocraft.entity.GoblinMageEntity;
import net.solocraft.SololevelingMod;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class GoblinMageOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double delay = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 0.05) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				entity.getPersistentData().putDouble("AL", (entity.getPersistentData().getDouble("AL") + 1));
				if (entity.getPersistentData().getBoolean("CanShoot")) {
					entity.getPersistentData().putDouble("MF", (entity.getPersistentData().getDouble("MF") + 1));
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999, 90, false, false));
				} else {
					entity.getPersistentData().putDouble("MF", 0);
					if (entity instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
				}
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 1.2),
						((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
				Entity target = entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null;
				CombatRangeHelper.maintainRangedBand(entity, target, 4.5D, 14.0D, 0.95D);
				if (CombatRangeHelper.withinSurfaceRange(entity, target, 17.0D)
						&& entity instanceof Mob mob && target instanceof LivingEntity livingTarget
						&& mob.getSensing().hasLineOfSight(livingTarget)) {
					entity.getPersistentData().putBoolean("CanShoot", true);
				} else {
					entity.getPersistentData().putBoolean("CanShoot", false);
				}
			} else {
				entity.getPersistentData().putDouble("MF", 0);
			}
			if (entity.getPersistentData().getDouble("MF") == 5) {
				if (entity instanceof GoblinMageEntity) {
					((GoblinMageEntity) entity).setAnimation("attack");
				}
			}
			if (entity.getPersistentData().getDouble("MF") == 13) {
				for (int index0 = 0; index0 < 4; index0++) {
					delay = delay + 5;
					SololevelingMod.queueServerWork((int) delay, () -> {
						{
							Entity _shootFrom = entity;
							Level projectileLevel = _shootFrom.level();
							if (!projectileLevel.isClientSide()) {
								Projectile _entityToSpawn = new Object() {
									public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
										AbstractArrow entityToSpawn = new ShamanMagicEntity(SololevelingModEntities.SHAMAN_MAGIC.get(), level);
										entityToSpawn.setOwner(shooter);
										entityToSpawn.setBaseDamage(damage);
										entityToSpawn.setKnockback(knockback);
										entityToSpawn.setSilent(true);
										return entityToSpawn;
									}
								}.getArrow(projectileLevel, entity, 5, (int) 0.2);
								_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
								_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, (float) 0.25, 0);
								projectileLevel.addFreshEntity(_entityToSpawn);
							}
						}
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, (float) 0.5);
							} else {
								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")), SoundSource.NEUTRAL, 1, (float) 0.5, false);
							}
						}
					});
				}
			}
			if (entity.getPersistentData().getDouble("MF") == 60) {
				entity.getPersistentData().putDouble("MF", 0);
			}
		}
	}
}
