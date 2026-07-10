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

public class DualWieldFlurryEntity extends Entity {
	private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(DualWieldFlurryEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> SEED = SynchedEntityData.defineId(DualWieldFlurryEntity.class, EntityDataSerializers.INT);
	private static final int LIFETIME = 15;

	public DualWieldFlurryEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.DUAL_WIELD_FLURRY.get(), world);
	}

	public DualWieldFlurryEntity(EntityType<? extends DualWieldFlurryEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(YAW, 0.0F);
		this.entityData.define(SEED, 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static void spawn(LevelAccessor world, LivingEntity owner) {
		if (!(world instanceof ServerLevel level))
			return;
		Vec3 look = owner.getLookAngle();
		DualWieldFlurryEntity flurry = new DualWieldFlurryEntity(SololevelingModEntities.DUAL_WIELD_FLURRY.get(), level);
		flurry.setYaw(owner.getYRot());
		flurry.setSeed(owner.getRandom().nextInt());
		flurry.moveTo(owner.getX() + look.x * 4.4D, owner.getY() + 1.25D + Mth.clamp(look.y, -0.45D, 0.45D) * 0.65D, owner.getZ() + look.z * 4.4D, owner.getYRot(), 0.0F);
		level.addFreshEntity(flurry);
	}

	public float getYaw() {
		return this.entityData.get(YAW);
	}

	private void setYaw(float yaw) {
		this.entityData.set(YAW, yaw);
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
		this.setSeed(compound.getInt("Seed"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		compound.putFloat("Yaw", this.getYaw());
		compound.putInt("Seed", this.getSeed());
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
