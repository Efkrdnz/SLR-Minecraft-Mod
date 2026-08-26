package net.solocraft.client.highlight;

import net.solocraft.network.EntityHighlightMessage;
import net.solocraft.util.EntityHighlightSystem;
import net.solocraft.util.SystemClientConfig;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

/** Client runtime state queried by the outline-rendering mixins. */
@EventBusSubscriber(value = Dist.CLIENT)
public final class ClientEntityHighlightManager {
	private static final int MAX_TARGETS = 2048;
	private static final int MAX_SOURCES_PER_TARGET = 16;
	private static final int MAX_VISIBLE_PARTY_TARGETS = 7;
	private static final Map<UUID, Map<String, Highlight>> HIGHLIGHTS = new HashMap<>();
	private static final Set<UUID> VISIBLE_TARGETS = new HashSet<>();
	private static long clientTick;
	private static long sequence;
	private static boolean selectionDirty = true;

	private ClientEntityHighlightManager() {
	}

	public static void handle(byte action, UUID targetId, ResourceLocation dimension, String source,
			int color, int durationTicks, int priority) {
		String safeSource = cleanSource(source);
		switch (action) {
			case EntityHighlightMessage.SET -> set(targetId, dimension, safeSource, color,
					durationTicks, priority);
			case EntityHighlightMessage.REMOVE -> remove(targetId, safeSource);
			case EntityHighlightMessage.CLEAR_SOURCE -> clearSource(safeSource);
			case EntityHighlightMessage.CLEAR_ALL -> clear();
			default -> {
			}
		}
	}

	/** Returns the highest-priority active RGB override for this exact client entity. */
	public static OptionalInt colorFor(Entity entity) {
		if (entity == null || !SystemClientConfig.isEntityOutlinesEnabled()
				|| !VISIBLE_TARGETS.contains(entity.getUUID()))
			return OptionalInt.empty();
		Map<String, Highlight> sources = HIGHLIGHTS.get(entity.getUUID());
		if (sources == null)
			return OptionalInt.empty();
		pruneExpired(sources);
		if (sources.isEmpty()) {
			HIGHLIGHTS.remove(entity.getUUID());
			return OptionalInt.empty();
		}
		return bestHighlight(sources, entity.level().dimension().location())
				.map(highlight -> OptionalInt.of(highlight.color()))
				.orElseGet(OptionalInt::empty);
	}

	public static boolean isHighlighted(Entity entity) {
		return colorFor(entity).isPresent();
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (false)
			return;
		clientTick++;
		if (clientTick % 20L == 0L)
			pruneAll();
		if (selectionDirty || clientTick % 5L == 0L)
			refreshVisibleTargets();
	}

