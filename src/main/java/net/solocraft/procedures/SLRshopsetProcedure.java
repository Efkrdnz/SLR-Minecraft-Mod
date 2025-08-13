package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.DoubleArgumentType;

public class SLRshopsetProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments, Entity entity) {
		if (entity == null)
			return;
		double slot = 0;
		slot = DoubleArgumentType.getDouble(arguments, "amount") - 1;
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				if ((ItemArgument.getItem(arguments, "item").getItem().getDefaultInstance()).is(ItemTags.create(new ResourceLocation("forge:shop_items")))) {
					if (slot == 0) {
						{
							ItemStack _setval = (ItemArgument.getItem(arguments, "item").getItem().getDefaultInstance());
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.shopitem1 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else if (slot == 1) {
						{
							ItemStack _setval = (ItemArgument.getItem(arguments, "item").getItem().getDefaultInstance());
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.shopitem2 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else if (slot == 2) {
						{
							ItemStack _setval = (ItemArgument.getItem(arguments, "item").getItem().getDefaultInstance());
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.shopitem3 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else if (slot == 3) {
						{
							ItemStack _setval = (ItemArgument.getItem(arguments, "item").getItem().getDefaultInstance());
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.shopitem4 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else if (slot == 4) {
						{
							ItemStack _setval = (ItemArgument.getItem(arguments, "item").getItem().getDefaultInstance());
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.shopitem5 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					} else if (slot == 5) {
						{
							ItemStack _setval = (ItemArgument.getItem(arguments, "item").getItem().getDefaultInstance());
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.shopitem6 = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
					}
				} else {
					if (entityiterator instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("This item cannot be added into the shop"), false);
				}
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
	}
}
