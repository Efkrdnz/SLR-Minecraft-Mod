package net.solocraft.network;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.SungIlHwanVfxClientState;

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
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * Compact server-authored presentation facts for Sung Il-Hwan's vessel kit.
 *
 * <p>This packet never decides hits, targets, charge success, damage, stage,
 * exhaustion, or risk. Those facts are authored by the server-side combat
 * manager. Clients only reconstruct bounded visual timelines from the supplied
 * positions, start tick, seed, and duration.</p>
 *
 * <p>The public Spatial Execution charge packet intentionally contains no aim
 * point. The full targeting sphere and target mark are sent in a separate
 * owner-only packet so opponents receive a readable charge tell without seeing
 * the caster's private targeting HUD.</p>
 */
@EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.MOD
)
public final class SungIlHwanVfxEventMessage {
	private static final double WORLD_LIMIT = 30_000_000.0D;
	private static boolean registered;

	public static final byte STAGE_ONE = 0;
	public static final byte STAGE_TWO = 1;
	public static final byte STAGE_END = 2;
	public static final byte FEAR_PULSE = 3;
	public static final byte FEAR_MARK = 4;
	public static final byte SPATIAL_SLASH = 5;
	public static final byte EXECUTION_PUBLIC_CHARGE = 6;
	public static final byte EXECUTION_PRIVATE_TARGET = 7;
	public static final byte EXECUTION_RELEASE = 8;
	public static final byte EXECUTION_FRACTURE = 9;
	public static final byte EXECUTION_CANCEL = 10;
	public static final byte EXHAUSTION = 11;
	public static final byte RISK_FEEDBACK = 12;
	public static final int EVENT_TYPE_COUNT = 13;

	public static final int STAGE_NONE_VALUE = 0;
	public static final int STAGE_ONE_VALUE = 1;
	public static final int STAGE_TWO_VALUE = 2;

	public static final int FLAG_ESSENTIAL = 1;
	public static final int FLAG_PRIVATE_CASTER = 1 << 1;
	public static final int FLAG_CONFIRMED_HIT = 1 << 2;
	public static final int FLAG_REPLAY = 1 << 3;
	public static final int FLAG_SILENT = 1 << 4;
	public static final int FLAG_HIGH_RISK = 1 << 5;

	public static final int DEFAULT_STAGE_TICKS = 20 * 60;
	public static final int FEAR_PULSE_TICKS = 24;
	public static final int FEAR_MARK_TICKS = 80;
	public static final int SPATIAL_SLASH_TICKS = 13;
	public static final int EXECUTION_CHARGE_TICKS = 20 * 5;
	/**
	 * Long enough to bridge the authored one-second suspension before the
	 * simultaneous fracture packet begins.
	 */
	public static final int EXECUTION_RELEASE_TICKS = 42;
	public static final int EXECUTION_FRACTURE_TICKS = 18;
	public static final int EXECUTION_CANCEL_TICKS = 9;
	public static final int EXHAUSTION_TICKS = 50;
	public static final int RISK_TICKS = 36;
	public static final int MAX_DURATION_TICKS = 20 * 60 * 20;
	public static final int MAX_FUTURE_START_TICKS = 100;
	public static final double DEFAULT_SEND_RANGE = 80.0D;

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

	public SungIlHwanVfxEventMessage(byte eventType, int casterEntityId,
			int targetEntityId, double originX, double originY, double originZ,
			double focusX, double focusY, double focusZ, short yaw, short pitch,
			long serverStartTick, int duration, int seed, int intensity,
			int variant, int flags, float radius) {
		this.eventType = eventType;
		this.casterEntityId = Math.max(0, casterEntityId);
		this.targetEntityId = targetEntityId < 0
				? -1 : Math.min(targetEntityId, Integer.MAX_VALUE - 1);
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
		this.variant = Mth.clamp(variant, 0, 15);
		this.flags = flags & 0xFF;
		this.radius = Mth.clamp(Float.isFinite(radius) ? radius : 1.0F,
				0.25F, 32.0F);
	}

