package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.entity.TuskShadowEntity;

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
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double hei = 0;
		if ((entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
			if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
				if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).isAlive())) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 30, 0.05, 0.05, 0.05, 1);
					if (!entity.level().isClientSide())
						entity.discard();
				}
			}
		}
		if (!(entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
			if (!entity.level().isClientSide())
				entity.discard();
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
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			entity.lookAt(EntityAnchorArgument.Anchor.EYES,
					new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getBbHeight()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
			if ((entity instanceof TuskShadowEntity _datEntI ? _datEntI.getEntityData().get(TuskShadowEntity.DATA_prot) : 0) == 0) {
				if (entity instanceof TuskShadowEntity _datEntSetI)
					_datEntSetI.getEntityData().set(TuskShadowEntity.DATA_prot, 200);
				if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
					if ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, false, true));
				}
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
						if (!(entityiterator == entity) && !((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)
								&& !(entityiterator == (entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null))
								&& !((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == (entityiterator instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null))) {
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
						if (!(entityiterator == entity) && !((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)
								&& !(entityiterator == (entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null))
								&& !((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == (entityiterator instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null))) {
							if (entityiterator.onGround()) {
								entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 20);
							}
						}
					}
				}
			}
		}
	}
}
