package net.solocraft.client.gui.system;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.shader.HunterEvaluationBackgroundRenderTypes;
import net.solocraft.network.HunterEvaluationActionMessage;
import net.solocraft.util.ClassStyleRules;
import net.solocraft.util.HunterEvaluationRules;
import net.solocraft.util.HunterEvaluationRules.Action;
import net.solocraft.util.HunterEvaluationRules.Mode;
import net.solocraft.util.HunterEvaluationRules.Phase;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.shaders.AbstractUniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import org.joml.Matrix4f;

import java.util.UUID;

/**
 * Evaluator-centered ceremony. The UI renders only authoritative snapshots and
 * sends action intents with the current session UUID.
 *
 * <p>The screen is built around one physical gesture: press and hold on the
 * appraisal gem. Everything in the chamber exists to make that hold legible —
 * a charge ring that fills, mana drawn inward, and the gem flooding with light
 * from the base up. Releasing early drains it back and says so.
 */
public final class HunterEvaluationScreen extends SystemScreen {
	private static final int PANEL_W = 340;
	private static final int PANEL_H = 320;

	private static final int HEADER_H = 22;
	private static final int STEP_Y = 28;
	private static final int STEP_H = 13;
	private static final int CHAMBER_Y = 46;
	private static final int CHAMBER_H = 152;
	private static final int CHAMBER_HEADER_H = 16;
	/** Panel-relative centre of the appraisal gem. */
	private static final int GEM_CY = 130;
	private static final int RING_RADIUS = 52;
	private static final int GEM_HALF_W = 21;
	private static final int GEM_HALF_H = 35;
	/** Gem cut proportions: table edge, then crown, belt and pavilion. */
	private static final float GEM_TABLE = 0.46F;
	private static final float GEM_CROWN = 0.26F;
	private static final float GEM_BELT = 0.52F;
	private static final int READOUT_Y = 204;
	private static final int STYLE_Y = 231;
	private static final int PROGRESS_Y = 246;
	private static final int DETAIL_Y = 255;
	private static final int BUTTON_Y = 270;
	private static final int ACCEPT_Y = 294;

	private static final int INK = 0xFF03060E;
	private static final int STEEL = 0xFF23374A;
	private static final int TEXT_DIM = 0xFF6D8295;
	private static final int TEXT_BODY = 0xFF9DB5C7;
	private static final String[] STAGE_NAMES =
			{ "CONTACT", "SCAN", "CLASS", "RANK", "RECORD" };
	private static final String[] STYLE_STAGE_NAMES =
			{ "CONTACT", "SCAN", "CLASS", "STYLE", "RANK", "RECORD" };
	private static final String[] RANK_LETTERS = { "E", "D", "C", "B", "A", "S" };

	private UUID sessionId;
	private Mode mode;
	private Phase phase;
	private int classId;
	private int rank;
	private int previousRank;
	private int phaseDurationTicks;
	private int remainingTicks;
	private boolean canReroll;
	private boolean fixedClass;
	private int styleId;
	private boolean canRerollStyle;
	private long stateReceivedAt;
	private long clickWaveAt;
	private long contactLostAt;
	private boolean holdingContact;
	private boolean actionPending;

	/** Eased 0..1 charge shown on the gem and ring; drains when contact breaks. */
	private float holdCharge;
	private boolean gemHovered;

	private SystemButton rerollButton;
	private SystemButton rerollStyleButton;
	private SystemButton acceptButton;
	private SystemButton doneButton;

	private HunterEvaluationScreen(UUID sessionId, int mode, int phase,
			int classId, int rank, int previousRank, int phaseDurationTicks,
			int remainingTicks, boolean canReroll, boolean fixedClass,
			int styleId, boolean canRerollStyle) {
		super(Component.literal("HUNTER EVALUATION"));
		this.panelW = PANEL_W;
		this.panelH = PANEL_H;
		updateState(sessionId, mode, phase, classId, rank, previousRank,
				phaseDurationTicks, remainingTicks, canReroll, fixedClass,
				styleId, canRerollStyle);
	}

	@Override
	protected void init() {
		super.init();
		int buttonY = panelY + BUTTON_Y;
		rerollButton = addRenderableWidget(new SystemButton(
				panelX + 34, buttonY, 132, 21,
				Component.literal("REROLL CLASS"),
				button -> send(Action.REROLL_CLASS)));
		rerollStyleButton = addRenderableWidget(new SystemButton(
				panelX + 174, buttonY, 132, 21,
				Component.literal("REROLL STYLE"),
				button -> send(Action.REROLL_STYLE)));
		acceptButton = addRenderableWidget(new SystemButton(
				panelX + 95, panelY + ACCEPT_Y, 150, 21,
				Component.literal("ACCEPT RESULT"),
				button -> send(Action.ACCEPT_RESULT)));
		doneButton = addRenderableWidget(new SystemButton(
				panelX + 95, panelY + ACCEPT_Y, 150, 21,
				Component.literal("CLOSE RECORD"),
				button -> send(Action.ACKNOWLEDGE)));
	}

	@Override
	protected boolean allowsNonSystemAccess() {
		return true;
	}

	@Override
	protected boolean shouldPlaySystemSounds() {
		return false;
	}

	// ── background ─────────────────────────────────────────────────────────────

	@Override
	protected void renderAnimatedBackground(GuiGraphics graphics, int mouseX,
			int mouseY) {
		float localX = clamp01((mouseX - panelX) / (float) panelW);
		float localY = clamp01((mouseY - panelY) / (float) panelH);
		ShaderInstance shader = HunterEvaluationBackgroundRenderTypes.get();
		if (shader == null) {
			renderFallback(graphics);
			return;
		}

		int color = activeColor();
		float red = ((color >> 16) & 0xFF) / 255.0F;
		float green = ((color >> 8) & 0xFF) / 255.0F;
		float blue = (color & 0xFF) / 255.0F;
		float intensity = rank > 0
				? HunterEvaluationRules.rankIntensity(rank) : 0.34F;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableCull();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShader(HunterEvaluationBackgroundRenderTypes::get);
		AbstractUniform mouse = shader.safeGetUniform("MousePos");
		mouse.set(localX, localY);
		shader.safeGetUniform("ClassColor").set(red, green, blue);
		shader.safeGetUniform("RankIntensity").set(intensity);
		shader.safeGetUniform("WaveStrength").set(waveStrength());
		shader.safeGetUniform("Reveal").set(revealAmount());
		shader.safeGetUniform("HoldCharge").set(holdCharge);
		shader.safeGetUniform("ScanSweep").set(scanSweep());

		int x0 = panelX;
		int y0 = panelY;
		int x1 = panelX + panelW;
		int y1 = panelY + panelH;
		Matrix4f matrix = graphics.pose().last().pose();
		BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
				DefaultVertexFormat.POSITION_TEX);
		buffer.addVertex(matrix, x0, y1, 0).setUv(0.0F, 1.0F);
		buffer.addVertex(matrix, x1, y1, 0).setUv(1.0F, 1.0F);
		buffer.addVertex(matrix, x1, y0, 0).setUv(1.0F, 0.0F);
		buffer.addVertex(matrix, x0, y0, 0).setUv(0.0F, 0.0F);
		BufferUploader.drawWithShader(buffer.buildOrThrow());
		RenderSystem.enableCull();
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void renderFallback(GuiGraphics graphics) {
		int color = activeColor();
		graphics.fillGradient(panelX, panelY, panelX + panelW,
				panelY + panelH, mix(0xFF02040B, color, 0.14F), 0xFF010106);
		float time = (Util.getMillis() % 600000L) / 1000.0F;
		for (int i = 0; i < 42; i++) {
			float seed = i * 17.913F;
			float x = frac((float) Math.sin(seed) * 43758.545F);
			float y = frac((float) Math.sin(seed * 1.71F) * 24634.634F);
			int px = panelX + (int) (x * panelW);
			int py = panelY + (int) ((y * panelH
					+ time * (4.0F + x * 11.0F)) % panelH);
			graphics.fill(px, py, px + 1, py + 1, withAlpha(color, 0x64));
		}
	}

