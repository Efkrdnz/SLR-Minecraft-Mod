package net.solocraft.mixins;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SungIlHwanAttackMessage;
import net.solocraft.util.SungIlHwanCombatManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla/empty-swing request path for Assassin Stance.
 *
 * <p>The deliberately lower priority lets combat animation mods such as Better
 * Combat claim {@code startAttack} first. If they do, their attack-frame event
 * sends the request instead. If they decline the weapon, this observer sends a
 * target-free request and leaves vanilla melee/animation intact.</p>
 */
@Mixin(value = Minecraft.class, priority = 900)
public abstract class SungIlHwanAttackMixin {
	@Inject(method = "startAttack", at = @At("HEAD"))
	private void sololeveling$requestAssassinLineCut(
			CallbackInfoReturnable<Boolean> callback) {
		Minecraft minecraft = (Minecraft) (Object) this;
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null
				|| !SungIlHwanCombatManager.shouldReplaceBasicAttack(player))
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(
				new SungIlHwanAttackMessage());
	}
}
