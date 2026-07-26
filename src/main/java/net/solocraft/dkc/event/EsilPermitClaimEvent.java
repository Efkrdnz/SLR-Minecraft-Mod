package net.solocraft.dkc.event;

import net.solocraft.entity.EsilRadiruEntity;

import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import net.minecraft.server.level.ServerPlayer;

/**
 * Server-only decision point for claiming Esil's Floor 15 Entry Permit.
 *
 * <p>The entity fires this only while surrendered and still unclaimed. A DKC
 * listener must validate ownership, floor, run state, and inventory delivery,
 * then call {@link #grantPermit()}. Canceling or denying consumes the click but
 * does not mutate Esil. Leaving the decision as {@link Decision#PASS} lets
 * another interaction handler act.</p>
 */
@Cancelable
public final class EsilPermitClaimEvent extends Event {
	private final EsilRadiruEntity esil;
	private final ServerPlayer player;
	private Decision decision = Decision.PASS;

	public EsilPermitClaimEvent(EsilRadiruEntity esil, ServerPlayer player) {
		this.esil = esil;
		this.player = player;
	}

	public EsilRadiruEntity esil() {
		return esil;
	}

	public ServerPlayer player() {
		return player;
	}

	public Decision decision() {
		return decision;
	}

	public void grantPermit() {
		decision = Decision.GRANT;
	}

	public void deny() {
		decision = Decision.DENY;
	}

	public enum Decision {
		PASS,
		DENY,
		GRANT
	}
}
