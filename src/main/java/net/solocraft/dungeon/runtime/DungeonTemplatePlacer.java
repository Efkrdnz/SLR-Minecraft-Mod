package net.solocraft.dungeon.runtime;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads and places authored dungeon templates with one shared transform for
 * blocks, markers, sockets, collision bounds, and later shell generation.
 *
 * <p>The supplied world position is always the minimum corner of the rotated
 * template. Minecraft rotates templates around relative {@code (0, 0, 0)}, so
 * the actual placement origin may be outside that minimum corner. Keeping that
 * adjustment here prevents the assembler and marker runtime from developing
 * subtly different rotation math.</p>
 */
public final class DungeonTemplatePlacer {
	private static final Mirror MIRROR = Mirror.NONE;
	private static final BlockPos ROTATION_PIVOT = BlockPos.ZERO;
	private static final int PLACEMENT_FLAGS = 3;

	private DungeonTemplatePlacer() {
	}

	/**
	 * Loads an existing template and prepares its transform without changing the
	 * world. Missing templates are reported; this method never creates an empty
	 * template as {@code getOrCreate} would.
	 */
	public static PreparationResult prepare(ServerLevel level, ResourceLocation templateId,
			BlockPos desiredWorldMinimum, Rotation rotation) {
		if (level == null || templateId == null || desiredWorldMinimum == null || rotation == null)
			return PreparationResult.failure(ErrorCode.INVALID_ARGUMENT,
					"Level, template id, desired world minimum, and rotation are required.");

		try {
			StructureTemplate template = level.getStructureManager().get(templateId).orElse(null);
			if (template == null)
				return PreparationResult.failure(ErrorCode.MISSING_TEMPLATE,
						"Missing structure template " + templateId + ".");

			Vec3i sourceSize = template.getSize();
			if (!hasVolume(sourceSize))
				return PreparationResult.failure(ErrorCode.EMPTY_TEMPLATE,
						"Structure template " + templateId + " has no placeable volume.");

			Vec3i rotatedSize = template.getSize(rotation);
			BlockPos placementOrigin = template.getZeroPositionWithTransform(desiredWorldMinimum, MIRROR, rotation);
			StructurePlaceSettings settings = settings(rotation);
			BoundingBox minecraftBounds = template.getBoundingBox(settings, placementOrigin);
			WorldBounds worldBounds = WorldBounds.from(minecraftBounds);

			if (!worldBounds.min().equals(desiredWorldMinimum)
					|| worldBounds.sizeX() != rotatedSize.getX()
					|| worldBounds.sizeY() != rotatedSize.getY()
					|| worldBounds.sizeZ() != rotatedSize.getZ()) {
				return PreparationResult.failure(ErrorCode.TRANSFORM_MISMATCH,
						"Minecraft returned inconsistent rotated bounds for " + templateId + ".");
			}

			return PreparationResult.success(new PreparedTemplate(templateId, template, sourceSize,
					rotatedSize, rotation, desiredWorldMinimum, placementOrigin, worldBounds));
		} catch (RuntimeException exception) {
			return PreparationResult.failure(ErrorCode.LOAD_FAILED,
					"Could not load structure template " + templateId + ": " + exceptionMessage(exception));
		}
	}

	/** Prepares and immediately places a template. */
	public static PlacementResult place(ServerLevel level, ResourceLocation templateId,
			BlockPos desiredWorldMinimum, Rotation rotation) {
		PreparationResult preparation = prepare(level, templateId, desiredWorldMinimum, rotation);
		if (!preparation.success())
			return PlacementResult.failure(preparation.error(), preparation.message(), null);
		return place(level, preparation.template());
	}

