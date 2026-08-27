package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.AntaresVfxClientState;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.solocraft.network.compat.DistExecutor;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.solocraft.network.compat.NetworkDirection;
import net.solocraft.network.compat.NetworkEvent;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/** Compact server-authored visual facts for Antares's combat kit. */
@EventBusSubscriber(modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.MOD)
public final class AntaresVfxEventMessage {
	private static final double WORLD_LIMIT = 30_000_000.0D;
	// Must cover the manifested Extinction endpoint so distant victims receive the beam VFX.
	private static final double SEND_RANGE = 384.0D;
	private static boolean registered;

	public static final byte RUIN_SYNC = 0;
	public static final byte CLAW = 1;
	public static final byte BREATH_CHARGE = 2;
	public static final byte BREATH_STREAM = 3;
	public static final byte BREATH_END = 4;
	public static final byte DESCENT_LAUNCH = 5;
	public static final byte DESCENT_IMPACT = 6;
	public static final byte ROAR_CHARGE = 7;
	public static final byte ROAR_RELEASE = 8;
	public static final byte OVERAWED_MARK = 9;
	public static final byte EXTINCTION_CHARGE = 10;
	public static final byte EXTINCTION_PULSE = 11;
	public static final byte EXTINCTION_AFTERMATH = 12;
	public static final byte MANIFESTATION_START = 13;
	public static final byte MANIFESTATION_END = 14;
	public static final int EVENT_TYPE_COUNT = 15;

	public static final int FLAG_ESSENTIAL = 1;
	public static final int FLAG_PRIVATE_CASTER = 1 << 1;
	public static final int FLAG_CONFIRMED_HIT = 1 << 2;
	public static final int FLAG_MANIFESTED = 1 << 3;
	public static final int FLAG_FINISHER = 1 << 4;
	public static final int MAX_DURATION_TICKS = 20 * 60 * 5;
	public static final int MAX_FUTURE_START_TICKS = 100;

	public final byte eventType;
	public final int casterEntityId;
	public final int targetEntityId;
	public final double originX;
	public final double originY;
	public final double originZ;
	public final double focusX;
	public final double focusY;
	public final double focusZ;
	public final short yaw;
	public final short pitch;
	public final long serverStartTick;
	public final int duration;
	public final int seed;
	public final int intensity;
	public final int variant;
	public final int flags;
	public final float radius;

	public AntaresVfxEventMessage(byte eventType, int casterEntityId,
			int targetEntityId, double originX, double originY, double originZ,
			double focusX, double focusY, double focusZ, short yaw, short pitch,
			long serverStartTick, int duration, int seed, int intensity,
			int variant, int flags, float radius) {
		this.eventType = eventType;
		this.casterEntityId = Math.max(0, casterEntityId);
		this.targetEntityId = targetEntityId < 0 ? -1 : targetEntityId;
		this.originX = coordinate(originX);
		this.originY = coordinate(originY);
		this.originZ = coordinate(originZ);
		this.focusX = coordinate(focusX);
		this.focusY = coordinate(focusY);
		this.focusZ = coordinate(focusZ);
		this.yaw = yaw;
		this.pitch = pitch;
		this.serverStartTick = Math.max(0L, serverStartTick);
		this.duration = Mth.clamp(duration, 1, MAX_DURATION_TICKS);
		this.seed = seed;
		this.intensity = Mth.clamp(intensity, 0, 255);
		this.variant = Mth.clamp(variant, 0, 31);
		this.flags = flags & 0xFF;
		this.radius = Mth.clamp(Float.isFinite(radius) ? radius : 1.0F,
				0.1F, 48.0F);
	}

	public static AntaresVfxEventMessage create(byte type, Entity caster,
			@Nullable Entity target, Vec3 origin, Vec3 focus, int duration,
			int seed, int intensity, int variant, int flags, float radius) {
		Vec3 safeOrigin = origin == null ? caster.position() : origin;
		Vec3 safeFocus = focus == null ? safeOrigin : focus;
		return new AntaresVfxEventMessage(type, caster.getId(),
				target == null ? -1 : target.getId(), safeOrigin.x, safeOrigin.y,
				safeOrigin.z, safeFocus.x, safeFocus.y, safeFocus.z,
				packRotation(caster.getYRot()), packRotation(caster.getXRot()),
				caster.level().getGameTime(), duration, seed, intensity,
				variant, flags, radius);
	}

	public Vec3 origin() {
		return new Vec3(originX, originY, originZ);
	}

	public Vec3 focus() {
		return new Vec3(focusX, focusY, focusZ);
	}

	public float yawDegrees() {
		return unpackRotation(yaw);
	}

	public float pitchDegrees() {
		return unpackRotation(pitch);
	}

	public boolean hasFlag(int flag) {
		return (flags & flag) != 0;
	}

