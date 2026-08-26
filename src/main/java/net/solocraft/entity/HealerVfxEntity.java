package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Synced visual carrier for Healer effects.
 *
 * <p>Modelled on {@link BarrierVfxEntity}: one non-colliding entity per effect,
 * carrying everything the renderer needs and expiring on its own lifetime. This
 * is what lets the server stop scheduling per-tick particle work — the old
 * Heal Beam queued hundreds of delayed tasks per cast.</p>
 */
public class HealerVfxEntity extends Entity {
	public static final int BEAM = 0;
	public static final int SIGIL = 1;
	public static final int FIELD = 2;
	public static final int WAVE = 3;

	private static final EntityDataAccessor<Integer> VARIANT =
			SynchedEntityData.defineId(HealerVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> STAGE =
			SynchedEntityData.defineId(HealerVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> SCALE =
			SynchedEntityData.defineId(HealerVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> LIFETIME =
			SynchedEntityData.defineId(HealerVfxEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COLOR =
			SynchedEntityData.defineId(HealerVfxEntity.class, EntityDataSerializers.INT);
	/** Beam endpoint, stored relative to this entity's own position. */
	private static final EntityDataAccessor<Float> END_X =
			SynchedEntityData.defineId(HealerVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> END_Y =
			SynchedEntityData.defineId(HealerVfxEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> END_Z =
			SynchedEntityData.defineId(HealerVfxEntity.class, EntityDataSerializers.FLOAT);

	private int age;

	public HealerVfxEntity(EntityType<? extends HealerVfxEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	/** Spawns a beam between two world points. */
	public static void beam(ServerLevel level, Vec3 from, Vec3 to, int stage,
			int color, int lifetime) {
		HealerVfxEntity effect = create(level, from, BEAM, stage, 1.0F, color, lifetime);
		if (effect == null)
			return;
		Vec3 delta = to.subtract(from);
		effect.entityData.set(END_X, (float) delta.x);
		effect.entityData.set(END_Y, (float) delta.y);
		effect.entityData.set(END_Z, (float) delta.z);
		level.addFreshEntity(effect);
	}

	/** Spawns a ground sigil whose ring count reads as the output stage. */
	public static void sigil(ServerLevel level, Vec3 at, int stage, float scale,
			int color, int lifetime) {
		HealerVfxEntity effect = create(level, at, SIGIL, stage, scale, color, lifetime);
		if (effect != null)
			level.addFreshEntity(effect);
	}

	public static void field(ServerLevel level, Vec3 at, int stage, float radius,
			int color, int lifetime) {
		HealerVfxEntity effect = create(level, at, FIELD, stage, radius, color, lifetime);
		if (effect != null)
			level.addFreshEntity(effect);
	}

	public static void wave(ServerLevel level, Vec3 at, int stage, float radius,
			int color, int lifetime) {
		HealerVfxEntity effect = create(level, at, WAVE, stage, radius, color, lifetime);
		if (effect != null)
			level.addFreshEntity(effect);
	}

	private static HealerVfxEntity create(ServerLevel level, Vec3 at, int variant,
			int stage, float scale, int color, int lifetime) {
		EntityType<HealerVfxEntity> type = SololevelingModEntities.HEALER_VFX.get();
		HealerVfxEntity effect = new HealerVfxEntity(type, level);
		effect.moveTo(at.x, at.y, at.z, 0.0F, 0.0F);
		effect.entityData.set(VARIANT, variant);
		effect.entityData.set(STAGE, Math.max(1, Math.min(5, stage)));
		effect.entityData.set(SCALE, Math.max(0.1F, scale));
		effect.entityData.set(COLOR, color);
		effect.entityData.set(LIFETIME, Math.max(1, lifetime));
		return effect;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(VARIANT, BEAM);
		builder.define(STAGE, 1);
		builder.define(SCALE, 1.0F);
		builder.define(LIFETIME, 20);
		builder.define(COLOR, 0xFF57E08C);
		builder.define(END_X, 0.0F);
		builder.define(END_Y, 0.0F);
		builder.define(END_Z, 0.0F);
	}

	@Override
	public void tick() {
		super.tick();
		if (++age >= getLifetime())
			discard();
	}

	public int getVariant() {
		return entityData.get(VARIANT);
	}

	public int getStage() {
		return entityData.get(STAGE);
	}

	public float getScale() {
		return entityData.get(SCALE);
	}

	public int getLifetime() {
		return entityData.get(LIFETIME);
	}

	public int getColor() {
		return entityData.get(COLOR);
	}

	public Vec3 getEndOffset() {
		return new Vec3(entityData.get(END_X), entityData.get(END_Y), entityData.get(END_Z));
	}

	/** 0..1 progress through this effect's lifetime, for fades and expansion. */
	public float progress(float partialTick) {
		return Math.min(1.0F, (age + partialTick) / Math.max(1.0F, getLifetime()));
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		age = tag.getInt("Age");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.putInt("Age", age);
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 6400.0D;
	}
}
