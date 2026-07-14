package net.solocraft.mixins;

import net.solocraft.util.BeastMonarchManager;
import net.solocraft.util.GoliathCombatManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemInHandRenderer.class)
public abstract class GoliathFirstPersonHandMixin {
	@ModifyVariable(method = "renderArmWithItem", at = @At("HEAD"), argsOnly = true)
	private ItemStack sololeveling$hideGoliathMainHand(ItemStack stack) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.player != null && (GoliathCombatManager.isCombatStance(minecraft.player)
				|| BeastMonarchManager.isFangStance(minecraft.player)) ? ItemStack.EMPTY : stack;
	}
}