	public boolean privateToCaster() {
		return hasFlag(FLAG_PRIVATE_CASTER);
	}

	public static boolean isKnownEventType(byte type) {
		return (type & 0xFF) < EVENT_TYPE_COUNT;
	}

	public static void encode(AntaresVfxEventMessage message,
			FriendlyByteBuf buffer) {
		buffer.writeByte(message.eventType);
		buffer.writeVarInt(message.casterEntityId);
		buffer.writeVarInt(message.targetEntityId < 0 ? 0
				: message.targetEntityId + 1);
		buffer.writeDouble(message.originX);
		buffer.writeDouble(message.originY);
		buffer.writeDouble(message.originZ);
		buffer.writeDouble(message.focusX);
		buffer.writeDouble(message.focusY);
		buffer.writeDouble(message.focusZ);
		buffer.writeShort(message.yaw);
		buffer.writeShort(message.pitch);
		buffer.writeLong(message.serverStartTick);
		buffer.writeVarInt(message.duration);
		buffer.writeInt(message.seed);
		buffer.writeByte(message.intensity);
		buffer.writeByte(message.variant);
		buffer.writeByte(message.flags);
		buffer.writeFloat(message.radius);
	}

	public static AntaresVfxEventMessage decode(FriendlyByteBuf buffer) {
		byte type = buffer.readByte();
		int caster = buffer.readVarInt();
		int encodedTarget = buffer.readVarInt();
		return new AntaresVfxEventMessage(type, caster,
				encodedTarget == 0 ? -1 : encodedTarget - 1,
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readShort(), buffer.readShort(), buffer.readLong(),
				buffer.readVarInt(), buffer.readInt(), buffer.readUnsignedByte(),
				buffer.readUnsignedByte(), buffer.readUnsignedByte(),
				buffer.readFloat());
	}

