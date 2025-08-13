// Made with Blockbench 4.9.4
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports

public class ModelSlash5<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			new ResourceLocation("modid", "slash5"), "main");
	private final ModelPart base;

	public ModelSlash5(ModelPart root) {
		this.base = root.getChild("base");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition base = partdefinition.addOrReplaceChild("base", CubeListBuilder.create(),
				PartPose.offsetAndRotation(-1.2678F, 7.2635F, 0.5625F, 1.5708F, 0.7854F, 0.0F));

		PartDefinition cube_r1 = base.addOrReplaceChild("cube_r1",
				CubeListBuilder.create().texOffs(-1, -1).addBox(-13.5F, -0.0313F, -7.5F, 22.0F, 0.0625F, 15.125F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.7678F, 1.7678F, -0.0625F, 0.0F, 0.0F, -0.3927F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			float red, float green, float blue, float alpha) {
		base.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}