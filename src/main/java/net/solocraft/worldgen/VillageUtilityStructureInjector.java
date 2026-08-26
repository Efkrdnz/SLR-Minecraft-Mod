package net.solocraft.worldgen;

import com.mojang.datafixers.util.Either;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.List;
import java.util.Optional;

/**
 * Adds the two Solo Leveling public-service buildings to the generated piece
 * list of every vanilla village. Keeping them in the village StructureStart
 * means they are generated, saved, located, and chunk-clipped as village
 * pieces instead of being placed later by a chunk-load event.
 */
public final class VillageUtilityStructureInjector {
	private static final ResourceLocation INSTANCE_ENTRANCE =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "istanceenterance");
	private static final ResourceLocation EVALUATOR =
			ResourceLocation.fromNamespaceAndPath("sololeveling", "evaluation");
	private static final Holder<StructureProcessorList> PROCESSORS = Holder.direct(
			new StructureProcessorList(List.of(
					BlockIgnoreProcessor.STRUCTURE_BLOCK)));
	private static final int[] SEARCH_RADII = {
			28, 34, 40, 46, 52, 58, 64, 72, 80, 92
	};
	private static final int DIRECTION_COUNT = 16;
	private static final int PIECE_CLEARANCE = 3;
	private static final int MAX_PREFERRED_SLOPE = 4;

	private VillageUtilityStructureInjector() {
	}

	public static boolean isVanillaVillageStartPool(
			Holder<StructureTemplatePool> startPool) {
		if (startPool == null)
			return false;
		return startPool.unwrapKey().map(key -> {
			ResourceLocation id = key.location();
			String path = id.getPath();
			return "minecraft".equals(id.getNamespace())
					&& path.startsWith("village/")
					&& path.endsWith("/town_centers");
		}).orElse(false);
	}

	public static Optional<Structure.GenerationStub> inject(
			Structure.GenerationContext context,
			Holder<StructureTemplatePool> startPool,
			Optional<Structure.GenerationStub> generated) {
		if (!isVanillaVillageStartPool(startPool) || generated.isEmpty())
			return generated;

		Structure.GenerationStub original = generated.get();
		StructurePiecesBuilder pieces = original.getPiecesBuilder();
		RandomSource random = RandomSource.create(villageSeed(context));
		int directionOffset = random.nextInt(DIRECTION_COUNT);
		int radiusOffset = random.nextInt(SEARCH_RADII.length);

		addPiece(context, pieces, EVALUATOR, directionOffset, radiusOffset, 0);
		addPiece(context, pieces, INSTANCE_ENTRANCE,
				directionOffset + DIRECTION_COUNT / 2,
				radiusOffset + SEARCH_RADII.length / 3, 1);

		return Optional.of(new Structure.GenerationStub(original.position(),
				Either.right(pieces)));
	}

	private static void addPiece(Structure.GenerationContext context,
			StructurePiecesBuilder pieces, ResourceLocation templateId,
			int directionOffset, int radiusOffset, int pieceIndex) {
		StructureTemplateManager templates = context.structureTemplateManager();
		StructurePoolElement element = StructurePoolElement.single(
				templateId.toString(), PROCESSORS)
				.apply(StructureTemplatePool.Projection.RIGID);
		Placement bestFallback = null;

		for (int radiusIndex = 0; radiusIndex < SEARCH_RADII.length;
				radiusIndex++) {
			int radius = SEARCH_RADII[Math.floorMod(
					radiusIndex + radiusOffset, SEARCH_RADII.length)];
			for (int directionIndex = 0; directionIndex < DIRECTION_COUNT;
					directionIndex++) {
				int direction = Math.floorMod(directionIndex + directionOffset
						+ radiusIndex * 3, DIRECTION_COUNT);
				double angle = direction * Math.PI * 2.0D / DIRECTION_COUNT;
				int centerX = context.chunkPos().getMiddleBlockX()
						+ (int) Math.round(Math.cos(angle) * radius);
				int centerZ = context.chunkPos().getMiddleBlockZ()
						+ (int) Math.round(Math.sin(angle) * radius);
				Rotation rotation = rotationFacingVillage(direction);
				Placement placement = placementAt(context, templates, element,
						rotation, centerX, centerZ);
				if (placement == null || collides(pieces, placement.box()))
					continue;
				if (bestFallback == null
						|| placement.slope() < bestFallback.slope())
					bestFallback = placement;
				if (placement.slope() <= MAX_PREFERRED_SLOPE
						&& placement.waterDepth() <= 1) {
					add(pieces, templates, element, rotation, placement);
					return;
				}
			}
		}

		if (bestFallback != null) {
			add(pieces, templates, element, bestFallback.rotation(),
					bestFallback);
			return;
		}

		// A pathologically dense modded village can occupy every preferred
		// candidate. Use a deterministic distant fallback so the one-per-village
		// contract is still honored instead of silently omitting a building.
		int sign = pieceIndex == 0 ? 1 : -1;
		int centerX = context.chunkPos().getMiddleBlockX() + sign * 112;
		int centerZ = context.chunkPos().getMiddleBlockZ() - sign * 48;
		Rotation rotation = sign > 0
				? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90;
		Placement fallback = placementAt(context, templates, element, rotation,
				centerX, centerZ);
		if (fallback != null)
			add(pieces, templates, element, rotation, fallback);
	}

	private static Placement placementAt(Structure.GenerationContext context,
			StructureTemplateManager templates, StructurePoolElement element,
			Rotation rotation, int desiredCenterX, int desiredCenterZ) {
		BoundingBox zeroBox = element.getBoundingBox(templates, BlockPos.ZERO,
				rotation);
		int originX = desiredCenterX
				- Math.floorDiv(zeroBox.minX() + zeroBox.maxX(), 2);
		int originZ = desiredCenterZ
				- Math.floorDiv(zeroBox.minZ() + zeroBox.maxZ(), 2);
		int minX = originX + zeroBox.minX();
		int maxX = originX + zeroBox.maxX();
		int minZ = originZ + zeroBox.minZ();
		int maxZ = originZ + zeroBox.maxZ();
		int[] heights = {
			surfaceHeight(context, desiredCenterX, desiredCenterZ),
			surfaceHeight(context, minX, minZ),
			surfaceHeight(context, minX, maxZ),
			surfaceHeight(context, maxX, minZ),
			surfaceHeight(context, maxX, maxZ)
		};
		int minHeight = Integer.MAX_VALUE;
		int maxHeight = Integer.MIN_VALUE;
		for (int height : heights) {
			minHeight = Math.min(minHeight, height);
			maxHeight = Math.max(maxHeight, height);
		}
		int groundY = heights[0];
		int oceanFloor = context.chunkGenerator().getBaseHeight(
				desiredCenterX, desiredCenterZ,
				Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(),
				context.randomState());
		BlockPos origin = new BlockPos(originX, groundY, originZ);
		BoundingBox box = element.getBoundingBox(templates, origin, rotation);
		return new Placement(origin, box, rotation, maxHeight - minHeight,
				Math.max(0, groundY - oceanFloor));
	}

	private static int surfaceHeight(Structure.GenerationContext context,
			int x, int z) {
		return context.chunkGenerator().getBaseHeight(x, z,
				Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(),
				context.randomState());
	}

	private static boolean collides(StructurePiecesBuilder pieces,
			BoundingBox candidate) {
		return pieces.findCollisionPiece(
				candidate.inflatedBy(PIECE_CLEARANCE)) != null;
	}

	private static void add(StructurePiecesBuilder pieces,
			StructureTemplateManager templates, StructurePoolElement element,
			Rotation rotation, Placement placement) {
		pieces.addPiece(new PoolElementStructurePiece(templates, element,
				placement.origin(), element.getGroundLevelDelta(), rotation,
				placement.box(), LiquidSettings.APPLY_WATERLOGGING));
	}

	private static Rotation rotationFacingVillage(int direction) {
		int quadrant = Math.floorMod((direction + 2) / 4, 4);
		return switch (quadrant) {
			case 0 -> Rotation.COUNTERCLOCKWISE_90;
			case 1 -> Rotation.CLOCKWISE_180;
			case 2 -> Rotation.CLOCKWISE_90;
			default -> Rotation.NONE;
		};
	}

	private static long villageSeed(Structure.GenerationContext context) {
		long chunk = context.chunkPos().toLong();
		return context.seed() ^ Long.rotateLeft(chunk, 21)
				^ 0x534C5256494C4C41L;
	}

	private record Placement(BlockPos origin, BoundingBox box,
			Rotation rotation, int slope, int waterDepth) {
	}
}
