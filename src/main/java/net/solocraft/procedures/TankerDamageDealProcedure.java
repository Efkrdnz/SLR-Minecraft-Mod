package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModSounds;
import net.solocraft.entity.HunterEntity;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;

import org.joml.Vector3f;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class TankerDamageDealProcedure {
	private static final DustParticleOptions BLUE_SLASH_PARTICLE = new DustParticleOptions(new Vector3f(0.04F, 0.35F, 0.68F), 1.0F);
	private static final DustParticleOptions WHITE_SLASH_PARTICLE = new DustParticleOptions(new Vector3f(0.94F, 0.94F, 0.94F), 1.0F);

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
		double rand = 0;
		double radAngle = 0;
		double radYaw = 0;
		double radPitch = 0;
		double angle = 0;
		double y_pos = 0;
		double radius = 0;
		double rnk = 0;
		if (sourceentity instanceof HunterEntity) {
			if ((sourceentity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_HunterClass) : "").equals("Tanker")) {
				RandomSource random = RandomSource.create();
				rand = Mth.nextInt(random, 1, 6);
				if (((entity instanceof LivingEntity _entity) ? _entity.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof AxeItem
						|| (entity instanceof LivingEntity _entity ? _entity.getOffhandItem() : ItemStack.EMPTY).getItem() instanceof AxeItem
						|| ((entity instanceof LivingEntity _entity) ? _entity.getMainHandItem() : ItemStack.EMPTY).getItem() instanceof SwordItem
						|| (entity instanceof LivingEntity _entity ? _entity.getOffhandItem() : ItemStack.EMPTY).getItem() instanceof SwordItem) {
					if (rand == 1) {
						if (sourceentity instanceof LivingEntity _entity)
							_entity.swing(InteractionHand.MAIN_HAND, true);
						radius = Mth.nextDouble(random, 1.4, 2);
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
							if (entity.level() instanceof ServerLevel level)
								sendForcedDust(level, BLUE_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
						}
					} else if (rand == 2) {
						if (sourceentity instanceof LivingEntity _entity)
							_entity.swing(InteractionHand.MAIN_HAND, true);
						radius = Mth.nextDouble(random, 1.4, 2);
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
							if (entity.level() instanceof ServerLevel level)
								sendForcedDust(level, BLUE_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
						}
					} else if (rand == 3) {
						if (sourceentity instanceof LivingEntity _entity)
							_entity.swing(InteractionHand.MAIN_HAND, true);
						radius = Mth.nextDouble(random, 1.4, 2);
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
							if (entity.level() instanceof ServerLevel level)
								sendForcedDust(level, BLUE_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
						}
					} else if (rand == 4) {
						if (sourceentity instanceof LivingEntity _entity)
							_entity.swing(InteractionHand.MAIN_HAND, true);
						radius = Mth.nextDouble(random, 1.4, 2);
						hei = -2;
						speed = 5;
						particleNum = 30;
						arcAngle = 180;
						radYaw = Math.toRadians(sourceentity.getYRot() + 90);
						radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
						for (int index3 = 0; index3 < (int) particleNum; index3++) {
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
							if (entity.level() instanceof ServerLevel level)
								sendForcedDust(level, WHITE_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
						}
					} else if (rand == 5) {
						if (sourceentity instanceof LivingEntity _entity)
							_entity.swing(InteractionHand.MAIN_HAND, true);
						radius = Mth.nextDouble(random, 1.4, 2);
						hei = 2;
						speed = 5;
						particleNum = 30;
						arcAngle = 180;
						radYaw = Math.toRadians(sourceentity.getYRot() + 90);
						radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
						for (int index4 = 0; index4 < (int) particleNum; index4++) {
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
							if (entity.level() instanceof ServerLevel level)
								sendForcedDust(level, WHITE_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
						}
					} else {
						if (sourceentity instanceof LivingEntity _entity)
							_entity.swing(InteractionHand.MAIN_HAND, true);
						radius = Mth.nextDouble(random, 1.4, 2);
						hei = 0;
						speed = 5;
						particleNum = 30;
						arcAngle = 180;
						radYaw = Math.toRadians(sourceentity.getYRot() + 90);
						radPitch = Math.toRadians((sourceentity.getXRot() + 90) * (-1));
						for (int index5 = 0; index5 < (int) particleNum; index5++) {
							angle = i * (arcAngle / particleNum);
							radAngle = Math.toRadians(angle);
							vX = (Math.sin(radAngle) * Math.sin(radPitch) * Math.cos(radYaw) + Math.cos(radAngle) * Math.sin(radYaw)) * (-1);
							vY = Math.sin(radAngle) * Math.cos(radPitch);
							vZ = Math.sin(radAngle) * Math.sin(radPitch) * Math.sin(radYaw) * (-1) + Math.cos(radAngle) * Math.cos(radYaw);
							x_pos = sourceentity.getX() + radius * vX;
							y_pos = sourceentity.getY() + hei + radius * vY;
							z_pos = sourceentity.getZ() + radius * vZ;
							i = i + 1;
							if (entity.level() instanceof ServerLevel level)
								sendForcedDust(level, WHITE_SLASH_PARTICLE, x_pos, y_pos + 1.8, z_pos);
						}
					}
					playSwingSounds(world, sourceentity, random);
				} else {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, (y + 1.4), z, 2, 0.05, 0.05, 0.05, 1);
				}
			}
		}
		if (entity instanceof HunterEntity) {
			if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("S")) {
				rnk = 5;
			} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("A")) {
				rnk = 4;
			} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("B")) {
				rnk = 3;
			} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("C")) {
				rnk = 2;
			} else if ((entity instanceof HunterEntity _datEntS ? _datEntS.getEntityData().get(HunterEntity.DATA_Rank) : "").equals("D")) {
				rnk = 1;
			}
			if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("dm")))) {
				if (sourceentity.getPersistentData().getDouble("Level") > 0) {
					if (Math.random() < (5 * rnk) / ((float) sourceentity.getPersistentData().getDouble("Level"))) {
						if (event != null && event.isCancelable()) {
							event.setCanceled(true);
						}
						entity.setDeltaMovement(new Vec3((Mth.nextDouble(RandomSource.create(), 0.5, 1.2)), 0, (Mth.nextDouble(RandomSource.create(), 0.5, 1.2))));
						if (world instanceof ServerLevel _level)
							_level.sendParticles(ParticleTypes.LARGE_SMOKE, x, (y + 1.4), z, 12, 0.05, 0.05, 0.05, 1);
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
							if (Math.random() < (5 * rnk) / ((float) (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Level + 15)) {
								if (event != null && event.isCancelable()) {
									event.setCanceled(true);
								}
								if (world instanceof ServerLevel _level)
									_level.sendParticles(ParticleTypes.LARGE_SMOKE, x, (y + 1.4), z, 12, 0.05, 0.05, 0.05, 1);
								entity.setDeltaMovement(new Vec3((Mth.nextDouble(RandomSource.create(), 0.5, 1.2)), 0, (Mth.nextDouble(RandomSource.create(), 0.5, 1.2))));
								if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
									_entity.setTarget(_ent);
							}
						} else {
							if (Math.random() < (rnk) / ((float) (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).HunterRank + 6)) {
								if (event != null && event.isCancelable()) {
									event.setCanceled(true);
								}
								if (world instanceof ServerLevel _level)
									_level.sendParticles(ParticleTypes.LARGE_SMOKE, x, (y + 1.4), z, 12, 0.05, 0.05, 0.05, 1);
								entity.setDeltaMovement(new Vec3((Mth.nextDouble(RandomSource.create(), 0.5, 1.2)), 0, (Mth.nextDouble(RandomSource.create(), 0.5, 1.2))));
								if (entity instanceof Mob _entity && sourceentity instanceof LivingEntity _ent)
									_entity.setTarget(_ent);
							}
						}
					}
				}
			}
		}
	}

	private static void sendForcedDust(ServerLevel level, DustParticleOptions particle, double x, double y, double z) {
		for (ServerPlayer viewer : level.players())
			level.sendParticles(viewer, particle, true, x, y, z, 1, 0.0, 0.0, 0.0, 0.1);
	}

	private static void playSwingSounds(LevelAccessor world, Entity sourceentity, RandomSource random) {
		if (!(world instanceof Level level))
			return;
		float slashPitch = (float) Mth.nextDouble(random, 0.7, 2.0);
		float sweepPitch = (float) Mth.nextDouble(random, 0.7, 1.2);
		if (!level.isClientSide()) {
			BlockPos sourcePos = BlockPos.containing(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ());
			level.playSound(null, sourcePos, SololevelingModSounds.SLASH.get(), SoundSource.NEUTRAL, 0.3F, slashPitch);
			level.playSound(null, sourcePos, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.5F, sweepPitch);
		} else {
			level.playLocalSound(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ(), SololevelingModSounds.SLASH.get(), SoundSource.NEUTRAL, 0.3F, slashPitch, false);
			level.playLocalSound(sourceentity.getX(), sourceentity.getY(), sourceentity.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.NEUTRAL, 0.5F, sweepPitch, false);
		}
	}
}
