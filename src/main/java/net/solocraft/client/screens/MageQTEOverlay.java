package net.solocraft.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.MageQTEHelper;
import net.solocraft.util.MageQTEState;
import net.solocraft.util.QTEResult;

import org.joml.Matrix4f;

/**
 * Renders the mage QTE ring overlay and result flash text.
 * Adapted from BlackFlashQTERendererProcedure (1.21.1 reference project).
 *
 * Layout (centred on screen):
 *   ┌─ background circle (grey/white gradient) ─────────────┐
 *   │  ┌─ ring ────────────────────────────────────────────┐ │
 *   │  │  gold arc  = GOOD zone (40°)                      │ │
 *   │  │  cyan arc  = PERFECT zone (14°, inside gold)      │ │
 *   │  │  blue tick = rotating needle                       │ │
 *   │  └───────────────────────────────────────────────────┘ │
 *   │  [Spell name centred inside]                           │
 *   └────────────────────────────────────────────────────────┘
 *   [Release in the gold zone!]      ← hint below ring
 *   After release: [PERFECT!] / [GOOD!] / [MISS!]
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
@OnlyIn(Dist.CLIENT)
public class MageQTEOverlay {

    private static final float RADIUS         = 60f;
    private static final float RING_THICKNESS = 5f;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        MageQTEState state = MageQTEState.INSTANCE;

        // ── Timeout: cancel cleanly on client ────────────────────────────────
        if (state.isActive() && state.hasTimedOut()) {
            state.cancelQTE();
            state.showResult(QTEResult.MISS);
        }

        boolean showRing   = state.isActive();
        boolean showResult = state.isShowingResult();
        if (!showRing && !showResult) return;

        Minecraft mc        = Minecraft.getInstance();
        GuiGraphics gg      = event.getGuiGraphics();
        int w               = mc.getWindow().getGuiScaledWidth();
        int h               = mc.getWindow().getGuiScaledHeight();
        float cx            = w / 2f;
        float cy            = h / 2f;
        PoseStack ps        = gg.pose();

        ps.pushPose();
        ps.translate(0, 0, 300); // above all other overlays

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        if (showRing) {
            // 1. Dark grey gradient fill inside the ring
            renderFilledCircleGradient(ps, cx, cy, RADIUS - RING_THICKNESS,
                    0.18f, 0.18f, 0.22f, 0.72f,  // centre: dark grey, semi-opaque
                    0.10f, 0.10f, 0.12f, 0.50f); // edge: darker, fading

            // 2. Outer ring black outline — thick border
            renderRing(ps, cx, cy, RADIUS, RING_THICKNESS + 6f,
                    0.08f, 0.08f, 0.08f, 1.00f);

            // 3. Grey ring (on top of black outline)
            renderRing(ps, cx, cy, RADIUS, RING_THICKNESS,
                    0.58f, 0.58f, 0.63f, 0.95f);

            // 4. GOOD zone — gold arc (black outline + gold fill)
            //    Bumped +4 from baseline so they clear the thicker ring outline
            float goodStart = state.getGoodZoneStart();
            float goodEnd   = state.getGoodZoneEnd();
            renderArc(ps, cx, cy, RADIUS, goodStart, goodEnd,
                    RING_THICKNESS + 8f, 0f, 0f, 0f, 0.95f);
            renderArc(ps, cx, cy, RADIUS, goodStart, goodEnd,
                    RING_THICKNESS + 6f, 1.0f, 0.75f, 0.08f, 0.97f);

            // 5. PERFECT zone — cyan arc (black outline + cyan fill), centred in GOOD
            float perfectStart = state.getPerfectZoneStart();
            float perfectEnd   = state.getPerfectZoneEnd();
            renderArc(ps, cx, cy, RADIUS, perfectStart, perfectEnd,
                    RING_THICKNESS + 10f, 0f, 0f, 0f, 0.95f);
            renderArc(ps, cx, cy, RADIUS, perfectStart, perfectEnd,
                    RING_THICKNESS + 8f, 0.20f, 0.85f, 1.0f, 0.97f);

            // 6. Rotating needle (black outline + blue fill)
            float rot = state.getCurrentRotation();
            renderPerimeterTick(ps, cx, cy, RADIUS, rot,
                    18f, 3.8f, 7f, 0f, 0f, 0f, 1.00f);
            renderPerimeterTick(ps, cx, cy, RADIUS, rot,
                    18f, 2.4f, 7f, 0.10f, 0.40f, 1.0f, 1.0f);

            // 7. Spell name centred inside the ring
            String skillName = "";
            if (mc.player != null) {
                var cap = mc.player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
                        .orElse(new SololevelingModVariables.PlayerVariables());
                skillName = cap.PselectedPower;
            }
            if (!skillName.isEmpty()) {
                int nameW = mc.font.width(skillName);
                gg.drawString(mc.font, Component.literal("§f" + skillName),
                        (int)(cx - nameW / 2f), (int)(cy - 5), 0xFFFFFFFF, true);
            }

            // 8. Hint text below ring
            String hint = "Release in the §egold§r zone!";
            int hintW = mc.font.width(hint);
            gg.drawString(mc.font, Component.literal(hint),
                    (int)(cx - hintW / 2f), (int)(cy + RADIUS + 10), 0xFFAAAAAA, false);
            renderTimingBar(gg, mc, state, cx, cy + RADIUS + 28);
        }

        // 9. Result flash text (shown after key release, fades out)
        if (showResult) {
            QTEResult result = state.getLastResult();
            float alpha      = state.getResultAlpha();
            int   alphaInt   = (int)(alpha * 255) << 24;

            String label = switch (result) {
                case PERFECT -> "§bPERFECT!";
                case GOOD    -> "§eGOOD!";
                case MISS    -> "§cMISS!";
            };
            int labelW = mc.font.width(label);
            gg.drawString(mc.font, Component.literal(label),
                    (int)(cx - labelW / 2f), (int)(cy - RADIUS - 24), 0xFFFFFF | alphaInt, true);
        }

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        ps.popPose();
    }

    private static void renderTimingBar(GuiGraphics gg, Minecraft mc, MageQTEState state, float cx, float y) {
        int width = 164;
        int height = 9;
        int x = (int) (cx - width / 2f);
        int top = (int) y;
        gg.fill(x - 2, top - 2, x + width + 2, top + height + 2, 0xD0000000);
        gg.fill(x, top, x + width, top + height, 0xDD101522);
        drawZoneOnBar(gg, x, top, width, height, state.getGoodZoneStart(), state.getGoodZoneEnd(), 0xFFFFC94A);
        drawZoneOnBar(gg, x, top - 1, width, height + 2, state.getPerfectZoneStart(), state.getPerfectZoneEnd(), 0xFF46E7FF);

        int marker = x + Math.round((state.getCurrentRotation() / 360f) * width);
        gg.fill(marker - 1, top - 5, marker + 2, top + height + 5, 0xFFFFFFFF);
        gg.fill(marker, top - 4, marker + 1, top + height + 4, 0xFF74A8FF);

        gg.drawString(mc.font, Component.literal("RELEASE"), x + width + 8, top, 0xFFE8F7FF, true);
    }

    private static void drawZoneOnBar(GuiGraphics gg, int x, int y, int width, int height, float startDeg, float endDeg, int color) {
        if (endDeg < startDeg) {
            drawZoneOnBar(gg, x, y, width, height, startDeg, 360f, color);
            drawZoneOnBar(gg, x, y, width, height, 0f, endDeg, color);
            return;
        }
        int start = x + Math.round((startDeg / 360f) * width);
        int end = x + Math.round((endDeg / 360f) * width);
        gg.fill(start, y, Math.max(start + 1, end), y + height, color);
    }

    // ── Geometry helpers (adapted verbatim from BlackFlashQTERendererProcedure) ──

    private static void renderFilledCircleGradient(PoseStack ps,
            float cx, float cy, float radius,
            float cr, float cg, float cb, float ca,
            float er, float eg, float eb, float ea) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m   = ps.last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(m, cx, cy, 0).color(cr, cg, cb, ca).endVertex();
        int seg = 72;
        for (int i = 0; i <= seg; i++) {
            float ang = (float)(2 * Math.PI * i / seg);
            buf.vertex(m, cx + (float)Math.cos(ang) * radius,
                          cy + (float)Math.sin(ang) * radius, 0)
               .color(er, eg, eb, ea).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void renderRing(PoseStack ps,
            float cx, float cy, float radius, float thickness,
            float r, float g, float b, float a) {
        float outerR = radius + thickness * 0.5f;
        float innerR = radius - thickness * 0.5f;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m   = ps.last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int seg = 96;
        for (int i = 0; i <= seg; i++) {
            float ang = (float)(2 * Math.PI * i / seg);
            float cos = (float)Math.cos(ang), sin = (float)Math.sin(ang);
            buf.vertex(m, cx + cos * outerR, cy + sin * outerR, 0).color(r, g, b, a).endVertex();
            buf.vertex(m, cx + cos * innerR, cy + sin * innerR, 0).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    private static void renderArc(PoseStack ps,
            float cx, float cy, float radius,
            float startDeg, float endDeg, float thickness,
            float r, float g, float b, float a) {
        float outerR    = radius + thickness * 0.5f;
        float innerR    = radius - thickness * 0.5f;
        float startRad  = (float)Math.toRadians(startDeg - 90);
        float endRad    = (float)Math.toRadians(endDeg   - 90);
        if (endRad < startRad) endRad += 2 * Math.PI;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Matrix4f m   = ps.last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);
        int seg = 72;
        for (int i = 0; i <= seg; i++) {
            float ang = startRad + (endRad - startRad) * ((float)i / seg);
            float cos = (float)Math.cos(ang), sin = (float)Math.sin(ang);
            buf.vertex(m, cx + cos * outerR, cy + sin * outerR, 0).color(r, g, b, a).endVertex();
            buf.vertex(m, cx + cos * innerR, cy + sin * innerR, 0).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(buf.end());
    }

    /** A short radial quad (the rotating needle) that extends outward from the ring. */
    private static void renderPerimeterTick(PoseStack ps,
            float cx, float cy, float radius,
            float angleDeg, float tickLen, float halfWidth, float outwardOffset,
            float r, float g, float b, float a) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        float ang  = (float)Math.toRadians(angleDeg - 90);
        float cos  = (float)Math.cos(ang), sin = (float)Math.sin(ang);
        float ox   = cx + cos * (radius + outwardOffset);
        float oy   = cy + sin * (radius + outwardOffset);
        float ix   = cx + cos * (radius + outwardOffset - tickLen);
        float iy   = cy + sin * (radius + outwardOffset - tickLen);
        float px   = -sin * halfWidth, py = cos * halfWidth;
        Matrix4f m = ps.last().pose();
        BufferBuilder buf = Tesselator.getInstance().getBuilder();
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buf.vertex(m, ix - px, iy - py, 0).color(r, g, b, a).endVertex();
        buf.vertex(m, ix + px, iy + py, 0).color(r, g, b, a).endVertex();
        buf.vertex(m, ox + px, oy + py, 0).color(r, g, b, a).endVertex();
        buf.vertex(m, ox - px, oy - py, 0).color(r, g, b, a).endVertex();
        BufferUploader.drawWithShader(buf.end());
    }
}
