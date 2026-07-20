package net.solocraft.dungeon.runtime.layout;

import net.solocraft.dungeon.data.DungeonDataTypes.Int3;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.PreparedTemplate;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.RelativeBounds;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.TransformedSocket;
import net.solocraft.dungeon.runtime.DungeonTemplatePlacer.WorldBounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

/**
 * Shared room transform used by loaded templates and metadata-only builder
 * snapshots. The metadata path uses vanilla's own relative-position transform,
 * so rotations match final structure placement without requiring datapack reload.
 */
public final class DungeonRoomGeometry {
	private static final Mirror MIRROR = Mirror.NONE;
	private static final BlockPos PIVOT = BlockPos.ZERO;

	private final ResourceLocation templateId;
	private final Vec3i sourceSize;
	private final Vec3i rotatedSize;
	private final Rotation rotation;
	private final BlockPos desiredWorldMinimum;
	private final BlockPos placementOrigin;
	private final WorldBounds worldBounds;
	@Nullable private final PreparedTemplate runtimeTemplate;

	private DungeonRoomGeometry(ResourceLocation templateId, Vec3i sourceSize, Vec3i rotatedSize,
			Rotation rotation, BlockPos desiredWorldMinimum, BlockPos placementOrigin,
			WorldBounds worldBounds, @Nullable PreparedTemplate runtimeTemplate) {
		this.templateId = templateId;
		this.sourceSize = sourceSize;
		this.rotatedSize = rotatedSize;
		this.rotation = rotation;
		this.desiredWorldMinimum = desiredWorldMinimum;
		this.placementOrigin = placementOrigin;
		this.worldBounds = worldBounds;
		this.runtimeTemplate = runtimeTemplate;
	}

	public static DungeonRoomGeometry loaded(PreparedTemplate prepared) {
		if (prepared == null)
			throw new IllegalArgumentException("Prepared template is required.");
		return new DungeonRoomGeometry(prepared.templateId(), prepared.sourceSize(), prepared.rotatedSize(),
				prepared.rotation(), prepared.desiredWorldMinimum(), prepared.placementOrigin(),
				prepared.worldBounds(), prepared);
	}

	public static DungeonRoomGeometry declared(ResourceLocation templateId, Int3 declaredSize,
			BlockPos desiredWorldMinimum, Rotation rotation) {
		if (templateId == null || declaredSize == null || desiredWorldMinimum == null || rotation == null
				|| !declaredSize.isPositive())
			throw new IllegalArgumentException("Template id, positive declared size, minimum, and rotation are required.");
		Vec3i sourceSize = new Vec3i(declaredSize.x(), declaredSize.y(), declaredSize.z());
		int maxSourceX = sourceSize.getX() - 1;
		int maxSourceZ = sourceSize.getZ() - 1;
		BlockPos[] corners = new BlockPos[]{
				BlockPos.ZERO,
				new BlockPos(maxSourceX, 0, 0),
				new BlockPos(0, 0, maxSourceZ),
				new BlockPos(maxSourceX, 0, maxSourceZ)
		};
		int minX = Integer.MAX_VALUE;
		int minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE;
		int maxZ = Integer.MIN_VALUE;
		for (BlockPos corner : corners) {
			BlockPos transformed = StructureTemplate.transform(corner, MIRROR, rotation, PIVOT);
			minX = Math.min(minX, transformed.getX());
			minZ = Math.min(minZ, transformed.getZ());
			maxX = Math.max(maxX, transformed.getX());
			maxZ = Math.max(maxZ, transformed.getZ());
		}
		Vec3i rotatedSize = new Vec3i(maxX - minX + 1, sourceSize.getY(), maxZ - minZ + 1);
		BlockPos placementOrigin = desiredWorldMinimum.offset(-minX, 0, -minZ);
		WorldBounds bounds = new WorldBounds(desiredWorldMinimum,
				desiredWorldMinimum.offset(rotatedSize.getX() - 1, rotatedSize.getY() - 1,
						rotatedSize.getZ() - 1));
		return new DungeonRoomGeometry(templateId, sourceSize, rotatedSize, rotation,
				desiredWorldMinimum, placementOrigin, bounds, null);
	}

	public ResourceLocation templateId() {
		return templateId;
	}

	public Vec3i sourceSize() {
		return sourceSize;
	}

	public Vec3i rotatedSize() {
		return rotatedSize;
	}

	public Rotation rotation() {
		return rotation;
	}

	public BlockPos desiredWorldMinimum() {
		return desiredWorldMinimum;
	}

	public BlockPos placementOrigin() {
		return placementOrigin;
	}

	public WorldBounds worldBounds() {
		return worldBounds;
	}

	public boolean runtimePlaceable() {
		return runtimeTemplate != null;
	}

	public PreparedTemplate runtimeTemplate() {
		if (runtimeTemplate == null)
			throw new IllegalStateException("Metadata-only room geometry cannot be placed directly.");
		return runtimeTemplate;
	}

	public BlockPos transformRelative(BlockPos relative) {
		if (relative == null)
			throw new IllegalArgumentException("Relative position is required.");
		return placementOrigin.offset(StructureTemplate.transform(relative, MIRROR, rotation, PIVOT));
	}

	public Direction transformDirection(Direction direction) {
		if (direction == null)
			throw new IllegalArgumentException("Direction is required.");
		return rotation.rotate(direction);
	}

	public WorldBounds transformRelativeBounds(RelativeBounds bounds) {
		if (bounds == null)
			throw new IllegalArgumentException("Relative bounds are required.");
		return new WorldBounds(transformRelative(bounds.min()), transformRelative(bounds.max()));
	}

	public TransformedSocket transformSocket(RelativeBounds opening, Direction facing) {
		return new TransformedSocket(transformRelativeBounds(opening), transformDirection(facing));
	}
}
