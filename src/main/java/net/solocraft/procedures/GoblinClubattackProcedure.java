package net.solocraft.procedures;

import net.solocraft.entity.GoblinClubEntity;
import net.solocraft.util.CombatRangeHelper;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;

public class GoblinClubattackProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 0) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				if ((entity instanceof GoblinClubEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubEntity.DATA_state) : "").equals("attack")) {
					if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) == 1) {
						if (entity instanceof GoblinClubEntity) {
							((GoblinClubEntity) entity).setAnimation("attack_1");
						}
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 12, 2, false, false));
					}
					if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) == 4) {
						if (CombatRangeHelper.withinSurfaceRange(entity,
								(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null), 2.0D)) {
							(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 3);
						}
					}
					if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) == 8) {
						if (CombatRangeHelper.withinSurfaceRange(entity,
								(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null), 2.0D)) {
							if (entity instanceof GoblinClubEntity _datEntSetS)
								_datEntSetS.getEntityData().set(GoblinClubEntity.DATA_state, "attack2");
							if (entity instanceof GoblinClubEntity _datEntSetI)
								_datEntSetI.getEntityData().set(GoblinClubEntity.DATA_MF, 0);
						}
					}
					if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) >= 11) {
						if (entity instanceof GoblinClubEntity _datEntSetS)
							_datEntSetS.getEntityData().set(GoblinClubEntity.DATA_state, "idle");
						if (entity instanceof GoblinClubEntity _datEntSetI)
							_datEntSetI.getEntityData().set(GoblinClubEntity.DATA_MF, 0);
					}
				}
				if ((entity instanceof GoblinClubEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubEntity.DATA_state) : "").equals("attack2")) {
					if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) == 1) {
						if (entity instanceof GoblinClubEntity) {
							((GoblinClubEntity) entity).setAnimation("attack_2");
						}
						if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
							_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 22, 2, false, false));
					}
					if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) == 2) {
						if (CombatRangeHelper.withinSurfaceRange(entity,
								(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null), 2.0D)) {
							(entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), 3);
						}
					}
					if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) >= 21) {
						if (entity instanceof GoblinClubEntity _datEntSetS)
							_datEntSetS.getEntityData().set(GoblinClubEntity.DATA_state, "idle");
						if (entity instanceof GoblinClubEntity _datEntSetI)
							_datEntSetI.getEntityData().set(GoblinClubEntity.DATA_MF, 0);
					}
				}
			} else {
				if (entity instanceof GoblinClubEntity _datEntSetS)
					_datEntSetS.getEntityData().set(GoblinClubEntity.DATA_state, "idle");
			}
		}
	}
}
