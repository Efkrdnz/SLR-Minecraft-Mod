package net.solocraft.command;

import net.solocraft.procedures.CreatingPartyProcedure;
import net.solocraft.procedures.JoiningPartyProcedure;
import net.solocraft.procedures.PartyLeaveProcedure;
import net.solocraft.procedures.PartyMembersProcedure;
import net.solocraft.procedures.PartyShowProcedure;

import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.commands.Commands;

import com.mojang.brigadier.arguments.StringArgumentType;

/** Compatibility commands backed by the same server party service as the GUI. */
@EventBusSubscriber
public final class CreatePartyCommand {
	private CreatePartyCommand() {
	}

	@SubscribeEvent
	public static void registerCommand(RegisterCommandsEvent event) {
		event.getDispatcher().register(Commands.literal("Party")
				.executes(context -> {
					PartyShowProcedure.execute(context.getSource().getPlayerOrException());
					return 1;
				})
				.then(Commands.literal("Create")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> {
									CreatingPartyProcedure.execute(context,
											context.getSource().getPlayerOrException());
									return 1;
								})
								.then(Commands.argument("pass", StringArgumentType.word())
										.executes(context -> {
											CreatingPartyProcedure.execute(context,
													context.getSource().getPlayerOrException());
											return 1;
										}))))
				.then(Commands.literal("Join")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(context -> {
									JoiningPartyProcedure.execute(
											context.getSource().getUnsidedLevel(), context,
											context.getSource().getPlayerOrException());
									return 1;
								})
								.then(Commands.argument("pass", StringArgumentType.word())
										.executes(context -> {
											JoiningPartyProcedure.execute(
													context.getSource().getUnsidedLevel(), context,
													context.getSource().getPlayerOrException());
											return 1;
										}))))
				.then(Commands.literal("Leave").executes(context -> {
					PartyLeaveProcedure.execute(context.getSource().getUnsidedLevel(),
							context.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("Members").executes(context -> {
					PartyMembersProcedure.execute(context.getSource().getUnsidedLevel(),
							context.getSource().getPlayerOrException());
					return 1;
				}))
				.then(Commands.literal("Show").executes(context -> {
					PartyShowProcedure.execute(context.getSource().getPlayerOrException());
					return 1;
				})));
	}
}
