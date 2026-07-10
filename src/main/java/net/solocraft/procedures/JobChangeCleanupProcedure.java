package net.solocraft.procedures;

import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.entity.SpawnerPortalEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.Registries;

import java.util.Comparator;
import java.util.List;

public class JobChangeCleanupProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		Vec3 center = new Vec3(x, y, z);
		List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(160.0D), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList();
		TagKey<net.minecraft.world.entity.EntityType<?>> dungeonMobTag = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("dm"));
		for (Entity target : entities) {
			if (target.getType().is(dungeonMobTag) || target instanceof DKnight1Entity || target instanceof DKnight2Entity || target instanceof DKnight3Entity || target instanceof SpawnerPortalEntity) {
				if (!target.level().isClientSide())
					target.discard();
			}
		}
	}
}
