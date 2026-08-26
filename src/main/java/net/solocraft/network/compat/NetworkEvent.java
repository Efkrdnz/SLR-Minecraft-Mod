package net.solocraft.network.compat;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Minimal source-compatible facade for the old Forge packet context. */
public final class NetworkEvent {
	private NetworkEvent() {
	}

	public static final class Context {
		private final IPayloadContext delegate;

		Context(IPayloadContext delegate) {
			this.delegate = delegate;
		}

		public void enqueueWork(Runnable work) {
			delegate.enqueueWork(work);
		}

		public ServerPlayer getSender() {
			return delegate.player() instanceof ServerPlayer player ? player : null;
		}

		public NetworkDirection getDirection() {
			return delegate.flow() == PacketFlow.SERVERBOUND
					? NetworkDirection.PLAY_TO_SERVER
					: NetworkDirection.PLAY_TO_CLIENT;
		}

		/** NeoForge payload handlers are considered handled once they return. */
		public void setPacketHandled(boolean handled) {
			// Kept for source compatibility with the generated Forge handlers.
		}
	}
}
