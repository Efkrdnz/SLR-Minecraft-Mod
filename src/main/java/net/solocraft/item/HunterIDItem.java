package net.solocraft.item;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ClassStyleRules;
import net.solocraft.util.HunterEvaluationRules;
import net.solocraft.util.ItemStackData;
import net.solocraft.world.inventory.HunterIDGuiMenu;

import net.solocraft.network.compat.NetworkHooks;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import io.netty.buffer.Unpooled;


import java.util.List;

/**
 * Owner-bound Hunter Association ID. Its displayed rank is the last certified
 * rank ({@code prevRank}), not an uncertified level-up rank.
 */
public class HunterIDItem extends Item {
	private static final String OWNER_TAG = "EvaluationOwner";

	public HunterIDItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Item.TooltipContext context,
			List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, context, list, flag);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player entity,
			InteractionHand hand) {
		InteractionResultHolder<ItemStack> result =
				super.use(world, entity, hand);
		if (entity instanceof ServerPlayer serverPlayer) {
			NetworkHooks.openScreen(serverPlayer, new MenuProvider() {
				@Override
				public Component getDisplayName() {
					return Component.literal("Hunter ID");
				}

				@Override
				public AbstractContainerMenu createMenu(int id,
						Inventory inventory, Player player) {
					FriendlyByteBuf packetBuffer =
							new FriendlyByteBuf(Unpooled.buffer());
					packetBuffer.writeBlockPos(entity.blockPosition());
					packetBuffer.writeByte(
							hand == InteractionHand.MAIN_HAND ? 0 : 1);
					return new HunterIDGuiMenu(id, inventory, packetBuffer);
				}
			}, buffer -> {
				buffer.writeBlockPos(entity.blockPosition());
				buffer.writeByte(hand == InteractionHand.MAIN_HAND ? 0 : 1);
			});
		}
		return result;
	}

	@Override
	public void inventoryTick(ItemStack itemstack, Level world, Entity entity,
			int slot, boolean selected) {
		super.inventoryTick(itemstack, world, entity, slot, selected);
		if (!world.isClientSide() && entity instanceof ServerPlayer)
			refreshStack(entity, itemstack);
	}

	public static ItemStack createBoundCard(ServerPlayer player) {
		ItemStack card = new ItemStack(SololevelingModItems.HUNTER_ID.get());
		refreshStack(player, card);
		return card;
	}

	/**
	 * Updates every carried ID owned by this player. Legacy unbound cards bind to
	 * the first player who carries them, preserving old-world compatibility.
	 */
	public static void refreshAll(ServerPlayer player) {
		if (player == null)
			return;
		for (ItemStack stack : player.getInventory().items)
			refreshStack(player, stack);
		for (ItemStack stack : player.getInventory().offhand)
			refreshStack(player, stack);
		for (ItemStack stack : player.getInventory().armor)
			refreshStack(player, stack);
		player.getInventory().setChanged();
	}

	public static void refreshStack(Entity entity, ItemStack itemstack) {
		if (!(entity instanceof ServerPlayer player) || itemstack == null
				|| itemstack.isEmpty()
				|| itemstack.getItem()
						!= SololevelingModItems.HUNTER_ID.get())
			return;

		CompoundTag tag = ItemStackData.copy(itemstack);
		if (!tag.hasUUID(OWNER_TAG)) {
			ItemStackData.update(itemstack, data -> data.putUUID(OWNER_TAG, player.getUUID()));
			tag.putUUID(OWNER_TAG, player.getUUID());
		}
		if (!player.getUUID().equals(tag.getUUID(OWNER_TAG)))
			return;

		SololevelingModVariables.PlayerVariables variables =
				player.getCapability(
						SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY,
						null)
						.orElse(new SololevelingModVariables.PlayerVariables());
		int certifiedRank = bounded((int) Math.round(variables.prevRank), 0, 6);
		int classId = bounded((int) Math.round(variables.Classes), 0,
				HunterEvaluationRules.CLASS_COUNT);
		int styleId = ClassStyleRules.styleId(classId,
				variables.classStyle);
		if (styleId == 0)
			styleId = ClassStyleRules.styleId(classId,
					variables.mageSpecialization);
		final int certifiedStyle = styleId;

		ItemStackData.update(itemstack, data -> {
			putOrRemove(data, "Rank", rankDisplay(certifiedRank));
			putOrRemove(data, "Class", classDisplay(classId, certifiedStyle));
			data.putString("Person", "\u00A7c" + player.getDisplayName().getString());
			data.putInt("EvaluationSchema", 3);
		});
	}

	private static String rankDisplay(int rank) {
		return switch (rank) {
			case 1 -> "\u00A77E";
			case 2 -> "\u00A7aD";
			case 3 -> "\u00A7bC";
			case 4 -> "\u00A79B";
			case 5 -> "\u00A7dA";
			case 6 -> "\u00A76S";
			default -> "";
		};
	}

	/**
	 * The card carries one Hunter Type line, so class and style share it.
	 *
	 * <p>Mage style names already embed their class \u2014 "Fire Mage" \u2014 and
	 * repeating it would read "Mage - Fire Mage". Every other style is a
	 * standalone noun, so the class name stays in front of it. The rule is
	 * expressed as a containment test rather than a per-class special case, so
	 * the remaining rosters inherit the correct behaviour when they ship.</p>
	 */
	private static String classDisplay(int classId, int styleId) {
		String color = switch (classId) {
			case 1 -> "\u00A79";
			case 2 -> "\u00A75";
			case 3 -> "\u00A7f";
			case 4 -> "\u00A79";
			case 5 -> "\u00A7a";
			case 6 -> "\u00A76";
			default -> "";
		};
		if (color.isEmpty())
			return "";
		String className = HunterEvaluationRules.className(classId);
		if (!ClassStyleRules.isValidStyle(classId, styleId)
				|| !ClassStyleRules.isStylePublic(classId))
			return color + className;
		String styleName = ClassStyleRules.styleName(classId, styleId);
		return color + (styleName.contains(className)
				? styleName : className + " \u00B7 " + styleName);
	}

	private static void putOrRemove(CompoundTag tag, String key, String value) {
		if (value == null || value.isEmpty())
			tag.remove(key);
		else
			tag.putString(key, value);
	}

	private static int bounded(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

}
