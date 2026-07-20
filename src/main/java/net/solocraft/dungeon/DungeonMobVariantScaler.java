package net.solocraft.dungeon;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

/** Applies the same synchronized factor to a supported mob's model and hitbox. */
public final class DungeonMobVariantScaler {
	public static final String SCALE_TAG = "slr_dungeon_variant_scale";

	private DungeonMobVariantScaler() {
	}

	public static void applyForRank(Entity entity, ProceduralDungeonRank rank,
			RandomSource random) {
		if (rank == null)
			return;
		float base = switch (rank) {
			case E -> 0.96F;
			case D -> 1.03F;
			case C -> 1.14F;
			case B -> 1.28F;
			case A -> 1.43F;
			case S -> 1.58F;
		};
		apply(entity, varied(base, random));
	}

	public static void applyForLevel(Entity entity, int level, RandomSource random) {
		float base;
		if (level >= 100)
			base = 1.58F;
		else if (level >= 75)
			base = 1.43F;
		else if (level >= 50)
			base = 1.28F;
		else if (level >= 30)
			base = 1.14F;
		else if (level >= 15)
			base = 1.03F;
		else
			base = 0.96F;
		apply(entity, varied(base, random));
	}

	private static float varied(float base, RandomSource random) {
		float variance = random == null ? 0.0F : (random.nextFloat() - 0.5F) * 0.12F;
		return Mth.clamp(base + variance, 0.90F, 1.68F);
	}

	private static void apply(Entity entity, float scale) {
		if (!(entity instanceof DungeonScalableEntity scalable))
			return;
		float safeScale = Mth.clamp(scale, 0.90F, 1.68F);
		scalable.setDungeonScale(safeScale);
		entity.getPersistentData().putFloat(SCALE_TAG, safeScale);
		entity.refreshDimensions();
	}
}
