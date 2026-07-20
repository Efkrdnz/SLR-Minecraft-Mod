package net.solocraft.mixins;

import net.solocraft.SololevelingMod;
import net.solocraft.network.BeastCombatMessage;
import net.solocraft.network.GoliathCombatMessage;
import net.solocraft.util.BeastMonarchManager;
import net.solocraft.util.GoliathCombatManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Minecraft.class, priority = 1200)
public abstract class VesselCombatAttackMixin {
	@Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
	private void sololeveling$useVesselMelee(CallbackInfoReturnable<Boolean> callback) {
		Minecraft minecraft = (Minecraft) (Object) this;
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null)
			return;
		if (GoliathCombatManager.isCombatStance(player)) {
			SololevelingMod.PACKET_HANDLER.sendToServer(new GoliathCombatMessage());
			callback.setReturnValue(false);
		} else if (BeastMonarchManager.isFangStance(player)) {
			SololevelingMod.PACKET_HANDLER.sendToServer(new BeastCombatMessage());
			callback.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
	private void sololeveling$stopWeaponHoldAttack(boolean attackHeld, CallbackInfo callback) {
		Minecraft minecraft = (Minecraft) (Object) this;
		LocalPlayer player = minecraft.player;
		if (attackHeld && player != null && (GoliathCombatManager.isCombatStance(player)
				|| BeastMonarchManager.isFangStance(player)))
			callback.cancel();
	}
}
