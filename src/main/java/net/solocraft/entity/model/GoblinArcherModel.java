package net.solocraft.entity.model;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;

import net.solocraft.entity.GoblinArcherEntity;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class GoblinArcherModel extends GeoModel<GoblinArcherEntity> {
	@Override
	public ResourceLocation getAnimationResource(GoblinArcherEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "animations/goblin_archer.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GoblinArcherEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "geo/goblin_archer.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GoblinArcherEntity entity) {
		return ResourceLocation.fromNamespaceAndPath("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(GoblinArcherEntity animatable, long instanceId, AnimationState<GoblinArcherEntity> animationState) {
		GeoBone head = getAnimationProcessor().getBone("h_head");
		if (head != null) {
			EntityModelData entityData = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
