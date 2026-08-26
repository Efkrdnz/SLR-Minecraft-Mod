
package net.solocraft.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.solocraft.init.SololevelingModEntities;

import net.minecraft.core.registries.BuiltInRegistries;
import net.solocraft.network.compat.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.solocraft.entity.ai.LegacyMeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public class DemonEntity extends Monster implements GeoEntity {
	private static final int BODY_BROAD = 0;
	private static final int BODY_THIN = 1;
	private static final int TEXTURE_VARIANT_COUNT = 3;
	private static final float MIN_VISUAL_SCALE = 0.9f;
	private static final float MAX_NATURAL_VISUAL_SCALE = 1.12f;
	private static final float MAX_VISUAL_SCALE = 1.70f;
	private static final String BODY_VARIANT_TAG = "DemonBodyVariant";
	private static final String TEXTURE_VARIANT_TAG = "DemonTextureVariant";
	private static final String VISUAL_SCALE_TAG = "DemonVisualScale";
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> BODY_VARIANT = SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> TEXTURE_VARIANT = SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Float> VISUAL_SCALE = SynchedEntityData.defineId(DemonEntity.class, EntityDataSerializers.FLOAT);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";

	public DemonEntity(EntityType<DemonEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SHOOT, false);
		builder.define(ANIMATION, "undefined");
		builder.define(BODY_VARIANT, BODY_BROAD);
		builder.define(TEXTURE_VARIANT, 0);
		builder.define(VISUAL_SCALE, 1.0f);
	}

	public boolean isThinVariant() {
		return this.entityData.get(BODY_VARIANT) == BODY_THIN;
	}

	public int getTextureVariant() {
		return this.entityData.get(TEXTURE_VARIANT);
	}

	public float getVisualScale() {
		return this.entityData.get(VISUAL_SCALE);
	}

	/** Allows authored elite variants to grow beyond the ordinary random range. */
	public void setVisualScale(float scale) {
		this.entityData.set(VISUAL_SCALE, Math.max(MIN_VISUAL_SCALE, Math.min(MAX_VISUAL_SCALE, scale)));
	}

	public void randomizeAppearance() {
		this.entityData.set(BODY_VARIANT, this.random.nextBoolean() ? BODY_THIN : BODY_BROAD);
		this.entityData.set(TEXTURE_VARIANT, this.random.nextInt(TEXTURE_VARIANT_COUNT));
		this.setVisualScale(MIN_VISUAL_SCALE
				+ this.random.nextFloat() * (MAX_NATURAL_VISUAL_SCALE - MIN_VISUAL_SCALE));
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new LegacyMeleeAttackGoal(this, 1.2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return 6.25;
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1));
		this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		CompoundTag data = getPersistentData();
		if (data.getBoolean("radiru_resident") || data.getBoolean("radiru_training_dummy"))
			return false;
		return super.shouldDespawnInPeaceful();
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.generic.death"));
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.IN_FIRE))
			return false;
		return super.hurt(source, amount);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata) {
		SpawnGroupData spawnData = super.finalizeSpawn(world, difficulty, reason, livingdata);
		this.randomizeAppearance();
		return spawnData;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt(BODY_VARIANT_TAG, this.entityData.get(BODY_VARIANT));
		compound.putInt(TEXTURE_VARIANT_TAG, this.getTextureVariant());
		compound.putFloat(VISUAL_SCALE_TAG, this.getVisualScale());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains(BODY_VARIANT_TAG) && compound.contains(TEXTURE_VARIANT_TAG) && compound.contains(VISUAL_SCALE_TAG)) {
			this.entityData.set(BODY_VARIANT, compound.getInt(BODY_VARIANT_TAG) == BODY_THIN ? BODY_THIN : BODY_BROAD);
			this.entityData.set(TEXTURE_VARIANT, Math.max(0, Math.min(TEXTURE_VARIANT_COUNT - 1, compound.getInt(TEXTURE_VARIANT_TAG))));
			this.setVisualScale(compound.getFloat(VISUAL_SCALE_TAG));
		} else {
			// Existing demons saved with the former fixed appearance migrate once when loaded.
			this.randomizeAppearance();
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		this.refreshDimensions();
		net.solocraft.dkc.DkcWaveRuntime.tick(this);
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose p_33597_) {
		float eliteCollisionScale = this.getVisualScale() <= MAX_NATURAL_VISUAL_SCALE
				? 1.0F
				: Math.min(1.4F, this.getVisualScale() / MAX_NATURAL_VISUAL_SCALE);
		return super.getDefaultDimensions(p_33597_).scale(0.8F * eliteCollisionScale);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 60);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 12);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0.2);
		return builder;
	}

	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			if ((event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F))

			) {
				String animation = this.isThinVariant() ? "animation.el_demon_thin.pursuit" : "animation.el_demon.walk";
				return event.setAndContinue(RawAnimation.begin().thenLoop(animation));
			}
			String animation = this.isThinVariant() ? "animation.el_demon_thin.idle" : "animation.el_demon.idle";
			return event.setAndContinue(RawAnimation.begin().thenLoop(animation));
		}
		return PlayState.STOP;
	}

	private PlayState procedurePredicate(AnimationState event) {
		if (!animationprocedure.equals("empty") && event.getController().getAnimationState() == AnimationController.State.STOPPED) {
			event.getController().setAnimation(RawAnimation.begin().thenPlay(this.animationprocedure));
			if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
				this.animationprocedure = "empty";
				event.getController().forceAnimationReset();
			}
		} else if (animationprocedure.equals("empty")) {
			return PlayState.STOP;
		}
		return PlayState.CONTINUE;
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime == 20) {
			this.remove(DemonEntity.RemovalReason.KILLED);
			this.dropExperience(this.getKillCredit());
		}
	}

	public String getSyncedAnimation() {
		return this.entityData.get(ANIMATION);
	}

	public void setAnimation(String animation) {
		this.entityData.set(ANIMATION, animation);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		data.add(new AnimationController<>(this, "movement", 4, this::movementPredicate));
		data.add(new AnimationController<>(this, "procedure", 4, this::procedurePredicate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
