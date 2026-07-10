package net.solocraft.procedures;

import net.solocraft.util.RewardManager;

import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class SLRRewardCollectProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				while (RewardManager.hasRewards(entityiterator))
					RewardManager.claimReward(entityiterator, 1);
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
	}
}
