package net.solocraft.dkc;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/** Canonical floor metadata and the single shared Demon King's Castle level. */
public final class DkcFloorRegistry {
	public static final int FIRST_FLOOR = 1;
	public static final int LAST_FLOOR = 20;
	public static final int BUILD_Y = 29;
	public static final int SURFACE_Y = BUILD_Y + 2;
	public static final ResourceLocation SHARED_DIMENSION_ID =
			new ResourceLocation("sololeveling", "dungeon_dimension_dkc");
	public static final ResourceKey<Level> SHARED_DIMENSION =
			ResourceKey.create(Registries.DIMENSION, SHARED_DIMENSION_ID);

	private static final String[] NAMES = {
			"", "Ashen Threshold", "Cinder Streets", "Charred Gardens", "Broken Aqueduct",
			"Furnace Ward", "Iron Foundry", "Blood Market", "Chain Barracks", "Magma Reservoir",
			"Vulcan's Crucible", "Obsidian Causeway", "Black Cathedral", "Demon Archives",
			"Furnace Spires", "Radiru Castle", "Storm Ramparts", "Thunder Galleries",
			"Crown Approach", "Sky Throne Antechamber", "Baran's Tempest Throne"
	};

	private static final int[] REQUIRED_KILLS = {
			0, 0, 12, 14, 16, 18, 20, 22, 24, 26, 32,
			22, 24, 26, 28, 30, 30, 32, 34, 36, 0
	};

	private static final int[] ACTIVE_CAPS = {
			0, 0, 7, 7, 8, 8, 8, 9, 9, 9, 10,
			9, 9, 9, 10, 10, 10, 11, 11, 12, 0
	};

	private static final Map<ResourceLocation, Integer> LEGACY_FLOORS_BY_ID;

	static {
		Map<ResourceLocation, Integer> floors = new HashMap<>();
		for (int floor = FIRST_FLOOR; floor <= LAST_FLOOR; floor++)
			floors.put(legacyDimensionId(floor), floor);
		LEGACY_FLOORS_BY_ID = Map.copyOf(floors);
	}

	private DkcFloorRegistry() {
	}

	public static ResourceLocation dimensionId(int floor) {
		validateFloor(floor);
		return SHARED_DIMENSION_ID;
	}

	/** Old per-floor IDs are recognized only so players saved there can migrate. */
	public static ResourceLocation legacyDimensionId(int floor) {
		validateFloor(floor);
		return floor == 1 ? SHARED_DIMENSION_ID
				: new ResourceLocation("sololeveling", String.format("dungeon_dimension_dkc_f%02d", floor));
	}

	public static ResourceLocation biomeId(int floor) {
		validateFloor(floor);
		return floor == 1
				? new ResourceLocation("sololeveling", "dungeon_biome_dkc")
				: new ResourceLocation("sololeveling", String.format("dungeon_biome_dkc_f%02d", floor));
	}

	public static ResourceKey<Level> dimension(int floor) {
		validateFloor(floor);
		return SHARED_DIMENSION;
	}

	/**
	 * Legacy dimension-derived floor lookup. The shared level can only identify
	 * floor 1 here; live gameplay must use {@link DkcSpatialLayout}.
	 */
	public static int floor(ResourceKey<Level> dimension) {
		return legacyFloor(dimension);
	}

	public static int floor(LevelAccessor level) {
		return level instanceof Level actual ? legacyFloor(actual.dimension()) : 0;
	}

	public static int legacyFloor(ResourceKey<Level> dimension) {
		return dimension == null ? 0 : LEGACY_FLOORS_BY_ID.getOrDefault(dimension.location(), 0);
	}

	public static boolean isDkc(ResourceKey<Level> dimension) {
		return legacyFloor(dimension) != 0;
	}

	public static boolean isDkc(@Nullable LevelAccessor level) {
		return level instanceof Level actual && isDkc(actual.dimension());
	}

	public static boolean isSharedDkc(ResourceKey<Level> dimension) {
		return dimension != null && dimension.location().equals(SHARED_DIMENSION_ID);
	}

	public static boolean isSharedDkc(@Nullable LevelAccessor level) {
		return level instanceof Level actual && isSharedDkc(actual.dimension());
	}

	public static String name(int floor) {
		validateFloor(floor);
		return NAMES[floor];
	}

	public static int requiredKills(int floor) {
		validateFloor(floor);
		return REQUIRED_KILLS[floor];
	}

	public static int activeEnemyCap(int floor) {
		validateFloor(floor);
		return ACTIVE_CAPS[floor];
	}

	public static boolean isBossFloor(int floor) {
		return floor == 1 || floor == 10 || floor == 20;
	}

	/** Upper floors progressively replace ordinary demons with demon knights. */
	public static float knightShare(int floor) {
		if (floor <= 10)
			return 0.0F;
		return Math.min(0.72F, 0.18F + (floor - 11) * 0.0675F);
	}

	private static void validateFloor(int floor) {
		if (floor < FIRST_FLOOR || floor > LAST_FLOOR)
			throw new IllegalArgumentException("DKC floor must be between 1 and 20: " + floor);
	}
}
