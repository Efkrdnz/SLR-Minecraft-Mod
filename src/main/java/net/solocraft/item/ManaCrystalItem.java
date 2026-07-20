package net.solocraft.item;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.SystemPlayerAccess;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

/** Shared, server-authoritative conversion behavior for every mana crystal grade. */
public abstract class ManaCrystalItem extends Item {
	private final int goldValue;

	protected ManaCrystalItem(Rarity rarity, int goldValue) {
		super(new Item.Properties().stacksTo(64).rarity(rarity));
		this.goldValue = goldValue;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide())
			return InteractionResultHolder.success(stack);
		if (!SystemPlayerAccess.hasSystem(player))
			return InteractionResultHolder.fail(stack);

		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			capability.golds += goldValue;
			capability.syncPlayerVariables(player);
		});
		stack.shrink(1);
		player.displayClientMessage(Component.literal("+" + goldValue + " gold"), true);
		return InteractionResultHolder.consume(stack);
	}
}