	public static SungIlHwanVfxEventMessage create(byte eventType, Entity caster,
			@Nullable Entity target, Vec3 origin, Vec3 focus, float yaw, float pitch,
			long serverStartTick, int duration, int seed, int intensity,
			int variant, int flags, float radius) {
		Vec3 safeOrigin = origin == null ? caster.position() : origin;
		Vec3 safeFocus = focus == null ? safeOrigin : focus;
		return new SungIlHwanVfxEventMessage(eventType, caster.getId(),
				target == null ? -1 : target.getId(),
				safeOrigin.x, safeOrigin.y, safeOrigin.z,
				safeFocus.x, safeFocus.y, safeFocus.z,
				packRotation(yaw), packRotation(pitch), serverStartTick,
				duration, seed, intensity, variant, flags, radius);
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

	public static boolean isKnownEventType(byte eventType) {
		return (eventType & 0xFF) < EVENT_TYPE_COUNT;
	}

	public static short packRotation(float degrees) {
		float safe = Float.isFinite(degrees) ? degrees : 0.0F;
		return (short) Mth.floor(safe * 65536.0F / 360.0F);
	}

	public static float unpackRotation(short packed) {
		return (packed & 0xFFFF) * (360.0F / 65536.0F);
	}

	public static void encode(SungIlHwanVfxEventMessage message,
			FriendlyByteBuf buffer) {
		buffer.writeByte(message.eventType);
		buffer.writeVarInt(message.casterEntityId);
		buffer.writeVarInt(message.targetEntityId < 0
				? 0 : message.targetEntityId + 1);
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

	public static SungIlHwanVfxEventMessage decode(FriendlyByteBuf buffer) {
		byte eventType = buffer.readByte();
		int casterEntityId = buffer.readVarInt();
		int encodedTarget = buffer.readVarInt();
		int targetEntityId = encodedTarget <= 0 ? -1 : encodedTarget - 1;
		return new SungIlHwanVfxEventMessage(eventType, casterEntityId,
				targetEntityId, buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
				buffer.readDouble(), buffer.readShort(), buffer.readShort(),
				buffer.readLong(), buffer.readVarInt(), buffer.readInt(),
				buffer.readUnsignedByte(), buffer.readUnsignedByte(),
				buffer.readUnsignedByte(), buffer.readFloat());
	}

	public static void handle(SungIlHwanVfxEventMessage message,
			Supplier<NetworkEvent.Context> contextSupplier) {
		NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
				() -> () -> SungIlHwanVfxClientState.enqueue(message)));
		context.setPacketHandled(true);
	}

	@SubscribeEvent
	public static synchronized void register(FMLCommonSetupEvent event) {
		if (registered)
			return;
		registered = true;
		SololevelingMod.addNetworkMessage(SungIlHwanVfxEventMessage.class,
				SungIlHwanVfxEventMessage::encode,
				SungIlHwanVfxEventMessage::decode,
				SungIlHwanVfxEventMessage::handle,
				NetworkDirection.PLAY_TO_CLIENT);
	}

	/**
	 * Starts or refreshes the public body presentation for Stage I or Stage II.
	 */
	public static void sendStage(ServerPlayer caster, int stage,
			int durationTicks, int seed) {
		if (!validCaster(caster))
			return;
		int boundedStage = Mth.clamp(stage, STAGE_ONE_VALUE, STAGE_TWO_VALUE);
		byte type = boundedStage == STAGE_TWO_VALUE ? STAGE_TWO : STAGE_ONE;
		int intensity = boundedStage == STAGE_TWO_VALUE ? 255 : 176;
		int flags = FLAG_ESSENTIAL;
		SungIlHwanVfxEventMessage message = create(type, caster, null,
				caster.position(), caster.position(), caster.getYRot(),
				caster.getXRot(), caster.serverLevel().getGameTime(),
				durationTicks <= 0 ? DEFAULT_STAGE_TICKS : durationTicks,
				seed, intensity, boundedStage, flags,
				boundedStage == STAGE_TWO_VALUE ? 2.4F : 1.8F);
		sendTrackingAndSelf(caster, message);
	}

	public static void sendStageEnd(ServerPlayer caster, int seed) {
		if (!validCaster(caster))
			return;
		sendTrackingAndSelf(caster, create(STAGE_END, caster, null,
				caster.position(), caster.position(), caster.getYRot(),
				caster.getXRot(), caster.serverLevel().getGameTime(), 12,
				seed, 144, STAGE_NONE_VALUE, FLAG_ESSENTIAL, 1.8F));
	}

	public static void sendFearPulse(ServerPlayer caster, Vec3 origin,
			double radius, int seed) {
		if (!validCaster(caster))
			return;
		Vec3 safeOrigin = origin == null ? caster.position() : origin;
		sendNear(caster.serverLevel(), DEFAULT_SEND_RANGE,
				create(FEAR_PULSE, caster, null, safeOrigin, safeOrigin,
						caster.getYRot(), 0.0F,
						caster.serverLevel().getGameTime(), FEAR_PULSE_TICKS,
						seed, 220, 0, FLAG_ESSENTIAL,
						(float) radius));
	}

