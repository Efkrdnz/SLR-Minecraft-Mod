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
import net.minecraft.commands.CommandFunction;

import java.util.Optional;

public class SwordOfLightGiveProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 1200) {
			if (!(entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.SO_LCOOLDOWN.get()))) {
				{
					double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP - 1200;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MP = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.SO_LCOOLDOWN.get(), 400, 1, false, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.MANA_REFLESH_COOLDOWN.get(), 50, 1, false, false));
				if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
					_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.SWORD_OF_LIGHT.get(), 220, 1, false, false));
				if (world.getLevelData().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
					world.getLevelData().getGameRules().getRule(GameRules.RULE_SENDCOMMANDFEEDBACK).set(false, world.getServer());
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("sololeveling:yellow_lightning_1"));
							if (_fopt.isPresent())
								_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
						}
					}
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("sololeveling:yellow_lightning_2"));
							if (_fopt.isPresent())
								_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
						}
					}
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("sololeveling:yellow_lightning_3"));
							if (_fopt.isPresent())
								_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
						}
					}
					world.getLevelData().getGameRules().getRule(GameRules.RULE_SENDCOMMANDFEEDBACK).set(true, world.getServer());
				} else {
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("sololeveling:yellow_lightning_1"));
							if (_fopt.isPresent())
								_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
						}
					}
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("sololeveling:yellow_lightning_2"));
							if (_fopt.isPresent())
								_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
						}
					}
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							Optional<CommandFunction> _fopt = _ent.getServer().getFunctions().get(new ResourceLocation("sololeveling:yellow_lightning_3"));
							if (_fopt.isPresent())
								_ent.getServer().getFunctions().execute(_fopt.get(), _ent.createCommandSourceStack());
						}
					}
				}
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("Not enough MP!"), true);
		}
	}
}
