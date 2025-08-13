package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistButtonCon26Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (!(PlistReturn26Procedure.execute(entity)).equals("empty")) {
			return true;
		}
		return false;
	}
}
