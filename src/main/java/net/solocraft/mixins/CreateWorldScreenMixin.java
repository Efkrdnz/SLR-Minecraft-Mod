package net.solocraft.mixins;

import java.util.Arrays;

import net.solocraft.client.gui.worldcreation.SoloLevelingWorldCreationTab;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Appends to the tab array supplied by the screen instead of replacing the
 * navigation bar, preserving tabs added by other mods at the same hook.
 */
@Mixin(value = CreateWorldScreen.class, priority = 800)
public abstract class CreateWorldScreenMixin {
	@ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;addTabs([Lnet/minecraft/client/gui/components/tabs/Tab;)Lnet/minecraft/client/gui/components/tabs/TabNavigationBar$Builder;"), index = 0, require = 0)
	private Tab[] sololeveling$appendWorldCreationTab(Tab[] existingTabs) {
		for (Tab tab : existingTabs) {
			if (tab instanceof SoloLevelingWorldCreationTab) {
				return existingTabs;
			}
		}

		Tab[] expandedTabs = Arrays.copyOf(existingTabs, existingTabs.length + 1);
		expandedTabs[existingTabs.length] = new SoloLevelingWorldCreationTab(Minecraft.getInstance().font);
		return expandedTabs;
	}
}
