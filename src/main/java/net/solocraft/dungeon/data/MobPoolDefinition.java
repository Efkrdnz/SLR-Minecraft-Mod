package net.solocraft.dungeon.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

import static net.solocraft.dungeon.data.DungeonDataTypes.MobPoolEntry;

/** A weighted, addon-extensible mob selection pool. */
public record MobPoolDefinition(ResourceLocation id, int formatVersion, List<MobPoolEntry> entries) {
	public MobPoolDefinition {
		entries = List.copyOf(entries);
	}
}
