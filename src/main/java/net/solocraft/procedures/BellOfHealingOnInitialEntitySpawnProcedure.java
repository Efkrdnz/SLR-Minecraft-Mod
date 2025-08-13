package net.solocraft.procedures;

import net.solocraft.entity.BellOfHealingEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;

public class BellOfHealingOnInitialEntitySpawnProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		SololevelingMod.queueServerWork(2, () -> {
			if (entity instanceof BellOfHealingEntity) {
				((BellOfHealingEntity) entity).setAnimation("spawn");
			}
			{
				Entity _ent = entity;
				_ent.teleportTo((entity.getX()), (entity.getY() + 2), (entity.getZ()));
				if (_ent instanceof ServerPlayer _serverPlayer)
					_serverPlayer.connection.teleport((entity.getX()), (entity.getY() + 2), (entity.getZ()), _ent.getYRot(), _ent.getXRot());
			}
		});
		SololevelingMod.queueServerWork(200, () -> {
			if (!entity.level().isClientSide())
				entity.discard();
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.GLOW_SQUID_INK, x, y, z, 10, 2, 2, 2, 0.1);
		});
	}
}
