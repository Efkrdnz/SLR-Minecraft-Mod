package net.solocraft.util;

import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

@Mod.EventBusSubscriber
public final class LiuManifestationManager {
	private static final String ROOT = "slr_liu_manifestation";
	private static final String ACTIVE = "Active";
	private static final String SESSION = "Session";
	private static final String OWNER = "Owner";
	private static final String HAND = "Hand";
	private static final String LOCKED_SLOT = "LockedSlot";
	private static final String SAVED_MAIN = "SavedMain";
	private static final String SAVED_OFFHAND = "SavedOffhand";
	private static final String MANIFESTED = "slr_liu_manifested_sword";
	private static final String SCALED_STRENGTH = "slr_liu_scaled_strength";
	private static final UUID MANIFESTED_DAMAGE_UUID = UUID.fromString("64218a7e-c2ce-42c1-b996-53d4db0ce871");
	private static final UUID MANIFESTED_SPEED_UUID = UUID.fromString("0861e2c6-ea7d-4455-8cec-1324138a29ad");

	private LiuManifestationManager() {
	}

	public static boolean toggle(ServerPlayer player) {
		if (!LiuZhigangCombatManager.isLiuVessel(player))
			return false;
		if (isActive(player)) {
			restore(player);
			return false;
		}
		activate(player);
		return true;
	}

	public static boolean isActive(Entity entity) {
		return entity != null && entity.getPersistentData().getCompound(ROOT).getBoolean(ACTIVE);
	}

	public static boolean isManifestedSword(ItemStack stack) {
		return !stack.isEmpty() && stack.is(SololevelingModItems.DRAGON_SHORTSWORD.get())
				&& stack.hasTag() && stack.getTag().getBoolean(MANIFESTED);
	}

	public static boolean belongsTo(ItemStack stack, Player player) {
		if (!isManifestedSword(stack) || player == null || !stack.getTag().hasUUID(OWNER))
			return false;
		return player.getUUID().equals(stack.getTag().getUUID(OWNER));
	}

	public static void restore(ServerPlayer player) {
		CompoundTag root = player.getPersistentData().getCompound(ROOT);
		if (!root.getBoolean(ACTIVE)) {
			removeManifestedCopies(player);
			return;
		}

		int lockedSlot = Math.max(0, Math.min(8, root.getInt(LOCKED_SLOT)));
		ItemStack savedMain = ItemStack.of(root.getCompound(SAVED_MAIN));
		ItemStack savedOffhand = ItemStack.of(root.getCompound(SAVED_OFFHAND));
		removeManifestedCopies(player);
		placeRestoredStack(player, lockedSlot, savedMain);
		placeRestoredOffhand(player, savedOffhand);
		player.getPersistentData().remove(ROOT);
		player.getInventory().selected = lockedSlot;
		player.connection.send(new ClientboundSetCarriedItemPacket(lockedSlot));
		player.getInventory().setChanged();
		player.containerMenu.broadcastChanges();
	}

