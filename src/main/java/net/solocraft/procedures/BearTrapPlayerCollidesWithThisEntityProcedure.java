package net.solocraft.procedures;

import net.solocraft.entity.BearTrapEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;

public class BearTrapPlayerCollidesWithThisEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
			if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == sourceentity)) {
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 10, 4, false, false));
				if (sourceentity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 7, 0, false, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 10, 0, false, false));
				if (entity instanceof BearTrapEntity) {
					((BearTrapEntity) entity).setAnimation("close");
				}
				SololevelingMod.queueServerWork(7, () -> {
					if (world instanceof Level _level && !_level.isClientSide()) {
						_level.explode((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null),
								new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_EXPLOSION), entity, (entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null)),
								null, x, y, z, 4, true, Level.ExplosionInteraction.MOB);
					}
					if (!entity.level().isClientSide())
						entity.discard();
				});
			}
		}
	}
}
