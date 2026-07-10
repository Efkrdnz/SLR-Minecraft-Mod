package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class SLRFinishDailyProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				if ((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).ActiveDaily == true) {
					{
						double _setval = DailyQuestHelper.trainingTarget(entityiterator);
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.situp = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = DailyQuestHelper.trainingTarget(entityiterator);
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.squat = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = DailyQuestHelper.trainingTarget(entityiterator);
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.pushup = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = DailyQuestHelper.runTarget(entityiterator);
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.RUN = _setval;
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
