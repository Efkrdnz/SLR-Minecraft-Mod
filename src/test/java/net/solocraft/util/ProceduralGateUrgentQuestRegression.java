package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Dependency-free source contracts for procedural gate variety, bounded low-rank
 * encounter density, and feasible urgent kill objectives.
 */
public final class ProceduralGateUrgentQuestRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net",
			"solocraft");

	private ProceduralGateUrgentQuestRegression() {
	}

	public static void main(String[] args) throws IOException {
		progressionCeilingUsesRankLevelAndVessels();
		rankedAndOpenGateRollsStayVaried();
		lowRanksGenerateLargerBoundedEncounters();
		kaiselinJoinsBothHighRankBossPools();
		urgentKillTargetsAreFeasibleAndScoped();
	}

	private static void progressionCeilingUsesRankLevelAndVessels()
			throws IOException {
		String gates = read("util", "GateSpawnerUtil.java");
		assertContains(gates,
				"int certifiedRank = Mth.clamp((int) Math.floor(vars.HunterRank)",
				"HunterEvaluationRules.rankFloorForLevel(",
				"VesselManager.currentDefinition(target) != null",
				": Math.max(certifiedRank, levelFloor)",
				"ProceduralDungeonRank.values()[progressionRank - 1]");
		expectFalse(gates.contains("hunterRank >= 6 && level >="),
				"Gate progression must not require both certification and level");
	}

	private static void rankedAndOpenGateRollsStayVaried() throws IOException {
		String gates = read("util", "GateSpawnerUtil.java");
		assertContains(gates,
				"randomRankAtOrBelow(random, rankFor(target))",
				"rank.numericRank <= maximum.numericRank",
				"case 0 -> 40",
				"case 1 -> 28",
				"case 2 -> 17",
				"randomOpenRank(random, List.of(ProceduralDungeonRank.values()))",
				"case E -> 4",
				"case D -> 7",
				"case B -> 22",
				"case A -> 28",
				"case S -> 26");
		// Pinned on the entry existing at rank E, not on its weight. The old
		// assertion hardcoded 270, which froze a balance number in a regression
		// test: procedural gates were 78% of every roll at low rank and the test
		// treated that as the contract rather than the bug it was.
		assertContains(gates,
				"NaturalGateChoice.procedural(",
				"ProceduralDungeonRank.E), ranked, unlockedRank)",
				"NaturalGateChoice.entity(75, ProceduralDungeonRank.D");
		expectFalse(gates.contains("private static ProceduralDungeonRank randomRank("),
				"Open procedural gates must not return to uniform rank selection");
	}

	private static void lowRanksGenerateLargerBoundedEncounters()
			throws IOException {
		String ranks = read("dungeon", "ProceduralDungeonRank.java");
		String gates = read("util", "GateSpawnerUtil.java");
		String generator = read("dungeon", "ProceduralDungeonGenerator.java");
		assertContains(ranks,
				"E(1, 7, 11, 13, 17, 5, 4, 2)",
				"D(2, 8, 12, 15, 19, 5, 5, 2)");
		assertContains(gates,
				"case E -> Mth.nextInt(random, 2, 6)",
				"case D -> Mth.nextInt(random, 3, 7)");
		assertContains(generator,
				"int packs = packsForRoom(room, settings, random)",
				"int packSize = packSizeFor(settings, random)",
				"return Math.min(3, packs)",
				"case E -> 3",
				"case D -> 4",
				"Mth.clamp(size, settings.rank.packSize, maximum)",
				"mob(SololevelingModEntities.GOBLIN_MAGE.get(), 18)",
				"mob(SololevelingModEntities.STEEL_FANGED_LYCAN.get(), 10)");
	}

	private static void kaiselinJoinsBothHighRankBossPools()
			throws IOException {
		String generator = read("dungeon", "ProceduralDungeonGenerator.java");
		String lowRankBalance = read("dungeon", "LowRankDungeonBalance.java");
		assertContains(generator,
				"BossChoice bossChoice = pickBoss(bossTypes(settings.rank), random)",
				"case A -> List.of(",
				"boss(SololevelingModEntities.FUTURISTIC_GOLEM.get(), 70)",
				"boss(SololevelingModEntities.KAISELIN.get(), 30, 75, 89)",
				"case S -> List.of(",
				"boss(SololevelingModEntities.GEM_GOLEM.get(), 55)",
				"boss(SololevelingModEntities.KAISELIN.get(), 45, 100, 119)",
				"Mth.nextInt(random, choice.minimumLevel(),",
				"DungeonMobLevelAdapter.applyGenericScaling(mob, level,",
				"DungeonMobLevelAdapter.MobRole.BOSS)");
		assertContains(lowRankBalance,
				"rank != ProceduralDungeonRank.E",
				"rank != ProceduralDungeonRank.D");
	}

	private static void urgentKillTargetsAreFeasibleAndScoped()
			throws IOException {
		String urgent = read("util", "UrgentQuestManager.java");
		assertContains(urgent,
				"MIN_KILL_TARGET = 6",
				"MIN_KILL_HEADROOM = 2",
				"KILL_HEADROOM_DIVISOR = 5",
				"QuestDefinition feasible = feasibleKillQuest(player, candidate, tag)",
				"if (feasible != null)",
				"aliveMatchingDungeonMobs(player, quest.family, dungeonTag)",
				"Math.min(quest.target, Math.max(0, alive - headroom))",
				"if (target < MIN_KILL_TARGET)",
				"String objective = \"Kill \" + target",
				"player.serverLevel().getAllEntities()",
				"candidate.isAlive()",
				"!candidateTag.isBlank() && !dungeonTag.equals(candidateTag)",
				"UNTAGGED_DUNGEON_RADIUS_SQR",
				"candidate instanceof TamableAnimal tame && tame.isTame()",
				"ShadowMonarchManager.isShadowEntity(candidate)",
				"ShadowMonarchManager.getShadowOwnerUUID(candidate) != null",
				"!candidate.isAlliedTo(player) && !player.isAlliedTo(candidate)");
		assertContains(urgent,
				"String activeTag = player.getPersistentData().getString(ACTIVE_TAG)",
				"isEligibleDungeonKillTarget(player, killed, activeTag)");
		assertOrdered(urgent,
				"isEligibleDungeonKillTarget(player, killed, activeTag)",
				"|| !matchesFamily(killed, family)");
	}

	private static String read(String directory, String file) throws IOException {
		return Files.readString(MAIN.resolve(directory).resolve(file));
	}

	private static void assertContains(String source, String... snippets) {
		for (String snippet : snippets)
			expectTrue(source.contains(snippet), "Missing contract: " + snippet);
	}

	private static void assertOrdered(String source, String first, String second) {
		int firstIndex = source.indexOf(first);
		int secondIndex = source.indexOf(second, Math.max(0, firstIndex));
		expectTrue(firstIndex >= 0 && secondIndex > firstIndex,
				"Expected ordered contracts: " + first + " before " + second);
	}

	private static void expectFalse(boolean value, String message) {
		expectTrue(!value, message);
	}

	private static void expectTrue(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}
}
