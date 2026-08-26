package net.solocraft.network.compat;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import net.solocraft.SololevelingMod;

/**
 * NeoForge 1.21 payload channel that adapts the mod's existing message codecs.
 * The wire format is a stable numeric message id followed by that message's
 * original FriendlyByteBuf encoding.
 */
public final class LegacyNetworkChannel {
	private final Map<Integer, Registration<?>> byId = new HashMap<>();
	private final Map<Class<?>, Registration<?>> byType = new HashMap<>();

	public synchronized <T> void registerMessage(int id, Class<T> type,
			BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder,
			BiConsumer<T, Supplier<NetworkEvent.Context>> consumer) {
		registerMessage(id, type, encoder, decoder, consumer, Optional.empty());
	}

	public synchronized <T> void registerMessage(int id, Class<T> type,
			BiConsumer<T, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, T> decoder,
			BiConsumer<T, Supplier<NetworkEvent.Context>> consumer,
			Optional<NetworkDirection> direction) {
		// A generated variables class registers its sync packet twice. Preserve the
		// first stable id instead of making decoding order-dependent.
		if (byType.containsKey(type)) {
			return;
		}
		Registration<T> registration = new Registration<>(id, type, encoder, decoder,
				consumer, direction.orElse(null));
		if (byId.putIfAbsent(id, registration) != null) {
			throw new IllegalStateException("Duplicate Solo Leveling packet id " + id);
		}
		byType.put(type, registration);
	}

	public void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar("3").playBidirectional(LegacyPayload.TYPE, LegacyPayload.STREAM_CODEC,
				this::handle);
	}

	public void sendToServer(Object message) {
		send(PacketDistributor.SERVER.noArg(), message);
	}

	public void send(PacketDistributor.PacketTarget target, Object message) {
		if (message == null) {
			return;
		}
		Registration<?> registration = byType.get(message.getClass());
		if (registration == null) {
			throw new IllegalStateException("Unregistered Solo Leveling packet "
					+ message.getClass().getName());
		}
		target.send(new LegacyPayload(registration.id(), message));
	}

	private void handle(LegacyPayload payload, IPayloadContext payloadContext) {
		Registration<?> registration = byId.get(payload.messageId());
		if (registration == null) {
			SololevelingMod.LOGGER.warn("Ignoring unknown Solo Leveling packet id {}",
					payload.messageId());
			return;
		}
		NetworkDirection actualDirection = payloadContext.flow() == PacketFlow.SERVERBOUND
				? NetworkDirection.PLAY_TO_SERVER : NetworkDirection.PLAY_TO_CLIENT;
		if (registration.direction() != null && registration.direction() != actualDirection) {
			SololevelingMod.LOGGER.warn("Ignoring {} received in the wrong direction ({})",
					registration.type().getSimpleName(), actualDirection);
			return;
		}
		dispatch(registration, payload.message(), new NetworkEvent.Context(payloadContext));
	}

	@SuppressWarnings("unchecked")
	private static <T> void dispatch(Registration<T> registration, Object message,
			NetworkEvent.Context context) {
		registration.consumer().accept((T) message, () -> context);
	}

	private record Registration<T>(int id, Class<T> type,
			BiConsumer<T, FriendlyByteBuf> encoder,
			Function<FriendlyByteBuf, T> decoder,
			BiConsumer<T, Supplier<NetworkEvent.Context>> consumer,
			NetworkDirection direction) {
	}

	public record LegacyPayload(int messageId, Object message) implements CustomPacketPayload {
		public static final Type<LegacyPayload> TYPE = new Type<>(
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "legacy_message"));
		public static final StreamCodec<RegistryFriendlyByteBuf, LegacyPayload> STREAM_CODEC = StreamCodec.of(
				LegacyPayload::encode, LegacyPayload::decode);

		private static void encode(RegistryFriendlyByteBuf buffer, LegacyPayload payload) {
			buffer.writeVarInt(payload.messageId);
			Registration<Object> registration = registration(payload.messageId);
			registration.encoder().accept(payload.message, buffer);
		}

		private static LegacyPayload decode(RegistryFriendlyByteBuf buffer) {
			int id = buffer.readVarInt();
			Registration<Object> registration = registration(id);
			return new LegacyPayload(id, registration.decoder().apply(buffer));
		}

		@SuppressWarnings("unchecked")
		private static Registration<Object> registration(int id) {
			Registration<?> registration = SololevelingMod.PACKET_HANDLER.byId.get(id);
			if (registration == null) {
				throw new IllegalStateException("Unknown Solo Leveling packet id " + id);
			}
			return (Registration<Object>) registration;
		}

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}
}
