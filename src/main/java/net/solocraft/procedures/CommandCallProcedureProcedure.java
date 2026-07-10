package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;

import java.util.UUID;

public class CommandCallProcedureProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double hei = 0;
		if (ShadowMonarchManager.isShadowEntity(entity)) {
			UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(entity);
			Player owner = ownerId == null ? null : entity.level().getPlayerByUUID(ownerId);
			if (owner != null && !owner.isAlive()) {
				if (world instanceof ServerLevel _level)
					_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 30, 0.05, 0.05, 0.05, 1);
				if (!entity.level().isClientSide())
					entity.discard();
			}
			if (ownerId == null && !entity.level().isClientSide())
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
	}
}
