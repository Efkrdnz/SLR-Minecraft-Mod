package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.network.EntityHighlightMessage;

import net.minecraftforge.network.PacketDistributor;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * Server API for vanilla-style outlines visible only to a selected player.
 * Sources compose by priority so unrelated mechanics can safely highlight the
 * same target without clearing or recoloring one another.
 */
public final class EntityHighlightSystem {
	public static final int COLOR_WAVE_NORMAL = 0x58D8FF;
	public static final int COLOR_WAVE_ELITE = 0xFFB347;
	public static final int COLOR_WAVE_BOSS = 0xFF304F;
	public static final int COLOR_PERCEPTION_HOSTILE = 0xFF7043;
	public static final int COLOR_PERCEPTION_PLAYER = 0x55B8FF;
	public static final int COLOR_PERCEPTION_NEUTRAL = 0xF2D35E;

	public static final int PRIORITY_PERCEPTION = 100;
	public static final int PRIORITY_DUNGEON_NORMAL = 200;
	public static final int PRIORITY_DUNGEON_ELITE = 250;
	public static final int PRIORITY_DUNGEON_BOSS = 300;

	private static final int MAX_DURATION_TICKS = 1_200_000;
	private static final int MAX_PRIORITY = 10_000;
	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("minecraft", "soloboss"));
	private static final TagKey<EntityType<?>> PORTAL_TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("minecraft", "portals"));
	private static final TagKey<EntityType<?>> SIDE_TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("minecraft", "side"));

	private EntityHighlightSystem() {
	}

	public static void show(ServerPlayer viewer, Entity target, String source, int rgb,
			int durationTicks, int priority) {
		if (target == null || target.level().isClientSide())
			return;
		show(viewer, target.getUUID(), target.level().dimension(), source, rgb, durationTicks, priority);
	}

	public static void show(ServerPlayer viewer, UUID targetId, ResourceKey<Level> dimension,
			String source, int rgb, int durationTicks, int priority) {
		if (!valid(viewer, targetId, dimension))
			return;
		send(viewer, new EntityHighlightMessage(EntityHighlightMessage.SET, targetId,
				dimension.location(), cleanSource(source), rgb & 0xFFFFFF,
				Mth.clamp(durationTicks, 0, MAX_DURATION_TICKS),
				Mth.clamp(priority, 0, MAX_PRIORITY)));
	}

	public static void hide(ServerPlayer viewer, Entity target, String source) {
		if (target != null)
			hide(viewer, target.getUUID(), target.level().dimension(), source);
	}

	public static void hide(ServerPlayer viewer, UUID targetId, ResourceKey<Level> dimension,
			String source) {
		if (!valid(viewer, targetId, dimension))
			return;
		send(viewer, new EntityHighlightMessage(EntityHighlightMessage.REMOVE, targetId,
				dimension.location(), cleanSource(source), 0, 0, 0));
	}

	public static void clearSource(ServerPlayer viewer, String source) {
		if (viewer == null)
			return;
		send(viewer, new EntityHighlightMessage(EntityHighlightMessage.CLEAR_SOURCE,
				EntityHighlightMessage.NO_TARGET, viewer.level().dimension().location(),
				cleanSource(source), 0, 0, 0));
	}

	public static void clearAll(ServerPlayer viewer) {
		if (viewer == null)
			return;
		send(viewer, new EntityHighlightMessage(EntityHighlightMessage.CLEAR_ALL,
				EntityHighlightMessage.NO_TARGET, viewer.level().dimension().location(),
				"default", 0, 0, 0));
	}

	public static String dungeonSource(UUID instanceId) {
		return cleanSource("dungeon:" + instanceId);
	}

	public static int dungeonColor(DungeonMobLevelAdapter.MobRole role) {
		return switch (role == null ? DungeonMobLevelAdapter.MobRole.NORMAL : role) {
			case BOSS -> COLOR_WAVE_BOSS;
			case ELITE -> COLOR_WAVE_ELITE;
			case NORMAL -> COLOR_WAVE_NORMAL;
		};
	}

	public static int dungeonPriority(DungeonMobLevelAdapter.MobRole role) {
		return switch (role == null ? DungeonMobLevelAdapter.MobRole.NORMAL : role) {
			case BOSS -> PRIORITY_DUNGEON_BOSS;
			case ELITE -> PRIORITY_DUNGEON_ELITE;
			case NORMAL -> PRIORITY_DUNGEON_NORMAL;
		};
	}

	public static int perceptionColor(LivingEntity target) {
		DungeonMobLevelAdapter.MobRole role = DungeonMobLevelAdapter.MobRole.fromString(
				target.getPersistentData().getString(DungeonMobLevelAdapter.ROLE_TAG));
		if (role == DungeonMobLevelAdapter.MobRole.BOSS || target.getType().is(BOSS_TAG))
			return COLOR_WAVE_BOSS;
		if (role == DungeonMobLevelAdapter.MobRole.ELITE)
			return COLOR_WAVE_ELITE;
		if (target instanceof Enemy)
			return COLOR_PERCEPTION_HOSTILE;
		if (target instanceof Player)
			return COLOR_PERCEPTION_PLAYER;
		return COLOR_PERCEPTION_NEUTRAL;
	}

	public static int perceptionPriority(LivingEntity target) {
		DungeonMobLevelAdapter.MobRole role = DungeonMobLevelAdapter.MobRole.fromString(
				target.getPersistentData().getString(DungeonMobLevelAdapter.ROLE_TAG));
		if (role == DungeonMobLevelAdapter.MobRole.BOSS || target.getType().is(BOSS_TAG))
			return PRIORITY_PERCEPTION + 30;
		if (role == DungeonMobLevelAdapter.MobRole.ELITE || target instanceof Enemy)
			return PRIORITY_PERCEPTION + 10;
		return PRIORITY_PERCEPTION;
	}

	/** Excludes dead entities and the mod's non-combat portal/VFX entity families. */
	public static boolean isPerceptionCandidate(LivingEntity target) {
		return target != null && target.isAlive() && !target.isRemoved() && !target.isSpectator()
				&& !target.getType().is(PORTAL_TAG) && !target.getType().is(SIDE_TAG);
	}

	private static boolean valid(ServerPlayer viewer, UUID targetId, ResourceKey<Level> dimension) {
		return viewer != null && !viewer.hasDisconnected() && targetId != null
				&& !EntityHighlightMessage.NO_TARGET.equals(targetId) && dimension != null;
	}

	private static void send(ServerPlayer viewer, EntityHighlightMessage message) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> viewer), message);
	}

	private static String cleanSource(String source) {
		if (source == null || source.isBlank())
			return "default";
		String trimmed = source.trim();
		return trimmed.length() <= EntityHighlightMessage.MAX_SOURCE_LENGTH ? trimmed
				: trimmed.substring(0, EntityHighlightMessage.MAX_SOURCE_LENGTH);
	}
}
