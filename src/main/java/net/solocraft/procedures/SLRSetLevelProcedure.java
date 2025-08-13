package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.DoubleArgumentType;

public class SLRSetLevelProcedure {
	public static void execute(CommandContext<CommandSourceStack> arguments) {
		double uplvl = 0;
		double diff = 0;
		double diff2 = 0;
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				uplvl = DoubleArgumentType.getDouble(arguments, "amount");
				diff = DoubleArgumentType.getDouble(arguments, "amount") - (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level;
				diff2 = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level - DoubleArgumentType.getDouble(arguments, "amount");
				if (DoubleArgumentType.getDouble(arguments, "amount") > (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level) {
					{
						double _setval = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level + diff;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Level = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Vitality + diff;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Vitality = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength + diff;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Strength = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Intelligence + diff;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Intelligence = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Speed + diff;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Speed = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).perception + diff;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.perception = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				} else if (DoubleArgumentType.getDouble(arguments, "amount") < (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level) {
					{
						double _setval = uplvl;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Level = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = uplvl;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Vitality = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = uplvl;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Strength = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = uplvl;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Intelligence = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = uplvl;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.Speed = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
					{
						double _setval = uplvl;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.perception = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				}
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
	}
}
