package net.solocraft.client.renderer;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.SilladBossEntity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Vanilla-proportioned placeholder renderer for the developer-preview boss. */
public final class SilladBossRenderer extends HumanoidMobRenderer<SilladBossEntity,
		SilladBossRenderer.SilladHumanoidModel> {
	private static final ResourceLocation PLACEHOLDER_TEXTURE =
			ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
					"textures/entities/sillad_boss_placeholder.png");

	public SilladBossRenderer(EntityRendererProvider.Context context) {
		super(context, new SilladHumanoidModel(
				context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
		addLayer(new HumanoidArmorLayer<>(this,
				new HumanoidModel<>(context.bakeLayer(
						ModelLayers.PLAYER_INNER_ARMOR)),
				new HumanoidModel<>(context.bakeLayer(
						ModelLayers.PLAYER_OUTER_ARMOR)),
				context.getModelManager()));
		addLayer(new ItemInHandLayer<>(this,
				context.getItemInHandRenderer()));
	}

	@Override
	public ResourceLocation getTextureLocation(SilladBossEntity entity) {
		return PLACEHOLDER_TEXTURE;
	}

	/**
	 * Keeps vanilla locomotion and head tracking, then layers simple semantic
	 * combat poses over them so the placeholder remains readable before Sillad's
	 * final model and animation set exist.
	 */
	static final class SilladHumanoidModel
			extends HumanoidModel<SilladBossEntity> {
		private SilladHumanoidModel(ModelPart root) {
			super(root);
		}

		@Override
		public void setupAnim(SilladBossEntity entity, float limbSwing,
				float limbSwingAmount, float ageInTicks, float netHeadYaw,
				float headPitch) {
			super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks,
					netHeadYaw, headPitch);
			body.xRot = 0.0F;
			SilladBossEntity.Action action = entity.getCombatAction();
			if (action == SilladBossEntity.Action.IDLE)
				return;

			float partialTick = Mth.clamp(ageInTicks - entity.tickCount,
					0.0F, 1.0F);
			float actionTime = entity.getActionTick() + partialTick;
			float pulse = Mth.sin(actionTime * 0.22F) * 0.08F;
			float authority = entity.isSpiritualized() ? 0.12F : 0.0F;

			switch (action) {
				case FROST_CLEAVE -> poseCleave(actionTime);
				case ICE_SPEAR, GLACIAL_EXECUTION -> poseIceSpear(actionTime);
				case FROST_COUNTER -> poseCounter(pulse);
				case PHASE_TRANSITION, ABSOLUTE_ZERO ->
						poseSovereignCast(pulse, authority);
				case FROZEN_PATH, FROST_STEP -> poseFrostMovement(pulse);
				case FLASH_FREEZE, STILLNESS_DECREE, SPIRE_CAGE,
						WHITEOUT_PROCESSION, WINTER_REMEMBERS,
						CROWN_OF_WINTER -> poseForwardCast(pulse, authority);
				case IDLE -> {
				}
			}
		}

		private void poseCleave(float actionTime) {
			float sweep = Mth.sin(Mth.clamp(actionTime / 12.0F, 0.0F, 1.0F)
					* Mth.PI);
			body.yRot = -0.32F + sweep * 0.64F;
			rightArm.xRot = -1.75F + sweep * 0.55F;
			rightArm.yRot = -0.65F + sweep * 1.25F;
			rightArm.zRot = -0.18F;
			leftArm.xRot = -0.55F;
			leftArm.yRot = 0.28F;
		}

		private void poseIceSpear(float actionTime) {
			float draw = Mth.clamp(actionTime / 10.0F, 0.0F, 1.0F);
			rightArm.xRot = -1.35F - draw * 0.55F;
			rightArm.yRot = -0.38F;
			rightArm.zRot = 0.10F;
			leftArm.xRot = -0.78F;
			leftArm.yRot = 0.42F;
			leftArm.zRot = -0.18F;
		}

		private void poseCounter(float pulse) {
			rightArm.xRot = -1.22F + pulse;
			rightArm.yRot = -0.82F;
			rightArm.zRot = -0.18F;
			leftArm.xRot = -1.22F - pulse;
			leftArm.yRot = 0.82F;
			leftArm.zRot = 0.18F;
		}

		private void poseSovereignCast(float pulse, float authority) {
			rightArm.xRot = -2.68F - authority + pulse;
			rightArm.yRot = -0.22F;
			rightArm.zRot = 0.20F;
			leftArm.xRot = -2.68F - authority - pulse;
			leftArm.yRot = 0.22F;
			leftArm.zRot = -0.20F;
			head.xRot -= 0.12F;
		}

		private void poseFrostMovement(float pulse) {
			body.xRot = 0.18F;
			rightArm.xRot = 0.72F + pulse;
			rightArm.yRot = -0.12F;
			rightArm.zRot = 0.12F;
			leftArm.xRot = 0.72F - pulse;
			leftArm.yRot = 0.12F;
			leftArm.zRot = -0.12F;
		}

		private void poseForwardCast(float pulse, float authority) {
			rightArm.xRot = -1.42F - authority + pulse;
			rightArm.yRot = -0.28F;
			rightArm.zRot = 0.08F;
			leftArm.xRot = -1.42F - authority - pulse;
			leftArm.yRot = 0.28F;
			leftArm.zRot = -0.08F;
		}
	}
}
