
package net.solocraft.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
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
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public class DemonEntity extends Monster implements GeoEntity {
	private static final int BODY_BROAD = 0;
	private static final int BODY_THIN = 1;
	private static final int TEXTURE_VARIANT_COUNT = 3;
	private static final float MIN_VISUAL_SCALE = 0.9f;
	private static final float MAX_VISUAL_SCALE = 1.12f;
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

	public DemonEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.DEMON.get(), world);
	}

	public DemonEntity(EntityType<DemonEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(SHOOT, false);
		this.entityData.define(ANIMATION, "undefined");
		this.entityData.define(BODY_VARIANT, BODY_BROAD);
		this.entityData.define(TEXTURE_VARIANT, 0);
		this.entityData.define(VISUAL_SCALE, 1.0f);
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

	public void randomizeAppearance() {
		this.entityData.set(BODY_VARIANT, this.random.nextBoolean() ? BODY_THIN : BODY_BROAD);
		this.entityData.set(TEXTURE_VARIANT, this.random.nextInt(TEXTURE_VARIANT_COUNT));
		this.entityData.set(VISUAL_SCALE, MIN_VISUAL_SCALE + this.random.nextFloat() * (MAX_VISUAL_SCALE - MIN_VISUAL_SCALE));
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
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
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.IN_FIRE))
			return false;
		return super.hurt(source, amount);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		SpawnGroupData spawnData = super.finalizeSpawn(world, difficulty, reason, livingdata, tag);
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
			this.entityData.set(VISUAL_SCALE, Math.max(MIN_VISUAL_SCALE, Math.min(MAX_VISUAL_SCALE, compound.getFloat(VISUAL_SCALE_TAG))));
		} else {
			// Existing demons saved with the former fixed appearance migrate once when loaded.
			this.randomizeAppearance();
		}
	}

	@Override
	public void baseTick() {
		super.baseTick();
		this.refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		return super.getDimensions(p_33597_).scale((float) 0.8);
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
			this.dropExperience();
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
