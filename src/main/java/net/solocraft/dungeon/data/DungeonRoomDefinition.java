package net.solocraft.dungeon.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

import static net.solocraft.dungeon.data.DungeonDataTypes.*;

/** A reusable authored structure module and all of its logical anchors. */
public record DungeonRoomDefinition(
		ResourceLocation id,
		int formatVersion,
		ResourceLocation structure,
		RoomRole role,
		int weight,
		Int3 size,
		Int3 origin,
		Optional<ResourceLocation> defaultMobPool,
		Optional<ShellSettings> shellOverride,
		List<Region> regions,
		List<Socket> sockets,
		List<Marker> markers,
		List<Encounter> encounters) {

	public DungeonRoomDefinition {
		defaultMobPool = defaultMobPool == null ? Optional.empty() : defaultMobPool;
		shellOverride = shellOverride == null ? Optional.empty() : shellOverride;
		regions = List.copyOf(regions);
		sockets = List.copyOf(sockets);
		markers = List.copyOf(markers);
		encounters = List.copyOf(encounters);
	}
}
