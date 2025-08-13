package net.solocraft.mixins;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

@Mixin(KeyMapping.class)
public abstract class DisableHotbarKeymappingMixin {
	@Inject(method = "consumeClick", at = @At("HEAD"), cancellable = true)
	public void inject1(CallbackInfoReturnable<Boolean> cir) {
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.1")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.2")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.3")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.4")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.5")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.6")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.7")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.8")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
		if (((KeyMapping) (Object) this).getName().contains("key.hotbar.9")) {
			Entity entity = Minecraft.getInstance().player;
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).combatmode) {
				cir.setReturnValue(false);
			}
		}
	}
}
