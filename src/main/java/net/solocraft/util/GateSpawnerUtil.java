package net.solocraft.util;

import net.solocraft.dungeon.DungeonTheme;
import net.solocraft.dungeon.DatapackDungeonGateHandler;
import net.solocraft.dungeon.ProceduralDungeonRank;
import net.solocraft.dungeon.data.DungeonDataManager;
import net.solocraft.entity.DatapackGateEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModGameRules;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

public class GateSpawnerUtil {
	/** Rerolls before giving up and waiting for the next gate timer. */
	private static final int GATE_SITE_ATTEMPTS = 12;

	private GateSpawnerUtil() {
	}

	public static void spawnNearRandomOverworldPlayer(LevelAccessor world) {
		if (DungeonBuilderMode.isActive(world)
				|| !(world instanceof ServerLevel serverLevel)
				|| serverLevel.dimension() != Level.OVERWORLD)
			return;
		List<Entity> players = new ArrayList<>();
		for (Entity player : new ArrayList<>(world.players())) {
			if (player.level().dimension() == Level.OVERWORLD)
				players.add(player);
		}
		if (players.isEmpty())
			return;
		RandomSource random = RandomSource.create();
		Entity target = players.get(Mth.nextInt(random, 0, players.size() - 1));
		BlockPos pos = findDryGateSite(serverLevel, target, random);
		if (pos == null) {
			delayNextGate(world);
			return;
		}
		SololevelingModVariables.MapVariables mapVars = SololevelingModVariables.MapVariables.get(world);
		if (!mapVars.firstNaturalGateSpawned) {
			Entity firstGate = spawn(serverLevel, SololevelingModEntities.PORTAL_SEWERS.get(), pos);
			if (firstGate != null) {
				mapVars.firstNaturalGateSpawned = true;
				mapVars.syncData(world);
			} else {
				delayNextGate(world);
			}
			return;
		}
		spawnEligibleNaturalGate(serverLevel, target, pos, random);
	}

	private static double truncate(double value) {
		return (float) ((int) (Math.pow(10, 2) * value)) / (float) Math.pow(10, 2);
	}

	/**
	 * Picks a gate site on dry, solid ground near the target player.
	 *
	 * <p>The heightmap alone is not enough: MOTION_BLOCKING counts fluids, so over
	 * an ocean or a river it reports the water surface and the gate is planted in
	 * the sea. That also strands the player on the way out, because the return
	 * search looks for a dry standing spot around the gate and finds none, so it
	 * gives up and uses world spawn. Rejecting wet sites here fixes both.
	 */
	private static BlockPos findDryGateSite(ServerLevel level, Entity target,
			RandomSource random) {
		double baseX = truncate(target.getX());
		double baseZ = truncate(target.getZ());
		for (int attempt = 0; attempt < GATE_SITE_ATTEMPTS; attempt++) {
			int spawnX = (int) (baseX + Mth.nextInt(random, -200, 200));
			int spawnZ = (int) (baseZ + Mth.nextInt(random, -200, 200));
			int spawnY = level.getHeight(
					Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawnX, spawnZ);
			if (spawnY < -60)
				continue;
			BlockPos candidate = new BlockPos(spawnX, spawnY, spawnZ);
			if (isDryGateSite(level, candidate))
				return candidate;
		}
		return null;
	}

	/** Feet, head and the block underneath must all be free of fluid. */
	private static boolean isDryGateSite(ServerLevel level, BlockPos pos) {
		if (pos.getY() <= level.getMinBuildHeight()
				|| pos.getY() >= level.getMaxBuildHeight() - 1)
			return false;
		return level.getFluidState(pos).isEmpty()
				&& level.getFluidState(pos.above()).isEmpty()
				&& level.getFluidState(pos.below()).isEmpty()
				&& level.getBlockState(pos.below())
						.isFaceSturdy(level, pos.below(), Direction.UP);
	}

