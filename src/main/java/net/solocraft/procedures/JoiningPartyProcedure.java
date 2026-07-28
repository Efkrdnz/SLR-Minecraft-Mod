package net.solocraft.procedures;

import net.solocraft.party.PartyService;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

/** Legacy command adapter that submits the same join request used by the GUI. */
public final class JoiningPartyProcedure {
	private JoiningPartyProcedure() {
	}

	public static void execute(LevelAccessor world, CommandContext<CommandSourceStack> arguments,
			Entity entity) {
		if (entity instanceof ServerPlayer player)
			PartyService.legacyRequestJoin(player,
					StringArgumentType.getString(arguments, "name"));
	}
}
