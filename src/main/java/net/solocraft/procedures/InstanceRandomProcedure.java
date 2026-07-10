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
		execute(world, entity.getX(), entity.getY(), entity.getZ());
	}

	public static void execute(LevelAccessor world, double x, double y, double z) {
		double Xoff = 0;
		double Zoff = 0;
		double component = 0;
		double Yoff = 0;
		double rand1 = 0;
		Xoff = x + -5;
		Yoff = y - 2;
		Zoff = z + -5;
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "instance_first_room"));
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
