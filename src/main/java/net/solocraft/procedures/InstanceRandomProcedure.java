package net.solocraft.procedures;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class InstanceRandomProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double Xoff = 0;
		double Zoff = 0;
		double component = 0;
		double Yoff = 0;
		double rand1 = 0;
		Xoff = entity.getX() + -5;
		Yoff = entity.getY() - 2;
		Zoff = entity.getZ() + -5;
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "instancestart"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
						_serverworld.random, 3);
			}
		}
		Xoff = Xoff + 27;
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "dunduninstance1"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
						_serverworld.random, 3);
			}
		}
		Xoff = Xoff + 25;
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "dunduninstance2"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
						_serverworld.random, 3);
			}
		}
		Xoff = Xoff + 25;
		Yoff = Yoff - 11;
		Zoff = Zoff - 1;
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "instanceboss"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
						_serverworld.random, 3);
			}
		}
	}
}
