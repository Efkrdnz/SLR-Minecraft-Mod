package net.solocraft.client.gui.worldcreation;

import net.solocraft.init.SololevelingModGameRules;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameRules;

import java.util.List;
import java.util.function.Function;

/** Per-world Solo Leveling settings backed directly by the creation UI's GameRules. */
public final class SoloLevelingWorldCreationTab extends GridLayoutTab {
	private static final int COLUMN_WIDTH = 152;
	private static final int COLUMN_GAP = 8;
	private static final int FULL_WIDTH = COLUMN_WIDTH * 2 + COLUMN_GAP;
	private static final int CONTROL_HEIGHT = 20;

	private final WorldCreationUiState uiState;
	private final CycleButton<Boolean> storyMode;
	private final CycleButton<ProgressionPreset> progression;
	private final CycleButton<DifficultyPreset> difficulty;
	private final CycleButton<Integer> xpRate;
	private final CycleButton<Integer> jobChange;
	private final CycleButton<Integer> enemyScale;
	private final CycleButton<Integer> bossPower;
	private final CycleButton<Boolean> gateProgress;
	private final CycleButton<DeathRule> deathRules;
	private boolean synchronizing;

	public SoloLevelingWorldCreationTab(Font font, WorldCreationUiState uiState) {
		super(Component.literal("Solo Leveling"));
		this.uiState = uiState;

		GameRules rules = uiState.getGameRules();
		normalizeOfferedValues(rules);
		ProgressionPreset storedProgression = ProgressionPreset.fromId(rules.getInt(
				SololevelingModGameRules.SOLO_LEVELING_PROGRESSION_PRESET));
		ProgressionPreset initialProgression = storedProgression == ProgressionPreset.CUSTOM
				? ProgressionPreset.CUSTOM : progressionFor(rules);
		DifficultyPreset storedDifficulty = DifficultyPreset.fromId(rules.getInt(
				SololevelingModGameRules.SOLO_LEVELING_DIFFICULTY_PRESET));
		DifficultyPreset initialDifficulty = storedDifficulty == DifficultyPreset.CUSTOM
				? DifficultyPreset.CUSTOM : difficultyFor(rules);
		writeInteger(rules, SololevelingModGameRules.SOLO_LEVELING_PROGRESSION_PRESET, initialProgression.id);
		writeInteger(rules, SololevelingModGameRules.SOLO_LEVELING_DIFFICULTY_PRESET, initialDifficulty.id);

		this.storyMode = CycleButton.onOffBuilder(
						rules.getBoolean(SololevelingModGameRules.SOLO_LEVELING_STORY_MODE))
				.withTooltip(value -> Tooltip.create(Component.literal(
						"Start with the Solo Leveling story introduction. Designed for a single-player experience.")))
				.create(0, 0, FULL_WIDTH, CONTROL_HEIGHT, Component.literal("Story Mode"),
						(button, value) -> {
							writeBoolean(SololevelingModGameRules.SOLO_LEVELING_STORY_MODE, value);
							notifyChanged();
						});

		this.xpRate = integerOption("XP Rate",
				"Adjusts experience gained from Solo Leveling content.",
				List.of(10, 15, 20),
				value -> Component.literal(String.format(java.util.Locale.ROOT, "%.1fx", value / 10.0D)),
				closest(rules.getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER), List.of(10, 15, 20)),
				value -> {
					writeInteger(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER, value);
					syncProgressionPreset();
				});
		this.jobChange = integerOption("Job Change",
				"Sets the level at which the Job Change quest unlocks.",
				List.of(40, 50, 60), value -> Component.literal("Lv. " + value),
				closest(rules.getInt(SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_LEVEL), List.of(40, 50, 60)),
				value -> {
					writeInteger(SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_LEVEL, value);
					syncProgressionPreset();
				});
		this.enemyScale = integerOption("Enemy Scale",
				"Controls the health and damage of Solo Leveling enemies.",
				List.of(100, 125, 150), SoloLevelingWorldCreationTab::enemyScaleLabel,
				closest(rules.getInt(SololevelingModGameRules.SOLO_LEVELING_ENEMY_SCALE), List.of(100, 125, 150)),
				value -> {
					writeInteger(SololevelingModGameRules.SOLO_LEVELING_ENEMY_SCALE, value);
					syncDifficultyPreset();
				});
		this.bossPower = integerOption("Boss Power",
				"Adjusts boss health, damage, and resistance.",
				List.of(100, 125, 150), SoloLevelingWorldCreationTab::bossPowerLabel,
				closest(rules.getInt(SololevelingModGameRules.SOLO_LEVELING_BOSS_POWER), List.of(100, 125, 150)),
				value -> {
					writeInteger(SololevelingModGameRules.SOLO_LEVELING_BOSS_POWER, value);
					syncDifficultyPreset();
				});
		this.gateProgress = CycleButton.<Boolean>builder(
						value -> Component.literal(value ? "Ranked" : "Open"))
				.withValues(true, false)
				.withInitialValue(rules.getBoolean(SololevelingModGameRules.SOLO_LEVELING_RANKED_GATES))
				.withTooltip(value -> Tooltip.create(Component.literal(
						"Ranked limits natural gates to your current progression; Open allows every gate rank.")))
				.create(0, 0, COLUMN_WIDTH, CONTROL_HEIGHT, Component.literal("Gate Progress"),
						(button, value) -> {
							writeBoolean(SololevelingModGameRules.SOLO_LEVELING_RANKED_GATES, value);
							syncProgressionPreset();
						});
		this.deathRules = CycleButton.<DeathRule>builder(value -> Component.literal(value.label))
				.withValues(DeathRule.values())
				.withInitialValue(DeathRule.fromId(
						rules.getInt(SololevelingModGameRules.SOLO_LEVELING_DEATH_RULES)))
				.withTooltip(value -> Tooltip.create(Component.literal(value.tooltip)))
				.create(0, 0, COLUMN_WIDTH, CONTROL_HEIGHT, Component.literal("Death Rules"),
						(button, value) -> {
							writeInteger(SololevelingModGameRules.SOLO_LEVELING_DEATH_RULES, value.id);
							syncDifficultyPreset();
						});

