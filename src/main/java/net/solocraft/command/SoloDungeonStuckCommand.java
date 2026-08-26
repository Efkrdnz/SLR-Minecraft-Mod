
package net.solocraft.command;

import net.solocraft.procedures.SoloDungeonStuckProcedureProcedure;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

@EventBusSubscriber
public class SoloDungeonStuckCommand {
	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("solodungeonstuck")

				.executes(arguments -> {
					boolean escaped = SoloDungeonStuckProcedureProcedure.execute(
							arguments.getSource().getPlayerOrException());
					if (!escaped) {
						arguments.getSource().sendFailure(Component.literal(
								"No active dungeon recovery point was found."));
						return 0;
					}
					arguments.getSource().sendSuccess(() -> Component.literal(
							"Returned safely from the dungeon.")
							.withStyle(ChatFormatting.YELLOW), false);
					return 1;
				}));
	}
}
