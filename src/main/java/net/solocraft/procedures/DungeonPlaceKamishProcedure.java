package net.solocraft.procedures;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class DungeonPlaceKamishProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z) {
		double xOff = 0;
		double zOff = 0;
		double yOff = 0;
		double Rand = 0;
		double sx = 0;
		double sy = 0;
		double sz = 0;
		boolean asd = false;
		boolean found = false;
		boolean spawn = false;
		asd = false;
		spawn = true;
		xOff = x - 30;
		yOff = y - 23;
		zOff = z - 25.5;
		for (int index0 = 0; index0 < 50; index0++) {
			Rand = Math.random();
			if (spawn == false) {
				if (Rand < 0.33) {
					xOff = xOff + 49;
					asd = true;
				} else if (Rand < 0.67) {
					zOff = zOff + 49;
					asd = false;
				} else if (Rand < 1) {
					zOff = zOff - 49;
					asd = false;
				}
			} else if (spawn == true) {
				if (world instanceof ServerLevel _serverworld) {
					StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishupdatedstart"));
					if (template != null) {
						template.placeInWorld(_serverworld, BlockPos.containing(xOff - 24, yOff, zOff - 24), BlockPos.containing(xOff - 24, yOff, zOff - 24),
								new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
					}
				}
				xOff = xOff + 49;
				asd = false;
				spawn = false;
			}
			sx = -50;
			for (int index1 = 0; index1 < 100; index1++) {
				sy = -50;
				for (int index2 = 0; index2 < 100; index2++) {
					sz = -50;
					for (int index3 = 0; index3 < 100; index3++) {
						if ((world.getBlockState(BlockPos.containing(xOff + sx, yOff + sy, zOff + sz))).getBlock() == Blocks.OBSIDIAN) {
							if ((world.getBlockState(BlockPos.containing(xOff + sx + 27, (yOff + sy) - 5, zOff + sz))).getBlock() == Blocks.AIR) {
								if (world instanceof ServerLevel _serverworld) {
									StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishblock"));
									if (template != null) {
										template.placeInWorld(_serverworld, BlockPos.containing(xOff + sx + 25, yOff + sy + 3, (zOff + sz) - 1), BlockPos.containing(xOff + sx + 25, yOff + sy + 3, (zOff + sz) - 1),
												new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
									}
								}
							}
							if ((world.getBlockState(BlockPos.containing((xOff + sx) - 27, (yOff + sy) - 5, zOff + sz))).getBlock() == Blocks.AIR) {
								if (world instanceof ServerLevel _serverworld) {
									StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishblock"));
									if (template != null) {
										template.placeInWorld(_serverworld, BlockPos.containing((xOff + sx) - 26, yOff + sy + 3, (zOff + sz) - 1), BlockPos.containing((xOff + sx) - 26, yOff + sy + 3, (zOff + sz) - 1),
												new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
									}
								}
							}
							if ((world.getBlockState(BlockPos.containing(xOff + sx, yOff + sy + 10, (zOff + sz) - (-27)))).getBlock() == Blocks.AIR) {
								if (world instanceof ServerLevel _serverworld) {
									StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishblock"));
									if (template != null) {
										template.placeInWorld(_serverworld, BlockPos.containing((xOff + sx) - 1, yOff + sy + 3, (zOff + sz) - 26), BlockPos.containing((xOff + sx) - 1, yOff + sy + 3, (zOff + sz) - 26),
												new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
									}
								}
							}
							if ((world.getBlockState(BlockPos.containing(xOff + sx, (yOff + sy) - 5, zOff + sz + 27))).getBlock() == Blocks.AIR) {
								if (world instanceof ServerLevel _serverworld) {
									StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishblock"));
									if (template != null) {
										template.placeInWorld(_serverworld, BlockPos.containing((xOff + sx) - 1, yOff + sy + 3, zOff + sz + 25), BlockPos.containing((xOff + sx) - 1, yOff + sy + 3, zOff + sz + 25),
												new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
									}
								}
							}
						}
						sz = sz + 1;
					}
					sy = sy + 1;
				}
				sx = sx + 1;
			}
			if (!((world.getBlockState(BlockPos.containing(xOff, yOff, zOff))).getBlock() == Blocks.OBSIDIAN)) {
				Rand = Math.random();
				if (spawn == false) {
					if (asd == false) {
						if (Rand < 0.33) {
							if (world instanceof ServerLevel _serverworld) {
								StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishroom1"));
								if (template != null) {
									template.placeInWorld(_serverworld, BlockPos.containing(xOff - 24, yOff, zOff - 24), BlockPos.containing(xOff - 24, yOff, zOff - 24),
											new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
								}
							}
						} else if (Rand < 0.67) {
							if (world instanceof ServerLevel _serverworld) {
								StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishroom2"));
								if (template != null) {
									template.placeInWorld(_serverworld, BlockPos.containing(xOff - 24, yOff, zOff - 24), BlockPos.containing(xOff - 24, yOff, zOff - 24),
											new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
								}
							}
						} else if (Rand < 1) {
							if (world instanceof ServerLevel _serverworld) {
								StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishroom3"));
								if (template != null) {
									template.placeInWorld(_serverworld, BlockPos.containing(xOff - 24, yOff, zOff - 24), BlockPos.containing(xOff - 24, yOff, zOff - 24),
											new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
								}
							}
						}
					} else if (asd == true) {
						Rand = Math.random();
						if (Rand < 0.75) {
							Rand = Math.random();
							if (Rand < 0.33) {
								if (world instanceof ServerLevel _serverworld) {
									StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishroom1"));
									if (template != null) {
										template.placeInWorld(_serverworld, BlockPos.containing(xOff - 24, yOff, zOff - 24), BlockPos.containing(xOff - 24, yOff, zOff - 24),
												new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
									}
								}
							} else if (Rand < 0.67) {
								if (world instanceof ServerLevel _serverworld) {
									StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishroom2"));
									if (template != null) {
										template.placeInWorld(_serverworld, BlockPos.containing(xOff - 24, yOff, zOff - 24), BlockPos.containing(xOff - 24, yOff, zOff - 24),
												new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
									}
								}
							} else if (Rand < 1) {
								if (world instanceof ServerLevel _serverworld) {
									StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishroom3"));
									if (template != null) {
										template.placeInWorld(_serverworld, BlockPos.containing(xOff - 24, yOff, zOff - 24), BlockPos.containing(xOff - 24, yOff, zOff - 24),
												new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
									}
								}
							}
						} else if (Rand < 1) {
							if (world instanceof ServerLevel _serverworld) {
								StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "kamishboss"));
								if (template != null) {
									template.placeInWorld(_serverworld, BlockPos.containing(xOff - 24, yOff, zOff - 24), BlockPos.containing(xOff - 24, yOff, zOff - 24),
											new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
								}
							}
							break;
						}
					}
				}
			}
		}
	}
}
