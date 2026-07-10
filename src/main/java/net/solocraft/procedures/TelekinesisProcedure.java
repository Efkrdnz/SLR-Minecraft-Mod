package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.RulersHandEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;

public class TelekinesisProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double num_size = 0;
		double num_precision = 0;
		double num_speed = 0;
		double num_cooldown = 0;
		double num_range = 0;
		double num_width = 0;
		double num_delay = 0;
		double num = 0;
		if (entity.isShiftKeyDown()) {
			if (world instanceof Level _level) {
				if (!_level.isClientSide()) {
					_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:telepush")), SoundSource.NEUTRAL, 1, 2);
				} else {
					_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:telepush")), SoundSource.NEUTRAL, 1, 2, false);
				}
			}
			{
				Entity _shootFrom = entity;
				Level projectileLevel = _shootFrom.level();
				if (!projectileLevel.isClientSide()) {
					Projectile _entityToSpawn = new Object() {
						public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
							AbstractArrow entityToSpawn = new RulersHandEntity(SololevelingModEntities.RULERS_HAND.get(), level);
							entityToSpawn.setOwner(shooter);
							entityToSpawn.setBaseDamage(damage);
							entityToSpawn.setKnockback(knockback);
							entityToSpawn.setSilent(true);
							return entityToSpawn;
						}
					}.getArrow(projectileLevel, entity, 0, 0);
					_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
					_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 10, 0);
					projectileLevel.addFreshEntity(_entityToSpawn);
				}
			}
		} else {
			{
				final Vec3 _center = new Vec3(x, y, z);
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(50 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")
							&& !((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
									.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
						if (!(entity == entityiterator) && !(entityiterator instanceof TamableAnimal _tamIsTamedBy && entity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false)) {
							num = num + 1;
						}
					} else if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")) {
						if (!(entity == entityiterator) && !(entityiterator instanceof TamableAnimal _tamIsTamedBy && entity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false)) {
							num = num + 1;
						}
					}
				}
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= num * 100) {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal(("\u00A73" + "Using Telekinesis Push")), false);
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:telepush")), SoundSource.NEUTRAL, 1, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("sololeveling:telepush")), SoundSource.NEUTRAL, 1, 1, false);
					}
				}
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(50 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (!(entity == entityiterator) && !(entityiterator instanceof TamableAnimal _tamIsTamedBy && entity instanceof LivingEntity _livEnt ? _tamIsTamedBy.isOwnedBy(_livEnt) : false)) {
							if (!((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")
									&& !((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)
											.equals((entityiterator.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party)) {
								entityiterator.setDeltaMovement(new Vec3(
										((entityiterator.getX() - entity.getX())
												* (1 / Math.cbrt(Math.pow(entityiterator.getX() - entity.getX(), 2) + Math.pow(entityiterator.getY() - entity.getY(), 2) + Math.pow(entityiterator.getZ() - entity.getZ(), 2)))),
										0.5, ((entityiterator.getZ() - entity.getZ())
												* (1 / Math.cbrt(Math.pow(entityiterator.getX() - entity.getX(), 2) + Math.pow(entityiterator.getY() - entity.getY(), 2) + Math.pow(entityiterator.getZ() - entity.getZ(), 2))))));
								CooldownManager.set(entity, "mana_refresh", 10);
								CooldownManager.set(entity, "telekinesis", 50);
							} else if (((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).party).equals("")) {
								entityiterator.setDeltaMovement(new Vec3(
										((entityiterator.getX() - entity.getX())
												* (1 / Math.cbrt(Math.pow(entityiterator.getX() - entity.getX(), 2) + Math.pow(entityiterator.getY() - entity.getY(), 2) + Math.pow(entityiterator.getZ() - entity.getZ(), 2)))),
										0.5, ((entityiterator.getZ() - entity.getZ())
												* (1 / Math.cbrt(Math.pow(entityiterator.getX() - entity.getX(), 2) + Math.pow(entityiterator.getY() - entity.getY(), 2) + Math.pow(entityiterator.getZ() - entity.getZ(), 2))))));
								CooldownManager.set(entity, "mana_refresh", 20);
								CooldownManager.set(entity, "telekinesis", 60);
							}
						}
					}
				}
			} else {
				CooldownManager.set(entity, "mana_refresh", 10);
				CooldownManager.set(entity, "telekinesis", 20);
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal(("\u00A74" + "Not enough mana!")), false);
			}
		}
	}
}
