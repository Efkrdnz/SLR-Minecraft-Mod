package net.solocraft.entity.model;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.constant.DataTickets;

import net.solocraft.entity.TuskShadowEntity;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class TuskShadowModel extends GeoModel<TuskShadowEntity> {
	@Override
	public ResourceLocation getAnimationResource(TuskShadowEntity entity) {
		return new ResourceLocation("sololeveling", "animations/kardalgan_stand.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(TuskShadowEntity entity) {
		return new ResourceLocation("sololeveling", "geo/kardalgan_stand.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(TuskShadowEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(TuskShadowEntity animatable, long instanceId, AnimationState animationState) {
		CoreGeoBone head = getAnimationProcessor().getBone("tete");
		if (head != null) {
			EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
