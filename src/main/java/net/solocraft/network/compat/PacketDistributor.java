package net.solocraft.network.compat;

import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Source-compatible target selectors backed by NeoForge's payload distributor.
 */
public final class PacketDistributor {
	public static final TargetFactory<ServerPlayer> PLAYER = new TargetFactory<>(player ->
			payload -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, payload));
	public static final TargetFactory<Entity> TRACKING_ENTITY_AND_SELF = new TargetFactory<>(entity ->
			payload -> net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity, payload));
	public static final TargetFactory<ResourceKey<Level>> DIMENSION = new TargetFactory<>(dimension -> payload -> {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		ServerLevel level = server == null ? null : server.getLevel(dimension);
		if (level != null) {
			net.neoforged.neoforge.network.PacketDistributor.sendToPlayersInDimension(level, payload);
		}
	});
	public static final TargetFactory<TargetPoint> NEAR = new TargetFactory<>(point -> payload -> {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		ServerLevel level = server == null ? null : server.getLevel(point.dimension());
		if (level != null) {
			net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(level, null,
					point.x(), point.y(), point.z(), point.radius(), payload);
		}
	});
	public static final NoArgTarget ALL = new NoArgTarget(() ->
			payload -> net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(payload));
	public static final NoArgTarget SERVER = new NoArgTarget(() ->
			payload -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(payload));

	private PacketDistributor() {
	}

	@FunctionalInterface
	public interface PacketTarget {
		void send(LegacyNetworkChannel.LegacyPayload payload);
	}

	public static final class TargetFactory<T> {
		private final Function<T, PacketTarget> factory;

		private TargetFactory(Function<T, PacketTarget> factory) {
			this.factory = factory;
		}

		public PacketTarget with(Supplier<? extends T> value) {
			return factory.apply(value.get());
		}

		public PacketTarget with(T value) {
			return factory.apply(value);
		}
	}

	public static final class NoArgTarget {
		private final Supplier<PacketTarget> factory;

		private NoArgTarget(Supplier<PacketTarget> factory) {
			this.factory = factory;
		}

		public PacketTarget noArg() {
			return factory.get();
		}
	}

	public record TargetPoint(double x, double y, double z, double radius,
			ResourceKey<Level> dimension) {
		public static TargetPoint p(double x, double y, double z, double radius,
				ResourceKey<Level> dimension) {
			return new TargetPoint(x, y, z, radius, dimension);
		}
	}
}
