package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.entity.BeruBossEntity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class BeruPunchesProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
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
		if (sourceentity instanceof BeruBossEntity) {
			if ((damagesource).is(DamageTypes.MOB_ATTACK)) {
				if (sourceentity instanceof BeruBossEntity) {
					((BeruBossEntity) sourceentity).setAnimation("flyattack");
				}
				if (sourceentity instanceof BeruBossEntity _datEntSetI)
					_datEntSetI.getEntityData().set(BeruBossEntity.DATA_recovery, 5);
				entity.setDeltaMovement(new Vec3((1.3 * sourceentity.getLookAngle().x), (1.3 * sourceentity.getLookAngle().y), (1.3 * sourceentity.getLookAngle().z)));
				sourceentity.setDeltaMovement(new Vec3((Mth.nextDouble(RandomSource.create(), -2, 2)), 0.6, (Mth.nextDouble(RandomSource.create(), -2, 2))));
				if (world instanceof ServerLevel _level)
					_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.IMPACT_22.get()), x, y, z, 3, 0.2, 0.5, 0.2, 0);
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 2, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 2, 1, false);
					}
				}
			}
		}
	}
}
