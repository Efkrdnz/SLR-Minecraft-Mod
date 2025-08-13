package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;

public class SLRRewardSetFullRecoveryProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		double reward_slot = 0;
		boolean collect_prev_reward = false;
		String reward_to_recieve = "";
		String reward_to_assign = "";
		reward_slot = DoubleArgumentType.getDouble(arguments, "slot");
		collect_prev_reward = BoolArgumentType.getBool(arguments, "AutoCollect");
		reward_to_assign = "FR";
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				if (reward_slot == 1) {
					reward_to_recieve = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_1;
					if (collect_prev_reward) {
						RewardCollectProcedure.execute(entityiterator, reward_to_recieve);
					}
					{
						String _setval = reward_to_assign;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.reward_1 = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				}
				if (reward_slot == 2) {
					reward_to_recieve = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_2;
					if (collect_prev_reward) {
						RewardCollectProcedure.execute(entityiterator, reward_to_recieve);
					}
					{
						String _setval = reward_to_assign;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.reward_2 = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				}
				if (reward_slot == 3) {
					reward_to_recieve = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).reward_3;
					if (collect_prev_reward) {
						RewardCollectProcedure.execute(entityiterator, reward_to_recieve);
					}
					{
						String _setval = reward_to_assign;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.reward_3 = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				}
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
	}
}
