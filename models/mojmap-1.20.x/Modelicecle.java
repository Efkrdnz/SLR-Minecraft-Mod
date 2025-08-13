// Made with Blockbench 4.9.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modelicecle<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "icecle"), "main");
	private final ModelPart main;

	public Modelicecle(ModelPart root) {
		this.main = root.getChild("main");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition main = partdefinition.addOrReplaceChild("main",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F, new CubeDeformation(0.0F)).texOffs(0, 18)
						.addBox(-3.0F, -18.0F, -3.0F, 6.0F, 8.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(36, 36)
						.addBox(-2.0F, -26.0F, -2.0F, 4.0F, 8.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 24.0F, 0.0F));

		PartDefinition cube_r1 = main.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(24, 18).mirror()
				.addBox(-4.0F, -6.25F, -2.0F, 1.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false).texOffs(0, 32)
				.mirror().addBox(-0.75F, -9.0F, -1.0F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(1.1967F, -15.3337F, 3.2154F, -1.4491F, 1.145F, -1.3903F));

		PartDefinition cube_r2 = main.addOrReplaceChild("cube_r2",
				CubeListBuilder.create().texOffs(32, 20).mirror()
						.addBox(0.0F, -10.0F, 4.25F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-5.4993F, -8.4186F, 4.064F, -0.9173F, 0.8754F, -0.9509F));

		PartDefinition cube_r3 = main.addOrReplaceChild("cube_r3",
				CubeListBuilder.create().texOffs(48, 0).mirror()
						.addBox(-1.0F, -8.0F, -1.0F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(48, 0).addBox(-5.0F, -8.0F, -1.0F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.0F, -6.627F, -5.56F, 0.7854F, 0.0F, 0.0F));

		PartDefinition cube_r4 = main.addOrReplaceChild("cube_r4",
				CubeListBuilder.create().texOffs(48, 18).mirror()
						.addBox(-2.5F, -6.5F, -1.75F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(32, 48).mirror()
						.addBox(-1.0F, -8.5F, -0.5F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(48, 18).addBox(-1.5F, -6.5F, -1.75F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F))
						.texOffs(32, 48).addBox(-3.0F, -8.5F, -0.5F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.0F, -10.75F, -5.0F, 0.48F, 0.0F, 0.0F));

		PartDefinition cube_r5 = main.addOrReplaceChild("cube_r5",
				CubeListBuilder.create().texOffs(44, 48).mirror()
						.addBox(0.0F, -12.75F, 4.25F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(44, 48).addBox(-3.0F, -12.75F, 4.25F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.0F, -10.75F, -5.0F, 0.3927F, 0.0F, 0.0F));

		PartDefinition cube_r6 = main.addOrReplaceChild("cube_r6",
				CubeListBuilder.create().texOffs(0, 52).mirror()
						.addBox(-0.5F, -8.0F, -4.0F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(4.621F, -21.2746F, 1.7059F, -1.7861F, 1.1978F, -1.4356F));

		PartDefinition cube_r7 = main.addOrReplaceChild("cube_r7",
				CubeListBuilder.create().texOffs(4, 52).mirror()
						.addBox(-3.0F, -11.25F, -0.5F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(8, 52).mirror()
						.addBox(-0.5F, -8.0F, -0.5F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(1.4673F, -18.862F, -0.5405F, 0.0275F, -0.0529F, 0.3325F));

		PartDefinition cube_r8 = main.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(40, 48).mirror()
				.addBox(1.25F, -5.25F, 0.5F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false).texOffs(16, 32)
				.mirror().addBox(2.25F, -7.25F, 2.75F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(2.25F, -16.9562F, -2.4293F, 0.0179F, -0.0569F, 0.5073F));

		PartDefinition cube_r9 = main.addOrReplaceChild("cube_r9",
				CubeListBuilder.create().texOffs(32, 0).mirror()
						.addBox(-1.0F, -10.0F, 1.5F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(5.4993F, -8.4186F, 4.064F, -0.6803F, -0.6289F, 0.6084F));

		PartDefinition cube_r10 = main.addOrReplaceChild("cube_r10",
				CubeListBuilder.create().texOffs(8, 32).mirror()
						.addBox(-1.0F, -10.25F, -1.0F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(5.1897F, -7.6524F, 0.5992F, -0.129F, 0.1927F, 0.4909F));

		PartDefinition cube_r11 = main.addOrReplaceChild("cube_r11",
				CubeListBuilder.create().texOffs(1, 1).addBox(-0.5F, -8.0F, -0.5F, 1.0F, 16.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.3269F, -22.4254F, -1.7716F, 1.78F, 1.1866F, 2.1013F));

		PartDefinition cube_r12 = main.addOrReplaceChild("cube_r12",
				CubeListBuilder.create().texOffs(32, 0).addBox(-1.0F, -10.0F, 1.5F, 2.0F, 18.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.4993F, -8.4186F, 4.064F, -0.6803F, 0.6289F, -0.6084F));

		PartDefinition cube_r13 = main.addOrReplaceChild("cube_r13",
				CubeListBuilder.create().texOffs(8, 32).addBox(-1.0F, -10.25F, -1.0F, 2.0F, 18.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-5.1897F, -7.6524F, 0.5992F, -0.129F, -0.1927F, -0.4909F));

		PartDefinition cube_r14 = main.addOrReplaceChild("cube_r14",
				CubeListBuilder.create().texOffs(16, 32)
						.addBox(-4.25F, -7.25F, 2.75F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(40, 48)
						.addBox(-2.25F, -5.25F, 0.5F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-2.25F, -16.9562F, -2.4293F, 0.0179F, 0.0569F, -0.5073F));

		PartDefinition cube_r15 = main.addOrReplaceChild("cube_r15",
				CubeListBuilder.create().texOffs(1, 1).mirror()
						.addBox(-0.5F, -8.0F, -0.5F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-1.3269F, -22.4254F, -1.7716F, 1.78F, -1.1866F, -2.1013F));

		PartDefinition cube_r16 = main.addOrReplaceChild("cube_r16",
				CubeListBuilder.create().texOffs(0, 52).addBox(-0.5F, -8.0F, -4.0F, 1.0F, 16.0F, 1.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.621F, -21.2746F, 1.7059F, -1.7861F, -1.1978F, 1.4356F));

		PartDefinition cube_r17 = main.addOrReplaceChild("cube_r17",
				CubeListBuilder.create().texOffs(4, 52)
						.addBox(2.0F, -11.25F, -0.5F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)).texOffs(8, 52)
						.addBox(-0.5F, -8.0F, -0.5F, 1.0F, 16.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.4673F, -18.862F, -0.5405F, 0.0275F, 0.0529F, -0.3325F));

		PartDefinition cube_r18 = main.addOrReplaceChild("cube_r18",
				CubeListBuilder.create().texOffs(0, 0).mirror()
						.addBox(-1.0F, -8.0F, -1.0F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-2.349F, -17.6108F, -2.762F, 0.2405F, 0.015F, -0.3832F));

		PartDefinition cube_r19 = main.addOrReplaceChild("cube_r19", CubeListBuilder.create().texOffs(0, 0).mirror()
				.addBox(-0.5F, -8.0F, -2.0F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false).texOffs(0, 0)
				.mirror().addBox(-4.25F, -2.25F, -4.25F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-6.1419F, -15.0483F, -3.6895F, 0.3233F, 0.0569F, -0.5073F));

		PartDefinition cube_r20 = main.addOrReplaceChild("cube_r20",
				CubeListBuilder.create().texOffs(0, 0).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 16.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(2.349F, -17.6108F, -2.762F, 0.2405F, -0.015F, 0.3832F));

		PartDefinition cube_r21 = main.addOrReplaceChild("cube_r21",
				CubeListBuilder.create().texOffs(0, 0)
						.addBox(2.25F, -2.25F, -4.25F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(0, 0)
						.addBox(-1.5F, -8.0F, -2.0F, 2.0F, 16.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(6.1419F, -15.0483F, -3.6895F, 0.3233F, -0.0569F, 0.5073F));

		PartDefinition cube_r22 = main.addOrReplaceChild("cube_r22",
				CubeListBuilder.create().texOffs(24, 18)
						.addBox(3.0F, -6.25F, -2.0F, 1.0F, 18.0F, 3.0F, new CubeDeformation(0.0F)).texOffs(0, 32)
						.addBox(-1.25F, -9.0F, -1.0F, 2.0F, 18.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.1967F, -15.3337F, 3.2154F, -1.4491F, -1.145F, 1.3903F));

		PartDefinition cube_r23 = main.addOrReplaceChild("cube_r23",
				CubeListBuilder.create().texOffs(32, 20).addBox(-2.0F, -10.0F, 4.25F, 2.0F, 18.0F, 2.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(5.4993F, -8.4186F, 4.064F, -0.9173F, -0.8754F, 0.9509F));

		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		main.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}