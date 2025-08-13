package net.solocraft.procedures;

import net.solocraft.entity.GoblinKingEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Comparator;

public class GoblinBossattack2Procedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 1) {
			if (entity instanceof GoblinKingEntity) {
				((GoblinKingEntity) entity).setAnimation("attack_2");
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 7) {
			{
				final Vec3 _center = new Vec3((entity.getX() + 2 * entity.getLookAngle().x), (entity.getY() + 1), (entity.getZ() + 2 * entity.getLookAngle().z));
				List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
				for (Entity entityiterator : _entfound) {
					if (!(entity == entityiterator)) {
						if (world instanceof Level _level && !_level.isClientSide())
							_level.explode(null, (entityiterator.getX()), (entityiterator.getY() + 1), (entityiterator.getZ()), 1, Level.ExplosionInteraction.NONE);
						entityiterator.hurt(new DamageSource(world.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MOB_ATTACK)), 6);
					}
				}
			}
		}
		if ((entity instanceof GoblinKingEntity _datEntI ? _datEntI.getEntityData().get(GoblinKingEntity.DATA_MF) : 0) == 41) {
			GoblinBossstatechangerProcedure.execute(entity);
		}
	}
}
