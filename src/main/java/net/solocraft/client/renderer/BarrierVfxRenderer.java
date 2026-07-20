package net.solocraft.client.renderer;

import net.solocraft.client.renderer.shader.BarrierVfxRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.entity.BarrierVfxEntity;

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

public class BarrierVfxRenderer extends EntityRenderer<BarrierVfxEntity> {
	private static final ResourceLocation FALLBACK = new ResourceLocation("sololeveling",
			"textures/particle/glow_yellow.png");

	public BarrierVfxRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public void render(BarrierVfxEntity entity, float entityYaw, float partialTick, PoseStack stack,
			MultiBufferSource buffers, int packedLight) {
		float fade = entity.getFade(partialTick);
		if (fade <= 0.002F || hideFirstPersonMirror(entity))
			return;

		VertexConsumer out = DeferredWorldShaderRenderer.buffer(buffers,
				BarrierVfxRenderTypes.effect(FALLBACK), true);
		stack.pushPose();
		switch (entity.getStyle()) {
			case BarrierVfxEntity.FRACTURE_BOLT -> renderBolt(entity, partialTick, stack, out, fade);
			case BarrierVfxEntity.FRACTURE_MARK -> renderMark(entity, partialTick, stack, out, fade);
			case BarrierVfxEntity.PRISM_RAMPART -> renderRampart(entity, partialTick, stack, out, fade, false);
			case BarrierVfxEntity.SHARD_PLATE -> renderRampart(entity, partialTick, stack, out, fade, true);
			case BarrierVfxEntity.REPULSION_FRAME -> renderRepulsion(entity, partialTick, stack, out, fade);
			case BarrierVfxEntity.SEALING_PRISM -> renderPrison(entity, partialTick, stack, out, fade);
			case BarrierVfxEntity.MIRROR_WARD -> renderMirror(entity, partialTick, stack, out, fade);
			case BarrierVfxEntity.RESONANT_COLLAPSE -> renderCollapse(entity, partialTick, stack, out, fade);
			case BarrierVfxEntity.ABSOLUTE_BASTION -> renderBastion(entity, partialTick, stack, out, fade);
			case BarrierVfxEntity.RETURN_SHARD -> renderReturnShard(entity, partialTick, stack, out, fade);
			default -> renderImpact(entity, partialTick, stack, out, fade);
		}
		stack.popPose();
		super.render(entity, entityYaw, partialTick, stack, buffers, packedLight);
	}

	private boolean hideFirstPersonMirror(BarrierVfxEntity entity) {
		Minecraft minecraft = Minecraft.getInstance();
		return entity.getStyle() == BarrierVfxEntity.MIRROR_WARD
				&& minecraft.options.getCameraType().isFirstPerson()
				&& minecraft.getCameraEntity() != null
				&& entity.getOwnerId().filter(minecraft.getCameraEntity().getUUID()::equals).isPresent();
	}

	private void renderBolt(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		float time = entity.tickCount + partialTick;
		float width = entity.getScale() * (0.90F + Mth.sin(time * 1.2F + entity.getSeed()) * 0.08F);
		float length = entity.getLength();
		int stage = entity.getStage();
		drawBlade(out, stack.last(), width * 1.75F, length * 1.08F, 0.0F, 0.0F,
				entity.getPrimaryColor(), alpha(72 * fade), stage, 0.0F);
		drawBlade(out, stack.last(), width, length, 0.0F, 0.015F,
				entity.getSecondaryColor(), alpha(235 * fade), stage, 0.0F);
		stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
		drawBlade(out, stack.last(), width * 0.52F, length * 0.92F, 0.0F, 0.02F,
				lighten(entity.getSecondaryColor(), 0.72F), alpha(185 * fade), stage, 0.0F);
		if (stage >= 4) {
			stack.mulPose(Axis.ZP.rotationDegrees(45.0F));
			drawBlade(out, stack.last(), width * 0.35F, length * 0.82F, 0.0F, 0.03F,
					entity.getPrimaryColor(), alpha(125 * fade), stage, 0.0F);
		}
	}

