package net.solocraft.init;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.minecraft.world.level.GameRules;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class SololevelingModGameRules {
	/**
	 * Keeps this static game-rule registry initialized during mod construction.
	 * NeoForge rejects automatic subscriber classes that declare no listeners.
	 */
	@SubscribeEvent
	public static void init(FMLCommonSetupEvent event) {
	}

	public static final GameRules.Key<GameRules.BooleanValue> SOLO_LEVELING_STORY_MODE = GameRules.register("soloLevelingStoryMode", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_PROGRESSION_PRESET = GameRules.register("soloLevelingProgressionPreset", GameRules.Category.PLAYER, GameRules.IntegerValue.create(0));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_DIFFICULTY_PRESET = GameRules.register("soloLevelingDifficultyPreset", GameRules.Category.MOBS, GameRules.IntegerValue.create(0));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_XP_MULTIPLIER = GameRules.register("soloLevelingXPMultiplier", GameRules.Category.PLAYER, GameRules.IntegerValue.create(10));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_JOB_CHANGE_LEVEL = GameRules.register("soloLevelingJobChangeLevel", GameRules.Category.PLAYER, GameRules.IntegerValue.create(40));
	/**
	 * The peak of the progression curve. {@code 0} restores the original
	 * unbounded levelling; see {@link net.solocraft.util.LevelCapRules}.
	 */
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_LEVEL_CAP = GameRules.register("soloLevelingLevelCap", GameRules.Category.PLAYER, GameRules.IntegerValue.create(150));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_ENEMY_SCALE = GameRules.register("soloLevelingEnemyScale", GameRules.Category.MOBS, GameRules.IntegerValue.create(100));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_BOSS_POWER = GameRules.register("soloLevelingBossPower", GameRules.Category.MOBS, GameRules.IntegerValue.create(100));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_LEVELING_RANKED_GATES = GameRules.register("soloLevelingRankedGates", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_DEATH_RULES = GameRules.register("soloLevelingDeathRules", GameRules.Category.PLAYER, GameRules.IntegerValue.create(0));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_MONARCH_LIMIT = GameRules.register("soloLevelingMonarchLimit", GameRules.Category.PLAYER, GameRules.IntegerValue.create(1));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_JOB_CHANGE_POINTS = GameRules.register("soloLevelingJobChangePoints", GameRules.Category.PLAYER, GameRules.IntegerValue.create(50));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_LEVELING_LORE_ACCURATE_RANKS = GameRules.register("soloLevelingLoreAccurateRanks", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	// Awakening odds are stored as raw weights, not literal percentages: they are
	// normalized to total 100 when an evaluation rolls. Defaults match the
	// original 25/25/25/12/10/3 balance.
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_RANK_ODDS_E = GameRules.register("soloLevelingRankOddsE", GameRules.Category.PLAYER, GameRules.IntegerValue.create(25));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_RANK_ODDS_D = GameRules.register("soloLevelingRankOddsD", GameRules.Category.PLAYER, GameRules.IntegerValue.create(25));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_RANK_ODDS_C = GameRules.register("soloLevelingRankOddsC", GameRules.Category.PLAYER, GameRules.IntegerValue.create(25));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_RANK_ODDS_B = GameRules.register("soloLevelingRankOddsB", GameRules.Category.PLAYER, GameRules.IntegerValue.create(12));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_RANK_ODDS_A = GameRules.register("soloLevelingRankOddsA", GameRules.Category.PLAYER, GameRules.IntegerValue.create(10));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_RANK_ODDS_S = GameRules.register("soloLevelingRankOddsS", GameRules.Category.PLAYER, GameRules.IntegerValue.create(3));
	public static final GameRules.Key<GameRules.BooleanValue> DISABLE_BLOCK_BREAKING = GameRules.register("disableBlockBreaking", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_DAILY_QUEST = GameRules.register("soloDailyQuest", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_DUNGEON_PROGRESSION_ONLY = GameRules.register("soloDungeonProgressionOnly", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_FATIGUE = GameRules.register("soloFatigue", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	/**
	 * The original boolean is retained so worlds created before the three-mode
	 * setting continue to load with their existing destruction preference.
	 */
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_ABILITY_DESTRUCTION = GameRules.register("soloAbilityDestruction", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
	/**
	 * -1 means an older save has not selected a mode yet; resolve it from the
	 * legacy boolean instead of silently changing that world's behaviour.
	 */
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_ABILITY_DESTRUCTION_MODE = GameRules.register("soloAbilityDestructionMode", GameRules.Category.PLAYER, GameRules.IntegerValue.create(-1));
	/** Source-compatibility alias for the retired, forcibly-enabled legacy rule. */
	@Deprecated
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_WORLD_GRIEFING = SOLO_ABILITY_DESTRUCTION;
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_GATE_NOTIFICATION = GameRules.register("soloGateNotification", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_GATE_DELAY = GameRules.register("soloGateDelay", GameRules.Category.SPAWNING, GameRules.IntegerValue.create(400));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_GATE_SPAWNING = GameRules.register("soloGateSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_MISC_ITEMS = GameRules.register("soloMiscItems", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_DUNGEON_BREAK = GameRules.register("soloDungeonBreak", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_BLOOD_EFFECTS = GameRules.register("soloBloodEffects", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_PUNISHMENT = GameRules.register("soloPunishment", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));

	public enum AbilityDestructionMode {
		FALSE(0, "false"),
		PARTIAL(1, "partial"),
		TRUE(2, "true");

		private final int id;
		private final String label;

		AbilityDestructionMode(int id, String label) {
			this.id = id;
			this.label = label;
		}

		public int id() {
			return this.id;
		}

		public String label() {
			return this.label;
		}

		public static AbilityDestructionMode fromId(int id) {
			return switch (id) {
				case 1 -> PARTIAL;
				case 2 -> TRUE;
				default -> FALSE;
			};
		}
	}

	/** Returns the saved three-mode value, including a safe legacy-world fallback. */
	public static AbilityDestructionMode abilityDestructionMode(GameRules rules) {
		int stored = rules.getInt(SOLO_ABILITY_DESTRUCTION_MODE);
		if (stored >= AbilityDestructionMode.FALSE.id()
				&& stored <= AbilityDestructionMode.TRUE.id())
			return AbilityDestructionMode.fromId(stored);
		return rules.getBoolean(SOLO_ABILITY_DESTRUCTION)
				? AbilityDestructionMode.TRUE : AbilityDestructionMode.FALSE;
	}

	/** Writes the mode and keeps the old boolean meaningful for legacy integrations. */
	public static void setAbilityDestructionMode(GameRules rules,
			AbilityDestructionMode mode) {
		AbilityDestructionMode selected = mode == null ? AbilityDestructionMode.FALSE : mode;
		rules.getRule(SOLO_ABILITY_DESTRUCTION_MODE).set(selected.id(), null);
		rules.getRule(SOLO_ABILITY_DESTRUCTION).set(selected != AbilityDestructionMode.FALSE, null);
	}
}
