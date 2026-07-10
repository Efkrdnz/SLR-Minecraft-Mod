package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.JobChangeQuestManager;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;

public class DungeonDimensionPlayerLeavesDimensionProcedure {
	private static final double RETURN_X_OFFSET = 3.0D;

	public static void execute(LevelAccessor world, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!((sourceentity.level().dimension()) == Level.OVERWORLD)) {
			if ((sourceentity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_kasaka")))
					|| (sourceentity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_igris")))) {
				boolean isIgrisDungeon = (sourceentity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_igris")));
				boolean canLeave = isIgrisDungeon ? JobChangeQuestManager.isFinished(sourceentity) : (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).instancecomplete == true;
				if (canLeave) {
					if (!entity.level().isClientSide())
						entity.discard();
					sourceentity.getPersistentData().putString("dungeon_tag", (entity.getStringUUID()));
					if (sourceentity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
						ResourceKey<Level> destinationType = Level.OVERWORLD;
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
					SololevelingMod.queueServerWork(2, () -> {
						{
							Entity _ent = sourceentity;
							_ent.teleportTo(((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunX + RETURN_X_OFFSET),
									((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunY),
									((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunZ));
							if (_ent instanceof ServerPlayer _serverPlayer)
								_serverPlayer.connection.teleport(((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunX + RETURN_X_OFFSET),
										((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunY),
										((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunZ), _ent.getYRot(), _ent.getXRot());
						}
						{
							boolean _setval = false;
							sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.instancecomplete = _setval;
								capability.syncPlayerVariables(sourceentity);
							});
						}
					});
				} else {
					if (sourceentity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal(isIgrisDungeon ? "You cant leave the Job Change Quest before it is complete" : "You cant leave before you complete the dungeon"), true);
				}
			} else if ((sourceentity.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow")))) {
				if (!(sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).BossKilled) {
					if (sourceentity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("You cant leave a red gate before you defeat the boss"), true);
					return;
				}
				if (sourceentity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
					ResourceKey<Level> destinationType = Level.OVERWORLD;
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
				for (Entity entityiterator : new ArrayList<>(world.players())) {
					if ((entityiterator.level().dimension()) == (ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow")))) {
						if (entityiterator instanceof ServerPlayer _player && !_player.level().isClientSide()) {
							ResourceKey<Level> destinationType = Level.OVERWORLD;
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
						entityiterator.getPersistentData().putString("dungeon_tag", "");
						entityiterator.getPersistentData().putBoolean("slr_procedural_dungeon", false);
						entityiterator.getPersistentData().putBoolean("slr_procedural_red_gate", false);
						SololevelingMod.queueServerWork(2, () -> {
							{
								Entity _ent = entityiterator;
								_ent.teleportTo(((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunX + RETURN_X_OFFSET),
										((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunY),
										((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunZ));
								if (_ent instanceof ServerPlayer _serverPlayer)
									_serverPlayer.connection.teleport(((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunX + RETURN_X_OFFSET),
											((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunY),
											((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunZ), _ent.getYRot(), _ent.getXRot());
							}
							resetDungeonReturnState(entityiterator);
						});
					}
				}
			} else if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dungeoning) {
				boolean proceduralRedGate = sourceentity.getPersistentData().getBoolean("slr_procedural_red_gate");
				if (proceduralRedGate && !(sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).BossKilled) {
					if (sourceentity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("You cant leave a red gate before you defeat the boss"), true);
					return;
				}
				String dungeonTag = currentDungeonTag(sourceentity, entity);
				if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).BossKilled)
					markGateCleared(world, dungeonTag);
				if (sourceentity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
					ResourceKey<Level> destinationType = Level.OVERWORLD;
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
				if (!dungeonTag.isEmpty())
					sourceentity.getPersistentData().putString("dungeon_tag", dungeonTag);
				sourceentity.getPersistentData().putBoolean("slr_procedural_dungeon", false);
				sourceentity.getPersistentData().putBoolean("slr_procedural_red_gate", false);
				SololevelingMod.queueServerWork(2, () -> {
					{
						Entity _ent = sourceentity;
						_ent.teleportTo(((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunX + RETURN_X_OFFSET),
								((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunY),
								((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunZ));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport(((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunX + RETURN_X_OFFSET),
									((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunY),
								((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).DunZ), _ent.getYRot(), _ent.getXRot());
					}
					resetDungeonReturnState(sourceentity);
				});
			}
		}
	}

	private static String currentDungeonTag(Entity player, Entity returnPortal) {
		String tag = player.getPersistentData().getString("dungeon_tag");
		if (tag == null || tag.isEmpty())
			tag = returnPortal.getPersistentData().getString("dungeon_tag");
		return tag == null ? "" : tag;
	}

	private static void markGateCleared(LevelAccessor world, String dungeonTag) {
		if (dungeonTag == null || dungeonTag.isEmpty())
			return;
		String token = dungeonTag + ",";
		if (!SololevelingModVariables.MapVariables.get(world).GatesCleared.contains(token)) {
			SololevelingModVariables.MapVariables.get(world).GatesCleared = SololevelingModVariables.MapVariables.get(world).GatesCleared + token;
			SololevelingModVariables.MapVariables.get(world).syncData(world);
		}
	}

	private static void resetDungeonReturnState(Entity entity) {
		entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.BossKilled = false;
			capability.dungeoning = false;
			capability.syncPlayerVariables(entity);
		});
	}
}
