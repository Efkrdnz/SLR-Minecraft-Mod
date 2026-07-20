package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.BarrierMageSpellManager;
import net.solocraft.util.MageCombatHelper;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** Synced visual carrier and lightweight gameplay body for Barrier Mage constructs. */
public class BarrierVfxEntity extends Entity {
	public static final int FRACTURE_BOLT = 0;
	public static final int FRACTURE_MARK = 1;
	public static final int PRISM_RAMPART = 2;
	public static final int SHARD_PLATE = 3;
	public static final int REPULSION_FRAME = 4;
	public static final int SEALING_PRISM = 5;
	public static final int MIRROR_WARD = 6;
	public static final int RESONANT_COLLAPSE = 7;
	public static final int ABSOLUTE_BASTION = 8;
	public static final int IMPACT = 9;
	public static final int RETURN_SHARD = 10;

	private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PRIMARY_COLOR = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SECONDARY_COLOR = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> ORB_AMPLIFIED = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Optional<UUID>> TARGET = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Float> INTEGRITY = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> MAX_INTEGRITY = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> RESONANCE = SynchedEntityData.defineId(BarrierVfxEntity.class, EntityDataSerializers.FLOAT);

	public BarrierVfxEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.BARRIER_VFX.get(), level);
	}

	public BarrierVfxEntity(EntityType<? extends BarrierVfxEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(STYLE, FRACTURE_BOLT);
		this.entityData.define(STAGE, 1);
		this.entityData.define(SCALE, 1.0F);
		this.entityData.define(LENGTH, 1.0F);
		this.entityData.define(LIFETIME, 20);
		this.entityData.define(PRIMARY_COLOR, BarrierMageSpellManager.NORMAL_PRIMARY);
		this.entityData.define(SECONDARY_COLOR, BarrierMageSpellManager.NORMAL_SECONDARY);
		this.entityData.define(SEED, 0);
		this.entityData.define(ORB_AMPLIFIED, false);
		this.entityData.define(ACTIVE, false);
		this.entityData.define(OWNER, Optional.empty());
		this.entityData.define(TARGET, Optional.empty());
		this.entityData.define(INTEGRITY, 0.0F);
		this.entityData.define(MAX_INTEGRITY, 0.0F);
		this.entityData.define(RESONANCE, 0.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static BarrierVfxEntity spawn(ServerLevel level, Vec3 position, int style, int stage,
			float scale, float length, int lifetime, float yaw, float pitch, Entity owner,
			Entity target, boolean orbAmplified, float integrity, boolean active) {
		BarrierVfxEntity effect = new BarrierVfxEntity(SololevelingModEntities.BARRIER_VFX.get(), level);
		effect.entityData.set(STYLE, Mth.clamp(style, FRACTURE_BOLT, RETURN_SHARD));
		effect.entityData.set(STAGE, Mth.clamp(stage, 1, 5));
		effect.entityData.set(SCALE, Math.max(0.04F, scale));
		effect.entityData.set(LENGTH, Math.max(0.04F, length));
		effect.entityData.set(LIFETIME, Math.max(2, lifetime));
		effect.entityData.set(PRIMARY_COLOR, orbAmplified
				? BarrierMageSpellManager.ORB_PRIMARY : BarrierMageSpellManager.NORMAL_PRIMARY);
		effect.entityData.set(SECONDARY_COLOR, orbAmplified
				? BarrierMageSpellManager.ORB_SECONDARY : BarrierMageSpellManager.NORMAL_SECONDARY);
		effect.entityData.set(SEED, level.getRandom().nextInt());
		effect.entityData.set(ORB_AMPLIFIED, orbAmplified);
		effect.entityData.set(ACTIVE, active);
		effect.entityData.set(OWNER, owner == null ? Optional.empty() : Optional.of(owner.getUUID()));
		effect.entityData.set(TARGET, target == null ? Optional.empty() : Optional.of(target.getUUID()));
		effect.entityData.set(INTEGRITY, Math.max(0.0F, integrity));
		effect.entityData.set(MAX_INTEGRITY, Math.max(0.0F, integrity));
		effect.entityData.set(RESONANCE, 0.0F);
		effect.moveTo(position.x, position.y, position.z, yaw, pitch);
		effect.refreshDimensions();
		effect.refreshBarrierBounds();
		level.addFreshEntity(effect);
		return effect;
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setDeltaMovement(Vec3.ZERO);

		if (!level().isClientSide()) {
			followLinkedEntity();
			if (isBlockingConstruct() && isActive())
				interceptProjectiles();
		}
		refreshBarrierBounds();

		if (tickCount >= getLifetime())
			discard();
	}

	private void followLinkedEntity() {
		if (!(level() instanceof ServerLevel serverLevel))
			return;
		Entity linked = null;
		if (getStyle() == MIRROR_WARD)
			linked = getOwnerEntity(serverLevel);
		else if (getStyle() == FRACTURE_MARK || getStyle() == SEALING_PRISM)
			linked = getTargetEntity(serverLevel);
		if (linked == null)
			return;
		if (!linked.isAlive()) {
			discard();
			return;
		}
		double y = getStyle() == FRACTURE_MARK
				? linked.getY() + linked.getBbHeight() * 0.58D
				: linked.getY() + 0.04D;
		setPos(linked.getX(), y, linked.getZ());
		if (getStyle() == MIRROR_WARD)
			setYRot(linked.getYRot());
	}

	private void interceptProjectiles() {
		if (!(level() instanceof ServerLevel serverLevel))
			return;
		Entity owner = getOwnerEntity(serverLevel);
		AABB search = getBoundingBoxForCulling().inflate(1.5D);
		for (Projectile projectile : serverLevel.getEntitiesOfClass(Projectile.class, search,
				candidate -> candidate.isAlive())) {
			Entity source = projectile.getOwner();
			if (source != null && owner != null && MageCombatHelper.areAllied(owner, source))
				continue;
			Vec3 previous = new Vec3(projectile.xo, projectile.yo, projectile.zo);
			if (!intersectsSegment(previous, projectile.position()))
				continue;
			float pressure = (float) Mth.clamp(4.0D + projectile.getDeltaMovement().length() * 5.0D,
					4.0D, 24.0D);
			Vec3 impact = projectile.position();
			projectile.discard();
			absorbDamage(pressure);
			if (isOrbAmplified() && getStyle() == ABSOLUTE_BASTION)
				repairFromProjectile(pressure * 0.35F);
			BarrierMageSpellManager.onProjectileBlocked(this, impact);
			if (!isAlive())
				break;
		}
	}

	public boolean intersectsSegment(Vec3 from, Vec3 to) {
		if (getStyle() == ABSOLUTE_BASTION) {
			boolean fromInside = containsInBastion(from);
			boolean toInside = containsInBastion(to);
			return fromInside != toInside;
		}
		if (getStyle() != PRISM_RAMPART && getStyle() != SHARD_PLATE)
			return false;

		Vec3 origin = position();
		Vec3 normal = Vec3.directionFromRotation(0.0F, getYRot()).multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 right = new Vec3(-normal.z, 0.0D, normal.x);
		double fromDistance = from.subtract(origin).dot(normal);
		double toDistance = to.subtract(origin).dot(normal);
		if (fromDistance * toDistance > 0.0D || Math.abs(fromDistance - toDistance) < 1.0E-5D)
			return false;
		double t = fromDistance / (fromDistance - toDistance);
		if (t < 0.0D || t > 1.0D)
			return false;
		Vec3 hit = from.add(to.subtract(from).scale(t));
		Vec3 local = hit.subtract(origin);
		return Math.abs(local.dot(right)) <= getScale()
				&& local.y >= -0.2D && local.y <= getLength() + 0.2D;
	}

	public boolean containsInBastion(Vec3 point) {
		if (getStyle() != ABSOLUTE_BASTION)
			return false;
		Vec3 local = point.subtract(position());
		double horizontal = local.x * local.x + local.z * local.z;
		return horizontal <= getScale() * getScale()
				&& local.y >= -1.0D && local.y <= getLength();
	}

	public float absorbDamage(float amount) {
		if (!isActive() || amount <= 0.0F || getIntegrity() <= 0.0F)
			return 0.0F;
		float blocked = Math.min(amount, getIntegrity());
		setIntegrity(getIntegrity() - blocked);
		setResonance(Math.min(getMaxIntegrity() * 1.5F, getResonance() + blocked * 0.35F));
		if (getIntegrity() <= 0.001F)
			breakConstruct();
		return blocked;
	}

	private void repairFromProjectile(float amount) {
		float repairLimit = getMaxIntegrity() * 0.10F;
		float repaired = getPersistentData().getFloat("OrbRepair");
		float accepted = Math.min(Math.max(0.0F, amount), Math.max(0.0F, repairLimit - repaired));
		if (accepted <= 0.0F)
			return;
		getPersistentData().putFloat("OrbRepair", repaired + accepted);
		setIntegrity(Math.min(getMaxIntegrity(), getIntegrity() + accepted));
	}

	public void breakConstruct() {
		if (!isActive())
			return;
		this.entityData.set(ACTIVE, false);
		if (!level().isClientSide())
			BarrierMageSpellManager.onConstructBroken(this);
		discard();
	}

	public void dissolve() {
		this.entityData.set(ACTIVE, false);
		discard();
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (!isGameplayConstruct() || !isActive() || amount <= 0.0F)
			return false;
		if (level().isClientSide())
			return true;
		if (level() instanceof ServerLevel serverLevel) {
			Entity owner = getOwnerEntity(serverLevel);
			Entity attacker = source.getEntity();
			if (owner != null && attacker != null && MageCombatHelper.areAllied(owner, attacker))
				return false;
		}
		absorbDamage(amount);
		return true;
	}

	public Entity getOwnerEntity(ServerLevel level) {
		return getOwnerId().map(level::getEntity).orElse(null);
	}

	public Entity getTargetEntity(ServerLevel level) {
		return getTargetId().map(level::getEntity).orElse(null);
	}

	public int getStyle() {
		return entityData.get(STYLE);
	}

	public int getStage() {
		return entityData.get(STAGE);
	}

	public float getScale() {
		return entityData.get(SCALE);
	}

	public float getLength() {
		return entityData.get(LENGTH);
	}

	public int getLifetime() {
		return entityData.get(LIFETIME);
	}

	public int getPrimaryColor() {
		return entityData.get(PRIMARY_COLOR);
	}

	public int getSecondaryColor() {
		return entityData.get(SECONDARY_COLOR);
	}

	public int getSeed() {
		return entityData.get(SEED);
	}

	public boolean isOrbAmplified() {
		return entityData.get(ORB_AMPLIFIED);
	}

	public boolean isActive() {
		return entityData.get(ACTIVE);
	}

	public Optional<UUID> getOwnerId() {
		return entityData.get(OWNER);
	}

	public Optional<UUID> getTargetId() {
		return entityData.get(TARGET);
	}

	public float getIntegrity() {
		return entityData.get(INTEGRITY);
	}

	public float getMaxIntegrity() {
		return entityData.get(MAX_INTEGRITY);
	}

	public float getResonance() {
		return entityData.get(RESONANCE);
	}

	public void setIntegrity(float value) {
		entityData.set(INTEGRITY, Math.max(0.0F, value));
	}

	public void setResonance(float value) {
		entityData.set(RESONANCE, Math.max(0.0F, value));
	}

	public float getProgress(float partialTick) {
		return Mth.clamp((tickCount + partialTick) / Math.max(1.0F, getLifetime()), 0.0F, 1.0F);
	}

	public float getFade(float partialTick) {
		float progress = getProgress(partialTick);
		float fadeIn = Mth.clamp(progress * 10.0F, 0.0F, 1.0F);
		float fadeOut = Mth.clamp((1.0F - progress) * (isGameplayConstruct() ? 10.0F : 4.5F), 0.0F, 1.0F);
		return Math.min(fadeIn, fadeOut);
	}

	public boolean isBlockingConstruct() {
		return getStyle() == PRISM_RAMPART || getStyle() == SHARD_PLATE
				|| getStyle() == ABSOLUTE_BASTION;
	}

	public boolean countsTowardConstructLimit() {
		return getStyle() == PRISM_RAMPART || getStyle() == ABSOLUTE_BASTION;
	}

	public boolean isGameplayConstruct() {
		return isBlockingConstruct() || getStyle() == SEALING_PRISM || getStyle() == MIRROR_WARD;
	}

	@Override
	public boolean isPickable() {
		return isGameplayConstruct() && getStyle() != ABSOLUTE_BASTION && isActive();
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return switch (getStyle()) {
			case PRISM_RAMPART, SHARD_PLATE -> EntityDimensions.scalable(
					Math.max(0.25F, getScale() * 2.0F), Math.max(0.25F, getLength()));
			case SEALING_PRISM -> EntityDimensions.scalable(
					Math.max(0.25F, getScale() * 2.15F), Math.max(0.25F, getLength()));
			case MIRROR_WARD -> EntityDimensions.scalable(
					Math.max(0.25F, getScale() * 1.8F), Math.max(0.25F, getLength()));
			default -> super.getDimensions(pose);
		};
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (STYLE.equals(key) || SCALE.equals(key) || LENGTH.equals(key))
			refreshDimensions();
		refreshBarrierBounds();
	}

	private void refreshBarrierBounds() {
		if (getStyle() != PRISM_RAMPART && getStyle() != SHARD_PLATE)
			return;
		Vec3 normal = Vec3.directionFromRotation(0.0F, getYRot())
				.multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 right = new Vec3(-normal.z, 0.0D, normal.x);
		double extentX = Math.abs(right.x) * getScale() + Math.abs(normal.x) * 0.14D;
		double extentZ = Math.abs(right.z) * getScale() + Math.abs(normal.z) * 0.14D;
		setBoundingBox(new AABB(getX() - extentX, getY() - 0.05D, getZ() - extentZ,
				getX() + extentX, getY() + getLength(), getZ() + extentZ));
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		double radius = Math.max(2.0D, getScale() * 1.35D);
		double below = getStyle() == MIRROR_WARD ? getLength() * 0.5D : 1.0D;
		return new AABB(getX() - radius, getY() - below, getZ() - radius,
				getX() + radius, getY() + getLength() + 1.0D, getZ() + radius);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double range = getStyle() == ABSOLUTE_BASTION || getStyle() == RESONANT_COLLAPSE
				? 384.0D : 192.0D;
		return distance < range * range;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		entityData.set(STYLE, Mth.clamp(tag.getInt("Style"), FRACTURE_BOLT, RETURN_SHARD));
		entityData.set(STAGE, Mth.clamp(tag.getInt("Stage"), 1, 5));
		entityData.set(SCALE, Math.max(0.04F, tag.getFloat("Scale")));
		entityData.set(LENGTH, Math.max(0.04F, tag.getFloat("Length")));
		entityData.set(LIFETIME, Math.max(2, tag.getInt("Lifetime")));
		entityData.set(PRIMARY_COLOR, tag.getInt("PrimaryColor"));
		entityData.set(SECONDARY_COLOR, tag.getInt("SecondaryColor"));
		entityData.set(SEED, tag.getInt("Seed"));
		entityData.set(ORB_AMPLIFIED, tag.getBoolean("OrbAmplified"));
		entityData.set(ACTIVE, tag.getBoolean("Active"));
		entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
		entityData.set(TARGET, tag.hasUUID("Target") ? Optional.of(tag.getUUID("Target")) : Optional.empty());
		entityData.set(INTEGRITY, Math.max(0.0F, tag.getFloat("Integrity")));
		entityData.set(MAX_INTEGRITY, Math.max(0.0F, tag.getFloat("MaxIntegrity")));
		entityData.set(RESONANCE, Math.max(0.0F, tag.getFloat("Resonance")));
		refreshDimensions();
		refreshBarrierBounds();
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Style", getStyle());
		tag.putInt("Stage", getStage());
		tag.putFloat("Scale", getScale());
		tag.putFloat("Length", getLength());
		tag.putInt("Lifetime", getLifetime());
		tag.putInt("PrimaryColor", getPrimaryColor());
		tag.putInt("SecondaryColor", getSecondaryColor());
		tag.putInt("Seed", getSeed());
		tag.putBoolean("OrbAmplified", isOrbAmplified());
		tag.putBoolean("Active", isActive());
		getOwnerId().ifPresent(uuid -> tag.putUUID("Owner", uuid));
		getTargetId().ifPresent(uuid -> tag.putUUID("Target", uuid));
		tag.putFloat("Integrity", getIntegrity());
		tag.putFloat("MaxIntegrity", getMaxIntegrity());
		tag.putFloat("Resonance", getResonance());
	}
}
