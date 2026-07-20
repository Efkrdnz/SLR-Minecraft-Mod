package net.solocraft.procedures;

import net.solocraft.entity.BloodRedComIgrisEntity;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;

public class IgrisSlamProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			if ((entity.getPersistentData().getString("state")).equals("slam")) {
				if (entity.getPersistentData().getDouble("MF") == 1) {
					if (entity instanceof BloodRedComIgrisEntity) {
						((BloodRedComIgrisEntity) entity).setAnimation("attack_slam");
					}
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 55, 5, false, false));
				}
				if (entity.getPersistentData().getDouble("MF") == 21) {
					{
						final Vec3 _center = new Vec3((x + 2 * entity.getLookAngle().x), (entity.getY() + 1), (z + 2 * entity.getLookAngle().z));
						List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(8 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
						for (Entity entityiterator : _entfound) {
							if (!(entityiterator == entity) && entityiterator instanceof LivingEntity) {
								entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK), entity), scaledDamage(entity, 1.20F, 16.8F));
								if (entityiterator instanceof LivingEntity _entity && !_entity.level().isClientSide())
									_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
								if (world instanceof Level _level) {
									if (!_level.isClientSide()) {
										_level.playSound(null, BlockPos.containing(x, y, z), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 1, (float) 0.5);
									} else {
										_level.playLocalSound(x, y, z, ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.explode")), SoundSource.NEUTRAL, 1, (float) 0.5, false);
									}
								}
							}
						}
					}
				}
				if (entity.getPersistentData().getDouble("MF") >= 44) {
					entity.getPersistentData().putString("state", "idle");
					entity.getPersistentData().putDouble("MF", 0);
				}
			}
		} else {
			entity.getPersistentData().putString("state", "idle");
			entity.getPersistentData().putDouble("MF", 0);
		}
	}

	private static float scaledDamage(Entity entity, float multiplier, float minimum) {
		if (entity instanceof LivingEntity living && living.getAttribute(Attributes.ATTACK_DAMAGE) != null)
			return (float) Math.max(minimum, living.getAttributeValue(Attributes.ATTACK_DAMAGE) * multiplier);
		return minimum;
	}
}
