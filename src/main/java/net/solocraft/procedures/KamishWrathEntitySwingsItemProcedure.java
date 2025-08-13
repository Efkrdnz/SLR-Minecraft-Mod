package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;

public class KamishWrathEntitySwingsItemProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
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
		double xpos2 = 0;
		double xpos3 = 0;
		double zpos2 = 0;
		double zpos3 = 0;
		double ypos2 = 0;
		double ypos3 = 0;
		double xpos4 = 0;
		double ypos4 = 0;
		double zpos4 = 0;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).kamishcharge == 60 && entity instanceof LivingEntity _livEnt0
				&& _livEnt0.hasEffect(SololevelingModMobEffects.SWORD_ENHANCE.get())) {
			if (entity instanceof LivingEntity _entity)
				_entity.swing(InteractionHand.MAIN_HAND, true);
			radius = 4;
			speed = 15;
			particleNum = 120;
			arcAngle = 180;
			radYaw = Math.toRadians(entity.getYRot() + 90);
			radPitch = Math.toRadians((entity.getXRot() + 90) * (-1));
			{
				double _setval = 0;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.kamishcharge = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:seismicslash")), SoundSource.NEUTRAL, 2, 1);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:seismicslash")), SoundSource.NEUTRAL, 2, 1, false);
				}
			}
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.KAMISHCOOL.get(), 200, 1));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20, 3));
			if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
				_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3));
			if (entity instanceof LivingEntity _entity)
				_entity.removeEffect(SololevelingModMobEffects.SWORD_ENHANCE.get());
			for (int index0 = 0; index0 < (int) particleNum; index0++) {
				angle = i * (arcAngle / particleNum);
				radAngle = Math.toRadians(angle);
				vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
				vY = Math.sin(radAngle) * Math.cos(radPitch);
				vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
				x_pos = x + radius * vX;
				xpos2 = x + 1 * vX;
				xpos3 = x + 2 * vX;
				xpos4 = x + 3 * vX;
				y_pos = y + radius * vY;
				ypos2 = y + 1 * vY;
				ypos3 = y + 2 * vY;
				ypos4 = y + 3 * vY;
				z_pos = z + radius * vZ;
				zpos2 = z + 1 * vZ;
				zpos3 = z + 2 * vZ;
				zpos4 = z + 3 * vZ;
				i = i + 1;
				world.addParticle(ParticleTypes.LAVA, x_pos, (y_pos + 1.8), z_pos, 0, 0, 0);
				world.addParticle(ParticleTypes.FLAME, x_pos, (y_pos + 1.8), z_pos, 0, 0, 0);
				world.addParticle((SimpleParticleType) (SololevelingModParticleTypes.MANA_RED.get()), x_pos, (y_pos + 1.8), z_pos, 0, 0, 0);
				{
					final Vec3 _center = new Vec3(xpos2, (ypos2 + 1.8), zpos2);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator || entityiterator instanceof ExperienceOrb || entityiterator instanceof ItemEntity)) {
							entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_ATTACK), entity),
									(float) (24 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength / 3));
							entityiterator.setDeltaMovement(new Vec3((6 * entity.getLookAngle().x), (4 * entity.getLookAngle().y), (6 * entity.getLookAngle().z)));
						}
					}
				}
				{
					final Vec3 _center = new Vec3(xpos3, (ypos3 + 1.8), zpos3);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator || entityiterator instanceof ExperienceOrb || entityiterator instanceof ItemEntity)) {
							entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_ATTACK), entity),
									(float) (24 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength / 3));
							entityiterator.setDeltaMovement(new Vec3((5 * entity.getLookAngle().x), (3 * entity.getLookAngle().y), (5 * entity.getLookAngle().z)));
						}
					}
				}
				{
					final Vec3 _center = new Vec3(x_pos, (y_pos + 1.8), z_pos);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator || entityiterator instanceof ExperienceOrb || entityiterator instanceof ItemEntity)) {
							entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_ATTACK), entity),
									(float) (24 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength / 3));
							entityiterator.setDeltaMovement(new Vec3((4 * entity.getLookAngle().x), (2 * entity.getLookAngle().y), (4 * entity.getLookAngle().z)));
						}
					}
				}
			}
		}
	}
}