	public static void sendFearMark(ServerPlayer caster, @Nullable Entity target,
			int durationTicks, int seed) {
		if (!validCaster(caster) || target == null
				|| target.level() != caster.level())
			return;
		Vec3 focus = target.position().add(0.0D,
				Math.max(0.75D, target.getBbHeight() * 0.72D), 0.0D);
		sendNear(caster.serverLevel(), DEFAULT_SEND_RANGE,
				create(FEAR_MARK, caster, target, caster.position(), focus,
						caster.getYRot(), caster.getXRot(),
						caster.serverLevel().getGameTime(),
						durationTicks <= 0 ? FEAR_MARK_TICKS : durationTicks,
						seed, 205, 0, FLAG_ESSENTIAL,
						Math.max(0.65F, target.getBbWidth() * 0.9F)));
	}

	public static void sendSpatialSlash(ServerPlayer caster,
			@Nullable Entity target, Vec3 from, Vec3 to, int comboIndex,
			boolean confirmedHit, int seed) {
		if (!validCaster(caster))
			return;
		Vec3 origin = from == null
				? caster.getEyePosition().add(caster.getLookAngle().scale(0.35D))
				: from;
		Vec3 focus = to == null
				? origin.add(caster.getLookAngle().scale(9.0D)) : to;
		int flags = confirmedHit ? FLAG_CONFIRMED_HIT : 0;
		sendNear(caster.serverLevel(), DEFAULT_SEND_RANGE,
				create(SPATIAL_SLASH, caster, target, origin, focus,
						caster.getYRot(), caster.getXRot(),
						caster.serverLevel().getGameTime(),
						SPATIAL_SLASH_TICKS, seed,
						confirmedHit ? 255 : 205,
						Mth.clamp(comboIndex, 0, 7), flags,
						0.72F + Mth.clamp(comboIndex, 0, 7) * 0.035F));
	}

	/**
	 * Sends a public body tell and a separate owner-only targeting sphere.
	 */
	public static void sendExecutionCharge(ServerPlayer caster, Vec3 focus,
			double radius, int durationTicks, int seed) {
		if (!validCaster(caster))
			return;
		long now = caster.serverLevel().getGameTime();
		int boundedDuration = durationTicks <= 0 ? 60 : durationTicks;
		Vec3 origin = caster.position();
		// Public packet deliberately carries only the caster position.
		sendNear(caster.serverLevel(), DEFAULT_SEND_RANGE,
				create(EXECUTION_PUBLIC_CHARGE, caster, null, origin, origin,
						caster.getYRot(), caster.getXRot(), now,
						boundedDuration, seed, 230, 0,
						FLAG_ESSENTIAL, 2.2F));
		// Spatial Execution grows around the caster. The optional focus is not a
		// sphere center; target locks arrive as separate private mark packets.
		Vec3 privateFocus = origin;
		sendTo(caster, create(EXECUTION_PRIVATE_TARGET, caster, null,
				origin, privateFocus, caster.getYRot(), caster.getXRot(), now,
				boundedDuration, seed, 32, 0,
				FLAG_ESSENTIAL | FLAG_PRIVATE_CASTER, (float) radius));
	}

	/**
	 * Refreshes private targeting facts. Call on target acquisition/change or at a
	 * bounded cadence rather than every server tick.
	 */
	public static void sendExecutionTarget(ServerPlayer caster,
			@Nullable Entity target, Vec3 focus, double radius,
			int remainingTicks, int seed) {
		if (!validCaster(caster))
			return;
		Entity scopedTarget = target != null
				&& target.level() == caster.level() ? target : null;
		Vec3 privateFocus = scopedTarget != null
				? scopedTarget.position().add(0.0D,
						scopedTarget.getBbHeight() * 0.5D, 0.0D)
				: caster.position();
		int chargeProgress = Mth.clamp(255
				- Mth.floor(Math.max(0, remainingTicks) * 255.0F
						/ EXECUTION_CHARGE_TICKS),
				32, 255);
		sendTo(caster, create(EXECUTION_PRIVATE_TARGET, caster, scopedTarget,
				caster.position(), privateFocus, caster.getYRot(),
				caster.getXRot(), caster.serverLevel().getGameTime(),
				Math.max(2, remainingTicks), seed, chargeProgress,
				scopedTarget == null ? 1 : 2,
				FLAG_ESSENTIAL | FLAG_PRIVATE_CASTER, (float) radius));
	}

