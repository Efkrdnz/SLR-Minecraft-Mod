package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;

public class ExchangeCancel6Procedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		double EX = 0;
		double EY = 0;
		double EZ = 0;
		String ESY = "";
		String ESX = "";
		String ESZ = "";
		if (ExchangeCon6Procedure.execute(entity)) {
			ESX = ExchangeCordReturn6Procedure.execute(entity);
			ESY = ExchangeDim6Procedure.execute(entity);
			{
				String _setval = ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ExchangeDimensions).replace(ESY + ",", "");
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.ExchangeDimensions = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				String _setval = ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ExchangeCords).replace(ESX + ",", "");
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.ExchangeCords = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
		}
	}
}
