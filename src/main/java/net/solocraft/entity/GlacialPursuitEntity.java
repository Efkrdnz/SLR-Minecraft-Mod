package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Synchronized visual and riding anchor for the Frost Monarch's guided causeway.
 * Combat, steering, and terrain changes remain server-authoritative in the manager.
 */
public class GlacialPursuitEntity extends Entity {
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(
			GlacialPursuitEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> RIDER_MODE = SynchedEntityData.defineId(
			GlacialPursuitEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> MANIFESTED = SynchedEntityData.defineId(
			GlacialPursuitEntity.class, EntityDataSerializers.BOOLEAN);
	private int missingOwnerTicks;

	public GlacialPursuitEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.GLACIAL_PURSUIT.get(), level);
	}

	public GlacialPursuitEntity(EntityType<? extends GlacialPursuitEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public static GlacialPursuitEntity spawn(ServerLevel level, Player owner, Vec3 position,
			Vec3 direction, boolean riderMode, boolean manifested) {
		GlacialPursuitEntity entity = new GlacialPursuitEntity(SololevelingModEntities.GLACIAL_PURSUIT.get(), level);
		entity.entityData.set(OWNER, Optional.of(owner.getUUID()));
		entity.entityData.set(RIDER_MODE, riderMode);
		entity.entityData.set(MANIFESTED, manifested);
		entity.moveTo(position.x, position.y, position.z, yawFor(direction), pitchFor(direction));
		entity.setDeltaMovement(direction);
		level.addFreshEntity(entity);
		return entity;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER, Optional.empty());
		this.entityData.define(RIDER_MODE, false);
		this.entityData.define(MANIFESTED, false);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public Optional<UUID> getOwnerId() {
		return this.entityData.get(OWNER);
	}

	public boolean isRiderMode() {
		return this.entityData.get(RIDER_MODE);
	}

	public boolean isManifested() {
		return this.entityData.get(MANIFESTED);
	}

	public void setFlight(Vec3 position, Vec3 movement) {
		Vec3 direction = movement.lengthSqr() < 1.0E-6D ? this.getDeltaMovement() : movement.normalize();
		this.setDeltaMovement(movement);
		this.setYRot(yawFor(direction));
		this.setXRot(pitchFor(direction));
		this.setPos(position.x, position.y, position.z);
		this.hasImpulse = true;
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setNoGravity(true);
		if (!this.level().isClientSide()) {
			Entity owner = getOwnerId().map(id -> ((ServerLevel) this.level()).getEntity(id)).orElse(null);
			if (owner == null || !owner.isAlive()) {
				if (++this.missingOwnerTicks > 10)
					this.discard();
			} else {
				this.missingOwnerTicks = 0;
			}
			if (this.tickCount > 180)
				this.discard();
		}
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return isRiderMode() && this.getPassengers().isEmpty() && passenger instanceof Player
				&& getOwnerId().map(passenger.getUUID()::equals).orElse(false);
	}

	@Override
	public double getPassengersRidingOffset() {
		return 0.55D;
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
		if (!this.hasPassenger(passenger))
			return;
		Vec3 backward = this.getDeltaMovement().lengthSqr() < 1.0E-6D
				? Vec3.ZERO : this.getDeltaMovement().normalize().scale(-0.34D);
		moveFunction.accept(passenger, this.getX() + backward.x,
				this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(),
				this.getZ() + backward.z);
	}

	@Override
	public boolean shouldRiderSit() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean canCollideWith(Entity other) {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.hasUUID("Owner"))
			this.entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
		this.entityData.set(RIDER_MODE, tag.getBoolean("RiderMode"));
		this.entityData.set(MANIFESTED, tag.getBoolean("Manifested"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		getOwnerId().ifPresent(id -> tag.putUUID("Owner", id));
		tag.putBoolean("RiderMode", isRiderMode());
		tag.putBoolean("Manifested", isManifested());
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		return this.getBoundingBox().inflate(isRiderMode() ? 5.0D : 4.0D);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 256.0D * 256.0D;
	}

	private static float yawFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
	}

	private static float pitchFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.asin(-Math.max(-1.0D, Math.min(1.0D, direction.y))));
	}
}
