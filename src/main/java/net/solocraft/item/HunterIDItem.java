
package net.solocraft.item;

import net.solocraft.world.inventory.HunterIDGuiMenu;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.item.inventory.HunterIDInventoryCapability;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

import java.util.List;

import io.netty.buffer.Unpooled;

public class HunterIDItem extends Item {
	public HunterIDItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity, InteractionHand hand) {
		InteractionResultHolder<ItemStack> ar = super.use(world, entity, hand);
		if (entity instanceof ServerPlayer serverPlayer) {
			NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Hunter ID");
				}

				@Override
				public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
					FriendlyByteBuf packetBuffer = new FriendlyByteBuf(Unpooled.buffer());
					packetBuffer.writeBlockPos(entity.blockPosition());
					packetBuffer.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1);
					return new HunterIDGuiMenu(id, inventory, packetBuffer);
				}
			}, buf -> {
				buf.writeBlockPos(entity.blockPosition());
				buf.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1);
			});
		}
		return ar;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		updateHunterIdTags(entity, itemstack);
	}

	private static void updateHunterIdTags(Entity entity, ItemStack itemstack) {
		if (entity == null)
			return;
		SololevelingModVariables.PlayerVariables vars = entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables());
		if (itemstack.getOrCreateTag().getString("Rank").equals("")) {
			if (vars.HunterRank == 1) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A7aE");
			} else if (vars.HunterRank == 2) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A70D");
			} else if (vars.HunterRank == 3) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A7aC");
			} else if (vars.HunterRank == 4) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A79B");
			} else if (vars.HunterRank == 5) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A79A");
			} else if (vars.HunterRank == 6) {
				itemstack.getOrCreateTag().putString("Rank", "\u00A76S");
			}
		}
		if (itemstack.getOrCreateTag().getString("Class").equals("")) {
			if (vars.Classes == 1) {
				itemstack.getOrCreateTag().putString("Class", "\u00A79Assassin");
			} else if (vars.Classes == 2) {
				itemstack.getOrCreateTag().putString("Class", "\u00A76Mage");
			} else if (vars.Classes == 3) {
				itemstack.getOrCreateTag().putString("Class", "\u00A7cFighter");
			} else if (vars.Classes == 4) {
				itemstack.getOrCreateTag().putString("Class", "\u00A78Tanker");
			} else if (vars.Classes == 5) {
				itemstack.getOrCreateTag().putString("Class", "\u00A7aHealer");
			} else if (vars.Classes == 6) {
				itemstack.getOrCreateTag().putString("Class", "\u00A73Ranger");
			}
		}
		if (itemstack.getOrCreateTag().getString("Person").equals(""))
			itemstack.getOrCreateTag().putString("Person", "\u00A7c" + entity.getDisplayName().getString());
	}

	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag compound) {
		return new HunterIDInventoryCapability();
	}

	@Override
	public CompoundTag getShareTag(ItemStack stack) {
		CompoundTag nbt = stack.getOrCreateTag();
		stack.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> nbt.put("Inventory", ((ItemStackHandler) capability).serializeNBT()));
		return nbt;
	}

	@Override
	public void readShareTag(ItemStack stack, @Nullable CompoundTag nbt) {
		super.readShareTag(stack, nbt);
		if (nbt != null)
			stack.getCapability(ForgeCapabilities.ITEM_HANDLER, null).ifPresent(capability -> ((ItemStackHandler) capability).deserializeNBT((CompoundTag) nbt.get("Inventory")));
	}
}
