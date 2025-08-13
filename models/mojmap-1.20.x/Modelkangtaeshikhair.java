// Made with Blockbench 4.9.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modelkangtaeshikhair<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "kangtaeshikhair"), "main");
	private final ModelPart Head;

	public Modelkangtaeshikhair(ModelPart root) {
		this.Head = root.getChild("Head");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition Head = partdefinition.addOrReplaceChild("Head", CubeListBuilder.create(),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		PartDefinition Head_r1 = Head.addOrReplaceChild("Head_r1",
				CubeListBuilder.create().texOffs(0, 124).addBox(-3.25F, 0.25F, -2.75F, 6.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.75F, -8.25F, -0.25F, 0.0F, 0.0F, -1.0036F));

		PartDefinition Head_r2 = Head.addOrReplaceChild("Head_r2",
				CubeListBuilder.create().texOffs(0, 124).addBox(-3.25F, 0.25F, -2.75F, 6.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.75F, -9.5F, 2.75F, 0.0F, 0.0F, -0.6545F));

		PartDefinition Head_r3 = Head.addOrReplaceChild("Head_r3",
				CubeListBuilder.create().texOffs(0, 124).addBox(-3.25F, 0.25F, -2.75F, 6.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.75F, -9.5F, 1.25F, 0.0F, 0.0F, 0.6981F));

		PartDefinition Head_r4 = Head.addOrReplaceChild("Head_r4",
				CubeListBuilder.create().texOffs(0, 124)
						.addBox(-3.25F, 0.25F, -2.75F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 124)
						.addBox(-3.25F, 0.25F, 0.75F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(4.0F, -8.75F, 0.0F, 0.0F, 0.0F, 0.6545F));

		PartDefinition Head_r5 = Head.addOrReplaceChild("Head_r5",
				CubeListBuilder.create().texOffs(0, 124).addBox(-3.25F, 0.25F, 0.75F, 6.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.5F, -8.5F, 3.5F, 2.168F, -0.7057F, -1.4798F));

		PartDefinition Head_r6 = Head.addOrReplaceChild("Head_r6",
				CubeListBuilder.create().texOffs(0, 124).addBox(-4.25F, 0.25F, 0.75F, 8.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, -8.0F, 3.5F, -0.8625F, -0.6431F, 1.4843F));

		PartDefinition Head_r7 = Head.addOrReplaceChild("Head_r7",
				CubeListBuilder.create().texOffs(0, 124).addBox(-3.25F, 0.25F, 0.75F, 6.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(4.0F, -8.0F, 3.5F, -0.818F, -0.947F, 1.4521F));

		PartDefinition Head_r8 = Head.addOrReplaceChild("Head_r8",
				CubeListBuilder.create().texOffs(0, 124)
						.addBox(-3.25F, -2.0F, -3.25F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 124)
						.addBox(-3.25F, -2.0F, -8.5F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, -8.75F, 5.25F, 0.0F, 0.0F, -1.7453F));

		PartDefinition Head_r9 = Head.addOrReplaceChild("Head_r9",
				CubeListBuilder.create().texOffs(0, 124).addBox(-2.75F, -1.5F, -4.5F, 6.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.5F, -8.75F, 0.0F, 0.0F, 0.0F, -1.5708F));

		PartDefinition Head_r10 = Head.addOrReplaceChild("Head_r10",
				CubeListBuilder.create().texOffs(0, 124)
						.addBox(-3.25F, -2.0F, -3.25F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 124)
						.addBox(-3.25F, -2.0F, -6.0F, 6.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.75F, -8.75F, 2.75F, 0.0F, 0.0F, -1.3526F));

		PartDefinition Head_r11 = Head.addOrReplaceChild("Head_r11",
				CubeListBuilder.create().texOffs(0, 124).addBox(-3.25F, -2.0F, -3.25F, 5.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(6.0F, -8.75F, 4.75F, 0.0F, 0.0F, -0.9599F));

		PartDefinition Head_r12 = Head.addOrReplaceChild("Head_r12",
				CubeListBuilder.create().texOffs(0, 124)
						.addBox(-4.0F, -3.0F, -4.75F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 124)
						.addBox(-3.25F, -2.0F, -3.25F, 5.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(4.0F, -8.75F, 0.0F, 0.0F, 0.0F, -0.9599F));

		PartDefinition Head_r13 = Head.addOrReplaceChild("Head_r13",
				CubeListBuilder.create().texOffs(0, 124).addBox(-3.0F, -1.0F, -1.0F, 6.0F, 2.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(4.0F, -8.75F, 0.0F, 0.0F, 0.0F, -0.5672F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		Head.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
		this.Head.yRot = netHeadYaw / (180F / (float) Math.PI);
		this.Head.xRot = headPitch / (180F / (float) Math.PI);
	}
}