package net.solocraft.procedures;

import net.solocraft.SololevelingMod;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import java.util.List;
import java.util.Comparator;

public class DunPlaceRandomLargeProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2000 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("dm"))) || entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("portals")))
						|| entityiterator instanceof ItemEntity || entityiterator instanceof ExperienceOrb) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
		}
		SololevelingMod.queueServerWork(20, () -> {
			DungeonPlaceLargeProcedure.execute(world, x, y, z);
		});
	}
}
