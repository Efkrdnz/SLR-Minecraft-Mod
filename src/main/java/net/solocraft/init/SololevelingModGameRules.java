
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.solocraft.init;

import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.level.GameRules;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class SololevelingModGameRules {
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_XP_MULTIPLIER = GameRules.register("soloLevelingXPMultiplier", GameRules.Category.PLAYER, GameRules.IntegerValue.create(10));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_JOB_CHANGE_LEVEL = GameRules.register("soloLevelingJobChangeLevel", GameRules.Category.PLAYER, GameRules.IntegerValue.create(40));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_LEVELING_MONARCH_LIMIT = GameRules.register("soloLevelingMonarchLimit", GameRules.Category.PLAYER, GameRules.IntegerValue.create(1));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_LEVELING_LORE_ACCURATE_RANKS = GameRules.register("soloLevelingLoreAccurateRanks", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> DISABLE_BLOCK_BREAKING = GameRules.register("disableBlockBreaking", GameRules.Category.PLAYER, GameRules.BooleanValue.create(false));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_DAILY_QUEST = GameRules.register("soloDailyQuest", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_DUNGEON_PROGRESSION_ONLY = GameRules.register("soloDungeonProgressionOnly", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_FATIGUE = GameRules.register("soloFatigue", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_WORLD_GRIEFING = GameRules.register("soloWorldGriefing", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_GATE_NOTIFICATION = GameRules.register("soloGateNotification", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.IntegerValue> SOLO_GATE_DELAY = GameRules.register("soloGateDelay", GameRules.Category.SPAWNING, GameRules.IntegerValue.create(400));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_GATE_SPAWNING = GameRules.register("soloGateSpawning", GameRules.Category.SPAWNING, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_MISC_ITEMS = GameRules.register("soloMiscItems", GameRules.Category.MISC, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_DUNGEON_BREAK = GameRules.register("soloDungeonBreak", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_BLOOD_EFFECTS = GameRules.register("soloBloodEffects", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
	public static final GameRules.Key<GameRules.BooleanValue> SOLO_PUNISHMENT = GameRules.register("soloPunishment", GameRules.Category.PLAYER, GameRules.BooleanValue.create(true));
}
