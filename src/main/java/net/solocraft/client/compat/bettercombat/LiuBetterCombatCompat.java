package net.solocraft.client.compat.bettercombat;

import net.solocraft.client.LiuCombatClientEvents;

import net.bettercombat.api.client.BetterCombatClientEvents;

public final class LiuBetterCombatCompat {
	private static boolean registered;

	private LiuBetterCombatCompat() {
	}

	public static void register() {
		if (registered)
			return;
		registered = true;
		BetterCombatClientEvents.ATTACK_START.register((player, hand) ->
				LiuCombatClientEvents.sendEnhancedAttack(hand.isOffHand(), hand.combo().current()));
	}
}