	// ── chrome ─────────────────────────────────────────────────────────────────

	@Override
	protected void renderFrame(GuiGraphics graphics) {
		// GuiGraphics flushes per fill outside a managed batch, so every draw
		// below would otherwise be its own GL draw call.
		graphics.drawManaged(() -> renderFrameBody(graphics));
	}

	private void renderFrameBody(GuiGraphics graphics) {
		int color = activeColor();
		int soft = withAlpha(color, 0x55);
		int dim = mix(0xFF102638, color, 0.52F);

		graphics.fill(panelX - 2, panelY - 2, panelX + panelW + 2, panelY - 1,
				withAlpha(color, 0x22));
		graphics.fill(panelX - 2, panelY + panelH + 1, panelX + panelW + 2,
				panelY + panelH + 2, withAlpha(color, 0x22));
		graphics.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY, soft);
		graphics.fill(panelX - 1, panelY + panelH, panelX + panelW + 1,
				panelY + panelH + 1, soft);
		graphics.fill(panelX - 1, panelY, panelX, panelY + panelH, soft);
		graphics.fill(panelX + panelW, panelY, panelX + panelW + 1,
				panelY + panelH, soft);
		outline(graphics, panelX, panelY, panelW, panelH, dim);

		graphics.fillGradient(panelX, panelY, panelX + panelW,
				panelY + HEADER_H, withAlpha(mix(0xFF02050C, color, 0.22F), 0xE6),
				withAlpha(mix(0xFF02050C, color, 0.08F), 0xC4));
		graphics.fill(panelX, panelY + 7, panelX + panelW, panelY + 8,
				withAlpha(0xFFFFFFFF, 0x0E));
		graphics.fill(panelX, panelY + HEADER_H - 1, panelX + panelW,
				panelY + HEADER_H, color);
		drawCorners(graphics, color);

		String titleText = this.title.getString();
		int titleColor = rank == 6 ? 0xFFFFFFFF : color;
		int titleWidth = trackedWidth(titleText, 1);
		int titleX = panelX + (panelW - titleWidth) / 2;
		drawTracked(graphics, titleText, titleX + 1, panelY + 8, 0xCC000308, 1);
		drawTracked(graphics, titleText, titleX, panelY + 7, titleColor, 1);
		drawDiamond(graphics, titleX - 9, panelY + 11, 3, withAlpha(color, 0xB0));
		drawDiamond(graphics, titleX + titleWidth + 8, panelY + 11, 3,
				withAlpha(color, 0xB0));

