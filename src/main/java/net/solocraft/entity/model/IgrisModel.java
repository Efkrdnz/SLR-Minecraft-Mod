package net.solocraft.entity.model;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.constant.DataTickets;

import net.solocraft.entity.IgrisEntity;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class IgrisModel extends GeoModel<IgrisEntity> {
	@Override
	public ResourceLocation getAnimationResource(IgrisEntity entity) {
		return new ResourceLocation("sololeveling", "animations/igris_prev.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(IgrisEntity entity) {
		return new ResourceLocation("sololeveling", "geo/igris_prev.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(IgrisEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(IgrisEntity animatable, long instanceId, AnimationState animationState) {
		CoreGeoBone head = getAnimationProcessor().getBone("");
		if (head != null) {
			EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
