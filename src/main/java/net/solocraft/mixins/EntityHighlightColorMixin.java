package net.solocraft.mixins;

import com.mojang.blaze3d.vertex.PoseStack;

import net.solocraft.client.highlight.ClientEntityHighlightManager;
import net.solocraft.client.renderer.shader.IrisCompat;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.OptionalInt;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityHighlightColorMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private <E extends Entity> void sololeveling$usePlayerScopedHighlightColor(E entity,
			double x, double y, double z, float yaw, float partialTick, PoseStack poseStack,
			MultiBufferSource buffers, int packedLight, CallbackInfo callback) {
		if (!(buffers instanceof OutlineBufferSource outlines))
			return;
		OptionalInt customColor = ClientEntityHighlightManager.colorFor(entity);
		if (customColor.isEmpty() || IrisCompat.isRenderingShadowPass())
			return;
		int color = customColor.getAsInt();
		outlines.setColor(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, 0xFF);
	}
}
