package net.solocraft.client.gui.worldcreation;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;

/**
 * Preview-only world creation settings for Solo Leveling.
 *
 * Values intentionally live only inside these widgets until the world-setting
 * storage and gameplay hooks are implemented.
 */
public final class SoloLevelingWorldCreationTab extends GridLayoutTab {
	private static final int COLUMN_WIDTH = 152;
	private static final int COLUMN_GAP = 8;
	private static final int FULL_WIDTH = COLUMN_WIDTH * 2 + COLUMN_GAP;
	private static final int CONTROL_HEIGHT = 20;

	public SoloLevelingWorldCreationTab(Font font) {
		super(Component.literal("Solo Leveling"));

		this.layout.columnSpacing(COLUMN_GAP).rowSpacing(3);
		GridLayout.RowHelper rows = this.layout.createRowHelper(2);

		CycleButton<Boolean> storyMode = CycleButton.onOffBuilder(false)
				.withTooltip(value -> Tooltip.create(Component.literal("Designed for a single-player experience.")))
				.create(0, 0, FULL_WIDTH, CONTROL_HEIGHT, Component.literal("Story Mode"), (button, value) -> {
				});
		rows.addChild(storyMode, 2, rows.newCellSettings().alignHorizontallyCenter());

		rows.addChild(text(font, FULL_WIDTH, "Preview only - settings are not applied yet.", 0xA0A0A0, true), 2);
		rows.addChild(heading(font, "PROGRESSION"));
		rows.addChild(heading(font, "DIFFICULTY"));

		rows.addChild(option("Progression", "Controls the overall pace of Solo Leveling progression.", "Standard", "Fast", "Custom"));
		rows.addChild(option("Difficulty", "Sets the intended challenge of dungeon encounters.", "Normal", "Hard", "Brutal"));
		rows.addChild(option("XP Rate", "Adjusts experience gained from Solo Leveling content.", "1.0x", "1.5x", "2.0x"));
		rows.addChild(option("Enemy Scale", "Controls how strongly enemies scale with player progress.", "Standard", "High", "Extreme"));
		rows.addChild(option("Job Change", "Sets the level at which the Job Change quest unlocks.", "Lv. 40", "Lv. 50", "Lv. 60"));
		rows.addChild(option("Boss Power", "Adjusts boss health, damage, and resistance.", "Standard", "+25%", "+50%"));
		rows.addChild(option("Gate Progress", "Chooses whether natural gates follow rank progression.", "Ranked", "Open"));
		rows.addChild(option("Death Rules", "Controls how punishing dungeon failure will be.", "Standard", "Forgiving", "Harsh"));

		rows.addChild(text(font, FULL_WIDTH, "Final values will be stored per world.", 0x808080, true), 2);
	}

	private static StringWidget heading(Font font, String label) {
		Component text = Component.literal(label).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD);
		return new StringWidget(COLUMN_WIDTH, 9, text, font).alignLeft();
	}

	private static StringWidget text(Font font, int width, String label, int color, boolean centered) {
		StringWidget widget = new StringWidget(width, 9, Component.literal(label), font).setColor(color);
		return centered ? widget.alignCenter() : widget.alignLeft();
	}

	private static CycleButton<String> option(String label, String tooltip, String... values) {
		return CycleButton.<String>builder(Component::literal)
				.withValues(values)
				.withInitialValue(values[0])
				.withTooltip(value -> Tooltip.create(Component.literal(tooltip + " Preview only.")))
				.create(0, 0, COLUMN_WIDTH, CONTROL_HEIGHT, Component.literal(label), (button, value) -> {
				});
	}
}