		String modeTag = mode == Mode.REEVALUATION ? "RE-EVAL" : "INITIAL";
		graphics.drawString(this.font, modeTag,
				panelX + panelW - 8 - this.font.width(modeTag), panelY + 7,
				withAlpha(color, 0x99), false);
	}

	// ── content ────────────────────────────────────────────────────────────────

	@Override
	protected void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
			float partialTicks) {
		updateButtons();
		advanceCharge();
		this.gemHovered = phase == Phase.CONTACT
				&& isInsideChamber(mouseX, mouseY);
		graphics.drawManaged(() -> renderContentBody(graphics));
	}

	private void renderContentBody(GuiGraphics graphics) {
		int color = activeColor();
		int centerX = panelX + panelW / 2;

		renderStepper(graphics, panelX + 18, panelY + STEP_Y, panelW - 36, color);
		renderChamber(graphics, centerX, color);
		renderReadout(graphics, color);
		renderStyleReadout(graphics, centerX, color);
		renderProgress(graphics, centerX, color);
	}

	/** Breadcrumb of the five ceremony stages, so the phase text has context. */
	private void renderStepper(GuiGraphics graphics, int x, int y, int width,
			int color) {
		boolean styled = ClassStyleRules.supportsStyles(classId);
		String[] stages = styled ? STYLE_STAGE_NAMES : STAGE_NAMES;
		int current = stageOf(phase, styled);
		int segments = stages.length;
		for (int index = 0; index < segments; index++) {
			int x0 = x + (width * index) / segments;
			int x1 = x + (width * (index + 1)) / segments - 2;
			boolean done = index < current;
			boolean active = index == current;
			int fill = done ? withAlpha(color, 0x59)
					: active ? withAlpha(color, 0x8C) : 0x66091421;
			graphics.fill(x0, y, x1, y + STEP_H, fill);
			graphics.fill(x0, y, x1, y + 1,
					done || active ? color : STEEL);
			if (active) {
				int alpha = 120 + (int) (110.0F * pulse01());
				graphics.fill(x0, y + STEP_H - 2, x1, y + STEP_H,
						withAlpha(color, alpha));
			}
			String label = stages[index];
			int labelColor = active ? 0xFFFFFFFF : done ? TEXT_BODY : TEXT_DIM;
			graphics.drawString(this.font, label,
					x0 + (x1 - x0 - this.font.width(label)) / 2, y + 3,
					labelColor, false);
		}
	}

	private void renderChamber(GuiGraphics graphics, int centerX, int color) {
		int x = panelX + 18;
		int y = panelY + CHAMBER_Y;
		int width = panelW - 36;
		int gemCenterY = panelY + GEM_CY;

		graphics.fillGradient(x, y, x + width, y + CHAMBER_H, 0x66020610,
				0x8C01040A);
		outline(graphics, x, y, width, CHAMBER_H, withAlpha(color, 0x7A));
		graphics.fill(x + 1, y + 1, x + width - 1, y + CHAMBER_HEADER_H - 1,
				withAlpha(mix(INK, color, 0.10F), 0xC4));
		graphics.fill(x + 1, y + CHAMBER_HEADER_H - 1, x + width - 1,
				y + CHAMBER_HEADER_H, withAlpha(color, 0x6B));
		String title = phaseTitle();
		if (isSRankError()) {
			drawGlitchText(graphics, title, centerX, y + 4, 17);
		} else {
			int titleColor = rank == 6 && phase == Phase.RANK_REVEAL
					? 0xFFFFFFFF : color;
			drawTracked(graphics, title,
					centerX - trackedWidth(title, 1) / 2, y + 4, titleColor, 1);
		}

		renderChargeRing(graphics, centerX, gemCenterY, color);
		renderGem(graphics, centerX, gemCenterY, color);
		drawClickWave(graphics, centerX, gemCenterY, color);
		renderPrompt(graphics, centerX, y + CHAMBER_H - 13, color);

		if (isSRankError())
			drawGlitchTears(graphics, x + 1, y + CHAMBER_HEADER_H,
					width - 2, CHAMBER_H - CHAMBER_HEADER_H - 1);
		float flash = sRankResolveFlash();
		if (flash > 0.0F)
			graphics.fill(x + 1, y + CHAMBER_HEADER_H, x + width - 1,
					y + CHAMBER_H - 1,
					withAlpha(0xFFFFFFFF, (int) (0xD8 * flash)));
	}

	/**
	 * Dial around the gem: a dim track, twelve ticks, and an arc that sweeps as
	 * the hold builds. This is the primary feedback for the press-and-hold.
	 */
	private void renderChargeRing(GuiGraphics graphics, int centerX, int centerY,
			int color) {
		strokeCircle(graphics, centerX, centerY, RING_RADIUS, 1,
				withAlpha(color, 0x3D));
		for (int tick = 0; tick < 12; tick++) {
			double angle = -Math.PI / 2.0 + tick * Math.PI / 6.0;
			boolean major = tick % 3 == 0;
			int inner = RING_RADIUS + (major ? 3 : 4);
			int outer = RING_RADIUS + (major ? 9 : 7);
			int tx0 = centerX + (int) Math.round(Math.cos(angle) * inner);
			int ty0 = centerY + (int) Math.round(Math.sin(angle) * inner);
			int tx1 = centerX + (int) Math.round(Math.cos(angle) * outer);
			int ty1 = centerY + (int) Math.round(Math.sin(angle) * outer);
			boolean reached = holdCharge >= tick / 12.0F;
			drawLine(graphics, tx0, ty0, tx1, ty1,
					withAlpha(reached ? color : STEEL, reached ? 0xD8 : 0x8C));
		}
		if (holdCharge > 0.004F) {
			strokeArc(graphics, centerX, centerY, RING_RADIUS, 3, -90.0F,
					360.0F * holdCharge, withAlpha(color, 0xF2));
			strokeArc(graphics, centerX, centerY, RING_RADIUS + 3, 1, -90.0F,
					360.0F * holdCharge, withAlpha(color, 0x66));
			// Bright head on the leading edge of the sweep.
			double head = Math.toRadians(-90.0F + 360.0F * holdCharge);
			int hx = centerX + (int) Math.round(Math.cos(head) * RING_RADIUS);
			int hy = centerY + (int) Math.round(Math.sin(head) * RING_RADIUS);
			graphics.fill(hx - 2, hy - 2, hx + 3, hy + 3,
					mix(color, 0xFFFFFFFF, 0.55F));
		}
		// A break in contact drains the ring and flashes the track red.
		float lost = contactLostAt == 0L ? 0.0F
				: Math.max(0.0F, 1.0F - (Util.getMillis() - contactLostAt) / 520.0F);
		if (lost > 0.0F)
			strokeCircle(graphics, centerX, centerY, RING_RADIUS + 5, 1,
					withAlpha(0xFFFF5B6E, (int) (0xC0 * lost)));
	}

	private void renderGem(GuiGraphics graphics, int centerX, int centerY,
			int color) {
		float reveal = revealAmount();
		float pulse = pulse01();

		// A faulting reading shakes the stone and strobes it toward alarm red.
		if (isSRankError()) {
			int slot = (int) (Util.getMillis() / 55L);
			centerX += Math.round(hash01(slot, 3) * 5.0F) - 2;
			centerY += Math.round(hash01(7, slot) * 3.0F) - 1;
			color = hash01(slot, slot) > 0.5F
					? mix(color, 0xFFFF2A4B, 0.75F)
					: mix(color, 0xFFFFFFFF, 0.35F);
		}

		// Stacked discs, widest first: a single translucent circle reads as a flat
		// plate, whereas five overlapping ones accumulate into a soft falloff.
		int auraAlpha = 4 + (int) (4 * pulse) + (int) (7 * holdCharge);
		for (int layer = 4; layer >= 0; layer--)
			fillCircle(graphics, centerX, centerY, GEM_HALF_H + 2 + layer * 5,
					withAlpha(color, auraAlpha));
		if (gemHovered)
			strokeCircle(graphics, centerX, centerY, GEM_HALF_H + 12, 1,
					withAlpha(color, 0x7A));

		// Mana drawn inward while the hold builds.
		if (holdCharge > 0.02F) {
			for (int mote = 0; mote < 18; mote++) {
				float travel = ((Util.getMillis() / 880.0F) + mote * 0.137F) % 1.0F;
				float distance = (1.0F - travel) * (RING_RADIUS + 16);
				double angle = mote * 2.3999 + Util.getMillis() / 1500.0;
				int mx = centerX + (int) Math.round(Math.cos(angle) * distance);
				int my = centerY + (int) Math.round(Math.sin(angle) * distance);
				int alpha = (int) (210 * travel * holdCharge);
				int size = travel > 0.72F ? 2 : 1;
				graphics.fill(mx, my, mx + size, my + size,
						withAlpha(mix(color, 0xFFFFFFFF, 0.45F), alpha));
			}
		}

		// Faceted body, scanline by scanline, flooding with light from the base.
		int fillLine = centerY + GEM_HALF_H
				- Math.round(holdCharge * GEM_HALF_H * 2.0F);
		for (int dy = -GEM_HALF_H; dy <= GEM_HALF_H; dy++) {
			int halfWidth = gemHalfWidthAt(dy);
			if (halfWidth <= 0)
				continue;
			int y = centerY + dy;
			boolean charged = y >= fillLine;
			int body = charged ? mix(color, 0xFFFFFFFF, 0.34F)
					: mix(0xFF060C16, color, 0.20F + reveal * 0.30F);
			graphics.fill(centerX - halfWidth, y, centerX + halfWidth, y + 1,
					withAlpha(body, charged ? 0xE6 : 0xC4));
			// Light from the upper left: shade the right flank, catch the left.
			int shade = Math.max(1, halfWidth / 3);
			graphics.fill(centerX + halfWidth - shade, y, centerX + halfWidth,
					y + 1, 0x3A000000);
			graphics.fill(centerX - halfWidth, y, centerX - halfWidth + 1, y + 1,
					withAlpha(mix(body, 0xFFFFFFFF, 0.55F), 0xC4));
		}

		// Meniscus: a bright line where the light has risen to.
		if (holdCharge > 0.01F && holdCharge < 0.995F) {
			int halfWidth = gemHalfWidthAt(fillLine - centerY);
			if (halfWidth > 0) {
				graphics.fill(centerX - halfWidth, fillLine, centerX + halfWidth,
						fillLine + 1, mix(color, 0xFFFFFFFF, 0.8F));
				graphics.fill(centerX - halfWidth, fillLine + 1,
						centerX + halfWidth, fillLine + 2,
						withAlpha(0xFFFFFFFF, 0x6B));
			}
		}

		// Facet seams: table, crown, belt and pavilion.
		int tableHalf = Math.round(GEM_HALF_W * GEM_TABLE);
		int tableY = centerY - GEM_HALF_H;
		int beltTop = centerY - GEM_HALF_H
				+ Math.round(GEM_HALF_H * 2 * GEM_CROWN);
		int beltBottom = centerY - GEM_HALF_H
				+ Math.round(GEM_HALF_H * 2 * GEM_BELT);
		int seam = withAlpha(mix(color, 0xFFFFFFFF, 0.65F), 0x6B);
		drawLine(graphics, centerX - tableHalf, tableY,
				centerX - GEM_HALF_W, beltTop, seam);
		drawLine(graphics, centerX + tableHalf, tableY,
				centerX + GEM_HALF_W, beltTop, seam);
		drawLine(graphics, centerX - GEM_HALF_W, beltBottom,
				centerX, centerY + GEM_HALF_H, seam);
		drawLine(graphics, centerX + GEM_HALF_W, beltBottom,
				centerX, centerY + GEM_HALF_H, seam);
		// Table rim reads as the polished flat top of the cut.
		graphics.fill(centerX - tableHalf, tableY, centerX + tableHalf,
				tableY + 1, withAlpha(mix(color, 0xFFFFFFFF, 0.85F), 0xD8));
		graphics.fill(centerX - tableHalf, beltTop, centerX + tableHalf,
				beltTop + 1, seam);
		graphics.fill(centerX - GEM_HALF_W, beltTop, centerX + GEM_HALF_W,
				beltTop + 1, seam);
		graphics.fill(centerX - GEM_HALF_W, beltBottom, centerX + GEM_HALF_W,
				beltBottom + 1, seam);
		graphics.fill(centerX, beltBottom, centerX + 1, centerY + GEM_HALF_H,
				withAlpha(0xFFFFFFFF, 0x26));

		// Core: brightens with the hold, whites out entirely for an S rank.
		int coreRadius = 4 + Math.round((holdCharge * 0.6F + reveal * 0.4F) * 6.0F);
		int core = rank == 6 && reveal > 0.5F ? 0xFFFFFFFF
				: mix(color, 0xFFFFFFFF, 0.35F + holdCharge * 0.45F);
		fillCircle(graphics, centerX, centerY, coreRadius,
				withAlpha(core, 0xB4 + (int) (0x40 * pulse)));
		fillCircle(graphics, centerX, centerY, Math.max(1, coreRadius - 3),
				withAlpha(0xFFFFFFFF, 0x8C + (int) (0x50 * holdCharge)));
	}

	/**
	 * Half-width of the gem silhouette at a vertical offset from its centre:
	 * flat table, flared crown, straight belt, then a taper to the culet.
	 */
	private static int gemHalfWidthAt(int dy) {
		float t = (dy + GEM_HALF_H) / (float) (GEM_HALF_H * 2);
		if (t < 0.0F || t > 1.0F)
			return 0;
		float profile = t < GEM_CROWN
				? GEM_TABLE + (t / GEM_CROWN) * (1.0F - GEM_TABLE)
				: t < GEM_BELT ? 1.0F : (1.0F - t) / (1.0F - GEM_BELT);
		return Math.round(GEM_HALF_W * Math.max(0.0F, profile));
	}

	private void renderPrompt(GuiGraphics graphics, int centerX, int y,
			int color) {
		if (isSRankError()) {
			drawGlitchText(graphics, "VALUE OUT OF RANGE", centerX, y, 73);
			return;
		}
		float lost = contactLostAt == 0L ? 0.0F
				: Math.max(0.0F, 1.0F - (Util.getMillis() - contactLostAt) / 900.0F);
		if (lost > 0.0F && phase == Phase.CONTACT) {
			String message = "CONTACT LOST - HOLD AGAIN";
			graphics.drawCenteredString(font, message, centerX, y,
					withAlpha(0xFFFF6B7C, (int) (0xFF * lost)));
			return;
		}
		if (phase == Phase.CONTACT) {
			if (holdingContact) {
				String message = "HOLD STEADY  "
						+ Math.round(holdCharge * 100.0F) + "%";
				graphics.drawCenteredString(font, message, centerX, y,
						mix(color, 0xFFFFFFFF, 0.5F));
			} else {
				int alpha = 0x9E + (int) (0x61 * pulse01());
				graphics.drawCenteredString(font, "PRESS AND HOLD THE GEM",
						centerX, y, withAlpha(0xFFE8F6FF, alpha));
			}
			return;
		}
		if (phase == Phase.DECISION && mode == Mode.INITIAL && fixedClass) {
			graphics.drawCenteredString(font,
					canRerollStyle
							? "Class preserved; " + styleOwnerLabel()
									+ " may be rerolled."
							: "Legacy class preserved; rank certification only.",
					centerX, y, TEXT_BODY);
		} else if (phase == Phase.DECISION && mode == Mode.REEVALUATION) {
			String comparison = previousRank > 0
					? HunterEvaluationRules.rankName(previousRank)
							+ "  >  " + HunterEvaluationRules.rankName(rank)
					: "CERTIFY " + HunterEvaluationRules.rankName(rank);
			graphics.drawCenteredString(font, comparison, centerX, y,
					0xFFDCEEFF);
		}
	}

	/** Class crest on the left, rank ladder on the right. */
	private void renderReadout(GuiGraphics graphics, int color) {
		int y = panelY + READOUT_Y;
		boolean hasClass = classId > 0;

		int crestX = panelX + 20;
		graphics.fill(crestX, y, crestX + 26, y + 26,
				withAlpha(mix(INK, color, 0.16F), 0xB4));
		outline(graphics, crestX, y, 26, 26,
				withAlpha(hasClass ? color : STEEL, 0xB4));
		if (hasClass)
			drawClassGlyph(graphics, crestX + 13, y + 13, classId,
					rank == 6 ? 0xFFFFFFFF : color);
		else
			drawDiamond(graphics, crestX + 13, y + 13, 6, withAlpha(STEEL, 0xC4));

		drawTracked(graphics, "CLASS", crestX + 34, y + 2, TEXT_DIM, 1);
		String className = hasClass
				? HunterEvaluationRules.className(classId).toUpperCase()
				: "UNRESOLVED";
		graphics.drawString(this.font, className, crestX + 34, y + 14,
				hasClass ? (rank == 6 ? 0xFFFFFFFF : color) : TEXT_DIM, false);

		int ladderWidth = RANK_LETTERS.length * 21 - 3;
		int ladderX = panelX + panelW - 20 - ladderWidth;
		String rankLabel = "RANK";
		drawTracked(graphics, rankLabel,
				panelX + panelW - 20 - trackedWidth(rankLabel, 1), y + 2,
				TEXT_DIM, 1);
		renderRankLadder(graphics, ladderX, y + 13, color);
	}

	private void renderStyleReadout(GuiGraphics graphics, int centerX,
			int color) {
		if (!ClassStyleRules.supportsStyles(classId))
			return;
		int y = panelY + STYLE_Y;
		int width = panelW - 40;
		graphics.fill(panelX + 20, y, panelX + 20 + width, y + 12,
				withAlpha(mix(INK, color, 0.13F), 0xA8));
		outline(graphics, panelX + 20, y, width, 12,
				withAlpha(styleId > 0 ? color : STEEL, 0x78));
		String label = styleId > 0
				? "STYLE  " + ClassStyleRules.styleName(classId, styleId)
						.toUpperCase()
				: "STYLE  ANALYZING " + styleNoun(true);
		graphics.drawCenteredString(font, label, centerX, y + 2,
				styleId > 0 ? color : TEXT_DIM);
	}

	/**
	 * Six cells, E through S. During the reveal they light in sequence so the
	 * measurement reads as climbing rather than snapping to a value.
	 */
	private void renderRankLadder(GuiGraphics graphics, int x, int y, int color) {
		int width = RANK_LETTERS.length * 21 - 3;
		// While the evaluator is faulting it reports no scale at all.
		if (isSRankError()) {
			graphics.fill(x, y, x + width, y + 13, 0x59180810);
			graphics.fill(x, y + 12, x + width, y + 13, 0xFFFF2A4B);
			drawGlitchText(graphics, "ERROR", x + width / 2, y + 3, 41);
			return;
		}
		boolean revealing = phase == Phase.RANK_REVEAL;
		float progress = revealing ? rankRevealProgress() : 1.0F;
		int lit = rank <= 0 ? 0 : Math.max(1, Math.round(rank * progress));
		for (int index = 0; index < RANK_LETTERS.length; index++) {
			int cellX = x + index * 21;
			boolean on = index < lit;
			boolean peak = index == lit - 1 && lit > 0;
			int fill = on ? withAlpha(color, peak ? 0xB4 : 0x66) : 0x59081321;
			graphics.fill(cellX, y, cellX + 18, y + 13, fill);
			graphics.fill(cellX, y + 12, cellX + 18, y + 13,
					on ? color : STEEL);
			if (peak) {
				outline(graphics, cellX - 1, y - 1, 20, 15,
						index == 5 ? 0xFFFFFFFF : mix(color, 0xFFFFFFFF, 0.5F));
				int alpha = 60 + (int) (90.0F * pulse01());
				graphics.fill(cellX, y, cellX + 18, y + 13,
						withAlpha(0xFFFFFFFF, alpha / 3));
			}
			String letter = RANK_LETTERS[index];
			graphics.drawString(this.font, letter,
					cellX + (18 - this.font.width(letter)) / 2, y + 3,
					on ? (peak ? 0xFFFFFFFF : 0xFFE8F6FF) : TEXT_DIM, false);
		}
	}

	private void renderProgress(GuiGraphics graphics, int centerX, int color) {
		int x = panelX + 20;
		int width = panelW - 40;
		int y = panelY + PROGRESS_Y;
		graphics.fill(x, y, x + width, y + 4, 0x7A0C1A26);
		int filled = Math.round(width * phaseProgress());
		graphics.fill(x, y, x + filled, y + 4,
				rank == 6 ? 0xFFFFFFFF : color);
		if (filled > 2)
			graphics.fill(x + filled - 2, y, x + filled, y + 4,
					mix(color, 0xFFFFFFFF, 0.7F));
		// Segment ticks give the bar a sense of scale.
		for (int tick = 1; tick < 4; tick++)
			graphics.fill(x + width * tick / 4, y, x + width * tick / 4 + 1,
					y + 4, 0x59000000);
		graphics.drawCenteredString(font, phaseDetail(), centerX,
				panelY + DETAIL_Y, TEXT_BODY);
	}

	// ── input ──────────────────────────────────────────────────────────────────

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		double logicalX = logicalMouseX(mouseX);
		double logicalY = logicalMouseY(mouseY);
		if (button == 0 && phase == Phase.CONTACT
				&& isInsideChamber(logicalX, logicalY)
				&& !holdingContact) {
			holdingContact = true;
			clickWaveAt = Util.getMillis();
			contactLostAt = 0L;
			// Sent immediately, like its matching cancel: the one-shot guard on
			// send() is for commit actions, and letting it swallow a press would
			// leave the client believing it holds contact the server never got.
			sendImmediate(Action.BEGIN_CONTACT);
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && holdingContact) {
			holdingContact = false;
			if (phase == Phase.CONTACT) {
				contactLostAt = Util.getMillis();
				sendImmediate(Action.CANCEL_CONTACT);
			}
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	protected void beginClose() {
		if (holdingContact) {
			holdingContact = false;
			sendImmediate(Action.CANCEL_CONTACT);
		}
		super.beginClose();
	}

	@Override
	public void onClose() {
		if (holdingContact) {
			holdingContact = false;
			sendImmediate(Action.CANCEL_CONTACT);
		}
		super.onClose();
	}

	private boolean isInsideChamber(double mouseX, double mouseY) {
		int x = panelX + 18;
		int y = panelY + CHAMBER_Y + CHAMBER_HEADER_H;
		return mouseX >= x && mouseX < x + panelW - 36
				&& mouseY >= y && mouseY < panelY + CHAMBER_Y + CHAMBER_H;
	}

	private void updateButtons() {
		if (rerollButton == null || rerollStyleButton == null
				|| acceptButton == null || doneButton == null)
			return;
		boolean decision = phase == Phase.DECISION;
		rerollButton.visible = decision && mode == Mode.INITIAL && canReroll;
		rerollStyleButton.visible = decision && mode == Mode.INITIAL
				&& canRerollStyle;
		acceptButton.visible = decision;
		doneButton.visible = phase == Phase.COMPLETE;
		if (mode == Mode.REEVALUATION)
			acceptButton.setMessage(Component.literal(
					rank > previousRank ? "CERTIFY NEW RANK" : "CONFIRM RECORD"));
		else
			acceptButton.setMessage(Component.literal("ACCEPT RESULT"));
		boolean classChoice = rerollButton.visible;
		boolean styleChoice = rerollStyleButton.visible;
		if (classChoice && styleChoice) {
			rerollButton.setX(panelX + 34);
			rerollStyleButton.setX(panelX + 174);
		} else if (classChoice) {
			rerollButton.setX(panelX + 104);
		} else if (styleChoice) {
			rerollStyleButton.setX(panelX + 104);
		}
		acceptButton.setX(panelX + 95);
		acceptButton.setWidth(150);
	}

	/**
	 * Server ticks drive the authoritative countdown, but the local charge is
	 * eased so releasing drains visibly instead of snapping, and so the local
	 * extrapolation cannot keep filling after contact breaks.
	 */
	private void advanceCharge() {
		float target;
		if (phase == Phase.CONTACT)
			target = holdingContact ? phaseProgress() : 0.0F;
		else if (phase == Phase.BOOT)
			target = 0.0F;
		else
			target = 1.0F;
		float rate = target > holdCharge ? 0.20F : 0.28F;
		holdCharge += (target - holdCharge) * rate;
		if (Math.abs(target - holdCharge) < 0.004F)
			holdCharge = target;
	}

	// ── phase copy ─────────────────────────────────────────────────────────────

	/**
	 * The evaluator is one machine measuring six different kinds of hunter, so
	 * the wording tracks the class in front of it. A Mage's style is a mana
	 * aspect; everyone else's is a combat discipline.
	 */
	private String styleNoun(boolean upper) {
		boolean mage = classId == ClassStyleRules.MAGE_CLASS_ID;
		if (upper)
			return mage ? "MANA ASPECT" : "COMBAT DISCIPLINE";
		return mage ? "mana aspect" : "combat discipline";
	}

	/** "Mage style", "Assassin style", and so on. */
	private String styleOwnerLabel() {
		return HunterEvaluationRules.className(classId) + " style";
	}

	private static int stageOf(Phase phase, boolean styled) {
		if (styled) {
			return switch (phase) {
				case BOOT, CONTACT -> 0;
				case SCAN -> 1;
				case CLASS_REVEAL, REROLL -> 2;
				case STYLE_REVEAL, REROLL_STYLE -> 3;
				case RANK_REVEAL, SETTLE -> 4;
				case DECISION, COMPLETE -> 5;
			};
		}
		return switch (phase) {
			case BOOT, CONTACT -> 0;
			case SCAN -> 1;
			case CLASS_REVEAL, REROLL, STYLE_REVEAL, REROLL_STYLE -> 2;
			case RANK_REVEAL, SETTLE -> 3;
			case DECISION, COMPLETE -> 4;
		};
	}

	private String phaseTitle() {
		return switch (phase) {
			case BOOT -> "INITIALIZING EVALUATOR";
			case CONTACT -> holdingContact
					? "MANA CONTACT STABILIZING" : "PLACE YOUR HAND";
			case SCAN -> "READING MANA SIGNATURE";
			case CLASS_REVEAL -> "CLASS RESONANCE DETECTED";
			case STYLE_REVEAL -> styleNoun(true) + " DETECTED";
			case RANK_REVEAL -> isSRankError()
					? "ERROR" : "MEASURING MANA OUTPUT";
			case SETTLE -> "FINALIZING RESULT";
			case DECISION -> mode == Mode.INITIAL
					? "EVALUATION RESULT" : "REEVALUATION RESULT";
			case REROLL -> "RECALIBRATING CLASS";
			case REROLL_STYLE -> "RECALIBRATING " + styleNoun(true);
			case COMPLETE -> "HUNTER RECORD CERTIFIED";
		};
	}

	private String phaseDetail() {
		return switch (phase) {
			case BOOT -> "Association terminal handshake";
			case CONTACT -> "Maintain contact for 1.5 seconds";
			case SCAN -> mode == Mode.INITIAL
					? "Rank cannot be influenced or rerolled"
					: "Comparing earned power with certified rank";
			case CLASS_REVEAL -> "Hue identifies class resonance";
			case STYLE_REVEAL -> styleId > 0
					? ClassStyleRules.styleDescription(classId, styleId)
					: "Resolving the hunter's " + styleNoun(false);
			case RANK_REVEAL -> isSRankError()
					? "Measured output exceeds the certified scale"
					: "Brightness identifies Hunter rank";
			case SETTLE -> "Locking the measured rank";
			case DECISION -> mode == Mode.INITIAL && canRerollStyle
					? "Class and " + styleOwnerLabel()
							+ " rerolls use separate shuffles"
					: mode == Mode.INITIAL && canReroll
							? "Rerolls never repeat the current class"
							: "Confirm the official Association record";
			case REROLL -> "Drawing from the remaining class shuffle";
			case REROLL_STYLE -> "Drawing from the remaining styles";
			case COMPLETE -> "Hunter ID synchronized";
		};
	}

	private float phaseProgress() {
		if (phase == Phase.DECISION || phase == Phase.COMPLETE)
			return 1.0F;
		if (phaseDurationTicks <= 0)
			return 0.0F;
		return clamp01(1.0F - remainingNow() / (float) phaseDurationTicks);
	}

	private int remainingNow() {
		long elapsed = Math.max(0L, Util.getMillis() - stateReceivedAt);
		return Math.max(0, remainingTicks - (int) (elapsed / 50L));
	}

	private float waveStrength() {
		return switch (phase) {
			case BOOT -> 0.30F;
			case CONTACT -> 0.55F + holdCharge * 0.55F;
			case SCAN -> 1.0F;
			case CLASS_REVEAL, STYLE_REVEAL, RANK_REVEAL, REROLL,
					REROLL_STYLE -> 1.25F;
			case SETTLE -> 0.86F;
			case DECISION, COMPLETE -> 0.62F;
		};
	}

	private float scanSweep() {
		return phase == Phase.SCAN ? phaseProgress() : 0.0F;
	}

	/**
	 * True while an S-Rank reveal is still reporting a measurement error. The
	 * evaluator cannot express a result this far outside its range, so it faults
	 * before the real rank resolves.
	 */
	private boolean isSRankError() {
		return phase == Phase.RANK_REVEAL && rank == 6
				&& phaseProgress() < HunterEvaluationRules.sRankErrorFraction();
	}

	/** Reveal progress with the S-Rank error window factored out. */
	private float rankRevealProgress() {
		float progress = phaseProgress();
		if (rank != 6)
			return progress;
		float errorShare = HunterEvaluationRules.sRankErrorFraction();
		return progress <= errorShare ? 0.0F
				: (progress - errorShare) / (1.0F - errorShare);
	}

	/** Bright burst in the instant the fault clears and S resolves. */
	private float sRankResolveFlash() {
		if (rank != 6 || phase != Phase.RANK_REVEAL || isSRankError())
			return 0.0F;
		return clamp01(1.0F - rankRevealProgress() * 5.0F);
	}

	private float revealAmount() {
		if (phase == Phase.CLASS_REVEAL || phase == Phase.STYLE_REVEAL
				|| phase == Phase.RANK_REVEAL || phase == Phase.REROLL
				|| phase == Phase.REROLL_STYLE)
			return phaseProgress();
		return classId > 0 ? 1.0F : 0.0F;
	}

	private int activeColor() {
		if (styleId > 0)
			return ClassStyleRules.styleColor(classId, styleId);
		return HunterEvaluationRules.classColor(classId);
	}

	// ── networking / state ─────────────────────────────────────────────────────

	private void send(Action action) {
		if (actionPending)
			return;
		actionPending = true;
		sendImmediate(action);
	}

	private void sendImmediate(Action action) {
		if (sessionId == null)
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(
				new HunterEvaluationActionMessage(sessionId, action));
	}

	private void updateState(UUID sessionId, int mode, int phase,
			int classId, int rank, int previousRank, int phaseDurationTicks,
			int remainingTicks, boolean canReroll, boolean fixedClass,
			int styleId, boolean canRerollStyle) {
		this.sessionId = sessionId;
		this.mode = Mode.fromId(mode);
		this.phase = Phase.fromId(phase);
		this.classId = classId;
		this.rank = rank;
		this.previousRank = previousRank;
		this.phaseDurationTicks = Math.max(0, phaseDurationTicks);
		this.remainingTicks = Math.max(0, remainingTicks);
		this.canReroll = canReroll;
		this.fixedClass = fixedClass;
		this.styleId = ClassStyleRules.isValidStyle(classId, styleId)
				? styleId : 0;
		this.canRerollStyle = canRerollStyle;
		this.stateReceivedAt = Util.getMillis();
		this.actionPending = false;
		if (this.phase != Phase.CONTACT)
			this.holdingContact = false;
	}

	public static void handleServerState(boolean open, boolean forceOpen,
			UUID sessionId, int mode, int phase, int classId, int rank,
			int previousRank, int phaseDurationTicks, int remainingTicks,
			boolean canReroll, boolean fixedClass, int styleId,
			boolean canRerollStyle) {

		Minecraft minecraft = Minecraft.getInstance();
		if (!open) {
			if (minecraft.screen instanceof HunterEvaluationScreen)
				minecraft.setScreen(null);
			return;
		}
		if (minecraft.screen instanceof HunterEvaluationScreen screen) {
			screen.updateState(sessionId, mode, phase, classId, rank,
					previousRank, phaseDurationTicks, remainingTicks,
					canReroll, fixedClass, styleId, canRerollStyle);
		} else if (forceOpen) {
			minecraft.setScreen(new HunterEvaluationScreen(sessionId, mode,
					phase, classId, rank, previousRank, phaseDurationTicks,
					remainingTicks, canReroll, fixedClass, styleId,
					canRerollStyle));
		}
	}

	private void drawClickWave(GuiGraphics graphics, int centerX, int centerY,
			int color) {
		if (clickWaveAt == 0L)
			return;
		float age = (Util.getMillis() - clickWaveAt) / 700.0F;
		if (age >= 1.0F) {
			clickWaveAt = 0L;
			return;
		}
		int radius = 22 + Math.round(age * 58.0F);
		int alpha = Math.max(0, Math.round((1.0F - age) * 150.0F));
		strokeCircle(graphics, centerX, centerY, radius, 1,
				withAlpha(color, alpha));
	}

	// ── primitives ─────────────────────────────────────────────────────────────

	/** Scanline disc: one fill per row rather than one per pixel. */
	private static void fillCircle(GuiGraphics graphics, int centerX, int centerY,
			int radius, int color) {
		if (radius <= 0)
			return;
		int squared = radius * radius;
		for (int dy = -radius; dy <= radius; dy++) {
			int span = (int) Math.sqrt(Math.max(0, squared - dy * dy));
			if (span <= 0)
				continue;
			graphics.fill(centerX - span, centerY + dy, centerX + span + 1,
					centerY + dy + 1, color);
		}
	}

	/** Scanline ring: two fills per row, so cost scales with radius not area. */
	private static void strokeCircle(GuiGraphics graphics, int centerX,
			int centerY, int radius, int thickness, int color) {
		if (radius <= 0)
			return;
		int inner = Math.max(0, radius - Math.max(1, thickness));
		int outerSq = radius * radius;
		int innerSq = inner * inner;
		for (int dy = -radius; dy <= radius; dy++) {
			int ySq = dy * dy;
			if (ySq > outerSq)
				continue;
			int outerSpan = (int) Math.sqrt(outerSq - ySq);
			int y = centerY + dy;
			if (ySq >= innerSq) {
				graphics.fill(centerX - outerSpan, y, centerX + outerSpan + 1,
						y + 1, color);
			} else {
				int innerSpan = (int) Math.sqrt(innerSq - ySq);
				graphics.fill(centerX - outerSpan, y, centerX - innerSpan, y + 1,
						color);
				graphics.fill(centerX + innerSpan + 1, y, centerX + outerSpan + 1,
						y + 1, color);
			}
		}
	}

	private static void strokeArc(GuiGraphics graphics, int centerX, int centerY,
			int radius, int thickness, float startDegrees, float sweepDegrees,
			int color) {
		if (sweepDegrees <= 0.0F || radius <= 0)
			return;
		int steps = Math.max(1, Math.round(sweepDegrees * radius / 110.0F));
		int half = Math.max(1, thickness) / 2;
		int rest = Math.max(1, thickness) - half;
		for (int step = 0; step <= steps; step++) {
			double angle = Math.toRadians(startDegrees + sweepDegrees * step / steps);
			int x = centerX + (int) Math.round(Math.cos(angle) * radius);
			int y = centerY + (int) Math.round(Math.sin(angle) * radius);
			graphics.fill(x - half, y - half, x + rest, y + rest, color);
		}
	}

	private static void drawLine(GuiGraphics graphics, int x0, int y0, int x1,
			int y1, int color) {
		int dx = x1 - x0;
		int dy = y1 - y0;
		int steps = Math.max(Math.abs(dx), Math.abs(dy));
		if (steps <= 0) {
			graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
			return;
		}
		for (int step = 0; step <= steps; step++) {
			int x = x0 + Math.round(dx * (step / (float) steps));
			int y = y0 + Math.round(dy * (step / (float) steps));
			graphics.fill(x, y, x + 1, y + 1, color);
		}
	}

	private static void drawDiamond(GuiGraphics graphics, int centerX,
			int centerY, int radius, int color) {
		for (int dy = -radius; dy <= radius; dy++) {
			int span = radius - Math.abs(dy);
			if (span < 0)
				continue;
			graphics.fill(centerX - span, centerY + dy, centerX + span + 1,
					centerY + dy + 1, color);
		}
	}

	/** One distinct crest per class, drawn from primitives. */
	private static void drawClassGlyph(GuiGraphics graphics, int centerX,
			int centerY, int classId, int color) {
		switch (classId) {
			case 1 -> { // Assassin: crossed daggers
				drawLine(graphics, centerX - 7, centerY + 7, centerX + 6, centerY - 6, color);
				drawLine(graphics, centerX + 7, centerY + 7, centerX - 6, centerY - 6, color);
				graphics.fill(centerX - 8, centerY + 1, centerX - 2, centerY + 3, color);
				graphics.fill(centerX + 3, centerY + 1, centerX + 9, centerY + 3, color);
			}
			case 2 -> { // Mage: orb with rays
				fillCircle(graphics, centerX, centerY, 4, color);
				for (int ray = 0; ray < 8; ray++) {
					double angle = ray * Math.PI / 4.0;
					int x0 = centerX + (int) Math.round(Math.cos(angle) * 6);
					int y0 = centerY + (int) Math.round(Math.sin(angle) * 6);
					int x1 = centerX + (int) Math.round(Math.cos(angle) * 9);
					int y1 = centerY + (int) Math.round(Math.sin(angle) * 9);
					drawLine(graphics, x0, y0, x1, y1, color);
				}
			}
			case 3 -> { // Fighter: sword
				graphics.fill(centerX - 1, centerY - 9, centerX + 2, centerY + 5, color);
				graphics.fill(centerX - 6, centerY + 1, centerX + 7, centerY + 3, color);
				graphics.fill(centerX - 2, centerY + 5, centerX + 3, centerY + 9, color);
			}
			case 4 -> { // Tanker: shield
				for (int dy = -8; dy <= 8; dy++) {
					float t = (dy + 8) / 16.0F;
					int span = t < 0.55F ? 7 : Math.round(7 * (1.0F - t) / 0.45F);
					if (span <= 0)
						continue;
					boolean edge = dy == -8 || span <= 1;
					graphics.fill(centerX - span, centerY + dy, centerX + span + 1,
							centerY + dy + 1, edge ? color : withAlpha(color, 0x59));
				}
				graphics.fill(centerX - 7, centerY - 8, centerX + 8, centerY - 6, color);
				graphics.fill(centerX - 1, centerY - 6, centerX + 2, centerY + 6, color);
			}
			case 5 -> { // Healer: cross
				graphics.fill(centerX - 2, centerY - 9, centerX + 3, centerY + 10, color);
				graphics.fill(centerX - 8, centerY - 3, centerX + 9, centerY + 4, color);
			}
			default -> { // Ranger: bow and arrow
				for (int dy = -8; dy <= 8; dy++) {
					int span = 6 - (dy * dy) / 12;
					graphics.fill(centerX - 2 + span, centerY + dy,
							centerX - 1 + span, centerY + dy + 1, color);
				}
				drawLine(graphics, centerX - 3, centerY - 8, centerX - 3, centerY + 8,
						withAlpha(color, 0x8C));
				graphics.fill(centerX - 8, centerY - 1, centerX + 5, centerY + 1, color);
				drawLine(graphics, centerX + 5, centerY, centerX + 1, centerY - 4, color);
				drawLine(graphics, centerX + 5, centerY, centerX + 1, centerY + 4, color);
			}
		}
	}

	/**
	 * Chromatic-split text used for the fault report: a red and a cyan ghost
	 * jitter either side of the white body, resampled a few times a second.
	 */
	private void drawGlitchText(GuiGraphics graphics, String text, int centerX,
			int y, int seed) {
		int slot = (int) (Util.getMillis() / 60L);
		int offset = Math.round(hash01(seed, slot) * 5.0F) - 2;
		int width = trackedWidth(text, 1);
		int x = centerX - width / 2;
		drawTracked(graphics, text, x - 2 + offset, y, 0xFFFF2A4B, 1);
		drawTracked(graphics, text, x + 2 - offset, y, 0xFF2AF0FF, 1);
		drawTracked(graphics, text, x, y, 0xFFFFFFFF, 1);
	}

	/** Horizontal tear bars that sweep the chamber while the reading faults. */
	private void drawGlitchTears(GuiGraphics graphics, int x, int y, int width,
			int height) {
		int slot = (int) (Util.getMillis() / 90L);
		for (int tear = 0; tear < 4; tear++) {
			float seed = hash01(tear * 31 + slot, slot);
			int tearY = y + (int) (seed * (height - 4));
			int tearHeight = 1 + (int) (hash01(slot, tear) * 3.0F);
			int shift = Math.round(hash01(tear, slot * 7) * 12.0F) - 6;
			graphics.fill(x + Math.max(0, shift), tearY,
					x + width + Math.min(0, shift), tearY + tearHeight,
					tear % 2 == 0 ? 0x59FF2A4B : 0x4D2AF0FF);
		}
	}

	private static float hash01(int x, int y) {
		int hash = x * 374761393 + y * 668265263;
		hash = (hash ^ (hash >>> 13)) * 1274126177;
		return ((hash ^ (hash >>> 16)) & 0xFFFF) / 65535.0F;
	}

	private int drawTracked(GuiGraphics graphics, String text, int x, int y,
			int color, int tracking) {
		int cursor = x;
		for (int index = 0; index < text.length(); index++) {
			String glyph = String.valueOf(text.charAt(index));
			graphics.drawString(this.font, glyph, cursor, y, color, false);
			cursor += this.font.width(glyph) + tracking;
		}
		return cursor - tracking;
	}

	private int trackedWidth(String text, int tracking) {
		if (text.isEmpty())
			return 0;
		int width = -tracking;
		for (int index = 0; index < text.length(); index++)
			width += this.font.width(String.valueOf(text.charAt(index))) + tracking;
		return width;
	}

	private void outline(GuiGraphics graphics, int x, int y, int width,
			int height, int color) {
		graphics.fill(x, y, x + width, y + 1, color);
		graphics.fill(x, y + height - 1, x + width, y + height, color);
		graphics.fill(x, y, x + 1, y + height, color);
		graphics.fill(x + width - 1, y, x + width, y + height, color);
	}

	private void drawCorners(GuiGraphics graphics, int color) {
		int length = 13;
		graphics.fill(panelX - 1, panelY - 1, panelX + length, panelY + 1, color);
		graphics.fill(panelX - 1, panelY - 1, panelX + 1, panelY + length, color);
		graphics.fill(panelX + panelW - length, panelY - 1,
				panelX + panelW + 1, panelY + 1, color);
		graphics.fill(panelX + panelW - 1, panelY - 1,
				panelX + panelW + 1, panelY + length, color);
		graphics.fill(panelX - 1, panelY + panelH - 1,
				panelX + length, panelY + panelH + 1, color);
		graphics.fill(panelX - 1, panelY + panelH - length,
				panelX + 1, panelY + panelH + 1, color);
		graphics.fill(panelX + panelW - length, panelY + panelH - 1,
				panelX + panelW + 1, panelY + panelH + 1, color);
		graphics.fill(panelX + panelW - 1, panelY + panelH - length,
				panelX + panelW + 1, panelY + panelH + 1, color);
	}

	private static float pulse01() {
		return 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 170.0D);
	}

	private static int withAlpha(int color, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
	}

	private static int mix(int from, int to, float amount) {
		float bounded = clamp01(amount);
		int red = Math.round(((from >> 16) & 0xFF) * (1.0F - bounded)
				+ ((to >> 16) & 0xFF) * bounded);
		int green = Math.round(((from >> 8) & 0xFF) * (1.0F - bounded)
				+ ((to >> 8) & 0xFF) * bounded);
		int blue = Math.round((from & 0xFF) * (1.0F - bounded)
				+ (to & 0xFF) * bounded);
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}

	private static float frac(float value) {
		return value - (float) Math.floor(value);
	}

	private static float clamp01(float value) {
		return value < 0.0F ? 0.0F : Math.min(1.0F, value);
	}
}
