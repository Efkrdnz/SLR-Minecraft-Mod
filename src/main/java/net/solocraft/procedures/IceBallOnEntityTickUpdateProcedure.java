package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import java.util.List;
import java.util.Comparator;

public class IceBallOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		Entity caster = null;
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if ((entity.getPersistentData().getString("caster")).equals(entityiterator.getDisplayName().getString())) {
					caster = entityiterator;
				}
			}
		}
		{
			Entity _ent = entity;
			if (!_ent.level().isClientSide() && _ent.getServer() != null) {
				_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
						_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "/particle snowflake ~ ~ ~ 2 0 2 0 12 force");
			}
		}
		if ((entity.getPersistentData().getString("state")).equals("")) {
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(100 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if ((entity.getPersistentData().getString("caster")).equals(entityiterator.getDisplayName().getString())) {
						entityiterator.setDeltaMovement(new Vec3(0, 0, 0));
						if (!entity.isShiftKeyDown()) {
							{
								Entity _ent = entity;
								_ent.teleportTo((entityiterator.getX() + 1 * entityiterator.getLookAngle().x), (entityiterator.getY() + 1.6 + 1 * entityiterator.getLookAngle().y), (entityiterator.getZ() + 1 * entityiterator.getLookAngle().z));
								if (_ent instanceof ServerPlayer _serverPlayer)
									_serverPlayer.connection.teleport((entityiterator.getX() + 1 * entityiterator.getLookAngle().x), (entityiterator.getY() + 1.6 + 1 * entityiterator.getLookAngle().y),
											(entityiterator.getZ() + 1 * entityiterator.getLookAngle().z), _ent.getYRot(), _ent.getXRot());
							}
						} else {
							{
								Entity _ent = entity;
								_ent.teleportTo((entityiterator.getX() + 1 * entityiterator.getLookAngle().x), (entityiterator.getY() + 1.2 + 1 * entityiterator.getLookAngle().y), (entityiterator.getZ() + 1 * entityiterator.getLookAngle().z));
								if (_ent instanceof ServerPlayer _serverPlayer)
									_serverPlayer.connection.teleport((entityiterator.getX() + 1 * entityiterator.getLookAngle().x), (entityiterator.getY() + 1.2 + 1 * entityiterator.getLookAngle().y),
											(entityiterator.getZ() + 1 * entityiterator.getLookAngle().z), _ent.getYRot(), _ent.getXRot());
							}
						}
					}
				}
			}
		}
		if ((entity.getPersistentData().getString("state")).equals("move")) {
			entity.getPersistentData().putDouble("IceLife", (entity.getPersistentData().getDouble("IceLife") + 1));
			if (entity.getPersistentData().getDouble("IceLife") >= 2 && entity.getPersistentData().getDouble("IceLife") % 2 == 0) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(5 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity.getPersistentData().getString("caster")).equals(entityiterator.getDisplayName().getString()) && !(entity == entityiterator)) {
							if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
								_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.FREEZE.get(), 60, 1, false, false));
							entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), caster),
									(float) (5 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Intelligence / 20));
						}
					}
				}
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(1 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity.getPersistentData().getString("caster")).equals(entityiterator.getDisplayName().getString()) && !(entity == entityiterator)) {
							entity.getPersistentData().putDouble("IceLife", 40);
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("IceLife") >= 2) {
				entity.setDeltaMovement(new Vec3((entity.getPersistentData().getDouble("IceX")), (entity.getPersistentData().getDouble("IceY")), (entity.getPersistentData().getDouble("IceZ"))));
			}
			if (entity.getPersistentData().getDouble("IceLife") >= 40) {
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 2, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 2, 1, false);
					}
				}
				int horizontalRadiusSphere = (int) 8 - 1;
				int verticalRadiusSphere = (int) 8 - 1;
				int yIterationsSphere = verticalRadiusSphere;
				for (int i = -yIterationsSphere; i <= yIterationsSphere; i++) {
					for (int xi = -horizontalRadiusSphere; xi <= horizontalRadiusSphere; xi++) {
						for (int zi = -horizontalRadiusSphere; zi <= horizontalRadiusSphere; zi++) {
							double distanceSq = (xi * xi) / (double) (horizontalRadiusSphere * horizontalRadiusSphere) + (i * i) / (double) (verticalRadiusSphere * verticalRadiusSphere)
									+ (zi * zi) / (double) (horizontalRadiusSphere * horizontalRadiusSphere);
							if (distanceSq <= 1.0) {
								if (world instanceof ServerLevel _level)
									_level.getServer().getCommands().performPrefixedCommand(
											new CommandSourceStack(CommandSource.NULL, new Vec3(x + xi, y + i, z + zi), Vec2.ZERO, _level, 4, "", Component.literal(""), _level.getServer(), null).withSuppressedOutput(),
											"/particle dust 0.26 0.85 0.88 2 ~ ~ ~ 0 0 0 0 2 force");
							}
						}
					}
				}
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(16 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity.getPersistentData().getString("caster")).equals(entityiterator.getDisplayName().getString()) && !(entity == entityiterator)) {
							entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), caster),
									(float) (16 + (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Intelligence / 10));
						}
					}
				}
				if (!entity.level().isClientSide())
					entity.discard();
			}
		}
	}
}
