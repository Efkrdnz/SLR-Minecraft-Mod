package net.solocraft.procedures;

import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.BeruShadowEntity;
import net.solocraft.entity.BeruBossEntity;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Comparator;

public class BeruShadowEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double rand = 0;
		if (entity instanceof BeruBossEntity _datEntSetI)
			_datEntSetI.getEntityData().set(BeruBossEntity.DATA_recovery, 5);
		if (entity instanceof BeruBossEntity) {
			rand = Mth.nextInt(RandomSource.create(), 1, 3);
			if (rand == 3) {
				if (entity instanceof BeruBossEntity) {
					((BeruBossEntity) entity).setAnimation("attack2");
				}
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(10 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows"))) && !(sourceentity instanceof IgrisShadowEntity) && !(sourceentity instanceof BeruShadowEntity)) {
						}
					}
				}
			}
		}
	}
}
