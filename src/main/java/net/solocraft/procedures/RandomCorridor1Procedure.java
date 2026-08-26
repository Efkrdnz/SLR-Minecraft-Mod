package net.solocraft.procedures;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class RandomCorridor1Procedure {
	private static final int ELITE_COMPONENT = 2;
	private static final int FINAL_COMBAT_COMPONENT = 4;

	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double Xoff = 0;
		double Zoff = 0;
		double component = 0;
		double Yoff = 0;
		double rand1 = 0;
		Xoff = entity.getX() + -5;
		Yoff = entity.getY() - 15;
		Zoff = entity.getZ() + -5;
		component = 0;
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankstart"));
			if (template != null) {
				template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
						_serverworld.random, 2);
			}
		}
		Xoff = Xoff + 26;
		for (int index0 = 0; index0 < 30; index0++) {
			if (component <= FINAL_COMBAT_COMPONENT
					&& component != ELITE_COMPONENT) {
				rand1 = Mth.nextInt(RandomSource.create(), 1, 5);
				if (rand1 <= 3) {
					rand1 = Mth.nextInt(RandomSource.create(), 1, 4);
					if (rand1 == 1) {
						if (world instanceof ServerLevel _serverworld) {
							StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankroom1"));
							if (template != null) {
								template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
										_serverworld.random, 2);
							}
						}
					} else if (rand1 == 2) {
						if (world instanceof ServerLevel _serverworld) {
							StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankroom2"));
							if (template != null) {
								template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
										_serverworld.random, 2);
							}
						}
					} else if (rand1 == 3) {
						if (world instanceof ServerLevel _serverworld) {
							StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankroom3"));
							if (template != null) {
								template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
										_serverworld.random, 2);
							}
						}
					} else if (rand1 == 4) {
						if (world instanceof ServerLevel _serverworld) {
							StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankroom4"));
							if (template != null) {
								template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
										_serverworld.random, 2);
							}
						}
					}
					Xoff = Xoff + 25;
					component = component + 1;
				} else if (rand1 == 4) {
					Zoff = Zoff - 25;
					if (world instanceof ServerLevel _serverworld) {
						StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankleftrightand"));
						if (template != null) {
							template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
									_serverworld.random, 2);
						}
					}
					component = component + 1;
					Xoff = Xoff + 25;
				} else if (rand1 == 5) {
					if (world instanceof ServerLevel _serverworld) {
						StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankrightleftand"));
						if (template != null) {
							template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
									_serverworld.random, 2);
						}
					}
					component = component + 1;
					Xoff = Xoff + 25;
					Zoff = Zoff + 25;
				}
			} else if (component == ELITE_COMPONENT) {
				// The lighter elite room keeps the dungeon's mid-run spike without
				// the four-knight pileup from erankbig1.
				if (world instanceof ServerLevel _serverworld) {
					StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankbig2"));
					if (template != null) {
						template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff - 8), BlockPos.containing(Xoff, Yoff, Zoff - 8),
								new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 2);
					}
				}
				Xoff = Xoff + 29;
				component = component + 1;
			} else {
				Zoff = Zoff - 8;
				if (world instanceof ServerLevel _serverworld) {
					StructureTemplate template = _serverworld.getStructureManager().getOrCreate(ResourceLocation.fromNamespaceAndPath("sololeveling", "erankboss"));
					if (template != null) {
						template.placeInWorld(_serverworld, BlockPos.containing(Xoff, Yoff, Zoff), BlockPos.containing(Xoff, Yoff, Zoff), new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false),
								_serverworld.random, 2);
					}
				}
				break;
			}
		}
	}
}
