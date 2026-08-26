package net.solocraft.client.gui.system;

import net.solocraft.util.OverlayLayoutConfig;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Direct-manipulation editor for the movable System overlays.
 *
 * <p>Drag an element's body to move it and drag a corner handle to resize it.
 * The world stays visible behind the editor and the real overlays keep
 * rendering, so what is being dragged is the actual HUD rather than a mockup —
 * the boxes here are just the grab regions drawn on top of them.</p>
 */
public class OverlayLayoutScreen extends Screen {
	/** Approximate footprints, used only as drag targets and outlines. */
	private static final int[][] BOUNDS = {
			// x, y, width, height at scale 1 with no offset
			{ 8, 76, 96, 30 },   // passives
			{ 8, 8, 150, 62 },   // vitals
			{ 8, 134, 130, 46 }  // quests
	};
	private static final int HANDLE = 7;
	private static final int OUTLINE = 0xFF3FC6FF;
	private static final int OUTLINE_ACTIVE = 0xFFFFD45A;
	private static final int FILL = 0x2213B8FF;
	private static final int FILL_ACTIVE = 0x33FFD45A;

	private final Screen parent;
	private int dragging = -1;
	private boolean resizing;
	private int grabOffsetX;
	private int grabOffsetY;
	private float grabScale;
	private double grabDistance;

	public OverlayLayoutScreen(Screen parent) {
		super(Component.literal("OVERLAY LAYOUT"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		int buttonY = this.height - 28;
		addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
						Component.literal("Reset All"), button -> {
							OverlayLayoutConfig.resetAll();
							dragging = -1;
						})
				.bounds(this.width / 2 - 104, buttonY, 100, 20).build());
		addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
						Component.literal("Done"), button -> onClose())
				.bounds(this.width / 2 + 4, buttonY, 100, 20).build());
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void onClose() {
		if (this.minecraft != null)
			this.minecraft.setScreen(parent);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// No darkening: the point is to see the live HUD while arranging it.
		super.render(graphics, mouseX, mouseY, partialTick);

		for (int element = 0; element < OverlayLayoutConfig.ELEMENT_COUNT; element++) {
			int[] box = boxOf(element);
			boolean active = element == dragging
					|| (dragging < 0 && contains(box, mouseX, mouseY));
			graphics.fill(box[0], box[1], box[0] + box[2], box[1] + box[3],
					active ? FILL_ACTIVE : FILL);
			outline(graphics, box[0], box[1], box[2], box[3],
					active ? OUTLINE_ACTIVE : OUTLINE);

			// Corner handles, drawn last so they stay grabbable visually.
			for (int[] handle : handlesOf(box))
				graphics.fill(handle[0], handle[1], handle[0] + HANDLE,
						handle[1] + HANDLE, active ? OUTLINE_ACTIVE : OUTLINE);

			String label = OverlayLayoutConfig.label(element)
					+ "  " + Math.round(OverlayLayoutConfig.getScale(element) * 100.0F) + "%";
			graphics.drawString(this.font, label, box[0] + 3, box[1] - 10,
					active ? OUTLINE_ACTIVE : 0xFFBBD6E8, true);
		}

		graphics.drawCenteredString(this.font,
				"Drag an element to move it · drag a corner to resize · right-click to reset one",
				this.width / 2, this.height - 44, 0xFF9DB5C7);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button))
			return true;
		for (int element = OverlayLayoutConfig.ELEMENT_COUNT - 1; element >= 0; element--) {
			int[] box = boxOf(element);
			boolean onHandle = onAnyHandle(box, mouseX, mouseY);
			if (!onHandle && !contains(box, mouseX, mouseY))
				continue;
			if (button == 1) {
				OverlayLayoutConfig.reset(element);
				return true;
			}
			if (button != 0)
				continue;
			dragging = element;
			resizing = onHandle;
			grabScale = OverlayLayoutConfig.getScale(element);
			if (resizing) {
				// Resize tracks distance from the element's anchor, so dragging
				// away from the corner grows it and toward it shrinks it.
				grabDistance = Math.max(1.0D, distance(box[0], box[1], mouseX, mouseY));
			} else {
				grabOffsetX = (int) mouseX - box[0];
				grabOffsetY = (int) mouseY - box[1];
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button,
			double deltaX, double deltaY) {
		if (dragging < 0)
			return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		int[] base = BOUNDS[dragging];
		if (resizing) {
			int[] box = boxOf(dragging);
			double current = Math.max(1.0D, distance(box[0], box[1], mouseX, mouseY));
			float scale = (float) (grabScale * (current / grabDistance));
			OverlayLayoutConfig.setScale(dragging, scale);
		} else {
			int targetX = (int) mouseX - grabOffsetX;
			int targetY = (int) mouseY - grabOffsetY;
			// Keep at least a corner of the element on screen.
			targetX = Mth.clamp(targetX, -base[2] + 12, this.width - 12);
			targetY = Mth.clamp(targetY, 0, this.height - 12);
			OverlayLayoutConfig.setOffset(dragging, targetX - base[0], targetY - base[1]);
		}
		return true;
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragging >= 0) {
			dragging = -1;
			resizing = false;
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	/** Current screen-space box for an element, offset and scale applied. */
	private int[] boxOf(int element) {
		int[] base = BOUNDS[element];
		float scale = OverlayLayoutConfig.getScale(element);
		return new int[] {
				base[0] + OverlayLayoutConfig.getOffsetX(element),
				base[1] + OverlayLayoutConfig.getOffsetY(element),
				Math.max(16, Math.round(base[2] * scale)),
				Math.max(12, Math.round(base[3] * scale))
		};
	}

	private static int[][] handlesOf(int[] box) {
		int right = box[0] + box[2] - HANDLE;
		int bottom = box[1] + box[3] - HANDLE;
		return new int[][] {
				{ box[0], box[1] }, { right, box[1] },
				{ box[0], bottom }, { right, bottom }
		};
	}

	private static boolean onAnyHandle(int[] box, double mouseX, double mouseY) {
		for (int[] handle : handlesOf(box)) {
			if (mouseX >= handle[0] && mouseX <= handle[0] + HANDLE
					&& mouseY >= handle[1] && mouseY <= handle[1] + HANDLE)
				return true;
		}
		return false;
	}

	private static boolean contains(int[] box, double mouseX, double mouseY) {
		return mouseX >= box[0] && mouseX <= box[0] + box[2]
				&& mouseY >= box[1] && mouseY <= box[1] + box[3];
	}

	private static double distance(int x, int y, double mouseX, double mouseY) {
		double dx = mouseX - x;
		double dy = mouseY - y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	private static void outline(GuiGraphics graphics, int x, int y, int width,
			int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}
}
