package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.RulersAuthorityAuraRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.RulersAuthorityAuraEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class RulersAuthorityAuraRenderer extends EntityRenderer<RulersAuthorityAuraEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling:textures/particle/mana_blue.png");
    private static final int SEGMENTS = 24;

    public RulersAuthorityAuraRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(RulersAuthorityAuraEntity aura, float entityYaw, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight) {
        float age = aura.tickCount + partialTick;
        float fadeIn = smoothstep(0.0F, 5.0F, age);
        if (fadeIn <= 0.0F)
            return;

        float width = Math.max(0.5F, aura.getTargetWidth());
        float height = Math.max(0.65F, aura.getTargetHeight());
        float radius = width * (aura.isAuthority() ? 0.74F : 0.66F) + 0.18F;
        float intensity = aura.isResisted() ? 0.82F : 1.0F;
        RenderType renderType = RulersAuthorityAuraRenderTypes.aura(TEXTURE);
        VertexConsumer vertices = DeferredWorldShaderRenderer.buffer(bufferSource, renderType);

        poseStack.pushPose();
        Entity target = aura.getTarget();
        if (target != null) {
            Vec3 targetPosition = target.getPosition(partialTick);
            Vec3 auraPosition = aura.getPosition(partialTick);
            poseStack.translate(targetPosition.x - auraPosition.x,
                    targetPosition.y - auraPosition.y,
                    targetPosition.z - auraPosition.z);
        }
        poseStack.translate(0.0D, -0.12D, 0.0D);

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 1.55F));
        drawCylinder(vertices, poseStack.last(), radius * 1.12F, height + 0.46F,
                aura.isAuthority() ? 54 : 38, fadeIn * intensity, 0.0F);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-age * 2.25F + 47.0F));
        drawCylinder(vertices, poseStack.last(), radius * 0.86F, height + 0.28F,
                aura.isAuthority() ? 78 : 58, fadeIn * intensity, 0.37F);
        poseStack.popPose();

        int ribbonCount = aura.isAuthority() ? 4 : 3;
        for (int ribbon = 0; ribbon < ribbonCount; ribbon++) {
            float direction = ribbon % 2 == 0 ? 1.0F : -1.0F;
            float phase = ribbon * (360.0F / ribbonCount) + age * direction * (3.4F + ribbon * 0.45F);
            float ribbonRadius = radius * (0.94F + (ribbon % 2) * 0.13F);
            float ribbonAlpha = (aura.isAuthority() ? 104.0F : 78.0F) * fadeIn * intensity;
            drawSpiral(vertices, poseStack.last(), ribbonRadius, height + 0.38F, phase,
                    direction, Math.round(ribbonAlpha), ribbon * 0.19F);
        }

        drawGroundRing(vertices, poseStack.last(), radius * 1.22F, 0.035F,
                Math.round((aura.isAuthority() ? 94.0F : 66.0F) * fadeIn * intensity), age);
        poseStack.popPose();
        super.render(aura, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void drawCylinder(VertexConsumer vertices, PoseStack.Pose pose, float radius, float height,
            int alpha, float fade, float uvOffset) {
        int finalAlpha = Math.round(alpha * fade);
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float u0 = segment / (float) SEGMENTS;
            float u1 = (segment + 1) / (float) SEGMENTS;
            double a0 = Math.PI * 2.0D * u0;
            double a1 = Math.PI * 2.0D * u1;
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            vertex(vertices, pose, x0, 0.0F, z0, u0, 1.0F, finalAlpha);
            vertex(vertices, pose, x1, 0.0F, z1, u1, 1.0F, finalAlpha);
            vertex(vertices, pose, x1, height, z1, u1, 0.0F, finalAlpha);
            vertex(vertices, pose, x0, height, z0, u0, 0.0F, finalAlpha);
        }
    }

    private static void drawSpiral(VertexConsumer vertices, PoseStack.Pose pose, float radius, float height,
            float phaseDegrees, float direction, int alpha, float uvOffset) {
        float thickness = Math.max(0.09F, height * 0.065F);
        float turns = 1.35F;
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float t0 = segment / (float) SEGMENTS;
            float t1 = (segment + 1) / (float) SEGMENTS;
            double a0 = Math.toRadians(phaseDegrees) + direction * Math.PI * 2.0D * turns * t0;
            double a1 = Math.toRadians(phaseDegrees) + direction * Math.PI * 2.0D * turns * t1;
            float y0 = t0 * height;
            float y1 = t1 * height;
            float x0 = (float) Math.cos(a0) * radius;
            float z0 = (float) Math.sin(a0) * radius;
            float x1 = (float) Math.cos(a1) * radius;
            float z1 = (float) Math.sin(a1) * radius;
            vertex(vertices, pose, x0, y0 - thickness, z0, t0, 1.0F, alpha);
            vertex(vertices, pose, x1, y1 - thickness, z1, t1, 1.0F, alpha);
            vertex(vertices, pose, x1, y1 + thickness, z1, t1, 0.0F, alpha);
            vertex(vertices, pose, x0, y0 + thickness, z0, t0, 0.0F, alpha);
        }
    }

    private static void drawGroundRing(VertexConsumer vertices, PoseStack.Pose pose, float radius,
            float thickness, int alpha, float age) {
        float pulse = 1.0F + (float) Math.sin(age * 0.18F) * 0.045F;
        float outer = radius * pulse;
        float inner = Math.max(0.05F, outer - Math.max(thickness, radius * 0.08F));
        for (int segment = 0; segment < SEGMENTS; segment++) {
            float u0 = segment / (float) SEGMENTS;
            float u1 = (segment + 1) / (float) SEGMENTS;
            double a0 = Math.PI * 2.0D * u0;
            double a1 = Math.PI * 2.0D * u1;
            vertex(vertices, pose, (float) Math.cos(a0) * inner, 0.03F, (float) Math.sin(a0) * inner, u0, 1.0F, alpha);
            vertex(vertices, pose, (float) Math.cos(a1) * inner, 0.03F, (float) Math.sin(a1) * inner, u1, 1.0F, alpha);
            vertex(vertices, pose, (float) Math.cos(a1) * outer, 0.03F, (float) Math.sin(a1) * outer, u1, 0.0F, alpha);
            vertex(vertices, pose, (float) Math.cos(a0) * outer, 0.03F, (float) Math.sin(a0) * outer, u0, 0.0F, alpha);
        }
    }

    private static void vertex(VertexConsumer vertices, PoseStack.Pose pose, float x, float y, float z,
            float u, float v, int alpha) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();
        vertices.vertex(matrix, x, y, z).color(120, 210, 255, alpha).uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240)
                .normal(normal, x, 0.15F, z).endVertex();
    }

    private static float smoothstep(float edge0, float edge1, float value) {
        float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
        return t * t * (3.0F - 2.0F * t);
    }

    @Override
    public ResourceLocation getTextureLocation(RulersAuthorityAuraEntity entity) {
        return TEXTURE;
    }
}
