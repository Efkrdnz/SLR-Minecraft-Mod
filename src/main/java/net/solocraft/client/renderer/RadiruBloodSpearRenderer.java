package net.solocraft.client.renderer;

import net.solocraft.entity.RadiruBloodSpearEntity;
import net.solocraft.init.SololevelingModItems;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public class RadiruBloodSpearRenderer extends EntityRenderer<RadiruBloodSpearEntity> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("sololeveling", "textures/item/radiru_blood_spear.png");
	private final net.minecraft.client.renderer.entity.ItemRenderer itemRenderer;

	public RadiruBloodSpearRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.itemRenderer = context.getItemRenderer();
		this.shadowRadius = 0.0F;
	}

	@Override
	public void render(RadiruBloodSpearEntity entity, float yaw, float partialTick, PoseStack stack,
			MultiBufferSource buffers, int packedLight) {
		stack.pushPose();
		stack.mulPose(Axis.YP.rotationDegrees(-entity.getYRot() + 90.0F));
		stack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot() - 90.0F));
		stack.mulPose(Axis.YP.rotationDegrees((entity.tickCount + partialTick) * 28.0F));
		float scale = entity.isManifested() ? 1.18F : 1.0F;
		stack.scale(scale, scale, scale);
		this.itemRenderer.renderStatic(new ItemStack(SololevelingModItems.RADIRU_BLOOD_SPEAR.get()),
				ItemDisplayContext.FIXED, 240, OverlayTexture.NO_OVERLAY, stack, buffers,
				entity.level(), entity.getId());
		stack.popPose();
		super.render(entity, yaw, partialTick, stack, buffers, packedLight);
	}

	@Override
	public ResourceLocation getTextureLocation(RadiruBloodSpearEntity entity) {
		return TEXTURE;
	}
}
