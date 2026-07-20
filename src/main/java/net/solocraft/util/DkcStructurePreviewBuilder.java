package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.DKCTowerAuraEntity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Places connected, staged previews of the authored DKC rework modules. */
public final class DkcStructurePreviewBuilder {
	private static final String FLOOR_ONE = "dkcfloor1";
	private static final String LOWER_CITY = "dkclowercity";
	private static final String REWORK = "dkcrework";
	private static final int TICKS_PER_PIECE = 3;
	private static final Set<UUID> ACTIVE = ConcurrentHashMap.newKeySet();

	private DkcStructurePreviewBuilder() {
	}

	public static List<String> suggestions() {
		return List.of("dkc_floor_1", "dkc_lower_city", "dkc_rework");
	}

	public static boolean handles(String normalizedName) {
		return FLOOR_ONE.equals(normalizedName) || LOWER_CITY.equals(normalizedName) || REWORK.equals(normalizedName);
	}

	public static boolean start(ServerPlayer player, String normalizedName) {
		if (player == null || !handles(normalizedName) || !ACTIVE.add(player.getUUID()))
			return false;

		ServerLevel level = player.serverLevel();
		boolean includeFloorOne = !LOWER_CITY.equals(normalizedName);
		boolean includeLowerCity = !FLOOR_ONE.equals(normalizedName);
		int requiredHeight = includeFloorOne ? 240 : 24;
		if (level.getMaxBuildHeight() - level.getMinBuildHeight() < requiredHeight + 4) {
			ACTIVE.remove(player.getUUID());
			player.sendSystemMessage(Component.literal("This dimension is not tall enough for the selected DKC preview.")
					.withStyle(ChatFormatting.RED));
			return false;
		}

		int baseY = Math.max(level.getMinBuildHeight(), player.getBlockY() - 3);
		baseY = Math.min(baseY, level.getMaxBuildHeight() - requiredHeight);
		BlockPos origin = new BlockPos(player.getBlockX() + 16, baseY, player.getBlockZ() + 16);
		List<Placement> placements = new ArrayList<>();
		if (includeFloorOne)
			addFloorOne(placements, 0, 0);
		if (includeLowerCity)
			addLowerCity(placements, includeFloorOne ? 352 : 0, 0);

		player.sendSystemMessage(Component.literal("DKC structure preview started at " + origin.toShortString()
				+ " using " + placements.size() + " aligned modules.").withStyle(ChatFormatting.AQUA));
		player.sendSystemMessage(Component.literal("Source orientation is south to north; placement is staged to protect TPS.")
				.withStyle(ChatFormatting.DARK_GRAY));

		for (int index = 0; index < placements.size(); index++) {
			Placement placement = placements.get(index);
			boolean last = index == placements.size() - 1;
			SololevelingMod.queueServerWork(index * TICKS_PER_PIECE, () -> {
				try {
					place(level, origin, placement);
					if (last && includeFloorOne)
						placeTowerAura(level, origin);
				} finally {
					if (last) {
						ACTIVE.remove(player.getUUID());
						if (player.serverLevel() == level)
							player.sendSystemMessage(Component.literal("DKC structure preview complete.")
									.withStyle(ChatFormatting.GREEN));
					}
				}
			});
		}
		return true;
	}

	private static void addFloorOne(List<Placement> placements, int ox, int oz) {
		placements.add(piece("dkc_f1_arrival_plaza", ox, 0, oz));
		placements.add(piece("dkc_f1_approach_a", ox + 8, 0, oz + 48));
		for (int z = 0; z < 2; z++) {
			for (int x = 0; x < 2; x++)
				placements.add(piece("dkc_f1_cerberus_courtyard_x" + x + "_z" + z,
						ox - 16 + x * 40, 0, oz + 72 + z * 40));
		}

		int towerX = ox - 8;
		int towerZ = oz + 152;
		addTowerStage(placements, "base", towerX, 0, towerZ);
		addTowerStage(placements, "mid_a", towerX, 48, towerZ);
		addTowerStage(placements, "mid_b", towerX, 84, towerZ);
		addTowerStage(placements, "mid_c", towerX, 120, towerZ);
		addTowerStage(placements, "mid_d", towerX, 156, towerZ);
		addTowerStage(placements, "crown", towerX, 192, towerZ);
		placements.add(piece("dkc_tower_gate_closed", ox + 14, 0, towerZ));
		placements.add(piece("dkc_tower_lobby", ox + 6, 0, oz + 166));
		placements.add(piece("dkc_f1_ascension_chamber", ox + 8, 0, oz + 208));
	}

