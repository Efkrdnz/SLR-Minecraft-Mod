package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;

public class PuricficationCastProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double num = 0;
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.END_ROD, x, y, z, 5, 3, 3, 3, 1);
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.HEALING_PARTICLE.get()), x, y, z, 15, 3, 3, 3, 1);
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(16 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
						.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
					num = num + 1;
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(SololevelingModMobEffects.BLEED.get());
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.WEAKNESS);
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.DARKNESS);
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.POISON);
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(SololevelingModMobEffects.PARALYZE.get());
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.WITHER);
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.HUNGER);
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.UNLUCK);
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.LEVITATION);
					if (entityiterator instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.BLINDNESS);
				}
			}
		}
		if (num > 0) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.beacon.deactivate")), SoundSource.NEUTRAL, 1, 2);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.beacon.deactivate")), SoundSource.NEUTRAL, 1, 2, false);
				}
			}
		}
	}
}