		this.progression = CycleButton.<ProgressionPreset>builder(value -> Component.literal(value.label))
				.withValues(ProgressionPreset.values())
				.withInitialValue(initialProgression)
				.withTooltip(value -> Tooltip.create(Component.literal(value.tooltip)))
				.create(0, 0, COLUMN_WIDTH, CONTROL_HEIGHT, Component.literal("Progression"),
						(button, value) -> applyProgressionPreset(value));
		this.difficulty = CycleButton.<DifficultyPreset>builder(value -> Component.literal(value.label))
				.withValues(DifficultyPreset.values())
				.withInitialValue(initialDifficulty)
				.withTooltip(value -> Tooltip.create(Component.literal(value.tooltip)))
				.create(0, 0, COLUMN_WIDTH, CONTROL_HEIGHT, Component.literal("Difficulty"),
						(button, value) -> applyDifficultyPreset(value));

		this.layout.columnSpacing(COLUMN_GAP).rowSpacing(3);
		GridLayout.RowHelper rows = this.layout.createRowHelper(2);
		rows.addChild(this.storyMode, 2, rows.newCellSettings().alignHorizontallyCenter());
		rows.addChild(text(font, FULL_WIDTH, "Settings are stored with this world.", 0xA0A0A0, true), 2);
		rows.addChild(heading(font, "PROGRESSION"));
		rows.addChild(heading(font, "DIFFICULTY"));
		rows.addChild(this.progression);
		rows.addChild(this.difficulty);
		rows.addChild(this.xpRate);
		rows.addChild(this.enemyScale);
		rows.addChild(this.jobChange);
		rows.addChild(this.bossPower);
		rows.addChild(this.gateProgress);
		rows.addChild(this.deathRules);
		rows.addChild(text(font, FULL_WIDTH, "Presets update the settings below; individual changes use Custom.", 0x808080, true), 2);
		this.uiState.addListener(ignored -> resyncWidgets());
	}

	private void applyProgressionPreset(ProgressionPreset preset) {
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_PROGRESSION_PRESET, preset.id);
		if (preset == ProgressionPreset.STANDARD) {
			setProgressionChildren(10, 40, true);
		} else if (preset == ProgressionPreset.FAST) {
			setProgressionChildren(15, 40, false);
		}
		notifyChanged();
	}

	private void setProgressionChildren(int xp, int jobLevel, boolean rankedGates) {
		this.xpRate.setValue(xp);
		this.jobChange.setValue(jobLevel);
		this.gateProgress.setValue(rankedGates);
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER, xp);
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_LEVEL, jobLevel);
		writeBoolean(SololevelingModGameRules.SOLO_LEVELING_RANKED_GATES, rankedGates);
	}

	private void applyDifficultyPreset(DifficultyPreset preset) {
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_DIFFICULTY_PRESET, preset.id);
		switch (preset) {
			case NORMAL -> setDifficultyChildren(100, 100, DeathRule.STANDARD);
			case HARD -> setDifficultyChildren(125, 125, DeathRule.STANDARD);
			case BRUTAL -> setDifficultyChildren(150, 150, DeathRule.HARSH);
			case CUSTOM -> {
			}
		}
		notifyChanged();
	}

	private void setDifficultyChildren(int enemyPercent, int bossPercent, DeathRule deathRule) {
		this.enemyScale.setValue(enemyPercent);
		this.bossPower.setValue(bossPercent);
		this.deathRules.setValue(deathRule);
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_ENEMY_SCALE, enemyPercent);
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_BOSS_POWER, bossPercent);
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_DEATH_RULES, deathRule.id);
	}

	private void syncProgressionPreset() {
		ProgressionPreset preset = this.progression.getValue() == ProgressionPreset.CUSTOM
				? ProgressionPreset.CUSTOM : progressionFor(this.uiState.getGameRules());
		this.progression.setValue(preset);
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_PROGRESSION_PRESET, preset.id);
		notifyChanged();
	}

	private void syncDifficultyPreset() {
		DifficultyPreset preset = this.difficulty.getValue() == DifficultyPreset.CUSTOM
				? DifficultyPreset.CUSTOM : difficultyFor(this.uiState.getGameRules());
		this.difficulty.setValue(preset);
		writeInteger(SololevelingModGameRules.SOLO_LEVELING_DIFFICULTY_PRESET, preset.id);
		notifyChanged();
	}

	private static ProgressionPreset progressionFor(GameRules rules) {
		int xp = rules.getInt(SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER);
		int jobLevel = rules.getInt(SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_LEVEL);
		boolean rankedGates = rules.getBoolean(SololevelingModGameRules.SOLO_LEVELING_RANKED_GATES);
		if (xp == 10 && jobLevel == 40 && rankedGates)
			return ProgressionPreset.STANDARD;
		if (xp == 15 && jobLevel == 40 && !rankedGates)
			return ProgressionPreset.FAST;
		return ProgressionPreset.CUSTOM;
	}

	private static DifficultyPreset difficultyFor(GameRules rules) {
		int enemy = rules.getInt(SololevelingModGameRules.SOLO_LEVELING_ENEMY_SCALE);
		int boss = rules.getInt(SololevelingModGameRules.SOLO_LEVELING_BOSS_POWER);
		DeathRule deathRule = DeathRule.fromId(
				rules.getInt(SololevelingModGameRules.SOLO_LEVELING_DEATH_RULES));
		if (enemy == 100 && boss == 100 && deathRule == DeathRule.STANDARD)
			return DifficultyPreset.NORMAL;
		if (enemy == 125 && boss == 125 && deathRule == DeathRule.STANDARD)
			return DifficultyPreset.HARD;
		if (enemy == 150 && boss == 150 && deathRule == DeathRule.HARSH)
			return DifficultyPreset.BRUTAL;
		return DifficultyPreset.CUSTOM;
	}

	private CycleButton<Integer> integerOption(String label, String tooltip, List<Integer> values,
			Function<Integer, Component> display, int initial, java.util.function.IntConsumer onChanged) {
		return CycleButton.<Integer>builder(display)
				.withValues(values)
				.withInitialValue(initial)
				.withTooltip(value -> Tooltip.create(Component.literal(tooltip)))
				.create(0, 0, COLUMN_WIDTH, CONTROL_HEIGHT, Component.literal(label),
						(button, value) -> onChanged.accept(value));
	}

	private void writeInteger(GameRules.Key<GameRules.IntegerValue> key, int value) {
		writeInteger(this.uiState.getGameRules(), key, value);
	}

	private static void writeInteger(GameRules rules,
			GameRules.Key<GameRules.IntegerValue> key, int value) {
		rules.getRule(key).set(value, null);
	}

	private void writeBoolean(GameRules.Key<GameRules.BooleanValue> key, boolean value) {
		this.uiState.getGameRules().getRule(key).set(value, null);
	}

	private void notifyChanged() {
		this.uiState.setGameRules(this.uiState.getGameRules());
	}

	private void resyncWidgets() {
		if (this.synchronizing)
			return;
		this.synchronizing = true;
		try {
			GameRules rules = this.uiState.getGameRules();
			normalizeOfferedValues(rules);
			this.storyMode.setValue(rules.getBoolean(
					SololevelingModGameRules.SOLO_LEVELING_STORY_MODE));
			this.xpRate.setValue(rules.getInt(
					SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER));
			this.jobChange.setValue(rules.getInt(
					SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_LEVEL));
			this.enemyScale.setValue(rules.getInt(
					SololevelingModGameRules.SOLO_LEVELING_ENEMY_SCALE));
			this.bossPower.setValue(rules.getInt(
					SololevelingModGameRules.SOLO_LEVELING_BOSS_POWER));
			this.gateProgress.setValue(rules.getBoolean(
					SololevelingModGameRules.SOLO_LEVELING_RANKED_GATES));
			this.deathRules.setValue(DeathRule.fromId(rules.getInt(
					SololevelingModGameRules.SOLO_LEVELING_DEATH_RULES)));

			ProgressionPreset storedProgression = ProgressionPreset.fromId(rules.getInt(
					SololevelingModGameRules.SOLO_LEVELING_PROGRESSION_PRESET));
			ProgressionPreset displayedProgression = storedProgression == ProgressionPreset.CUSTOM
					? ProgressionPreset.CUSTOM : progressionFor(rules);
			this.progression.setValue(displayedProgression);
			writeInteger(rules, SololevelingModGameRules.SOLO_LEVELING_PROGRESSION_PRESET,
					displayedProgression.id);

			DifficultyPreset storedDifficulty = DifficultyPreset.fromId(rules.getInt(
					SololevelingModGameRules.SOLO_LEVELING_DIFFICULTY_PRESET));
			DifficultyPreset displayedDifficulty = storedDifficulty == DifficultyPreset.CUSTOM
					? DifficultyPreset.CUSTOM : difficultyFor(rules);
			this.difficulty.setValue(displayedDifficulty);
			writeInteger(rules, SololevelingModGameRules.SOLO_LEVELING_DIFFICULTY_PRESET,
					displayedDifficulty.id);
		} finally {
			this.synchronizing = false;
		}
	}

	private static void normalizeOfferedValues(GameRules rules) {
		normalize(rules, SololevelingModGameRules.SOLO_LEVELING_PROGRESSION_PRESET,
				List.of(0, 1, 2));
		normalize(rules, SololevelingModGameRules.SOLO_LEVELING_DIFFICULTY_PRESET,
				List.of(0, 1, 2, 3));
		normalize(rules, SololevelingModGameRules.SOLO_LEVELING_XP_MULTIPLIER,
				List.of(10, 15, 20));
		normalize(rules, SololevelingModGameRules.SOLO_LEVELING_JOB_CHANGE_LEVEL,
				List.of(40, 50, 60));
		normalize(rules, SololevelingModGameRules.SOLO_LEVELING_ENEMY_SCALE,
				List.of(100, 125, 150));
		normalize(rules, SololevelingModGameRules.SOLO_LEVELING_BOSS_POWER,
				List.of(100, 125, 150));
		normalize(rules, SololevelingModGameRules.SOLO_LEVELING_DEATH_RULES,
				List.of(0, 1, 2));
	}

	private static void normalize(GameRules rules,
			GameRules.Key<GameRules.IntegerValue> key, List<Integer> allowed) {
		writeInteger(rules, key, closest(rules.getInt(key), allowed));
	}

	private static int closest(int value, List<Integer> allowed) {
		int closest = allowed.get(0);
		int distance = Math.abs(value - closest);
		for (int candidate : allowed) {
			int candidateDistance = Math.abs(value - candidate);
			if (candidateDistance < distance) {
				closest = candidate;
				distance = candidateDistance;
			}
		}
		return closest;
	}

	private static Component enemyScaleLabel(int value) {
		return Component.literal(switch (value) {
			case 125 -> "High";
			case 150 -> "Extreme";
			default -> "Standard";
		});
	}

	private static Component bossPowerLabel(int value) {
		return Component.literal(value == 100 ? "Standard" : "+" + (value - 100) + "%");
	}

	private static StringWidget heading(Font font, String label) {
		Component text = Component.literal(label).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
		return new StringWidget(COLUMN_WIDTH, 9, text, font).alignLeft();
	}

	private static StringWidget text(Font font, int width, String label, int color, boolean centered) {
		StringWidget widget = new StringWidget(width, 9, Component.literal(label), font).setColor(color);
		return centered ? widget.alignCenter() : widget.alignLeft();
	}

	private enum ProgressionPreset {
		STANDARD(0, "Standard", "1.0x XP, Job Change at level 40, and ranked natural gates."),
		FAST(1, "Fast", "1.5x XP, Job Change at level 40, and all natural gate ranks open."),
		CUSTOM(2, "Custom", "Keeps the individual progression settings selected below.");

		private final int id;
		private final String label;
		private final String tooltip;

		ProgressionPreset(int id, String label, String tooltip) {
			this.id = id;
			this.label = label;
			this.tooltip = tooltip;
		}

		private static ProgressionPreset fromId(int id) {
			for (ProgressionPreset value : values()) {
				if (value.id == id)
					return value;
			}
			return STANDARD;
		}
	}

	private enum DifficultyPreset {
		NORMAL(0, "Normal", "Standard enemy power, boss power, and death rules."),
		HARD(1, "Hard", "Enemies and bosses gain 25% health and damage."),
		BRUTAL(2, "Brutal", "Enemies and bosses gain 50% power and Harsh dungeon deaths apply."),
		CUSTOM(3, "Custom", "Keeps the individual difficulty settings selected below.");

		private final int id;
		private final String label;
		private final String tooltip;

		DifficultyPreset(int id, String label, String tooltip) {
			this.id = id;
			this.label = label;
			this.tooltip = tooltip;
		}

		private static DifficultyPreset fromId(int id) {
			for (DifficultyPreset value : values()) {
				if (value.id == id)
					return value;
			}
			return NORMAL;
		}
	}

	private enum DeathRule {
		STANDARD(0, "Standard", "Vanilla inventory and experience loss."),
		FORGIVING(1, "Forgiving", "Keep inventory and vanilla experience after a Solo Leveling dungeon death."),
		HARSH(2, "Harsh", "Vanilla death rules, plus lose 25% Solo XP and 10% gold in Solo Leveling dungeons.");

		private final int id;
		private final String label;
		private final String tooltip;

		DeathRule(int id, String label, String tooltip) {
			this.id = id;
			this.label = label;
			this.tooltip = tooltip;
		}

		private static DeathRule fromId(int id) {
			for (DeathRule value : values()) {
				if (value.id == id)
					return value;
			}
			return STANDARD;
		}
	}
}
