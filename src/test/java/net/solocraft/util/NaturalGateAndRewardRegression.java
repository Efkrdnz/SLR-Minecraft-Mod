package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Guards three failures that all present as "nothing happened" in game.
 *
 * <p>A gate planted in the sea, a dungeon exit that silently relocates the
 * player to world spawn, and a reward that is consumed without granting
 * anything share a shape: the code reports success and the player sees none.
 */
public final class NaturalGateAndRewardRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");
	private static final Path LOOT_TAG = Path.of("src", "main", "resources", "data",
			"minecraft", "tags", "item", "loot_items.json");
	private static final Pattern TAG_ENTRY = Pattern.compile(
			"\"([a-z0-9_.-]+):([a-z0-9_./-]+)\"");

	private NaturalGateAndRewardRegression() {
	}

	public static void main(String[] args) throws IOException {
		gatesRefuseWetGround();
		dungeonExitPrefersTheGateOverWorldSpawn();
		randomItemRewardHasSomethingToRoll();
		rewardsAreOnlyConsumedWhenGranted();
		proceduralClearsAnnounceThemselves();
		System.out.println("Natural gate and reward regression checks passed.");
	}

	/**
	 * Only runtime instances and a handful of bespoke named-boss procedures ever
	 * said "DUNGEON CLEARED". An ordinary procedural boss completed the run in
	 * silence: exit opened, clear recorded, no feedback.
	 */
	private static void proceduralClearsAnnounceThemselves() throws IOException {
		String boss = read("procedures", "BossKilledProcedure.java");
		expect(boss.contains("DUNGEON CLEARED"),
				"The procedural clear notification must use the same wording as runtime dungeons");
		expect(boss.contains("CLEAR_ANNOUNCED_TAG"),
				"The announcement must be idempotent so both death paths cannot double-fire");
		// Count invocations, not mentions. Matching the method name alone still
		// passed when both call sites were deleted and only the declaration
		// remained, which is precisely the silent regression being guarded here.
		int declarations = countOccurrences(boss,
				"private static void announceProceduralClear(");
		int mentions = countOccurrences(boss, "announceProceduralClear(");
		expect(declarations == 1,
				"Expected exactly one announceProceduralClear declaration, found " + declarations);
		expect(mentions - declarations >= 2,
				"Both the environmental and credited boss-death paths must call"
						+ " announceProceduralClear; found " + (mentions - declarations)
						+ " call site(s)");
	}

	/**
	 * MOTION_BLOCKING counts fluids, so the raw heightmap reports the water
	 * surface over an ocean and the gate spawns in the sea.
	 */
	private static void gatesRefuseWetGround() throws IOException {
		String spawner = read("util", "GateSpawnerUtil.java");
		expect(spawner.contains("findDryGateSite")
						&& spawner.contains("getFluidState(pos).isEmpty()")
						&& spawner.contains("getFluidState(pos.above()).isEmpty()")
						&& spawner.contains("getFluidState(pos.below()).isEmpty()"),
				"Natural gate sites must reject positions with fluid at the feet, head or floor");
		expect(spawner.contains("isFaceSturdy"),
				"A gate site must stand on solid ground, not on a fluid surface");
		expect(spawner.contains("GATE_SITE_ATTEMPTS"),
				"A wet roll must be retried rather than accepted");
	}

	/**
	 * The exit search only accepts fully dry footing. Over water it exhausts every
	 * candidate, and the old fallback was world spawn -- which teleported the
	 * player across the map instead of back to their own gate.
	 */
	private static void dungeonExitPrefersTheGateOverWorldSpawn() throws IOException {
		String exit = read("procedures",
				"DungeonDimensionPlayerLeavesDimensionProcedure.java");
		expect(exit.contains("surfaceAboveFluid"),
				"The exit must try the gate's own column before falling back to world spawn");
		int surfaced = exit.indexOf("BlockPos surfaced = surfaceAboveFluid(");
		int spawnFallback = exit.indexOf("BlockPos spawn = overworld.getSharedSpawnPos().above();");
		expect(surfaced >= 0 && spawnFallback > surfaced,
				"World spawn must remain the last resort, after the gate column is tried");
	}

	/** An empty tag rolls AIR, and AIR is handed over as a silent nothing. */
	private static void randomItemRewardHasSomethingToRoll() throws IOException {
		expect(Files.exists(LOOT_TAG),
				"The loot_items item tag backing the Random Item reward must exist at "
						+ LOOT_TAG);
		String tag = Files.readString(LOOT_TAG);
		List<String> entries = new ArrayList<>();
		Matcher matcher = TAG_ENTRY.matcher(tag);
		while (matcher.find())
			entries.add(matcher.group(1) + ":" + matcher.group(2));
		expect(entries.size() >= 5,
				"The Random Item reward needs a populated loot_items tag, found: " + entries);

		String collector = read("procedures", "RewardCollectProcedure.java");
		expect(collector.contains("rolled == Items.AIR")
						&& collector.contains("return false"),
				"Rolling AIR must fail the claim so the reward stays in its slot");
	}

	private static void rewardsAreOnlyConsumedWhenGranted() throws IOException {
		String collector = read("procedures", "RewardCollectProcedure.java");
		expect(collector.contains("boolean granted = false;")
						&& collector.contains("return granted;"),
				"The collector must report whether it actually granted the reward");
		expect(!collector.contains("\t\treturn true;\n\t}"),
				"An unrecognised reward must not report success and be destroyed");

		String manager = read("util", "RewardManager.java");
		expect(manager.contains("String reward = clean(rewardAt(entity, slot));"),
				"The claim path must use the same cleaned string the label is rendered from");
	}

	private static int countOccurrences(String haystack, String needle) {
		int count = 0;
		for (int at = haystack.indexOf(needle); at >= 0;
				at = haystack.indexOf(needle, at + needle.length()))
			count++;
		return count;
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path).replace("\r\n", "\n");
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