	private static void addTowerStage(List<Placement> placements, String stage, int x, int y, int z) {
		for (int tileZ = 0; tileZ < 2; tileZ++) {
			for (int tileX = 0; tileX < 2; tileX++)
				placements.add(piece("dkc_tower_" + stage + "_x" + tileX + "_z" + tileZ,
						x + tileX * 32, y, z + tileZ * 32));
		}
	}

	private static void addLowerCity(List<Placement> placements, int ox, int oz) {
		placements.add(piece("dkc_lower_start", ox + 4, 0, oz));
		placements.add(piece("dkc_lower_street_a", ox, 0, oz + 24));
		placements.add(piece("dkc_lower_intersection_four_way", ox, 0, oz + 48));
		placements.add(piece("dkc_lower_rune_plaza_branch", ox - 32, 0, oz + 48, Rotation.CLOCKWISE_90));
		// Even-sized counterclockwise rotation shifts the source center down one block.
		placements.add(piece("dkc_lower_rune_plaza_branch", ox + 32, 0, oz + 49, Rotation.COUNTERCLOCKWISE_90));
		placements.add(piece("dkc_lower_rune_plaza_through", ox, 0, oz + 80));
		placements.add(piece("dkc_lower_street_b", ox, 0, oz + 112));
		placements.add(piece("dkc_lower_transition", ox, 0, oz + 136));
	}

	private static Placement piece(String name, int x, int y, int z) {
		return piece(name, x, y, z, Rotation.NONE);
	}

	private static Placement piece(String name, int x, int y, int z, Rotation rotation) {
		return new Placement(new ResourceLocation(SololevelingMod.MODID, name), x, y, z, rotation);
	}

	private static void place(ServerLevel level, BlockPos origin, Placement placement) {
		StructureTemplate template = level.getStructureManager().getOrCreate(placement.template());
		Vec3i size = template.getSize();
		if (size.getX() <= 0 || size.getY() <= 0 || size.getZ() <= 0)
			return;
		BlockPos desiredMin = origin.offset(placement.x(), placement.y(), placement.z());
		BlockPos templateOrigin = rotationOrigin(desiredMin, size, placement.rotation());
		StructurePlaceSettings settings = new StructurePlaceSettings()
				.setRotation(placement.rotation()).setMirror(Mirror.NONE).setIgnoreEntities(true);
		template.placeInWorld(level, templateOrigin, templateOrigin, settings, level.random, 3);
		for (StructureTemplate.StructureBlockInfo marker : template.filterBlocks(templateOrigin, settings, Blocks.STRUCTURE_BLOCK))
			level.setBlock(marker.pos(), Blocks.AIR.defaultBlockState(), 3);
	}

	private static void placeTowerAura(ServerLevel level, BlockPos origin) {
		BlockPos anchor = origin.offset(24, 0, 184);
		AABB duplicateArea = new AABB(anchor.getX() - 4.0D, anchor.getY() - 8.0D, anchor.getZ() - 4.0D,
				anchor.getX() + 4.0D, anchor.getY() + 8.0D, anchor.getZ() + 4.0D);
		level.getEntitiesOfClass(DKCTowerAuraEntity.class, duplicateArea).forEach(DKCTowerAuraEntity::discard);
		DKCTowerAuraEntity aura = DKCTowerAuraEntity.spawn(level, anchor.getX(), anchor.getY(), anchor.getZ(),
				32.0F, 320.0F, 1.0F);
		aura.setCrownLightning(true);
	}

	private static BlockPos rotationOrigin(BlockPos desiredMin, Vec3i size, Rotation rotation) {
		return switch (rotation) {
			case CLOCKWISE_90 -> desiredMin.offset(size.getZ() - 1, 0, 0);
			case CLOCKWISE_180 -> desiredMin.offset(size.getX() - 1, 0, size.getZ() - 1);
			case COUNTERCLOCKWISE_90 -> desiredMin.offset(0, 0, size.getX() - 1);
			default -> desiredMin;
		};
	}

	private record Placement(ResourceLocation template, int x, int y, int z, Rotation rotation) {
	}
}
