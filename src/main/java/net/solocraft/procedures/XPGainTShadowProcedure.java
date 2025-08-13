package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.entity.StoneGolemEntity;
import net.solocraft.entity.SteelFangWolfEntity;
import net.solocraft.entity.RedAntsEntity;
import net.solocraft.entity.PolarBearEntity;
import net.solocraft.entity.OrcEntity;
import net.solocraft.entity.MutatedEntity;
import net.solocraft.entity.MiniGemGolemEntity;
import net.solocraft.entity.IceElfEntity;
import net.solocraft.entity.GoblinMageEntity;
import net.solocraft.entity.GoblinClubEntity;
import net.solocraft.entity.GoblinArcherEntity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.CentipedeEntity;
import net.solocraft.entity.AncientSamuraiEntity;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class XPGainTShadowProcedure {
	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event != null && event.getEntity() != null) {
			execute(event, event.getEntity().level(), event.getEntity(), event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		execute(null, world, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (sourceentity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
			if (((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Player) {
				if (!(entity instanceof Animal)) {
					if (entity instanceof AncientSamuraiEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 50;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof GoblinArcherEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 5;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof GoblinMageEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 5;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof CentipedeEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 100;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof DKnight1Entity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 20;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof DKnight2Entity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 20;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof DKnight3Entity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 20;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof GoblinClubEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 5;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof IceElfEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 40;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof PolarBearEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 32;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof MiniGemGolemEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 35;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof StoneGolemEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 30;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof SteelFangWolfEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 7;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof OrcEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 12;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof RedAntsEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 60;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else if (entity instanceof MutatedEntity) {
						{
							double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
									.orElse(new SololevelingModVariables.PlayerVariables())).Xp
									+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
											.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 25;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.Xp = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else {
						if (!world.getLevelData().getGameRules().getBoolean(SololevelingModGameRules.SOLO_DUNGEON_PROGRESSION_ONLY)) {
							{
								double _setval = ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
										.orElse(new SololevelingModVariables.PlayerVariables())).Xp
										+ ((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
												.orElse(new SololevelingModVariables.PlayerVariables())).xpmultiplier * ((world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER)) / 10) * 4;
								(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
									capability.Xp = _setval;
									capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
								});
							}
						}
					}
				}
			}
		}
	}
}
