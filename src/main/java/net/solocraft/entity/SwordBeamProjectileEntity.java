package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class SwordBeamProjectileEntity extends AbstractArrow implements ItemSupplier {
	public static final ItemStack PROJECTILE_ITEM = new ItemStack(Blocks.AIR);
	private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(SwordBeamProjectileEntity.class, EntityDataSerializers.FLOAT);
	private static final int LIFETIME = 42;
	private double originX;
	private double originY;
	private double originZ;

	public SwordBeamProjectileEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.SWORD_BEAM_PROJECTILE.get(), world);
	}

	public SwordBeamProjectileEntity(EntityType<? extends SwordBeamProjectileEntity> type, Level world) {
		super(type, world);
		this.setNoGravity(true);
	}

	public SwordBeamProjectileEntity(EntityType<? extends SwordBeamProjectileEntity> type, LivingEntity owner, Level world) {
		super(type, owner, world);
		this.setNoGravity(true);
		this.setOrigin(owner.getX(), owner.getEyeY(), owner.getZ());
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(ROLL, 0.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public ItemStack getItem() {
		return PROJECTILE_ITEM;
	}

	@Override
	protected ItemStack getPickupItem() {
		return PROJECTILE_ITEM;
	}

	public float getFade(float partialTick) {
		float age = this.tickCount + partialTick;
		float in = Math.min(1.0F, age / 3.0F);
		float out = Math.max(0.0F, 1.0F - age / LIFETIME);
		return in * out;
	}

	public void setOrigin(double x, double y, double z) {
		this.originX = x;
		this.originY = y;
		this.originZ = z;
	}

	public float getRoll() {
		return this.entityData.get(ROLL);
	}

	public void setRoll(float roll) {
		this.entityData.set(ROLL, roll);
	}

	@Override
	protected void doPostHurtEffects(LivingEntity entity) {
		super.doPostHurtEffects(entity);
		entity.setArrowCount(Math.max(0, entity.getArrowCount() - 1));
	}

	@Override
	protected void onHitEntity(EntityHitResult entityHitResult) {
		super.onHitEntity(entityHitResult);
		if (!this.level().isClientSide()) {
			this.discard();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult blockHitResult) {
		super.onHitBlock(blockHitResult);
		if (!this.level().isClientSide()) {
			this.discard();
		}
	}

	@Override
	public void tick() {
		this.setNoGravity(true);
		super.tick();
		if (!this.level().isClientSide()) {
			double rangeSqr = this.distanceToSqr(this.originX, this.originY, this.originZ);
			if (this.tickCount >= LIFETIME || rangeSqr >= 24.0D * 24.0D || this.inGround) {
				this.discard();
			}
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putDouble("OriginX", this.originX);
		compound.putDouble("OriginY", this.originY);
		compound.putDouble("OriginZ", this.originZ);
		compound.putFloat("Roll", this.getRoll());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		this.originX = compound.getDouble("OriginX");
		this.originY = compound.getDouble("OriginY");
		this.originZ = compound.getDouble("OriginZ");
		this.setRoll(compound.getFloat("Roll"));
	}
}
