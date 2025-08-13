package net.solocraft.procedures;

import net.solocraft.entity.KamishShadowEntity;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.BeruShadowEntity;

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

public class IgrisEntityIsHurtProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double rand = 0;
		rand = Mth.nextInt(RandomSource.create(), 1, 3);
		if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
			if (rand == 3) {
				{
					final Vec3 _center = new Vec3(x, y, z);
					List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(12 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
					for (Entity entityiterator : _entfound) {
						if (sourceentity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows"))) && !(sourceentity instanceof IgrisShadowEntity) && !(sourceentity instanceof BeruShadowEntity)
								&& !(sourceentity instanceof KamishShadowEntity)) {
						}
					}
				}
			}
		}
	}
}
