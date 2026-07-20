package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.VesselManager;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class SLRResetProcedure {
	public static void execute(LevelAccessor world, CommandContext<CommandSourceStack> arguments) {
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				if (entityiterator instanceof net.minecraft.server.level.ServerPlayer serverPlayer)
					VesselManager.resetPlayer(serverPlayer);
				if ((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).JOB == 1) {
					if (SololevelingModVariables.MapVariables.get(world).shmlimit > 0) {
						SololevelingModVariables.MapVariables.get(world).shmlimit = SololevelingModVariables.MapVariables.get(world).shmlimit - 1;
						SololevelingModVariables.MapVariables.get(world).syncData(world);
					}
					{
						double _setval = 0;
						entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.JOB = _setval;
							capability.syncPlayerVariables(entityiterator);
						});
					}
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Vitality = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Strength = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Intelligence = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Speed = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Durability = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Xp = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Level = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.prevLevel = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 10;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MaxXP = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.SkillPoints = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.shadowstorageusage = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 10;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.shadowstorage = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.polarbear = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.polarbearmax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.summonlimit = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.shadowdragonnum = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.shadowdragonmax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.tuskmax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.tuskspawned = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				if (entityiterator instanceof LivingEntity _entity)
					_entity.setHealth(20);
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.MainQuest = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Dialogue = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = ".";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Plist = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Pslot1 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Pslot2 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Pslot3 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Pslot4 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Pslot5 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Pslot6 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Pslot7 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Pslot8 = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = "";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.PselectedPower = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = ".";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ExchangeCords = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					String _setval = ".";
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ExchangeDimensions = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					boolean _setval = false;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.combatmode = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					boolean _setval = false;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ShadowBody = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					boolean _setval = false;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ShadowExchange = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.JOB = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.statshown = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.GuildCode = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.xpmultiplier = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ordshadowmax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.OrdShadow = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.igris = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.IgrisSpawned = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.GobShadow = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.GobShadowMax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.WolfShadow = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.WolfShadowMax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.orcmax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.berumax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.beru = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.orcspawned = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.golds = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.HunterRank = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.prevRank = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.perception = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.Classes = _setval;
						capability.mageSpecialization = "";
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.statshown = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.squat = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.situp = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.pushup = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.RUN = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.shadowdragonnum = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.shadowdragonmax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ShadowGoblinMageMax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ShadowGoblinArcherMax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ShadowGoblinMageAmount = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.ShadowGoblinArcherAmount = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.tuskmax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.tuskspawned = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.highorcmax = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 0;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.highorcspawned = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_multiplier_assassin = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_multiplier_mage = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_multiplier_fighter = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_multiplier_tanker = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_multiplier_healer = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_multiplier_ranger = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_assassin = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_mage = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_fighter = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_tanker = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_healer = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				{
					double _setval = 1;
					entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.progression_ranger = _setval;
						capability.syncPlayerVariables(entityiterator);
					});
				}
				if (entityiterator instanceof LivingEntity _entity)
					_entity.removeAllEffects();
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
	}
}
