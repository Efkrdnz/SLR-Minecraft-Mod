
package net.solocraft.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.solocraft.procedures.IceElfOnEntityTickUpdateProcedure;
import net.solocraft.procedures.IceElfEntityIsHurtProcedure;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.ai.ShadowThreatTargetGoal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.solocraft.network.compat.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.solocraft.entity.ai.LegacyMeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.nbt.CompoundTag;

public class IceElfEntity extends Monster implements GeoEntity {
	private static final String COMBAT_BALANCE_VERSION_TAG =
			"SLRIceElfCombatBalanceVersion";
	private static final int COMBAT_BALANCE_VERSION = 1;
	private static final double LEGACY_ATTACK_DAMAGE_REDUCTION = 1.0D;
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(IceElfEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(IceElfEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(IceElfEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> DATA_AI = SynchedEntityData.defineId(IceElfEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_MF = SynchedEntityData.defineId(IceElfEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Boolean> DATA_canshoot = SynchedEntityData.defineId(IceElfEntity.class, EntityDataSerializers.BOOLEAN);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	private ShadowThreatTargetGoal shadowThreatGoal;
	public String animationprocedure = "empty";

	public IceElfEntity(EntityType<IceElfEntity> type, Level world) {
		super(type, world);
		xpReward = 20;
		setNoAi(false);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SHOOT, false);
		builder.define(ANIMATION, "undefined");
		builder.define(TEXTURE, "iceelf");
		builder.define(DATA_AI, 0);
		builder.define(DATA_MF, 0);
		builder.define(DATA_canshoot, false);
	}

	public void setTexture(String texture) {
		this.entityData.set(TEXTURE, texture);
	}

	public String getTexture() {
		return this.entityData.get(TEXTURE);
	}
@Override
	protected void registerGoals() {
		super.registerGoals();
		shadowThreatGoal = new ShadowThreatTargetGoal(this);
		this.targetSelector.addGoal(0, shadowThreatGoal);
		this.goalSelector.addGoal(1, new LegacyMeleeAttackGoal(this, 1.2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth();
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, Player.class, false, false));
		this.targetSelector.addGoal(4, new HurtByTargetGoal(this).setAlertOthers());
		this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(7, new FloatGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
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
		IceElfEntityIsHurtProcedure.execute(this);
		float healthBefore = getHealth();
		boolean hurt = super.hurt(source, amount);
		if (hurt && shadowThreatGoal != null)
			shadowThreatGoal.recordSuccessfulHit(source,
					Math.max(0.0F, healthBefore - getHealth()));
		return hurt;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", this.getTexture());
		compound.putInt("DataAI", this.entityData.get(DATA_AI));
		compound.putInt("DataMF", this.entityData.get(DATA_MF));
		compound.putBoolean("Datacanshoot", this.entityData.get(DATA_canshoot));
		compound.putInt(COMBAT_BALANCE_VERSION_TAG, COMBAT_BALANCE_VERSION);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
		if (compound.contains("DataAI"))
			this.entityData.set(DATA_AI, compound.getInt("DataAI"));
		if (compound.contains("DataMF"))
			this.entityData.set(DATA_MF, compound.getInt("DataMF"));
		if (compound.contains("Datacanshoot"))
			this.entityData.set(DATA_canshoot, compound.getBoolean("Datacanshoot"));
		if (compound.getInt(COMBAT_BALANCE_VERSION_TAG)
				< COMBAT_BALANCE_VERSION
				&& getAttribute(Attributes.ATTACK_DAMAGE) != null)
			getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(Math.max(1.0D,
					getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()
							- LEGACY_ATTACK_DAMAGE_REDUCTION));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		IceElfOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
		this.refreshDimensions();
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose p_33597_) {
		return super.getDefaultDimensions(p_33597_).scale((float) 1).withEyeHeight(1.9F);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 72);
		builder = builder.add(Attributes.ARMOR, 15);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 2);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0.1);
		return builder;
	}

	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			if ((event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F))

			) {
				return event.setAndContinue(RawAnimation.begin().thenLoop("move.walk"));
			}
			return event.setAndContinue(RawAnimation.begin().thenLoop("misc.idle"));
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
			this.remove(IceElfEntity.RemovalReason.KILLED);
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
