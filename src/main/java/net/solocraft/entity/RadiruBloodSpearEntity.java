package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.WhiteFlameMonarchManager;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class RadiruBloodSpearEntity extends Entity {
	private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(RadiruBloodSpearEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> MANIFESTED = SynchedEntityData.defineId(RadiruBloodSpearEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(RadiruBloodSpearEntity.class, EntityDataSerializers.FLOAT);
	private final Set<UUID> struck = new HashSet<>();

	public RadiruBloodSpearEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.RADIRU_BLOOD_SPEAR.get(), level);
	}

	public RadiruBloodSpearEntity(EntityType<? extends RadiruBloodSpearEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(OWNER, Optional.empty());
		this.entityData.define(MANIFESTED, false);
		this.entityData.define(DAMAGE, 1.0F);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static void launch(ServerPlayer owner, float damage, boolean manifested) {
		ServerLevel level = owner.serverLevel();
		RadiruBloodSpearEntity spear = new RadiruBloodSpearEntity(SololevelingModEntities.RADIRU_BLOOD_SPEAR.get(), level);
		spear.entityData.set(OWNER, Optional.of(owner.getUUID()));
		spear.entityData.set(MANIFESTED, manifested);
		spear.entityData.set(DAMAGE, damage);
		Vec3 look = owner.getLookAngle().normalize();
		Vec3 start = owner.getEyePosition().add(look.scale(0.8D));
		spear.moveTo(start.x, start.y - 0.18D, start.z, owner.getYRot(), owner.getXRot());
		spear.setDeltaMovement(look.scale(manifested ? 3.2D : 2.7D));
		level.addFreshEntity(spear);
	}

	public Entity getOwner() {
		if (!(this.level() instanceof ServerLevel level))
			return null;
		return this.entityData.get(OWNER).map(level::getEntity).orElse(null);
	}

	public boolean isManifested() {
		return this.entityData.get(MANIFESTED);
	}

	@Override
	public void tick() {
		super.tick();
		Vec3 start = this.position();
		Vec3 velocity = this.getDeltaMovement();
		Vec3 end = start.add(velocity);
		if (!this.level().isClientSide && this.level() instanceof ServerLevel level) {
			BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			Vec3 travelEnd = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
			hitEntities(level, start, travelEnd);
			if (blockHit.getType() != HitResult.Type.MISS) {
				this.setPos(travelEnd);
				impact(level, travelEnd);
				this.discard();
				return;
			}
		}
		this.setPos(end);
		this.setDeltaMovement(velocity.scale(0.995D).add(0.0D, -0.006D, 0.0D));
		if (this.tickCount > (isManifested() ? 44 : 36)) {
			if (this.level() instanceof ServerLevel level)
				impact(level, this.position());
			this.discard();
		}
	}

	private void hitEntities(ServerLevel level, Vec3 start, Vec3 end) {
		Entity ownerEntity = getOwner();
		if (!(ownerEntity instanceof ServerPlayer owner))
			return;
		AABB path = new AABB(start, end).inflate(isManifested() ? 0.9D : 0.65D);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, path,
				candidate -> WhiteFlameMonarchManager.validTarget(owner, candidate)).stream()
				.sorted(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(start))).toList()) {
			if (!struck.add(target.getUUID()))
				continue;
			WhiteFlameMonarchManager.dealMagic(owner, target, this.entityData.get(DAMAGE));
			WhiteFlameMonarchManager.brand(target, owner, isManifested() ? 180 : 120, isManifested() ? 2 : 1);
			target.setSecondsOnFire(isManifested() ? 5 : 3);
			if (struck.size() >= (isManifested() ? 7 : 4)) {
				impact(level, target.getBoundingBox().getCenter());
				this.discard();
				return;
			}
		}
	}

	private void impact(ServerLevel level, Vec3 point) {
		WhiteFlameVfxEntity.spawn(level, point.x, point.y + 0.05D, point.z,
				WhiteFlameVfxEntity.SPEAR_IMPACT, isManifested() ? 3.6F : 2.5F, 1.0F, 12, 0, 0);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		if (tag.hasUUID("Owner"))
			this.entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
		this.entityData.set(MANIFESTED, tag.getBoolean("Manifested"));
		this.entityData.set(DAMAGE, tag.getFloat("Damage"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		this.entityData.get(OWNER).ifPresent(uuid -> tag.putUUID("Owner", uuid));
		tag.putBoolean("Manifested", isManifested());
		tag.putFloat("Damage", this.entityData.get(DAMAGE));
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
