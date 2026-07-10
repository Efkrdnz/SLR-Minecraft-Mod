package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class SkillSlotHelper {
	public static String getSlot(SololevelingModVariables.PlayerVariables vars, int slot) {
		return switch (slot) {
			case 1 -> vars.Pslot1;
			case 2 -> vars.Pslot2;
			case 3 -> vars.Pslot3;
			case 4 -> vars.Pslot4;
			case 5 -> vars.Pslot5;
			case 6 -> vars.Pslot6;
			case 7 -> vars.Pslot7;
			case 8 -> vars.Pslot8;
			case 9 -> vars.Pslot9;
			case 10 -> vars.Pslot10;
			case 11 -> vars.Pslot11;
			case 12 -> vars.Pslot12;
			case 13 -> vars.Pslot13;
			case 14 -> vars.Pslot14;
			case 15 -> vars.Pslot15;
			case 16 -> vars.Pslot16;
			default -> "";
		};
	}

	public static void setSlot(SololevelingModVariables.PlayerVariables vars, int slot, String value) {
		switch (slot) {
			case 1 -> vars.Pslot1 = value;
			case 2 -> vars.Pslot2 = value;
			case 3 -> vars.Pslot3 = value;
			case 4 -> vars.Pslot4 = value;
			case 5 -> vars.Pslot5 = value;
			case 6 -> vars.Pslot6 = value;
			case 7 -> vars.Pslot7 = value;
			case 8 -> vars.Pslot8 = value;
			case 9 -> vars.Pslot9 = value;
			case 10 -> vars.Pslot10 = value;
			case 11 -> vars.Pslot11 = value;
			case 12 -> vars.Pslot12 = value;
			case 13 -> vars.Pslot13 = value;
			case 14 -> vars.Pslot14 = value;
			case 15 -> vars.Pslot15 = value;
			case 16 -> vars.Pslot16 = value;
		}
	}

	public static int activeSlot(Entity entity, int hotbarSlot) {
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		int clamped = Math.max(1, Math.min(8, hotbarSlot));
		return vars.PskillPage >= 2 ? clamped + 8 : clamped;
	}
}
