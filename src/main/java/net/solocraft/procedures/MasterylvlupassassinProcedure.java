package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class MasterylvlupassassinProcedure {
	private static final List<String> MASTERY_SKILLS = List.of("Quickslashes", "Shadowstep", "Backstab", "Dualwield");

	public static void execute(Entity entity) {
		if (entity == null)
			return;
		String current = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables()).Plist;
		if (current == null)
			current = ".";
		List<String> missing = new ArrayList<>();
		for (String skill : MASTERY_SKILLS) {
			if (!containsSkill(current, skill))
				missing.add(skill);
		}
		if (missing.isEmpty())
			return;

		String unlocked = missing.get(entity.level().random.nextInt(missing.size()));
		String updated = current + unlocked + ",";
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.Plist = updated;
			capability.syncPlayerVariables(entity);
		});
		if (entity instanceof Player player && !player.level().isClientSide())
			player.displayClientMessage(Component.literal("Gained skill: " + unlocked), false);
	}

	private static boolean containsSkill(String encoded, String skill) {
		if (encoded == null || encoded.isBlank())
			return false;
		for (String entry : encoded.split(",")) {
			if (entry.replaceFirst("^\\.", "").trim().equalsIgnoreCase(skill))
				return true;
		}
		return false;
	}
}
