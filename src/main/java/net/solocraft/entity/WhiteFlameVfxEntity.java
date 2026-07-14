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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class WhiteFlameVfxEntity extends Entity {
	public static final int LIGHTNING_BREATH = 0;
	public static final int HELLSTORM_STRIKE = 1;
	public static final int KINGS_VERDICT = 2;
	public static final int DOPPELGANGER = 3;
	public static final int HELL_GATE = 4;
	public static final int DODGE = 5;
	public static final int SPEAR_IMPACT = 6;

	private static final EntityDataAccessor<Integer> STYLE = SynchedEntityData.defineId(WhiteFlameVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(WhiteFlameVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> LENGTH = SynchedEntityData.defineId(WhiteFlameVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(WhiteFlameVfxEntity.class, EntityDataSerializers.INT);

	public WhiteFlameVfxEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.WHITE_FLAME_VFX.get(), level);
	}

	public WhiteFlameVfxEntity(EntityType<? extends WhiteFlameVfxEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(STYLE, LIGHTNING_BREATH);
		this.entityData.define(SCALE, 1.0F);
		this.entityData.define(LENGTH, 1.0F);
		this.entityData.define(LIFETIME, 12);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static WhiteFlameVfxEntity spawn(ServerLevel level, double x, double y, double z, int style,
			float scale, float length, int lifetime, float yaw, float pitch) {
		WhiteFlameVfxEntity effect = new WhiteFlameVfxEntity(SololevelingModEntities.WHITE_FLAME_VFX.get(), level);
		effect.entityData.set(STYLE, style);
		effect.entityData.set(SCALE, Math.max(0.1F, scale));
		effect.entityData.set(LENGTH, Math.max(0.1F, length));
		effect.entityData.set(LIFETIME, Math.max(2, lifetime));
		effect.moveTo(x, y, z, yaw, pitch);
		level.addFreshEntity(effect);
		return effect;
	}

	public int getStyle() {
		return this.entityData.get(STYLE);
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

	public float getFade(float partialTick) {
		float progress = (this.tickCount + partialTick) / Math.max(1.0F, getLifetime());
		float fadeIn = Math.min(1.0F, progress * 6.0F);
		float fadeOut = Math.min(1.0F, (1.0F - progress) * 4.0F);
		return Math.max(0.0F, Math.min(fadeIn, fadeOut));
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
		this.entityData.set(SCALE, tag.getFloat("Scale"));
		this.entityData.set(LENGTH, tag.getFloat("Length"));
		this.entityData.set(LIFETIME, tag.getInt("Lifetime"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Style", getStyle());
		tag.putFloat("Scale", getScale());
		tag.putFloat("Length", getLength());
		tag.putInt("Lifetime", getLifetime());
	}

	@Override
	public boolean isPickable() {
		return false;
	}
}