	public static void rejectContainerMove(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			serverPlayer.displayClientMessage(Component.literal("The manifested swords cannot leave your hands."), true);
			enforce(serverPlayer);
		}
	}

	private static void activate(ServerPlayer player) {
		Inventory inventory = player.getInventory();
		int slot = Math.max(0, Math.min(8, inventory.selected));
		UUID session = UUID.randomUUID();
		CompoundTag root = new CompoundTag();
		root.putBoolean(ACTIVE, true);
		root.putUUID(SESSION, session);
		root.putInt(LOCKED_SLOT, slot);
		root.put(SAVED_MAIN, inventory.getItem(slot).save(new CompoundTag()));
		root.put(SAVED_OFFHAND, inventory.offhand.get(0).save(new CompoundTag()));
		player.getPersistentData().put(ROOT, root);

		inventory.setItem(slot, createSword(player, session, "main"));
		inventory.offhand.set(0, createSword(player, session, "offhand"));
		inventory.selected = slot;
		player.connection.send(new ClientboundSetCarriedItemPacket(slot));
		inventory.setChanged();
		player.containerMenu.broadcastChanges();
	}

	private static ItemStack createSword(ServerPlayer player, UUID session, String hand) {
		ItemStack stack = new ItemStack(SololevelingModItems.DRAGON_SHORTSWORD.get());
		CompoundTag tag = stack.getOrCreateTag();
		tag.putBoolean(MANIFESTED, true);
		tag.putUUID(OWNER, player.getUUID());
		tag.putUUID(SESSION, session);
		tag.putString(HAND, hand);
		tag.putBoolean("Unbreakable", true);
		refreshScaledAttributes(stack, player);
		return stack;
	}

	private static void enforce(ServerPlayer player) {
		CompoundTag root = player.getPersistentData().getCompound(ROOT);
		if (!root.getBoolean(ACTIVE)) {
			removeManifestedCopies(player);
			return;
		}
		if (!LiuZhigangCombatManager.isLiuVessel(player) || !root.hasUUID(SESSION)) {
			restore(player);
			return;
		}

		int lockedSlot = Math.max(0, Math.min(8, root.getInt(LOCKED_SLOT)));
		UUID session = root.getUUID(SESSION);
		removeManifestedCopiesExcept(player, lockedSlot);
		if (!validSword(player.getInventory().getItem(lockedSlot), player, session, "main"))
			player.getInventory().setItem(lockedSlot, createSword(player, session, "main"));
		else
			refreshScaledAttributes(player.getInventory().getItem(lockedSlot), player);
		if (!validSword(player.getOffhandItem(), player, session, "offhand"))
			player.getInventory().offhand.set(0, createSword(player, session, "offhand"));
		else
			refreshScaledAttributes(player.getOffhandItem(), player);
		if (player.getInventory().selected != lockedSlot) {
			player.getInventory().selected = lockedSlot;
			player.connection.send(new ClientboundSetCarriedItemPacket(lockedSlot));
		}
		player.getInventory().setChanged();
	}

	private static void refreshScaledAttributes(ItemStack stack, ServerPlayer player) {
		CompoundTag tag = stack.getOrCreateTag();
		double strength = player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(capability -> capability.Strength).orElse(0.0D);
		strength = Math.max(0.0D, strength);
		if (tag.contains(SCALED_STRENGTH) && tag.contains("AttributeModifiers")
				&& Math.abs(tag.getDouble(SCALED_STRENGTH) - strength) < 0.001D)
			return;

		// Manifested swords begin modestly and grow into their National Rank output with Strength.
		double attackBonus = Mth.clamp(2.5D + strength * 0.065D
				+ Math.pow(strength, 1.15D) * 0.008D, 3.0D, 28.0D);
		tag.remove("AttributeModifiers");
		stack.addAttributeModifier(Attributes.ATTACK_DAMAGE,
				new AttributeModifier(MANIFESTED_DAMAGE_UUID, "Liu manifested sword damage",
						attackBonus, AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND);
		stack.addAttributeModifier(Attributes.ATTACK_SPEED,
				new AttributeModifier(MANIFESTED_SPEED_UUID, "Liu manifested sword speed",
						-1.9D, AttributeModifier.Operation.ADDITION), EquipmentSlot.MAINHAND);
		tag.putDouble(SCALED_STRENGTH, strength);
	}

	private static boolean validSword(ItemStack stack, ServerPlayer player, UUID session, String hand) {
		if (!belongsTo(stack, player) || !stack.getTag().hasUUID(SESSION))
			return false;
		return session.equals(stack.getTag().getUUID(SESSION)) && hand.equals(stack.getTag().getString(HAND));
	}

	private static void removeManifestedCopiesExcept(ServerPlayer player, int retainedMainSlot) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.items.size(); i++) {
			if (i != retainedMainSlot && isManifestedSword(inventory.items.get(i)))
				inventory.items.set(i, ItemStack.EMPTY);
		}
		for (int i = 0; i < inventory.armor.size(); i++) {
			if (isManifestedSword(inventory.armor.get(i)))
				inventory.armor.set(i, ItemStack.EMPTY);
		}
	}

	private static void removeManifestedCopies(Player player) {
		Inventory inventory = player.getInventory();
		for (int i = 0; i < inventory.items.size(); i++) {
			if (isManifestedSword(inventory.items.get(i)))
				inventory.items.set(i, ItemStack.EMPTY);
		}
		for (int i = 0; i < inventory.armor.size(); i++) {
			if (isManifestedSword(inventory.armor.get(i)))
				inventory.armor.set(i, ItemStack.EMPTY);
		}
		for (int i = 0; i < inventory.offhand.size(); i++) {
			if (isManifestedSword(inventory.offhand.get(i)))
				inventory.offhand.set(i, ItemStack.EMPTY);
		}
	}

	private static void placeRestoredStack(ServerPlayer player, int slot, ItemStack restored) {
		ItemStack displaced = player.getInventory().getItem(slot);
		player.getInventory().setItem(slot, restored);
		returnDisplaced(player, displaced);
	}

	private static void placeRestoredOffhand(ServerPlayer player, ItemStack restored) {
		ItemStack displaced = player.getInventory().offhand.get(0);
		player.getInventory().offhand.set(0, restored);
		returnDisplaced(player, displaced);
	}

	private static void returnDisplaced(ServerPlayer player, ItemStack displaced) {
		if (displaced.isEmpty() || isManifestedSword(displaced))
			return;
		if (!player.getInventory().add(displaced))
			player.drop(displaced, false);
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()
				|| !(event.player instanceof ServerPlayer player))
			return;
		if (isActive(player) || player.tickCount % 20 == 0)
			enforce(player);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			restore(player);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			restore(player);
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && (isActive(player) || hasManifestedCopy(player)))
			restore(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onToss(ItemTossEvent event) {
		if (!isManifestedSword(event.getEntity().getItem()))
			return;
		event.setCanceled(true);
		event.getEntity().discard();
		if (event.getPlayer() instanceof ServerPlayer player)
			enforce(player);
	}

	@SubscribeEvent
	public static void onDrops(LivingDropsEvent event) {
		event.getDrops().removeIf(item -> isManifestedSword(item.getItem()));
	}

	@SubscribeEvent
	public static void onItemEntityJoin(EntityJoinLevelEvent event) {
		if (!event.getLevel().isClientSide() && event.getEntity() instanceof ItemEntity item
				&& isManifestedSword(item.getItem())) {
			event.setCanceled(true);
			item.discard();
		}
	}

	private static boolean hasManifestedCopy(Player player) {
		for (ItemStack stack : player.getInventory().items)
			if (isManifestedSword(stack))
				return true;
		for (ItemStack stack : player.getInventory().offhand)
			if (isManifestedSword(stack))
				return true;
		return false;
	}
}
