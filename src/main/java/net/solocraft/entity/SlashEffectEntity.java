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

public class SlashEffectEntity extends Entity {
	private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(SlashEffectEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SlashEffectEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(SlashEffectEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(SlashEffectEntity.class, EntityDataSerializers.INT);
	private static final int LIFETIME = 10;
	private float damage;
	private boolean dealtDamage;

	public SlashEffectEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.SLASH_EFFECT.get(), world);
	}

	public SlashEffectEntity(EntityType<? extends SlashEffectEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER_UUID, Optional.empty());
		this.entityData.define(SCALE, 1.0F);
		this.entityData.define(ROLL, 0.0F);
		this.entityData.define(VARIANT, 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static void spawn(LevelAccessor world, LivingEntity owner, double x, double y, double z, float yaw, float pitch, float roll, float scale, float damage, int variant) {
		if (!(world instanceof ServerLevel level))
			return;
		SlashEffectEntity slash = new SlashEffectEntity(SololevelingModEntities.SLASH_EFFECT.get(), level);
		slash.setOwner(owner);
		slash.setScale(scale);
		slash.setRoll(roll);
		slash.setVariant(variant);
		slash.damage = damage;
		slash.moveTo(x, y, z, yaw, pitch);
		level.addFreshEntity(slash);
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

	public float getScale() {
		return this.entityData.get(SCALE);
	}

	public void setScale(float scale) {
		this.entityData.set(SCALE, scale);
	}

	public float getRoll() {
		return this.entityData.get(ROLL);
	}

	public void setRoll(float roll) {
		this.entityData.set(ROLL, roll);
	}

	public int getVariant() {
		return this.entityData.get(VARIANT);
	}

	public void setVariant(int variant) {
		this.entityData.set(VARIANT, variant);
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
		if (!this.level().isClientSide && !this.dealtDamage) {
			this.dealtDamage = true;
			this.damageTargets();
		}
		if (this.tickCount >= LIFETIME) {
			this.discard();
		}
	}

	private void damageTargets() {
		Entity owner = this.getOwner();
		if (!(owner instanceof LivingEntity livingOwner))
			return;
		float scale = this.getScale();
		AABB hitBox = this.getBoundingBox().inflate(1.35D * scale, 0.9D * scale, 1.35D * scale);
		List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, target -> target.isAlive() && target != livingOwner);
		DamageSource source = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:fighter"))),
				livingOwner);
		Vec3 slashCarry = livingOwner.getLookAngle().multiply(0.16D, 0.0D, 0.16D).add(0.0D, 0.025D, 0.0D);
		for (LivingEntity target : targets) {
			Vec3 motionBeforeHit = target.getDeltaMovement();
			target.invulnerableTime = 0;
			boolean hurt = target.hurt(source, this.damage);
			target.invulnerableTime = 0;
			target.hurtTime = 0;
			target.hurtDuration = 0;
			target.setDeltaMovement(motionBeforeHit.add(slashCarry));
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
		this.setScale(compound.getFloat("Scale"));
		this.setRoll(compound.getFloat("Roll"));
		this.setVariant(compound.getInt("Variant"));
		this.damage = compound.getFloat("Damage");
		this.dealtDamage = compound.getBoolean("DealtDamage");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		this.entityData.get(OWNER_UUID).ifPresent(uuid -> compound.putUUID("Owner", uuid));
		compound.putFloat("Scale", this.getScale());
		compound.putFloat("Roll", this.getRoll());
		compound.putInt("Variant", this.getVariant());
		compound.putFloat("Damage", this.damage);
		compound.putBoolean("DealtDamage", this.dealtDamage);
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
