package net.solocraft.client.screens;

import net.solocraft.client.gui.DungeonBuilderClientState;
import net.solocraft.item.DungeonBuilderWandItem;
import net.solocraft.network.DungeonBuilderStatusMessage;
import net.solocraft.util.DungeonBuilderMode;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

/** Live, builder-world-only checklist that replaces chat spam with actionable status. */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public final class DungeonBuilderHudOverlay {
	private static final int CYAN = 0xFF52DDF5;
	private static final int GREEN = 0xFF64E68A;
	private static final int YELLOW = 0xFFFFC857;
	private static final int RED = 0xFFFF5F67;
	private static final int WHITE = 0xFFF2FAFF;
	private static final int MUTED = 0xFFA9BBC6;
	private static final int DIM = 0xFF71838D;

	private DungeonBuilderHudOverlay() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void render(RenderGuiEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.level == null || minecraft.options.hideGui
				|| minecraft.screen != null || !DungeonBuilderMode.isActive(minecraft.level))
			return;
		DungeonBuilderStatusMessage.View view = DungeonBuilderClientState.view();
		if (!view.active())
			return;

		int screenWidth = event.getWindow().getGuiScaledWidth();
		int screenHeight = event.getWindow().getGuiScaledHeight();
		int width = Math.max(190, Math.min(254, screenWidth - 12));
		int x = Math.max(6, screenWidth - width - 6);
		int y = 6;
		int fixedHeight = 112;
		int maxLines = Math.max(3, Math.min(view.lines().size(), (screenHeight - fixedHeight - 18) / 11));
		int height = fixedHeight + maxLines * 11;
		if (height > screenHeight - 12) {
			maxLines = Math.max(1, (screenHeight - fixedHeight - 12) / 11);
			height = fixedHeight + maxLines * 11;
		}
		renderPanel(event.getGuiGraphics(), minecraft.font, view, x, y, width, height, maxLines);
	}

	private static void renderPanel(GuiGraphics graphics, Font font,
			DungeonBuilderStatusMessage.View view, int x, int y, int width, int height, int maxLines) {
		int accent = view.errors() == 0 ? GREEN : RED;
		drawPanel(graphics, x, y, width, height, accent);
		graphics.drawString(font, "DUNGEON BUILDER", x + 8, y + 6, CYAN, false);
		String result = view.errors() == 0 ? "READY" : view.errors() + " ERROR" + (view.errors() == 1 ? "" : "S");
		graphics.drawString(font, result, x + width - 8 - font.width(result), y + 6, accent, false);

		graphics.drawString(font, fit(font, view.projectId(), width - 16), x + 8, y + 18, WHITE, false);
		String type = view.type().toUpperCase(Locale.ROOT) + "  |  RANK " + view.ranks();
		graphics.drawString(font, fit(font, type, width - 16), x + 8, y + 29, MUTED, false);
		graphics.drawString(font, fit(font, "ONE MODULE PROJECT = ONE ROOM", width - 16), x + 8, y + 40, CYAN, false);

		String tool = heldTool();
		graphics.drawString(font, fit(font, tool, width - 16), x + 8, y + 52,
				tool.startsWith("Wand:") ? YELLOW : DIM, false);
		graphics.drawString(font, fit(font, "Group: " + view.group() + "  |  Pool: " + view.pool(), width - 16),
				x + 8, y + 63, MUTED, false);
		graphics.drawString(font, fit(font, "Bounds: " + view.bounds(), width - 16), x + 8, y + 74, MUTED, false);
		String counts = view.regionCount() + " regions  |  " + view.socketCount() + " sockets  |  "
				+ view.markerCount() + " markers";
		graphics.drawString(font, fit(font, counts, width - 16), x + 8, y + 85, DIM, false);

		int lineY = y + 98;
		List<DungeonBuilderStatusMessage.StatusLine> lines = view.lines();
		for (int index = 0; index < Math.min(maxLines, lines.size()); index++) {
			DungeonBuilderStatusMessage.StatusLine line = lines.get(index);
			String prefix = switch (line.status()) {
				case DungeonBuilderStatusMessage.OK -> "[OK] ";
				case DungeonBuilderStatusMessage.ERROR -> "[!] ";
				case DungeonBuilderStatusMessage.WARNING -> "[~] ";
				case DungeonBuilderStatusMessage.INFO -> "[i] ";
				default -> "[ ] ";
			};
			int color = statusColor(line.status());
			String text = prefix + line.label() + ": " + line.detail();
			graphics.drawString(font, fit(font, text, width - 16), x + 8, lineY, color, false);
			lineY += 11;
		}
		if (!view.pending().isBlank())
			graphics.drawString(font, fit(font, view.pending(), width - 16), x + 8, y + height - 12, YELLOW, false);
		else
			graphics.drawString(font, fit(font, "N: Builder Studio  |  Sneak+RMB: wand mode", width - 16),
					x + 8, y + height - 12, DIM, false);
	}

	private static String heldTool() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null)
			return "Hold a builder wand to see its mode";
		ItemStack stack = minecraft.player.getMainHandItem();
		if (!(stack.getItem() instanceof DungeonBuilderWandItem))
			stack = minecraft.player.getOffhandItem();
		if (!(stack.getItem() instanceof DungeonBuilderWandItem wand))
			return "Hold a builder wand to see its mode";
		return "Wand: " + human(wand.tool().name()) + "  |  Mode: " + human(wand.currentMode(stack));
	}

	private static String human(String value) {
		String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replace('_', ' ');
		StringBuilder result = new StringBuilder();
		for (String word : normalized.split(" ")) {
			if (word.isBlank())
				continue;
			if (!result.isEmpty())
				result.append(' ');
			result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return result.toString();
	}

	private static int statusColor(int status) {
		return switch (status) {
			case DungeonBuilderStatusMessage.OK -> GREEN;
			case DungeonBuilderStatusMessage.ERROR -> RED;
			case DungeonBuilderStatusMessage.WARNING, DungeonBuilderStatusMessage.TODO -> YELLOW;
			case DungeonBuilderStatusMessage.INFO -> CYAN;
			default -> MUTED;
		};
	}

	private static String fit(Font font, String text, int width) {
		if (font.width(text) <= width)
			return text;
		String ellipsis = "...";
		return font.plainSubstrByWidth(text, Math.max(1, width - font.width(ellipsis))) + ellipsis;
	}

	private static void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
		graphics.fill(x, y, x + width, y + height, 0xE10A1118);
		graphics.fill(x, y, x + width, y + 2, accent);
		graphics.fill(x, y + height - 1, x + width, y + height, 0xCC294452);
		graphics.fill(x, y, x + 1, y + height, 0xCC294452);
		graphics.fill(x + width - 1, y, x + width, y + height, accent);
		graphics.fill(x + 4, y + 4, x + 16, y + 5, 0xAA52DDF5);
		graphics.fill(x + 4, y + 4, x + 5, y + 16, 0xAA52DDF5);
	}
}
