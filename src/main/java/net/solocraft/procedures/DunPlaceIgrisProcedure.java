package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.Portal12Entity;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Comparator;
import java.util.UUID;

public class DunPlaceIgrisProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		execute(world, x, y, z, null, null);
	}

	public static boolean executeForAttempt(ServerPlayer player) {
		if (player == null || player.level().isClientSide())
			return false;
		UUID attemptId = JobChangeQuestManager.activeAttemptId(player);
		if (attemptId == null || !JobChangeQuestManager.isAttemptActive(
				player.server, attemptId))
			return false;
		AABB nearby = AABB.ofSize(player.position(), 202.0D, 202.0D,
				202.0D);
		if (!player.serverLevel().getEntitiesOfClass(Portal12Entity.class,
				nearby, portal -> true).isEmpty())
			return false;
		execute(player.serverLevel(), player.getX(), player.getY(),
				player.getZ(), attemptId, player.getUUID());
		return true;
	}

	private static void execute(LevelAccessor world, double x, double y,
			double z, UUID attemptId, UUID ownerId) {
		{
			final Vec3 _center = new Vec3(x, y, z);
			List<Entity> _entfound = world.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(2000 / 2d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).toList();
			for (Entity entityiterator : _entfound) {
				if (entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("dm"))) || entityiterator.getType().is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("portals")))
						|| entityiterator instanceof ItemEntity || entityiterator instanceof ExperienceOrb) {
					if (!entityiterator.level().isClientSide())
						entityiterator.discard();
				}
			}
		}
		SololevelingMod.queueServerWork(20, () -> {
			if (world instanceof ServerLevel _serverworld) {
				if (attemptId != null
						&& !JobChangeQuestManager.isAttemptActive(
								_serverworld.getServer(), attemptId))
					return;
				StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "jobchange_dungeon1"));
				if (template != null) {
					template.placeInWorld(_serverworld, BlockPos.containing(x, y, z), BlockPos.containing(x, y, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 2);
					if (attemptId != null) {
						TagKey<net.minecraft.world.entity.EntityType<?>> dungeonMobTag =
								TagKey.create(Registries.ENTITY_TYPE,
										ResourceLocation.parse("dm"));
						TagKey<net.minecraft.world.entity.EntityType<?>> portalTag =
								TagKey.create(Registries.ENTITY_TYPE,
										ResourceLocation.parse("portals"));
						Vec3 center = new Vec3(x, y, z);
						for (Entity spawned : _serverworld.getEntitiesOfClass(
								Entity.class,
								new AABB(center, center).inflate(200.0D),
								candidate -> candidate.getType().is(
										dungeonMobTag)
										|| candidate.getType().is(
												portalTag))) {
							JobChangeQuestManager.tagAttemptEntity(spawned,
									attemptId, ownerId);
						}
					}
				}
			}
		});
	}
}
