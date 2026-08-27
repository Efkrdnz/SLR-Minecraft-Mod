package net.solocraft.client.gui.worldcreation;

import net.solocraft.util.HunterEvaluationRules;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * Per-world editor for the odds of awakening at each Hunter rank.
 *
 * <p>The six values are weights rather than literal percentages. They are not
 * required to total 100: whatever the player sets is rescaled proportionally, so
 * weights totalling 110 with an S weight of 11 give an S chance of 10%. The
 * effective percentage is shown beside every slider and as a stacked bar, so the
 * rescaling is visible while editing rather than a surprise in game.
 */
public final class AwakeningOddsScreen extends Screen {
	private static final String[] RANK_LABELS = { "E", "D", "C", "B", "A", "S" };
	private static final int[] RANK_COLORS = {
			0xFF8A94A0, 0xFF57C46B, 0xFF4A9EFF, 0xFFB067FF, 0xFFFFA23D, 0xFFFFE066
	};
	private static final int CONTROL_WIDTH = 300;
	private static final int CONTROL_HEIGHT = 20;
	private static final int ROW_SPACING = 3;

	private final Screen parent;
	private final Consumer<int[]> onApply;
	private final int[] weights;
	private final WeightSlider[] sliders =
			new WeightSlider[HunterEvaluationRules.RANK_COUNT];
	private int listTop;

	public AwakeningOddsScreen(Screen parent, int[] initial,
			Consumer<int[]> onApply) {
		super(Component.literal("Awakening Odds"));
		this.parent = parent;
		this.onApply = onApply;
		this.weights = new int[HunterEvaluationRules.RANK_COUNT];
		for (int index = 0; index < this.weights.length; index++)
			this.weights[index] = initial != null && index < initial.length
					? Mth.clamp(initial[index], 0,
							HunterEvaluationRules.MAX_RANK_WEIGHT)
					: HunterEvaluationRules.DEFAULT_RANK_ODDS[index];
	}

	@Override
	protected void init() {
		int left = (this.width - CONTROL_WIDTH) / 2;
		int rows = HunterEvaluationRules.RANK_COUNT;
		int blockHeight = rows * (CONTROL_HEIGHT + ROW_SPACING);
		this.listTop = Math.max(56, (this.height - blockHeight) / 2 - 6);

		for (int index = 0; index < rows; index++) {
			int rank = index;
			WeightSlider slider = new WeightSlider(left,
					this.listTop + index * (CONTROL_HEIGHT + ROW_SPACING), rank);
			slider.setTooltip(Tooltip.create(Component.literal(
					"Relative weight for " + RANK_LABELS[rank]
							+ "-Rank. Values are rescaled so all six total 100%.")));
			this.sliders[rank] = addRenderableWidget(slider);
		}

		int buttonY = this.listTop + blockHeight + 30;
		int half = (CONTROL_WIDTH - 4) / 2;
		addRenderableWidget(Button.builder(
						Component.literal("Reset to Default"), button -> reset())
				.bounds(left, buttonY, half, CONTROL_HEIGHT).build());
		addRenderableWidget(Button.builder(
						Component.literal("Even Split"), button -> evenSplit())
				.bounds(left + half + 4, buttonY, half, CONTROL_HEIGHT).build());
		addRenderableWidget(Button.builder(
						CommonComponents.GUI_DONE, button -> applyAndClose())
				.bounds(left, buttonY + CONTROL_HEIGHT + 4, half, CONTROL_HEIGHT)
				.build());
		addRenderableWidget(Button.builder(
						CommonComponents.GUI_CANCEL, button -> onClose())
				.bounds(left + half + 4, buttonY + CONTROL_HEIGHT + 4, half,
						CONTROL_HEIGHT)
				.build());
		refreshLabels();
	}

	private void reset() {
		System.arraycopy(HunterEvaluationRules.DEFAULT_RANK_ODDS, 0,
				this.weights, 0, this.weights.length);
		syncSliders();
	}

	private void evenSplit() {
		for (int index = 0; index < this.weights.length; index++)
			this.weights[index] = 100 / this.weights.length;
		syncSliders();
	}

	private void syncSliders() {
		for (WeightSlider slider : this.sliders) {
			if (slider != null)
				slider.pullFromWeights();
		}
		refreshLabels();
	}

	private void refreshLabels() {
		int[] normalized =
				HunterEvaluationRules.normalizedRankOdds(this.weights);
		for (int index = 0; index < this.sliders.length; index++) {
			if (this.sliders[index] != null)
				this.sliders[index].showEffective(normalized[index]);
		}
	}

