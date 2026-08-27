package net.solocraft.procedures;

import net.solocraft.entity.DKnight1Entity;
import net.solocraft.entity.DKnight2Entity;
import net.solocraft.entity.DKnight3Entity;
import net.solocraft.entity.IgrisDeadBodyEntity;
import net.solocraft.entity.Portal12Entity;
import net.solocraft.entity.SpawnerPortalEntity;
import net.solocraft.util.JobChangeQuestManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.Registries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class JobChangeCleanupProcedure {
	private static final ResourceKey<Level> IGRIS_DIMENSION =
			ResourceKey.create(Registries.DIMENSION,
					ResourceLocation.fromNamespaceAndPath("sololeveling",
							"dungeon_dimension_igris"));

	public static void execute(LevelAccessor world, double x, double y, double z) {
		Vec3 center = new Vec3(x, y, z);
		List<Entity> entities = world.getEntitiesOfClass(Entity.class, new AABB(center, center).inflate(160.0D), e -> true).stream().sorted(Comparator.comparingDouble(e -> e.distanceToSqr(center))).toList();
		TagKey<net.minecraft.world.entity.EntityType<?>> dungeonMobTag = TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("dm"));
		for (Entity target : entities) {
			if (target.getType().is(dungeonMobTag) || target instanceof DKnight1Entity || target instanceof DKnight2Entity || target instanceof DKnight3Entity || target instanceof SpawnerPortalEntity) {
				if (!target.level().isClientSide())
					target.discard();
			}
		}
	}

	/**
	 * Removes only entities belonging to one durable Job Change attempt. This
	 * cannot erase another player's arena even if two random arenas happen to be
	 * generated unusually close together.
	 */
	public static void executeAttempt(MinecraftServer server, UUID attemptId) {
		cleanupAttempt(server, attemptId, false);
	}

	/**
	 * Cleans combat remnants after a successful trial while preserving the
	 * authored return portal and Igris body needed by the post-quest flow.
	 */
	public static void completeAttempt(MinecraftServer server,
			UUID attemptId) {
		cleanupAttempt(server, attemptId, true);
	}

	private static void cleanupAttempt(MinecraftServer server,
			UUID attemptId, boolean preserveCompletionEntities) {
		if (server == null || attemptId == null)
			return;
		ServerLevel level = server.getLevel(IGRIS_DIMENSION);
		if (level == null)
			return;
		List<Entity> removals = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (!JobChangeQuestManager.hasAttemptId(entity, attemptId))
				continue;
			if (preserveCompletionEntities
					&& (entity instanceof Portal12Entity
							|| entity instanceof IgrisDeadBodyEntity)) {
				entity.getPersistentData().remove(
						JobChangeQuestManager.ATTEMPT_ID_TAG);
				entity.getPersistentData().remove(
						JobChangeQuestManager.ATTEMPT_OWNER_TAG);
			} else {
				removals.add(entity);
			}
		}
		for (Entity entity : removals)
			entity.discard();
	}
}