	private void renderReturnShard(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		float pulse = 0.92F + Mth.sin((entity.tickCount + partialTick) * 1.35F) * 0.08F;
		drawBlade(out, stack.last(), entity.getScale() * 1.5F * pulse, entity.getLength(),
				0.0F, 0.0F, entity.getPrimaryColor(), alpha(105 * fade), entity.getStage(), 0.0F);
		drawBlade(out, stack.last(), entity.getScale() * 0.62F * pulse,
				entity.getLength() * 1.04F, 0.0F, 0.02F, entity.getSecondaryColor(),
				alpha(245 * fade), entity.getStage(), 0.0F);
	}

	private void renderMark(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float time = entity.tickCount + partialTick;
		float size = entity.getScale() * (0.94F + Mth.sin(time * 0.42F + entity.getSeed()) * 0.06F);
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		stack.mulPose(Axis.ZP.rotationDegrees(time * 1.4F));
		drawBillboard(out, stack.last(), size * 1.35F, 1.0F, entity.getPrimaryColor(),
				alpha(62 * fade), entity.getStage());
		stack.mulPose(Axis.ZP.rotationDegrees(-time * 3.1F));
		drawBillboard(out, stack.last(), size, 1.0F, entity.getSecondaryColor(),
				alpha(210 * fade), entity.getStage());
		for (int i = -1; i <= 1; i++) {
			stack.pushPose();
			stack.translate(i * size * 0.27F, -size * 0.72F + Math.abs(i) * size * 0.18F, 0.025F);
			stack.mulPose(Axis.ZP.rotationDegrees(i * 18.0F));
			drawThinVertical(out, stack.last(), size * 0.11F, size * (1.35F - Math.abs(i) * 0.22F),
					7.0F, lighten(entity.getSecondaryColor(), 0.8F), alpha(230 * fade), entity.getStage());
			stack.popPose();
		}
	}

	private void renderRampart(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade, boolean shardPlate) {
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		int stage = entity.getStage();
		float width = entity.getScale();
		float height = entity.getLength();
		float integrity = integrityFade(entity);
		float shimmer = 0.92F + Mth.sin((entity.tickCount + partialTick) * 0.22F + entity.getSeed()) * 0.08F;
		int panelAlpha = alpha((shardPlate ? 105 : 132) * fade * integrity * shimmer);

		drawPanel(out, stack.last(), width, height, 2.0F, entity.getPrimaryColor(), panelAlpha, stage, 0.0F);
		drawPanel(out, stack.last(), width * 0.96F, height * 0.96F, 2.0F,
				entity.getSecondaryColor(), alpha(45 * fade * integrity), stage, -0.028F);
		drawPanelFrame(out, stack.last(), width, height, 0.055F + stage * 0.012F,
				3.0F, entity.getSecondaryColor(), alpha(235 * fade * integrity), stage, 0.035F);

		if (stage >= 3 && !shardPlate) {
			renderRampartWing(stack, out, width, height, -1.0F, entity, fade * integrity);
			renderRampartWing(stack, out, width, height, 1.0F, entity, fade * integrity);
		}
		if (stage >= 5 && !shardPlate) {
			stack.pushPose();
			stack.translate(0.0D, height * 0.92D, 0.0D);
			drawArcCrown(out, stack.last(), width * 1.04F, height * 0.30F, 7.0F,
					entity.getSecondaryColor(), alpha(205 * fade * integrity), stage, 14);
			stack.popPose();
		}
	}

	private void renderRampartWing(PoseStack stack, VertexConsumer out, float width, float height,
			float side, BarrierVfxEntity entity, float fade) {
		stack.pushPose();
		stack.translate(side * width * 0.94D, 0.0D, width * 0.20D);
		stack.mulPose(Axis.YP.rotationDegrees(side * 27.0F));
		drawPanel(out, stack.last(), width * 0.42F, height * 0.88F, 2.0F,
				entity.getPrimaryColor(), alpha(92 * fade), entity.getStage(), 0.0F);
		drawPanelFrame(out, stack.last(), width * 0.42F, height * 0.88F, 0.07F,
				3.0F, entity.getSecondaryColor(), alpha(190 * fade), entity.getStage(), 0.025F);
		stack.popPose();
	}

