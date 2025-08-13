// Made with Blockbench 4.9.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class Modellight_ball<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "light_ball"), "main");
	private final ModelPart ball;

	public Modellight_ball(ModelPart root) {
		this.ball = root.getChild("ball");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition ball = partdefinition.addOrReplaceChild("ball", CubeListBuilder.create().texOffs(36, 15)
				.addBox(-8.25F, -3.75F, -3.75F, 14.25F, 7.5F, 7.5F, new CubeDeformation(0.0F)).texOffs(0, 8)
				.addBox(-6.75F, -5.25F, -5.25F, 12.75F, 10.5F, 10.5F, new CubeDeformation(0.0F)).texOffs(36, 24)
				.addBox(-7.5F, -4.5F, -4.5F, 13.5F, 9.0F, 9.0F, new CubeDeformation(0.0F)).texOffs(4, 0).mirror()
				.addBox(-6.0F, -6.0F, -6.0F, 12.0F, 12.0F, 12.0F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(8, 16).addBox(-5.25F, -6.75F, -5.25F, 10.5F, 12.75F, 10.5F, new CubeDeformation(0.0F))
				.texOffs(20, 37).addBox(-4.5F, -7.5F, -4.5F, 9.0F, 13.5F, 9.0F, new CubeDeformation(0.0F))
				.texOffs(28, 37).addBox(-3.75F, -8.25F, -3.75F, 7.5F, 14.25F, 7.5F, new CubeDeformation(0.0F))
				.texOffs(6, 13).addBox(-4.5F, -4.5F, -7.5F, 9.0F, 9.0F, 13.5F, new CubeDeformation(0.0F))
				.texOffs(16, 16).addBox(-5.25F, -5.25F, -6.75F, 10.5F, 10.5F, 12.75F, new CubeDeformation(0.0F))
				.texOffs(16, 23).mirror().addBox(-3.75F, -3.75F, -8.25F, 7.5F, 7.5F, 14.25F, new CubeDeformation(0.0F))
				.mirror(false).texOffs(11, 28).mirror()
				.addBox(-4.5F, -4.5F, -6.0F, 9.0F, 9.0F, 13.5F, new CubeDeformation(0.0F)).mirror(false).texOffs(0, 4)
				.addBox(-5.25F, -5.25F, -6.0F, 10.5F, 10.5F, 12.75F, new CubeDeformation(0.0F)).texOffs(16, 32)
				.addBox(-3.75F, -3.75F, -6.0F, 7.5F, 7.5F, 14.25F, new CubeDeformation(0.0F)).texOffs(0, 38)
				.addBox(-4.5F, -6.0F, -4.5F, 9.0F, 13.5F, 9.0F, new CubeDeformation(0.0F)).texOffs(12, 16)
				.addBox(-5.25F, -6.0F, -5.25F, 10.5F, 12.75F, 10.5F, new CubeDeformation(0.0F)).texOffs(30, 8)
				.addBox(-3.75F, -6.0F, -3.75F, 7.5F, 14.25F, 7.5F, new CubeDeformation(0.0F)).texOffs(39, 26)
				.addBox(-6.0F, -4.5F, -4.5F, 13.5F, 9.0F, 9.0F, new CubeDeformation(0.0F)).texOffs(24, 24)
				.addBox(-6.0F, -5.25F, -5.25F, 12.75F, 10.5F, 10.5F, new CubeDeformation(0.0F)).texOffs(39, 14)
				.addBox(-6.0F, -3.75F, -3.75F, 14.25F, 7.5F, 7.5F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 15.75F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		ball.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}