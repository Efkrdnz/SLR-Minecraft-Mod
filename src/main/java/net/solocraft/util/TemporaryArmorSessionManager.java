package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Cancels delayed manifestation equips when the initiating character state is
 * no longer current. This prevents a queued five-tick callback from overwriting
 * real armor after a reset or another manifestation transition.
 */
public final class TemporaryArmorSessionManager {
	public static final String GENERATION_TAG =
			"slr_temporary_armor_generation";
	public static final String ACTIVE_ESCROW_TAG =
			"slr_temporary_armor_escrow_active";
	public static final String EQUIPPED_ESCROW_TAG =
			"slr_temporary_armor_escrow_equipped";

	private TemporaryArmorSessionManager() {
	}

	public static long begin(Entity entity) {
		long generation = advance(entity);
		if (generation > 0) {
			entity.getPersistentData().putBoolean(ACTIVE_ESCROW_TAG, true);
			entity.getPersistentData().remove(EQUIPPED_ESCROW_TAG);
		}
		return generation;
	}

	/** Invalidates pending equips but leaves the escrow marked for restoration. */
	public static void invalidatePendingEquip(Entity entity) {
		advance(entity);
	}

	/** Called only after the saved armor has been restored or safely returned. */
	public static void finishAfterRestore(Entity entity) {
		advance(entity);
		if (entity != null) {
			entity.getPersistentData().remove(ACTIVE_ESCROW_TAG);
			entity.getPersistentData().remove(EQUIPPED_ESCROW_TAG);
		}
	}

	/**
	 * Drops an escrow marker only when this exact callback is still current.
	 * A reset advances the generation first, so its restoration remains owner.
	 */
	public static void abandonIfCurrent(Entity entity, long generation) {
		if (entity != null && generation > 0
				&& entity.getPersistentData().getLong(GENERATION_TAG)
						== generation) {
			entity.getPersistentData().remove(ACTIVE_ESCROW_TAG);
			entity.getPersistentData().remove(EQUIPPED_ESCROW_TAG);
		}
	}

	public static boolean hasActiveEscrow(Entity entity) {
		return entity != null
				&& entity.getPersistentData().getBoolean(ACTIVE_ESCROW_TAG);
	}

	public static boolean hasEquippedEscrow(Entity entity) {
		return hasActiveEscrow(entity)
				&& entity.getPersistentData().getBoolean(EQUIPPED_ESCROW_TAG);
	}

	public static void markEquipped(Entity entity, long generation) {
		if (entity != null && generation > 0
				&& entity.getPersistentData().getLong(GENERATION_TAG)
						== generation
				&& hasActiveEscrow(entity))
			entity.getPersistentData().putBoolean(EQUIPPED_ESCROW_TAG, true);
	}

	public static boolean canEquipShadow(Entity entity, long generation) {
		return canEquip(entity, generation, 1);
	}

	public static boolean canEquipGoliath(Entity entity, long generation) {
		return canEquip(entity, generation, 5);
	}

	/**
	 * Cancels a queued manifestation equip and restores armor that was already
	 * replaced before a vessel assignment changes.
	 */
	public static void endForVesselChange(Entity entity) {
		if (!(entity instanceof LivingEntity living)
				|| entity.level().isClientSide() || !hasActiveEscrow(entity))
			return;
		boolean restoreArmor = hasEquippedEscrow(entity);
		invalidatePendingEquip(entity);
		if (restoreArmor) {
			entity.getCapability(
					SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.ifPresent(variables -> {
						living.setItemSlot(EquipmentSlot.HEAD,
								variables.overridehead.copy());
						living.setItemSlot(EquipmentSlot.CHEST,
								variables.overridetorso.copy());
						living.setItemSlot(EquipmentSlot.LEGS,
								variables.overridelegs.copy());
						living.setItemSlot(EquipmentSlot.FEET,
								variables.overridefeet.copy());
					});
		}
		finishAfterRestore(entity);
	}

	private static boolean canEquip(Entity entity, long generation,
			int requiredJob) {
		if (!(entity instanceof LivingEntity living) || entity.isRemoved()
				|| entity.level().isClientSide() || generation <= 0
				|| entity.getPersistentData().getLong(GENERATION_TAG)
						!= generation
				|| !hasActiveEscrow(entity))
			return false;
		SololevelingModVariables.PlayerVariables variables = entity
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY,
						null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		boolean hasRequiredJob = variables.JOB == requiredJob;
		return hasRequiredJob
				&& ItemStack.matches(living.getItemBySlot(EquipmentSlot.HEAD),
						variables.overridehead)
				&& ItemStack.matches(living.getItemBySlot(EquipmentSlot.CHEST),
						variables.overridetorso)
				&& ItemStack.matches(living.getItemBySlot(EquipmentSlot.LEGS),
						variables.overridelegs)
				&& ItemStack.matches(living.getItemBySlot(EquipmentSlot.FEET),
						variables.overridefeet);
	}

	private static long advance(Entity entity) {
		if (entity == null || entity.level().isClientSide())
			return -1L;
		long current = entity.getPersistentData().getLong(GENERATION_TAG);
		long next = current == Long.MAX_VALUE ? 1L
				: Math.max(1L, current + 1L);
		entity.getPersistentData().putLong(GENERATION_TAG, next);
		return next;
	}
}
