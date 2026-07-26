package net.solocraft.mixins;

import net.solocraft.client.highlight.ClientEntityHighlightManager;
import net.solocraft.client.renderer.shader.IrisCompat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class EntityHighlightVisibilityMixin {
	@Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
	private void sololeveling$showPlayerScopedHighlight(Entity entity,
			CallbackInfoReturnable<Boolean> callback) {
		if (ClientEntityHighlightManager.isHighlighted(entity) && !IrisCompat.isRenderingShadowPass())
			callback.setReturnValue(true);
	}
}
