package net.solocraft.procedures;

import net.solocraft.init.SololevelingModSounds;
import net.solocraft.entity.HunterEntity;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.BlockPos;

import org.joml.Vector3f;

import javax.annotation.Nullable;

@EventBusSubscriber
public class FighterDamageDealProcedure {
	private static final DustParticleOptions FIGHTER_SLASH_PARTICLE = new DustParticleOptions(new Vector3f(0.61F, 0.0F, 0.0F), 1.0F);

	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getY(), entity.getZ(), event.getSource(), entity, event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, DamageSource damagesource, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, damagesource, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, DamageSource damagesource, Entity entity, Entity sourceentity) {
		if (damagesource == null || entity == null || sourceentity == null)
			return;
		double particleNum = 0;
		double vX = 0;
		double vY = 0;
		double vZ = 0;
		double i = 0;
		double x_pos = 0;
		double z_pos = 0;
		double hei = 0;
		double speed = 0;
		double arcAngle = 0;
		double rand = 0;
		double radAngle = 0;
		double radYaw = 0;
		double radPitch = 0;
		double angle = 0;
		double y_pos = 0;
		double radius = 0;
		if (sourceentity instanceof HunterEntity) {
			if ((sourceentity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_HunterClass) : "").equals("Fighter")) {
				RandomSource random = RandomSource.create();
				rand = Mth.nextInt(random, 1, 3);
				if (rand == 1) {
					if (sourceentity instanceof LivingEntity _entity)
						_entity.swing(InteractionHand.MAIN_HAND, true);
					radius = 2.3;
					hei = -2;
					speed = 5;
					particleNum = 30;
					arcAngle = 180;
					radYaw = Math.toRadians(sourceentity.getYRot() + 90);
					radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
					for (int index0 = 0; index0 < (int) particleNum; index0++) {
						angle = i * (arcAngle / particleNum);
						radAngle = Math.toRadians(angle);
						vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
						vY = Math.sin(radAngle) * Math.cos(radPitch);
						vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
						x_pos = sourceentity.getX() + radius * vX;
						y_pos = sourceentity.getY() + hei + radius * vY;
						z_pos = sourceentity.getZ() + radius * vZ;
						i = i + 1;
						hei = hei + 0.133;
						if (entity.level() instanceof ServerLevel level)
							sendForcedDust(level, FIGHTER_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
					}
				} else if (rand == 2) {
					if (sourceentity instanceof LivingEntity _entity)
						_entity.swing(InteractionHand.MAIN_HAND, true);
					radius = 2.3;
					hei = 2;
					speed = 5;
					particleNum = 30;
					arcAngle = 180;
					radYaw = Math.toRadians(sourceentity.getYRot() + 90);
					radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
					for (int index1 = 0; index1 < (int) particleNum; index1++) {
						angle = i * (arcAngle / particleNum);
						radAngle = Math.toRadians(angle);
						vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
						vY = Math.sin(radAngle) * Math.cos(radPitch);
						vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
						x_pos = sourceentity.getX() + radius * vX;
						y_pos = sourceentity.getY() + hei + radius * vY;
						z_pos = sourceentity.getZ() + radius * vZ;
						i = i + 1;
						hei = hei - 0.133;
						if (entity.level() instanceof ServerLevel level)
							sendForcedDust(level, FIGHTER_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
					}
				} else {
					if (sourceentity instanceof LivingEntity _entity)
						_entity.swing(InteractionHand.MAIN_HAND, true);
					radius = 2.3;
					hei = 0;
					speed = 5;
					particleNum = 30;
					arcAngle = 180;
					radYaw = Math.toRadians(sourceentity.getYRot() + 90);
					radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
					for (int index2 = 0; index2 < (int) particleNum; index2++) {
						angle = i * (arcAngle / particleNum);
						radAngle = Math.toRadians(angle);
						vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
						vY = Math.sin(radAngle) * Math.cos(radPitch);
						vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
						x_pos = sourceentity.getX() + radius * vX;
						y_pos = sourceentity.getY() + hei + radius * vY;
						z_pos = sourceentity.getZ() + radius * vZ;
						i = i + 1;
						if (entity.level() instanceof ServerLevel level)
							sendForcedDust(level, FIGHTER_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
					}
				}
				playSwingSounds(world, sourceentity, random);
			}
		}
	}

	private static void sendForcedDust(ServerLevel level, DustParticleOptions particle, double x, double y, double z) {
		for (ServerPlayer viewer : level.players())
			level.sendParticles(viewer, particle, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.1);
	}

	private static void playSwingSounds(LevelAccessor world, Entity sourceentity, RandomSource random) {
		if (!(world instanceof Level level))
			return;
		float slashPitch = (float) Mth.nextDouble(random, 0.7, 2.0);
		float sweepPitch = (float) Mth.nextDouble(random, 0.7, 1.2);
		if (!level.isClientSide()) {
			BlockPos sourcePos = BlockPos.containing(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ());
			level.playSound(null, sourcePos, SololevelingModSounds.SLASH.get(), SoundSource.NEUTRAL, 0.3F, slashPitch);
			level.playSound(null, sourcePos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.5F, sweepPitch);
		} else {
			level.playLocalSound(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ(), SololevelingModSounds.SLASH.get(), SoundSource.NEUTRAL, 0.3F, slashPitch, false);
			level.playLocalSound(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.5F, sweepPitch, false);
		}
	}
}
