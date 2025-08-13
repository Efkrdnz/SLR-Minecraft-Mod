
package net.solocraft.client.renderer;

import net.solocraft.entity.TrainingBotEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.HumanoidModel;

public class TrainingBotRenderer extends HumanoidMobRenderer<TrainingBotEntity, HumanoidModel<TrainingBotEntity>> {
	public TrainingBotRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER)), 0.5f);
		this.addLayer(new HumanoidArmorLayer(this, new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)), new HumanoidModel(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
	}

	@Override
	public ResourceLocation getTextureLocation(TrainingBotEntity entity) {
		return new ResourceLocation("sololeveling:textures/entities/noob_saibot_bi_han_mortal_kombat_xl_villain_ninja_dark_black_shadow_mask_fighter_mk11.png");
	}
}