	private static Entity spawn(ServerLevel level, EntityType<?> type, BlockPos pos) {
		return type.spawn(level, pos, MobSpawnType.MOB_SUMMONED);
	}

	private static void spawnEligibleNaturalGate(ServerLevel level, Entity target, BlockPos pos,
			RandomSource random) {
		boolean ranked = level.getGameRules().getBoolean(
				SololevelingModGameRules.SOLO_LEVELING_RANKED_GATES);
		ProceduralDungeonRank unlockedRank = rankFor(target);
		List<ResourceLocation> datapackDungeons = eligibleDatapackDungeons(ranked, unlockedRank);
		List<NaturalGateChoice> choices = new ArrayList<>();
		// Procedural gates are the varied core of natural gate progression. The
		// starter sewer remains guaranteed once, but it must not dominate every
		// later roll as a second de-facto starter gate.
		// 270 was set when procedural gates were the varied option and the fixed
		// portals were not. At low rank it meant 78% of every roll was the same
		// gate, because almost every portal below is rank-locked out and there
		// were only two entries left in the table. Procedural is still the most
		// common gate by a wide margin; it is no longer four fifths of the game.
		addChoice(choices, NaturalGateChoice.procedural(150, ProceduralDungeonRank.E), ranked, unlockedRank);
		if (!datapackDungeons.isEmpty())
			choices.add(NaturalGateChoice.datapack(100));
		addChoice(choices, NaturalGateChoice.entity(137, ProceduralDungeonRank.C,
				SololevelingModEntities.RANDOM_CAVE_LARGE.get()), ranked, unlockedRank);
		addChoice(choices, NaturalGateChoice.entity(114, ProceduralDungeonRank.B,
				SololevelingModEntities.PORTAL_LUSH.get()), ranked, unlockedRank);
		addChoice(choices, NaturalGateChoice.entity(95, ProceduralDungeonRank.C,
				SololevelingModEntities.PORTAL_ANCIENT_GOLEM.get()), ranked, unlockedRank);
		addChoice(choices, NaturalGateChoice.entity(79, ProceduralDungeonRank.A,
				SololevelingModEntities.PORTAL_LAB.get()), ranked, unlockedRank);
		addChoice(choices, NaturalGateChoice.entity(66, ProceduralDungeonRank.B,
				SololevelingModEntities.PORTAL_CEMETERY.get()), ranked, unlockedRank);
		addChoice(choices, NaturalGateChoice.red(33, ProceduralDungeonRank.B), ranked, unlockedRank);
		addChoice(choices, NaturalGateChoice.entity(59, ProceduralDungeonRank.A,
				SololevelingModEntities.PORTAL_KARGALGANS_THRONE_ROOM.get()), ranked, unlockedRank);
		addChoice(choices, NaturalGateChoice.entity(47, ProceduralDungeonRank.S,
				SololevelingModEntities.PORTAL_BERU.get()), ranked, unlockedRank);
		addChoice(choices, NaturalGateChoice.entity(75, ProceduralDungeonRank.D,
				SololevelingModEntities.PORTAL_SEWERS.get()), ranked, unlockedRank);

		int totalWeight = choices.stream().mapToInt(NaturalGateChoice::weight).sum();
		if (totalWeight <= 0) {
			delayNextGate(level);
			return;
		}
		int roll = random.nextInt(totalWeight);
		NaturalGateChoice selected = choices.get(choices.size() - 1);
		for (NaturalGateChoice choice : choices) {
			roll -= choice.weight();
			if (roll < 0) {
				selected = choice;
				break;
			}
		}

		if (selected.datapack()) {
			spawnDatapackGate(level, pos, random, ranked, unlockedRank, datapackDungeons);
		} else if (selected.procedural()) {
			spawnProceduralGate(level, target, pos, random, ranked);
		} else if (selected.red()) {
			if (!SololevelingModVariables.MapVariables.get(level).RedGate)
				spawn(level, SololevelingModEntities.RED_GATE.get(), pos);
			else
				delayNextGate(level);
		} else {
			spawn(level, selected.type(), pos);
		}
	}

