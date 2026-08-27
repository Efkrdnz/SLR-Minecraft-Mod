package net.solocraft.procedures;

import net.solocraft.util.daily.DailyPunishmentManager;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class SLRPenaltyTriggerProcedure {
	public static void execute(LevelAccessor world, CommandContext<CommandSourceStack> arguments) {
		if (!world.isClientSide()) {
			try {
				for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
					if (!(entityiterator instanceof ServerPlayer player))
						continue;
					boolean keepSecret = DailyQuestHelper.isSecretQuest(entityiterator)
							|| DailyQuestHelper.canActivateSecretQuest(entityiterator);
					DailyQuestHelper.sendQuestFailedChat(entityiterator);
					DailyQuestHelper.resetDailyProgress(entityiterator);
					if (keepSecret)
						DailyQuestHelper.keepSecretQuestPending(entityiterator);
					DailyPunishmentManager.enter(player);
				}
			} catch (CommandSyntaxException e) {
				e.printStackTrace();
			}
		}
	}
}
