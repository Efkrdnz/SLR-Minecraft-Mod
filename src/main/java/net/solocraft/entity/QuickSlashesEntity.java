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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;

public class QuickSlashesEntity extends Entity {
	private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(QuickSlashesEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(QuickSlashesEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(QuickSlashesEntity.class, EntityDataSerializers.INT);
	private static final int LIFETIME = 12;

	public QuickSlashesEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.QUICK_SLASHES.get(), world);
	}

	public QuickSlashesEntity(EntityType<? extends QuickSlashesEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(YAW, 0.0F);
		this.entityData.define(SCALE, 1.0F);
		this.entityData.define(SEED, 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static void spawn(LevelAccessor world, LivingEntity owner, Entity target) {
		if (!(world instanceof ServerLevel level))
			return;
		QuickSlashesEntity slashes = new QuickSlashesEntity(SololevelingModEntities.QUICK_SLASHES.get(), level);
		slashes.setYaw(owner.getYRot());
		slashes.setSeed(owner.getRandom().nextInt());
		slashes.setScale(Mth.clamp((float) Math.max(target.getBbWidth(), target.getBbHeight() * 0.62F), 0.85F, 2.35F));
		slashes.moveTo(target.getX(), target.getY() + target.getBbHeight() * 0.55D, target.getZ(), owner.getYRot(), 0.0F);
		level.addFreshEntity(slashes);
	}

	public float getYaw() {
		return this.entityData.get(YAW);
	}

	private void setYaw(float yaw) {
		this.entityData.set(YAW, yaw);
	}

	public float getScale() {
		return this.entityData.get(SCALE);
	}

	private void setScale(float scale) {
		this.entityData.set(SCALE, scale);
	}

	public int getSeed() {
		return this.entityData.get(SEED);
	}

	private void setSeed(int seed) {
		this.entityData.set(SEED, seed);
	}

	public float getFade(float partialTick) {
		float age = this.tickCount + partialTick;
		return Math.max(0.0F, 1.0F - age / LIFETIME);
	}

	public float getReveal(float partialTick) {
		float age = this.tickCount + partialTick;
		return Math.max(0.0F, Math.min(1.0F, age / 4.0F));
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setDeltaMovement(Vec3.ZERO);
		if (this.tickCount >= LIFETIME) {
			this.discard();
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag compound) {
		this.setYaw(compound.getFloat("Yaw"));
		this.setScale(compound.getFloat("Scale"));
		this.setSeed(compound.getInt("Seed"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		compound.putFloat("Yaw", this.getYaw());
		compound.putFloat("Scale", this.getScale());
		compound.putInt("Seed", this.getSeed());
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
