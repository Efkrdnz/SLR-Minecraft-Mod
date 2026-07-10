package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CrossStrikeEntity extends Entity {
	private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(CrossStrikeEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(CrossStrikeEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(CrossStrikeEntity.class, EntityDataSerializers.FLOAT);
	private static final int SECOND_SLASH_TICK = 3;
	private static final int LIFETIME = 15;
	private float damage;
	private boolean firstHit;
	private boolean secondHit;

	public CrossStrikeEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.CROSS_STRIKE.get(), world);
	}

	public CrossStrikeEntity(EntityType<? extends CrossStrikeEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER_UUID, Optional.empty());
		this.entityData.define(YAW, 0.0F);
		this.entityData.define(SCALE, 1.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static void spawn(LevelAccessor world, LivingEntity owner, float damage, float scale) {
		if (!(world instanceof ServerLevel level))
			return;
		Vec3 look = owner.getLookAngle();
		Vec3 eye = owner.getEyePosition();
		CrossStrikeEntity cross = new CrossStrikeEntity(SololevelingModEntities.CROSS_STRIKE.get(), level);
		cross.setOwner(owner);
		cross.setYaw(owner.getYRot());
		cross.setScale(scale);
		cross.damage = damage;
		cross.moveTo(eye.x + look.x * 3.65D, eye.y - 0.15D + look.y * 3.65D, eye.z + look.z * 3.65D, owner.getYRot(), owner.getXRot());
		level.addFreshEntity(cross);
	}

	public void setOwner(Entity owner) {
		this.entityData.set(OWNER_UUID, Optional.of(owner.getUUID()));
	}

	public Entity getOwner() {
		Optional<UUID> uuid = this.entityData.get(OWNER_UUID);
		if (uuid.isEmpty() || !(this.level() instanceof ServerLevel serverLevel))
			return null;
		return serverLevel.getEntity(uuid.get());
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

	public float getFade(float partialTick) {
		float age = this.tickCount + partialTick;
		return Math.max(0.0F, 1.0F - age / LIFETIME);
	}

	public float getSecondSlashProgress(float partialTick) {
		float t = (this.tickCount + partialTick - SECOND_SLASH_TICK) / 2.0F;
		return Math.max(0.0F, Math.min(1.0F, t));
	}

	public float getMergeProgress(float partialTick) {
		float t = (this.tickCount + partialTick - SECOND_SLASH_TICK - 2.0F) / 7.0F;
		return Math.max(0.0F, Math.min(1.0F, t));
	}

	@Override
	public void tick() {
		super.tick();
		this.noPhysics = true;
		this.setDeltaMovement(Vec3.ZERO);
		if (!this.level().isClientSide) {
			if (!this.firstHit) {
				this.firstHit = true;
				this.damageTargets(0.45F);
			}
			if (!this.secondHit && this.tickCount >= SECOND_SLASH_TICK) {
				this.secondHit = true;
				this.damageTargets(0.7F);
			}
		}
		if (this.tickCount >= LIFETIME) {
			this.discard();
		}
	}

	private void damageTargets(float multiplier) {
		Entity owner = this.getOwner();
		if (!(owner instanceof LivingEntity livingOwner))
			return;
		float scale = this.getScale();
		AABB hitBox = this.getBoundingBox().inflate(3.55D * scale, 1.55D * scale, 3.55D * scale);
		List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, target -> target.isAlive() && target != livingOwner);
		DamageSource source = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:fighter"))),
				livingOwner);
		Vec3 carry = livingOwner.getLookAngle().multiply(0.32D, 0.0D, 0.32D).add(0.0D, 0.035D, 0.0D);
		for (LivingEntity target : targets) {
			Vec3 motionBeforeHit = target.getDeltaMovement();
			target.invulnerableTime = 0;
			boolean hurt = target.hurt(source, this.damage * multiplier);
			target.invulnerableTime = 0;
			target.setDeltaMovement(motionBeforeHit.add(carry));
			target.hurtMarked = true;
			if (hurt && livingOwner instanceof Player player) {
				target.setLastHurtByPlayer(player);
			}
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag compound) {
		if (compound.hasUUID("Owner")) {
			this.entityData.set(OWNER_UUID, Optional.of(compound.getUUID("Owner")));
		}
		this.setYaw(compound.getFloat("Yaw"));
		this.setScale(compound.getFloat("Scale"));
		this.damage = compound.getFloat("Damage");
		this.firstHit = compound.getBoolean("FirstHit");
		this.secondHit = compound.getBoolean("SecondHit");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		this.entityData.get(OWNER_UUID).ifPresent(uuid -> compound.putUUID("Owner", uuid));
		compound.putFloat("Yaw", this.getYaw());
		compound.putFloat("Scale", this.getScale());
		compound.putFloat("Damage", this.damage);
		compound.putBoolean("FirstHit", this.firstHit);
		compound.putBoolean("SecondHit", this.secondHit);
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
