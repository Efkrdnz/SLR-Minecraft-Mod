package net.solocraft.procedures;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class DungeonPlaceLargeProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		boolean asd = false;
		boolean found = false;
		boolean spawn = false;
		double xOff = 0;
		double zOff = 0;
		double yOff = 0;
		double Rand = 0;
		double sx = 0;
		double sy = 0;
		double sz = 0;
		double comp = 0;
		comp = 0;
		xOff = x - 30;
		yOff = y - 23;
		zOff = z - 25.5;
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "updatedlargerandstart"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(xOff, yOff, zOff), BlockPos.containing(xOff, yOff, zOff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
						_serverworld.random, 3);
			}
		}
		xOff = xOff + 49;
		for (int index0 = 0; index0 < 10; index0++) {
			comp = comp + 1;
			Rand = Mth.nextInt(RandomSource.create(), 1, 4);
			if (comp < 4) {
				if (Rand == 1) {
					if (world instanceof ServerLevel _serverworld) {
						StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "bigroom1"));
						if (template != null) {
							template.placeInWorld(_serverworld, BlockPos.containing(xOff, yOff, zOff), BlockPos.containing(xOff, yOff, zOff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
									_serverworld.random, 3);
						}
					}
					xOff = xOff + 49;
				}
				if (Rand == 2) {
					if (world instanceof ServerLevel _serverworld) {
						StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "bigroom2"));
						if (template != null) {
							template.placeInWorld(_serverworld, BlockPos.containing(xOff, yOff, zOff), BlockPos.containing(xOff, yOff, zOff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
									_serverworld.random, 3);
						}
					}
					xOff = xOff + 49;
				}
				if (Rand == 3) {
					if (world instanceof ServerLevel _serverworld) {
						StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "bigroom3"));
						if (template != null) {
							template.placeInWorld(_serverworld, BlockPos.containing(xOff, yOff, zOff), BlockPos.containing(xOff, yOff, zOff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
									_serverworld.random, 3);
						}
					}
					xOff = xOff + 49;
				}
				if (Rand == 4) {
					if (world instanceof ServerLevel _serverworld) {
						StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "bigroom4"));
						if (template != null) {
							template.placeInWorld(_serverworld, BlockPos.containing(xOff, yOff, zOff), BlockPos.containing(xOff, yOff, zOff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
									_serverworld.random, 3);
						}
					}
					xOff = xOff + 49;
				}
				if (Rand == 5) {
					if (world instanceof ServerLevel _serverworld) {
						StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "bigroom5"));
						if (template != null) {
							template.placeInWorld(_serverworld, BlockPos.containing(xOff, yOff, zOff), BlockPos.containing(xOff, yOff, zOff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
									_serverworld.random, 3);
						}
					}
					xOff = xOff + 49;
				}
			} else {
				if (world instanceof ServerLevel _serverworld) {
					StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "bigroomboss"));
					if (template != null) {
						template.placeInWorld(_serverworld, BlockPos.containing(xOff, yOff, zOff), BlockPos.containing(xOff, yOff, zOff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
								_serverworld.random, 3);
					}
				}
				break;
			}
		}
	}
}
