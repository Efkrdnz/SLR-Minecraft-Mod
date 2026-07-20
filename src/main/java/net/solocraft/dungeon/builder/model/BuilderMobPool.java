package net.solocraft.dungeon.builder.model;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;

/**
 * Immutable, lossless authoring form of a mob pool.
 *
 * <p>Unlike the runtime pool loader, this model keeps entries whose optional
 * mods are not currently installed. That lets a creator edit and re-export an
 * addon without silently losing its optional integrations.</p>
 */
public record BuilderMobPool(ResourceLocation id, List<Entry> entries) {
	public BuilderMobPool {
		if (id == null)
			throw new IllegalArgumentException("Mob-pool id is required.");
		entries = entries == null ? List.of() : List.copyOf(entries);
	}

	public enum SelectorKind {
		ENTITY,
		TAG
	}

	public record LevelRange(int min, int max) {
		public LevelRange {
			if (min > max) {
				int swap = min;
				min = max;
				max = swap;
			}
		}
	}

	public record Entry(SelectorKind selectorKind, ResourceLocation selector, int weight,
			Optional<String> requiredMod, Optional<LevelRange> eligibleLevel,
			Optional<LevelRange> spawnLevel, Optional<Integer> baseXp) {
		public Entry {
			if (selectorKind == null)
				throw new IllegalArgumentException("Selector kind is required.");
			if (selector == null)
				throw new IllegalArgumentException("Entity or tag selector is required.");
			requiredMod = requiredMod == null ? Optional.empty() : requiredMod;
			eligibleLevel = eligibleLevel == null ? Optional.empty() : eligibleLevel;
			spawnLevel = spawnLevel == null ? Optional.empty() : spawnLevel;
			baseXp = baseXp == null ? Optional.empty() : baseXp;
		}
	}
}
