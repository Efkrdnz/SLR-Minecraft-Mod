package net.solocraft.procedures;

import net.solocraft.entity.ChoijongEntity;
import net.solocraft.util.CombatRangeHelper;
import net.solocraft.util.FireMageSpellManager;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class ChoijongOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		double dmg_modifier = 0;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == (null))) {
			Entity target = entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null;
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()),
					target.getY() + target.getBbHeight() * 0.6D, target.getZ()));
			CombatRangeHelper.maintainRangedBand(entity, target, 9.0D, 22.0D, 1.15D);
			if (entity instanceof ChoijongEntity _datEntSetI)
				_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, (int) ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_IA) : 0) + 1));
			int attackTimer = entity instanceof ChoijongEntity choi
					? choi.getEntityData().get(ChoijongEntity.DATA_IA) : 0;
			if (attackTimer % 24 == 0 && entity instanceof ChoijongEntity choi
					&& target instanceof net.minecraft.world.entity.LivingEntity livingTarget
					&& CombatRangeHelper.withinSurfaceRange(entity, target, 24.0D)
					&& choi.getSensing().hasLineOfSight(livingTarget)) {
				choi.performRangedAttack(livingTarget, 1.0F);
			}
			if ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_IA) : 0) == 60) {
				rand = Mth.nextInt(RandomSource.create(), 1, 100);
				String spell = rand <= 30 ? FireMageSpellManager.INFERNO_LANCE
						: rand <= 52 ? FireMageSpellManager.IGNITION_ORB
						: rand <= 70 ? FireMageSpellManager.FLASHFIRE
						: rand <= 86 ? FireMageSpellManager.CREMATION
						: rand <= 97 ? FireMageSpellManager.FURNACE_DOMINION
						: FireMageSpellManager.HEAVENFALL;
				if (!FireMageSpellManager.castNpc(entity, spell) && FireMageSpellManager.CREMATION.equals(spell))
					FireMageSpellManager.castNpc(entity, FireMageSpellManager.INFERNO_LANCE);
			} else if ((entity instanceof ChoijongEntity _datEntI ? _datEntI.getEntityData().get(ChoijongEntity.DATA_IA) : 0) > 80) {
				if (entity instanceof ChoijongEntity _datEntSetI)
					_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, 0);
			}
		} else {
			if (entity instanceof ChoijongEntity _datEntSetI)
				_datEntSetI.getEntityData().set(ChoijongEntity.DATA_IA, 0);
		}
	}
}
