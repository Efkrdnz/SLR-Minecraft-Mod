package net.solocraft.procedures;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.entity.FuturisticGolemEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;

public class FuturisticGolemTeleportProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			if (!(entity instanceof LivingEntity _livEnt2 && _livEnt2.hasEffect(SololevelingModMobEffects.GOLEM_TELEPORT_COOLDOWN.get()))) {
				if (entity.getPersistentData().getDouble("MF") == 1) {
					if (entity instanceof FuturisticGolemEntity) {
						((FuturisticGolemEntity) entity).setAnimation("tpattack");
					}
					entity.setNoGravity(true);
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 105, 10));
				}
				if (entity.getPersistentData().getDouble("MF") == 6) {
					{
						Entity _ent = entity;
						_ent.teleportTo(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 3),
								((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()));
						if (_ent instanceof ServerPlayer _serverPlayer)
							_serverPlayer.connection.teleport(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 3),
									((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ()), _ent.getYRot(), _ent.getXRot());
					}
				}
				if (entity.getPersistentData().getDouble("MF") == 15) {
					entity.setNoGravity(false);
				}
				if (entity.getPersistentData().getDouble("MF") == 31) {
					world.addParticle(ParticleTypes.EXPLOSION, x, y, z, 0, 0, 0);
					if (world instanceof Level _level) {
						if (!_level.isClientSide()) {
							_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 1, 1);
						} else {
							_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 1, 1, false);
						}
					}
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(SololevelingModMobEffects.GOLEM_TELEPORT_COOLDOWN.get(), 310, 1, false, false));
					{
						final Vec3 _center = new Vec3(x, y, z);
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(8 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entity == entityiterator)) {
								entityiterator.hurt((new DamageSource(((Level) world).registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity) {
									@Override
									public Component getLocalizedDeathMessage(LivingEntity _livingEntity) {
										Component _attackerName = null;
										Component _entityName = _livingEntity.getDisplayName();
										Component _itemName = null;
										Entity _attacker = this.getEntity();
										ItemStack _itemStack = ItemStack.EMPTY;
										if (_attacker != null) {
											_attackerName = _attacker.getDisplayName();
										}
										if (_attacker instanceof LivingEntity _livingAttacker) {
											_itemStack = _livingAttacker.getMainHandItem();
										}
										if (!_itemStack.isEmpty() && _itemStack.hasCustomHoverName()) {
											_itemName = _itemStack.getDisplayName();
										}
										if (_attacker != null && _itemName != null) {
											return Component.translatable("death.attack." + "mob.item", _entityName, _attackerName, _itemName);
										} else if (_attacker != null) {
											return Component.translatable("death.attack." + "mob", _entityName, _attackerName);
										} else {
											return Component.translatable("death.attack." + "mob", _entityName);
										}
									}
								}), 26);
							}
						}
					}
				}
				if (entity.getPersistentData().getDouble("MF") >= 101) {
					entity.getPersistentData().putString("state", "idle");
					entity.getPersistentData().putDouble("MF", 0);
				}
			} else {
				entity.getPersistentData().putString("state", "idle");
				entity.getPersistentData().putDouble("MF", 0);
			}
		} else {
			entity.getPersistentData().putString("state", "idle");
			entity.getPersistentData().putDouble("MF", 0);
		}
	}
}
