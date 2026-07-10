package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;

public class ConsecutiveSlashesOnEffectActiveTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
			_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 5, false, false));
		CooldownManager.set(entity, "mana_refresh", 40);
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP > 100) {
			{
				double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 5;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.MP = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		} else {
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(SololevelingModMobEffects.CONSECUTIVE_SLASHES.get());
			CooldownManager.set(entity, "mana_refresh", 60);
		}
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GOODSLASH_1.get()), x, y, z, 4, 5, 5, 5, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GOOD_SLASH_2.get()), x, y, z, 12, 5, 5, 5, 0);
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.SWEEP_ATTACK, x, y, z, 4, 5, 5, 5, 0);
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (!(entity == entityiterator)) {
					{
						DamageSource _damageSource = new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:assassin"))), entity);
						if (_damageSource != null) {
							entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:assassin"))), entity),
									(float) ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength / 50 + 2));
						}
					}
				}
			}
		}
	}
}
