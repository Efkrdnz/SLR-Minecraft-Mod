package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.network.DatapackGateSelectionMessage;
import net.solocraft.network.DatapackGateSelectionStateMessage.Option;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

/** Compact, server-fed dungeon and rank picker for a spawn-egg datapack gate. */
public final class DatapackGateSelectionScreen extends SystemScreen {
	private static final int VISIBLE_ROWS = 7;
	private static final int ROW_HEIGHT = 20;
	private static final int LIST_TOP = 48;

	private final UUID gateId;
	private long revision;
	private List<Option> options;
	private String notice;
	private int selectedIndex;
	private int scroll;
	private String selectedRank = "";
	private boolean submitted;

	private DatapackGateSelectionScreen(UUID gateId, long revision, List<Option> options,
			String notice) {
		super(Component.literal("DATAPACK GATE"));
		this.panelW = 352;
		this.panelH = 294;
		this.gateId = gateId;
		applyState(revision, options, notice);
	}

	public static void handleServerState(boolean open, UUID gateId, long revision,
			List<Option> options, String notice) {
		Minecraft minecraft = Minecraft.getInstance();
		if (!open) {
			if (minecraft.screen instanceof DatapackGateSelectionScreen screen
					&& screen.gateId.equals(gateId)) {
				screen.submitted = true;
				minecraft.setScreen(null);
			}
			return;
		}

		if (minecraft.screen instanceof DatapackGateSelectionScreen screen
				&& screen.gateId.equals(gateId)) {
			screen.applyState(revision, options, notice);
			if (screen.minecraft != null)
				screen.rebuildWidgets();
		} else {
			minecraft.setScreen(new DatapackGateSelectionScreen(gateId, revision, options, notice));
		}
	}

	@Override
	protected void init() {
		super.init();
		rebuildWidgets();
	}

	@Override
	protected void rebuildWidgets() {
		clearWidgets();
		int listX = panelX + 12;
		int listY = panelY + LIST_TOP;
		int listW = panelW - 24;
		int end = Math.min(options.size(), scroll + VISIBLE_ROWS);
		for (int index = scroll; index < end; index++) {
			final int optionIndex = index;
			Option option = options.get(index);
			String prefix = index == selectedIndex ? "> " : "  ";
			String label = fit(prefix + option.dungeonId(), listW - 12);
			addRenderableWidget(new SystemButton(listX, listY + (index - scroll) * ROW_HEIGHT,
					listW, 18, Component.literal(label), button -> selectOption(optionIndex)));
		}

		Option selected = selectedOption();
		if (selected != null) {
			int rankY = panelY + 216;
			int rankX = panelX + 12;
			for (String rank : selected.ranks()) {
				String value = rank;
				String label = value.equals(selectedRank) ? "[" + value + "]" : value;
				addRenderableWidget(new SystemButton(rankX, rankY, 36, 18, Component.literal(label),
						button -> selectRank(value)));
				rankX += 40;
			}
		}

		if (!submitted && selected != null && !selectedRank.isBlank()) {
			addRenderableWidget(new SystemButton(panelX + panelW - 166, panelY + panelH - 28,
					76, 18, Component.literal("Bind Gate"), button -> submitSelection()));
		}
		addRenderableWidget(new SystemButton(panelX + panelW - 84, panelY + panelH - 28,
				72, 18, Component.literal("Cancel"), button -> beginClose()));
	}

	private void applyState(long revision, List<Option> nextOptions, String nextNotice) {
		String previousId = selectedOption() == null ? "" : selectedOption().dungeonId();
		this.revision = Math.max(0L, revision);
		this.options = nextOptions == null ? List.of()
				: List.copyOf(nextOptions.stream().filter(java.util.Objects::nonNull)
						.limit(256).toList());
		this.notice = clean(nextNotice, 192);
		this.submitted = false;
		this.selectedIndex = 0;
		for (int index = 0; index < this.options.size(); index++) {
			if (this.options.get(index).dungeonId().equals(previousId)) {
				this.selectedIndex = index;
				break;
			}
		}
		this.scroll = clamp(scroll, 0, maxScroll());
		chooseValidRank();
	}

