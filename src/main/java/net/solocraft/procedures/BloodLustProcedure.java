package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.MurderousIntentFearGoal;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.List;
import net.solocraft.util.CooldownManager;

public class BloodLustProcedure {
	private static final int EFFECT_DURATION = 80;
	private static final int MAX_PANICKING_TARGETS = 32;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_BLUE.get()), x, y, z, 20, 2, 2, 2, 1);
		if (entity instanceof Player player && player.isCreative())
			CooldownManager.clear(entity, "Murderious Intent");
		else
			CooldownManager.setFullDuration(entity, "Murderious Intent", 220);
		{
			final Vec3 _center = new Vec3(x, y, z);
			SololevelingModVariables.PlayerVariables casterVars = entity
					.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.orElse(new SololevelingModVariables.PlayerVariables());
			String casterParty = casterVars.party;
			ServerPlayer caster = entity instanceof ServerPlayer player ? player : null;
			List<LivingEntity> _entfound = world.getEntitiesOfClass(LivingEntity.class,
					new AABB(_center, _center).inflate(25 / 2d), e -> e != entity);
			int panickingTargets = 0;
			for (LivingEntity entityiterator : _entfound) {
					String targetParty = entityiterator
							.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
							.map(data -> data.party).orElse("");
					if (!casterParty.isEmpty() && casterParty.equals(targetParty)
							|| caster != null && isOwnedCompanion(caster, entityiterator))
						continue;

					boolean frightened = panickingTargets < MAX_PANICKING_TARGETS && caster != null
							&& entityiterator instanceof PathfinderMob pathfinder
							&& MurderousIntentFearGoal.apply(caster, pathfinder, EFFECT_DURATION);
					if (frightened)
						panickingTargets++;
					if (!entityiterator.level().isClientSide()) {
						entityiterator.addEffect(new MobEffectInstance(MobEffects.DARKNESS, EFFECT_DURATION, 1, false, false));
						if (!frightened)
							entityiterator.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_DURATION, 1, false, false));
						entityiterator.addEffect(new MobEffectInstance(SololevelingModMobEffects.SCREEN_SHAKE, EFFECT_DURATION, 1, false, false));
				}
				if (world instanceof ServerLevel _level)
					_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.MANA_BLUE.get()),
							entityiterator.getX(), entityiterator.getY(), entityiterator.getZ(), 10, 1, 1, 1, 1);
			}
		}
	}

	private static boolean isOwnedCompanion(ServerPlayer caster, LivingEntity target) {
		return caster.isAlliedTo(target) || target.isAlliedTo(caster)
				|| ShadowMonarchManager.isOwnedShadow(target, caster)
				|| target instanceof TamableAnimal tame && caster.getUUID().equals(tame.getOwnerUUID());
	}
}
