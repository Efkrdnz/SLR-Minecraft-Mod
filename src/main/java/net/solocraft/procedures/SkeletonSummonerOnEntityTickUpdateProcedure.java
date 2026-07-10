package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.SkeletonSummonerEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class SkeletonSummonerOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying())
			return;
		Entity target = null;
		if (entity instanceof SkeletonSummonerEntity _datEntSetI)
			_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_GlobalAttackCooldown, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_GlobalAttackCooldown) : 0) - 1));
		if (entity instanceof SkeletonSummonerEntity _datEntSetI)
			_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_MeleeCooldown, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_MeleeCooldown) : 0) - 1));
		if (entity instanceof SkeletonSummonerEntity _datEntSetI)
			_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_SummoningCooldown, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_SummoningCooldown) : 0) - 1));
		if (entity instanceof SkeletonSummonerEntity _datEntSetI)
			_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_ProjectileCooldown, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_ProjectileCooldown) : 0) - 1));
		if (entity instanceof SkeletonSummonerEntity _datEntSetI)
			_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_RushCooldown, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_RushCooldown) : 0) - 1));
		if (entity instanceof SkeletonSummonerEntity _datEntSetI)
			_datEntSetI.getEntityData().set(SkeletonSummonerEntity.DATA_RepulsionCooldown, (int) ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_RepulsionCooldown) : 0) - 1));
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == (null))) {
			target = entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null;
			entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3((target.getX()), (target.getY()), (target.getZ())));
			SololevelingModVariables.MapVariables.get(world).gatetimer = Math.sqrt(Math.pow(entity.getX() - target.getX(), 2) + Math.pow(entity.getY() - target.getY(), 2) + Math.pow(entity.getZ() - target.getZ(), 2));
			SololevelingModVariables.MapVariables.get(world).syncData(world);
			if ((entity instanceof SkeletonSummonerEntity _datEntS ? _datEntS.getEntityData().get(SkeletonSummonerEntity.DATA_State) : "").equals("IDLE")) {
				if (entity instanceof SkeletonSummonerEntity _datEntSetS)
					_datEntSetS.getEntityData().set(SkeletonSummonerEntity.DATA_State, "TARGETING");
			} else if ((entity instanceof SkeletonSummonerEntity _datEntS ? _datEntS.getEntityData().get(SkeletonSummonerEntity.DATA_State) : "").equals("TARGETING")) {
				HandleTargetingStateProcedure.execute(entity);
			} else if ((entity instanceof SkeletonSummonerEntity _datEntS ? _datEntS.getEntityData().get(SkeletonSummonerEntity.DATA_State) : "").equals("MELEE")) {
				if ((entity instanceof SkeletonSummonerEntity _datEntI ? _datEntI.getEntityData().get(SkeletonSummonerEntity.DATA_MeleeType) : 0) == 1) {
					Melee1Procedure.execute(world, entity);
				} else {
					Melee2Procedure.execute(world, entity);
				}
			} else if ((entity instanceof SkeletonSummonerEntity _datEntS ? _datEntS.getEntityData().get(SkeletonSummonerEntity.DATA_State) : "").equals("SUMMONING")) {
				SummoningAttackProcedure.execute(world, entity);
			} else if ((entity instanceof SkeletonSummonerEntity _datEntS ? _datEntS.getEntityData().get(SkeletonSummonerEntity.DATA_State) : "").equals("PROJECTILE")) {
				ProjectileAttackProcedure.execute(world, entity);
			} else if ((entity instanceof SkeletonSummonerEntity _datEntS ? _datEntS.getEntityData().get(SkeletonSummonerEntity.DATA_State) : "").equals("REPULSION")) {
				RepulsionAttackProcedure.execute(world, entity);
			}
		} else {
			if (entity instanceof SkeletonSummonerEntity _datEntSetS)
				_datEntSetS.getEntityData().set(SkeletonSummonerEntity.DATA_State, "IDLE");
		}
	}
}
