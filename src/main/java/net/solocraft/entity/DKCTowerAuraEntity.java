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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Persistent, invisible anchor for the Demon King's Castle tower aura. Place it
 * at the center of the tower footprint and at the tower's ground level.
 */
public class DKCTowerAuraEntity extends Entity {
	private static final EntityDataAccessor<Float> RADIUS = SynchedEntityData.defineId(DKCTowerAuraEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HEIGHT = SynchedEntityData.defineId(DKCTowerAuraEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> INTENSITY = SynchedEntityData.defineId(DKCTowerAuraEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> CROWN_LIGHTNING = SynchedEntityData.defineId(DKCTowerAuraEntity.class, EntityDataSerializers.BOOLEAN);
	private static final double RENDER_DISTANCE_SQR = 512.0D * 512.0D;

	public DKCTowerAuraEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.DKC_TOWER_AURA.get(), level);
	}

	public DKCTowerAuraEntity(EntityType<? extends DKCTowerAuraEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(RADIUS, 32.0F);
		this.entityData.define(HEIGHT, 320.0F);
		this.entityData.define(INTENSITY, 1.0F);
		this.entityData.define(CROWN_LIGHTNING, true);
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setNoGravity(true);
		this.setDeltaMovement(Vec3.ZERO);
		if (this.level() instanceof ServerLevel server && this.hasCrownLightning() && this.random.nextInt(240) == 0) {
			this.strikeCrown(server);
			if (this.random.nextInt(4) == 0)
				this.strikeCrown(server);
		}
	}

	private void strikeCrown(ServerLevel server) {
		LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(server);
		if (lightning == null)
			return;
		lightning.setVisualOnly(true);
		lightning.setSilent(true);
		lightning.moveTo(this.getX(), this.getY() + this.getAuraHeight(), this.getZ());
		server.addFreshEntity(lightning);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.contains("Radius"))
			this.setAuraRadius(tag.getFloat("Radius"));
		if (tag.contains("Height"))
			this.setAuraHeight(tag.getFloat("Height"));
		if (tag.contains("Intensity"))
			this.setIntensity(tag.getFloat("Intensity"));
		if (tag.contains("CrownLightning"))
			this.setCrownLightning(tag.getBoolean("CrownLightning"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putFloat("Radius", this.getAuraRadius());
		tag.putFloat("Height", this.getAuraHeight());
		tag.putFloat("Intensity", this.getIntensity());
		tag.putBoolean("CrownLightning", this.hasCrownLightning());
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public AABB getBoundingBoxForCulling() {
		double radius = this.getAuraRadius() + 5.0D;
		return new AABB(this.getX() - radius, this.getY() - 3.0D, this.getZ() - radius,
				this.getX() + radius, this.getY() + this.getAuraHeight() + 6.0D, this.getZ() + radius);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < RENDER_DISTANCE_SQR;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void push(Entity entity) {
	}

	public float getAuraRadius() {
		return this.entityData.get(RADIUS);
	}

	public void setAuraRadius(float radius) {
		this.entityData.set(RADIUS, Mth.clamp(radius, 2.0F, 256.0F));
	}

	public float getAuraHeight() {
		return this.entityData.get(HEIGHT);
	}

	public void setAuraHeight(float height) {
		this.entityData.set(HEIGHT, Mth.clamp(height, 4.0F, 1024.0F));
	}

	public float getIntensity() {
		return this.entityData.get(INTENSITY);
	}

	public void setIntensity(float intensity) {
		this.entityData.set(INTENSITY, Mth.clamp(intensity, 0.0F, 3.0F));
	}

	public boolean hasCrownLightning() {
		return this.entityData.get(CROWN_LIGHTNING);
	}

	public void setCrownLightning(boolean crownLightning) {
		this.entityData.set(CROWN_LIGHTNING, crownLightning);
	}

	public static DKCTowerAuraEntity spawn(ServerLevel level, double x, double y, double z,
			float radius, float height, float intensity) {
		DKCTowerAuraEntity aura = new DKCTowerAuraEntity(SololevelingModEntities.DKC_TOWER_AURA.get(), level);
		aura.moveTo(x, y, z);
		aura.setAuraRadius(radius);
		aura.setAuraHeight(height);
		aura.setIntensity(intensity);
		level.addFreshEntity(aura);
		return aura;
	}
}
