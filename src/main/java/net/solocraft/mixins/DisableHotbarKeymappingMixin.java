package net.solocraft.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public abstract class DisableHotbarKeymappingMixin {
	@Shadow
	private int clickCount;

	@Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
	public void inject1(CallbackInfoReturnable<Boolean> cir) {
		String keyName = ((KeyMapping) (Object) this).getName();
		if (!keyName.startsWith("key.hotbar."))
			return;
		Entity entity = Minecraft.getInstance().player;
		if (entity != null && entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables()).combatmode) {
			this.clickCount = 0;
			cir.setReturnValue(false);
		}
	}
}
