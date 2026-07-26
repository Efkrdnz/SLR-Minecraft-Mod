package net.solocraft.mixins;

import net.solocraft.dkc.DkcFloorRegistry;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents renderer replacements from restoring the vanilla DKC sun/moon pass. */
@Mixin(value = LevelRenderer.class, priority = 1200)
public abstract class DkcSkyMixin {
	@Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
	private void sololeveling$suppressVanillaDkcSky(PoseStack poseStack, Matrix4f projectionMatrix,
			float partialTick, Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo callback) {
		ClientLevel level = Minecraft.getInstance().level;
		if (DkcFloorRegistry.isSharedDkc(level)) {
			setupFog.run();
			callback.cancel();
		}
	}

	@Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
	private void sololeveling$suppressVanillaDkcClouds(PoseStack poseStack, Matrix4f projectionMatrix,
			float partialTick, double cameraX, double cameraY, double cameraZ, CallbackInfo callback) {
		if (DkcFloorRegistry.isSharedDkc(Minecraft.getInstance().level))
			callback.cancel();
	}
}
