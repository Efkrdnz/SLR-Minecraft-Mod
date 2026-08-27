package net.solocraft.potion;

import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Marker effect that carries Infiltrator concealment to every client.
 *
 * <p>It grants no attributes. Its only job is to be a synchronised, dimension-
 * safe flag the client render layer can read, which a server-side combat map
 * cannot be. Hidden from the inventory and HUD because the Veil meter already
 * communicates the state to its owner.</p>
 */
public class VeilShroudMobEffect extends MobEffect {
	public VeilShroudMobEffect() {
		super(MobEffectCategory.NEUTRAL, 0xFF6E5CFF);
	}

	@Override
	public String getDescriptionId() {
		return "effect.sololeveling.veil_shroud";
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return false;
	}

	@Override
	public void initializeClient(java.util.function.Consumer<IClientMobEffectExtensions> consumer) {
		consumer.accept(new IClientMobEffectExtensions() {
			@Override
			public boolean isVisibleInInventory(MobEffectInstance effect) {
				return false;
			}

			@Override
			public boolean renderInventoryText(MobEffectInstance instance, EffectRenderingInventoryScreen<?> screen, GuiGraphics guiGraphics, int x, int y, int blitOffset) {
				return false;
			}

			@Override
			public boolean isVisibleInGui(MobEffectInstance effect) {
				return false;
			}
		});
	}
}
