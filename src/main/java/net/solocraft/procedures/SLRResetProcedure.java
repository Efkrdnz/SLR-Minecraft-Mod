package net.solocraft.procedures;

import net.solocraft.util.PlayerProgressResetManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class SLRResetProcedure {
	public static void execute(LevelAccessor world,
			CommandContext<CommandSourceStack> arguments) {
		try {
			int resetCount = 0;
			for (ServerPlayer player : EntityArgument.getPlayers(arguments, "name"))
				if (PlayerProgressResetManager.reset(player))
					resetCount++;
			int result = resetCount;
			arguments.getSource().sendSuccess(() -> Component.literal(
					"Reset Solo Leveling progression for " + result + " player(s)."), true);
		} catch (CommandSyntaxException exception) {
			arguments.getSource().sendFailure(
					Component.literal("Unable to resolve reset targets."));
		}
	}
}
