package net.solocraft.procedures;

import net.solocraft.util.MageSpellProgression;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Shared runestone behaviour: teach one exact ability, permanently.
 *
 * <p>The older runestone procedures each inlined a {@code Plist.contains(...)}
 * substring test, which cannot distinguish "Cross Strike" from "Critical
 * Strike". This helper matches exact tokens and records the runestone ownership
 * marker, so a later class or style change can never delete a rune-taught
 * ability.</p>
 *
 * <p>Runestones are universal by design. Nothing here checks class or style —
 * only vessel powers are identity-locked.</p>
 */
public final class RunestoneGrantHelper {
	private RunestoneGrantHelper() {
	}

	public static void grant(Entity entity, ItemStack stack, String skill) {
		if (entity == null || skill == null || skill.isBlank())
			return;
		if (MageSpellProgression.hasSkill(entity, skill)) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(
						Component.literal("You already have this skill!"), false);
			return;
		}

		if (!MageSpellProgression.unlockFromRunestone(entity, skill))
			return;
		if (entity instanceof Player player) {
			if (!player.level().isClientSide())
				player.displayClientMessage(
						Component.literal("Gained skill: " + skill), false);
			if (!player.isCreative() && stack != null && !stack.isEmpty())
				stack.shrink(1);
		}
	}
}
