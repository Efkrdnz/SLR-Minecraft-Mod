package net.solocraft.client.aura;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Client runtime state for packet-driven continuous auras and short bursts. */
public final class ClientPlayerAuraManager {
	private static final Map<Integer, AuraInstance> CONTINUOUS = new HashMap<>();
	private static final Map<Integer, List<AuraInstance>> BURSTS = new HashMap<>();
	private static final Map<Integer, List<TrailPoint>> TRAILS = new HashMap<>();
	private static final int TRAIL_LIFETIME = 14;
	private static final int MAX_TRAIL_POINTS = 8;

	private ClientPlayerAuraManager() {
	}

	public static void handle(int entityId, String auraId, byte action, int duration, float intensity) {
		if (action == 1) {
			CONTINUOUS.remove(entityId);
			return;
		}
		if (PlayerAuraRegistry.get(auraId) == null)
			return;
		long now = gameTime();
		AuraInstance instance = new AuraInstance(auraId, now, action == 2 ? Math.max(1, duration) : -1,
				Math.max(0.05F, Math.min(3.0F, intensity)), entityId * 31 + auraId.hashCode());
		if (action == 2)
			BURSTS.computeIfAbsent(entityId, ignored -> new ArrayList<>()).add(instance);
		else
			CONTINUOUS.put(entityId, instance);
	}

	public static List<AuraInstance> activeFor(int entityId) {
		long now = gameTime();
		List<AuraInstance> result = new ArrayList<>(4);
		AuraInstance continuous = CONTINUOUS.get(entityId);
		if (continuous != null)
			result.add(continuous);
		List<AuraInstance> bursts = BURSTS.get(entityId);
		if (bursts != null) {
			Iterator<AuraInstance> iterator = bursts.iterator();
			while (iterator.hasNext()) {
				AuraInstance burst = iterator.next();
				if (burst.expired(now))
					iterator.remove();
				else
					result.add(burst);
			}
			if (bursts.isEmpty())
				BURSTS.remove(entityId);
		}
		return result;
	}

	public static void recordTrail(int entityId, Vec3 position) {
		long now = gameTime();
		List<TrailPoint> trail = TRAILS.computeIfAbsent(entityId, ignored -> new ArrayList<>());
		trail.removeIf(point -> now - point.tick() > TRAIL_LIFETIME);
		if (!trail.isEmpty()) {
			TrailPoint newest = trail.get(0);
			double distance = newest.position().distanceToSqr(position);
			if (distance > 64.0D) {
				trail.clear();
			} else if (distance < 0.018D) {
				return;
			}
		}
		trail.add(0, new TrailPoint(position, now));
		while (trail.size() > MAX_TRAIL_POINTS)
			trail.remove(trail.size() - 1);
	}

	public static List<TrailPoint> trailFor(int entityId) {
		long now = gameTime();
		List<TrailPoint> trail = TRAILS.get(entityId);
		if (trail == null)
			return List.of();
		trail.removeIf(point -> now - point.tick() > TRAIL_LIFETIME);
		if (trail.isEmpty()) {
			TRAILS.remove(entityId);
			return List.of();
		}
		return List.copyOf(trail);
	}

	public static void clearTrail(int entityId) {
		TRAILS.remove(entityId);
	}

	private static long gameTime() {
		return Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
	}

	public record AuraInstance(String auraId, long startTick, int duration, float intensity, int seed) {
		public boolean expired(long now) {
			return duration >= 0 && now - startTick >= duration;
		}

		public float envelope(float partialTick, long now) {
			if (duration < 0)
				return 1.0F;
			float progress = Math.max(0.0F, Math.min(1.0F, (now - startTick + partialTick) / duration));
			float fadeIn = Math.min(1.0F, progress * 7.0F);
			float fadeOut = Math.min(1.0F, (1.0F - progress) * 4.0F);
			return fadeIn * fadeOut;
		}
	}

	public record TrailPoint(Vec3 position, long tick) {
	}
}