	/**
	 * Places a previously prepared template. Structure block entities are placed,
	 * while free-standing saved entities are deliberately ignored.
	 */
	public static PlacementResult place(ServerLevel level, @Nullable PreparedTemplate prepared) {
		if (level == null || prepared == null)
			return PlacementResult.failure(ErrorCode.INVALID_ARGUMENT,
					"A level and prepared template are required.", prepared);

		try {
			boolean placed = prepared.template.placeInWorld(level, prepared.placementOrigin,
					prepared.placementOrigin, settings(prepared.rotation), level.random, PLACEMENT_FLAGS);
			if (!placed)
				return PlacementResult.failure(ErrorCode.PLACEMENT_REJECTED,
						"Minecraft rejected placement of structure template " + prepared.templateId + ".", prepared);
			return PlacementResult.success(prepared);
		} catch (RuntimeException exception) {
			return PlacementResult.failure(ErrorCode.PLACEMENT_FAILED,
					"Could not place structure template " + prepared.templateId + ": " + exceptionMessage(exception),
					prepared);
		}
	}

	private static StructurePlaceSettings settings(Rotation rotation) {
		return new StructurePlaceSettings()
				.setMirror(MIRROR)
				.setRotation(rotation)
				.setRotationPivot(ROTATION_PIVOT)
				.setIgnoreEntities(true);
	}

	private static boolean hasVolume(Vec3i size) {
		return size.getX() > 0 && size.getY() > 0 && size.getZ() > 0;
	}

	private static String exceptionMessage(RuntimeException exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
	}

	public enum ErrorCode {
		NONE,
		INVALID_ARGUMENT,
		MISSING_TEMPLATE,
		EMPTY_TEMPLATE,
		LOAD_FAILED,
		TRANSFORM_MISMATCH,
		PLACEMENT_REJECTED,
		PLACEMENT_FAILED
	}

	public record PreparationResult(boolean success, ErrorCode error, String message,
			@Nullable PreparedTemplate template) {
		private static PreparationResult success(PreparedTemplate template) {
			return new PreparationResult(true, ErrorCode.NONE, "Prepared " + template.templateId + ".", template);
		}

		private static PreparationResult failure(ErrorCode error, String message) {
			return new PreparationResult(false, error, message, null);
		}
	}

	public record PlacementResult(boolean success, ErrorCode error, String message,
			@Nullable PreparedTemplate template) {
		private static PlacementResult success(PreparedTemplate template) {
			return new PlacementResult(true, ErrorCode.NONE, "Placed " + template.templateId + ".", template);
		}

		private static PlacementResult failure(ErrorCode error, String message,
				@Nullable PreparedTemplate template) {
			return new PlacementResult(false, error, message, template);
		}
	}