	@SubscribeEvent
	public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
		clear();
	}

	private static void set(UUID targetId, ResourceLocation dimension, String source, int color,
			int durationTicks, int priority) {
		if (targetId == null || EntityHighlightMessage.NO_TARGET.equals(targetId) || dimension == null)
			return;
		if (!HIGHLIGHTS.containsKey(targetId) && HIGHLIGHTS.size() >= MAX_TARGETS) {
			pruneAll();
			if (HIGHLIGHTS.size() >= MAX_TARGETS)
				evictOldestTarget();
		}
		Map<String, Highlight> sources = HIGHLIGHTS.computeIfAbsent(targetId, ignored -> new HashMap<>());
		int safePriority = Math.max(0, priority);
		if (!sources.containsKey(source) && sources.size() >= MAX_SOURCES_PER_TARGET) {
			Map.Entry<String, Highlight> weakest = sources.entrySet().stream()
					.min(Comparator
							.comparingInt((Map.Entry<String, Highlight> entry) ->
									entry.getValue().priority())
							.thenComparingLong(entry -> entry.getValue().sequence()))
					.orElse(null);
			if (weakest != null && weakest.getValue().priority() > safePriority)
				return;
			if (weakest != null)
				sources.remove(weakest.getKey());
		}
		int safeDuration = Math.max(0, durationTicks);
		long expiresAt = safeDuration == 0 ? Long.MAX_VALUE : saturatingAdd(clientTick, safeDuration);
		sources.put(source, new Highlight(dimension, color & 0xFFFFFF, expiresAt,
				safePriority, ++sequence));
		selectionDirty = true;
	}

	private static void remove(UUID targetId, String source) {
		Map<String, Highlight> sources = HIGHLIGHTS.get(targetId);
		if (sources == null)
			return;
		sources.remove(source);
		if (sources.isEmpty())
			HIGHLIGHTS.remove(targetId);
		selectionDirty = true;
	}

	private static void clearSource(String source) {
		Iterator<Map.Entry<UUID, Map<String, Highlight>>> iterator = HIGHLIGHTS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map<String, Highlight> sources = iterator.next().getValue();
			sources.remove(source);
			if (sources.isEmpty())
				iterator.remove();
		}
		selectionDirty = true;
	}

	private static void clear() {
		HIGHLIGHTS.clear();
		VISIBLE_TARGETS.clear();
		clientTick = 0L;
		sequence = 0L;
		selectionDirty = true;
	}

	private static void pruneAll() {
		Iterator<Map.Entry<UUID, Map<String, Highlight>>> iterator = HIGHLIGHTS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map<String, Highlight> sources = iterator.next().getValue();
			pruneExpired(sources);
			if (sources.isEmpty())
				iterator.remove();
		}
		selectionDirty = true;
	}

	private static void pruneExpired(Map<String, Highlight> sources) {
		sources.values().removeIf(highlight -> highlight.expiresAt() <= clientTick);
	}

	private static Optional<Highlight> bestHighlight(Map<String, Highlight> sources,
			ResourceLocation dimension) {
		Highlight best = null;
		for (Map.Entry<String, Highlight> entry : sources.entrySet()) {
			Highlight candidate = entry.getValue();
			if (!candidate.dimension().equals(dimension) || !sourceEnabled(entry.getKey()))
				continue;
			if (best == null || candidate.priority() > best.priority()
					|| candidate.priority() == best.priority()
							&& candidate.sequence() > best.sequence())
				best = candidate;
		}
		return Optional.ofNullable(best);
	}

	private static boolean sourceEnabled(String source) {
		if (source.startsWith("dungeon:") || source.startsWith("dkc:"))
			return SystemClientConfig.isEncounterOutlinesEnabled();
		if (source.startsWith("perception:") || source.startsWith("skill:"))
			return SystemClientConfig.isPerceptionOutlinesEnabled();
		return true;
	}

	/**
	 * Chooses a small stable subset of the active leases. Hidden, aimed-at,
	 * elite, and boss targets win over nearby visible normal mobs.
	 */
	private static void refreshVisibleTargets() {
		VISIBLE_TARGETS.clear();
		selectionDirty = false;
		if (!SystemClientConfig.isEntityOutlinesEnabled())
			return;

		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return;
		UUID aimedTarget = minecraft.hitResult instanceof EntityHitResult hit
				? hit.getEntity().getUUID() : null;
		int density = SystemClientConfig.getOutlineDensity();
		int targetLimit = switch (density) {
			case SystemClientConfig.OUTLINE_DENSITY_MINIMAL -> 4;
			case SystemClientConfig.OUTLINE_DENSITY_HIGH -> 24;
			default -> 8;
		};
		double range = switch (density) {
			case SystemClientConfig.OUTLINE_DENSITY_MINIMAL -> 48.0D;
			case SystemClientConfig.OUTLINE_DENSITY_HIGH -> 128.0D;
			default -> 80.0D;
		};
		double rangeSqr = range * range;
		ResourceLocation dimension = minecraft.level.dimension().location();
		List<Candidate> candidates = new ArrayList<>();

		for (Entity entity : minecraft.level.entitiesForRendering()) {
			Map<String, Highlight> sources = HIGHLIGHTS.get(entity.getUUID());
			if (sources == null)
				continue;
			pruneExpired(sources);
			Optional<Highlight> selected = bestHighlight(sources, dimension);
			if (selected.isEmpty())
				continue;
			Highlight highlight = selected.get();
			Highlight partyHighlight = activePartyHighlight(sources, dimension);
			boolean partyMember = partyHighlight != null;
			boolean selectedPartyColor = partyHighlight == highlight;
			boolean targeted = entity.getUUID().equals(aimedTarget);
			boolean boss = !selectedPartyColor
					&& (highlight.priority() >= EntityHighlightSystem.PRIORITY_DUNGEON_BOSS
							|| highlight.color() == EntityHighlightSystem.COLOR_WAVE_BOSS);
			boolean elite = !boss && !selectedPartyColor
					&& (highlight.priority() >= EntityHighlightSystem.PRIORITY_DUNGEON_ELITE
							|| highlight.color() == EntityHighlightSystem.COLOR_WAVE_ELITE);
			double distanceSqr = minecraft.player.distanceToSqr(entity);
			if (!boss && !targeted && !partyMember && distanceSqr > rangeSqr)
				continue;
			boolean visible = minecraft.player.hasLineOfSight(entity);
			int tier = boss ? 2 : elite ? 1 : 0;
			if (!targeted && !partyMember && tier == 0) {
				if (density == SystemClientConfig.OUTLINE_DENSITY_MINIMAL && visible)
					continue;
				if (density == SystemClientConfig.OUTLINE_DENSITY_BALANCED
						&& visible && distanceSqr < 12.0D * 12.0D)
					continue;
			}
			candidates.add(new Candidate(entity.getUUID(), highlight, distanceSqr,
					targeted, visible, tier, partyMember));
		}

		candidates.sort(Comparator
				.comparingInt((Candidate candidate) -> candidate.targeted() ? 0 : 1)
				.thenComparingInt(candidate -> -candidate.tier())
				.thenComparingInt(candidate -> candidate.partyMember() ? 0 : 1)
				.thenComparingInt(candidate -> candidate.visible() ? 1 : 0)
				.thenComparingInt(candidate -> -candidate.highlight().priority())
				.thenComparingDouble(Candidate::distanceSqr));
		int limitedTargets = 0;
		int partyTargets = 0;
		for (Candidate candidate : candidates) {
			if (candidate.tier() == 2 || candidate.targeted()) {
				VISIBLE_TARGETS.add(candidate.targetId());
				continue;
			}
			if (candidate.partyMember()) {
				if (partyTargets >= MAX_VISIBLE_PARTY_TARGETS)
					continue;
				VISIBLE_TARGETS.add(candidate.targetId());
				partyTargets++;
				continue;
			}
			if (limitedTargets >= targetLimit)
				continue;
			VISIBLE_TARGETS.add(candidate.targetId());
			limitedTargets++;
		}
	}

	private static void evictOldestTarget() {
		HIGHLIGHTS.entrySet().stream()
				.min(Comparator.comparingLong(entry -> entry.getValue().values().stream()
						.mapToLong(Highlight::sequence).max().orElse(Long.MIN_VALUE)))
				.map(Map.Entry::getKey).ifPresent(HIGHLIGHTS::remove);
		selectionDirty = true;
	}

	private static long saturatingAdd(long left, int right) {
		return left > Long.MAX_VALUE - right ? Long.MAX_VALUE : left + right;
	}

	private static Highlight activePartyHighlight(Map<String, Highlight> sources,
			ResourceLocation dimension) {
		Highlight party = sources.get(EntityHighlightSystem.SOURCE_PARTY_MEMBERS);
		return party != null && party.dimension().equals(dimension)
				&& sourceEnabled(EntityHighlightSystem.SOURCE_PARTY_MEMBERS) ? party : null;
	}

	private static String cleanSource(String source) {
		if (source == null || source.isBlank())
			return "default";
		String trimmed = source.trim();
		return trimmed.length() <= EntityHighlightMessage.MAX_SOURCE_LENGTH ? trimmed
				: trimmed.substring(0, EntityHighlightMessage.MAX_SOURCE_LENGTH);
	}

	private record Highlight(ResourceLocation dimension, int color, long expiresAt, int priority,
			long sequence) {
	}

	private record Candidate(UUID targetId, Highlight highlight, double distanceSqr,
			boolean targeted, boolean visible, int tier, boolean partyMember) {
	}
}
