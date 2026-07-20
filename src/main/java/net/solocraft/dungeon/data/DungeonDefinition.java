package net.solocraft.dungeon.data;

import net.solocraft.dungeon.ProceduralDungeonRank;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static net.solocraft.dungeon.data.DungeonDataTypes.*;

/** A fixed dungeon or the assembly rules for a procedural dungeon. */
public record DungeonDefinition(
		ResourceLocation id,
		int formatVersion,
		DungeonKind kind,
		Set<ProceduralDungeonRank> allowedRanks,
		Optional<ResourceLocation> structure,
		Optional<Int3> size,
		Optional<Int3> origin,
		Optional<ResourceLocation> defaultMobPool,
		Optional<ResourceLocation> bossMobPool,
		Map<RoomRole, List<WeightedRoom>> roomPools,
		IntRange roomCount,
		int maxDepth,
		LevelRule level,
		ShellSettings shell,
		List<Region> regions,
		List<Socket> sockets,
		List<Marker> markers,
		List<Encounter> encounters,
		List<FixedRoomPlacement> fixedPlacements,
		List<FixedRoomConnection> fixedConnections,
		DungeonTopology topology) {

	public DungeonDefinition {
		allowedRanks = Set.copyOf(allowedRanks);
		if (allowedRanks.isEmpty())
			throw new IllegalArgumentException("A dungeon must allow at least one gate rank.");
		structure = structure == null ? Optional.empty() : structure;
		size = size == null ? Optional.empty() : size;
		origin = origin == null ? Optional.empty() : origin;
		defaultMobPool = defaultMobPool == null ? Optional.empty() : defaultMobPool;
		bossMobPool = bossMobPool == null ? Optional.empty() : bossMobPool;
		EnumMap<RoomRole, List<WeightedRoom>> copiedPools = new EnumMap<>(RoomRole.class);
		roomPools.forEach((role, rooms) -> copiedPools.put(role, List.copyOf(rooms)));
		roomPools = Collections.unmodifiableMap(copiedPools);
		regions = List.copyOf(regions);
		sockets = List.copyOf(sockets);
		markers = List.copyOf(markers);
		encounters = List.copyOf(encounters);
		fixedPlacements = fixedPlacements == null ? List.of() : List.copyOf(fixedPlacements);
		fixedConnections = fixedConnections == null ? List.of() : List.copyOf(fixedConnections);
		topology = topology == null ? DungeonTopology.LINEAR : topology;
	}

	/** Compatibility constructor for exact layouts created before topology became persisted. */
	public DungeonDefinition(ResourceLocation id, int formatVersion, DungeonKind kind,
			Set<ProceduralDungeonRank> allowedRanks, Optional<ResourceLocation> structure,
			Optional<Int3> size, Optional<Int3> origin, Optional<ResourceLocation> defaultMobPool,
			Optional<ResourceLocation> bossMobPool, Map<RoomRole, List<WeightedRoom>> roomPools,
			IntRange roomCount, int maxDepth, LevelRule level, ShellSettings shell,
			List<Region> regions, List<Socket> sockets, List<Marker> markers, List<Encounter> encounters,
			List<FixedRoomPlacement> fixedPlacements, List<FixedRoomConnection> fixedConnections) {
		this(id, formatVersion, kind, allowedRanks, structure, size, origin, defaultMobPool, bossMobPool,
				roomPools, roomCount, maxDepth, level, shell, regions, sockets, markers, encounters,
				fixedPlacements, fixedConnections, DungeonTopology.LINEAR);
	}

	/** Backward-compatible constructor for preset/procedural definitions. */
	public DungeonDefinition(ResourceLocation id, int formatVersion, DungeonKind kind,
			Set<ProceduralDungeonRank> allowedRanks, Optional<ResourceLocation> structure,
			Optional<Int3> size, Optional<Int3> origin, Optional<ResourceLocation> defaultMobPool,
			Optional<ResourceLocation> bossMobPool, Map<RoomRole, List<WeightedRoom>> roomPools,
			IntRange roomCount, int maxDepth, LevelRule level, ShellSettings shell,
			List<Region> regions, List<Socket> sockets, List<Marker> markers, List<Encounter> encounters) {
		this(id, formatVersion, kind, allowedRanks, structure, size, origin, defaultMobPool, bossMobPool,
				roomPools, roomCount, maxDepth, level, shell, regions, sockets, markers, encounters,
				List.of(), List.of(), DungeonTopology.LINEAR);
	}

	public List<WeightedRoom> rooms(RoomRole role) {
		return roomPools.getOrDefault(role, List.of());
	}

	public boolean supportsRank(ProceduralDungeonRank rank) {
		return rank != null && allowedRanks.contains(rank);
	}
}
