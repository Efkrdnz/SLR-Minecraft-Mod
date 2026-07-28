package net.solocraft.procedures;

import net.solocraft.party.PartyService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

/** Legacy command adapter for party creation. */
public final class CreatingPartyProcedure {
	private CreatingPartyProcedure() {
	}

	public static void execute(CommandContext<CommandSourceStack> arguments, Entity entity) {
		if (entity instanceof ServerPlayer player)
			PartyService.legacyCreate(player, StringArgumentType.getString(arguments, "name"));
	}
}
