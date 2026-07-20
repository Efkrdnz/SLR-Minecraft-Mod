package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.ArcaneMageSpellManager;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/** Synced, visual-only carrier for Arcane Mage world effects. */
public class ArcaneVfxEntity extends Entity {
	public static final int AETHER_BOLT = 0;
	public static final int AETHER_IMPACT = 1;
	public static final int VECTOR_TRAIL = 2;
	public static final int VECTOR_ANCHOR = 3;
	public static final int POLARITY_SPHERE = 4;
	public static final int RUNIC_RELAY = 5;
	public static final int RELAY_BEAM = 6;
	public static final int ASTRAL_ARSENAL = 7;
	public static final int ASTRAL_BLADE = 8;
	public static final int DIMENSIONAL_REND = 9;
	public static final int SPATIAL_SCAR = 10;
	public static final int CONVERGENCE_GROUND = 11;
	public static final int CONVERGENCE_SKY = 12;
	public static final int CONVERGENCE_TETHER = 13;
	public static final int ZERO_POINT = 14;
	public static final int FORMULA_RUNES = 15;

	private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PRIMARY_COLOR = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SECONDARY_COLOR = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> ORB_AMPLIFIED = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> OVERCAST = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Optional<UUID>> TARGET = SynchedEntityData.defineId(ArcaneVfxEntity.class, EntityDataSerializers.OPTIONAL_UUID);

