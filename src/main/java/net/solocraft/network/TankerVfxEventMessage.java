package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.TankerVfxRenderer;
import net.solocraft.util.TankerSkillManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.function.Supplier;

/**
 * One compact, server-authored visual event for all six Tanker skills.
 *
 * <p>The packet carries visual facts only. Damage, mitigation, target acceptance,
 * movement, Strain, integrity, and cooldown decisions remain server-side.</p>
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TankerVfxEventMessage {
	private static boolean registered;

	// Tank Leap
	public static final byte LEAP_START = 0;
	public static final byte LEAP_LAND = 1;
	// Taunt
	public static final byte TAUNT_RING = 2;
	// Shield Bash
	public static final byte BASH_SWEEP = 3;
	public static final byte BASH_HIT = 4;
	public static final byte BASH_STRAIN_RELIEF = 5;
	// Reinforcement
	public static final byte REINFORCEMENT_BRACE_START = 6;
	public static final byte REINFORCEMENT_BRACE_HIT = 7;
	public static final byte REINFORCEMENT_STANCE_START = 8;
	public static final byte REINFORCEMENT_STANCE_END = 9;
	// Willpower
	public static final byte WILLPOWER_START = 10;
	public static final byte WILLPOWER_STRAIN_THRESHOLD = 11;
	public static final byte WILLPOWER_SETTLE = 12;
	public static final byte WILLPOWER_BREAK = 13;
	// Protection Mark
	public static final byte MARK_DEPLOY = 14;
	public static final byte MARK_INTEGRITY_THRESHOLD = 15;
	public static final byte MARK_BREAK = 16;
	public static final byte MARK_CANCEL = 17;

	public static final int EVENT_TYPE_COUNT = 18;

	public static final int FLAG_ESSENTIAL = 1;
	public static final int FLAG_CONFIRMED_HIT = 1 << 1;
	public static final int FLAG_PVP = 1 << 2;
	/** Suppresses initial audio when replaying an already-active state to a tracker. */
	public static final int FLAG_REPLAY = 1 << 3;
	public static final int FLAG_SILENT = 1 << 4;

	public static final int INTENSITY_25 = 64;
	public static final int INTENSITY_50 = 128;
	public static final int INTENSITY_75 = 192;
	public static final int INTENSITY_100 = 255;

	public static final int LEAP_START_TICKS = 12;
	public static final int LEAP_LAND_TICKS = 10;
	public static final int TAUNT_TICKS = 120;
	public static final int BASH_SWEEP_TICKS = 6;
	public static final int BASH_ACCENT_TICKS = 6;
	public static final int REINFORCEMENT_BRACE_TICKS = 12;
	public static final int REINFORCEMENT_STANCE_TICKS = 80;
	public static final int REINFORCEMENT_END_TICKS = 6;
	public static final int WILLPOWER_TICKS = 160;
	public static final int WILLPOWER_THRESHOLD_TICKS = 8;
	public static final int WILLPOWER_SETTLE_PULSE_TICKS = 10;
	public static final int WILLPOWER_BREAK_TICKS = 8;
	public static final int MARK_TICKS = 240;
	public static final int MARK_THRESHOLD_TICKS = 8;
	public static final int MARK_END_TICKS = 8;

	public static final double TAUNT_RADIUS = 12.0D;
	public static final double BASH_REACH = 3.6D;
	public static final double LEAP_LAND_RADIUS = 5.0D;
	public static final double MARK_RADIUS = 6.0D;
	public static final double DEFAULT_SEND_RANGE = 64.0D;
	public static final int MAX_DURATION_TICKS = 1200;

	public final byte eventType;
	public final int ownerEntityId;
	public final int targetEntityId;
	public final double originX;
	public final double originY;
	public final double originZ;
	public final short yaw;
	public final short pitch;
	public final long serverStartTick;
	public final int duration;
	public final int seed;
	public final int intensity;
	public final int flags;

	public TankerVfxEventMessage(byte eventType, int ownerEntityId, int targetEntityId,
			double originX, double originY, double originZ, short yaw, short pitch,
			long serverStartTick, int duration, int seed, int intensity, int flags) {
		this.eventType = eventType;
		this.ownerEntityId = Math.max(0, ownerEntityId);
		this.targetEntityId = targetEntityId < 0
				? -1
				: Math.min(targetEntityId, Integer.MAX_VALUE - 1);
		this.originX = finite(originX);
		this.originY = finite(originY);
		this.originZ = finite(originZ);
		this.yaw = yaw;
		this.pitch = pitch;
		this.serverStartTick = serverStartTick;
		this.duration = Mth.clamp(duration, 1, MAX_DURATION_TICKS);
		this.seed = seed;
		this.intensity = Mth.clamp(intensity, 0, 255);
		this.flags = flags & 0xFF;
	}

	/**
	 * Server convenience factory. Pass {@code null} for an event without a target.
	 */
	public static TankerVfxEventMessage create(byte eventType, Entity owner, Entity target,
			Vec3 origin, float yaw, float pitch, long serverStartTick, int duration,
			int seed, int intensity, int flags) {
		return new TankerVfxEventMessage(eventType, owner.getId(),
				target == null ? -1 : target.getId(), origin.x, origin.y, origin.z,
				packRotation(yaw), packRotation(pitch), serverStartTick, duration,
				seed, intensity, flags);
	}

	/** Uses the canonical client duration for the supplied event type. */
	public static TankerVfxEventMessage create(byte eventType, Entity owner, Entity target,
			Vec3 origin, float yaw, float pitch, long serverStartTick, int seed,
			int intensity, int flags) {
		return create(eventType, owner, target, origin, yaw, pitch, serverStartTick,
				defaultDuration(eventType), seed, intensity, flags);
	}

	public Vec3 origin() {
		return new Vec3(originX, originY, originZ);
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

	public static short packRotation(float degrees) {
		return (short) Mth.floor(degrees * 65536.0F / 360.0F);
	}

	public static float unpackRotation(short packed) {
		return (packed & 0xFFFF) * (360.0F / 65536.0F);
	}

	public static int defaultDuration(byte eventType) {
		return switch (eventType) {
			case LEAP_START -> LEAP_START_TICKS;
			case LEAP_LAND -> LEAP_LAND_TICKS;
			case TAUNT_RING -> TAUNT_TICKS;
			case BASH_SWEEP -> BASH_SWEEP_TICKS;
			case BASH_HIT, BASH_STRAIN_RELIEF, REINFORCEMENT_BRACE_HIT -> BASH_ACCENT_TICKS;
			case REINFORCEMENT_BRACE_START -> REINFORCEMENT_BRACE_TICKS;
			case REINFORCEMENT_STANCE_START -> REINFORCEMENT_STANCE_TICKS;
			case REINFORCEMENT_STANCE_END -> REINFORCEMENT_END_TICKS;
			case WILLPOWER_START -> WILLPOWER_TICKS;
			case WILLPOWER_STRAIN_THRESHOLD -> WILLPOWER_THRESHOLD_TICKS;
			case WILLPOWER_SETTLE -> WILLPOWER_SETTLE_PULSE_TICKS;
			case WILLPOWER_BREAK -> WILLPOWER_BREAK_TICKS;
			case MARK_DEPLOY -> MARK_TICKS;
			case MARK_INTEGRITY_THRESHOLD -> MARK_THRESHOLD_TICKS;
			case MARK_BREAK, MARK_CANCEL -> MARK_END_TICKS;
			default -> 1;
		};
	}

	public static boolean isKnownEventType(byte eventType) {
		int unsigned = eventType & 0xFF;
		return unsigned < EVENT_TYPE_COUNT;
	}

	public static void encode(TankerVfxEventMessage message, FriendlyByteBuf buffer) {
		buffer.writeByte(message.eventType);
		buffer.writeVarInt(message.ownerEntityId);
		buffer.writeVarInt(message.targetEntityId < 0 ? 0 : message.targetEntityId + 1);
		buffer.writeDouble(message.originX);
		buffer.writeDouble(message.originY);
		buffer.writeDouble(message.originZ);
		buffer.writeShort(message.yaw);
		buffer.writeShort(message.pitch);
		buffer.writeLong(message.serverStartTick);
		buffer.writeVarInt(message.duration);
		buffer.writeInt(message.seed);
		buffer.writeByte(message.intensity);
		buffer.writeByte(message.flags);
	}

	public static TankerVfxEventMessage decode(FriendlyByteBuf buffer) {
		byte eventType = buffer.readByte();
		int ownerEntityId = buffer.readVarInt();
		int encodedTarget = buffer.readVarInt();
		int targetEntityId = encodedTarget <= 0 ? -1 : encodedTarget - 1;
		return new TankerVfxEventMessage(eventType, ownerEntityId, targetEntityId,
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readShort(), buffer.readShort(), buffer.readLong(),
				buffer.readVarInt(), buffer.readInt(), buffer.readUnsignedByte(),
				buffer.readUnsignedByte());
	}

	public static void handle(TankerVfxEventMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> TankerVfxRenderer.enqueue(message)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static synchronized void register(FMLCommonSetupEvent event) {
		if (registered)
			return;
		registered = true;
		SololevelingMod.addNetworkMessage(TankerVfxEventMessage.class,
				TankerVfxEventMessage::encode, TankerVfxEventMessage::decode,
				TankerVfxEventMessage::handle, NetworkDirection.PLAY_TO_CLIENT);
		TankerSkillManager.installVfxSink(TankerVfxEventMessage::sendManagerEvent);
	}

	private static void sendManagerEvent(ServerLevel level,
			TankerSkillManager.VfxEvent event) {
		byte packetEvent = switch (event.eventType()) {
			case TankerSkillManager.VFX_LEAP_START -> LEAP_START;
			case TankerSkillManager.VFX_LEAP_LAND -> LEAP_LAND;
			case TankerSkillManager.VFX_TAUNT_RING -> TAUNT_RING;
			case TankerSkillManager.VFX_BASH_SWEEP -> BASH_SWEEP;
			case TankerSkillManager.VFX_BASH_HIT -> BASH_HIT;
			case TankerSkillManager.VFX_BASH_STRAIN_RELIEF -> BASH_STRAIN_RELIEF;
			case TankerSkillManager.VFX_BRACE_START -> REINFORCEMENT_BRACE_START;
			case TankerSkillManager.VFX_BRACE_HIT -> REINFORCEMENT_BRACE_HIT;
			case TankerSkillManager.VFX_STANCE_START -> REINFORCEMENT_STANCE_START;
			case TankerSkillManager.VFX_STANCE_END -> REINFORCEMENT_STANCE_END;
			case TankerSkillManager.VFX_WILLPOWER_START -> WILLPOWER_START;
			case TankerSkillManager.VFX_WILLPOWER_THRESHOLD -> WILLPOWER_STRAIN_THRESHOLD;
			case TankerSkillManager.VFX_WILLPOWER_SETTLE -> WILLPOWER_SETTLE;
			case TankerSkillManager.VFX_WILLPOWER_BREAK -> WILLPOWER_BREAK;
			case TankerSkillManager.VFX_MARK_DEPLOY -> MARK_DEPLOY;
			case TankerSkillManager.VFX_MARK_THRESHOLD -> MARK_INTEGRITY_THRESHOLD;
			case TankerSkillManager.VFX_MARK_BREAK -> MARK_BREAK;
			case TankerSkillManager.VFX_MARK_CANCEL -> MARK_CANCEL;
			default -> -1;
		};
		if (packetEvent < 0)
			return;
		sendNear(level, new TankerVfxEventMessage(packetEvent,
				event.ownerEntityId(), event.targetEntityId(),
				event.x(), event.y(), event.z(), event.yaw(), event.pitch(),
				event.serverStartTick(), event.duration(), event.seed(),
				event.intensity(), event.flags()));
	}

	/** Broadcasts a visual event near its authored origin in the same dimension. */
	public static void sendNear(ServerLevel level, TankerVfxEventMessage message) {
		sendNear(level, DEFAULT_SEND_RANGE, message);
	}

	public static void sendNear(ServerLevel level, double range,
			TankerVfxEventMessage message) {
		double boundedRange = Mth.clamp(range, 1.0D, 128.0D);
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.NEAR.with(
				PacketDistributor.TargetPoint.p(message.originX, message.originY,
						message.originZ, boundedRange, level.dimension())), message);
	}

	/** Sends an active-state replay to a player who began tracking it late. */
	public static void sendTo(ServerPlayer player, TankerVfxEventMessage message) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				message);
	}

	/** Useful for owner-bound casts when the owner's tracking set is the desired audience. */
	public static void sendTrackingAndSelf(Entity owner, TankerVfxEventMessage message) {
		SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> owner), message);
	}

	private static double finite(double value) {
		return Double.isFinite(value) ? value : 0.0D;
	}
}
