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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
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

public class BasicAttackSlashEntity extends Entity {
	public static final int STYLE_FIST = 0;
	public static final int STYLE_SWORD = 1;
	public static final int STYLE_DAGGER = 2;
	public static final int STYLE_DUAL_DAGGER = 3;

	private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(BasicAttackSlashEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(BasicAttackSlashEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> SWING_INDEX = SynchedEntityData.defineId(BasicAttackSlashEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> ROLL = SynchedEntityData.defineId(BasicAttackSlashEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(BasicAttackSlashEntity.class, EntityDataSerializers.FLOAT);
	private static final int LIFETIME = 8;
	private float damage;
	private boolean dealtDamage;

	public BasicAttackSlashEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.BASIC_ATTACK_SLASH.get(), world);
	}

	public BasicAttackSlashEntity(EntityType<? extends BasicAttackSlashEntity> type, Level world) {
		super(type, world);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER_UUID, Optional.empty());
		this.entityData.define(STYLE, STYLE_FIST);
		this.entityData.define(SWING_INDEX, 0);
		this.entityData.define(ROLL, 0.0F);
		this.entityData.define(SCALE, 1.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static void spawn(LevelAccessor world, LivingEntity owner, int style, int swingIndex, float damage, float scale) {
		if (!(world instanceof ServerLevel level))
			return;
		Vec3 look = owner.getLookAngle();
		Vec3 eye = owner.getEyePosition();
		Vec3 right = new Vec3(-look.z, 0.0D, look.x);
		if (right.lengthSqr() < 0.001D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		} else {
			right = right.normalize();
		}
		double side = switch (style) {
			case STYLE_DUAL_DAGGER -> ((swingIndex % 2) == 0 ? -0.18D : 0.18D);
			case STYLE_DAGGER -> ((swingIndex % 2) == 0 ? -0.1D : 0.1D);
			default -> 0.0D;
		};
		double forward = style == STYLE_FIST ? 1.25D : 1.9D;
		double yOffset = style == STYLE_FIST ? -0.35D : -0.22D + ((swingIndex % 3) - 1) * 0.08D;
		BasicAttackSlashEntity slash = new BasicAttackSlashEntity(SololevelingModEntities.BASIC_ATTACK_SLASH.get(), level);
		slash.setOwner(owner);
		slash.setStyle(style);
		slash.setSwingIndex(swingIndex);
		slash.setScale(scale);
		slash.damage = damage;
		slash.setRoll(rollFor(style, swingIndex));
		slash.moveTo(eye.x + look.x * forward + right.x * side, eye.y + yOffset + look.y * forward, eye.z + look.z * forward + right.z * side, owner.getYRot(), owner.getXRot());
		level.addFreshEntity(slash);
	}

	private static float rollFor(int style, int swingIndex) {
		return switch (style) {
			case STYLE_FIST -> 0.0F;
			case STYLE_DAGGER -> (swingIndex % 2 == 0 ? -1.0F : 1.0F) * (28.0F + swingIndex * 8.0F);
			case STYLE_DUAL_DAGGER -> (swingIndex % 2 == 0 ? -42.0F : 42.0F);
			default -> (swingIndex % 2 == 0 ? -1.0F : 1.0F) * (18.0F + swingIndex * 10.0F);
		};
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

	public int getStyle() {
		return this.entityData.get(STYLE);
	}

	private void setStyle(int style) {
		this.entityData.set(STYLE, style);
	}

	public int getSwingIndex() {
		return this.entityData.get(SWING_INDEX);
	}

	private void setSwingIndex(int swingIndex) {
		this.entityData.set(SWING_INDEX, swingIndex);
	}

	public float getRoll() {
		return this.entityData.get(ROLL);
	}

	private void setRoll(float roll) {
		this.entityData.set(ROLL, roll);
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
		int style = this.getStyle();
		Vec3 look = livingOwner.getLookAngle();
		Vec3 eye = livingOwner.getEyePosition();
		Vec3 center = eye.add(look.scale(hitForward(style))).add(0.0D, style == STYLE_FIST ? -0.35D : -0.25D, 0.0D);
		double width = style == STYLE_FIST ? 1.35D : style == STYLE_SWORD ? 2.2D : 1.65D;
		double height = style == STYLE_FIST ? 0.95D : 1.15D;
		AABB hitBox = new AABB(center, center).inflate(width, height, width);
		List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, target -> target.isAlive() && target != livingOwner);
		DamageSource source = new DamageSource(this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.PLAYER_ATTACK), livingOwner);
		Vec3 carry = look.multiply(knockback(style), 0.0D, knockback(style)).add(0.0D, style == STYLE_FIST ? 0.08D : 0.035D, 0.0D);
		for (LivingEntity target : targets) {
			Vec3 motionBeforeHit = target.getDeltaMovement();
			target.invulnerableTime = 0;
			boolean hurt = target.hurt(source, this.damage);
			target.invulnerableTime = 0;
			if (style == STYLE_DAGGER || style == STYLE_DUAL_DAGGER) {
				target.hurtTime = 0;
				target.hurtDuration = 0;
			}
			target.setDeltaMovement(motionBeforeHit.add(carry));
			target.hurtMarked = true;
			if (hurt && livingOwner instanceof Player player) {
				target.setLastHurtByPlayer(player);
			}
		}
	}

	private static double hitForward(int style) {
		return style == STYLE_FIST ? 1.25D : style == STYLE_SWORD ? 2.05D : 1.9D;
	}

	private static double knockback(int style) {
		return style == STYLE_FIST ? 0.28D : style == STYLE_SWORD ? 0.18D : 0.12D;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag compound) {
		if (compound.hasUUID("Owner")) {
			this.entityData.set(OWNER_UUID, Optional.of(compound.getUUID("Owner")));
		}
		this.setStyle(compound.getInt("Style"));
		this.setSwingIndex(compound.getInt("SwingIndex"));
		this.setRoll(compound.getFloat("Roll"));
		this.setScale(compound.getFloat("Scale"));
		this.damage = compound.getFloat("Damage");
		this.dealtDamage = compound.getBoolean("DealtDamage");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag compound) {
		this.entityData.get(OWNER_UUID).ifPresent(uuid -> compound.putUUID("Owner", uuid));
		compound.putInt("Style", this.getStyle());
		compound.putInt("SwingIndex", this.getSwingIndex());
		compound.putFloat("Roll", this.getRoll());
		compound.putFloat("Scale", this.getScale());
		compound.putFloat("Damage", this.damage);
		compound.putBoolean("DealtDamage", this.dealtDamage);
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
