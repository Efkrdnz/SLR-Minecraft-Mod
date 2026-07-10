package net.solocraft.procedures;

import net.solocraft.entity.FuturisticGolemEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;
import net.solocraft.util.CooldownManager;

public class FuturisticGolemMeleeProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (!CooldownManager.isOnCooldown(entity, "golem_kick")) {
			if (entity.getPersistentData().getDouble("AI") == 1) {
				if (entity instanceof FuturisticGolemEntity) {
					((FuturisticGolemEntity) entity).setAnimation("kick");
				}
			}
			if (entity.getPersistentData().getDouble("AI") == 7) {
				world.addParticle(ParticleTypes.EXPLOSION, (entity.getX() + 1.5 * entity.getLookAngle().x), (entity.getY() + 0.25), (entity.getZ() + 1.5 * entity.getLookAngle().z), 0, 0, 0);
				if (world instanceof Level _level) {
					if (!_level.isClientSide()) {
						_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, (float) 0.5, 1);
					} else {
						_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, (float) 0.5, 1, false);
					}
				}
				CooldownManager.set(entity, "golem_kick", 20);
				{
					final Vec3 _center = new Vec3((entity.getX() + 1.5 * entity.getLookAngle().x), (entity.getY() + 0.25), (entity.getZ() + 1.5 * entity.getLookAngle().z));
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(3 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
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
							}), 20);
						}
					}
				}
			}
			if (entity.getPersistentData().getDouble("AI") >= 12) {
				entity.getPersistentData().putString("state", "idle");
				entity.getPersistentData().putDouble("AI", 0);
			}
		}
	}
}
