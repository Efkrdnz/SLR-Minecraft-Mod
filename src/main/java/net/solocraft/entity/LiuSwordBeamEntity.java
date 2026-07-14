package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.LiuZhigangCombatManager;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class LiuSwordBeamEntity extends Entity {
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DUAL = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> PRIMARY_COLOR = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SECONDARY_COLOR = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> WIDTH = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MAX_RANGE = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DIRECTION_X = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DIRECTION_Y = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DIRECTION_Z = SynchedEntityData.defineId(LiuSwordBeamEntity.class, EntityDataSerializers.FLOAT);

	private final Set<UUID> touched = new HashSet<>();
	private final Set<UUID> marked = new HashSet<>();
	private final Map<UUID, UUID> markerEffects = new HashMap<>();
	private float travelled;
	private boolean finishing;
	private boolean executionScheduled;

	public LiuSwordBeamEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.LIU_SWORD_BEAM.get(), level);
	}

	public LiuSwordBeamEntity(EntityType<? extends LiuSwordBeamEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER, Optional.empty());
		this.entityData.define(TIER, 0);
		this.entityData.define(DUAL, false);
		this.entityData.define(PRIMARY_COLOR, 0xFFD34E);
		this.entityData.define(SECONDARY_COLOR, 0xFFD34E);
		this.entityData.define(WIDTH, 4.0F);
		this.entityData.define(MAX_RANGE, 20.0F);
		this.entityData.define(SPEED, 3.2F);
		this.entityData.define(DAMAGE, 10.0F);
		this.entityData.define(DIRECTION_X, 0.0F);
		this.entityData.define(DIRECTION_Y, 0.0F);
		this.entityData.define(DIRECTION_Z, 1.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static LiuSwordBeamEntity spawn(ServerLevel level, ServerPlayer owner, Vec3 origin, Vec3 direction,
			int tier, boolean dual, int primaryColor, int secondaryColor, float width, float range,
			float speed, float damage) {
		LiuSwordBeamEntity beam = new LiuSwordBeamEntity(SololevelingModEntities.LIU_SWORD_BEAM.get(), level);
		beam.entityData.set(OWNER, Optional.of(owner.getUUID()));
		beam.entityData.set(TIER, Mth.clamp(tier, 0, 3));
		beam.entityData.set(DUAL, dual);
		beam.entityData.set(PRIMARY_COLOR, primaryColor & 0xFFFFFF);
		beam.entityData.set(SECONDARY_COLOR, secondaryColor & 0xFFFFFF);
		beam.entityData.set(WIDTH, Math.max(1.0F, width));
		beam.entityData.set(MAX_RANGE, Math.max(2.0F, range));
		beam.entityData.set(SPEED, Math.max(0.2F, speed));
		beam.entityData.set(DAMAGE, Math.max(0.5F, damage));
		Vec3 normalized = direction.lengthSqr() < 0.001D ? owner.getLookAngle().normalize() : direction.normalize();
		beam.entityData.set(DIRECTION_X, (float) normalized.x);
		beam.entityData.set(DIRECTION_Y, (float) normalized.y);
		beam.entityData.set(DIRECTION_Z, (float) normalized.z);
		beam.setDeltaMovement(normalized.scale(speed));
		beam.setRot(yawFor(normalized), pitchFor(normalized));
		beam.moveTo(origin.x, origin.y, origin.z, beam.getYRot(), beam.getXRot());
		level.addFreshEntity(beam);
		return beam;
	}

	public int getTier() {
		return this.entityData.get(TIER);
	}

	public boolean isDual() {
		return this.entityData.get(DUAL);
	}

	public int getPrimaryColor() {
		return this.entityData.get(PRIMARY_COLOR);
	}

	public int getSecondaryColor() {
		return this.entityData.get(SECONDARY_COLOR);
	}

	public float getBeamWidth() {
		return this.entityData.get(WIDTH);
	}

	public float getMaxRange() {
		return this.entityData.get(MAX_RANGE);
	}

	public float getBeamSpeed() {
		return this.entityData.get(SPEED);
	}

	public float getDamage() {
		return this.entityData.get(DAMAGE);
	}

	public Vec3 getBeamDirection() {
		Vec3 direction = new Vec3(this.entityData.get(DIRECTION_X), this.entityData.get(DIRECTION_Y),
				this.entityData.get(DIRECTION_Z));
		return direction.lengthSqr() < 0.001D ? Vec3.directionFromRotation(getXRot(), getYRot()) : direction.normalize();
	}

	public boolean isExecutionBeam() {
		return getTier() >= 3;
	}

	public float getVisualScale() {
		return isExecutionBeam() ? 2.2F : 1.0F;
	}

	public int getTravelDurationTicks() {
		return Math.max(1, Mth.ceil(getMaxRange() / Math.max(0.2F, getBeamSpeed())));
	}

	public float getRenderFade(float partialTick) {
		float progress = Mth.clamp((this.tickCount + partialTick) / getTravelDurationTicks(),
				0.0F, 1.0F);
		return 1.0F - progress;
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		if (this.level().isClientSide()) {
			Vec3 motion = getBeamDirection().scale(getBeamSpeed());
			this.setDeltaMovement(motion);
			this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
			return;
		}
		if (!(this.level() instanceof ServerLevel level)) {
			finish(null);
			return;
		}
		ServerPlayer owner = getOwner(level);
		if (owner == null || !owner.isAlive()) {
			finish(owner);
			return;
		}
		if (this.tickCount > getTravelDurationTicks() + 4) {
			finish(owner);
			return;
		}
		Vec3 direction = getBeamDirection();
		if (direction.lengthSqr() < 0.001D) {
			finish(owner);
			return;
		}
		direction = direction.normalize();
		float remaining = getMaxRange() - travelled;
		if (remaining <= 0.01F) {
			finish(owner);
			return;
		}
		double step = Math.min(getBeamSpeed(), remaining);
		Vec3 start = this.position();
		Vec3 intendedEnd = start.add(direction.scale(step));
		if (!level.hasChunkAt(BlockPos.containing(intendedEnd))) {
			finish(owner);
			return;
		}
		Vec3 end = intendedEnd;
		boolean blocked = false;
		if (!isExecutionBeam()) {
			BlockHitResult blockHit = level.clip(new ClipContext(start, intendedEnd,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			if (blockHit.getType() == HitResult.Type.BLOCK) {
				end = blockHit.getLocation();
				blocked = true;
			}
		}
		processTargets(level, owner, start, end);
		this.setPos(end.x, end.y, end.z);
		this.setRot(yawFor(direction), pitchFor(direction));
		this.setDeltaMovement(direction.scale(getBeamSpeed()));
		this.travelled += (float) start.distanceTo(end);
		this.hasImpulse = true;
		if (blocked || this.travelled + 0.01F >= getMaxRange())
			finish(owner);
	}

	private void processTargets(ServerLevel level, ServerPlayer owner, Vec3 start, Vec3 end) {
		double halfWidth = getBeamWidth() * (isExecutionBeam() ? 1.3D : 1.18D);
		double halfHeight = Math.max(1.5D, getBeamWidth()
				* (isExecutionBeam() ? (isDual() ? 0.95D : 0.78D) : (isDual() ? 0.78D : 0.58D)));
		AABB swept = new AABB(start, end).inflate(halfWidth + 4.0D, halfHeight + 4.0D, halfWidth + 4.0D);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, swept,
				candidate -> LiuZhigangCombatManager.isValidTarget(owner, candidate))) {
			UUID targetId = target.getUUID();
			if (touched.contains(targetId) || !intersectsSlashVolume(target, start, end, halfWidth, halfHeight))
				continue;
			touched.add(targetId);
			if (isExecutionBeam()) {
				marked.add(targetId);
				LiuZhigangCombatManager.restrainExecutionTarget(owner, target, 120);
				LiuSwordVfxEntity marker = LiuSwordVfxEntity.spawnAttached(level, target, LiuSwordVfxEntity.MARK,
						getPrimaryColor(), getSecondaryColor(), Math.max(1.2F, target.getBbWidth() * 1.45F),
						Math.max(1.8F, target.getBbHeight() * 1.1F), 0.0F, 120, isDual());
				markerEffects.put(targetId, marker.getUUID());
			} else {
				LiuZhigangCombatManager.hitBySwordBeam(owner, target, getDamage(), getTier());
			}
		}
	}

	private boolean intersectsSlashVolume(LivingEntity target, Vec3 start, Vec3 end,
			double halfWidth, double halfHeight) {
		Vec3 segment = end.subtract(start);
		double lengthSq = segment.lengthSqr();
		Vec3 forward = lengthSq < 1.0E-6D ? getBeamDirection() : segment.scale(1.0D / Math.sqrt(lengthSq));
		Vec3 referenceUp = Math.abs(forward.y) > 0.96D ? new Vec3(1.0D, 0.0D, 0.0D) : new Vec3(0.0D, 1.0D, 0.0D);
		Vec3 right = forward.cross(referenceUp).normalize();
		Vec3 up = right.cross(forward).normalize();
		AABB bounds = target.getBoundingBox();
		Vec3 center = bounds.getCenter();
		double t = lengthSq < 1.0E-6D ? 0.0D : Mth.clamp(center.subtract(start).dot(segment) / lengthSq, 0.0D, 1.0D);
		Vec3 nearest = start.add(segment.scale(t));
		Vec3 offset = center.subtract(nearest);
		double padding = isExecutionBeam() ? 1.5D : 0.7D;
		double forwardAllowance = projectedHalfExtent(bounds, forward) + 0.75D;
		double widthAllowance = halfWidth + projectedHalfExtent(bounds, right) + padding;
		double heightAllowance = halfHeight + projectedHalfExtent(bounds, up) + padding;
		return Math.abs(offset.dot(forward)) <= forwardAllowance
				&& Math.abs(offset.dot(right)) <= widthAllowance
				&& Math.abs(offset.dot(up)) <= heightAllowance;
	}

	private static double projectedHalfExtent(AABB bounds, Vec3 axis) {
		return Math.abs(axis.x) * bounds.getXsize() * 0.5D
				+ Math.abs(axis.y) * bounds.getYsize() * 0.5D
				+ Math.abs(axis.z) * bounds.getZsize() * 0.5D;
	}

	private void finish(ServerPlayer owner) {
		if (finishing)
			return;
		finishing = true;
		if (this.level() instanceof ServerLevel level) {
			if (!executionScheduled && owner != null && isExecutionBeam() && !marked.isEmpty()) {
				LiuZhigangCombatManager.scheduleExecutionDetonation(owner, marked, markerEffects, getDamage(),
						getPrimaryColor(), getSecondaryColor(), isDual());
				executionScheduled = true;
			}
			if (!executionScheduled) {
				for (UUID markerId : markerEffects.values()) {
					Entity marker = level.getEntity(markerId);
					if (marker != null)
						marker.discard();
				}
			}
		}
		this.discard();
	}

	private ServerPlayer getOwner(ServerLevel level) {
		return this.entityData.get(OWNER).map(level::getPlayerByUUID)
				.filter(ServerPlayer.class::isInstance).map(ServerPlayer.class::cast).orElse(null);
	}

	private static float yawFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
	}

	private static float pitchFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.asin(-Mth.clamp(direction.y, -1.0D, 1.0D)));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.hasUUID("Owner"))
			this.entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
		this.entityData.set(TIER, tag.getInt("Tier"));
		this.entityData.set(DUAL, tag.getBoolean("Dual"));
		this.entityData.set(PRIMARY_COLOR, tag.getInt("PrimaryColor"));
		this.entityData.set(SECONDARY_COLOR, tag.getInt("SecondaryColor"));
		this.entityData.set(WIDTH, tag.getFloat("Width"));
		this.entityData.set(MAX_RANGE, tag.getFloat("MaxRange"));
		this.entityData.set(SPEED, tag.getFloat("Speed"));
		this.entityData.set(DAMAGE, tag.getFloat("Damage"));
		if (tag.contains("DirectionX")) {
			this.entityData.set(DIRECTION_X, tag.getFloat("DirectionX"));
			this.entityData.set(DIRECTION_Y, tag.getFloat("DirectionY"));
			this.entityData.set(DIRECTION_Z, tag.getFloat("DirectionZ"));
		}
		this.travelled = tag.getFloat("Travelled");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("Owner", uuid));
		tag.putInt("Tier", getTier());
		tag.putBoolean("Dual", isDual());
		tag.putInt("PrimaryColor", getPrimaryColor());
		tag.putInt("SecondaryColor", getSecondaryColor());
		tag.putFloat("Width", getBeamWidth());
		tag.putFloat("MaxRange", getMaxRange());
		tag.putFloat("Speed", getBeamSpeed());
		tag.putFloat("Damage", getDamage());
		tag.putFloat("DirectionX", this.entityData.get(DIRECTION_X));
		tag.putFloat("DirectionY", this.entityData.get(DIRECTION_Y));
		tag.putFloat("DirectionZ", this.entityData.get(DIRECTION_Z));
		tag.putFloat("Travelled", travelled);
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		double visualScale = getVisualScale();
		double horizontal = Math.max(2.0D, getBeamWidth() * visualScale * 1.55D);
		double vertical = Math.max(2.0D, getBeamWidth() * visualScale
				* (isDual() ? 1.15D : 0.92D));
		return new AABB(getX() - horizontal, getY() - vertical, getZ() - horizontal,
				getX() + horizontal, getY() + vertical, getZ() + horizontal);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double range = Math.max(256.0D, getMaxRange() + 128.0D);
		return distance < range * range;
	}
}
