package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.SilladBossEntity;
import net.solocraft.init.SololevelingModEntities;

import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative spawn gate for the unreleased Sillad boss.
 *
 * <p>A fresh entity must consume an in-memory ticket created by
 * {@link #spawnForDeveloper(ServerPlayer)}. The persistent authorizer marker is
 * used only to let that same entity return from disk; it cannot authorize a
 * fresh command, egg, spawner, structure, or natural spawn.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class SilladBossSpawnManager {
	private static final String AUTHORIZED_BY_TAG =
			"slr_sillad_preview_authorizer";
	private static final double SPAWN_DISTANCE = 7.0D;
	private static final int[] VERTICAL_OFFSETS = {0, 1, -1, 2, -2, 3, -3};
	private static final int[][] HORIZONTAL_OFFSETS = {
			{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1},
			{1, 1}, {1, -1}, {-1, 1}, {-1, -1}
	};
	private static final Set<UUID> PENDING_AUTHORIZATIONS =
			ConcurrentHashMap.newKeySet();

	private SilladBossSpawnManager() {
	}

	/** Spawns Sillad near an entitled player, or returns {@code null}. */
	@Nullable
	public static SilladBossEntity spawnForDeveloper(ServerPlayer player) {
		if (player == null || !DeveloperModeManager.isEnabled(player))
			return null;

		ServerLevel level = player.serverLevel();
		SilladBossEntity sillad = SololevelingModEntities.SILLAD_BOSS.get()
				.create(level);
		if (sillad == null)
			return null;

		BlockPos spawnPos = findSafeSpawn(level, player, sillad);
		if (spawnPos == null)
			return null;

		float facing = player.getYRot() + 180.0F;
		sillad.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(),
				spawnPos.getZ() + 0.5D, facing, 0.0F);
		sillad.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos),
				MobSpawnType.EVENT, null);
		sillad.setPersistenceRequired();
		sillad.getPersistentData().putUUID(AUTHORIZED_BY_TAG,
				player.getUUID());

		UUID entityId = sillad.getUUID();
		PENDING_AUTHORIZATIONS.add(entityId);
		try {
			return level.addFreshEntity(sillad) ? sillad : null;
		} finally {
			PENDING_AUTHORIZATIONS.remove(entityId);
		}
	}

	/**
	 * Rejects every Sillad instance that did not originate from the gated
	 * server command. This event is the common path for both fresh entities and
	 * entities restored from a chunk save.
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onEntityJoin(EntityJoinLevelEvent event) {
		if (event.getLevel().isClientSide()
				|| !(event.getEntity() instanceof SilladBossEntity sillad))
			return;

		boolean hasAuthorizer = sillad.getPersistentData()
				.hasUUID(AUTHORIZED_BY_TAG);
		boolean authorized = event.loadedFromDisk()
				? hasAuthorizer
				: hasAuthorizer
						&& PENDING_AUTHORIZATIONS.remove(sillad.getUUID());
		if (!authorized)
			event.setCanceled(true);
	}

	@Nullable
	private static BlockPos findSafeSpawn(ServerLevel level,
			ServerPlayer player, SilladBossEntity sillad) {
		Vec3 look = player.getLookAngle();
		Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
		if (horizontal.lengthSqr() < 1.0E-6D)
			horizontal = Vec3.directionFromRotation(0.0F, player.getYRot());
		horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z).normalize();
		Vec3 target = player.position().add(horizontal.scale(SPAWN_DISTANCE));
		BlockPos base = BlockPos.containing(target.x, player.getY(), target.z);

		for (int[] horizontalOffset : HORIZONTAL_OFFSETS) {
			for (int verticalOffset : VERTICAL_OFFSETS) {
				BlockPos candidate = base.offset(horizontalOffset[0],
						verticalOffset, horizontalOffset[1]);
				if (!level.hasChunkAt(candidate))
					continue;
				BlockPos support = candidate.below();
				if (!level.getBlockState(support).isFaceSturdy(level, support,
						Direction.UP))
					continue;
				sillad.moveTo(candidate.getX() + 0.5D, candidate.getY(),
						candidate.getZ() + 0.5D, player.getYRot() + 180.0F,
						0.0F);
				if (level.noCollision(sillad, sillad.getBoundingBox()))
					return candidate.immutable();
			}
		}
		return null;
	}
}
