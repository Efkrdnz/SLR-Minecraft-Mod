package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistButtonCon16Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (!(PlistReturn16Procedure.execute(entity)).equals("empty")) {
			return true;
		}
		return false;
	}
}
