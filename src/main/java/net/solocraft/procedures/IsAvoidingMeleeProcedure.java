package net.solocraft.procedures;

import net.solocraft.entity.HunterEntity;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;

public class IsAvoidingMeleeProcedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null) && ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_HunterClass) : "").equals("Ranger")
				|| (entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_HunterClass) : "").equals("Mage")
				|| (entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_HunterClass) : "").equals("Healer"))) {
			return true;
		}
		return false;
	}
}
