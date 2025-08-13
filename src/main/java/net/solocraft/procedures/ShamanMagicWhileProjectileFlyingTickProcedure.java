package net.solocraft.procedures;

import net.solocraft.init.SololevelingModParticleTypes;
import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.List;
import java.util.Comparator;

public class ShamanMagicWhileProjectileFlyingTickProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity immediatesourceentity) {
		if (entity == null || immediatesourceentity == null)
			return;
		immediatesourceentity.setNoGravity(true);
		if (world instanceof ServerLevel _level)
			_level.sendParticles((SimpleParticleType) (SololevelingModParticleTypes.SHAMAN_MAGIC_PARTICLE.get()), (immediatesourceentity.getX()), (immediatesourceentity.getY()), (immediatesourceentity.getZ()), 2, 0.05, 0.05, 0.05, 0);
		immediatesourceentity.getPersistentData().putDouble("life", (immediatesourceentity.getPersistentData().getDouble("life") + 1));
		if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
			if (immediatesourceentity.getPersistentData().getDouble("life") <= 40) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(40 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == entityiterator) {
							immediatesourceentity.getPersistentData().putDouble("delta", Math.sqrt(Math.pow(entityiterator.getX() - immediatesourceentity.getX(), 2)
									+ Math.pow((entityiterator.getY() + entityiterator.getBbHeight()) - immediatesourceentity.getY(), 2) + Math.pow(entityiterator.getZ() - immediatesourceentity.getZ(), 2)));
							SololevelingMod.queueServerWork(20, () -> {
								immediatesourceentity.setDeltaMovement(new Vec3(((entityiterator.getX() - immediatesourceentity.getX()) / (4 * immediatesourceentity.getPersistentData().getDouble("delta"))),
										(((entityiterator.getY() + entityiterator.getBbHeight()) - immediatesourceentity.getY()) / (4 * immediatesourceentity.getPersistentData().getDouble("delta"))),
										((entityiterator.getZ() - immediatesourceentity.getZ()) / (4 * immediatesourceentity.getPersistentData().getDouble("delta")))));
							});
						}
					}
				}
			}
		}
		if (immediatesourceentity.getPersistentData().getDouble("life") >= 100) {
			if (!immediatesourceentity.level().isClientSide())
				immediatesourceentity.discard();
		}
	}
}
