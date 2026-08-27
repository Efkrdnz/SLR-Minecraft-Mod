package net.solocraft.api.vessel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.world.entity.player.Player;

import net.solocraft.SololevelingMod;
import net.solocraft.network.VesselMeleeMessage;

/**
 * Lets a contributed vessel take over the left-click attack.
 *
 * <p>Claiming an input is exclusive by nature, so claims carry a priority and the
 * highest wins. Equal priorities tie-break on the claim id, which makes the
 * outcome the same on every install regardless of mod load order -- otherwise
 * which vessel's punch you got would depend on which mod happened to load first.
 *
 * <p>The built-in stances are not routed through here. They keep their original
 * branch in the attack mixin and are checked first, so nothing a contributed
 * vessel does can change how Goliath or the Beast Monarch already behave.
 */
public final class VesselMelee {
	private static final List<Claim> CLAIMS = new ArrayList<>();

	private VesselMelee() {
	}

	/**
	 * Claims the attack button while a {@link VesselState} form is active.
	 *
	 * <p>The simple path: the mod sends the packet and fires
	 * {@link VesselMeleeAttackEvent} on the server, so the addon only writes the
	 * server-side behaviour.
	 */
	public static synchronized void claimForForm(String formId, int priority) {
		String normalised = normalise(formId);
		if (normalised.isEmpty())
			return;
		register(normalised, priority,
				player -> VesselState.isFormActive(player, normalised),
				player -> SololevelingMod.PACKET_HANDLER.sendToServer(
						new VesselMeleeMessage(normalised)));
	}

	/**
	 * Claims the attack button on a condition of your own, running your own
	 * client-side action. Use when you need to send your own packet.
	 *
	 * @param active     evaluated on the client every attack; keep it cheap
	 * @param clientSide what to do when this claim wins. It must not decide the
	 *                   outcome of the attack -- only ask the server for one.
	 */
	public static synchronized void register(String id, int priority,
			Predicate<Player> active, Consumer<Player> clientSide) {
		String normalised = normalise(id);
		if (normalised.isEmpty() || active == null || clientSide == null)
			return;
		CLAIMS.removeIf(claim -> claim.id().equals(normalised));
		CLAIMS.add(new Claim(normalised, priority, active, clientSide));
		CLAIMS.sort(Comparator.comparingInt(Claim::priority).reversed()
				.thenComparing(Claim::id));
	}

	/** The claim that would take this player's next attack, if any. */
	public static Optional<String> claimant(Player player) {
		return winning(player).map(Claim::id);
	}

	public static boolean isClaimed(Player player) {
		return winning(player).isPresent();
	}

	/**
	 * Runs the winning claim's client-side action.
	 *
	 * @return true when a claim took the attack, meaning the vanilla swing must
	 *         be cancelled
	 */
	public static boolean fire(Player player) {
		Optional<Claim> claim = winning(player);
		if (claim.isEmpty())
			return false;
		try {
			claim.get().clientSide().accept(player);
		} catch (RuntimeException exception) {
			// An addon throwing here must not take the attack button down with it.
			SololevelingMod.LOGGER.error("Vessel melee claim {} failed",
					claim.get().id(), exception);
		}
		return true;
	}

	private static Optional<Claim> winning(Player player) {
		if (player == null)
			return Optional.empty();
		List<Claim> snapshot;
		synchronized (VesselMelee.class) {
			if (CLAIMS.isEmpty())
				return Optional.empty();
			snapshot = List.copyOf(CLAIMS);
		}
		for (Claim claim : snapshot) {
			try {
				if (claim.active().test(player))
					return Optional.of(claim);
			} catch (RuntimeException exception) {
				SololevelingMod.LOGGER.error("Vessel melee claim {} threw while being tested",
						claim.id(), exception);
			}
		}
		return Optional.empty();
	}

	private static String normalise(String id) {
		return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
	}

	private record Claim(String id, int priority, Predicate<Player> active,
			Consumer<Player> clientSide) {
	}
}
