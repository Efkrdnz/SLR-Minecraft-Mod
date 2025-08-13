package net.solocraft.entity.model;

import software.bernie.geckolib.model.data.EntityModelData;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.constant.DataTickets;

import net.solocraft.entity.GoblinClubEntity;

import net.minecraft.util.Mth;
import net.minecraft.resources.ResourceLocation;

public class GoblinClubModel extends GeoModel<GoblinClubEntity> {
	@Override
	public ResourceLocation getAnimationResource(GoblinClubEntity entity) {
		return new ResourceLocation("sololeveling", "animations/goblin_club.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(GoblinClubEntity entity) {
		return new ResourceLocation("sololeveling", "geo/goblin_club.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(GoblinClubEntity entity) {
		return new ResourceLocation("sololeveling", "textures/entities/" + entity.getTexture() + ".png");
	}

	@Override
	public void setCustomAnimations(GoblinClubEntity animatable, long instanceId, AnimationState animationState) {
		CoreGeoBone head = getAnimationProcessor().getBone("h_head");
		if (head != null) {
			EntityModelData entityData = (EntityModelData) animationState.getData(DataTickets.ENTITY_MODEL_DATA);
			head.setRotX(entityData.headPitch() * Mth.DEG_TO_RAD);
			head.setRotY(entityData.netHeadYaw() * Mth.DEG_TO_RAD);
		}

	}
}
