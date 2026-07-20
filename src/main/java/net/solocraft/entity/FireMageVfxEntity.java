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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * A short-lived, server-spawned carrier for Fire Mage shader geometry. Gameplay
 * stays in {@code FireMageSpellManager}; this entity only synchronizes visuals.
 */
public class FireMageVfxEntity extends Entity {
	public static final int FLAME_WEAVING = 0;
	public static final int IGNITION_ORB = 1;
	public static final int ORB_IMPACT = 2;
	public static final int INFERNO_LANCE = 3;
	public static final int FLASHFIRE = 4;
	public static final int CREMATION = 5;
	public static final int FURNACE_DOMINION = 6;
	public static final int HEAVENFALL = 7;
	public static final int HEAVENFALL_IMPACT = 8;
	public static final int SCORCH = 9;

	private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(FireMageVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STAGE = SynchedEntityData.defineId(FireMageVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(FireMageVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(FireMageVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(FireMageVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PRIMARY_COLOR = SynchedEntityData.defineId(FireMageVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SECONDARY_COLOR = SynchedEntityData.defineId(FireMageVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(FireMageVfxEntity.class, EntityDataSerializers.INT);

	public FireMageVfxEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.FIRE_MAGE_VFX.get(), level);
	}

	public FireMageVfxEntity(EntityType<? extends FireMageVfxEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(STYLE, FLAME_WEAVING);
		this.entityData.define(STAGE, 1);
		this.entityData.define(SCALE, 1.0F);
		this.entityData.define(LENGTH, 1.0F);
		this.entityData.define(LIFETIME, 12);
		this.entityData.define(PRIMARY_COLOR, 0xFF5A0A);
		this.entityData.define(SECONDARY_COLOR, 0xFFD34A);
		this.entityData.define(SEED, 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static FireMageVfxEntity spawn(ServerLevel level, double x, double y, double z,
			int style, int stage, float scale, float length, int lifetime, float yaw, float pitch) {
		return spawn(level, x, y, z, style, stage, scale, length, lifetime, yaw, pitch,
				0xFF5A0A, 0xFFD34A);
	}

	public static FireMageVfxEntity spawn(ServerLevel level, double x, double y, double z,
			int style, int stage, float scale, float length, int lifetime, float yaw, float pitch,
			int primaryColor, int secondaryColor) {
		FireMageVfxEntity effect = new FireMageVfxEntity(SololevelingModEntities.FIRE_MAGE_VFX.get(), level);
		effect.entityData.set(STYLE, Mth.clamp(style, FLAME_WEAVING, SCORCH));
		effect.entityData.set(STAGE, Mth.clamp(stage, 1, 5));
		effect.entityData.set(SCALE, Math.max(0.05F, scale));
		effect.entityData.set(LENGTH, Math.max(0.05F, length));
		effect.entityData.set(LIFETIME, Math.max(2, lifetime));
		effect.entityData.set(PRIMARY_COLOR, primaryColor & 0xFFFFFF);
		effect.entityData.set(SECONDARY_COLOR, secondaryColor & 0xFFFFFF);
		effect.entityData.set(SEED, level.getRandom().nextInt());
		effect.moveTo(x, y, z, yaw, pitch);
		level.addFreshEntity(effect);
		return effect;
	}

	public int getStyle() {
		return this.entityData.get(STYLE);
	}

	public int getStage() {
		return this.entityData.get(STAGE);
	}

	public float getScale() {
		return this.entityData.get(SCALE);
	}

	public float getLength() {
		return this.entityData.get(LENGTH);
	}

	public int getLifetime() {
		return this.entityData.get(LIFETIME);
	}

	public int getPrimaryColor() {
		return this.entityData.get(PRIMARY_COLOR);
	}

	public int getSecondaryColor() {
		return this.entityData.get(SECONDARY_COLOR);
	}

	public int getSeed() {
		return this.entityData.get(SEED);
	}

	public float getProgress(float partialTick) {
		return Mth.clamp((this.tickCount + partialTick) / Math.max(1.0F, getLifetime()), 0.0F, 1.0F);
	}

	public float getFade(float partialTick) {
		float progress = getProgress(partialTick);
		float fadeIn = Mth.clamp(progress * 9.0F, 0.0F, 1.0F);
		float fadeOut = Mth.clamp((1.0F - progress) * (getStyle() == FURNACE_DOMINION ? 8.0F : 4.5F), 0.0F, 1.0F);
		return Math.min(fadeIn, fadeOut);
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setDeltaMovement(0.0D, 0.0D, 0.0D);
		if (this.tickCount >= getLifetime())
			this.discard();
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(STYLE, tag.getInt("Style"));
		this.entityData.set(STAGE, Mth.clamp(tag.getInt("Stage"), 1, 5));
		this.entityData.set(SCALE, Math.max(0.05F, tag.getFloat("Scale")));
		this.entityData.set(LENGTH, Math.max(0.05F, tag.getFloat("Length")));
		this.entityData.set(LIFETIME, Math.max(2, tag.getInt("Lifetime")));
		this.entityData.set(PRIMARY_COLOR, tag.getInt("PrimaryColor"));
		this.entityData.set(SECONDARY_COLOR, tag.getInt("SecondaryColor"));
		this.entityData.set(SEED, tag.getInt("Seed"));
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
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		double radius = Math.max(4.0D, Math.max(getScale(), getLength()) * 2.2D);
		return new AABB(getX() - radius, getY() - radius, getZ() - radius,
				getX() + radius, getY() + radius, getZ() + radius);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		double range = getStyle() == HEAVENFALL || getStyle() == HEAVENFALL_IMPACT ? 512.0D : 256.0D;
		return distance < range * range;
	}
}
