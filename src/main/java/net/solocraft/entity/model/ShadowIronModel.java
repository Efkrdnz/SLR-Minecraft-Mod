package net.solocraft.entity.model;

import net.solocraft.entity.ShadowIronEntity;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class ShadowIronModel extends GeoModel<ShadowIronEntity> {
	private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
			"sololeveling", "geo/shadow_iron.geo.json");
	private static final ResourceLocation ANIMATIONS = ResourceLocation.fromNamespaceAndPath(
			"sololeveling", "animations/shadow_iron.animation.json");
	private static final ResourceLocation NORMAL = ResourceLocation.fromNamespaceAndPath(
			"sololeveling", "textures/entities/iron_shadow.png");
	private static final ResourceLocation DOMAIN = ResourceLocation.fromNamespaceAndPath(
			"sololeveling", "textures/entities/iron_shadow_domain.png");

	@Override
	public ResourceLocation getModelResource(ShadowIronEntity entity) {
		return MODEL;
	}

	@Override
	public ResourceLocation getTextureResource(ShadowIronEntity entity) {
		return entity.isDomainBoosted() ? DOMAIN : NORMAL;
	}

	@Override
	public ResourceLocation getAnimationResource(ShadowIronEntity entity) {
		return ANIMATIONS;
	}

	@Override
	public void setCustomAnimations(ShadowIronEntity entity, long instanceId,
			AnimationState<ShadowIronEntity> state) {
		if (entity.isActing())
			return;
		GeoBone head = getAnimationProcessor().getBone("bone34");
		if (head == null)
			return;
		EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
		head.setRotX(data.headPitch() * Mth.DEG_TO_RAD);
		head.setRotY(data.netHeadYaw() * Mth.DEG_TO_RAD);
	}
}
