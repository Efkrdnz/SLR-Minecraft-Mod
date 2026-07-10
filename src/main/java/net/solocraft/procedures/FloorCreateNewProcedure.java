package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;

public class FloorCreateNewProcedure {
	/** bossType: "normal" | "vulcan" | "cerberus" | "baran" */
	public static void execute(LevelAccessor world, Entity entity, String bossType) {
		if (entity == null)
			return;
		String middleTemplate = switch (bossType) {
			case "vulcan"   -> "dkc_middle_boss";
			case "cerberus" -> "dkc_middle_boss_cerberus";
			case "baran"    -> "dkc_middle_boss_baran";
			default         -> "dkc_middle";
		};
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "dkc_left"));
			if (template != null) {
				template.placeInWorld(_serverworld,
						BlockPos.containing((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z
										+ (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared * 200),
						BlockPos.containing((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z
										+ (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared * 200),
						new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", middleTemplate));
			if (template != null) {
				template.placeInWorld(_serverworld,
						BlockPos.containing((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z + 56
										+ (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared * 200),
						BlockPos.containing((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z + 56
										+ (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared * 200),
						new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
			}
		}
		if (world instanceof ServerLevel _serverworld) {
			StructureTemplate template = _serverworld.getStructureManager().getOrCreate(new ResourceLocation("sololeveling", "dkc_right"));
			if (template != null) {
				template.placeInWorld(_serverworld,
						BlockPos.containing((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z + 133
										+ (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared * 200),
						BlockPos.containing((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_x,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_y,
								(entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_z + 133
										+ (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).dkc_cleared * 200),
						new StructurePlaceSettings().setRotation(Rotation.NONE).setMirror(Mirror.NONE).setIgnoreEntities(false), _serverworld.random, 3);
			}
		}
	}

	/** Convenience overload for the normal (non-boss) floor layout. */
	public static void execute(LevelAccessor world, Entity entity) {
		execute(world, entity, "normal");
	}
}