	private static void addChoice(List<NaturalGateChoice> choices, NaturalGateChoice choice,
			boolean ranked, ProceduralDungeonRank unlockedRank) {
		if (!ranked || unlockedRank.numericRank >= choice.minimumRank().numericRank)
			choices.add(choice);
	}

	private static List<ResourceLocation> eligibleDatapackDungeons(boolean ranked,
			ProceduralDungeonRank unlockedRank) {
		var snapshot = DungeonDataManager.snapshot();
		return snapshot.dungeonIds().stream()
				.filter(id -> snapshot.dungeon(id)
						.map(definition -> !ranked || definition.supportsRank(unlockedRank))
						.orElse(false))
				.toList();
	}

	private static void spawnDatapackGate(ServerLevel level, BlockPos pos, RandomSource random,
			boolean ranked, ProceduralDungeonRank unlockedRank,
			List<ResourceLocation> eligibleDungeons) {
		if (eligibleDungeons.isEmpty()) {
			delayNextGate(level);
			return;
		}
		ResourceLocation dungeonId = eligibleDungeons.get(
				Mth.nextInt(random, 0, eligibleDungeons.size() - 1));
		var definition = DungeonDataManager.dungeon(dungeonId).orElse(null);
		if (definition == null) {
			delayNextGate(level);
			return;
		}
		ProceduralDungeonRank rank;
		if (ranked) {
			if (!definition.supportsRank(unlockedRank)) {
				delayNextGate(level);
				return;
			}
			rank = unlockedRank;
		} else {
			List<ProceduralDungeonRank> allowedRanks = java.util.Arrays.stream(
					ProceduralDungeonRank.values())
					.filter(definition.allowedRanks()::contains)
					.toList();
			if (allowedRanks.isEmpty()) {
				delayNextGate(level);
				return;
			}
			rank = randomOpenRank(random, allowedRanks);
		}

		DatapackGateEntity gate = SololevelingModEntities.DATAPACK_GATE.get().spawn(
				level, pos, MobSpawnType.MOB_SUMMONED);
		if (gate == null) {
			delayNextGate(level);
			return;
		}
		if (!DatapackDungeonGateHandler.bind(gate, dungeonId, rank)) {
			gate.discard();
			delayNextGate(level);
		}
	}

	private static void spawnProceduralGate(ServerLevel level, Entity target, BlockPos pos,
			RandomSource random, boolean ranked) {
		Entity gate = spawn(level, SololevelingModEntities.PORTAL_1.get(), pos);
		if (gate == null)
			return;
		ProceduralDungeonRank rank = ranked
				? randomRankAtOrBelow(random, rankFor(target))
				: randomOpenRank(random, List.of(ProceduralDungeonRank.values()));
		DungeonTheme theme = randomTheme(random);
		gate.getPersistentData().putBoolean("slr_procedural_gate", true);
		gate.getPersistentData().putBoolean("slr_procedural_red_gate", false);
		gate.getPersistentData().putString("slr_procedural_rank", rank.name());
		gate.getPersistentData().putString("slr_procedural_theme", theme.name());
		gate.getPersistentData().putInt("slr_procedural_complexity", complexityFor(rank, random));
	}

	/**
	 * Ranked worlds favor the player's current ceiling without turning every gate
	 * into the exact same rank. No roll can exceed that ceiling.
	 */
	private static ProceduralDungeonRank randomRankAtOrBelow(RandomSource random,
			ProceduralDungeonRank maximum) {
		List<ProceduralDungeonRank> eligible = java.util.Arrays.stream(
				ProceduralDungeonRank.values())
				.filter(rank -> rank.numericRank <= maximum.numericRank)
				.toList();
		int totalWeight = eligible.stream().mapToInt(rank -> switch (
				maximum.numericRank - rank.numericRank) {
			case 0 -> 40;
			case 1 -> 28;
			case 2 -> 17;
			default -> 9;
		}).sum();
		int roll = random.nextInt(totalWeight);
		for (ProceduralDungeonRank rank : eligible) {
			roll -= switch (maximum.numericRank - rank.numericRank) {
				case 0 -> 40;
				case 1 -> 28;
				case 2 -> 17;
				default -> 9;
			};
			if (roll < 0)
				return rank;
		}
		return maximum;
	}

