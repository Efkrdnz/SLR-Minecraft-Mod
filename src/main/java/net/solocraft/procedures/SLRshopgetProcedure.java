package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class SLRshopgetProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		int slot = (int) DoubleArgumentType.getDouble(arguments, "amount") - 1;
		try {
			for (Entity target : EntityArgument.getEntities(arguments, "name")) {
				if (!(target instanceof Player player))
					continue;
				SololevelingModVariables.PlayerVariables variables = target.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables());
				ItemStack stored = switch (slot) {
					case 0 -> variables.shopitem1;
					case 1 -> variables.shopitem2;
					case 2 -> variables.shopitem3;
					case 3 -> variables.shopitem4;
					case 4 -> variables.shopitem5;
					case 5 -> variables.shopitem6;
					default -> ItemStack.EMPTY;
				};
				if (!stored.isEmpty()) {
					ItemStack granted = stored.copy();
					granted.setCount(1);
					ItemHandlerHelper.giveItemToPlayer(player, granted);
				}
			}
		} catch (CommandSyntaxException exception) {
			exception.printStackTrace();
		}
	}
}