	private void renderRepulsion(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		orientForward(entity, stack);
		int stage = entity.getStage();
		float width = entity.getScale();
		float height = entity.getLength();
		int echoes = stage >= 5 ? 4 : stage >= 3 ? 3 : 2;
		for (int i = echoes; i >= 0; i--) {
			stack.pushPose();
			stack.translate(0.0D, 0.0D, i * 0.28D);
			float echoFade = fade * (1.0F - i / (float) (echoes + 1));
			drawPanel(out, stack.last(), width, height, 3.0F, entity.getPrimaryColor(),
					alpha(36 * echoFade), stage, 0.0F);
			drawPanelFrame(out, stack.last(), width, height, 0.10F, 3.0F,
					i == 0 ? entity.getSecondaryColor() : entity.getPrimaryColor(),
					alpha((i == 0 ? 240 : 125) * echoFade), stage, 0.025F);
			if (stage >= 4 && i == 0)
				drawDiamond(out, stack.last(), width * 0.48F, height * 0.36F, 7.0F,
						entity.getSecondaryColor(), alpha(190 * fade), stage, 0.04F);
			stack.popPose();
		}
	}

	private void renderPrison(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		int stage = entity.getStage();
		float radius = entity.getScale();
		float height = entity.getLength();
		float integrity = integrityFade(entity);
		int sides = stage >= 4 ? 6 : 4;
		float rotation = (entity.tickCount + partialTick) * (stage >= 5 ? 0.35F : 0.14F);
		stack.mulPose(Axis.YP.rotationDegrees(rotation));
		drawWallRing(out, stack.last(), radius, height, 4.0F, entity.getPrimaryColor(),
				alpha(102 * fade * integrity), stage, sides, 0.0F);
		drawWallRing(out, stack.last(), radius * 1.018F, height, 4.0F, entity.getSecondaryColor(),
				alpha(34 * fade * integrity), stage, sides, 0.0F);
		drawRing(out, stack.last(), radius * 1.02F, radius * 0.83F, 6.0F,
				entity.getSecondaryColor(), alpha(225 * fade * integrity), stage, sides * 4, 0.02F);
		stack.pushPose();
		stack.translate(0.0D, height, 0.0D);
		drawRing(out, stack.last(), radius * 1.02F, 0.0F, 6.0F,
				entity.getSecondaryColor(), alpha(170 * fade * integrity), stage, sides * 4, -0.02F);
		stack.popPose();
		if (stage >= 3)
			drawPrismEdges(out, stack.last(), radius, height, sides, entity.getSecondaryColor(),
					alpha(235 * fade * integrity), stage);
	}

	private void renderMirror(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		float width = entity.getScale();
		float height = entity.getLength();
		float integrity = integrityFade(entity);
		int stage = entity.getStage();
		int panels = stage >= 4 ? 5 : stage >= 2 ? 3 : 1;
		for (int i = 0; i < panels; i++) {
			float centered = i - (panels - 1) * 0.5F;
			float angle = centered * 22.0F;
			stack.pushPose();
			stack.mulPose(Axis.YP.rotationDegrees(angle));
			stack.translate(0.0D, 0.0D, -0.72D - Math.abs(centered) * 0.08D);
			drawPanel(out, stack.last(), width * (panels == 1 ? 1.0F : 0.58F), height,
					5.0F, entity.getPrimaryColor(), alpha(70 * fade * integrity), stage, 0.0F);
			drawPanelFrame(out, stack.last(), width * (panels == 1 ? 1.0F : 0.58F), height,
					0.055F, 5.0F, entity.getSecondaryColor(), alpha(180 * fade * integrity), stage, 0.025F);
			stack.popPose();
		}
		stack.pushPose();
		stack.translate(0.0D, height * 0.51D, -0.78D);
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		drawBillboard(out, stack.last(), width * 1.35F, 5.0F, entity.getSecondaryColor(),
				alpha(42 * fade * integrity), stage);
		stack.popPose();
	}

