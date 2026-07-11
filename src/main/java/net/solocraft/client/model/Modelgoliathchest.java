package net.solocraft.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
public class Modelgoliathchest<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("sololeveling", "goliath_chest"), "main");
	public final ModelPart Body;
	public final ModelPart RightArm;
	public final ModelPart LeftArm;

	public Modelgoliathchest(ModelPart root) {
		this.Body = root.getChild("Body");
		this.RightArm = root.getChild("RightArm");
		this.LeftArm = root.getChild("LeftArm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(-1, 15).addBox(-5.0F, -1.0F, -3.0F, 10.0F, 13.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition cube_r1 = Body.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 51).addBox(-4.0F, -0.25F, -4.25F, 8.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.2618F, 0.0F, 0.0F));

		PartDefinition cube_r2 = Body.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(28, 15).addBox(-2.0F, -4.25F, -13.0F, 5.0F, 3.0F, 11.0F, new CubeDeformation(0.0F))
		.texOffs(0, 0).addBox(-2.0F, -1.25F, -13.5F, 5.0F, 2.0F, 13.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.5F, -0.5521F, 9.6382F, 1.0036F, 0.0F, 0.0F));

		PartDefinition cube_r3 = Body.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(60, 24).addBox(-7.5F, 2.75F, -3.0F, 5.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(60, 19).addBox(2.5F, 2.75F, -3.0F, 5.0F, 2.0F, 3.0F, new CubeDeformation(0.0F))
		.texOffs(50, 38).addBox(2.5F, -1.25F, -2.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(50, 29).addBox(-7.5F, -1.25F, -2.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.0F))
		.texOffs(36, 0).addBox(-3.0F, -2.25F, -2.0F, 6.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 1.5F, 4.0F, 0.3491F, 0.0F, 0.0F));

		PartDefinition RightArm = partdefinition.addOrReplaceChild("RightArm", CubeListBuilder.create().texOffs(28, 29).addBox(-4.0F, -2.0F, -3.0F, 6.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(48, 61).addBox(-5.0F, -3.0F, -1.0F, 5.0F, 5.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(58, 47).addBox(-5.0F, 8.0F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(20, 59).addBox(-7.0F, -8.0F, 0.0F, 7.0F, 9.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(0, 69).addBox(-8.0F, 4.0F, -1.0F, 9.0F, 12.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(0, 60).addBox(-6.0F, 7.0F, 1.0F, 7.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-5.0F, 2.0F, 0.0F));

		PartDefinition cube_r4 = RightArm.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(64, 0).addBox(-8.0F, -0.25F, -4.25F, 4.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(5.0F, -2.0F, 0.0F, 0.2618F, 0.0F, 0.0F));

		PartDefinition LeftArm = partdefinition.addOrReplaceChild("LeftArm", CubeListBuilder.create().texOffs(-2, 33).addBox(-3.0F, -2.0F, -3.0F, 7.0F, 12.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(60, 11).addBox(-1.0F, 7.0F, 1.0F, 7.0F, 8.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(40, 47).addBox(-1.0F, 4.0F, -1.0F, 9.0F, 12.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(34, 59).addBox(0.0F, -8.0F, 0.0F, 7.0F, 9.0F, 0.0F, new CubeDeformation(0.0F))
		.texOffs(58, 54).addBox(0.0F, 8.0F, -2.0F, 5.0F, 3.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(62, 61).addBox(0.0F, -3.0F, -1.0F, 5.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(5.0F, 2.0F, 0.0F));

		PartDefinition cube_r5 = LeftArm.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(63, 5).addBox(4.0F, -0.25F, -4.25F, 5.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-5.0F, -2.0F, 0.0F, 0.2618F, 0.0F, 0.0F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		Body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		RightArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
		LeftArm.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
