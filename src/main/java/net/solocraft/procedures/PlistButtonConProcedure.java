package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistButtonConProcedure {
	public static boolean execute(Entity entity, int index) {
		if (entity == null)
			return false;
		if (!(PlistReturnProcedure.execute(entity, index)).equals("empty")) {
			return true;
		}
		return false;
	}
}
