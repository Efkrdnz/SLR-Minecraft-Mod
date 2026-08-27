package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Guards the Rift's event-only population and high-rank procedural density. */
public final class RiftSpawnAndProceduralDensityRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");
	private static final Path BIOMES = Path.of("src", "main", "resources", "data",
			"sololeveling", "worldgen", "biome");

	private RiftSpawnAndProceduralDensityRegression() {
	}

	public static void main(String[] args) throws Exception {
		highRankGatesUseFocusedEncounters();
		dimensionalRiftHasNoNaturalMobPools();
		System.out.println("Rift-spawn and procedural-density regression checks passed.");
	}

	private static void highRankGatesUseFocusedEncounters() throws IOException {
		String ranks = Files.readString(MAIN.resolve("dungeon").resolve("ProceduralDungeonRank.java")).replace("\r\n", "\n");
		String generator = Files.readString(MAIN.resolve("dungeon").resolve("ProceduralDungeonGenerator.java")).replace("\r\n", "\n");
		expect(ranks.contains("A(5, 14, 17, 23, 27, 7, 9, 2)")
					&& ranks.contains("S(6, 16, 19, 27, 31, 7, 11, 3)"),
				"A/S ranks must use reduced base pack sizes");
		expect(generator.contains("settings.rank == ProceduralDungeonRank.A\n\t\t\t\t|| settings.rank == ProceduralDungeonRank.S)\n\t\t\treturn 1;")
					&& generator.contains("boolean highRank")
					&& generator.contains("case A -> 2;")
					&& generator.contains("case S -> 3;"),
				"A/S ranks must limit rooms to one focused pack without complexity inflation");
	}

	private static void dimensionalRiftHasNoNaturalMobPools() throws IOException {
		for (String biome : List.of("rift_battlefield.json", "monarch_war_wasteland.json",
				"dimensional_rift_scar.json", "rift_void.json", "territory_beginning.json",
				"territory_destruction.json", "territory_fangs.json", "territory_frost.json",
				"territory_iron_body.json", "territory_plagues.json",
				"territory_transfiguration.json", "territory_white_flames.json")) {
			String json = Files.readString(BIOMES.resolve(biome)).replace("\r\n", "\n");
			expect(json.matches("(?s).*\\\"monster\\\"\\s*:\\s*\\[\\s*\\].*"),
					"Rift biome must not define a natural monster pool: " + biome);
		}
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
