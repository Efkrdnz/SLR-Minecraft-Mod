package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.PortalJobChangeEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

public class PortalJobChangeRightClickedOnEntityProcedure {
	private static final double PLAYER_PORTAL_ENTRY_X_OFFSET = 3.0D;

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Player) {
			if ((entity instanceof PortalJobChangeEntity _datEntS ? _datEntS.getEntityData().get(PortalJobChangeEntity.DATA_person_to_enter) : "").equals(sourceentity.getStringUUID())) {
				{
					double _setval = entity.getX();
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.DunX = _setval;
						capability.tpd = false;
						capability.syncPlayerVariables(sourceentity);
					});
				}
				{
					double _setval = entity.getY();
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.DunY = _setval;
						capability.syncPlayerVariables(sourceentity);
					});
				}
				{
					double _setval = entity.getZ();
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.DunZ = _setval;
						capability.syncPlayerVariables(sourceentity);
					});
				}
				sourceentity.setNoGravity(true);
				{
					double _setval = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.randplayerx = _setval;
						capability.syncPlayerVariables(sourceentity);
					});
				}
				{
					double _setval = Mth.nextInt(RandomSource.create(), 60, 120);
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.randplayery = _setval;
						capability.syncPlayerVariables(sourceentity);
					});
				}
				{
					double _setval = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
					sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.randplayerz = _setval;
						capability.syncPlayerVariables(sourceentity);
					});
				}
				SololevelingMod.queueServerWork(10, () -> {
					if (sourceentity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
						ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_igris"));
						if (_player.level().dimension() == destinationType)
							return;
						ServerLevel nextLevel = _player.server.getLevel(destinationType);
						if (nextLevel != null) {
							_player.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, 0));
							_player.teleportTo(nextLevel, _player.getX(), _player.getY(), _player.getZ(), _player.getYRot(), _player.getXRot());
							_player.connection.send(new ClientboundPlayerAbilitiesPacket(_player.getAbilities()));
							for (MobEffectInstance _effectinstance : _player.getActiveEffects())
								_player.connection.send(new ClientboundUpdateMobEffectPacket(_player.getId(), _effectinstance));
							_player.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
						}
					}
					SololevelingMod.queueServerWork(60, () -> {
						SololevelingMod.queueServerWork(10, () -> {
							{
								Entity _ent = sourceentity;
								_ent.teleportTo(((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayerx + PLAYER_PORTAL_ENTRY_X_OFFSET),
										((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayery),
										((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayerz));
								if (_ent instanceof ServerPlayer _serverPlayer)
									_serverPlayer.connection.teleport(((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayerx + PLAYER_PORTAL_ENTRY_X_OFFSET),
											((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayery),
											((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayerz), _ent.getYRot(), _ent.getXRot());
							}
							SololevelingMod.queueServerWork(10, () -> {
								{
									Entity _ent = sourceentity;
									if (!_ent.level().isClientSide() && _ent.getServer() != null) {
										_ent.getServer().getCommands().performPrefixedCommand(
												new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
														_ent.getDisplayName(), _ent.level().getServer(), _ent),
												"execute in sololeveling:dungeon_dimension_igris as @s at @s unless entity @e[type=sololeveling:portal_12,distance=..100] run spawnigris");
									}
								}
							});
						});
					});
				});
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
	}
}
