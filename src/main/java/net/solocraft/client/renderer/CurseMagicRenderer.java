
package net.solocraft.client.renderer;

import net.solocraft.entity.CurseMagicEntity;
import net.solocraft.client.model.Modelinv;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class CurseMagicRenderer extends MobRenderer<CurseMagicEntity, Modelinv<CurseMagicEntity>> {
	public CurseMagicRenderer(EntityRendererProvider.Context context) {
		super(context, new Modelinv(context.bakeLayer(Modelinv.LAYER_LOCATION)), 0f);
	}

	@Override
	public ResourceLocation getTextureLocation(CurseMagicEntity entity) {
		return new ResourceLocation("sololeveling:textures/entities/invistext.png");
	}

	@Override
	protected boolean isBodyVisible(CurseMagicEntity entity) {
		return false;
	}

	@Override
	protected boolean isShaking(CurseMagicEntity entity) {
		return true;
	}
}
