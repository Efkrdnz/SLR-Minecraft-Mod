package net.solocraft.procedures;

import net.solocraft.util.daily.DailyQuestLifecycleManager;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class SLRFinishDailyProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				if (entityiterator instanceof ServerPlayer player)
					DailyQuestLifecycleManager.finishQuestNow(player);
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
	}
}
