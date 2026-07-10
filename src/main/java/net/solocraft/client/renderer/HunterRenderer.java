
package net.solocraft.client.renderer;

import net.solocraft.entity.HunterEntity;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.syncher.EntityDataAccessor;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class HunterRenderer extends HumanoidMobRenderer<HunterEntity, HumanoidModel<HunterEntity>> {

	/** Returns true if the entity's data accessor equals value and it is not invisible. */
	private static boolean variantVisible(HunterEntity entity, EntityDataAccessor<Integer> accessor, int value) {
		return entity.getEntityData().get(accessor) == value && !entity.hasEffect(MobEffects.INVISIBILITY);
	}

	/** Returns true if the entity's data accessor equals value (no invisibility check). */
	private static boolean variantVisibleNoInvis(HunterEntity entity, EntityDataAccessor<Integer> accessor, int value) {
		return entity.getEntityData().get(accessor) == value;
	}

	/** Adds a simple variant layer that renders texturePath when DATA matches value (with invisibility check). */
	private void addVariantLayer(EntityRendererProvider.Context context, String texturePath,
			EntityDataAccessor<Integer> accessor, int value) {
		final ResourceLocation tex = new ResourceLocation("sololeveling:" + texturePath);
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity,
					float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
					float netHeadYaw, float headPitch) {
				if (variantVisible(entity, accessor, value)) {
					VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(tex));
					this.getParentModel().renderToBuffer(poseStack, vc, 15728640,
							LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
	}

	/** Adds a variant layer without the invisibility check (used for Eyes). */
	private void addVariantLayerNoInvis(EntityRendererProvider.Context context, String texturePath,
			EntityDataAccessor<Integer> accessor, int value) {
		final ResourceLocation tex = new ResourceLocation("sololeveling:" + texturePath);
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity,
					float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
					float netHeadYaw, float headPitch) {
				if (variantVisibleNoInvis(entity, accessor, value)) {
					VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(tex));
					this.getParentModel().renderToBuffer(poseStack, vc, 15728640,
							LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
	}

	public HunterRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer(this,
				new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
				context.getModelManager()));

		// Eyes (no invisibility check)
		for (int i = 1; i <= 8; i++) {
			addVariantLayerNoInvis(context, "textures/entities/eyes_var" + i + ".png", HunterEntity.DATA_Eyes, i);
		}

		// Hair
		for (int i = 1; i <= 8; i++) {
			addVariantLayer(context, "textures/entities/mhair_var" + i + ".png", HunterEntity.DATA_Hair, i);
		}

		// Top Inner
		for (int i = 1; i <= 4; i++) {
			addVariantLayer(context, "textures/entities/top_in_var" + i + ".png", HunterEntity.DATA_TopIn, i);
		}

		// Top Outer
		for (int i = 1; i <= 15; i++) {
			addVariantLayer(context, "textures/entities/top_out_var" + i + ".png", HunterEntity.DATA_TopOut, i);
		}

		// Bottom
		for (int i = 1; i <= 5; i++) {
			addVariantLayer(context, "textures/entities/bottom_var" + i + ".png", HunterEntity.DATA_Bottom, i);
		}

		// Foot
		for (int i = 1; i <= 4; i++) {
			addVariantLayer(context, "textures/entities/foot_var" + i + ".png", HunterEntity.DATA_Foot, i);
		}

		// Mouth
		addVariantLayer(context, "textures/entities/mouth_var1.png", HunterEntity.DATA_Mouth, 1);

		// EyeBrows (mouth_var2 texture for value 2 was the original — kept as-is)
		addVariantLayer(context, "textures/entities/mouth_var2.png", HunterEntity.DATA_EyeBs, 2);

		// EyeBrow layers
		addVariantLayer(context, "textures/entities/eyeb_var1.png", HunterEntity.DATA_EyeBs, 1);
		addVariantLayer(context, "textures/entities/eyeb_var2.png", HunterEntity.DATA_EyeBs, 2);
	}

	@Override
	public ResourceLocation getTextureLocation(HunterEntity entity) {
		return new ResourceLocation("sololeveling:textures/entities/human_base.png");
	}
}