	public static void handle(AntaresVfxEventMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> AntaresVfxClientState.enqueue(message)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static synchronized void register(FMLCommonSetupEvent event) {
		if (registered)
			return;
		registered = true;
		SololevelingMod.addNetworkMessage(AntaresVfxEventMessage.class,
				AntaresVfxEventMessage::encode, AntaresVfxEventMessage::decode,
				AntaresVfxEventMessage::handle, NetworkDirection.PLAY_TO_CLIENT);
	}

	public static void sendRuin(ServerPlayer caster, int charges, int maximum,
			boolean manifested) {
		if (!valid(caster))
			return;
		int flags = FLAG_PRIVATE_CASTER | FLAG_ESSENTIAL
				| (manifested ? FLAG_MANIFESTED : 0);
		sendTo(caster, create(RUIN_SYNC, caster, null, caster.position(),
				caster.position(), 40, caster.tickCount * 31, 255,
				Mth.clamp(charges, 0, 31), flags, Math.max(1, maximum)));
	}

	public static void sendClaw(ServerPlayer caster, Vec3 origin, Vec3 focus,
			boolean finisher, boolean hit, int seed) {
		int flags = FLAG_ESSENTIAL | (finisher ? FLAG_FINISHER : 0)
				| (hit ? FLAG_CONFIRMED_HIT : 0);
		sendNear(caster, create(CLAW, caster, null, origin, focus, 13, seed,
				finisher ? 255 : 210, finisher ? 1 : 0, flags,
				finisher ? 2.6F : 1.65F));
	}

	public static void sendBreathCharge(ServerPlayer caster, Vec3 origin,
			Vec3 focus, int duration, boolean manifested, int seed) {
		sendNear(caster, create(BREATH_CHARGE, caster, null, origin, focus,
				duration, seed, manifested ? 235 : 205, 0,
				FLAG_ESSENTIAL | manifestedFlag(manifested),
				manifested ? 1.8F : 1.4F));
	}

	public static void sendBreathStream(ServerPlayer caster, Vec3 origin,
			Vec3 focus, boolean manifested, boolean hit, int seed) {
		int flags = FLAG_ESSENTIAL | manifestedFlag(manifested)
				| (hit ? FLAG_CONFIRMED_HIT : 0);
		sendNear(caster, create(BREATH_STREAM, caster, null, origin, focus,
				7, seed, manifested ? 245 : 220, 0, flags,
				manifested ? 1.8F : 1.4F));
	}

	public static void sendBreathEnd(ServerPlayer caster, Vec3 origin,
			Vec3 focus, int seed) {
		sendNear(caster, create(BREATH_END, caster, null, origin, focus,
				10, seed, 150, 0, FLAG_ESSENTIAL, 1.4F));
	}

	public static void sendDescentLaunch(ServerPlayer caster,
			boolean manifested, int seed) {
		sendNear(caster, create(DESCENT_LAUNCH, caster, null,
				caster.position(), caster.position(), 45, seed,
				manifested ? 240 : 210, 0,
				FLAG_ESSENTIAL | manifestedFlag(manifested), 2.5F));
	}

	public static void sendDescentImpact(ServerPlayer caster, Vec3 center,
			float radius, boolean manifested, boolean hit, int seed) {
		int flags = FLAG_ESSENTIAL | manifestedFlag(manifested)
				| (hit ? FLAG_CONFIRMED_HIT : 0);
		sendNear(caster, create(DESCENT_IMPACT, caster, null, center, center,
				34, seed, manifested ? 255 : 225, 0, flags, radius));
	}

	public static void sendRoarCharge(ServerPlayer caster, boolean manifested,
			int seed) {
		sendNear(caster, create(ROAR_CHARGE, caster, null, caster.position(),
				caster.position(), 7, seed, manifested ? 240 : 210, 0,
				FLAG_ESSENTIAL | manifestedFlag(manifested), 3.4F));
	}

	public static void sendRoarRelease(ServerPlayer caster, Vec3 center,
			float radius, boolean manifested, boolean hit, int seed) {
		int flags = FLAG_ESSENTIAL | manifestedFlag(manifested)
				| (hit ? FLAG_CONFIRMED_HIT : 0);
		sendNear(caster, create(ROAR_RELEASE, caster, null, center, center,
				24, seed, manifested ? 255 : 225, 0, flags, radius));
	}

	public static void sendOverawedMark(ServerPlayer caster, LivingEntity target,
			int duration, int seed) {
		sendNear(caster, create(OVERAWED_MARK, caster, target, target.position(),
				target.position(), duration, seed, 205, 0, 0,
				Mth.clamp(target.getBbHeight(), 0.8F, 4.0F)));
	}

	public static void sendExtinctionCharge(ServerPlayer caster, Vec3 origin,
			Vec3 focus, boolean manifested, int seed) {
		sendNear(caster, create(EXTINCTION_CHARGE, caster, null, origin, focus,
				20, seed, manifested ? 255 : 235, 0,
				FLAG_ESSENTIAL | manifestedFlag(manifested),
				manifested ? 2.9F : 2.35F));
	}

	public static void sendExtinctionPulse(ServerPlayer caster, Vec3 origin,
			Vec3 focus, int pulse, boolean manifested, boolean hit, int seed) {
		int flags = FLAG_ESSENTIAL | manifestedFlag(manifested)
				| (hit ? FLAG_CONFIRMED_HIT : 0);
		sendNear(caster, create(EXTINCTION_PULSE, caster, null, origin, focus,
				14, seed, 255, Mth.clamp(pulse, 0, 2), flags,
				manifested ? 2.9F : 2.35F));
	}

	public static void sendExtinctionAftermath(ServerPlayer caster, Vec3 origin,
			Vec3 focus, boolean manifested, int seed) {
		sendNear(caster, create(EXTINCTION_AFTERMATH, caster, null, origin, focus,
				70, seed, manifested ? 235 : 205, 0,
				FLAG_ESSENTIAL | manifestedFlag(manifested),
				manifested ? 4.8F : 4.0F));
	}

	public static void sendManifestation(ServerPlayer caster, boolean active,
			int seed) {
		sendNear(caster, create(active ? MANIFESTATION_START : MANIFESTATION_END,
				caster, null, caster.position(), caster.position(), active ? 28 : 14,
				seed, active ? 255 : 175, 0, FLAG_ESSENTIAL,
				active ? 3.2F : 2.4F));
	}

	private static int manifestedFlag(boolean manifested) {
		return manifested ? FLAG_MANIFESTED : 0;
	}

	private static void sendNear(ServerPlayer caster,
			AntaresVfxEventMessage message) {
		if (!valid(caster))
			return;
		ServerLevel level = caster.serverLevel();
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.NEAR.with(
				() -> new PacketDistributor.TargetPoint(message.originX,
						message.originY, message.originZ, SEND_RANGE,
						level.dimension())), message);
	}

	private static void sendTo(ServerPlayer player,
			AntaresVfxEventMessage message) {
		if (valid(player))
			SololevelingMod.PACKET_HANDLER.send(
					PacketDistributor.PLAYER.with(() -> player), message);
	}

	private static boolean valid(ServerPlayer player) {
		return player != null && player.connection != null && player.level() != null;
	}

	private static short packRotation(float degrees) {
		float safe = Float.isFinite(degrees) ? degrees : 0.0F;
		return (short) Mth.floor(safe * 65536.0F / 360.0F);
	}

	private static float unpackRotation(short packed) {
		return (packed & 0xFFFF) * (360.0F / 65536.0F);
	}

	private static double coordinate(double value) {
		if (!Double.isFinite(value))
			return 0.0D;
		return Mth.clamp(value, -WORLD_LIMIT, WORLD_LIMIT);
	}
}
