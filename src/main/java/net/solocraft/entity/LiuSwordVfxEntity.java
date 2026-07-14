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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class LiuSwordVfxEntity extends Entity {
	public static final int ARC = 0;
	public static final int CHARGE = 1;
	public static final int TARGET = 2;
	public static final int DASH = 3;
	public static final int COUNTER = 4;
	public static final int DOMAIN = 5;
	public static final int MARK = 6;
	public static final int DETONATION = 7;
	public static final int DANCE = 8;
	public static final int EXECUTION_EXPLOSION = 9;
	public static final int EXECUTION_LINK = 10;

	private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PRIMARY_COLOR = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SECONDARY_COLOR = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DUAL = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> MOVE_SPEED = SynchedEntityData.defineId(LiuSwordVfxEntity.class, EntityDataSerializers.FLOAT);

	public LiuSwordVfxEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.LIU_SWORD_VFX.get(), level);
	}

	public LiuSwordVfxEntity(EntityType<? extends LiuSwordVfxEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(STYLE, ARC);
		this.entityData.define(PRIMARY_COLOR, 0xFFD34E);
		this.entityData.define(SECONDARY_COLOR, 0xFFD34E);
		this.entityData.define(SCALE, 1.0F);
		this.entityData.define(LENGTH, 1.0F);
		this.entityData.define(ROLL, 0.0F);
		this.entityData.define(LIFETIME, 10);
		this.entityData.define(TARGET_ID, -1);
		this.entityData.define(DUAL, false);
		this.entityData.define(MOVE_SPEED, 0.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static LiuSwordVfxEntity spawn(ServerLevel level, double x, double y, double z, int style,
			int primaryColor, int secondaryColor, float scale, float length, float roll, int lifetime,
			float yaw, float pitch, boolean dual) {
		return spawn(level, x, y, z, style, primaryColor, secondaryColor, scale, length, roll,
				lifetime, yaw, pitch, dual, -1, 0.0F);
	}

	public static LiuSwordVfxEntity spawnAttached(ServerLevel level, Entity target, int style,
			int primaryColor, int secondaryColor, float scale, float length, float roll, int lifetime,
			boolean dual) {
		return spawn(level, target.getX(), target.getY() + target.getBbHeight() * 0.52D, target.getZ(),
				style, primaryColor, secondaryColor, scale, length, roll, lifetime,
				target.getYRot(), target.getXRot(), dual, target.getId(), 0.0F);
	}

	public static LiuSwordVfxEntity spawnMoving(ServerLevel level, Vec3 origin, Vec3 direction, int style,
			int primaryColor, int secondaryColor, float scale, float length, float roll, int lifetime,
			boolean dual, float moveSpeed) {
		Vec3 normalized = direction.lengthSqr() < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
		return spawn(level, origin.x, origin.y, origin.z, style, primaryColor, secondaryColor,
				scale, length, roll, lifetime, yawFor(normalized), pitchFor(normalized), dual, -1,
				Math.max(0.0F, moveSpeed));
	}

	public static LiuSwordVfxEntity spawnExecutionLink(ServerLevel level, Vec3 start, Vec3 end,
			int primaryColor, int secondaryColor, float width, int lifetime) {
		Vec3 direction = end.subtract(start);
		double distance = direction.length();
		Vec3 normalized = distance < 0.001D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.scale(1.0D / distance);
		Vec3 middle = start.add(end).scale(0.5D);
		return spawn(level, middle.x, middle.y, middle.z, EXECUTION_LINK,
				primaryColor, secondaryColor, width, (float) Math.max(0.1D, distance),
				level.getRandom().nextFloat() * 180.0F, lifetime, yawFor(normalized), pitchFor(normalized),
				false, -1, 0.0F);
	}

	private static LiuSwordVfxEntity spawn(ServerLevel level, double x, double y, double z, int style,
			int primaryColor, int secondaryColor, float scale, float length, float roll, int lifetime,
			float yaw, float pitch, boolean dual, int targetId, float moveSpeed) {
		LiuSwordVfxEntity effect = new LiuSwordVfxEntity(SololevelingModEntities.LIU_SWORD_VFX.get(), level);
		effect.entityData.set(STYLE, style);
		effect.entityData.set(PRIMARY_COLOR, primaryColor & 0xFFFFFF);
		effect.entityData.set(SECONDARY_COLOR, secondaryColor & 0xFFFFFF);
		effect.entityData.set(SCALE, Math.max(0.08F, scale));
		effect.entityData.set(LENGTH, Math.max(0.08F, length));
		effect.entityData.set(ROLL, roll);
		effect.entityData.set(LIFETIME, Math.max(2, lifetime));
		effect.entityData.set(TARGET_ID, targetId);
		effect.entityData.set(DUAL, dual);
		effect.entityData.set(MOVE_SPEED, moveSpeed);
		effect.moveTo(x, y, z, yaw, pitch);
		level.addFreshEntity(effect);
		return effect;
	}

	public int getStyle() {
		return this.entityData.get(STYLE);
	}

	public int getPrimaryColor() {
		return this.entityData.get(PRIMARY_COLOR);
	}

	public int getSecondaryColor() {
		return this.entityData.get(SECONDARY_COLOR);
	}

	public float getScale() {
		return this.entityData.get(SCALE);
	}

	public float getLength() {
		return this.entityData.get(LENGTH);
	}

	public float getRoll() {
		return this.entityData.get(ROLL);
	}

	public int getLifetime() {
		return this.entityData.get(LIFETIME);
	}

	public int getTargetId() {
		return this.entityData.get(TARGET_ID);
	}

	public boolean isDual() {
		return this.entityData.get(DUAL);
	}

	public float getMoveSpeed() {
		return this.entityData.get(MOVE_SPEED);
	}

	public float getProgress(float partialTick) {
		return Math.min(1.0F, (this.tickCount + partialTick) / Math.max(1.0F, getLifetime()));
	}

	public float getFade(float partialTick) {
		if (getStyle() == CHARGE) {
			float age = this.tickCount + partialTick;
			float fadeInTicks = Math.min(5.0F, Math.max(1.0F, getLifetime() * 0.18F));
			float fadeOutTicks = Math.min(8.0F, Math.max(1.0F, getLifetime() * 0.25F));
			float fadeIn = Math.min(1.0F, age / fadeInTicks);
			float fadeOut = Math.min(1.0F, (getLifetime() - age) / fadeOutTicks);
			return Math.max(0.0F, Math.min(fadeIn, fadeOut));
		}
		float progress = getProgress(partialTick);
		float fadeIn = Math.min(1.0F, progress * 7.0F);
		float fadeOut = Math.min(1.0F, (1.0F - progress) * 4.5F);
		return Math.max(0.0F, Math.min(fadeIn, fadeOut));
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setDeltaMovement(0.0D, 0.0D, 0.0D);
		if (getTargetId() >= 0) {
			Entity target = this.level().getEntity(getTargetId());
			if (target == null || !target.isAlive()) {
				this.discard();
				return;
			}
			this.setPos(target.getX(), target.getY() + target.getBbHeight() * 0.52D, target.getZ());
			this.setYRot(target.getYRot());
		} else if (getMoveSpeed() > 0.0F) {
			Vec3 movement = Vec3.directionFromRotation(getXRot(), getYRot()).scale(getMoveSpeed());
			this.setDeltaMovement(movement);
			this.setPos(getX() + movement.x, getY() + movement.y, getZ() + movement.z);
			this.hasImpulse = true;
		}
		if (this.tickCount >= getLifetime())
			this.discard();
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(STYLE, tag.getInt("Style"));
		this.entityData.set(PRIMARY_COLOR, tag.getInt("PrimaryColor"));
		this.entityData.set(SECONDARY_COLOR, tag.getInt("SecondaryColor"));
		this.entityData.set(SCALE, tag.getFloat("Scale"));
		this.entityData.set(LENGTH, tag.getFloat("Length"));
		this.entityData.set(ROLL, tag.getFloat("Roll"));
		this.entityData.set(LIFETIME, tag.getInt("Lifetime"));
		this.entityData.set(TARGET_ID, tag.getInt("TargetId"));
		this.entityData.set(DUAL, tag.getBoolean("Dual"));
		this.entityData.set(MOVE_SPEED, tag.getFloat("MoveSpeed"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Style", getStyle());
		tag.putInt("PrimaryColor", getPrimaryColor());
		tag.putInt("SecondaryColor", getSecondaryColor());
		tag.putFloat("Scale", getScale());
		tag.putFloat("Length", getLength());
		tag.putFloat("Roll", getRoll());
		tag.putInt("Lifetime", getLifetime());
		tag.putInt("TargetId", getTargetId());
		tag.putBoolean("Dual", isDual());
		tag.putFloat("MoveSpeed", getMoveSpeed());
	}

	private static float yawFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
	}

	private static float pitchFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.asin(-Math.max(-1.0D, Math.min(1.0D, direction.y))));
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		double horizontal = Math.max(1.0D, getScale() * 1.55D);
		double vertical = Math.max(1.0D, getLength() * 1.3D);
		if (getStyle() == EXECUTION_LINK) {
			double linkRadius = Math.max(horizontal, getLength() * 0.62D);
			horizontal = linkRadius;
			vertical = linkRadius;
		}
		return new AABB(getX() - horizontal, getY() - vertical, getZ() - horizontal,
				getX() + horizontal, getY() + vertical, getZ() + horizontal);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double range = switch (getStyle()) {
			case MARK, EXECUTION_EXPLOSION, EXECUTION_LINK -> 384.0D;
			default -> 192.0D;
		};
		return distance < range * range;
	}
}
