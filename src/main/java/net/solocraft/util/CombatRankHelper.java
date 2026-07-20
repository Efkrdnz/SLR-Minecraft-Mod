package net.solocraft.util;

import net.solocraft.entity.HunterEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.biome.Biome;

/** Resolves the E-S combat rank used by rank-limited status effects. */
public final class CombatRankHelper {
	private static final TagKey<Biome> E_RANK_BIOME = biomeTag("dune");
	private static final TagKey<Biome> D_RANK_BIOME = biomeTag("dund");
	private static final TagKey<Biome> C_RANK_BIOME = biomeTag("dunc");
	private static final TagKey<Biome> B_RANK_BIOME = biomeTag("dunb");
	private static final TagKey<Biome> A_RANK_BIOME = biomeTag("duna");
	private static final TagKey<Biome> S_RANK_BIOME = biomeTag("duns");
	private static final TagKey<EntityType<?>> HIGH_TIER = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("hightier"));

	private CombatRankHelper() {
	}

	public static boolean isAtMost(Entity entity, int maximumRank) {
		return rankOf(entity) <= maximumRank;
	}

	/** Returns E=1, D=2, C=3, B=4, A=5, or S=6. */
	public static int rankOf(Entity entity) {
		if (entity == null)
			return 1;

		int explicitRank = explicitRank(entity);
		if (explicitRank > 0)
			return explicitRank;

		if (entity instanceof HunterEntity hunter) {
			int hunterRank = parseRank(hunter.getEntityData().get(HunterEntity.DATA_Rank));
			if (hunterRank > 0)
				return hunterRank;
		}

		var biome = entity.level().getBiome(entity.blockPosition());
		if (biome.is(E_RANK_BIOME))
			return 1;
		if (biome.is(D_RANK_BIOME))
			return 2;
		if (biome.is(C_RANK_BIOME))
			return 3;
		if (biome.is(B_RANK_BIOME))
			return 4;
		if (biome.is(A_RANK_BIOME))
			return 5;
		if (biome.is(S_RANK_BIOME))
			return 6;

		int dimensionRank = dimensionRank(entity.level().dimension().location().getPath());
		if (dimensionRank > 0)
			return dimensionRank;

		ResourceLocation entityId = EntityType.getKey(entity.getType());
		int knownRank = entityId == null ? 0 : knownEntityRank(entityId.getPath());
		if (knownRank > 0)
			return knownRank;
		if (entity.getType().is(HIGH_TIER))
			return 5;

		double level = entity.getPersistentData().getDouble("Level");
		if (level <= 0)
			return 2;
		if (level <= 30)
			return 3;
		if (level <= 55)
			return 4;
		if (level <= 80)
			return 5;
		return 6;
	}

	private static int explicitRank(Entity entity) {
		int numeric = entity.getPersistentData().getInt("slr_entity_rank");
		if (numeric >= 1 && numeric <= 6)
			return numeric;
		int named = parseRank(entity.getPersistentData().getString("slr_entity_rank"));
		if (named > 0)
			return named;
		return parseRank(entity.getPersistentData().getString("slr_procedural_rank"));
	}

	private static int dimensionRank(String path) {
		if (path != null && path.startsWith("monarch_territory_"))
			return 4;
		return switch (path) {
			case "dungeon_dimension_d" -> 2;
			case "dungeon_dimension_c", "dungeon_dimension_kasaka" -> 3;
			case "dungeon_dimension_b", "dungeon_dimension_snow", "dungeon_dimension_igris" -> 4;
			case "dungeon_dimension_a" -> 5;
			case "dungeon_dimension_s", "dungeon_dimension_dkc", "cartenon_temple", "survival_dimension" -> 6;
			default -> 0;
		};
	}

	private static int knownEntityRank(String path) {
		return switch (path) {
			case "kasaka", "fanged_kasaka", "goblin_king", "spider_boss", "ancient_golem" -> 3;
			case "igris", "blood_red_com_igris", "baruka", "skeleton_summoner" -> 4;
			case "gem_golem", "futuristic_golem", "kargalgan" -> 5;
			case "beru_boss", "kamish", "cerberus", "vulcan", "baran", "kaiselin", "statue_of_god" -> 6;
			default -> 0;
		};
	}

	private static int parseRank(String rank) {
		if (rank == null)
			return 0;
		return switch (rank.trim().toUpperCase()) {
			case "E" -> 1;
			case "D" -> 2;
			case "C" -> 3;
			case "B" -> 4;
			case "A" -> 5;
			case "S" -> 6;
			default -> 0;
		};
	}

	private static TagKey<Biome> biomeTag(String id) {
		return TagKey.create(Registries.BIOME, new ResourceLocation(id));
	}
}
