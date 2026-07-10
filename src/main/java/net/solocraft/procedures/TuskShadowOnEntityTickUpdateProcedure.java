package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument;

import java.util.List;
import java.util.Comparator;

public class TuskShadowOnEntityTickUpdateProcedure {
	private static final String SOUL_FLAME_COOLDOWN = "sl_tusk_soul_flame_cd";
	private static final String CURSE_FIELD_COOLDOWN = "sl_tusk_curse_field_cd";
	private static final String MASTER_BUFF_COOLDOWN = "sl_tusk_master_buff_cd";

	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double hei = 0;
		if ((entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
			if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
				if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).isAlive())) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 30, 0.05, 0.05, 0.05, 1);
					if (!entity.level().isClientSide()) {
						ShadowMonarchManager.dropStoredShadowInventory(entity);
						entity.discard();
					}
				}
			}
		}
		if (!(entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
			if (!entity.level().isClientSide()) {
				ShadowMonarchManager.dropStoredShadowInventory(entity);
				entity.discard();
			}
		}
		hei = entity.getBbHeight();
		if (entity instanceof LivingEntity _livEnt15 && _livEnt15.hasEffect(SololevelingModMobEffects.DOMAIN_BOOST.get())) {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_PURPLE.get()), (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 2, (entity.getBbWidth() * 0.5), (hei / 2), (entity.getBbWidth() * 0.5), 0.05);
		} else {
			if (world instanceof ServerLevel _level)
				_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_BLUE.get()), (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 2, (entity.getBbWidth() * 0.5), (hei / 2), (entity.getBbWidth() * 0.5), 0.05);
		}
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 5, (entity.getBbWidth() * 0.75), (hei / 2), (entity.getBbWidth() * 0.75), 0.05);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.LARGE_SMOKE, (entity.getX()), (entity.getY() + hei / 2), (entity.getZ()), 2, (entity.getBbWidth() * 0.75), (hei / 2), (entity.getBbWidth() * 0.75), 0.05);
		LivingEntity owner = getOwner(entity);
		Entity target = entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null;
		tickCooldown(entity, SOUL_FLAME_COOLDOWN);
		tickCooldown(entity, CURSE_FIELD_COOLDOWN);
		tickCooldown(entity, MASTER_BUFF_COOLDOWN);
		tryMasterBuff(world, entity, owner);
		if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_prot) : 0) > 0) {
				if (entity instanceof TuskShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(TuskShadowEntity.DATA_prot, (int) ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_prot) : 0) - 1));
			}
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_fireball) : 0) > 0) {
				if (entity instanceof TuskShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(TuskShadowEntity.DATA_fireball, (int) ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_fireball) : 0) - 1));
			}
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_smash) : 0) > 0) {
				if (entity instanceof TuskShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(TuskShadowEntity.DATA_smash, (int) ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_smash) : 0) - 1));
			}
		}
		if (!(target == null)) {
			entity.lookAt(EntityAnchorArgument.Anchor.EYES,
					new Vec3((target.getX()), (target.getY() + target.getBbHeight()), (target.getZ())));
			if (target instanceof LivingEntity livingTarget)
				tryCombatSpells(world, entity, owner, livingTarget);
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_prot) : 0) == 0) {
				if (entity instanceof TuskShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(TuskShadowEntity.DATA_prot, 200);
				if (owner != null && !owner.level().isClientSide())
					owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 1, false, true));
			}
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_fireball) : 0) == 0) {
				if (entity instanceof TuskShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(TuskShadowEntity.DATA_fireball, 120);
			}
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_fireball) : 0) == 118) {
				if (entity instanceof TuskShadowEntity) {
					((TuskShadowEntity) entity).setAnimation("cast");
				}
			}
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_fireball) : 0) == 108) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.FIRE_PARTICLE_2.get()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), 25, 2, 2, 2, 0.1);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), 15, 2, 2, 2, 0.1);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.FLAME, ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), 25, 2, 2, 2, 0.1);
				{
					final Vec3 _center = new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()));
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(8 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (entityiterator instanceof LivingEntity living && !isFriendly(entity, owner, living)) {
							entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), entity), 8);
						}
					}
				}
			}
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_smash) : 0) == 0) {
				if (entity instanceof TuskShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(TuskShadowEntity.DATA_smash, 400);
			}
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_smash) : 0) == 398) {
				if (entity instanceof TuskShadowEntity) {
					((TuskShadowEntity) entity).setAnimation("groundsmash");
				}
			}
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_smash) : 0) == 333) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 25, 7, 1, 7, 0.1);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 25, 7, 1, 7, 0.1);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.LARGE_SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 25, 7, 1, 7, 0.1);
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 25, 7, 1, 7, 0.1);
				{
					final Vec3 _center = new Vec3((entity.getX()), (entity.getY()), (entity.getZ()));
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(30 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (entityiterator instanceof LivingEntity living && !isFriendly(entity, owner, living)) {
							if (entityiterator.onGround()) {
								entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 20);
							}
						}
					}
				}
			}
		}
	}

	private static LivingEntity getOwner(Entity entity) {
		if (entity instanceof TamableAnimal tame)
			return tame.getOwner();
		return null;
	}

	private static void tickCooldown(Entity entity, String key) {
		int value = entity.getPersistentData().getInt(key);
		if (value > 0)
			entity.getPersistentData().putInt(key, value - 1);
	}

	private static void setCooldown(Entity entity, String key, int ticks) {
		entity.getPersistentData().putInt(key, ticks);
	}

	private static boolean isFriendly(Entity caster, LivingEntity owner, Entity target) {
		if (target == null || target == caster || target == owner)
			return true;
		if (owner != null && target instanceof TamableAnimal tame && owner.getUUID().equals(tame.getOwnerUUID()))
			return true;
		return ShadowMonarchManager.haveSameShadowOwner(caster, target);
	}

	private static void tryMasterBuff(LevelAccessor world, Entity entity, LivingEntity owner) {
		if (owner == null || entity.getPersistentData().getInt(MASTER_BUFF_COOLDOWN) > 0 || entity.distanceToSqr(owner) > 48 * 48)
			return;
		setCooldown(entity, MASTER_BUFF_COOLDOWN, 220 + entity.level().random.nextInt(120));
		if (entity instanceof TuskShadowEntity tusk)
			tusk.setAnimation("cast");
		int roll = entity.level().random.nextInt(3);
		if (!owner.level().isClientSide()) {
			if (roll == 0) {
				owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 140, 0, false, true));
				owner.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 140, 0, false, true));
			} else if (roll == 1) {
				owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 0, false, true));
				owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 0, false, true));
			} else {
				owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, true));
				owner.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 160, 0, false, true));
			}
		}
		if (world instanceof ServerLevel level) {
			level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.MANA_PURPLE.get(), owner.getX(), owner.getY() + owner.getBbHeight() * 0.55, owner.getZ(), 28, owner.getBbWidth() * 0.7, owner.getBbHeight() * 0.35, owner.getBbWidth() * 0.7, 0.05);
			level.sendParticles(ParticleTypes.ENCHANT, owner.getX(), owner.getY() + 1.0D, owner.getZ(), 18, 0.6D, 0.8D, 0.6D, 0.08D);
		}
	}

	private static void tryCombatSpells(LevelAccessor world, Entity entity, LivingEntity owner, LivingEntity target) {
		if (!target.isAlive() || isFriendly(entity, owner, target))
			return;
		if (entity.getPersistentData().getInt(SOUL_FLAME_COOLDOWN) <= 0 && entity.distanceToSqr(target) <= 36 * 36) {
			castSoulFlame(world, entity, owner, target);
			setCooldown(entity, SOUL_FLAME_COOLDOWN, 90 + entity.level().random.nextInt(60));
			return;
		}
		if (entity.getPersistentData().getInt(CURSE_FIELD_COOLDOWN) <= 0 && entity.distanceToSqr(target) <= 28 * 28) {
			castCurseField(world, entity, owner, target);
			setCooldown(entity, CURSE_FIELD_COOLDOWN, 170 + entity.level().random.nextInt(90));
		}
	}

	private static void castSoulFlame(LevelAccessor world, Entity entity, LivingEntity owner, LivingEntity target) {
		if (entity instanceof TuskShadowEntity tusk)
			tusk.setAnimation("cast");
		Vec3 start = entity.position().add(0.0D, entity.getBbHeight() * 0.75D, 0.0D);
		Vec3 end = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
		Vec3 delta = end.subtract(start);
		if (world instanceof ServerLevel level) {
			for (int i = 0; i <= 16; i++) {
				Vec3 point = start.add(delta.scale(i / 16.0D));
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, point.x, point.y, point.z, 2, 0.06D, 0.06D, 0.06D, 0.01D);
				level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.MANA_PURPLE.get(), point.x, point.y, point.z, 1, 0.04D, 0.04D, 0.04D, 0.01D);
			}
			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + target.getBbHeight() * 0.45D, target.getZ(), 18, 0.4D, 0.6D, 0.4D, 0.08D);
		}
		if (!target.level().isClientSide()) {
			target.setSecondsOnFire(4);
			target.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), entity), 7);
		}
	}

	private static void castCurseField(LevelAccessor world, Entity entity, LivingEntity owner, LivingEntity target) {
		if (entity instanceof TuskShadowEntity tusk)
			tusk.setAnimation("cast");
		Vec3 center = target.position();
		if (world instanceof ServerLevel level) {
			level.sendParticles((SimpleParticleType) SololevelingModParticleTypes.MANA_PURPLE.get(), center.x, center.y + 0.5D, center.z, 45, 2.8D, 0.6D, 2.8D, 0.08D);
			level.sendParticles(ParticleTypes.WITCH, center.x, center.y + 0.6D, center.z, 24, 2.2D, 0.4D, 2.2D, 0.04D);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.2D, center.z, 30, 2.6D, 0.25D, 2.6D, 0.05D);
		}
		List<Entity> found = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(7 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center))).toList();
		for (Entity foundEntity : found) {
			if (foundEntity instanceof LivingEntity living && !isFriendly(entity, owner, living)) {
				if (!living.level().isClientSide()) {
					living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1, false, true));
					living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true));
					living.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), entity), 5);
				}
			}
		}
	}
}
