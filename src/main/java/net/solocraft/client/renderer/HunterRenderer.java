
package net.solocraft.client.renderer;

import net.solocraft.procedures.ReturnHunterRandomTopOut9Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut8Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut7Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut6Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut5Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut4Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut3Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut2Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut1Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut15Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut14Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut13Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut12Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut11Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopOut10Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopIn4Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopIn3Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopIn2Procedure;
import net.solocraft.procedures.ReturnHunterRandomTopIn1Procedure;
import net.solocraft.procedures.ReturnHunterRandomMouth1Procedure;
import net.solocraft.procedures.ReturnHunterRandomHair8Procedure;
import net.solocraft.procedures.ReturnHunterRandomHair7Procedure;
import net.solocraft.procedures.ReturnHunterRandomHair6Procedure;
import net.solocraft.procedures.ReturnHunterRandomHair5Procedure;
import net.solocraft.procedures.ReturnHunterRandomHair4Procedure;
import net.solocraft.procedures.ReturnHunterRandomHair3Procedure;
import net.solocraft.procedures.ReturnHunterRandomHair2Procedure;
import net.solocraft.procedures.ReturnHunterRandomHair1Procedure;
import net.solocraft.procedures.ReturnHunterRandomFoot4Procedure;
import net.solocraft.procedures.ReturnHunterRandomFoot3Procedure;
import net.solocraft.procedures.ReturnHunterRandomFoot2Procedure;
import net.solocraft.procedures.ReturnHunterRandomFoot1Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyes8Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyes7Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyes6Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyes5Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyes4Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyes3Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyes2Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyes1Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyeBrows2Procedure;
import net.solocraft.procedures.ReturnHunterRandomEyeBrows1Procedure;
import net.solocraft.procedures.ReturnHunterRandomBottom5Procedure;
import net.solocraft.procedures.ReturnHunterRandomBottom4Procedure;
import net.solocraft.procedures.ReturnHunterRandomBottom3Procedure;
import net.solocraft.procedures.ReturnHunterRandomBottom2Procedure;
import net.solocraft.procedures.ReturnHunterRandomBottom1Procedure;
import net.solocraft.entity.HunterEntity;

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

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

public class HunterRenderer extends HumanoidMobRenderer<HunterEntity, HumanoidModel<HunterEntity>> {
	public HunterRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyes_var1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyes1Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyes_var2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyes2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyes_var3.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyes3Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyes_var4.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyes4Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyes_var5.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyes5Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyes_var6.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyes6Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyes_var7.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyes7Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyes_var8.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyes8Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mhair_var1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomHair1Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mhair_var2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomHair2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mhair_var3.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomHair3Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mhair_var4.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomHair4Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mhair_var5.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomHair5Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mhair_var6.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomHair6Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mhair_var7.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomHair7Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mhair_var8.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomHair8Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_in_var1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopIn1Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_in_var2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopIn2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_in_var3.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopIn3Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_in_var4.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopIn4Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut1Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var3.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut3Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var4.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut4Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var5.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut5Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var6.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut6Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var7.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut7Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var8.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut8Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var9.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut9Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var10.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut10Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var11.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut11Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var12.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut12Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var13.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut13Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var14.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut14Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/top_out_var15.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomTopOut15Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/bottom_var1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomBottom1Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/bottom_var2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomBottom2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/bottom_var3.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomBottom3Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/bottom_var4.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomBottom4Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/bottom_var5.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomBottom5Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/foot_var1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomFoot1Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/foot_var2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomFoot2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/foot_var3.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomFoot3Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/foot_var4.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomFoot4Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mouth_var1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomMouth1Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/mouth_var2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyeBrows2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyeb_var1.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyeBrows1Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
		this.addLayer(new RenderLayer<HunterEntity, HumanoidModel<HunterEntity>>(this) {
			final ResourceLocation LAYER_TEXTURE = new ResourceLocation("sololeveling:textures/entities/eyeb_var2.png");

			@Override
			public void render(PoseStack poseStack, MultiBufferSource bufferSource, int light, HunterEntity entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
				Level world = entity.level();
				double x = entity.getX();
				double y = entity.getY();
				double z = entity.getZ();
				if (ReturnHunterRandomEyeBrows2Procedure.execute(entity)) {
					VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(LAYER_TEXTURE));
					this.getParentModel().renderToBuffer(poseStack, vertexConsumer, 15728640, LivingEntityRenderer.getOverlayCoords(entity, 0), 1, 1, 1, 1);
				}
			}
		});
	}

	@Override
	public ResourceLocation getTextureLocation(HunterEntity entity) {
		return new ResourceLocation("sololeveling:textures/entities/human_base.png");
	}
}
