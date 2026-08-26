
package net.solocraft.item.inventory;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.client.gui.HunterIDGuiScreen;

import net.neoforged.neoforge.items.ComponentItemHandler;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;

@EventBusSubscriber(Dist.CLIENT)
public class HunterIDInventoryCapability extends ComponentItemHandler {
	public HunterIDInventoryCapability(ItemStack stack) {
		super(stack, DataComponents.CONTAINER, 9);
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public static void onItemDropped(ItemTossEvent event) {
		if (event.getEntity().getItem().getItem() == SololevelingModItems.HUNTER_ID.get()) {
			if (Minecraft.getInstance().screen instanceof HunterIDGuiScreen) {
				Minecraft.getInstance().player.closeContainer();
			}
		}
	}

	@Override
	public int getSlotLimit(int slot) {
		return 64;
	}

	@Override
	public boolean isItemValid(int slot, ItemStack stack) {
		return stack.getItem() != SololevelingModItems.HUNTER_ID.get();
	}
}
