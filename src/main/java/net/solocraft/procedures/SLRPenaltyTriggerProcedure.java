package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class SLRPenaltyTriggerProcedure {
	public static void execute(LevelAccessor world, CommandContext<CommandSourceStack> arguments) {
		if (!world.isClientSide()) {
			try {
				for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
					boolean keepSecret = DailyQuestHelper.isSecretQuest(entityiterator)
							|| ((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level >= 30
									&& (entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dailysecrettrans != 0);
					DailyQuestHelper.sendQuestFailedChat(entityiterator);
					DailyQuestHelper.resetDailyProgress(entityiterator);
					if (keepSecret)
						DailyQuestHelper.keepSecretQuestPending(entityiterator);
				}
			} catch (CommandSyntaxException e) {
				e.printStackTrace();
			}
		}
	}
}
