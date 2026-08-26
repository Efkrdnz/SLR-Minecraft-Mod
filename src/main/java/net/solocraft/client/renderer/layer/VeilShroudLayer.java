package net.solocraft.client.renderer.layer;

import net.solocraft.client.renderer.shader.VeilShroudRenderTypes;
import net.solocraft.init.SololevelingModMobEffects;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Renders the Infiltrator concealment shell over a player's own model.
 *
 * <p>Layers still run for an invisible entity, which is what lets the vanilla
 * body stay hidden while this shell draws the silhouette. The owner sees a
 * fainter shell than observers do, so a concealed player can always locate
 * themselves in third person.</p>
 */
public class VeilShroudLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
	private static final ResourceLocation SHROUD_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("sololeveling",
					"textures/entity/veil_shroud.png");
	/** Owner sees a low-opacity shell; observers see a stronger silhouette. */
	private static final float OWNER_ALPHA = 0.35F;
	private static final float OBSERVER_ALPHA = 0.85F;

	public VeilShroudLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
		super(parent);
	}

	@Override
	public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
			AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick,
			float ageInTicks, float netHeadYaw, float headPitch) {
		if (player.isSpectator()
				|| !player.hasEffect(SololevelingModMobEffects.VEIL_SHROUD))
			return;

		boolean owner = Minecraft.getInstance().player != null
				&& Minecraft.getInstance().player.getUUID().equals(player.getUUID());
		float alpha = owner ? OWNER_ALPHA : OBSERVER_ALPHA;

		RenderType renderType = VeilShroudRenderTypes.shroud(SHROUD_TEXTURE);
		VertexConsumer vertices = bufferSource.getBuffer(renderType);
		// The shader reads the violet tint and the owner/observer split out of
		// vertexColor, so both travel in the packed vertex colour.
		int tint = FastColor.ARGB32.color(Math.round(alpha * 255.0F), 140, 107, 255);
		getParentModel().renderToBuffer(poseStack, vertices, LightTexture.FULL_BRIGHT,
				OverlayTexture.NO_OVERLAY, tint);
	}
}