	private void renderCollapse(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float progress = entity.getProgress(partialTick);
		float radius = entity.getScale() * (0.16F + progress * 0.92F);
		int stage = entity.getStage();
		drawRing(out, stack.last(), radius, radius * (0.70F - progress * 0.22F), 6.0F,
				entity.getPrimaryColor(), alpha(210 * fade), stage, stage >= 5 ? 48 : 28, 0.03F);
		drawRing(out, stack.last(), radius * 0.64F, radius * 0.52F, 6.0F,
				entity.getSecondaryColor(), alpha(235 * fade), stage, stage >= 5 ? 36 : 20, 0.04F);
		int shards = stage >= 5 ? 12 : 5 + stage;
		for (int i = 0; i < shards; i++) {
			double angle = Math.PI * 2.0D * i / shards + entity.getSeed() * 0.0001D;
			float distance = radius * (0.30F + (i % 3) * 0.19F);
			stack.pushPose();
			stack.translate(Math.cos(angle) * distance, 0.0D, Math.sin(angle) * distance);
			stack.mulPose(Axis.YP.rotationDegrees((float) (-angle * Mth.RAD_TO_DEG)));
			drawThinVertical(out, stack.last(), entity.getScale() * 0.045F,
					entity.getLength() * (0.45F + (i % 4) * 0.13F), 7.0F,
					i % 2 == 0 ? entity.getSecondaryColor() : entity.getPrimaryColor(),
					alpha(210 * fade), stage);
			stack.popPose();
		}
	}

	private void renderBastion(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		int stage = entity.getStage();
		float radius = entity.getScale();
		float height = entity.getLength();
		float integrity = integrityFade(entity);
		float time = entity.tickCount + partialTick;
		int segments = stage >= 5 ? 32 : stage >= 3 ? 24 : 18;
		float wallHeight = height * 0.58F;
		float rotation = time * 0.08F;
		stack.mulPose(Axis.YP.rotationDegrees(rotation));

		drawWallRing(out, stack.last(), radius, wallHeight, 2.0F, entity.getPrimaryColor(),
				alpha(74 * fade * integrity), stage, segments, 0.0F);
		drawWallRing(out, stack.last(), radius * 0.992F, wallHeight, 2.0F,
				entity.getSecondaryColor(), alpha(24 * fade * integrity), stage, segments, 0.0F);
		drawDome(out, stack.last(), radius, wallHeight, height, 2.0F, entity.getPrimaryColor(),
				alpha(58 * fade * integrity), stage, segments, stage >= 4 ? 6 : 4);
		drawRing(out, stack.last(), radius, radius * 0.91F, 6.0F, entity.getSecondaryColor(),
				alpha(230 * fade * integrity), stage, segments, 0.025F);
		drawRing(out, stack.last(), radius * 0.67F, radius * 0.61F, 6.0F,
				entity.getPrimaryColor(), alpha(150 * fade * integrity), stage, segments, 0.035F);
		if (stage >= 3)
			drawRadialGlyph(out, stack.last(), radius * 0.88F, 6.0F, entity.getSecondaryColor(),
					alpha(145 * fade * integrity), stage, stage >= 5 ? 16 : 10);
	}

	private void renderImpact(BarrierVfxEntity entity, float partialTick, PoseStack stack,
			VertexConsumer out, float fade) {
		float progress = entity.getProgress(partialTick);
		float size = entity.getScale() * (0.20F + progress * 1.15F);
		drawRing(out, stack.last(), size, size * 0.54F, 6.0F, entity.getPrimaryColor(),
				alpha(220 * fade), entity.getStage(), 24, 0.025F);
		stack.translate(0.0D, size * 0.18D, 0.0D);
		stack.mulPose(this.entityRenderDispatcher.cameraOrientation());
		stack.mulPose(Axis.ZP.rotationDegrees(entity.getSeed() * 0.01F));
		drawBillboard(out, stack.last(), size, 7.0F, entity.getSecondaryColor(),
				alpha(230 * fade), entity.getStage());
	}

	private static float integrityFade(BarrierVfxEntity entity) {
		if (entity.getMaxIntegrity() <= 0.001F)
			return 1.0F;
		return 0.48F + 0.52F * Mth.clamp(entity.getIntegrity() / entity.getMaxIntegrity(), 0.0F, 1.0F);
	}

