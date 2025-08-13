package net.solocraft.procedures;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;

public class Swing1Procedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double particleNum = 0;
		double ypos4 = 0;
		double ypos3 = 0;
		double x_pos = 0;
		double ypos2 = 0;
		double hei = 0;
		double speed = 0;
		double radAngle = 0;
		double radYaw = 0;
		double radPitch = 0;
		double xpos4 = 0;
		double xpos3 = 0;
		double angle = 0;
		double xpos2 = 0;
		double y_pos = 0;
		double radius = 0;
		double vX = 0;
		double vY = 0;
		double vZ = 0;
		double i = 0;
		double z_pos = 0;
		double arcAngle = 0;
		double zpos4 = 0;
		double zpos3 = 0;
		double zpos2 = 0;
		if (entity instanceof LivingEntity _entity)
			_entity.swing(InteractionHand.MAIN_HAND, true);
		radius = 3;
		hei = -2;
		speed = 15;
		particleNum = 10;
		arcAngle = 180;
		radYaw = Math.toRadians(entity.getYRot() + 90);
		radPitch = Math.toRadians((entity.getXRot() + 90) * (-1));
		for (int index0 = 0; index0 < (int) particleNum; index0++) {
			angle = i * (arcAngle / particleNum);
			radAngle = Math.toRadians(angle);
			vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
			vY = Math.sin(radAngle) * Math.cos(radPitch);
			vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
			x_pos = x + radius * vX;
			y_pos = y + hei + radius * vY;
			z_pos = z + radius * vZ;
			i = i + 1;
			hei = hei + 0.4;
			world.addParticle(ParticleTypes.SWEEP_ATTACK, x_pos, (y_pos + 1.8), z_pos, 0, 0, 0);
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.5, 2);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.5, 2, false);
				}
			}
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL, 1, 1);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL, 1, 1, false);
				}
			}
			{
				final Vec3 _center = new Vec3(x_pos, (y_pos + 1.8), z_pos);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator || entityiterator instanceof ExperienceOrb || entityiterator instanceof ItemEntity)) {
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_ATTACK), entity),
								(float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
						entityiterator.setDeltaMovement(new Vec3((1 * entity.getLookAngle().x), (0.875 * entity.getLookAngle().y), (1 * entity.getLookAngle().z)));
					}
				}
			}
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator || entityiterator instanceof ExperienceOrb || entityiterator instanceof ItemEntity)) {
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_ATTACK), entity),
								(float) ((LivingEntity) entity).getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).getValue());
						entityiterator.setDeltaMovement(new Vec3((1 * entity.getLookAngle().x), (0.875 * entity.getLookAngle().y), (1 * entity.getLookAngle().z)));
					}
				}
			}
		}
	}
}
