package net.solocraft.procedures;

import net.solocraft.entity.GoblinClubEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class GoblinClubOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 0) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
						((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
				if (entity instanceof Mob _entity)
					_entity.getNavigation().moveTo(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY()),
							((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), 1);
				if ((entity instanceof GoblinClubEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubEntity.DATA_state) : "").equals("idle")) {
					if (Math.sqrt(Math.pow(entity.getX() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX(), 2) + Math.pow(entity.getY() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY(), 2)
							+ Math.pow(entity.getZ() - (entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ(), 2)) <= 2.5) {
						if (entity instanceof GoblinClubEntity _datEntSetS)
							_datEntSetS.getEntityData().set(GoblinClubEntity.DATA_state, "attack");
					} else {
						if (entity instanceof GoblinClubEntity _datEntSetS)
							_datEntSetS.getEntityData().set(GoblinClubEntity.DATA_state, "idle");
					}
				}
			}
			if ((entity instanceof GoblinClubEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubEntity.DATA_state) : "").equals("attack")) {
				GoblinClubattackProcedure.execute(world, entity);
				if (entity instanceof GoblinClubEntity _datEntSetI)
					_datEntSetI.getEntityData().set(GoblinClubEntity.DATA_MF, (int) ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) + 1));
				if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) >= 11) {
					if (entity instanceof GoblinClubEntity _datEntSetS)
						_datEntSetS.getEntityData().set(GoblinClubEntity.DATA_state, "idle");
					if (entity instanceof GoblinClubEntity _datEntSetI)
						_datEntSetI.getEntityData().set(GoblinClubEntity.DATA_MF, 0);
				}
			}
			if ((entity instanceof GoblinClubEntity _datEntS ? _datEntS.getEntityData().get(GoblinClubEntity.DATA_state) : "").equals("attack2")) {
				if (entity instanceof GoblinClubEntity _datEntSetI)
					_datEntSetI.getEntityData().set(GoblinClubEntity.DATA_MF, (int) ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) + 1));
				if ((entity instanceof GoblinClubEntity _datEntI ? _datEntI.getEntityData().get(GoblinClubEntity.DATA_MF) : 0) >= 21) {
					if (entity instanceof GoblinClubEntity _datEntSetS)
						_datEntSetS.getEntityData().set(GoblinClubEntity.DATA_state, "idle");
					if (entity instanceof GoblinClubEntity _datEntSetI)
						_datEntSetI.getEntityData().set(GoblinClubEntity.DATA_MF, 0);
				}
			}
		}
	}
}
