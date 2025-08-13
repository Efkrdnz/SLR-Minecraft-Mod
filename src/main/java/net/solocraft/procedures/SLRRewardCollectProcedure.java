package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class SLRRewardCollectProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		String r1 = "";
		String r2 = "";
		String r3 = "";
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				r1 = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_1;
				r2 = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_2;
				r3 = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_3;
				RewardCollectProcedure.execute(entityiterator, r1);
				RewardCollectProcedure.execute(entityiterator, r2);
				RewardCollectProcedure.execute(entityiterator, r3);
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.reward_1 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.reward_2 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.reward_3 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
	}
}
