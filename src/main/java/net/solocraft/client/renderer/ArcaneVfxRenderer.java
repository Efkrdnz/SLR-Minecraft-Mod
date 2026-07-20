package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.ArcaneVfxRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.ArcaneVfxEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

public class ArcaneVfxRenderer extends EntityRenderer<ArcaneVfxEntity> {
	private static final ResourceLocation FALLBACK = new ResourceLocation("sololeveling",
			"textures/particle/glow_yellow.png");

	public ArcaneVfxRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(ArcaneVfxEntity entity, float entityYaw, float partialTick, PoseStack stack,
			MultiBufferSource buffers, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.002F || hideOwnerFirstPerson(entity))
			return;
		VertexConsumer out = DeferredWorldShaderRenderer.buffer(buffers,
				ArcaneVfxRenderTypes.effect(FALLBACK), true);
		stack.pushPose();
		switch (entity.getStyle()) {
			case ArcaneVfxEntity.AETHER_BOLT -> renderBolt(entity, partialTick, stack, out, fade, false);
			case ArcaneVfxEntity.AETHER_IMPACT -> renderImpact(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.VECTOR_TRAIL -> renderVector(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.VECTOR_ANCHOR -> renderAnchor(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.POLARITY_SPHERE -> renderPolarity(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.RUNIC_RELAY -> renderRelay(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.RELAY_BEAM -> renderRelayBeam(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.ASTRAL_ARSENAL -> renderArsenal(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.ASTRAL_BLADE -> renderBolt(entity, partialTick, stack, out, fade, true);
			case ArcaneVfxEntity.DIMENSIONAL_REND -> renderRend(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.SPATIAL_SCAR -> renderScar(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.CONVERGENCE_GROUND -> renderConvergence(entity, partialTick, stack, out, fade, false);
			case ArcaneVfxEntity.CONVERGENCE_SKY -> renderConvergence(entity, partialTick, stack, out, fade, true);
			case ArcaneVfxEntity.CONVERGENCE_TETHER -> renderTether(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.ZERO_POINT -> renderZeroPoint(entity, partialTick, stack, out, fade);
			case ArcaneVfxEntity.FORMULA_RUNES -> renderFormula(entity, partialTick, stack, out, fade);
			default -> renderImpact(entity, partialTick, stack, out, fade);
		}
		stack.popPose();
		super.render(entity, entityYaw, partialTick, stack, buffers, packedLight);
	}

	private boolean hideOwnerFirstPerson(ArcaneVfxEntity entity) {
		if (entity.getStyle() != ArcaneVfxEntity.FORMULA_RUNES
				&& entity.getStyle() != ArcaneVfxEntity.ASTRAL_ARSENAL)
			return false;
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.options.getCameraType().isFirstPerson()
				&& minecraft.getCameraEntity() != null
				&& entity.getOwnerId().filter(minecraft.getCameraEntity().getUUID()::equals).isPresent();
	}

	private void renderBolt(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade, boolean astral) {
		orientForward(entity, stack);
		float time = entity.tickCount + partialTick;
		float pulse = 0.94F + Mth.sin(time * 0.75F + entity.getSeed()) * 0.06F;
		float width = entity.getScale() * pulse;
		float length = entity.getLength();
		float kind = astral ? 4.0F : 0.0F;
		drawBlade(out, stack.last(), width * (astral ? 1.2F : 1.65F), length * 1.08F,
				kind, entity.getPrimaryColor(), alpha(78 * fade), entity.getStage(), 0.0F);
		drawBlade(out, stack.last(), width * 0.62F, length,
				kind, lighten(entity.getSecondaryColor(), 0.65F), alpha(242 * fade),
				entity.getStage(), 0.012F);
		stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
		drawBlade(out, stack.last(), width * 0.34F, length * 0.88F,
				kind, entity.getSecondaryColor(), alpha(165 * fade), entity.getStage(), 0.02F);
	}

	private void renderImpact(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float progress = entity.getProgress(partialTick);
		float size = entity.getScale() * (0.3F + progress * 1.15F);
		stack.mulPose(entityRenderDispatcher.cameraOrientation());
		stack.mulPose(Axis.ZP.rotationDegrees((entity.tickCount + partialTick) * 2.8F));
		drawBillboard(out, stack.last(), size, 7.0F, entity.getPrimaryColor(),
				alpha(175 * fade), entity.getStage());
		stack.mulPose(Axis.ZP.rotationDegrees(-51.0F));
		drawBillboard(out, stack.last(), size * 0.62F, 1.0F,
				lighten(entity.getSecondaryColor(), 0.65F), alpha(240 * fade), entity.getStage());
	}

	private void renderVector(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		float width = entity.getScale();
		float length = entity.getLength();
		for (int i = 0; i < 3; i++) {
			stack.pushPose();
			stack.mulPose(Axis.ZP.rotationDegrees(i * 120.0F + (entity.tickCount + partialTick) * 1.6F));
			drawRibbon(out, stack.last(), width * (1.0F - i * 0.13F), length,
					3.0F, i == 0 ? entity.getSecondaryColor() : entity.getPrimaryColor(),
					alpha((190 - i * 38) * fade), entity.getStage());
			stack.popPose();
		}
	}

	private void renderAnchor(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float time = entity.tickCount + partialTick;
		float radius = entity.getScale() * (0.94F + Mth.sin(time * 0.18F) * 0.06F);
		stack.mulPose(Axis.YP.rotationDegrees(time * 1.2F));
		drawRing(out, stack.last(), radius, radius * 0.72F, 1.0F,
				entity.getPrimaryColor(), alpha(205 * fade), entity.getStage(), 28);
		drawRadialRunes(out, stack.last(), radius * 0.78F, 6, 1.0F,
				entity.getSecondaryColor(), alpha(235 * fade), entity.getStage());
	}

	private void renderPolarity(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float time = entity.tickCount + partialTick;
		float radius = entity.getScale();
		for (int i = 0; i < 3; i++) {
			stack.pushPose();
			stack.mulPose(Axis.YP.rotationDegrees(time * (0.7F + i * 0.18F) + i * 60.0F));
			stack.mulPose(Axis.XP.rotationDegrees(i * 60.0F));
			drawRing(out, stack.last(), radius * (0.46F + i * 0.12F),
					radius * (0.40F + i * 0.10F), 2.0F,
					i == 1 ? entity.getSecondaryColor() : entity.getPrimaryColor(),
					alpha((180 - i * 28) * fade), entity.getStage(), 32);
			stack.popPose();
		}
		stack.pushPose();
		stack.mulPose(entityRenderDispatcher.cameraOrientation());
		drawBillboard(out, stack.last(), radius * 0.72F, 2.0F,
				entity.getPrimaryColor(), alpha(96 * fade), entity.getStage());
		drawBillboard(out, stack.last(), radius * 0.28F, 7.0F,
				lighten(entity.getSecondaryColor(), 0.82F), alpha(230 * fade), entity.getStage());
		stack.popPose();
	}

	private void renderRelay(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float time = entity.tickCount + partialTick;
		float radius = entity.getScale();
		orientForward(entity, stack);
		stack.mulPose(Axis.ZP.rotationDegrees(time * 1.1F));
		drawBillboard(out, stack.last(), radius, 3.0F, entity.getPrimaryColor(),
				alpha(98 * fade), entity.getStage());
		drawDiamond(out, stack.last(), radius * 0.58F, 3.0F,
				entity.getSecondaryColor(), alpha(235 * fade), entity.getStage());
		stack.mulPose(Axis.ZP.rotationDegrees(-time * 2.0F));
		drawRadialRunesBillboard(out, stack.last(), radius * 0.82F, 8, 3.0F,
				entity.getSecondaryColor(), alpha(215 * fade), entity.getStage());
	}

	private void renderRelayBeam(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		float pulse = 0.85F + Mth.sin((entity.tickCount + partialTick) * 0.55F) * 0.15F;
		for (int i = 0; i < 2; i++) {
			stack.pushPose();
			stack.mulPose(Axis.ZP.rotationDegrees(i * 90.0F));
			drawRibbon(out, stack.last(), entity.getScale() * (i == 0 ? 1.0F : 0.52F),
					entity.getLength(), 3.0F,
					i == 0 ? entity.getPrimaryColor() : entity.getSecondaryColor(),
					alpha((i == 0 ? 72 : 135) * fade * pulse), entity.getStage());
			stack.popPose();
		}
	}

	private void renderArsenal(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		int blades = Mth.clamp(Math.round(entity.getScale()), 1, 11);
		float time = entity.tickCount + partialTick;
		float radius = 1.15F + entity.getStage() * 0.15F;
		for (int i = 0; i < blades; i++) {
			double angle = Math.PI * 2.0D * i / blades + time * 0.035D;
			stack.pushPose();
			stack.translate(Math.cos(angle) * radius,
					0.18D + Math.sin(angle * 2.0D + time * 0.08D) * 0.28D,
					Math.sin(angle) * radius);
			stack.mulPose(Axis.YP.rotationDegrees((float) (-angle * Mth.RAD_TO_DEG) + 90.0F));
			stack.mulPose(Axis.XP.rotationDegrees(-16.0F));
			drawBlade(out, stack.last(), 0.16F + entity.getStage() * 0.025F,
					0.85F + entity.getStage() * 0.09F, 4.0F,
					i % 2 == 0 ? entity.getSecondaryColor() : entity.getPrimaryColor(),
					alpha(225 * fade), entity.getStage(), 0.01F);
			stack.popPose();
		}
	}

	private void renderRend(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		float width = entity.getScale();
		float length = entity.getLength();
		drawBlade(out, stack.last(), width, length, 5.0F, entity.getPrimaryColor(),
				alpha(105 * fade), entity.getStage(), 0.0F);
		drawBlade(out, stack.last(), width * 0.42F, length * 1.05F, 5.0F,
				lighten(entity.getSecondaryColor(), 0.8F), alpha(244 * fade),
				entity.getStage(), 0.018F);
		if (entity.getStage() >= 3 || entity.isOvercast()) {
			stack.mulPose(Axis.ZP.rotationDegrees(entity.isOvercast() ? 31.0F : 17.0F));
			drawBlade(out, stack.last(), width * 0.30F, length * 0.88F, 5.0F,
					entity.getSecondaryColor(), alpha(175 * fade), entity.getStage(), 0.025F);
		}
	}

	private void renderScar(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		float time = entity.tickCount + partialTick;
		float width = entity.getScale() * (1.0F - entity.getProgress(partialTick) * 0.28F);
		stack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 0.12F) * 3.5F));
		drawBlade(out, stack.last(), width, entity.getLength(), 5.0F,
				entity.getPrimaryColor(), alpha(130 * fade), entity.getStage(), 0.0F);
		drawBlade(out, stack.last(), width * 0.28F, entity.getLength() * 1.06F, 5.0F,
				lighten(entity.getSecondaryColor(), 0.75F), alpha(238 * fade),
				entity.getStage(), 0.02F);
	}

	private void renderConvergence(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade, boolean sky) {
		float time = entity.tickCount + partialTick;
		float radius = entity.getScale();
		if (sky)
			stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
		stack.mulPose(Axis.YP.rotationDegrees((sky ? -1.0F : 1.0F) * time * 0.32F));
		drawRing(out, stack.last(), radius, radius * 0.76F, 6.0F,
				entity.getPrimaryColor(), alpha(190 * fade), entity.getStage(), 52);
		drawRing(out, stack.last(), radius * 0.62F, radius * 0.38F, 6.0F,
				entity.getSecondaryColor(), alpha(224 * fade), entity.getStage(), 36);
		drawRadialRunes(out, stack.last(), radius * 0.82F,
				entity.getStage() >= 5 ? 12 : 6 + entity.getStage(), 6.0F,
				entity.getSecondaryColor(), alpha(235 * fade), entity.getStage());
		if (entity.getStage() >= 4 || entity.isOvercast())
			drawStar(out, stack.last(), radius * 0.72F, 6.0F,
					lighten(entity.getSecondaryColor(), 0.5F), alpha(155 * fade), entity.getStage());
	}

	private void renderTether(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float time = entity.tickCount + partialTick;
		int strands = entity.getStage() >= 5 ? 6 : 4;
		for (int i = 0; i < strands; i++) {
			double angle = Math.PI * 2.0D * i / strands + time * 0.025D;
			stack.pushPose();
			stack.translate(Math.cos(angle) * entity.getScale(), 0.0D,
					Math.sin(angle) * entity.getScale());
			stack.mulPose(Axis.XP.rotationDegrees(90.0F));
			drawRibbon(out, stack.last(), 0.09F + entity.getStage() * 0.012F,
					entity.getLength(), 6.0F,
					i % 2 == 0 ? entity.getPrimaryColor() : entity.getSecondaryColor(),
					alpha(120 * fade), entity.getStage());
			stack.popPose();
		}
	}

	private void renderZeroPoint(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float progress = entity.getProgress(partialTick);
		float radius = entity.getScale() * (0.18F + progress * 0.94F);
		stack.mulPose(entityRenderDispatcher.cameraOrientation());
		drawBillboard(out, stack.last(), radius, 7.0F, entity.getPrimaryColor(),
				alpha(175 * fade), entity.getStage());
		stack.mulPose(Axis.ZP.rotationDegrees(45.0F + progress * 90.0F));
		drawBillboard(out, stack.last(), radius * 0.62F, 1.0F,
				lighten(entity.getSecondaryColor(), 0.82F), alpha(245 * fade), entity.getStage());
	}

	private void renderFormula(ArcaneVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		int runes = Mth.clamp(Math.round(entity.getScale()), 1, 3);
		float time = entity.tickCount + partialTick;
		float radius = 0.95F + entity.getStage() * 0.10F;
		for (int i = 0; i < runes; i++) {
			double angle = Math.PI * 2.0D * i / 3.0D + time * 0.045D;
			stack.pushPose();
			stack.translate(Math.cos(angle) * radius, 0.42D + Math.sin(time * 0.08D + i) * 0.12D,
					Math.sin(angle) * radius);
			stack.mulPose(entityRenderDispatcher.cameraOrientation());
			stack.mulPose(Axis.ZP.rotationDegrees(time * 1.8F + i * 37.0F));
			drawDiamond(out, stack.last(), 0.25F + entity.getStage() * 0.025F, 1.0F,
					i == runes - 1 ? entity.getSecondaryColor() : entity.getPrimaryColor(),
					alpha(238 * fade), entity.getStage());
			stack.popPose();
		}
	}

	private static void orientForward(ArcaneVfxEntity entity, PoseStack stack) {
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		stack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
	}

	private static void drawBlade(VertexConsumer out, PoseStack.Pose pose, float halfWidth,
			float length, float kind, int color, int alpha, int stage, float zOffset) {
		panelVertex(out, pose, 0.0F, 0.0F, length * 0.18F + zOffset,
				0.5F, 1.0F, kind, color, 0, stage);
		panelVertex(out, pose, halfWidth, 0.0F, -length * 0.12F + zOffset,
				1.0F, 0.70F, kind, color, alpha, stage);
		panelVertex(out, pose, 0.0F, 0.0F, -length + zOffset,
				0.5F, 0.0F, kind, color, 0, stage);
		panelVertex(out, pose, -halfWidth, 0.0F, -length * 0.12F + zOffset,
				0.0F, 0.70F, kind, color, alpha, stage);
	}

	private static void drawRibbon(VertexConsumer out, PoseStack.Pose pose, float halfWidth,
			float length, float kind, int color, int alpha, int stage) {
		panelVertex(out, pose, -halfWidth, 0.0F, 0.0F, 0.0F, 1.0F, kind, color, 0, stage);
		panelVertex(out, pose, halfWidth, 0.0F, 0.0F, 1.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, halfWidth * 0.24F, 0.0F, -length, 1.0F, 0.0F, kind, color, 0, stage);
		panelVertex(out, pose, -halfWidth * 0.24F, 0.0F, -length, 0.0F, 0.0F, kind, color, 0, stage);
	}

	private static void drawBillboard(VertexConsumer out, PoseStack.Pose pose, float radius,
			float kind, int color, int alpha, int stage) {
		panelVertex(out, pose, -radius, -radius, 0.0F, 0.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, radius, -radius, 0.0F, 1.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, radius, radius, 0.0F, 1.0F, 0.0F, kind, color, alpha, stage);
		panelVertex(out, pose, -radius, radius, 0.0F, 0.0F, 0.0F, kind, color, alpha, stage);
	}

	private static void drawDiamond(VertexConsumer out, PoseStack.Pose pose, float radius,
			float kind, int color, int alpha, int stage) {
		panelVertex(out, pose, 0.0F, radius, 0.0F, 0.5F, 0.0F, kind, color, alpha, stage);
		panelVertex(out, pose, radius * 0.72F, 0.0F, 0.0F, 1.0F, 0.5F, kind, color, alpha, stage);
		panelVertex(out, pose, 0.0F, -radius, 0.0F, 0.5F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, -radius * 0.72F, 0.0F, 0.0F, 0.0F, 0.5F, kind, color, alpha, stage);
	}

	private static void drawRing(VertexConsumer out, PoseStack.Pose pose, float outer,
			float inner, float kind, int color, int alpha, int stage, int segments) {
		int count = Math.max(8, segments);
		for (int i = 0; i < count; i++) {
			double a0 = Math.PI * 2.0D * i / count;
			double a1 = Math.PI * 2.0D * (i + 1) / count;
			ringVertex(out, pose, (float) Math.cos(a0) * outer, 0.0F,
					(float) Math.sin(a0) * outer, 0.0F, 0.0F, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a1) * outer, 0.0F,
					(float) Math.sin(a1) * outer, 1.0F, 0.0F, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a1) * inner, 0.0F,
					(float) Math.sin(a1) * inner, 1.0F, 1.0F, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a0) * inner, 0.0F,
					(float) Math.sin(a0) * inner, 0.0F, 1.0F, kind, color, alpha, stage);
		}
	}

	private static void drawRadialRunes(VertexConsumer out, PoseStack.Pose pose, float radius,
			int count, float kind, int color, int alpha, int stage) {
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0D * i / count;
			float along = radius * 0.14F;
			float across = radius * 0.045F;
			float cx = (float) Math.cos(angle) * radius;
			float cz = (float) Math.sin(angle) * radius;
			float dx = (float) Math.cos(angle) * along;
			float dz = (float) Math.sin(angle) * along;
			float px = (float) -Math.sin(angle) * across;
			float pz = (float) Math.cos(angle) * across;
			ringVertex(out, pose, cx - dx - px, 0.012F, cz - dz - pz, 0.0F, 1.0F, kind, color, alpha, stage);
			ringVertex(out, pose, cx - dx + px, 0.012F, cz - dz + pz, 1.0F, 1.0F, kind, color, alpha, stage);
			ringVertex(out, pose, cx + dx + px, 0.012F, cz + dz + pz, 1.0F, 0.0F, kind, color, alpha, stage);
			ringVertex(out, pose, cx + dx - px, 0.012F, cz + dz - pz, 0.0F, 0.0F, kind, color, alpha, stage);
		}
	}

	private static void drawRadialRunesBillboard(VertexConsumer out, PoseStack.Pose pose,
			float radius, int count, float kind, int color, int alpha, int stage) {
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2.0D * i / count;
			float size = radius * 0.10F;
			float cx = (float) Math.cos(angle) * radius;
			float cy = (float) Math.sin(angle) * radius;
			panelVertex(out, pose, cx - size, cy - size, 0.02F, 0.0F, 1.0F, kind, color, alpha, stage);
			panelVertex(out, pose, cx + size, cy - size, 0.02F, 1.0F, 1.0F, kind, color, alpha, stage);
			panelVertex(out, pose, cx + size, cy + size, 0.02F, 1.0F, 0.0F, kind, color, alpha, stage);
			panelVertex(out, pose, cx - size, cy + size, 0.02F, 0.0F, 0.0F, kind, color, alpha, stage);
		}
	}

	private static void drawStar(VertexConsumer out, PoseStack.Pose pose, float radius,
			float kind, int color, int alpha, int stage) {
		for (int i = 0; i < 3; i++) {
			double angle = Math.PI * i / 3.0D;
			float dx = (float) Math.cos(angle) * radius;
			float dz = (float) Math.sin(angle) * radius;
			float px = -dz * 0.035F;
			float pz = dx * 0.035F;
			ringVertex(out, pose, -dx - px, 0.018F, -dz - pz, 0.0F, 1.0F, kind, color, alpha, stage);
			ringVertex(out, pose, -dx + px, 0.018F, -dz + pz, 1.0F, 1.0F, kind, color, alpha, stage);
			ringVertex(out, pose, dx + px, 0.018F, dz + pz, 1.0F, 0.0F, kind, color, alpha, stage);
			ringVertex(out, pose, dx - px, 0.018F, dz - pz, 0.0F, 0.0F, kind, color, alpha, stage);
		}
	}

	private static void panelVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y,
			float z, float u, float v, float kind, int color, int alpha, int stage) {
		vertex(out, pose, x, y, z, u, v, kind, color, alpha, stage, 0.0F, 0.0F, 1.0F);
	}

