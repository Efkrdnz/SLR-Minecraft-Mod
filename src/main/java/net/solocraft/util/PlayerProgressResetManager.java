package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.dkc.DkcFloorBuilder;
import net.solocraft.dkc.DkcQuestProgressTracker;
import net.solocraft.dkc.DkcRadiruManager;
import net.solocraft.dkc.DkcRunSavedData;
import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.party.PartyHighlightManager;
import net.solocraft.procedures.DungeonDimensionPlayerLeavesDimensionProcedure;
import net.solocraft.procedures.JobChangeCleanupProcedure;
import net.solocraft.util.daily.DailyQuestLifecycleManager;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

import net.minecraftforge.registries.ForgeRegistries;

/**
 * Server-authoritative, player-scoped implementation of {@code /slr reset}.
 */
public final class PlayerProgressResetManager {
	private static final String STORY_DUNGEON_TAG = "story_intro_ancient_golem";
	private static final String IGRIS_DUNGEON = "dungeon_dimension_igris";

	private PlayerProgressResetManager() {
	}

	public static boolean reset(ServerPlayer player) {
		if (player == null || player.server == null)
			return false;
		if (StoryModeIntroManager.isStoryOwner(player)
				|| player.getPersistentData().getBoolean(
						"slr_cartenon_awakening_pending")) {
			player.displayClientMessage(Component.literal(
					"Character reset is unavailable while the System awakening story is active.")
					.withStyle(ChatFormatting.RED), false);
			return false;
		}

		SololevelingModVariables.PlayerVariables current = variables(player);
		PreservedState preserved = PreservedState.capture(current);
		boolean legacyTemporaryArmor = hasEquippedTemporaryArmor(player);
		boolean temporaryArmorEscrow =
				TemporaryArmorSessionManager.hasActiveEscrow(player)
						|| legacyTemporaryArmor;
		boolean temporaryArmorWasEquipped =
				TemporaryArmorSessionManager.hasEquippedEscrow(player)
						|| legacyTemporaryArmor;
		TemporaryArmorSessionManager.invalidatePendingEquip(player);
		PlayerEntryGenerationGuard.invalidate(player);

		detachFromActiveDungeon(player, current);
		DaggerThrowManager.recoverEscrowForReset(player);
		AssassinSkillManager.resetPlayerState(player);
		ArcaneMageSpellManager.resetPlayerState(player);
		BarrierMageSpellManager.resetPlayerState(player);
		FireMageSpellManager.resetPlayerState(player);
		StormMageSpellManager.resetPlayerState(player);
		RangerCombatManager.resetPlayerState(player);
		ClassPassiveManager.resetPlayerState(player);
		TankerSkillManager.resetPlayerState(player);
		FrostArchitectureManager.resetPlayerState(player);
		FrostMonarchManager.resetPlayerState(player);
		RulersAuthorityManager.resetPlayerState(player);
		GoliathCombatManager.resetPlayerState(player);
		LiuZhigangCombatManager.resetPlayerState(player);
		BeastMonarchManager.resetPlayerState(player);
		WhiteFlameMonarchManager.resetPlayerState(player);
		ShadowMonarchManager.resetPlayerProgress(player);
		UrgentQuestManager.resetForPlayerReset(player);
		restoreEscrowedArmor(player, current, temporaryArmorEscrow,
				temporaryArmorWasEquipped);
		VesselManager.resetPlayer(player);
		JobChangeQuestManager.resetForPlayerReset(player);
		DkcFloorBuilder.cancelPlayerBuilds(player.server, player.getUUID());
		DkcRadiruManager.resetPlayerState(player);
		DkcQuestProgressTracker.resetPlayerState(player);
		DkcRunSavedData.get(player.server).resetProgress(player.getUUID());
		PlayerAuraSystem.clearContinuous(player);
		PartyHighlightManager.clearNow(player);
		EntityHighlightSystem.clearAll(player);
		CooldownManager.clearAll(player);
		clearModPersistentData(player.getPersistentData());

		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			// Using the capability's own complete NBT schema makes newly-added
			// fields reset automatically instead of relying on a partial field list.
			capability.readNBT(new SololevelingModVariables.PlayerVariables().writeNBT());
			preserved.restore(capability);
			capability.syncPlayerVariables(player);
		});

		DailyQuestLifecycleManager.resetQuestState(player, true);
		revokeModProgressAdvancements(player);

		removeModEffects(player);
		player.setNoGravity(false);
		// Re-award only prerequisites implied by the retained System identity.
		// Job/boss receipts stay revoked and can therefore be earned again.
		VesselProgressionManager.reconcileEntitlements(player);
		PlayerVitalSync.restoreAfterRespawn(player);
		PartyHighlightManager.syncNow(player);
		return true;
	}

	private static void detachFromActiveDungeon(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables) {
		ResourceLocation dimension = player.level().dimension().location();
		boolean jobDungeon = SololevelingMod.MODID.equals(dimension.getNamespace())
				&& IGRIS_DUNGEON.equals(dimension.getPath());
		if (jobDungeon && player.serverLevel().players().stream()
				.noneMatch(other -> other != player && JobChangeQuestManager.canResumeDungeon(other)))
			JobChangeCleanupProcedure.execute(player.serverLevel(),
					player.getX(), player.getY(), player.getZ());

		boolean boundToRuntime = !player.getPersistentData()
				.getString(DungeonMobLevelAdapter.INSTANCE_TAG).isBlank();
		boolean resettableModDimension = SololevelingMod.MODID.equals(dimension.getNamespace())
				&& (dimension.getPath().contains("dungeon")
						|| dimension.getPath().contains("castle"));
		if (boundToRuntime || variables.dungeoning || resettableModDimension)
			DungeonDimensionPlayerLeavesDimensionProcedure.emergencyExit(player);
	}

	private static void clearModPersistentData(CompoundTag root) {
		clearKnownKeys(root);
		if (root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND))
			clearKnownKeys(root.getCompound(Player.PERSISTED_NBT_TAG));
	}

	private static void clearKnownKeys(CompoundTag tag) {
		for (String key : new ArrayList<>(tag.getAllKeys())) {
			if ("dungeon_tag".equals(key)
					&& STORY_DUNGEON_TAG.equals(tag.getString(key)))
				continue;
			if (PlayerResetKeyPolicy.shouldClear(key))
				tag.remove(key);
		}
	}

	private static void revokeModProgressAdvancements(ServerPlayer player) {
		for (Advancement advancement : player.server.getAdvancements().getAllAdvancements()) {
			if (!SololevelingMod.MODID.equals(advancement.getId().getNamespace()))
				continue;
			if ("awakened".equals(advancement.getId().getPath()))
				continue;
			AdvancementProgress progress = player.getAdvancements()
					.getOrStartProgress(advancement);
			ArrayList<String> completedCriteria = new ArrayList<>();
			for (String criterion : progress.getCompletedCriteria())
				completedCriteria.add(criterion);
			for (String criterion : completedCriteria)
				player.getAdvancements().revoke(advancement, criterion);
		}
	}

	private static void removeModEffects(ServerPlayer player) {
		for (MobEffectInstance active : new ArrayList<>(player.getActiveEffects())) {
			ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS
					.getKey(active.getEffect());
			if (effectId != null
					&& SololevelingMod.MODID.equals(effectId.getNamespace()))
				player.removeEffect(active.getEffect());
		}
	}

	private static void restoreEscrowedArmor(ServerPlayer player,
			SololevelingModVariables.PlayerVariables variables,
			boolean activeEscrow, boolean wasEquipped) {
		if (!activeEscrow)
			return;
		if (wasEquipped) {
			restoreArmorSlot(player, EquipmentSlot.HEAD, variables.overridehead);
			restoreArmorSlot(player, EquipmentSlot.CHEST, variables.overridetorso);
			restoreArmorSlot(player, EquipmentSlot.LEGS, variables.overridelegs);
			restoreArmorSlot(player, EquipmentSlot.FEET, variables.overridefeet);
			player.getInventory().setChanged();
		}
		TemporaryArmorSessionManager.finishAfterRestore(player);
	}

	private static void restoreArmorSlot(ServerPlayer player, EquipmentSlot slot,
			ItemStack saved) {
		ItemStack escrowed = saved == null ? ItemStack.EMPTY : saved;
		if (isTemporaryArmorForSlot(player.getItemBySlot(slot), slot)) {
			player.setItemSlot(slot, escrowed.copy());
			return;
		}
		if (escrowed.isEmpty())
			return;
		ItemStack returned = escrowed.copy();
		player.getInventory().add(returned);
		if (!returned.isEmpty())
			player.spawnAtLocation(returned);
	}

	private static boolean isTemporaryArmorForSlot(ItemStack stack,
			EquipmentSlot slot) {
		if (stack == null || stack.isEmpty())
			return false;
		Item item = stack.getItem();
		return switch (slot) {
			case HEAD -> item == SololevelingModItems.SHADOW_ARMOR_HELMET.get()
					|| item == SololevelingModItems.GOLIATH_ARMOR_HELMET.get();
			case CHEST -> item == SololevelingModItems.SHADOW_ARMOR_CHESTPLATE.get()
					|| item == SololevelingModItems.GOLIATH_ARMOR_CHESTPLATE.get();
			case LEGS -> item == SololevelingModItems.SHADOW_ARMOR_LEGGINGS.get()
					|| item == SololevelingModItems.GOLIATH_ARMOR_LEGGINGS.get();
			case FEET -> item == SololevelingModItems.SHADOW_ARMOR_BOOTS.get()
					|| item == SololevelingModItems.GOLIATH_ARMOR_BOOTS.get();
			default -> false;
		};
	}

	private static boolean hasEquippedTemporaryArmor(ServerPlayer player) {
		return isTemporaryArmorForSlot(
				player.getItemBySlot(EquipmentSlot.HEAD), EquipmentSlot.HEAD)
				|| isTemporaryArmorForSlot(
						player.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST)
				|| isTemporaryArmorForSlot(
						player.getItemBySlot(EquipmentSlot.LEGS), EquipmentSlot.LEGS)
				|| isTemporaryArmorForSlot(
						player.getItemBySlot(EquipmentSlot.FEET), EquipmentSlot.FEET);
	}

	private static SololevelingModVariables.PlayerVariables variables(ServerPlayer player) {
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private record PreservedState(boolean systemPlayer, double loreAccurateRankStart,
			boolean customHud, boolean pvpUrgentQuests, String party,
			double guildCode) {
		private static PreservedState capture(
				SololevelingModVariables.PlayerVariables variables) {
			return new PreservedState(variables.Player,
					variables.LoreAccurateRankStart, variables.CustomHUD,
					variables.pvpUrgentQuests,
					variables.party == null ? "" : variables.party,
					variables.GuildCode);
		}

		private void restore(SololevelingModVariables.PlayerVariables variables) {
			variables.Player = systemPlayer;
			variables.LoreAccurateRankStart = loreAccurateRankStart;
			variables.CustomHUD = customHud;
			variables.pvpUrgentQuests = pvpUrgentQuests;
			variables.party = party;
			variables.GuildCode = guildCode;
		}
	}
}
