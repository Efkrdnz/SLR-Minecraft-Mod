package net.solocraft.procedures;

import net.minecraft.world.entity.Entity;

public class PlistButtonCon21Procedure {
	public static boolean execute(Entity entity) {
		if (entity == null)
			return false;
		if (!(PlistReturn21Procedure.execute(entity)).equals("empty")) {
			return true;
		}
		return false;
	}
}
