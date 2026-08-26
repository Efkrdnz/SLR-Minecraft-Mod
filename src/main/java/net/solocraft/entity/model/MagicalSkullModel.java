package net.solocraft.entity.model;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;

import net.solocraft.entity.MagicalSkullEntity;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class MagicalSkullModel extends GeoModel<MagicalSkullEntity> {
	@Override
	public ResourceLocation getAnimationResource(MagicalSkullEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/skeleton_skull.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(MagicalSkullEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/skeleton_skull.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(MagicalSkullEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(MagicalSkullEntity animatable, long instanceId, AnimationState<MagicalSkullEntity> animationState) {
		GeoBone head = getAnimationProcessor().getBone("body");
		if (head != null) {
			EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