	private void applyAndClose() {
		if (this.onApply != null)
			this.onApply.accept(this.weights.clone());
		onClose();
	}

	@Override
	public void onClose() {
		if (this.minecraft != null)
			this.minecraft.setScreen(this.parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY,
			float partialTicks) {
		// Screen.render() owns the background pass in 1.21. Rendering it here as
		// well (the old 1.20 pattern) runs the menu blur a second time after our
		// custom labels/bar have been drawn. Draw the vanilla background and
		// widgets once, then add the informational layer so every glyph and bar
		// edge remains pixel-sharp at all GUI scales.
		super.render(graphics, mouseX, mouseY, partialTicks);
		graphics.drawCenteredString(this.font, this.title, this.width / 2, 16,
				0xFFFFFF);
		graphics.drawCenteredString(this.font, Component.literal(
						"Chance of awakening at each rank when first evaluated")
						.withStyle(ChatFormatting.GRAY),
				this.width / 2, 28, 0xA0A0A0);

		int[] normalized =
				HunterEvaluationRules.normalizedRankOdds(this.weights);
		int total = 0;
		for (int weight : this.weights)
			total += weight;

		int left = (this.width - CONTROL_WIDTH) / 2;
		renderDistributionBar(graphics, left, this.listTop - 16, normalized);

		String summary = total == 100
				? "Total 100% - used as entered"
				: String.format(Locale.ROOT,
						"Total %d%% - rescaled to 100%%", total);
		graphics.drawCenteredString(this.font,
				Component.literal(summary).withStyle(total == 100
						? ChatFormatting.GREEN : ChatFormatting.YELLOW),
				this.width / 2,
				this.listTop + this.sliders.length * (CONTROL_HEIGHT + ROW_SPACING)
						+ 8,
				0xFFFFFF);
		if (total <= 0)
			graphics.drawCenteredString(this.font,
					Component.literal("All zero - defaults will be used")
							.withStyle(ChatFormatting.RED),
					this.width / 2,
					this.listTop
							+ this.sliders.length * (CONTROL_HEIGHT + ROW_SPACING)
							+ 19,
					0xFFFFFF);
	}

	/** Stacked bar of the effective split, so rescaling is visible at a glance. */
	private void renderDistributionBar(GuiGraphics graphics, int x, int y,
			int[] normalized) {
		graphics.fill(x - 1, y - 1, x + CONTROL_WIDTH + 1, y + 7, 0xFF101418);
		int cursor = x;
		for (int index = 0; index < normalized.length; index++) {
			int segment = normalized[index] * CONTROL_WIDTH / 100;
			if (index == normalized.length - 1)
				segment = x + CONTROL_WIDTH - cursor;
			if (segment <= 0)
				continue;
			graphics.fill(cursor, y, cursor + segment, y + 6,
					RANK_COLORS[index]);
			cursor += segment;
		}
	}

	/** One rank's weight, labelled with the effective percentage after rescaling. */
	private final class WeightSlider extends AbstractSliderButton {
		private final int rank;
		private int effective;

		private WeightSlider(int x, int y, int rank) {
			super(x, y, CONTROL_WIDTH, CONTROL_HEIGHT, Component.empty(),
					AwakeningOddsScreen.this.weights[rank]
							/ (double) HunterEvaluationRules.MAX_RANK_WEIGHT);
			this.rank = rank;
			this.effective = AwakeningOddsScreen.this.weights[rank];
			updateMessage();
		}

		private void pullFromWeights() {
			this.value = AwakeningOddsScreen.this.weights[this.rank]
					/ (double) HunterEvaluationRules.MAX_RANK_WEIGHT;
			updateMessage();
		}

		private void showEffective(int percent) {
			this.effective = percent;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			int weight = AwakeningOddsScreen.this.weights[this.rank];
			setMessage(Component.literal(RANK_LABELS[this.rank]
					+ "-Rank:  " + weight + "  ->  " + this.effective + "%"));
		}

		@Override
		protected void applyValue() {
			AwakeningOddsScreen.this.weights[this.rank] = Mth.clamp(
					(int) Math.round(this.value
							* HunterEvaluationRules.MAX_RANK_WEIGHT),
					0, HunterEvaluationRules.MAX_RANK_WEIGHT);
			// Every rank's effective share moves when any one weight changes.
			AwakeningOddsScreen.this.refreshLabels();
		}
	}
}