	private void selectOption(int index) {
		if (index < 0 || index >= options.size())
			return;
		selectedIndex = index;
		chooseValidRank();
		rebuildWidgets();
	}

	private void selectRank(String rank) {
		Option selected = selectedOption();
		if (selected == null || !selected.ranks().contains(rank))
			return;
		selectedRank = rank;
		rebuildWidgets();
	}

	private void chooseValidRank() {
		Option selected = selectedOption();
		if (selected == null || selected.ranks().isEmpty()) {
			selectedRank = "";
		} else if (!selected.ranks().contains(selectedRank)) {
			selectedRank = selected.ranks().get(0);
		}
	}

	private void submitSelection() {
		Option selected = selectedOption();
		if (submitted || selected == null || !selected.ranks().contains(selectedRank))
			return;
		submitted = true;
		rebuildWidgets();
		SololevelingMod.PACKET_HANDLER.sendToServer(new DatapackGateSelectionMessage(
				gateId, revision, selected.dungeonId(), selectedRank));
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
		int next = clamp(scroll + (delta < 0.0D ? 1 : -1), 0, maxScroll());
		if (next != scroll) {
			scroll = next;
			rebuildWidgets();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, delta);
	}

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		graphics.drawCenteredString(font, "Choose the dungeon and gate rank.",
				panelX + panelW / 2, panelY + 26, TEXT_MAIN);
		graphics.fill(panelX + 10, panelY + 41, panelX + panelW - 10, panelY + 42, ACCENT_DIM);

		if (options.isEmpty()) {
			graphics.drawCenteredString(font, "NO DATAPACK DUNGEONS AVAILABLE",
					panelX + panelW / 2, panelY + 100, 0xFFFF6B78);
		} else {
			Option selected = selectedOption();
			if (selected != null) {
				String details = selected.kind() + "  |  "
						+ roomLabel(selected.minRooms(), selected.maxRooms());
				graphics.drawString(font, fit(details, panelW - 24), panelX + 12,
						panelY + 195, TEXT_SUB, false);
				graphics.drawString(font, "RANK", panelX + 12, panelY + 207, TEXT_SUB, false);
			}
			if (maxScroll() > 0) {
				graphics.drawString(font, (scroll + 1) + "-"
								+ Math.min(options.size(), scroll + VISIBLE_ROWS) + "/" + options.size(),
						panelX + panelW - 58, panelY + 34, TEXT_SUB, false);
			}
		}

		if (!notice.isBlank()) {
			int color = options.isEmpty() ? 0xFFFFA4AD : 0xFFFFD166;
			graphics.drawCenteredString(font, fit(notice, panelW - 24),
					panelX + panelW / 2, panelY + 247, color);
		}
	}

	@Override
	protected boolean allowsNonSystemAccess() {
		return true;
	}

	@Override
	protected boolean shouldPlaySystemSounds() {
		return true;
	}

	private Option selectedOption() {
		return options == null || selectedIndex < 0 || selectedIndex >= options.size()
				? null : options.get(selectedIndex);
	}

	private int maxScroll() {
		return Math.max(0, (options == null ? 0 : options.size()) - VISIBLE_ROWS);
	}

	private String fit(String text, int width) {
		String value = text == null ? "" : text;
		if (font.width(value) <= width)
			return value;
		String ellipsis = "...";
		while (!value.isEmpty() && font.width(value + ellipsis) > width)
			value = value.substring(0, value.length() - 1);
		return value + ellipsis;
	}

	private static String roomLabel(int min, int max) {
		return min == max ? min + " ROOMS" : min + "-" + max + " ROOMS";
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static String clean(String value, int maximum) {
		if (value == null)
			return "";
		String clean = value.replace('\u0000', ' ').trim();
		return clean.length() <= maximum ? clean : clean.substring(0, maximum);
	}
}
