package net.solocraft.client.compat.bettercombat;

import net.solocraft.SololevelingMod;
import net.solocraft.network.SungIlHwanAttackMessage;
import net.solocraft.util.SungIlHwanCombatManager;

import net.bettercombat.api.client.BetterCombatClientEvents;

/**
 * Optional Better Combat bridge. ATTACK_HIT is emitted at the actual attack
 * frame, including whiffs, so the spatial cut stays aligned with its animation.
 */
public final class SungIlHwanBetterCombatCompat {
	private static boolean registered;

	private SungIlHwanBetterCombatCompat() {
	}

	public static void register() {
		if (registered)
			return;
		registered = true;
		BetterCombatClientEvents.ATTACK_HIT.register(
				(player, hand, targets, cursorTarget) -> {
					if (SungIlHwanCombatManager.shouldReplaceBasicAttack(player))
						SololevelingMod.PACKET_HANDLER.sendToServer(
								new SungIlHwanAttackMessage());
				});
	}
}
