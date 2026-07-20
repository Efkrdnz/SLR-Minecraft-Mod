package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public class SLRshopsetProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		int slot = (int) DoubleArgumentType.getDouble(arguments, "amount") - 1;
		ItemStack selected = ItemArgument.getItem(arguments, "item").getItem().getDefaultInstance();
		try {
			for (Entity target : EntityArgument.getEntities(arguments, "name")) {
				if (!selected.is(ItemTags.create(new ResourceLocation("forge:shop_items")))) {
					if (target instanceof Player player && !player.level().isClientSide())
						player.displayClientMessage(Component.literal("This item cannot be added into the shop"), false);
					continue;
				}
				target.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					ItemStack value = selected.copy();
					switch (slot) {
						case 0 -> capability.shopitem1 = value;
						case 1 -> capability.shopitem2 = value;
						case 2 -> capability.shopitem3 = value;
						case 3 -> capability.shopitem4 = value;
						case 4 -> capability.shopitem5 = value;
						case 5 -> capability.shopitem6 = value;
						default -> {
							return;
						}
					}
					capability.syncPlayerVariables(target);
				});
			}
		} catch (CommandSyntaxException exception) {
			exception.printStackTrace();
		}
	}
}
