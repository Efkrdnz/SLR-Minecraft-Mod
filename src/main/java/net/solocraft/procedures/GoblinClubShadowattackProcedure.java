package net.solocraft.procedures;

import net.solocraft.entity.GoblinClubShadowEntity;
import net.solocraft.util.CombatRangeHelper;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;

public class GoblinClubShadowattackProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				if ((entity instanceof GoblinClubShadowEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubShadowEntity.DATA_state) : "").equals("attack")) {
					if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) == 1) {
						if (entity instanceof GoblinClubShadowEntity) {
							((GoblinClubShadowEntity) entity).setAnimation("attack_1");
						}
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 2, false, false));
					}
					if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) == 4) {
						if (CombatRangeHelper.withinSurfaceRange(entity,
								(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null), 2.5D)) {
							(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 5);
						}
					}
					if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) == 8) {
						if (CombatRangeHelper.withinSurfaceRange(entity,
								(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null), 2.5D)) {
							if (entity instanceof GoblinClubShadowEntity _datEntSetS)
								_datEntSetS.getEntityData().set(GoblinClubShadowEntity.DATA_state, "attack2");
							if (entity instanceof GoblinClubShadowEntity _datEntSetI)
								_datEntSetI.getEntityData().set(GoblinClubShadowEntity.DATA_MF, 0);
						}
					}
					if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) >= 11) {
						if (entity instanceof GoblinClubShadowEntity _datEntSetS)
							_datEntSetS.getEntityData().set(GoblinClubShadowEntity.DATA_state, "attack2");
						if (entity instanceof GoblinClubShadowEntity _datEntSetI)
							_datEntSetI.getEntityData().set(GoblinClubShadowEntity.DATA_MF, 0);
					}
				}
				if ((entity instanceof GoblinClubShadowEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubShadowEntity.DATA_state) : "").equals("attack2")) {
					if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) == 1) {
						if (entity instanceof GoblinClubShadowEntity) {
							((GoblinClubShadowEntity) entity).setAnimation("attack_2");
						}
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 22, 2, false, false));
					}
					if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) == 2) {
						if (CombatRangeHelper.withinSurfaceRange(entity,
								(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null), 2.5D)) {
							(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 8);
						}
					}
					if ((entity instanceof GoblinClubShadowEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubShadowEntity.DATA_MF) : 0) >= 21) {
						if (entity instanceof GoblinClubShadowEntity _datEntSetS)
							_datEntSetS.getEntityData().set(GoblinClubShadowEntity.DATA_state, "idle");
						if (entity instanceof GoblinClubShadowEntity _datEntSetI)
							_datEntSetI.getEntityData().set(GoblinClubShadowEntity.DATA_MF, 0);
					}
				}
			} else {
				if (entity instanceof GoblinClubShadowEntity _datEntSetS)
					_datEntSetS.getEntityData().set(GoblinClubShadowEntity.DATA_state, "idle");
			}
		}
	}
}
