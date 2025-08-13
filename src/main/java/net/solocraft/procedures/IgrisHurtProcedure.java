package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.BloodRedComIgrisEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class IgrisHurtProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getY(), entity.getZ(), entity, event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double invulnerable = 0;
		double rand = 0;
		double randX = 0;
		double randZ = 0;
		double tprand = 0;
		if (entity instanceof BloodRedComIgrisEntity || entity instanceof IgrisShadowEntity) {
			tprand = Mth.nextInt(RandomSource.create(), 1, 2);
			if (tprand == 1) {
				rand = Mth.nextInt(RandomSource.create(), 1, 5);
				randX = Mth.nextInt(RandomSource.create(), 1, 2);
				randZ = Mth.nextInt(RandomSource.create(), 1, 2);
				if (randX == 1) {
					randX = 1;
				} else if (randX == 2) {
					randX = -1;
				}
				if (randZ == 1) {
					randZ = 1;
				} else if (randZ == 2) {
					randZ = -1;
				}
				if (rand == 3) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_RED.get()), x, y, z, 4, 0.5, 1.5, 0.5, 0);
					entity.setDeltaMovement(new Vec3(randX, 0.1, randZ));
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_RED.get()), x, y, z, 4, 0.5, 1.5, 0.5, 0);
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
				}
			} else if (tprand == 2) {
				rand = Mth.nextInt(RandomSource.create(), 1, 5);
				if (rand == 3) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_RED.get()), x, y, z, 4, 0.5, 1.5, 0.5, 0);
					{
						Entity _ent = entity;
						_ent.teleportTo((sourceentity.getX() + 1 * sourceentity.getLookAngle().x), (sourceentity.getY() + 0.25), (sourceentity.getZ() + 1 * sourceentity.getLookAngle().z));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport((sourceentity.getX() + 1 * sourceentity.getLookAngle().x), (sourceentity.getY() + 0.25), (sourceentity.getZ() + 1 * sourceentity.getLookAngle().z), _ent.getYRot(), _ent.getXRot());
					}
					if (world instanceof ServerLevel _level)
						_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.GLOW_AURA_RED.get()), x, y, z, 4, 0.5, 1.5, 0.5, 0);
					if (event != null && event.isCancelable()) {
						event.setCanceled(true);
					}
				}
			}
		}
	}
}