	/** Open-rank worlds deliberately lean toward B-S gates while retaining tails. */
	private static ProceduralDungeonRank randomOpenRank(RandomSource random,
			List<ProceduralDungeonRank> allowed) {
		int totalWeight = allowed.stream().mapToInt(GateSpawnerUtil::openRankWeight).sum();
		int roll = random.nextInt(Math.max(1, totalWeight));
		for (ProceduralDungeonRank rank : allowed) {
			roll -= openRankWeight(rank);
			if (roll < 0)
				return rank;
		}
		return allowed.get(allowed.size() - 1);
	}

	private static int openRankWeight(ProceduralDungeonRank rank) {
		return switch (rank) {
			case E -> 4;
			case D -> 7;
			case C -> 13;
			case B -> 22;
			case A -> 28;
			case S -> 26;
		};
	}

	private static ProceduralDungeonRank rankFor(Entity target) {
		SololevelingModVariables.PlayerVariables vars = target.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		int certifiedRank = Mth.clamp((int) Math.floor(vars.HunterRank), 1,
				HunterEvaluationRules.RANK_COUNT);
		int levelFloor = HunterEvaluationRules.rankFloorForLevel(
				Math.max(0, (int) Math.floor(vars.Level)));
		int progressionRank = VesselManager.currentDefinition(target) != null
				? HunterEvaluationRules.RANK_COUNT
				: Math.max(certifiedRank, levelFloor);
		return ProceduralDungeonRank.values()[progressionRank - 1];
	}

	private static DungeonTheme randomTheme(RandomSource random) {
		DungeonTheme[] themes = DungeonTheme.values();
		return themes[Mth.nextInt(random, 0, themes.length - 1)];
	}

	private static int complexityFor(ProceduralDungeonRank rank, RandomSource random) {
		return switch (rank) {
			case E -> Mth.nextInt(random, 2, 6);
			case D -> Mth.nextInt(random, 3, 7);
			case C -> Mth.nextInt(random, 3, 6);
			case B -> Mth.nextInt(random, 5, 7);
			case A -> Mth.nextInt(random, 7, 9);
			case S -> Mth.nextInt(random, 8, 10);
		};
	}

	private static void delayNextGate(LevelAccessor world) {
		SololevelingModVariables.MapVariables.get(world).gatetimer = world.getLevelData().getGameRules().getInt(SololevelingModGameRules.SOLO_GATE_DELAY) - 1;
		SololevelingModVariables.MapVariables.get(world).syncData(world);
	}

	private record NaturalGateChoice(int weight, ProceduralDungeonRank minimumRank,
			EntityType<?> type, boolean procedural, boolean red, boolean datapack) {
		private static NaturalGateChoice procedural(int weight, ProceduralDungeonRank minimumRank) {
			return new NaturalGateChoice(weight, minimumRank, null, true, false, false);
		}

		private static NaturalGateChoice datapack(int weight) {
			return new NaturalGateChoice(weight, ProceduralDungeonRank.E, null, false, false, true);
		}

		private static NaturalGateChoice red(int weight, ProceduralDungeonRank minimumRank) {
			return new NaturalGateChoice(weight, minimumRank, null, false, true, false);
		}

		private static NaturalGateChoice entity(int weight, ProceduralDungeonRank minimumRank,
				EntityType<?> type) {
			return new NaturalGateChoice(weight, minimumRank, type, false, false, false);
		}
	}
}