	public static void sendExecutionRelease(ServerPlayer caster,
			@Nullable Entity target, Vec3 focus, double radius,
			int chargeTier, int seed) {
		if (!validCaster(caster))
			return;
		Vec3 impact = focus == null
				? caster.position()
				: focus;
		// Release is public presentation. Individual locks remain exclusively in
		// owner-only mark packets, so never serialize the first chosen target.
		sendNear(caster.serverLevel(), 112.0D,
				create(EXECUTION_RELEASE, caster, null, caster.position(),
						impact, caster.getYRot(), caster.getXRot(),
						caster.serverLevel().getGameTime(),
						EXECUTION_RELEASE_TICKS, seed,
						190 + Mth.clamp(chargeTier, 0, 3) * 21,
						Mth.clamp(chargeTier, 0, 3), FLAG_ESSENTIAL,
						(float) radius));
	}

	public static void sendExecutionFracture(ServerPlayer caster, Vec3 focus,
			double radius, int delayTicks, int seed) {
		if (!validCaster(caster))
			return;
		Vec3 impact = focus == null ? caster.position() : focus;
		int delay = Mth.clamp(delayTicks, 0, MAX_FUTURE_START_TICKS);
		sendNear(caster.serverLevel(), 112.0D,
				create(EXECUTION_FRACTURE, caster, null, impact, impact,
						caster.getYRot(), 0.0F,
						caster.serverLevel().getGameTime() + delay,
						EXECUTION_FRACTURE_TICKS, seed, 255, 0,
						FLAG_ESSENTIAL, (float) radius));
	}

	public static void sendExecutionCancel(ServerPlayer caster, int seed) {
		if (!validCaster(caster))
			return;
		sendNear(caster.serverLevel(), DEFAULT_SEND_RANGE,
				create(EXECUTION_CANCEL, caster, null, caster.position(),
						caster.position(), caster.getYRot(), caster.getXRot(),
						caster.serverLevel().getGameTime(),
						EXECUTION_CANCEL_TICKS, seed, 128, 0,
						FLAG_ESSENTIAL, 1.6F));
	}

	public static void sendExhaustion(ServerPlayer caster, int severity,
			int durationTicks, int seed) {
		if (!validCaster(caster))
			return;
		int boundedSeverity = Mth.clamp(severity, 0, 255);
		sendTrackingAndSelf(caster, create(EXHAUSTION, caster, null,
				caster.position(), caster.position(), caster.getYRot(),
				caster.getXRot(), caster.serverLevel().getGameTime(),
				durationTicks <= 0 ? EXHAUSTION_TICKS : durationTicks,
				seed, boundedSeverity, 0, FLAG_ESSENTIAL,
				1.4F + boundedSeverity / 255.0F));
	}

	public static void sendRiskFeedback(ServerPlayer caster, int severity,
			int durationTicks, int seed) {
		if (!validCaster(caster))
			return;
		int boundedSeverity = Mth.clamp(severity, 0, 255);
		int flags = FLAG_PRIVATE_CASTER
				| (boundedSeverity >= 192 ? FLAG_HIGH_RISK : 0);
		sendTo(caster, create(RISK_FEEDBACK, caster, null,
				caster.position(), caster.position(), caster.getYRot(),
				caster.getXRot(), caster.serverLevel().getGameTime(),
				durationTicks <= 0 ? RISK_TICKS : durationTicks,
				seed, boundedSeverity, 0, flags, 1.0F));
	}

	public static void sendNear(ServerLevel level,
			SungIlHwanVfxEventMessage message) {
		sendNear(level, DEFAULT_SEND_RANGE, message);
	}

	public static void sendNear(ServerLevel level, double range,
			SungIlHwanVfxEventMessage message) {
		if (level == null || message == null || message.privateToCaster())
			return;
		double boundedRange = Mth.clamp(range, 1.0D, 128.0D);
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.NEAR.with(
				PacketDistributor.TargetPoint.p(message.originX,
						message.originY, message.originZ, boundedRange,
						level.dimension())), message);
	}

	public static void sendTo(ServerPlayer player,
			SungIlHwanVfxEventMessage message) {
		if (player == null || message == null)
			return;
		if (message.privateToCaster()
				&& player.getId() != message.casterEntityId)
			return;
		SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player), message);
	}

	public static void sendTrackingAndSelf(Entity caster,
			SungIlHwanVfxEventMessage message) {
		if (caster == null || message == null || message.privateToCaster())
			return;
		SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> caster),
				message);
	}

	private static boolean validCaster(ServerPlayer caster) {
		return caster != null && caster.server != null && !caster.isRemoved();
	}

	private static double coordinate(double value) {
		if (!Double.isFinite(value))
			return 0.0D;
		return Mth.clamp(value, -WORLD_LIMIT, WORLD_LIMIT);
	}
}
