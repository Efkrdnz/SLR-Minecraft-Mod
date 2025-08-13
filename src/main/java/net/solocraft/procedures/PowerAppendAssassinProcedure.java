package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class PowerAppendAssassinProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double rank = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank;
		if (rank > 1) {
			if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist).contains("Shadowstep")) {
				{
					String _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist + "Shadowstep" + ",";
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Plist = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
			if (rank > 2) {
				if (Math.random() < (3) / ((float) 4)) {
					if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist).contains("Backstab")) {
						{
							String _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist + "Backstab" + ",";
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Plist = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					}
				}
				if (rank > 3) {
					if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist).contains("Stealth")) {
						{
							String _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist + "Stealth" + ",";
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Plist = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					}
					if (rank > 4) {
						if (Math.random() < (2) / ((float) 3)) {
							if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist).contains("Quickslashes")) {
								{
									String _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist + "Quickslashes" + ",";
									entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.Plist = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
							}
						}
						if (rank > 5) {
							if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist).contains("Dualwield")) {
								{
									String _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Plist + "Dualwield" + ",";
									entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.Plist = _setval;
										capability.syncPlayerVariables(entity);
									});
								}
							}
						}
					}
				}
			}
		}
	}
}
