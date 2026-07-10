package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.BarukaEntity;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import java.util.ArrayList;

public class BarukaEntityDiesProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double uplvl = 0;
		if (entity instanceof BarukaEntity) {
			if (sourceentity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
				if (!((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
					if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow")))) {
						{
							Entity _ent = (sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null);
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
												_ent.getDisplayName(), _ent.level().getServer(), _ent),
										"/slnotify both &6&lBoss Slain | &eDungeon Cleared!");
							}
						}
						{
							Entity _ent = (sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null);
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
												_ent.getDisplayName(), _ent.level().getServer(), _ent),
										"/title @p clear");
							}
						}
						{
							boolean _setval = true;
							(sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.BossKilled = _setval;
								capability.syncPlayerVariables((sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null));
							});
						}
					} else {
						{
							Entity _ent = (sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null);
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
												_ent.getDisplayName(), _ent.level().getServer(), _ent),
										"/title @p title {\"text\":\"Baruka Defeated\",\"color\":\"#DF9607\",\"bold\":true,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
							}
						}
						{
							Entity _ent = (sourceentity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null);
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
												_ent.getDisplayName(), _ent.level().getServer(), _ent),
										"/title @p subtitle {\"text\":\"Leveled Up!\",\"color\":\"gold\",\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
							}
						}
					}
				}
			} else if (sourceentity instanceof Player) {
				if (((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")) {
					if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow")))) {
						{
							Entity _ent = sourceentity;
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
												_ent.getDisplayName(), _ent.level().getServer(), _ent),
										"/slnotify both &6&lBoss Slain | &eDungeon Cleared!");
							}
						}
						{
							Entity _ent = sourceentity;
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
												_ent.getDisplayName(), _ent.level().getServer(), _ent),
										"/title @p clear");
							}
						}
						{
							boolean _setval = true;
							sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.BossKilled = _setval;
								capability.syncPlayerVariables(sourceentity);
							});
						}
					} else {
						{
							Entity _ent = sourceentity;
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
												_ent.getDisplayName(), _ent.level().getServer(), _ent),
										"/title @p title {\"text\":\"Baruka Defeated\",\"color\":\"#DF9607\",\"bold\":true,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
							}
						}
						{
							Entity _ent = sourceentity;
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands().performPrefixedCommand(
										new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
												_ent.getDisplayName(), _ent.level().getServer(), _ent),
										"/title @p subtitle {\"text\":\"Leveled Up!\",\"color\":\"gold\",\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
							}
						}
					}
				} else {
					for (Entity entityiterator : new ArrayList<>(world.players())) {
						if (((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
								.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
							if ((entity.level().dimension()) == (entityiterator.level().dimension())) {
								if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow")))) {
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(
													new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
															_ent.getDisplayName(), _ent.level().getServer(), _ent),
													"/slnotify both &6&lBoss Slain | &eDungeon Cleared!");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(
													new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
															_ent.getDisplayName(), _ent.level().getServer(), _ent),
													"/title @p clear");
										}
									}
									{
										boolean _setval = true;
										entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
											capability.BossKilled = _setval;
											capability.syncPlayerVariables(entityiterator);
										});
									}
								} else {
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(
													new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
															_ent.getDisplayName(), _ent.level().getServer(), _ent),
													"/title @p title {\"text\":\"Baruka Defeated\",\"color\":\"#DF9607\",\"bold\":true,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
										}
									}
									{
										Entity _ent = entityiterator;
										if (!_ent.level().isClientSide() && _ent.getServer() != null) {
											_ent.getServer().getCommands().performPrefixedCommand(
													new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
															_ent.getDisplayName(), _ent.level().getServer(), _ent),
													"/title @p subtitle {\"text\":\"Leveled Up!\",\"color\":\"gold\",\"bold\":false,\"italic\":false,\"underlined\":false,\"strikethrough\":false,\"obfuscated\":false}");
										}
									}
								}
							}
						}
					}
				}
			}
			if ((entity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow")))) {
				if (world instanceof ServerLevel _level) {
					Entity entityToSpawn = SololevelingModEntities.PORTAL_12.get().spawn(_level, BlockPos.containing(x, y, z), MobSpawnType.MOB_SUMMONED);
					if (entityToSpawn != null) {
					}
				}
			}
		}
	}
}
