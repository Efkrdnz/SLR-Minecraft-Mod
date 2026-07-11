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
import net.minecraft.world.phys.Vec3;

public class RulersAuthorityAuraEntity extends Entity {
    private static final EntityDataAccessor<Integer> TARGET_ID = SynchedEntityData.defineId(RulersAuthorityAuraEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TARGET_WIDTH = SynchedEntityData.defineId(RulersAuthorityAuraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TARGET_HEIGHT = SynchedEntityData.defineId(RulersAuthorityAuraEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> AUTHORITY = SynchedEntityData.defineId(RulersAuthorityAuraEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> RESISTED = SynchedEntityData.defineId(RulersAuthorityAuraEntity.class, EntityDataSerializers.BOOLEAN);
    private int missingTargetTicks;

    public RulersAuthorityAuraEntity(PlayMessages.SpawnEntity packet, Level level) {
        this(SololevelingModEntities.RULERS_AUTHORITY_AURA.get(), level);
    }

    public RulersAuthorityAuraEntity(EntityType<? extends RulersAuthorityAuraEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    public static RulersAuthorityAuraEntity spawn(ServerLevel level, Entity target, boolean authority, boolean resisted) {
        RulersAuthorityAuraEntity aura = new RulersAuthorityAuraEntity(SololevelingModEntities.RULERS_AUTHORITY_AURA.get(), level);
        aura.entityData.set(TARGET_ID, target.getId());
        aura.entityData.set(TARGET_WIDTH, Mth.clamp(target.getBbWidth(), 0.35F, 12.0F));
        aura.entityData.set(TARGET_HEIGHT, Mth.clamp(target.getBbHeight(), 0.35F, 16.0F));
        aura.entityData.set(AUTHORITY, authority);
        aura.entityData.set(RESISTED, resisted);
        aura.moveTo(target.getX(), target.getY(), target.getZ());
        level.addFreshEntity(aura);
        return aura;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TARGET_ID, -1);
        this.entityData.define(TARGET_WIDTH, 0.6F);
        this.entityData.define(TARGET_HEIGHT, 1.8F);
        this.entityData.define(AUTHORITY, false);
        this.entityData.define(RESISTED, false);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public Entity getTarget() {
        int id = this.entityData.get(TARGET_ID);
        return id < 0 ? null : this.level().getEntity(id);
    }

    public float getTargetWidth() {
        return this.entityData.get(TARGET_WIDTH);
    }

    public float getTargetHeight() {
        return this.entityData.get(TARGET_HEIGHT);
    }

    public boolean isAuthority() {
        return this.entityData.get(AUTHORITY);
    }

    public boolean isResisted() {
        return this.entityData.get(RESISTED);
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.setNoGravity(true);
        this.setDeltaMovement(Vec3.ZERO);
        Entity target = this.getTarget();
        if (target != null && !target.isRemoved()) {
            this.missingTargetTicks = 0;
            this.setPos(target.getX(), target.getY(), target.getZ());
        } else if (++this.missingTargetTicks > 10 && !this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(TARGET_ID, tag.getInt("TargetId"));
        this.entityData.set(TARGET_WIDTH, tag.getFloat("TargetWidth"));
        this.entityData.set(TARGET_HEIGHT, tag.getFloat("TargetHeight"));
        this.entityData.set(AUTHORITY, tag.getBoolean("Authority"));
        this.entityData.set(RESISTED, tag.getBoolean("Resisted"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("TargetId", this.entityData.get(TARGET_ID));
        tag.putFloat("TargetWidth", this.getTargetWidth());
        tag.putFloat("TargetHeight", this.getTargetHeight());
        tag.putBoolean("Authority", this.isAuthority());
        tag.putBoolean("Resisted", this.isResisted());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0D;
    }
}
