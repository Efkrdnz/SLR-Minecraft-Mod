package net.solocraft.procedures;

import net.solocraft.entity.GoblinKingEntity;
import net.solocraft.util.CombatRangeHelper;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;

public class GoblinBossstatechangerProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		rand = Mth.nextInt(RandomSource.create(), 1, 2);
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			double distance = CombatRangeHelper.surfaceDistance(entity,
					(entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null));
			if (distance <= 3.0D) {
				if (rand == 1) {
					if (entity instanceof GoblinKingEntity _datEntSetI)
						_datEntSetI.getEntityData().set(GoblinKingEntity.DATA_MF, 0);
					if (entity instanceof GoblinKingEntity _datEntSetS)
						_datEntSetS.getEntityData().set(GoblinKingEntity.DATA_state, "attack1");
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10));
				} else if (rand == 2) {
					if (entity instanceof GoblinKingEntity _datEntSetI)
						_datEntSetI.getEntityData().set(GoblinKingEntity.DATA_MF, 0);
					if (entity instanceof GoblinKingEntity _datEntSetS)
						_datEntSetS.getEntityData().set(GoblinKingEntity.DATA_state, "attack2");
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10));
				}
			} else if (distance <= 8.0D) {
				if (entity instanceof GoblinKingEntity _datEntSetS)
					_datEntSetS.getEntityData().set(GoblinKingEntity.DATA_state, "idle");
				if (entity instanceof GoblinKingEntity _datEntSetI)
					_datEntSetI.getEntityData().set(GoblinKingEntity.DATA_MF, 0);
			} else {
				if (rand == 1) {
					if (entity instanceof GoblinKingEntity _datEntSetI)
						_datEntSetI.getEntityData().set(GoblinKingEntity.DATA_MF, 0);
					if (entity instanceof GoblinKingEntity _datEntSetS)
						_datEntSetS.getEntityData().set(GoblinKingEntity.DATA_state, "Dash");
				} else if (rand == 2) {
					if (entity instanceof GoblinKingEntity _datEntSetI)
						_datEntSetI.getEntityData().set(GoblinKingEntity.DATA_MF, 0);
					if (entity instanceof GoblinKingEntity _datEntSetS)
						_datEntSetS.getEntityData().set(GoblinKingEntity.DATA_state, "Slam");
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 10));
				}
			}
		}
	}
}
