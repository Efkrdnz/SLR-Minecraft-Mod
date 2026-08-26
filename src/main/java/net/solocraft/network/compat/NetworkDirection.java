package net.solocraft.network.compat;

/**
 * Compatibility view of the two play-packet directions used by the original
 * Forge networking code.
 */
public enum NetworkDirection {
	PLAY_TO_CLIENT(ReceptionSide.CLIENT),
	PLAY_TO_SERVER(ReceptionSide.SERVER);

	private final ReceptionSide receptionSide;

	NetworkDirection(ReceptionSide receptionSide) {
		this.receptionSide = receptionSide;
	}

	public ReceptionSide getReceptionSide() {
		return receptionSide;
	}

	public enum ReceptionSide {
		CLIENT,
		SERVER;

		public boolean isServer() {
			return this == SERVER;
		}

		public boolean isClient() {
			return this == CLIENT;
		}
	}
}
