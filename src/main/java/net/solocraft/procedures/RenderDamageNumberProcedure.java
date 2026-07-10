package net.solocraft.procedures;

import org.joml.Vector3f;
import org.joml.Matrix4f;

import net.solocraft.util.SystemClientConfig;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.core.BlockPos;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.gui.Font;
import net.minecraft.client.Minecraft;

import javax.annotation.Nullable;

import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RenderDamageNumberProcedure {
	private static RenderLevelStageEvent provider = null;
	private static Map<EntityType, Entity> data = new HashMap<>();
	private static float textWidth = 1.0F;
	private static float textHeight = 1.0F;
	private static int textColor = -1;
	private static int backColor = 0;
	private static final List<DamageNumber> DAMAGE_NUMBERS = new ArrayList<>();
	private static final long DAMAGE_NUMBER_LIFETIME_MS = 1050L;

	private static class DamageNumber {
		final double x;
		final double y;
		final double z;
		final double offsetX;
		final double offsetZ;
		final float amount;
		final int color;
		final long createdAt;

		DamageNumber(double x, double y, double z, float amount, int color) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.amount = amount;
			this.color = color;
			this.createdAt = System.currentTimeMillis();
			this.offsetX = (Math.random() - 0.5D) * 0.55D;
			this.offsetZ = (Math.random() - 0.5D) * 0.55D;
		}
	}

	public static void addNumber(double x, double y, double z, float amount, int color) {
		if (!SystemClientConfig.isDamageNumbersEnabled())
			return;
		DAMAGE_NUMBERS.add(new DamageNumber(x, y, z, amount, color));
		if (DAMAGE_NUMBERS.size() > 80) {
			DAMAGE_NUMBERS.remove(0);
		}
	}

	public static void setBackColor(int color) {
		RenderDamageNumberProcedure.backColor = color;
	}

	public static void setTextColor(int color) {
		RenderDamageNumberProcedure.textColor = color;
	}

	public static void setScale(float width, float height) {
		RenderDamageNumberProcedure.textWidth = width;
		RenderDamageNumberProcedure.textHeight = height;
	}

	public static void renderBlock(BlockState blockState, double x, double y, double z, float yaw, float pitch, float roll, float scale, boolean glowing) {
		BlockPos blockPos = BlockPos.containing(x, y, z);
		Vec3 pos = provider.getCamera().getPosition();
		int packedLight = glowing ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(Minecraft.getInstance().level, blockPos);
		PoseStack poseStack = provider.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(x - pos.x(), y - pos.y(), z - pos.z());
		poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(yaw));
		poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
		poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(roll));
		poseStack.scale(scale, scale, scale);
		poseStack.translate(-0.5F, -0.5F, -0.5F);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		renderBlockModel(blockState, blockPos, poseStack, packedLight);
		renderBlockEntity(blockState, blockPos, poseStack, packedLight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}

	private static void renderBlockEntity(BlockState blockState, BlockPos blockPos, PoseStack poseStack, int packedLight) {
		if (blockState.getBlock() instanceof EntityBlock entityBlock) {
			Minecraft minecraft = Minecraft.getInstance();
			ClientLevel level = minecraft.level;
			BlockEntity blockEntity = entityBlock.newBlockEntity(blockPos, blockState);
			if (blockEntity != null) {
				BlockEntityRenderer blockEntityRenderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
				if (blockEntityRenderer != null) {
					MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
					blockEntity.setLevel(level);
					blockEntityRenderer.render(blockEntity, 0.0F, poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
				}
			}
		}
	}

	private static void renderBlockModel(BlockState blockState, BlockPos blockPos, PoseStack poseStack, int packedLight) {
		if (blockState.getRenderShape() == RenderShape.MODEL) {
			Minecraft minecraft = Minecraft.getInstance();
			ClientLevel level = minecraft.level;
			MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
			BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
			ModelBlockRenderer renderer = dispatcher.getModelRenderer();
			BakedModel bakedModel = dispatcher.getBlockModel(blockState);
			ModelData modelData = bakedModel.getModelData(level, blockPos, blockState, ModelData.builder().build());
			PoseStack.Pose pose = poseStack.last();
			int color = minecraft.getBlockColors().getColor(blockState, level, blockPos);
			float red = (color >> 16 & 255) / 255.0F;
			float green = (color >> 8 & 255) / 255.0F;
			float blue = (color & 255) / 255.0F;
			for (RenderType renderType : bakedModel.getRenderTypes(blockState, RandomSource.create(42L), modelData)) {
				renderer.renderModel(pose, bufferSource.getBuffer(Sheets.translucentCullBlockSheet()), blockState, bakedModel, red, green, blue, packedLight, OverlayTexture.NO_OVERLAY, modelData, renderType);
			}
		}
	}

	public static void renderEntity(EntityType type, double x, double y, double z, float yaw, float pitch, float roll, float scale, boolean glowing) {
		if (type == null)
			return;
		Entity entity;
		ClientLevel level = Minecraft.getInstance().level;
		if (data.containsKey(type)) {
			entity = data.get(type);
			if (entity.level() != level) {
				entity = type.create(level);
				data.put(type, entity);
			}
		} else {
			entity = type.create(level);
			data.put(type, entity);
		}
		renderEntity(entity, 0.0F, x, y, z, yaw, pitch, roll, scale, glowing ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(level, BlockPos.containing(x, y, z)));
	}

	public static void renderEntity(Entity entity, double x, double y, double z, float yaw, float pitch, float roll, float scale, boolean glowing) {
		float partialTick = provider.getPartialTick();
		int packedLight = glowing ? LightTexture.FULL_BRIGHT : Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(entity, partialTick);
		renderEntity(entity, partialTick, x, y, z, yaw, pitch, roll, scale, packedLight);
	}

	private static void renderEntity(Entity entity, float partialTick, double x, double y, double z, float yaw, float pitch, float roll, float scale, int packedLight) {
		if (entity == null)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		EntityRenderer renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
		Vec3 pos = provider.getCamera().getPosition();
		float offset = (entity.getBbHeight() / 2.0F) * scale;
		PoseStack poseStack = provider.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(x - pos.x(), y + offset - pos.y(), z - pos.z());
		poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(yaw));
		poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
		poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(roll));
		poseStack.translate(0.0F, -offset, 0.0F);
		poseStack.scale(scale, scale, scale);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		renderer.render(entity, entity.getViewYRot(partialTick), partialTick, poseStack, bufferSource, packedLight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}

	public static void renderItem(ItemStack itemStack, double x, double y, double z, float yaw, float pitch, float roll, float scale, boolean flipping, boolean glowing) {
		Minecraft minecraft = Minecraft.getInstance();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		ItemRenderer renderer = minecraft.getItemRenderer();
		Vec3 pos = provider.getCamera().getPosition();
		int packedLight = glowing ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(x, y, z));
		PoseStack poseStack = provider.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(x - pos.x(), y - pos.y(), z - pos.z());
		poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(yaw));
		poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
		poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(roll));
		poseStack.scale(scale, scale, scale);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		renderer.renderStatic(null, itemStack, ItemDisplayContext.FIXED, flipping, poseStack, bufferSource, minecraft.level, packedLight, OverlayTexture.NO_OVERLAY, 0);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}

	public static void renderLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		Vec3 pos = provider.getCamera().getPosition();
		Vector3f normal = new Vec3(x2 - x1, y2 - y1, z2 - z1).normalize().toVector3f();
		Matrix4f matrix4f = provider.getPoseStack().last().pose();
		VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.lines());
		vertexConsumer.vertex(matrix4f, (float) (x1 - pos.x()), (float) (y1 - pos.y()), (float) (z1 - pos.z())).color(color).normal(normal.x(), normal.y(), normal.z()).endVertex();
		vertexConsumer.vertex(matrix4f, (float) (x2 - pos.x()), (float) (y2 - pos.y()), (float) (z2 - pos.z())).color(color).normal(normal.x(), normal.y(), normal.z()).endVertex();
	}

	public static void renderTexts(String texts, double x, double y, double z, float yaw, float pitch, float roll, boolean glowing) {
		Minecraft minecraft = Minecraft.getInstance();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		Font font = minecraft.font;
		Vec3 pos = provider.getCamera().getPosition();
		int packedLight = glowing ? LightTexture.FULL_BRIGHT : LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(x, y, z));
		PoseStack poseStack = provider.getPoseStack();
		poseStack.pushPose();
		poseStack.translate(x - pos.x(), y - pos.y(), z - pos.z());
		poseStack.mulPose(com.mojang.math.Axis.YN.rotationDegrees(yaw));
		poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(pitch));
		poseStack.mulPose(com.mojang.math.Axis.ZN.rotationDegrees(roll));
		poseStack.scale(textWidth, -textHeight, 1.0F);
		poseStack.translate((font.width(texts) - 1) * -0.5F, (font.lineHeight - 1) * -0.5F, 0.0F);
		Matrix4f matrix4f = poseStack.last().pose();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		if (backColor != 0)
			font.drawInBatch(texts, 0.0F, 0.0F, 0, false, matrix4f, bufferSource, Font.DisplayMode.SEE_THROUGH, backColor, packedLight);
		font.drawInBatch(texts, 0.0F, 0.0F, textColor, false, matrix4f, bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		poseStack.popPose();
	}

	@SubscribeEvent
	public static void renderModels(RenderLevelStageEvent event) {
		provider = event;
		if (provider.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
			ClientLevel level = Minecraft.getInstance().level;
			Entity entity = provider.getCamera().getEntity();
			Vec3 pos = entity.getPosition(provider.getPartialTick());
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			execute(provider);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
			RenderSystem.enableCull();
			RenderSystem.enableDepthTest();
			RenderSystem.depthMask(true);
		}
	}

	public static void execute() {
		execute(null);
	}

	private static void execute(@Nullable Event event) {
		if (provider == null)
			return;
		if (!SystemClientConfig.isDamageNumbersEnabled()) {
			DAMAGE_NUMBERS.clear();
			return;
		}
		renderDamageNumbers();
	}

	private static void renderDamageNumbers() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level == null || minecraft.player == null)
			return;
		long now = System.currentTimeMillis();
		MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
		Vec3 cameraPos = provider.getCamera().getPosition();
		PoseStack poseStack = provider.getPoseStack();
		Font font = minecraft.font;

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.disableDepthTest();
		for (Iterator<DamageNumber> iterator = DAMAGE_NUMBERS.iterator(); iterator.hasNext();) {
			DamageNumber number = iterator.next();
			float age = (float) (now - number.createdAt) / (float) DAMAGE_NUMBER_LIFETIME_MS;
			if (age >= 1.0F) {
				iterator.remove();
				continue;
			}
			float rise = age * 0.85F;
			float alpha = 1.0F - age;
			String text = formatDamage(number.amount);
			int color = applyAlpha(number.color, alpha);
			int shadow = applyAlpha(0xFF020814, alpha * 0.72F);

			poseStack.pushPose();
			poseStack.translate(number.x + number.offsetX - cameraPos.x(), number.y + rise - cameraPos.y(), number.z + number.offsetZ - cameraPos.z());
			poseStack.mulPose(minecraft.getEntityRenderDispatcher().cameraOrientation());
			float scale = 0.026F + age * 0.006F;
			poseStack.scale(-scale, -scale, scale);
			float width = font.width(text) / 2.0F;
			Matrix4f matrix = poseStack.last().pose();
			font.drawInBatch(text, -width + 1.0F, 1.0F, shadow, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
			font.drawInBatch(text, -width, 0.0F, color, false, matrix, bufferSource, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
			poseStack.popPose();
		}
		bufferSource.endBatch();
		RenderSystem.enableDepthTest();
		RenderSystem.disableBlend();
	}

	private static String formatDamage(float amount) {
		if (amount < 10.0F && amount != Math.round(amount)) {
			return String.format(Locale.US, "%.1f", amount);
		}
		return Integer.toString(Math.round(amount));
	}

	private static int applyAlpha(int color, float alpha) {
		int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * alpha)));
		return (color & 0x00FFFFFF) | (a << 24);
	}
}
