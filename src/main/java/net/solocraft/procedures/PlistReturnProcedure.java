package net.solocraft.procedures;

import net.solocraft.util.SkillListHelper;

import net.minecraft.world.entity.Entity;

public class PlistReturnProcedure {
	public static String execute(Entity entity, int position) {
		return SkillListHelper.displaySkillAt(entity, position);
	}
}
