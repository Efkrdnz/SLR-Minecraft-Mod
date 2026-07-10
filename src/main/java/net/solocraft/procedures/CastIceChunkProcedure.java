package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.entity.IceChunkEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;

public class CastIceChunkProcedure {
	public static void execute(LevelAccessor world, double y, Entity entity) {
		if (entity == null)
			return;
		double x = 0;
		double z = 0;
		double yaw = 0;
		if (!world.isClientSide()) {
			if (!CooldownManager.isOnCooldown(entity, "job_2")) {
				if (entity.onGround()) {
					CooldownManager.set(entity, "job_2", 140);
					x = entity.getX() + 8 * entity.getLookAngle().x;
					z = entity.getZ() + 8 * entity.getLookAngle().z;
					yaw = entity.getYRot();
					entity.getPersistentData().putDouble("IceChunkX1", (entity.getX() + 4 * entity.getLookAngle().x));
					entity.getPersistentData().putDouble("IceChunkX2", (entity.getX() + 8 * entity.getLookAngle().x));
					entity.getPersistentData().putDouble("IceChunkX3", (entity.getX() + 12 * entity.getLookAngle().x));
					entity.getPersistentData().putDouble("IceChunkZ1", (entity.getZ() + 4 * entity.getLookAngle().z));
					entity.getPersistentData().putDouble("IceChunkZ2", (entity.getZ() + 8 * entity.getLookAngle().z));
					entity.getPersistentData().putDouble("IceChunkZ3", (entity.getZ() + 12 * entity.getLookAngle().z));
					{
						Entity _ent = entity;
						if (!_ent.level().isClientSide() && _ent.getServer() != null) {
							_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
									_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), ("summon sololeveling:ice_chunk " + x + " ~ " + z + " {Rotation:[" + yaw + "f,0f]}"));
						}
					}
					SololevelingMod.queueServerWork((int) 8.4, () -> {
						{
							final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("IceChunkX1")), y, (entity.getPersistentData().getDouble("IceChunkZ1")));
							List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
							for (Entity entityiterator : _entfound) {
								if (!(entity == entityiterator) && !(entityiterator instanceof ExperienceOrb) && !(entityiterator instanceof IceChunkEntity) && !(entityiterator instanceof ItemEntity)) {
									entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC), entity), 10);
									if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
										_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.FREEZE.get(), 120, 1, false, false));
								}
							}
						}
						SololevelingMod.queueServerWork(6, () -> {
							{
								final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("IceChunkX2")), y, (entity.getPersistentData().getDouble("IceChunkZ2")));
								List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(6 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
								for (Entity entityiterator : _entfound) {
									if (!(entity == entityiterator) && !(entityiterator instanceof ExperienceOrb) && !(entityiterator instanceof IceChunkEntity) && !(entityiterator instanceof ItemEntity)) {
										entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC), entity), 10);
										if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
											_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.FREEZE.get(), 120, 1, false, false));
									}
								}
							}
							SololevelingMod.queueServerWork(6, () -> {
								{
									final Vec3 _center = new Vec3((entity.getPersistentData().getDouble("IceChunkX3")), y, (entity.getPersistentData().getDouble("IceChunkZ3")));
									List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(8 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center)))
											.toList();
									for (Entity entityiterator : _entfound) {
										if (!(entity == entityiterator) && !(entityiterator instanceof ExperienceOrb) && !(entityiterator instanceof IceChunkEntity) && !(entityiterator instanceof ItemEntity)) {
											entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.GENERIC), entity), 10);
											if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
												_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.FREEZE.get(), 120, 1, false, false));
										}
									}
								}
							});
						});
					});
				} else {
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("You need to be grounded to use this move!"), true);
				}
			}
		}
	}
}
