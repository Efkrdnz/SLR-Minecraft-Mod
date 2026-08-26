package net.solocraft.procedures;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class DungeonNostalgiaRightclickedProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankroom1"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 60, z), BlockPos.containing(x, y + 60, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankroom2"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 75, z), BlockPos.containing(x, y + 75, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankroom3"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 90, z), BlockPos.containing(x, y + 90, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankroom4"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 105, z), BlockPos.containing(x, y + 105, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankstart"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 120, z), BlockPos.containing(x, y + 120, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankbig1"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 135, z), BlockPos.containing(x, y + 135, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankbig2"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 150, z), BlockPos.containing(x, y + 150, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankboss"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 165, z), BlockPos.containing(x, y + 165, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankleftrightand"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 180, z), BlockPos.containing(x, y + 180, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankrightleftand"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x, y + 195, z), BlockPos.containing(x, y + 195, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "bigroom1"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x + 100, y, z), BlockPos.containing(x + 100, y, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "bigroom2"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x + 150, y, z), BlockPos.containing(x + 150, y, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "bigroom3"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x + 200, y, z), BlockPos.containing(x + 200, y, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "bigroom4"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x + 250, y, z), BlockPos.containing(x + 250, y, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "bigroom5"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(x + 300, y, z), BlockPos.containing(x + 300, y, z), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random,
						2);
			}
		}
	}
}
