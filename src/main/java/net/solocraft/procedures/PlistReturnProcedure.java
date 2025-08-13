package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class PlistReturnProcedure {
	public static String execute(Entity entity, int position) {
		if (entity == null || position < 1)
			return "empty";
		String plistOriginal = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist;
		if (plistOriginal == null || plistOriginal.isEmpty() || !plistOriginal.contains(".") || !plistOriginal.contains(","))
			return "empty";
		String[] items = plistOriginal.split(",");
		if (position > items.length)
			return "empty";
		String item = items[position - 1];
		if (item.contains(".")) {
			return item.substring(item.indexOf(".") + 1);
		} else {
			return item;
		}
	}
}
