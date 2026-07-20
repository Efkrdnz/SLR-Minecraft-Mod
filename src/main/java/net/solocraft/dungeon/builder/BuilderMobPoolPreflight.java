package net.solocraft.dungeon.builder;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.builder.model.BuilderMobPool;
import net.solocraft.dungeon.data.DungeonDataTypes;
import net.solocraft.dungeon.data.DungeonDataTypes.EntitySelector;
import net.solocraft.dungeon.data.MobPoolResolver;

import net.minecraftforge.fml.ModList;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/** Resolves Studio-authored selectors against the live server before an export can succeed. */
public final class BuilderMobPoolPreflight {
	private BuilderMobPoolPreflight() {
	}

	public static List<String> problems(ServerLevel level, BuilderMobPool pool) {
		List<String> problems = new ArrayList<>();
		if (pool.entries().isEmpty()) {
			problems.add("Referenced pool " + pool.id() + " has no entries.");
			return problems;
		}
		boolean[] unconditionalCoverage = new boolean[DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL + 1];
		for (BuilderMobPool.Entry entry : pool.entries()) {
			boolean conditionLoaded = entry.requiredMod().isEmpty()
					|| ModList.get().isLoaded(entry.requiredMod().get());
			if (!conditionLoaded)
				continue;
			boolean resolves = resolvesSpawnable(level, entry.selectorKind(), entry.selector());
			if (!resolves) {
				String selector = (entry.selectorKind() == BuilderMobPool.SelectorKind.TAG ? "#" : "")
						+ entry.selector();
				problems.add("Pool " + pool.id() + " selector " + selector
						+ " resolves to no loaded spawnable mob. Check the ID/tag and required_mod.");
			}
			if (isGuaranteedAvailable(entry) && resolves) {
				int minimum = entry.eligibleLevel().map(BuilderMobPool.LevelRange::min).orElse(1);
				int maximum = entry.eligibleLevel().map(BuilderMobPool.LevelRange::max)
						.orElse(DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL);
				minimum = Math.max(1, minimum);
				maximum = Math.min(DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL, maximum);
				for (int dungeonLevel = minimum; dungeonLevel <= maximum; dungeonLevel++)
					unconditionalCoverage[dungeonLevel] = true;
			}
		}
		for (int dungeonLevel = 1; dungeonLevel <= DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL; dungeonLevel++) {
			if (!unconditionalCoverage[dungeonLevel]) {
				problems.add("Referenced pool " + pool.id()
						+ " has no guaranteed fallback mob at dungeon level " + dungeonLevel
						+ ". Leave Optional Mod blank on at least one resolvable entry whose Eligible Level covers 1-"
						+ DungeonBuilderProjectData.MAX_ENCOUNTER_LEVEL
						+ ", or remove an unnecessary Optional Mod condition.");
				break;
			}
		}
		return List.copyOf(problems);
	}

	/**
	 * A condition on this mod itself is redundant: Studio, exported SLR definitions,
	 * and their runtime loader cannot exist unless Solo Leveling is already loaded.
	 */
	private static boolean isGuaranteedAvailable(BuilderMobPool.Entry entry) {
		return entry.requiredMod().isEmpty()
				|| entry.requiredMod().get().equals(SololevelingMod.MODID);
	}

	public static boolean resolvesSpawnable(ServerLevel level, BuilderMobPool.SelectorKind kind,
			net.minecraft.resources.ResourceLocation id) {
		DungeonDataTypes.SelectorKind runtimeKind = kind == BuilderMobPool.SelectorKind.TAG
				? DungeonDataTypes.SelectorKind.TAG : DungeonDataTypes.SelectorKind.ENTITY;
		return !MobPoolResolver.resolve(level, new EntitySelector(runtimeKind, id)).isEmpty();
	}
}