	public ArcaneVfxEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.ARCANE_VFX.get(), level);
	}

	public ArcaneVfxEntity(EntityType<? extends ArcaneVfxEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		entityData.define(STYLE, AETHER_BOLT);
		entityData.define(STAGE, 1);
		entityData.define(SCALE, 1.0F);
		entityData.define(LENGTH, 1.0F);
		entityData.define(LIFETIME, 20);
		entityData.define(PRIMARY_COLOR, ArcaneMageSpellManager.NORMAL_PRIMARY);
		entityData.define(SECONDARY_COLOR, ArcaneMageSpellManager.NORMAL_SECONDARY);
		entityData.define(SEED, 0);
		entityData.define(ORB_AMPLIFIED, false);
		entityData.define(OVERCAST, false);
		entityData.define(OWNER, Optional.empty());
		entityData.define(TARGET, Optional.empty());
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static ArcaneVfxEntity spawn(ServerLevel level, Vec3 position, int style, int stage,
			float scale, float length, int lifetime, float yaw, float pitch, Entity owner,
			Entity target, boolean orbAmplified, boolean overcast) {
		ArcaneVfxEntity effect = new ArcaneVfxEntity(SololevelingModEntities.ARCANE_VFX.get(), level);
		effect.entityData.set(STYLE, Mth.clamp(style, AETHER_BOLT, FORMULA_RUNES));
		effect.entityData.set(STAGE, Mth.clamp(stage, 1, 5));
		effect.entityData.set(SCALE, Math.max(0.04F, scale));
		effect.entityData.set(LENGTH, Math.max(0.04F, length));
		effect.entityData.set(LIFETIME, Math.max(2, lifetime));
		effect.entityData.set(PRIMARY_COLOR, orbAmplified
				? ArcaneMageSpellManager.ORB_PRIMARY : ArcaneMageSpellManager.NORMAL_PRIMARY);
		effect.entityData.set(SECONDARY_COLOR, orbAmplified
				? ArcaneMageSpellManager.ORB_SECONDARY : ArcaneMageSpellManager.NORMAL_SECONDARY);
		effect.entityData.set(SEED, level.getRandom().nextInt());
		effect.entityData.set(ORB_AMPLIFIED, orbAmplified);
		effect.entityData.set(OVERCAST, overcast);
		effect.entityData.set(OWNER, owner == null ? Optional.empty() : Optional.of(owner.getUUID()));
		effect.entityData.set(TARGET, target == null ? Optional.empty() : Optional.of(target.getUUID()));
		effect.moveTo(position.x, position.y, position.z, yaw, pitch);
		level.addFreshEntity(effect);
		return effect;
	}

	@Override
	public void tick() {
		super.tick();
		noPhysics = true;
		setDeltaMovement(Vec3.ZERO);
		if (!level().isClientSide())
			followOwner();
		if (tickCount >= getLifetime())
			discard();
	}

	private void followOwner() {
		if (!(level() instanceof ServerLevel serverLevel)
				|| (getStyle() != FORMULA_RUNES && getStyle() != ASTRAL_ARSENAL))
			return;
		Entity owner = getOwnerEntity(serverLevel);
		if (owner == null || !owner.isAlive()) {
			discard();
			return;
		}
		setPos(owner.getX(), owner.getY() + owner.getBbHeight() * 0.52D, owner.getZ());
		setYRot(owner.getYRot());
	}

	public void setVisualPose(Vec3 position, float yaw, float pitch) {
		setPos(position.x, position.y, position.z);
		setYRot(yaw);
		setXRot(pitch);
	}

	public void setScale(float scale) {
		entityData.set(SCALE, Math.max(0.04F, scale));
	}

	public Entity getOwnerEntity(ServerLevel level) {
		return getOwnerId().map(level::getEntity).orElse(null);
	}

	public int getStyle() { return entityData.get(STYLE); }
	public int getStage() { return entityData.get(STAGE); }
	public float getScale() { return entityData.get(SCALE); }
	public float getLength() { return entityData.get(LENGTH); }
	public int getLifetime() { return entityData.get(LIFETIME); }
	public int getPrimaryColor() { return entityData.get(PRIMARY_COLOR); }
	public int getSecondaryColor() { return entityData.get(SECONDARY_COLOR); }
	public int getSeed() { return entityData.get(SEED); }
	public boolean isOrbAmplified() { return entityData.get(ORB_AMPLIFIED); }
	public boolean isOvercast() { return entityData.get(OVERCAST); }
	public Optional<UUID> getOwnerId() { return entityData.get(OWNER); }
	public Optional<UUID> getTargetId() { return entityData.get(TARGET); }

	public float getProgress(float partialTick) {
		return Mth.clamp((tickCount + partialTick) / Math.max(1.0F, getLifetime()), 0.0F, 1.0F);
	}

	public float getFade(float partialTick) {
		float progress = getProgress(partialTick);
		float fadeIn = Mth.clamp(progress * 9.0F, 0.0F, 1.0F);
		float fadeOut = Mth.clamp((1.0F - progress) * 5.0F, 0.0F, 1.0F);
		return Math.min(fadeIn, fadeOut);
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		double radius = Math.max(3.0D, Math.max(getScale(), getLength()) * 1.55D);
		return new AABB(getX() - radius, getY() - radius, getZ() - radius,
				getX() + radius, getY() + radius, getZ() + radius);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double range = getStyle() == CONVERGENCE_GROUND || getStyle() == CONVERGENCE_SKY
				|| getStyle() == DIMENSIONAL_REND ? 384.0D : 224.0D;
		return distance < range * range;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		entityData.set(STYLE, Mth.clamp(tag.getInt("Style"), AETHER_BOLT, FORMULA_RUNES));
		entityData.set(STAGE, Mth.clamp(tag.getInt("Stage"), 1, 5));
		entityData.set(SCALE, Math.max(0.04F, tag.getFloat("Scale")));
		entityData.set(LENGTH, Math.max(0.04F, tag.getFloat("Length")));
		entityData.set(LIFETIME, Math.max(2, tag.getInt("Lifetime")));
		entityData.set(PRIMARY_COLOR, tag.getInt("PrimaryColor"));
		entityData.set(SECONDARY_COLOR, tag.getInt("SecondaryColor"));
		entityData.set(SEED, tag.getInt("Seed"));
		entityData.set(ORB_AMPLIFIED, tag.getBoolean("OrbAmplified"));
		entityData.set(OVERCAST, tag.getBoolean("Overcast"));
		entityData.set(OWNER, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
		entityData.set(TARGET, tag.hasUUID("Target") ? Optional.of(tag.getUUID("Target")) : Optional.empty());
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
		tag.putBoolean("Overcast", isOvercast());
		getOwnerId().ifPresent(id -> tag.putUUID("Owner", id));
		getTargetId().ifPresent(id -> tag.putUUID("Target", id));
	}
}
