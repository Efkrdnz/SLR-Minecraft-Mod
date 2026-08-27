package net.solocraft.api.vessel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.solocraft.SololevelingMod;
import net.solocraft.util.ItemStackData;

/**
 * Gear that exists only while a spiritualization is active.
 *
 * <p>The mod's own Monarchs do this two ways. Goliath equips a full armour set
 * while its stance holds; Liu Zhigang replaces what is in your hands with a
 * manifested sword. Both are the same problem underneath: put something on the
 * player, remember exactly what it displaced, put that back afterwards, and make
 * sure the temporary item can never end up in a chest.
 *
 * <p>That last part is why this exists rather than being left to each addon.
 * Equipping is three lines. The hard part is death, logout, a dropped stack, an
 * inventory shuffle, and a server that stopped mid-form -- every one of which
 * either duplicates the temporary item or eats the player's real gear.
 *
 * <h2>Using it</h2>
 *
 * <pre>
 * // In your toggle ability's execute:
 * SpiritualizationGear.equipArmor(player, FORM,
 *         new ItemStack(MY_HELMET.get()), new ItemStack(MY_CHESTPLATE.get()),
 *         new ItemStack(MY_LEGGINGS.get()), new ItemStack(MY_BOOTS.get()));
 *
 * // In deactivate:
 * SpiritualizationGear.restore(player, FORM);
 * </pre>
 *
 * <p>{@code equipWeapon} is the same shape for hands. One form may hold both.
 *
 * <p>Nothing here decides <em>whether</em> the form is active -- that stays with
 * {@link VesselState} and your ability. This only moves items.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class SpiritualizationGear {
	/** Root tag on the player holding every form's displaced gear. */
	private static final String ROOT = "slr_spiritualization_gear";

	/** Marks a stack as belonging to a form rather than to the player. */
	private static final String FORM_TAG = "slr_spirit_form";
	private static final String OWNER_TAG = "slr_spirit_owner";

	private static final String SAVED_ARMOR = "armor";
	private static final String SAVED_MAIN = "main";
	private static final String SAVED_OFFHAND = "offhand";
	private static final String LOCKED_SLOT = "slot";
	private static final String HAS_ARMOR = "has_armor";
	private static final String HAS_HANDS = "has_hands";

	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private SpiritualizationGear() {
	}

	/**
	 * Equips a form's armour, remembering what it displaced.
	 *
	 * <p>Pass an empty stack for a slot the form leaves alone. The player's own
	 * piece stays on and is not recorded, so a partial set is a real option.
	 *
	 * <p>Calling this twice for one form does nothing the second time. Re-saving
	 * would overwrite the player's real armour with the form's own.
	 */
	public static void equipArmor(ServerPlayer player, String formId, ItemStack head,
			ItemStack chest, ItemStack legs, ItemStack feet) {
		if (player == null || formId == null || formId.isBlank())
			return;
		CompoundTag form = form(player, formId);
		if (form.getBoolean(HAS_ARMOR))
			return;

		ItemStack[] pieces = { head, chest, legs, feet };
		CompoundTag saved = new CompoundTag();
		for (int i = 0; i < ARMOR_SLOTS.length; i++) {
			if (pieces[i] == null || pieces[i].isEmpty())
				continue;
			EquipmentSlot slot = ARMOR_SLOTS[i];
			saved.put(slot.getName(),
					ItemStackData.save(player.getItemBySlot(slot), player.registryAccess()));
			player.setItemSlot(slot, mark(pieces[i].copy(), formId, player.getUUID()));
		}
		form.put(SAVED_ARMOR, saved);
		form.putBoolean(HAS_ARMOR, true);
		store(player, formId, form);
		sync(player);
	}

	/**
	 * Puts a form's weapon in the player's hands, remembering what it displaced.
	 *
	 * <p>The selected hotbar slot is recorded so {@link #restore} puts the player
	 * back on it. A form that ends must not leave them holding a slot they never
	 * chose.
	 */
	public static void equipWeapon(ServerPlayer player, String formId, ItemStack mainHand,
			ItemStack offHand) {
		if (player == null || formId == null || formId.isBlank())
			return;
		CompoundTag form = form(player, formId);
		if (form.getBoolean(HAS_HANDS))
			return;

		form.putInt(LOCKED_SLOT, player.getInventory().selected);
		form.put(SAVED_MAIN, ItemStackData.save(
				player.getItemBySlot(EquipmentSlot.MAINHAND), player.registryAccess()));
		form.put(SAVED_OFFHAND, ItemStackData.save(
				player.getItemBySlot(EquipmentSlot.OFFHAND), player.registryAccess()));

		if (mainHand != null && !mainHand.isEmpty())
			player.setItemSlot(EquipmentSlot.MAINHAND,
					mark(mainHand.copy(), formId, player.getUUID()));
		if (offHand != null && !offHand.isEmpty())
			player.setItemSlot(EquipmentSlot.OFFHAND,
					mark(offHand.copy(), formId, player.getUUID()));

		form.putBoolean(HAS_HANDS, true);
		store(player, formId, form);
		sync(player);
	}

	/**
	 * Ends a form's gear: every temporary piece is destroyed and the player's own
	 * equipment goes back where it was.
	 *
	 * <p>Safe when the form equipped nothing, so a deactivate path does not have
	 * to remember which of the two it used.
	 */
	public static void restore(ServerPlayer player, String formId) {
		if (player == null || formId == null)
			return;
		CompoundTag root = player.getPersistentData().getCompound(ROOT);
		if (!root.contains(formId)) {
			// Sweep anyway: a crash between equipping and storing can leave marked
			// items behind with no record of what they displaced.
			purge(player, formId);
			return;
		}
		CompoundTag form = root.getCompound(formId);
		purge(player, formId);

		if (form.getBoolean(HAS_ARMOR)) {
			CompoundTag saved = form.getCompound(SAVED_ARMOR);
			for (EquipmentSlot slot : ARMOR_SLOTS) {
				if (!saved.contains(slot.getName()))
					continue;
				giveBack(player, slot, ItemStackData.load(
						saved.getCompound(slot.getName()), player.registryAccess()));
			}
		}
		if (form.getBoolean(HAS_HANDS)) {
			int locked = Math.max(0, Math.min(8, form.getInt(LOCKED_SLOT)));
			giveBack(player, EquipmentSlot.MAINHAND, ItemStackData.load(
					form.getCompound(SAVED_MAIN), player.registryAccess()));
			giveBack(player, EquipmentSlot.OFFHAND, ItemStackData.load(
					form.getCompound(SAVED_OFFHAND), player.registryAccess()));
			player.getInventory().selected = locked;
			player.connection.send(new ClientboundSetCarriedItemPacket(locked));
		}

		root.remove(formId);
		if (root.isEmpty())
			player.getPersistentData().remove(ROOT);
		sync(player);
	}

	/** Ends every form's gear at once. Used on death, logout, and login. */
	public static void restoreAll(ServerPlayer player) {
		if (player == null)
			return;
		for (String formId : List.copyOf(
				player.getPersistentData().getCompound(ROOT).getAllKeys()))
			restore(player, formId);
	}

	/** True for a stack this system put on a player. */
	public static boolean isTemporary(ItemStack stack) {
		return stack != null && !stack.isEmpty()
				&& !ItemStackData.getString(stack, FORM_TAG).isEmpty();
	}

	/** The form a temporary stack belongs to, or an empty string. */
	public static String formOf(ItemStack stack) {
		return stack == null || stack.isEmpty() ? "" : ItemStackData.getString(stack, FORM_TAG);
	}

	/** True when this player is the one the stack was manifested for. */
	public static boolean belongsTo(ItemStack stack, Player player) {
		if (!isTemporary(stack) || player == null)
			return false;
		CompoundTag data = ItemStackData.copy(stack);
		return data.hasUUID(OWNER_TAG) && player.getUUID().equals(data.getUUID(OWNER_TAG));
	}

	/** Says why the item would not move, the way the manifested sword does. */
	public static void rejectMove(Player player) {
		if (player instanceof ServerPlayer serverPlayer)
			serverPlayer.displayClientMessage(
					Component.literal("Spiritualized equipment cannot leave your hands."), true);
	}

	/** Every form with gear currently out, for a status read. */
	public static List<String> activeGearForms(Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return List.of();
		return new ArrayList<>(player.getPersistentData().getCompound(ROOT).getAllKeys());
	}

	// ---- The guards that make the above safe -------------------------------

	/**
	 * Death would drop the temporary gear as loot and lose what it displaced: the
	 * saved copy lives on the player, and the player is about to be cleared.
	 */
	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			restoreAll(player);
	}

	/**
	 * A form cannot survive a logout. The player would return wearing it with no
	 * ability keeping the upkeep paid.
	 */
	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			restoreAll(player);
	}

	/**
	 * A server that stopped mid-form leaves the gear on. Sweeping on the way in
	 * is what stops it becoming permanent.
	 */
	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			restoreAll(player);
	}

	/** Dropping it would duplicate it: the player still holds the saved original. */
	@SubscribeEvent
	public static void onToss(ItemTossEvent event) {
		if (!isTemporary(event.getEntity().getItem()))
			return;
		event.setCanceled(true);
		rejectMove(event.getPlayer());
		if (event.getPlayer() instanceof ServerPlayer player)
			sync(player);
	}

	/** Belt and braces for any death path that still reaches the drop list. */
	@SubscribeEvent
	public static void onDrops(LivingDropsEvent event) {
		event.getDrops().removeIf(drop -> isTemporary(drop.getItem()));
	}

	/**
	 * Anything that reached the world as an entity is deleted rather than left
	 * for someone else to pick up.
	 */
	@SubscribeEvent
	public static void onItemEntityJoin(EntityJoinLevelEvent event) {
		if (event.getEntity() instanceof ItemEntity item && isTemporary(item.getItem()))
			event.setCanceled(true);
	}

	// ---- internals ---------------------------------------------------------

	private static ItemStack mark(ItemStack stack, String formId, UUID owner) {
		ItemStackData.update(stack, data -> {
			data.putString(FORM_TAG, formId);
			data.putUUID(OWNER_TAG, owner);
		});
		return stack;
	}

	private static CompoundTag form(ServerPlayer player, String formId) {
		CompoundTag root = player.getPersistentData().getCompound(ROOT);
		return root.contains(formId) ? root.getCompound(formId) : new CompoundTag();
	}

	private static void store(ServerPlayer player, String formId, CompoundTag form) {
		CompoundTag root = player.getPersistentData().getCompound(ROOT);
		root.put(formId, form);
		player.getPersistentData().put(ROOT, root);
	}

	private static void sync(ServerPlayer player) {
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
	}

	/** Removes every stack this form put on the player, wherever it ended up. */
	private static void purge(ServerPlayer player, String formId) {
		for (EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack worn = player.getItemBySlot(slot);
			if (isTemporary(worn) && formId.equals(formOf(worn)))
				player.setItemSlot(slot, ItemStack.EMPTY);
		}
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (isTemporary(stack) && formId.equals(formOf(stack)))
				inventory.setItem(i, ItemStack.EMPTY);
		}
	}

	/**
	 * Puts a displaced item back, or in the inventory if the slot was filled
	 * while the form was up. Dropping beats deleting: the player earned that item
	 * once already.
	 */
	private static void giveBack(ServerPlayer player, EquipmentSlot slot, ItemStack original) {
		if (original == null || original.isEmpty())
			return;
		if (player.getItemBySlot(slot).isEmpty()) {
			player.setItemSlot(slot, original);
			return;
		}
		if (!player.getInventory().add(original))
			player.drop(original, false);
	}
}
