package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistButtonCon53Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (!(PlistReturn53Procedure.execute(entity)).equals("empty")) {
			return true;
		}
		return false;
	}
}
