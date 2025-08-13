package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.entity.ChaHaeInEntity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class SwingRandomChaProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingAttackEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getY(), entity.getZ(), event.getSource(), entity, event.getSource().getEntity());
		}
	}

	public static void execute(LevelAccessor world, double x, double y, double z, DamageSource damagesource, Entity entity, Entity sourceentity) {
		execute(null, world, x, y, z, damagesource, entity, sourceentity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, double x, double y, double z, DamageSource damagesource, Entity entity, Entity sourceentity) {
		if (damagesource == null || entity == null || sourceentity == null)
			return;
		double rand = 0;
		double particleNum = 0;
		double vX = 0;
		double vY = 0;
		double vZ = 0;
		double i = 0;
		double x_pos = 0;
		double z_pos = 0;
		double hei = 0;
		double speed = 0;
		double arcAngle = 0;
		double radAngle = 0;
		double radYaw = 0;
		double radPitch = 0;
		double angle = 0;
		double y_pos = 0;
		double radius = 0;
		if (sourceentity instanceof ChaHaeInEntity) {
			rand = Mth.nextInt(RandomSource.create(), 1, 3);
			if (rand == 1) {
				if (sourceentity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				radius = 2.3;
				hei = -2;
				speed = 5;
				particleNum = 30;
				arcAngle = 180;
				radYaw = Math.toRadians(sourceentity.getYRot() + 90);
				radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
				for (int index0 = 0; index0 < (int) particleNum; index0++) {
					angle = i * (arcAngle / particleNum);
					radAngle = Math.toRadians(angle);
					vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
					vY = Math.sin(radAngle) * Math.cos(radPitch);
					vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
					x_pos = sourceentity.getX() + radius * vX;
					y_pos = sourceentity.getY() + hei + radius * vY;
					z_pos = sourceentity.getZ() + radius * vZ;
					i = i + 1;
					hei = hei + 0.133;
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), ("/particle dust 0.99 0.79 0 1 " + x_pos + " " + (y_pos + 1.8) + " " + z_pos + " 0 0 0 0.1 1 force"));
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL,
									(float) 0.3, (float) Mth.nextDouble(RandomSource.create(), 0.7, 2));
						} else {
							_level.playLocalSound((sourceentity.getX()), (sourceentity.getY()), (sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.3,
									(float) Mth.nextDouble(RandomSource.create(), 0.7, 2), false);
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL,
									(float) 0.5, (float) Mth.nextDouble(RandomSource.create(), 0.7, 1.2));
						} else {
							_level.playLocalSound((sourceentity.getX()), (sourceentity.getY()), (sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL, (float) 0.5,
									(float) Mth.nextDouble(RandomSource.create(), 0.7, 1.2), false);
						}
					}
				}
			} else if (rand == 2) {
				if (sourceentity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				radius = 2.3;
				hei = 2;
				speed = 5;
				particleNum = 30;
				arcAngle = 180;
				radYaw = Math.toRadians(sourceentity.getYRot() + 90);
				radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
				for (int index1 = 0; index1 < (int) particleNum; index1++) {
					angle = i * (arcAngle / particleNum);
					radAngle = Math.toRadians(angle);
					vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
					vY = Math.sin(radAngle) * Math.cos(radPitch);
					vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
					x_pos = sourceentity.getX() + radius * vX;
					y_pos = sourceentity.getY() + hei + radius * vY;
					z_pos = sourceentity.getZ() + radius * vZ;
					i = i + 1;
					hei = hei - 0.133;
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), ("/particle dust 0.99 0.79 0 1 " + x_pos + " " + (y_pos + 1.8) + " " + z_pos + " 0 0 0 0.1 1 force"));
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL,
									(float) 0.3, (float) Mth.nextDouble(RandomSource.create(), 0.7, 2));
						} else {
							_level.playLocalSound((sourceentity.getX()), (sourceentity.getY()), (sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.3,
									(float) Mth.nextDouble(RandomSource.create(), 0.7, 2), false);
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL,
									(float) 0.5, (float) Mth.nextDouble(RandomSource.create(), 0.7, 1.2));
						} else {
							_level.playLocalSound((sourceentity.getX()), (sourceentity.getY()), (sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL, (float) 0.5,
									(float) Mth.nextDouble(RandomSource.create(), 0.7, 1.2), false);
						}
					}
				}
			} else {
				if (sourceentity instanceof LivingEntity _entity)
					_entity.swing(InteractionHand.MAIN_HAND, true);
				radius = 2.3;
				hei = 0;
				speed = 5;
				particleNum = 30;
				arcAngle = 180;
				radYaw = Math.toRadians(sourceentity.getYRot() + 90);
				radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
				for (int index2 = 0; index2 < (int) particleNum; index2++) {
					angle = i * (arcAngle / particleNum);
					radAngle = Math.toRadians(angle);
					vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
					vY = Math.sin(radAngle) * Math.cos(radPitch);
					vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
					x_pos = sourceentity.getX() + radius * vX;
					y_pos = sourceentity.getY() + hei + radius * vY;
					z_pos = sourceentity.getZ() + radius * vZ;
					i = i + 1;
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), ("/particle dust 0.99 0.79 0 1 " + x_pos + " " + (y_pos + 1.8) + " " + z_pos + " 0 0 0 0.1 1 force"));
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL,
									(float) 0.3, (float) Mth.nextDouble(RandomSource.create(), 0.7, 2));
						} else {
							_level.playLocalSound((sourceentity.getX()), (sourceentity.getY()), (sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.3,
									(float) Mth.nextDouble(RandomSource.create(), 0.7, 2), false);
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL,
									(float) 0.5, (float) Mth.nextDouble(RandomSource.create(), 0.7, 1.2));
						} else {
							_level.playLocalSound((sourceentity.getX()), (sourceentity.getY()), (sourceentity.getZ()), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.player.attack.sweep")), SoundSource.NEUTRAL, (float) 0.5,
									(float) Mth.nextDouble(RandomSource.create(), 0.7, 1.2), false);
						}
					}
				}
			}
		}
		if (entity instanceof ChaHaeInEntity) {
			if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("dm")))) {
				if (sourceentity.getPersistentData().getDouble("Level") > 0) {
					if (Math.random() < (20) / ((float) sourceentity.getPersistentData().getDouble("Level"))) {
						if (event != null && event.isCancelable()) {
							event.setCanceled(true);
						}
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, (y + 1.4), z, 35, 0.05, 0.05, 0.05, 1);
						if (world instanceof Level _level) {
							if (!_level.isClientSide()) {
								_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:swordclash")), SoundSource.NEUTRAL, (float) 0.5, (float) 1.2);
							} else {
								_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:swordclash")), SoundSource.NEUTRAL, (float) 0.5, (float) 1.2, false);
							}
						}
						if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
							_entity.setTarget(_ent);
					}
				}
			} else if (sourceentity instanceof Player) {
				if (new Object() {
					public boolean checkGamemode(Entity _ent) {
						if (_ent instanceof ServerPlayer _serverPlayer) {
							return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL;
						} else if (_ent.level().isClientSide() && _ent instanceof Player _player) {
							return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
									&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.SURVIVAL;
						}
						return false;
					}
				}.checkGamemode(sourceentity) || new Object() {
					public boolean checkGamemode(Entity _ent) {
						if (_ent instanceof ServerPlayer _serverPlayer) {
							return _serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE;
						} else if (_ent.level().isClientSide() && _ent instanceof Player _player) {
							return Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()) != null
									&& Minecraft.getInstance().getConnection().getPlayerInfo(_player.getGameProfile().getId()).getGameMode() == GameType.ADVENTURE;
						}
						return false;
					}
				}.checkGamemode(sourceentity)) {
					if (!(damagesource).is(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:mage")))) {
						if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Player) {
							if (Math.random() < (15) / ((float) (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level)) {
								if (event != null && event.isCancelable()) {
									event.setCanceled(true);
								}
								if (world instanceof ServerLevel _level)
									_level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, (y + 1.4), z, 35, 0.05, 0.05, 0.05, 1);
								if (world instanceof Level _level) {
									if (!_level.isClientSide()) {
										_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:swordclash")), SoundSource.NEUTRAL, (float) 0.5, (float) 1.2);
									} else {
										_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:swordclash")), SoundSource.NEUTRAL, (float) 0.5, (float) 1.2, false);
									}
								}
								if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
									_entity.setTarget(_ent);
							}
						} else {
							if (Math.random() < (1) / ((float) (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank + 2)) {
								if (event != null && event.isCancelable()) {
									event.setCanceled(true);
								}
								if (world instanceof ServerLevel _level)
									_level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, (y + 1.4), z, 35, 0.05, 0.05, 0.05, 1);
								if (world instanceof Level _level) {
									if (!_level.isClientSide()) {
										_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:swordclash")), SoundSource.NEUTRAL, (float) 0.5, (float) 1.2);
									} else {
										_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:swordclash")), SoundSource.NEUTRAL, (float) 0.5, (float) 1.2, false);
									}
								}
								if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
									_entity.setTarget(_ent);
							}
						}
					}
				}
			}
		}
	}
}
