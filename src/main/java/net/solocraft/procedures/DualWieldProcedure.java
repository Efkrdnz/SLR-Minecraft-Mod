package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.entity.IceChunkEntity;
import net.solocraft.SololevelingMod;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import java.util.List;
import java.util.Comparator;

public class DualWieldProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		boolean CanRun = false;
		String ParticleAmount = "";
		String ParticleSpeed = "";
		String ParticleType = "";
		String ParticleMode = "";
		String dx = "";
		String dy = "";
		String dz = "";
		double motionZ = 0;
		double deltaZ = 0;
		double deltaX = 0;
		double motionY = 0;
		double Yspeed = 0;
		double deltaY = 0;
		double motionX = 0;
		double speed = 0;
		double DivAmountX = 0;
		double Spacing = 0;
		double AddDistanceY = 0;
		double DistanceX = 0;
		double AddDistanceX = 0;
		double BX = 0;
		double DistanceY = 0;
		double DivAmountY = 0;
		double AX = 0;
		double BY = 0;
		double DistanceZ = 0;
		double DivAmountZ = 0;
		double AddDistanceZ = 0;
		double AY = 0;
		double BZ = 0;
		double AZ = 0;
		double yaw = 0;
		double xpos = 0;
		double zpos = 0;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= 550) {
			if (!(entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.DUAL_WIELDING_COOLDOWN.get()))) {
				if ((entity instanceof LivingEntity _livEnt ? _livEnt.getMainHandItem() : ItemStack.EMPTY).is(ItemTags.create(new ResourceLocation("minecraft:dagger")))
						&& (entity instanceof LivingEntity _livEnt ? _livEnt.getOffhandItem() : ItemStack.EMPTY).is(ItemTags.create(new ResourceLocation("minecraft:dagger")))) {
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.DUAL_WIELDING_COOLDOWN.get(), 260, 1, false, false));
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.NO_FALL_DAMAGE.get(), 999, 1, false, false));
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 15, 99, false, false));
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 15, 1, false, false));
					xpos = entity.getX() + 4 * entity.getLookAngle().x;
					zpos = entity.getZ() + 4 * entity.getLookAngle().z;
					yaw = entity.getYRot();
					entity.getPersistentData().putDouble("SlashX1", (entity.getX() + 2 * entity.getLookAngle().x));
					entity.getPersistentData().putDouble("SlashX2", (entity.getX() + 4 * entity.getLookAngle().x));
					entity.getPersistentData().putDouble("SlashX3", (entity.getX() + 8 * entity.getLookAngle().x));
					entity.getPersistentData().putDouble("SlashZ1", (entity.getZ() + 2 * entity.getLookAngle().z));
					entity.getPersistentData().putDouble("SlashZ2", (entity.getZ() + 4 * entity.getLookAngle().z));
					entity.getPersistentData().putDouble("SlashZ3", (entity.getZ() + 8 * entity.getLookAngle().z));
					entity.getPersistentData().putDouble("SlashX4", (entity.getX() + 9 * entity.getLookAngle().x));
					entity.getPersistentData().putDouble("SlashZ4", (entity.getZ() + 9 * entity.getLookAngle().z));
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), ("summon sololeveling:dagger_slash " + xpos + " ~ " + zpos + " {Rotation:[" + yaw + "f,0f]}"));
						}
					}
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.5, 2);
						} else {
							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:slash")), SoundSource.NEUTRAL, (float) 0.5, 2, false);
						}
					}
					{
						final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("SlashX1")), y, (entity.getPersistentData().getDouble("SlashZ1")));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator) && !(entityiterator instanceof ExperienceOrb) && !(entityiterator instanceof IceChunkEntity) && !(entityiterator instanceof ItemEntity)) {
								entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:assassin"))), entity),
										(float) (10 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength / 10));
							}
						}
					}
					{
						final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("SlashX3")), y, (entity.getPersistentData().getDouble("SlashZ3")));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator) && !(entityiterator instanceof ExperienceOrb) && !(entityiterator instanceof IceChunkEntity) && !(entityiterator instanceof ItemEntity)) {
								entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:assassin"))), entity),
										(float) (10 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength / 10));
							}
						}
					}
					{
						final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("SlashX2")), y, (entity.getPersistentData().getDouble("SlashZ2")));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(4 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator) && !(entityiterator instanceof ExperienceOrb) && !(entityiterator instanceof IceChunkEntity) && !(entityiterator instanceof ItemEntity)) {
								entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:assassin"))), entity),
										(float) (10 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Strength / 10));
							}
						}
					}
					SololevelingMod.queueServerWork(12, () -> {
						if (!world.getBlockState(BlockPos.containing(entity.getPersistentData().getDouble("SlashX4"), y, entity.getPersistentData().getDouble("SlashZ4"))).canOcclude()
								&& !world.getBlockState(BlockPos.containing(entity.getPersistentData().getDouble("SlashX4"), y + 1, entity.getPersistentData().getDouble("SlashZ4"))).canOcclude()) {
							{
								Entity _ent = entity;
								_ent.teleportTo((entity.getPersistentData().getDouble("SlashX4")), y, (entity.getPersistentData().getDouble("SlashZ4")));
								if (_ent instanceof ServerPlayer _serverPlayer)
									_serverPlayer.connection.teleport((entity.getPersistentData().getDouble("SlashX4")), y, (entity.getPersistentData().getDouble("SlashZ4")), _ent.getYRot(), _ent.getXRot());
							}
						}
					});
				}
			}
		} else {
			if (entity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal("Not enough MP!"), true);
		}
	}
}