	private static void orientForward(BarrierVfxEntity entity, PoseStack stack) {
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot()));
		stack.mulPose(Axis.XP.rotationDegrees(entity.getXRot()));
	}

	private static void drawBlade(VertexConsumer out, PoseStack.Pose pose, float halfWidth,
			float length, float yOffset, float zOffset, int color, int alpha, int stage, float roll) {
		float sin = Mth.sin(roll * Mth.DEG_TO_RAD);
		float cos = Mth.cos(roll * Mth.DEG_TO_RAD);
		bladeVertex(out, pose, 0.0F, yOffset, length * 0.15F + zOffset, 0.50F, 1.0F,
				color, 0, stage, sin, cos);
		bladeVertex(out, pose, halfWidth, yOffset, -length * 0.18F + zOffset, 1.0F, 0.72F,
				color, alpha, stage, sin, cos);
		bladeVertex(out, pose, halfWidth * 0.08F, yOffset, -length + zOffset, 0.56F, 0.0F,
				color, 0, stage, sin, cos);
		bladeVertex(out, pose, -halfWidth, yOffset, -length * 0.18F + zOffset, 0.0F, 0.72F,
				color, alpha, stage, sin, cos);
	}

	private static void bladeVertex(VertexConsumer out, PoseStack.Pose pose, float x, float y,
			float z, float u, float v, int color, int alpha, int stage, float sin, float cos) {
		vertex(out, pose, x * cos - y * sin, x * sin + y * cos, z,
				u, v, 0.0F, color, alpha, stage, 0.0F, 1.0F, 0.0F);
	}

	private static void drawPanel(VertexConsumer out, PoseStack.Pose pose, float halfWidth,
			float height, float kind, int color, int alpha, int stage, float z) {
		panelVertex(out, pose, -halfWidth, 0.0F, z, 0.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, halfWidth, 0.0F, z, 1.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, halfWidth, height, z, 1.0F, 0.0F, kind, color, alpha, stage);
		panelVertex(out, pose, -halfWidth, height, z, 0.0F, 0.0F, kind, color, alpha, stage);
	}

	private static void drawPanelFrame(VertexConsumer out, PoseStack.Pose pose, float halfWidth,
			float height, float thickness, float kind, int color, int alpha, int stage, float z) {
		drawPanelRect(out, pose, -halfWidth, 0.0F, -halfWidth + thickness, height,
				kind, color, alpha, stage, z);
		drawPanelRect(out, pose, halfWidth - thickness, 0.0F, halfWidth, height,
				kind, color, alpha, stage, z);
		drawPanelRect(out, pose, -halfWidth, 0.0F, halfWidth, thickness,
				kind, color, alpha, stage, z);
		drawPanelRect(out, pose, -halfWidth, height - thickness, halfWidth, height,
				kind, color, alpha, stage, z);
	}

	private static void drawPanelRect(VertexConsumer out, PoseStack.Pose pose, float x0, float y0,
			float x1, float y1, float kind, int color, int alpha, int stage, float z) {
		panelVertex(out, pose, x0, y0, z, 0.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, x1, y0, z, 1.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, x1, y1, z, 1.0F, 0.0F, kind, color, alpha, stage);
		panelVertex(out, pose, x0, y1, z, 0.0F, 0.0F, kind, color, alpha, stage);
	}

	private static void drawDiamond(VertexConsumer out, PoseStack.Pose pose, float halfWidth,
			float halfHeight, float kind, int color, int alpha, int stage, float z) {
		panelVertex(out, pose, 0.0F, halfHeight * 2.0F, z, 0.5F, 0.0F, kind, color, alpha, stage);
		panelVertex(out, pose, halfWidth, halfHeight, z, 1.0F, 0.5F, kind, color, alpha, stage);
		panelVertex(out, pose, 0.0F, 0.0F, z, 0.5F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, -halfWidth, halfHeight, z, 0.0F, 0.5F, kind, color, alpha, stage);
	}

	private static void drawThinVertical(VertexConsumer out, PoseStack.Pose pose, float halfWidth,
			float height, float kind, int color, int alpha, int stage) {
		drawPanelRect(out, pose, -halfWidth, 0.0F, halfWidth, height,
				kind, color, alpha, stage, 0.0F);
	}

	private static void drawBillboard(VertexConsumer out, PoseStack.Pose pose, float radius,
			float kind, int color, int alpha, int stage) {
		panelVertex(out, pose, -radius, -radius, 0.0F, 0.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, radius, -radius, 0.0F, 1.0F, 1.0F, kind, color, alpha, stage);
		panelVertex(out, pose, radius, radius, 0.0F, 1.0F, 0.0F, kind, color, alpha, stage);
		panelVertex(out, pose, -radius, radius, 0.0F, 0.0F, 0.0F, kind, color, alpha, stage);
	}

	private static void drawWallRing(VertexConsumer out, PoseStack.Pose pose, float radius,
			float height, float kind, int color, int alpha, int stage, int segments, float offset) {
		int count = Math.max(4, segments);
		for (int i = 0; i < count; i++) {
			double a0 = Math.PI * 2.0D * i / count + offset;
			double a1 = Math.PI * 2.0D * (i + 1) / count + offset;
			float x0 = (float) Math.cos(a0) * radius;
			float z0 = (float) Math.sin(a0) * radius;
			float x1 = (float) Math.cos(a1) * radius;
			float z1 = (float) Math.sin(a1) * radius;
			float nx = (float) Math.cos((a0 + a1) * 0.5D);
			float nz = (float) Math.sin((a0 + a1) * 0.5D);
			vertex(out, pose, x0, 0.0F, z0, 0.0F, 1.0F, kind, color, alpha, stage, nx, 0.0F, nz);
			vertex(out, pose, x1, 0.0F, z1, 1.0F, 1.0F, kind, color, alpha, stage, nx, 0.0F, nz);
			vertex(out, pose, x1, height, z1, 1.0F, 0.0F, kind, color, alpha, stage, nx, 0.0F, nz);
			vertex(out, pose, x0, height, z0, 0.0F, 0.0F, kind, color, alpha, stage, nx, 0.0F, nz);
		}
	}

	private static void drawDome(VertexConsumer out, PoseStack.Pose pose, float radius,
			float baseHeight, float topHeight, float kind, int color, int alpha, int stage,
			int segments, int layers) {
		for (int layer = 0; layer < layers; layer++) {
			double t0 = layer / (double) layers * Math.PI * 0.5D;
			double t1 = (layer + 1) / (double) layers * Math.PI * 0.5D;
			float r0 = (float) Math.cos(t0) * radius;
			float r1 = (float) Math.cos(t1) * radius;
			float y0 = baseHeight + (float) Math.sin(t0) * (topHeight - baseHeight);
			float y1 = baseHeight + (float) Math.sin(t1) * (topHeight - baseHeight);
			for (int i = 0; i < segments; i++) {
				double a0 = Math.PI * 2.0D * i / segments;
				double a1 = Math.PI * 2.0D * (i + 1) / segments;
				float nx = (float) (Math.cos((a0 + a1) * 0.5D) * Math.cos((t0 + t1) * 0.5D));
				float ny = (float) Math.sin((t0 + t1) * 0.5D);
				float nz = (float) (Math.sin((a0 + a1) * 0.5D) * Math.cos((t0 + t1) * 0.5D));
				vertex(out, pose, (float) Math.cos(a0) * r0, y0, (float) Math.sin(a0) * r0,
						0.0F, 1.0F, kind, color, alpha, stage, nx, ny, nz);
				vertex(out, pose, (float) Math.cos(a1) * r0, y0, (float) Math.sin(a1) * r0,
						1.0F, 1.0F, kind, color, alpha, stage, nx, ny, nz);
				vertex(out, pose, (float) Math.cos(a1) * r1, y1, (float) Math.sin(a1) * r1,
						1.0F, 0.0F, kind, color, alpha, stage, nx, ny, nz);
				vertex(out, pose, (float) Math.cos(a0) * r1, y1, (float) Math.sin(a0) * r1,
						0.0F, 0.0F, kind, color, alpha, stage, nx, ny, nz);
			}
		}
	}

	private static void drawRing(VertexConsumer out, PoseStack.Pose pose, float outer, float inner,
			float kind, int color, int alpha, int stage, int segments, float y) {
		float safeInner = Mth.clamp(inner, 0.0F, Math.max(0.0F, outer - 0.01F));
		int count = Math.max(8, segments);
		for (int i = 0; i < count; i++) {
			double a0 = Math.PI * 2.0D * i / count;
			double a1 = Math.PI * 2.0D * (i + 1) / count;
			ringVertex(out, pose, (float) Math.cos(a0) * outer, y,
					(float) Math.sin(a0) * outer, 0.0F, 1.0F, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a1) * outer, y,
					(float) Math.sin(a1) * outer, 1.0F, 1.0F, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a1) * safeInner, y,
					(float) Math.sin(a1) * safeInner, 1.0F, 0.0F, kind, color, alpha, stage);
			ringVertex(out, pose, (float) Math.cos(a0) * safeInner, y,
					(float) Math.sin(a0) * safeInner, 0.0F, 0.0F, kind, color, alpha, stage);
		}
	}

	private static void drawPrismEdges(VertexConsumer out, PoseStack.Pose pose, float radius,
			float height, int sides, int color, int alpha, int stage) {
		for (int i = 0; i < sides; i++) {
			double angle = Math.PI * 2.0D * i / sides;
			float x = (float) Math.cos(angle) * radius;
			float z = (float) Math.sin(angle) * radius;
			float nextX = (float) Math.cos(angle + 0.06D) * radius;
			float nextZ = (float) Math.sin(angle + 0.06D) * radius;
			vertex(out, pose, x, 0.0F, z, 0.0F, 1.0F, 7.0F, color, alpha, stage, x, 0.0F, z);
			vertex(out, pose, nextX, 0.0F, nextZ, 1.0F, 1.0F, 7.0F, color, alpha, stage, x, 0.0F, z);
			vertex(out, pose, nextX, height, nextZ, 1.0F, 0.0F, 7.0F, color, alpha, stage, x, 0.0F, z);
			vertex(out, pose, x, height, z, 0.0F, 0.0F, 7.0F, color, alpha, stage, x, 0.0F, z);
		}
	}

	private static void drawRadialGlyph(VertexConsumer out, PoseStack.Pose pose, float radius,
			float kind, int color, int alpha, int stage, int spokes) {
		float thickness = Math.max(0.025F, radius * 0.012F);
		for (int i = 0; i < spokes; i++) {
			double angle = Math.PI * 2.0D * i / spokes;
			float dx = (float) Math.cos(angle);
			float dz = (float) Math.sin(angle);
			float px = -dz * thickness;
			float pz = dx * thickness;
			vertex(out, pose, px, 0.04F, pz, 0.0F, 1.0F, kind, color, alpha, stage, 0, 1, 0);
			vertex(out, pose, -px, 0.04F, -pz, 1.0F, 1.0F, kind, color, alpha, stage, 0, 1, 0);
			vertex(out, pose, dx * radius - px, 0.04F, dz * radius - pz,
					1.0F, 0.0F, kind, color, alpha, stage, 0, 1, 0);
			vertex(out, pose, dx * radius + px, 0.04F, dz * radius + pz,
					0.0F, 0.0F, kind, color, alpha, stage, 0, 1, 0);
		}
	}

	private static void drawArcCrown(VertexConsumer out, PoseStack.Pose pose, float radius,
			float height, float kind, int color, int alpha, int stage, int segments) {
		float thickness = Math.max(0.04F, radius * 0.025F);
		for (int i = 0; i < segments; i++) {
			double a0 = Math.PI * i / segments;
			double a1 = Math.PI * (i + 1) / segments;
			float x0 = (float) Math.cos(a0) * radius;
			float y0 = (float) Math.sin(a0) * height;
			float x1 = (float) Math.cos(a1) * radius;
			float y1 = (float) Math.sin(a1) * height;
			panelVertex(out, pose, x0, y0 - thickness, 0.05F, 0.0F, 1.0F, kind, color, alpha, stage);
			panelVertex(out, pose, x1, y1 - thickness, 0.05F, 1.0F, 1.0F, kind, color, alpha, stage);
			panelVertex(out, pose, x1, y1 + thickness, 0.05F, 1.0F, 0.0F, kind, color, alpha, stage);
			panelVertex(out, pose, x0, y0 + thickness, 0.05F, 0.0F, 0.0F, kind, color, alpha, stage);
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
	public ResourceLocation getTextureLocation(BarrierVfxEntity entity) {
		return FALLBACK;
	}
}