	private static void ringVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y,
			float z, float u, float v, float kind, int color, int alpha, int stage) {
		vertex(out, pose, x, y, z, u, v, kind, color, alpha, stage, 0.0F, 1.0F, 0.0F);
	}

	private static void vertex(VertexConsumer out, PoseStack.Pose pose, float x, float y, float z,
			float u, float v, float kind, int color, int alpha, int stage,
			float nx, float ny, float nz) {
		out.vertex(pose.pose(), x, y, z)
				.color((color >> 16) & 255, (color >> 8) & 255, color & 255, alpha)
				.uv(kind + 0.001F + u * 0.998F, stage + 0.001F + v * 0.998F)
				.overlayCoords(OverlayTexture.NO_OVERLAY).uv2(240)
				.normal(pose.normal(), nx, ny, nz).endVertex();
	}

	private static int lighten(int color, float amount) {
		int red = (color >> 16) & 255;
		int green = (color >> 8) & 255;
		int blue = color & 255;
		red += Math.round((255 - red) * amount);
		green += Math.round((255 - green) * amount);
		blue += Math.round((255 - blue) * amount);
		return red << 16 | green << 8 | blue;
	}

	private static int alpha(float value) {
		return Mth.clamp(Math.round(value), 0, 255);
	}

	@Override
	public ResourceLocation getTextureLocation(ArcaneVfxEntity entity) {
		return FALLBACK;
	}
}
