package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import java.util.List;
import java.util.Comparator;

public class DKCTravelRightclickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof ServerPlayer player) {
			if (!player.level().dimension().equals(Level.OVERWORLD)) {
				player.displayClientMessage(net.minecraft.network.chat.Component.literal("\u00A74The Demon King's Castle can only be entered from the Overworld."), true);
				return;
			}
			if (!DKCCombatTrackerProcedure.canEnterCastle(player)) {
				DKCCombatTrackerProcedure.sendCombatBlockedMessage(player);
				return;
			}
		}
		double rand = 0;
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(500 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))
						&& (entityiterator instanceof TamableAnimal _tamIsTamedBy && entity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false)) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
		}
		{
			double _setval = entity.getX();
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.DunX = _setval;
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
			double _setval = entity.getZ();
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.DunZ = _setval;
				capability.syncPlayerVariables(entity);
			});
		}
		entity.setNoGravity(true);
		SololevelingMod.queueServerWork(10, () -> {
			if (entity instanceof ServerPlayer _player && !_player.level().isClientSide()) {
				ResourceKey<Level> destinationType = ResourceKey.create(Registries.DIMENSION, new ResourceLocation("sololeveling:dungeon_dimension_dkc"));
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
			SololevelingMod.queueServerWork(5, () -> {
				{
					Entity _ent = entity;
					_ent.teleportTo(((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x + 100),
							((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y + 4),
							((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z + 4));
					if (_ent instanceof ServerPlayer _serverPlayer)
						_serverPlayer.connection.teleport(((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x + 100),
								((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y + 4),
								((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z + 4), _ent.getYRot(), _ent.getXRot());
				}
				SololevelingMod.queueServerWork(10, () -> {
					entity.setNoGravity(false);
					if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_started == false) {
						{
							boolean _setval = true;
							entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
								capability.dkc_started = _setval;
								capability.syncPlayerVariables(entity);
							});
						}
						{
							Entity _ent = entity;
							if (!_ent.level().isClientSide() && _ent.getServer() != null) {
								_ent.getServer().getCommands()
										.performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
												_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent),
												"execute in sololeveling:dungeon_dimension_dkc as @s at @s unless entity @e[type=sololeveling:portal_12,distance=..100] run spawndkc");
							}
						}
					}
				});
			});
		});
	}
}
