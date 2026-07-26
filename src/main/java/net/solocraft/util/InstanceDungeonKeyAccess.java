package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;

import net.minecraft.advancements.Advancement;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Durable ownership and recovery rules for the one-time Instant Dungeon Key.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class InstanceDungeonKeyAccess {
	private static final String CLAIMED_TAG = "slr_instance_dungeon_key_claimed";
	private static final String COMPLETED_TAG = "slr_instance_dungeon_completed";
	private static final ResourceLocation KASAKA_ADVANCEMENT =
			new ResourceLocation("sololeveling", "kasakas_domain");
	private static final ResourceLocation INSTANCE_ENTRY_ADVANCEMENT =
			new ResourceLocation("sololeveling", "explore_dun_instance_c");

	private InstanceDungeonKeyAccess() {
	}

	public static boolean grantInitialKey(Player player) {
		if (player == null || hasClaimed(player))
			return false;
		markClaimed(player);
		ItemHandlerHelper.giveItemToPlayer(player,
				new ItemStack(SololevelingModItems.INSTANCE_DUNGEON_KEY.get()));
		return true;
	}

	public static boolean canEnter(ServerPlayer player) {
		if (player == null || hasCompleted(player))
			return false;
		boolean gettingStronger = variables(player).MainQuest.equals("Getting Stronger");
		return hasPhysicalKey(player) || gettingStronger && hasClaimed(player);
	}

	public static boolean hasClaimed(Player player) {
		if (player == null)
			return false;
		CompoundTag persisted = persistentPlayerData(player);
		if (persisted.getBoolean(CLAIMED_TAG))
			return true;
		SololevelingModVariables.PlayerVariables variables = variables(player);
		boolean legacyClaim = hasPhysicalKey(player)
				|| "Getting Stronger".equals(variables.MainQuest)
						&& variables.QuestProgression >= 1.0D
				|| hasAdvancement(player, INSTANCE_ENTRY_ADVANCEMENT);
		if (legacyClaim)
			persisted.putBoolean(CLAIMED_TAG, true);
		return legacyClaim;
	}

	public static boolean hasCompleted(Player player) {
		if (player == null)
			return false;
		CompoundTag persisted = persistentPlayerData(player);
		if (persisted.getBoolean(COMPLETED_TAG))
			return true;
		if (hasAdvancement(player, KASAKA_ADVANCEMENT)
				&& hasAdvancement(player, INSTANCE_ENTRY_ADVANCEMENT)) {
			persisted.putBoolean(CLAIMED_TAG, true);
			persisted.putBoolean(COMPLETED_TAG, true);
			return true;
		}
		return false;
	}

	public static void markClaimed(Player player) {
		if (player != null)
			persistentPlayerData(player).putBoolean(CLAIMED_TAG, true);
	}

	public static void markCompleted(Player player) {
		if (player == null)
			return;
		CompoundTag persisted = persistentPlayerData(player);
		persisted.putBoolean(CLAIMED_TAG, true);
		persisted.putBoolean(COMPLETED_TAG, true);
	}

	public static boolean hasPhysicalKey(Player player) {
		return player != null && player.getInventory()
				.contains(new ItemStack(SololevelingModItems.INSTANCE_DUNGEON_KEY.get()));
	}

	public static void consumePhysicalKey(Player player) {
		if (player == null || player.getAbilities().instabuild)
			return;
		player.getInventory().clearOrCountMatchingItems(
				stack -> stack.is(SololevelingModItems.INSTANCE_DUNGEON_KEY.get()),
				1, player.inventoryMenu.getCraftSlots());
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		CompoundTag originalRoot = event.getOriginal().getPersistentData();
		if (!originalRoot.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			return;
		CompoundTag originalPersisted =
				originalRoot.getCompound(Player.PERSISTED_NBT_TAG);
		CompoundTag clonePersisted = persistentPlayerData(event.getEntity());
		if (originalPersisted.getBoolean(CLAIMED_TAG))
			clonePersisted.putBoolean(CLAIMED_TAG, true);
		if (originalPersisted.getBoolean(COMPLETED_TAG))
			clonePersisted.putBoolean(COMPLETED_TAG, true);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		hasClaimed(player);
		hasCompleted(player);
	}

	private static boolean hasAdvancement(Player player, ResourceLocation id) {
		if (!(player instanceof ServerPlayer serverPlayer))
			return false;
		Advancement advancement = serverPlayer.server.getAdvancements()
				.getAdvancement(id);
		return advancement != null && serverPlayer.getAdvancements()
				.getOrStartProgress(advancement).isDone();
	}

	private static CompoundTag persistentPlayerData(Player player) {
		CompoundTag root = player.getPersistentData();
		if (!root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
		return root.getCompound(Player.PERSISTED_NBT_TAG);
	}

	private static SololevelingModVariables.PlayerVariables variables(Player player) {
		return player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}
}