	/** Inclusive, normalized bounds in a template's unrotated coordinate space. */
	public record RelativeBounds(BlockPos min, BlockPos max) {
		public RelativeBounds {
			if (min == null || max == null)
				throw new IllegalArgumentException("Relative bounds require two positions.");
			BlockPos normalizedMin = new BlockPos(Math.min(min.getX(), max.getX()),
					Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
			BlockPos normalizedMax = new BlockPos(Math.max(min.getX(), max.getX()),
					Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
			min = normalizedMin;
			max = normalizedMax;
		}
	}

	/** Inclusive axis-aligned world bounds. */
	public record WorldBounds(BlockPos min, BlockPos max) {
		public WorldBounds {
			if (min == null || max == null)
				throw new IllegalArgumentException("World bounds require two positions.");
			BlockPos normalizedMin = new BlockPos(Math.min(min.getX(), max.getX()),
					Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
			BlockPos normalizedMax = new BlockPos(Math.max(min.getX(), max.getX()),
					Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
			min = normalizedMin;
			max = normalizedMax;
		}

		private static WorldBounds from(BoundingBox bounds) {
			return new WorldBounds(new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ()),
					new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ()));
		}

		public int sizeX() {
			return max.getX() - min.getX() + 1;
		}

		public int sizeY() {
			return max.getY() - min.getY() + 1;
		}

		public int sizeZ() {
			return max.getZ() - min.getZ() + 1;
		}

		public boolean intersects(WorldBounds other) {
			return max.getX() >= other.min.getX() && min.getX() <= other.max.getX()
					&& max.getY() >= other.min.getY() && min.getY() <= other.max.getY()
					&& max.getZ() >= other.min.getZ() && min.getZ() <= other.max.getZ();
		}
	}

	/** A socket opening after applying the room's placement transform. */
	public record TransformedSocket(WorldBounds opening, Direction facing) {
	}

	/**
	 * Immutable prepared placement. The wrapped vanilla template is intentionally
	 * private; callers receive only placement and transform operations.
	 */
	public static final class PreparedTemplate {
		private final ResourceLocation templateId;
		private final StructureTemplate template;
		private final Vec3i sourceSize;
		private final Vec3i rotatedSize;
		private final Rotation rotation;
		private final BlockPos desiredWorldMinimum;
		private final BlockPos placementOrigin;
		private final WorldBounds worldBounds;
		@Nullable private List<BlockPos> transformedBlockEntityPositions;

		private PreparedTemplate(ResourceLocation templateId, StructureTemplate template, Vec3i sourceSize,
				Vec3i rotatedSize, Rotation rotation, BlockPos desiredWorldMinimum,
				BlockPos placementOrigin, WorldBounds worldBounds) {
			this.templateId = templateId;
			this.template = template;
			this.sourceSize = sourceSize;
			this.rotatedSize = rotatedSize;
			this.rotation = rotation;
			this.desiredWorldMinimum = desiredWorldMinimum;
			this.placementOrigin = placementOrigin;
			this.worldBounds = worldBounds;
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

		/**
		 * Returns block-entity positions encoded by this template after applying its
		 * exact placement transform. The list is derived lazily from vanilla structure
		 * NBT so corridor preflight can reject destructive doorway/path carving before
		 * any room blocks are placed.
		 */
		public List<BlockPos> transformedBlockEntityPositions() {
			if (transformedBlockEntityPositions != null)
				return transformedBlockEntityPositions;
			CompoundTag saved = template.save(new CompoundTag());
			ListTag blocks = saved.getList("blocks", Tag.TAG_COMPOUND);
			List<BlockPos> result = new ArrayList<>();
			for (int index = 0; index < blocks.size(); index++) {
				CompoundTag block = blocks.getCompound(index);
				if (!block.contains("nbt", Tag.TAG_COMPOUND))
					continue;
				ListTag position = block.getList("pos", Tag.TAG_INT);
				if (position.size() < 3)
					continue;
				result.add(transformRelative(new BlockPos(position.getInt(0), position.getInt(1),
						position.getInt(2))));
			}
			transformedBlockEntityPositions = List.copyOf(result);
			return transformedBlockEntityPositions;
		}

		/** Returns whether a point is inside the source template's normalized volume. */
		public boolean containsRelative(BlockPos relative) {
			return relative != null
					&& relative.getX() >= 0 && relative.getX() < sourceSize.getX()
					&& relative.getY() >= 0 && relative.getY() < sourceSize.getY()
					&& relative.getZ() >= 0 && relative.getZ() < sourceSize.getZ();
		}

		/**
		 * Transforms a normalized source position into the exact world position used
		 * by vanilla structure placement. Use this for markers and socket corners.
		 */
		public BlockPos transformRelative(BlockPos relative) {
			if (relative == null)
				throw new IllegalArgumentException("Relative position is required.");
			BlockPos rotated = StructureTemplate.transform(relative, MIRROR, rotation, ROTATION_PIVOT);
			return placementOrigin.offset(rotated);
		}

		/** Transforms a source-facing direction with the room's rotation. */
		public Direction transformDirection(Direction sourceDirection) {
			if (sourceDirection == null)
				throw new IllegalArgumentException("Source direction is required.");
			return rotation.rotate(sourceDirection);
		}

		/** Transforms an inclusive source-space rectangle or volume into world bounds. */
		public WorldBounds transformRelativeBounds(RelativeBounds relativeBounds) {
			if (relativeBounds == null)
				throw new IllegalArgumentException("Relative bounds are required.");
			return new WorldBounds(transformRelative(relativeBounds.min()),
					transformRelative(relativeBounds.max()));
		}

		/** Transforms a socket opening and its outward direction as one value. */
		public TransformedSocket transformSocket(RelativeBounds opening, Direction sourceFacing) {
			return new TransformedSocket(transformRelativeBounds(opening), transformDirection(sourceFacing));
		}
	}
}
