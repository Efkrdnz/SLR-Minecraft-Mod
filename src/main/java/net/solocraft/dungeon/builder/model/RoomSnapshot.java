package net.solocraft.dungeon.builder.model;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Metadata for an atomically captured room structure stored outside SavedData. */
public record RoomSnapshot(ResourceLocation structureKey, BlockPos size, BlockPos captureMin,
		String checksum, long capturedAt, long metadataRevision) {
	public RoomSnapshot {
		if (structureKey == null || size == null || captureMin == null)
			throw new IllegalArgumentException("Snapshot structure key, size, and capture minimum are required.");
		checksum = checksum == null ? "" : checksum;
		size = size.immutable();
		captureMin = captureMin.immutable();
	}
}
