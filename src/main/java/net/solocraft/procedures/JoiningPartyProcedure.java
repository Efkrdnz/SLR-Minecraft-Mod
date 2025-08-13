package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;

public class JoiningPartyProcedure {
	public static void execute(LevelAccessor world, CommandContext<CommandSourceStack> arguments, Entity entity) {
		if (entity == null)
			return;
		boolean found = false;
		double i = 0;
		double found_index = 0;
		i = 0;
		for (int index0 = 0; index0 < (int) SololevelingModVariables.parties.size(); index0++) {
			if ((SololevelingModVariables.parties.get((int) i) instanceof String _s ? _s : "").equals((StringArgumentType.getString(arguments, "name")).toUpperCase())) {
				found = true;
				found_index = i;
				break;
			} else {
				i = i + 1;
			}
		}
		if (found) {
			if ((SololevelingModVariables.partypassword.get((int) found_index) instanceof String _s ? _s : "").equals(StringArgumentType.getString(arguments, "pass"))) {
				{
					String _setval = (StringArgumentType.getString(arguments, "name")).toUpperCase();
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.party = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				for (Entity entityiterator : new ArrayList<>(world.players())) {
					if (!(entity == entityiterator)) {
						if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")
								&& ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
										.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
							if (entityiterator instanceof Player _player && !_player.level().isClientSide())
								_player.displayClientMessage(Component.literal(("\u00A7e" + entity.getDisplayName().getString() + " \u00A7fhave joined the party!")), false);
						}
					}
				}
			}
		}
	}
}
