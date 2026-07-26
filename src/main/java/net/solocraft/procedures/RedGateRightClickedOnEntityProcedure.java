package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.RedGateEntity;
import net.solocraft.dungeon.runtime.SnowRedGateArenaManager;
import net.solocraft.util.MagicReadingHelper;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.tags.TagKey;
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

import java.util.List;
import java.util.Comparator;
import java.util.ArrayList;

public class RedGateRightClickedOnEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		if (!MagicReadingHelper.isHoldingMagicReader(sourceentity)) {
			if (net.solocraft.guild.GuildGateHelper.prepareGateEntry(world, entity, sourceentity))
				return;
			// The dedicated legacy red gate now shares the finite, instance-scoped wave
			// runtime used by procedural gates. Keep the generated procedure below as a
			// fallback for non-player callers and old integrations.
			if (entity instanceof RedGateEntity redGate && sourceentity instanceof Player) {
				if (!world.isClientSide() && sourceentity instanceof ServerPlayer serverPlayer)
					SnowRedGateArenaManager.enterLegacy(world, redGate, serverPlayer);
				return;
			}
			if (!(entity instanceof RedGateEntity _datEntL2 && _datEntL2.getEntityData().get(RedGateEntity.DATA_usedbefore))) {
				if (entity instanceof RedGateEntity _datEntSetL)
					_datEntSetL.getEntityData().set(RedGateEntity.DATA_usedbefore, true);
				if (entity instanceof RedGateEntity animatable)
					animatable.setTexture("21");
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(500 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))
								&& (entityiterator instanceof TamableAnimal _tamIsTamedBy && sourceentity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false)) {
							if (!entityiterator.level().isClientSide())
								entityiterator.discard();
						}
					}
				}
				if (((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")) {
					{
						double _setval = sourceentity.getX();
						sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.DunX = _setval;
							capability.syncPlayerVariables(sourceentity);
						});
					}
					{
						double _setval = sourceentity.getY();
						sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.DunY = _setval;
							capability.syncPlayerVariables(sourceentity);
						});
					}
					{
						double _setval = sourceentity.getZ();
						sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
							capability.DunZ = _setval;
							capability.syncPlayerVariables(sourceentity);
						});
					}
					SololevelingMod.queueServerWork(10, () -> {
						if (sourceentity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
							ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow"));
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
						sourceentity.getPersistentData().putString("dungeon_tag", (entity.getStringUUID()));
					});
				} else {
					for (Entity entityiterator : new ArrayList<>(world.players())) {
						if (((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
								.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
							if (Math.sqrt(Math.pow(entity.getX() - entityiterator.getX(), 2) + Math.pow(entity.getY() - entityiterator.getY(), 2) + Math.pow(entity.getZ() - entityiterator.getZ(), 2)) <= 10) {
								{
									double _setval = sourceentity.getX();
									entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.DunX = _setval;
										capability.syncPlayerVariables(entityiterator);
									});
								}
								{
									double _setval = sourceentity.getY();
									entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.DunY = _setval;
										capability.syncPlayerVariables(entityiterator);
									});
								}
								{
									double _setval = sourceentity.getZ();
									entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
										capability.DunZ = _setval;
										capability.syncPlayerVariables(entityiterator);
									});
								}
								SololevelingMod.queueServerWork(10, () -> {
									if (entityiterator instanceof ServerPlayer _player && !_player.level().isClientSide()) {
										ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_snow"));
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
									entityiterator.getPersistentData().putString("dungeon_tag", (entity.getStringUUID()));
								});
							}
						}
					}
				}
			}
		} else {
			MagicReadingHelper.showUnreadableReading(sourceentity);
		}
	}
}
