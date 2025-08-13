package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistButtonCon11Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (!(PlistReturn11Procedure.execute(entity)).equals("empty")) {
			return true;
		}
		return false;
	}
}
