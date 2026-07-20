package net.solocraft.dungeon.data;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.solocraft.dungeon.data.DungeonDataTypes.ValidationIssue;

/** The all-or-nothing immutable view consumed by dungeon runtime code. */
public record DungeonDataSnapshot(
		long revision,
		Map<ResourceLocation, DungeonRoomDefinition> rooms,
		Map<ResourceLocation, DungeonDefinition> dungeons,
		Map<ResourceLocation, MobPoolDefinition> mobPools,
		List<ValidationIssue> issues) {

	private static final DungeonDataSnapshot EMPTY = new DungeonDataSnapshot(0, Map.of(), Map.of(), Map.of(), List.of());

	public DungeonDataSnapshot {
		rooms = immutableSortedMap(rooms);
		dungeons = immutableSortedMap(dungeons);
		mobPools = immutableSortedMap(mobPools);
		issues = List.copyOf(issues);
	}

	public static DungeonDataSnapshot empty() {
		return EMPTY;
	}

	DungeonDataSnapshot withRevision(long value) {
		return new DungeonDataSnapshot(value, rooms, dungeons, mobPools, issues);
	}

	public Optional<DungeonRoomDefinition> room(ResourceLocation id) {
		return Optional.ofNullable(rooms.get(id));
	}

	public Optional<DungeonDefinition> dungeon(ResourceLocation id) {
		return Optional.ofNullable(dungeons.get(id));
	}

	public Optional<MobPoolDefinition> mobPool(ResourceLocation id) {
		return Optional.ofNullable(mobPools.get(id));
	}

	public List<ResourceLocation> roomIds() {
		return List.copyOf(rooms.keySet());
	}

	public List<ResourceLocation> dungeonIds() {
		return List.copyOf(dungeons.keySet());
	}

	public List<ResourceLocation> mobPoolIds() {
		return List.copyOf(mobPools.keySet());
	}

	private static <T> Map<ResourceLocation, T> immutableSortedMap(Map<ResourceLocation, T> input) {
		List<Map.Entry<ResourceLocation, T>> entries = new ArrayList<>(input.entrySet());
		entries.sort(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)));
		LinkedHashMap<ResourceLocation, T> sorted = new LinkedHashMap<>();
		entries.forEach(entry -> sorted.put(entry.getKey(), entry.getValue()));
		return Collections.unmodifiableMap(sorted);
	}
}
