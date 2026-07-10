package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.entity.PortalBeruEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import java.util.List;
import java.util.Comparator;

public class PortalBeruPlayerCollidesWithThisEntityProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double rand = 0;
		if (!((sourceentity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).getItem() == SololevelingModItems.MAGIC_READER.get())) {
			if (net.solocraft.guild.GuildGateHelper.prepareGateEntry(world, entity, sourceentity))
				return;
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
			if (!world.isClientSide()) {
				sourceentity.getPersistentData().putDouble("tpx", (entity.getPersistentData().getDouble("tpx")));
				sourceentity.getPersistentData().putDouble("tpx", (entity.getPersistentData().getDouble("tpx")));
				sourceentity.getPersistentData().putDouble("tpx", (entity.getPersistentData().getDouble("tpx")));
			}
			sourceentity.setNoGravity(true);
			SololevelingMod.queueServerWork(10, () -> {
				if (sourceentity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
					ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_s"));
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
				net.solocraft.util.UrgentQuestManager.markDungeonId(sourceentity, "ant_island");
				SololevelingMod.queueServerWork(5, () -> {
					{
						Entity _ent = sourceentity;
						_ent.teleportTo((entity.getPersistentData().getDouble("tpx")), (entity.getPersistentData().getDouble("tpy")), (entity.getPersistentData().getDouble("tpz")));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport((entity.getPersistentData().getDouble("tpx")), (entity.getPersistentData().getDouble("tpy")), (entity.getPersistentData().getDouble("tpz")), _ent.getYRot(), _ent.getXRot());
					}
					sourceentity.getPersistentData().putString("dungeon_tag", (entity.getStringUUID()));
					SololevelingMod.queueServerWork(10, () -> {
						if ((entity instanceof PortalBeruEntity _datEntL26 && _datEntL26.getEntityData().get(PortalBeruEntity.DATA_usedbefore)) == false) {
							if (entity instanceof PortalBeruEntity _datEntSetL)
								_datEntSetL.getEntityData().set(PortalBeruEntity.DATA_usedbefore, true);
							{
								Entity _ent = sourceentity;
								if (!_ent.level().isClientSide() && _ent.getServer() != null) {
									_ent.getServer().getCommands()
											.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
													_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent),
													"execute in sololeveling:dungeon_dimension_s as @s at @s unless entity @e[type=sololeveling:portal_12,distance=..100] run spawnberu");
								}
							}
							sourceentity.getPersistentData().putString("dungeon_tag", (entity.getStringUUID()));
						}
					});
				});
			});
		} else {
			rand = Mth.nextInt(RandomSource.create(), 1, 4);
			if (rand == 1) {
				if (sourceentity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("Magic Reading: 9999"), false);
			} else if (rand == 2) {
				if (sourceentity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("Magic Reading: ERROR"), false);
			} else if (rand == 3) {
				if (sourceentity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("Magic Reading: N/A"), false);
			} else if (rand == 4) {
				if (sourceentity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal("Magic Reading: Cannot Read!"), false);
			}
		}
	}
}
