package net.solocraft.dungeon.data;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static net.solocraft.dungeon.data.DungeonDataTypes.*;

/** Resolves exact entities and live entity tags from an immutable mob pool. */
public final class MobPoolResolver {
	private static final ConcurrentMap<EntityType<?>, Boolean> MOB_TYPE_CACHE = new ConcurrentHashMap<>();

	private MobPoolResolver() {
	}

	public static Optional<Selection> select(ServerLevel level, ResourceLocation poolId,
			int dungeonLevel, RandomSource random) {
		return select(level, poolId, dungeonLevel, Optional.empty(), random);
	}

	/** A wave-level range overrides an entry's spawn_level, which overrides dungeonLevel. */
	public static Optional<Selection> select(ServerLevel level, ResourceLocation poolId,
			int dungeonLevel, Optional<IntRange> waveLevel, RandomSource random) {
		Optional<MobPoolDefinition> pool = DungeonDataManager.mobPool(poolId);
		if (pool.isEmpty())
			return Optional.empty();

		List<Candidate> candidates = new ArrayList<>();
		long totalWeight = 0L;
		for (MobPoolEntry entry : pool.get().entries()) {
			if (!entry.eligibleAt(dungeonLevel))
				continue;
			List<EntityType<?>> types = resolve(level, entry.selector());
			if (types.isEmpty())
				continue;
			candidates.add(new Candidate(entry, types));
			totalWeight = Math.min(Long.MAX_VALUE / 2L, totalWeight + entry.weight());
		}
		if (candidates.isEmpty() || totalWeight <= 0L)
			return Optional.empty();

		long roll = Math.floorMod(random.nextLong(), totalWeight);
		Candidate selected = candidates.get(candidates.size() - 1);
		for (Candidate candidate : candidates) {
			roll -= candidate.entry.weight();
			if (roll < 0L) {
				selected = candidate;
				break;
			}
		}
		EntityType<?> type = selected.types.get(random.nextInt(selected.types.size()));
		ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(type);
		if (typeId == null)
			return Optional.empty();
		Optional<IntRange> chosenRange = waveLevel.isPresent() ? waveLevel : selected.entry.spawnLevel();
		int assignedLevel = chosenRange.map(range -> range.random(random)).orElse(Math.max(1, dungeonLevel));
		return Optional.of(new Selection(poolId, selected.entry.selector(), typeId, type, assignedLevel,
				selected.entry.baseXp()));
	}

	/** Resolves a selector against the current server registry/tag state. */
	public static List<EntityType<?>> resolve(ServerLevel level, EntitySelector selector) {
		if (selector.kind() == SelectorKind.ENTITY) {
			EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(selector.id());
			return type == null || !isSpawnableMob(level, type) ? List.of() : List.of(type);
		}

		Registry<EntityType<?>> registry = level.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
		TagKey<EntityType<?>> tag = TagKey.create(Registries.ENTITY_TYPE, selector.id());
		Map<ResourceLocation, EntityType<?>> unique = new LinkedHashMap<>();
		for (Holder<EntityType<?>> holder : registry.getTagOrEmpty(tag)) {
			EntityType<?> type = holder.value();
			ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
			if (id != null && isSpawnableMob(level, type))
				unique.put(id, type);
		}
		return unique.entrySet().stream()
				.sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
				.map(Map.Entry::getValue).toList();
	}

	/** Entity-type tags may legally contain projectiles or decorations; dungeon pools may not. */
	public static boolean isSpawnableMob(ServerLevel level, EntityType<?> type) {
		if (level == null || type == null || type == EntityType.PLAYER)
			return false;
		Boolean cached = MOB_TYPE_CACHE.get(type);
		if (cached != null)
			return cached;
		Entity entity = null;
		boolean spawnable = false;
		try {
			entity = type.create(level);
			spawnable = entity instanceof Mob;
		} catch (RuntimeException ignored) {
			spawnable = false;
		} finally {
			if (entity != null)
				entity.discard();
		}
		MOB_TYPE_CACHE.putIfAbsent(type, spawnable);
		return spawnable;
	}

	public record Selection(ResourceLocation pool, EntitySelector selector,
			ResourceLocation entityTypeId, EntityType<?> entityType, int level,
			Optional<Integer> baseXp) {
		public Selection {
			baseXp = baseXp == null ? Optional.empty() : baseXp;
		}
	}

	private record Candidate(MobPoolEntry entry, List<EntityType<?>> types) {
	}
}
