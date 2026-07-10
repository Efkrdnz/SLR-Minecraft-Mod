
package net.solocraft.item;

import net.solocraft.procedures.DemonKingsCastleKeyUseProcedure;
import net.solocraft.util.DkcQuestManager;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;

import java.util.List;

public class RedkeyItem extends Item {
	private static final String OWNER_UUID_TAG = "dkc_key_owner_uuid";
	private static final String OWNER_NAME_TAG = "dkc_key_owner_name";

	public RedkeyItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean isFoil(ItemStack itemstack) {
		return true;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("A key carved from a dying throne's shadow.").withStyle(ChatFormatting.DARK_PURPLE));
		list.add(Component.literal("Right click in the Overworld to enter the castle.").withStyle(ChatFormatting.DARK_RED));
		list.add(Component.literal("Right click inside the castle to return where you stood.").withStyle(ChatFormatting.DARK_PURPLE));
		list.add(Component.literal("The castle remembers every hand that opens it.").withStyle(ChatFormatting.GRAY));
		if (itemstack.hasTag() && itemstack.getTag().hasUUID(OWNER_UUID_TAG)) {
			list.add(Component.literal("Bound to: " + itemstack.getTag().getString(OWNER_NAME_TAG)).withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.BOW;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		ItemStack stack = entity.getItemInHand(hand);
		if (!world.isClientSide() && !bindOrVerifyOwner(stack, entity)) {
			entity.displayClientMessage(Component.literal("\u00A74The key rejects your hand. It belongs to " + getOwnerName(stack) + "."), true);
			return InteractionResultHolder.fail(stack);
		}
		if (!world.isClientSide())
			DkcQuestManager.unlock(entity);
		DemonKingsCastleKeyUseProcedure.execute(world, entity, stack);
		return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (!world.isClientSide() && entity instanceof Player player) {
			bindOrVerifyOwner(itemstack, player);
			DkcQuestManager.unlock(player);
		}
	}

	public static boolean bindOrVerifyOwner(ItemStack stack, Player player) {
		if (stack.isEmpty() || !(stack.getItem() instanceof RedkeyItem))
			return true;
		if (!stack.getOrCreateTag().hasUUID(OWNER_UUID_TAG)) {
			stack.getOrCreateTag().putUUID(OWNER_UUID_TAG, player.getUUID());
			stack.getOrCreateTag().putString(OWNER_NAME_TAG, player.getName().getString());
			return true;
		}
		return stack.getOrCreateTag().getUUID(OWNER_UUID_TAG).equals(player.getUUID());
	}

	public static String getOwnerName(ItemStack stack) {
		if (stack.hasTag() && stack.getTag().contains(OWNER_NAME_TAG)) {
			return stack.getTag().getString(OWNER_NAME_TAG);
		}
		return "another hunter";
	}
}
