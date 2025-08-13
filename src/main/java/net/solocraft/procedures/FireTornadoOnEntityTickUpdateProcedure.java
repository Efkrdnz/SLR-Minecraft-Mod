package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleTypes;

import java.util.List;
import java.util.Comparator;

public class FireTornadoOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double inteligence = 0;
		if (entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) {
			if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
				inteligence = ((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).Intelligence;
			}
		}
		if (world instanceof ServerLevel _level)
			_level.sendParticles(ParticleTypes.FLAME, x, (y + 1.5), z, 10, 0.4, 1, 0.4, 0.05);
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.FIRE_PARTICLE_2.get()), x, (y + 1.5), z, 10, 0.4, 1, 0.4, 0.05);
		if (entity.getPersistentData().getDouble("Timer") < 60) {
			entity.getPersistentData().putDouble("Timer", (entity.getPersistentData().getDouble("Timer") + 1));
		} else {
			if (!entity.level().isClientSide())
				entity.discard();
		}
		entity.setDeltaMovement(new Vec3((entity.getLookAngle().x * 0.5), (entity.getDeltaMovement().y()), (entity.getLookAngle().z * 0.5)));
		entity.noPhysics = true;
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (!(entity == entityiterator) && !(entityiterator instanceof ExperienceOrb) && !(entityiterator instanceof ItemEntity)) {
					entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:mage"))),
							(entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null)), (float) (5 + inteligence / 25));
					entityiterator.setSecondsOnFire(5);
					entityiterator.setDeltaMovement(new Vec3(((x - entityiterator.getX()) * 0.6), ((y - entityiterator.getY()) * 0.6), ((z - entityiterator.getZ()) * 0.6)));
				}
			}
		}
	}
}
