package net.solocraft.client.renderer.layer;

import net.solocraft.client.model.Modelgoliathchest;
import net.solocraft.client.model.Modelgoliathhelm;
import net.solocraft.init.SololevelingModItems;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/** Full-bright mask pass for the custom Goliath spiritualization armor. */
public class GoliathArmorGlowLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation HELMET_GLOW = new ResourceLocation("sololeveling", "textures/models/armor/goliath_helm_glow.png");
	private static final ResourceLocation CHEST_GLOW = new ResourceLocation("sololeveling", "textures/models/armor/goliath_chest_glow.png");

	private final Modelgoliathhelm<AbstractClientPlayer> helmetModel;
	private final Modelgoliathchest<AbstractClientPlayer> chestModel;

	public GoliathArmorGlowLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
			EntityModelSet modelSet) {
		super(parent);
		this.helmetModel = new Modelgoliathhelm<>(modelSet.bakeLayer(Modelgoliathhelm.LAYER_LOCATION));
		this.chestModel = new Modelgoliathchest<>(modelSet.bakeLayer(Modelgoliathchest.LAYER_LOCATION));
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
		if (player.isInvisible())
			return;

		PlayerModel<AbstractClientPlayer> playerModel = getParentModel();
		if (player.getItemBySlot(EquipmentSlot.HEAD).is(SololevelingModItems.GOLIATH_ARMOR_HELMET.get())) {
			helmModelPose(playerModel);
			renderGlow(helmetModel, HELMET_GLOW, poseStack, bufferSource);
		}
		if (player.getItemBySlot(EquipmentSlot.CHEST).is(SololevelingModItems.GOLIATH_ARMOR_CHESTPLATE.get())) {
			chestModelPose(playerModel);
			renderGlow(chestModel, CHEST_GLOW, poseStack, bufferSource);
		}
	}

	private void helmModelPose(PlayerModel<AbstractClientPlayer> playerModel) {
		helmetModel.Head.copyFrom(playerModel.head);
	}

	private void chestModelPose(PlayerModel<AbstractClientPlayer> playerModel) {
		chestModel.Body.copyFrom(playerModel.body);
		chestModel.RightArm.copyFrom(playerModel.rightArm);
		chestModel.LeftArm.copyFrom(playerModel.leftArm);
	}

	private static void renderGlow(net.minecraft.client.model.EntityModel<AbstractClientPlayer> model,
			ResourceLocation texture, PoseStack poseStack, MultiBufferSource bufferSource) {
		VertexConsumer vertices = bufferSource.getBuffer(RenderType.eyes(texture));
		model.renderToBuffer(poseStack, vertices, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
				1.0F, 1.0F, 1.0F, 1.0F);
	}
}
