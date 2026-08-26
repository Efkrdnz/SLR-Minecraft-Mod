package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * The Cartenon Temple strips a Monarch back down to a hunter.
 *
 * <p>Inside the temple the player is measured on their own, so nothing they
 * inherited from a vessel works: no vessel actives, none of the passive
 * mitigation that lets a Ruler shrug off sword beams and heavy strikes, and no
 * shadow army. What remains is the class they trained -- the hunter who walked
 * in here at level one.
 *
 * <p>Deliberately one predicate consulted from many places rather than a check
 * copied into each. Vessel power reaches combat through several unrelated
 * managers, and a rule enforced in only some of them is not a rule.
 */
@EventBusSubscriber
public final class CartenonSuppression {
	private static final String NOTIFIED_TAG = "slr_temple_suppression_notified";

	private CartenonSuppression() {
	}

	/** True while this entity stands in the Cartenon Temple. */
	public static boolean isSuppressed(Entity entity) {
		return entity != null && entity.level().dimension()
				== CartenonTempleManager.CARTENON_DIMENSION;
	}

	/**
	 * Gate for a vessel <em>active</em>. Tells the player why nothing happened,
	 * because a skill that silently does nothing reads as a bug.
	 */
	public static boolean blockVesselSkill(Entity entity) {
		if (!isSuppressed(entity))
			return false;
		if (entity instanceof ServerPlayer player)
			player.displayClientMessage(Component.literal(
					"The temple does not answer to Monarchs.")
					.withStyle(ChatFormatting.DARK_PURPLE), true);
		return true;
	}

	/** Gate for passive vessel mitigation, which has nothing to announce. */
	public static boolean blockVesselPassive(Entity entity) {
		return isSuppressed(entity);
	}

	/**
	 * Keeps the army out. Shadows are recalled on entry and cannot be summoned
	 * again while inside, so a player cannot walk in with them already standing.
	 */
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (!isSuppressed(player)) {
			player.getPersistentData().remove(NOTIFIED_TAG);
			return;
		}
		if (player.tickCount % 20 != 0)
			return;
		// Repeated rather than once on entry: a shadow can be summoned by a queued
		// action, arrive through a portal, or survive a relog inside the temple.
		ShadowMonarchManager.recallAllShadows(player);
		if (player.getPersistentData().getBoolean(NOTIFIED_TAG))
			return;
		player.getPersistentData().putBoolean(NOTIFIED_TAG, true);
		if (!isVessel(player))
			return;
		SystemNotifications.showTitleUnder(player, 0xFF9B5CFF, 110,
				Component.literal("STRIPPED").withStyle(ChatFormatting.LIGHT_PURPLE,
						ChatFormatting.BOLD),
				Component.literal("Your vessel is silent here. Only the hunter remains.")
						.withStyle(ChatFormatting.GRAY));
	}

	private static boolean isVessel(Player player) {
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(variables -> variables.JOB > 0)
				.orElse(false);
	}
}
