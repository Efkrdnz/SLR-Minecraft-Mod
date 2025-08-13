package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.entity.BeruBossEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

public class BeruBossOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		Entity ent = null;
		double distance = 0;
		double rand = 0;
		boolean cantp = false;
		boolean canupslam = false;
		boolean cangroundslam = false;
		boolean recovery = false;
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_PURPLE.get()), (entity.getX()), (entity.getY() + entity.getBbHeight() / 3), (entity.getZ()), 5, (entity.getBbWidth() / 4), (entity.getBbHeight() / 4),
					(entity.getBbWidth() / 4), 1);
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_RED.get()), (entity.getX()), (entity.getY() + entity.getBbHeight() / 3), (entity.getZ()), 5, (entity.getBbWidth() / 4), (entity.getBbHeight() / 4),
					(entity.getBbWidth() / 4), 1);
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			ent = entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null;
			distance = Math.sqrt(Math.pow(entity.getX() - ent.getX(), 2) + Math.pow(entity.getY() - ent.getY(), 2) + Math.pow(entity.getZ() - ent.getZ(), 2));
			if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("idle")) {
				if (entity instanceof BeruBossEntity _datEntSetS)
					_datEntSetS.getEntityData().set(BeruBossEntity.DATA_state, "attacking");
			}
		}
		if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_recovery) : 0) > 0) {
			recovery = true;
		} else {
			recovery = false;
		}
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) / (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) <= 0.33) {
			if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_phase) : 0) == 2) {
				if (entity instanceof BeruBossEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BeruBossEntity.DATA_phase, 3);
			}
		} else if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) / (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) <= 0.67) {
			if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_phase) : 0) == 1) {
				if (entity instanceof BeruBossEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BeruBossEntity.DATA_phase, 2);
				if (entity instanceof BeruBossEntity _datEntSetS)
					_datEntSetS.getEntityData().set(BeruBossEntity.DATA_state, "ready");
			}
		}
		if (!recovery) {
			if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("attacking")) {
				if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
					if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_phase) : 0) > 1) {
						entity.setNoGravity(true);
						if (distance >= 5) {
							entity.setDeltaMovement(new Vec3(((ent.getX() - entity.getX()) * (0.4 / distance)), ((ent.getY() - entity.getY()) * (0.4 / distance)), ((ent.getZ() - entity.getZ()) * (0.4 / distance))));
						} else {
							if (world.dayTime() % 10 == 0) {
								entity.setDeltaMovement(new Vec3(((ent.getX() - entity.getX()) * (1 / distance)), ((ent.getY() - entity.getY()) * (1 / distance)), ((ent.getZ() - entity.getZ()) * (1 / distance))));
								if (entity instanceof BeruBossEntity _datEntSetI)
									_datEntSetI.getEntityData().set(BeruBossEntity.DATA_recovery, 5);
							}
						}
					}
				}
			}
			if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("groundslam")) {
				if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
					if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_phase) : 0) > 1) {
						entity.setNoGravity(true);
						entity.setDeltaMovement(new Vec3(((ent.getX() - entity.getX()) * (1 / distance)), 0, ((ent.getZ() - entity.getZ()) * (1 / distance))));
					}
				}
			}
		}
		if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("ready")) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
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
			}
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 4, 5, false, false));
			if (entity instanceof BeruBossEntity _datEntSetI)
				_datEntSetI.getEntityData().set(BeruBossEntity.DATA_IAI, (int) ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_IAI) : 0) + 1));
			if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_IAI) : 0) == 1) {
				if (entity instanceof BeruBossEntity) {
					((BeruBossEntity) entity).setAnimation("start_flying");
				}
			}
			if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_IAI) : 0) == 36) {
				entity.setDeltaMovement(new Vec3(0, 1, 0));
			}
			if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_IAI) : 0) == 39) {
				entity.setDeltaMovement(new Vec3(0, 0, 0));
			}
			if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_IAI) : 0) >= 45) {
				if (entity instanceof BeruBossEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BeruBossEntity.DATA_IAI, 0);
				if (entity instanceof BeruBossEntity _datEntSetS)
					_datEntSetS.getEntityData().set(BeruBossEntity.DATA_state, "attacking");
			}
		}
		if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("attacking")) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				if (entity instanceof BeruBossEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BeruBossEntity.DATA_IA, (int) ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_IA) : 0) + 1));
			}
		}
		if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_recovery) : 0) > 0) {
			if (entity instanceof BeruBossEntity _datEntSetI)
				_datEntSetI.getEntityData().set(BeruBossEntity.DATA_recovery, (int) ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_recovery) : 0) - 1));
		}
		if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownUpslam) : 0) > 0) {
			if (entity instanceof BeruBossEntity _datEntSetI)
				_datEntSetI.getEntityData().set(BeruBossEntity.DATA_CooldownUpslam, (int) ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownUpslam) : 0) - 1));
		}
		if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownGroundslam) : 0) > 0) {
			if (entity instanceof BeruBossEntity _datEntSetI)
				_datEntSetI.getEntityData().set(BeruBossEntity.DATA_CooldownGroundslam, (int) ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownGroundslam) : 0) - 1));
		}
		if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownTeleport) : 0) > 0) {
			if (entity instanceof BeruBossEntity _datEntSetI)
				_datEntSetI.getEntityData().set(BeruBossEntity.DATA_CooldownTeleport, (int) ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownTeleport) : 0) - 1));
		}
		if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownUpslam) : 0) <= 0) {
			if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_phase) : 0) >= 2) {
				if (!((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").contains("upslam"))) {
					if (entity instanceof BeruBossEntity _datEntSetS)
						_datEntSetS.getEntityData().set(BeruBossEntity.DATA_available_attacks, ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "") + "upslam,"));
				}
			}
		}
		if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownGroundslam) : 0) <= 0) {
			if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_phase) : 0) >= 3) {
				if (!((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").contains("groundslam"))) {
					if (entity instanceof BeruBossEntity _datEntSetS)
						_datEntSetS.getEntityData().set(BeruBossEntity.DATA_available_attacks, ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "") + "groundslam,"));
				}
			}
		}
		if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_CooldownTeleport) : 0) <= 0) {
			if (!((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").contains("tp"))) {
				if (entity instanceof BeruBossEntity _datEntSetS)
					_datEntSetS.getEntityData().set(BeruBossEntity.DATA_available_attacks, ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "") + "tp,"));
			}
		}
		if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("attacking")) {
			if (!recovery) {
				if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").contains("tp") && distance >= 15) {
					cantp = true;
				}
				if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").contains("groundslam")
						&& (entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_phase) : 0) == 3) {
					cangroundslam = true;
				}
				if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").contains("upslam")
						&& (entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_phase) : 0) >= 2 && distance < 3) {
					canupslam = true;
				}
			}
		}
		if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("attacking")) {
			if (!(ent == null)) {
				if (cantp) {
					{
						Entity _ent = entity;
						_ent.teleportTo((ent.getX() + (-1.5) * entity.getLookAngle().x), (ent.getY() + 0.5), (ent.getZ() + (-1.5) * entity.getLookAngle().z));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport((ent.getX() + (-1.5) * entity.getLookAngle().x), (ent.getY() + 0.5), (ent.getZ() + (-1.5) * entity.getLookAngle().z), _ent.getYRot(), _ent.getXRot());
					}
					if (entity instanceof BeruBossEntity _datEntSetI)
						_datEntSetI.getEntityData().set(BeruBossEntity.DATA_CooldownTeleport, Mth.nextInt(RandomSource.create(), 40, 80));
					if (entity instanceof BeruBossEntity _datEntSetS)
						_datEntSetS.getEntityData().set(BeruBossEntity.DATA_available_attacks, ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").replace("tp", "")));
				}
				if (canupslam) {
					BeruUpslamProcedure.execute(world, x, y, z, entity);
					if (entity instanceof BeruBossEntity _datEntSetS)
						_datEntSetS.getEntityData().set(BeruBossEntity.DATA_available_attacks, ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").replace("upslam", "")));
					if (entity instanceof BeruBossEntity _datEntSetI)
						_datEntSetI.getEntityData().set(BeruBossEntity.DATA_CooldownUpslam, Mth.nextInt(RandomSource.create(), 100, 140));
				}
				if (cangroundslam) {
					if (entity instanceof BeruBossEntity _datEntSetI)
						_datEntSetI.getEntityData().set(BeruBossEntity.DATA_CooldownGroundslam, Mth.nextInt(RandomSource.create(), 300, 400));
					if (entity instanceof BeruBossEntity _datEntSetS)
						_datEntSetS.getEntityData().set(BeruBossEntity.DATA_available_attacks, ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_available_attacks) : "").replace("groundslam", "")));
					if (entity instanceof BeruBossEntity _datEntSetS)
						_datEntSetS.getEntityData().set(BeruBossEntity.DATA_state, "groundslam");
				}
			}
		}
		if (!(ent == null)) {
			if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("groundslam")) {
				if (entity instanceof BeruBossEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BeruBossEntity.DATA_SlamTimer, (int) ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_SlamTimer) : 0) + 1));
				if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_SlamTimer) : 0) == 5) {
					{
						Entity _ent = entity;
						_ent.teleportTo((ent.getX()), (ent.getY() + 6), (ent.getZ()));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport((ent.getX()), (ent.getY() + 6), (ent.getZ()), _ent.getYRot(), _ent.getXRot());
					}
				}
				if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_SlamTimer) : 0) == 20) {
					entity.setNoGravity(false);
					entity.setDeltaMovement(new Vec3(0, (-20), 0));
				}
				if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_SlamTimer) : 0) >= 15) {
					if (entity.onGround()) {
						BeruDownslamProcedure.execute(world, x, y, z, entity);
						if (entity instanceof BeruBossEntity _datEntSetS)
							_datEntSetS.getEntityData().set(BeruBossEntity.DATA_state, "attacking");
						if (entity instanceof BeruBossEntity _datEntSetI)
							_datEntSetI.getEntityData().set(BeruBossEntity.DATA_SlamTimer, 0);
					}
				}
				if ((entity instanceof BeruBossEntity _datEntI ? _datEntI.getEntityData().get(BeruBossEntity.DATA_SlamTimer) : 0) >= 40) {
					if (entity instanceof BeruBossEntity _datEntSetS)
						_datEntSetS.getEntityData().set(BeruBossEntity.DATA_state, "attacking");
					if (entity instanceof BeruBossEntity _datEntSetI)
						_datEntSetI.getEntityData().set(BeruBossEntity.DATA_SlamTimer, 0);
				}
			}
		}
		if ((entity instanceof BeruBossEntity _datEntS ? _datEntS.getEntityData().get(BeruBossEntity.DATA_state) : "").equals("attacking")) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				if (entity instanceof BeruBossEntity) {
					((BeruBossEntity) entity).setAnimation("flying");
				}
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
			} else {
				if (entity instanceof BeruBossEntity _datEntSetS)
					_datEntSetS.getEntityData().set(BeruBossEntity.DATA_state, "idle");
				if (entity instanceof BeruBossEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BeruBossEntity.DATA_IA, 0);
			}
		} else {
			entity.setNoGravity(false);
		}
	}
}
