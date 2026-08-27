package net.solocraft.api.vessel;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * Fired on the server when a hunter left-clicks while a contributed form has
 * claimed the attack button.
 *
 * <p>This is the server-authoritative half of a contributed melee. The client
 * only reports that the button was pressed; whether anything happens, and what,
 * is decided here. Subscribers must apply their own resource and cooldown checks
 * -- reaching this event proves the player pressed a button, nothing more.
 */
public class VesselMeleeAttackEvent extends Event {
	private final ServerPlayer player;
	private final String formId;

	public VesselMeleeAttackEvent(ServerPlayer player, String formId) {
		this.player = player;
		this.formId = formId;
	}

	public ServerPlayer getPlayer() {
		return player;
	}

	/** The form that claimed the attack, as registered. */
	public String getFormId() {
		return formId;
	}
}
