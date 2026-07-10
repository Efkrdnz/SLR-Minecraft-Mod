package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
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

public class InstanceDungeonKeyLoggerOnBlockRightClickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Player) {
			if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.INSTANCE_DUNGEON_KEY.get()) {
				{
					int _value = 1;
					BlockPos _pos = BlockPos.containing(x, y, z);
					BlockState _bs = world.getBlockState(_pos);
					if (_bs.getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp && _integerProp.getPossibleValues().contains(_value))
						world.setBlock(_pos, _bs.setValue(_integerProp, _value), 3);
				}
				if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).QuestProgression == 1
						&& ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MainQuest).equals("Getting Stronger")) {
					{
						double _setval = 2;
						entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.QuestProgression = _setval;
							capability.syncPlayerVariables(entity);
						});
					}
				}
				if (entity instanceof Player _player) {
					ItemStack _stktoremove = new ItemStack(SololevelingModItems.INSTANCE_DUNGEON_KEY.get());
					_player.getInventory().clearOrCountMatchingItems(p -> _stktoremove.getItem() == p.getItem(), 1, _player.inventoryMenu.getCraftSlots());
				}
				{
					double _setval = x;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.DunX = _setval;
						capability.instancecomplete = false;
						capability.tpd = false;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					double _setval = entity.getY();
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.DunY = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					double _setval = z;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.DunZ = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				entity.setNoGravity(true);
				{
					double _setval = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.randplayerx = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					double _setval = Mth.nextInt(RandomSource.create(), 60, 120);
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.randplayery = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					double _setval = Mth.nextInt(RandomSource.create(), -29999999, 29999999);
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.randplayerz = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				SololevelingMod.queueServerWork(10, () -> {
					if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
						ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_kasaka"));
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
								Entity _ent = entity;
								_ent.teleportTo(((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayerx),
										((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayery),
										((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayerz));
								if (_ent instanceof ServerPlayer _serverPlayer)
									_serverPlayer.connection.teleport(((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayerx),
											((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayery),
											((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).randplayerz), _ent.getYRot(), _ent.getXRot());
							}
							SololevelingMod.queueServerWork(10, () -> {
								{
									Entity _ent = entity;
									if (!_ent.level().isClientSide() && _ent.getServer() != null) {
										_ent.getServer().getCommands().performPrefixedCommand(
												new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4, _ent.getName().getString(),
														_ent.getDisplayName(), _ent.level().getServer(), _ent),
												"execute in sololeveling:dungeon_dimension_kasaka as @s at @s unless entity @e[type=sololeveling:portal_12,distance=..100] run spawninstance");
										SololevelingMod.queueServerWork(30, () -> DunKasakaTeleportAndSpawnProcedure.execute(_ent.level(), _ent));
									}
								}
							});
						});
					});
				});
			}
		}
	}
}
