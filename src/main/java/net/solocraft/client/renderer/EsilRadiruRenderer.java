package net.solocraft.client.renderer;

import net.solocraft.entity.EsilRadiruEntity;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renders the supplied 64 x 64 slim-player skin, including its outer layer. */
public final class EsilRadiruRenderer extends HumanoidMobRenderer<EsilRadiruEntity, PlayerModel<EsilRadiruEntity>> {
	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
			"sololeveling", "textures/entities/esil_radiru.png");

	public EsilRadiruRenderer(EntityRendererProvider.Context context) {
		super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
	}

	@Override
	public ResourceLocation getTextureLocation(EsilRadiruEntity entity) {
		return TEXTURE;
	}
}
