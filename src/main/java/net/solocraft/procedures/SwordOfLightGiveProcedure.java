package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.functions.CommandFunction;

import java.util.Optional;
import net.solocraft.util.CooldownManager;

public class SwordOfLightGiveProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 1200) {
			if (!CooldownManager.isOnCooldown(entity, "Sword of Light")) {
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 1200;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MP = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				CooldownManager.set(entity, "Sword of Light", 400);
				CooldownManager.set(entity, "mana_refresh", 50);
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.SWORD_OF_LIGHT, 220, 1, false, false));
				// The previous implementation flipped the global
				// sendCommandFeedback gamerule off, ran these functions, then
				// flipped it back. Any throw between those points left command
				// feedback disabled server-wide, permanently. Suppressing output
				// on this one command source achieves the same silence without
				// touching shared world state.
				runSilently(entity, "sololeveling:yellow_lightning_1");
				runSilently(entity, "sololeveling:yellow_lightning_2");
				runSilently(entity, "sololeveling:yellow_lightning_3");
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("Not enough MP!"), true);
		}
	}

	/** Runs one VFX function with output suppressed on its own source stack. */
	private static void runSilently(Entity entity, String functionId) {
		if (entity.level().isClientSide() || entity.getServer() == null)
			return;
		Optional<CommandFunction<CommandSourceStack>> function =
				entity.getServer().getFunctions().get(ResourceLocation.parse(functionId));
		if (function.isEmpty())
			return;
		entity.getServer().getFunctions().execute(function.get(),
				entity.createCommandSourceStack().withSuppressedOutput());
	}
}
