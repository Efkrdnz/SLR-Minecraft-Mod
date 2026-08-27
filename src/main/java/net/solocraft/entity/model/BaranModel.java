package net.solocraft.entity.model;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;

import net.solocraft.entity.BaranEntity;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class BaranModel extends GeoModel<BaranEntity> {
	@Override
	public ResourceLocation getAnimationResource(BaranEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/baran.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BaranEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/baran.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BaranEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(BaranEntity animatable, long instanceId, AnimationState<BaranEntity> animationState) {
		GeoBone head = getAnimationProcessor().getBone("head");
		if (head != null) {
			EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
