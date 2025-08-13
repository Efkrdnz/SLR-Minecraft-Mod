
package net.solocraft.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.solocraft.procedures.KargalganOnEntityTickUpdateProcedure;
import net.solocraft.procedures.KargalganEntityIsHurtProcedure;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Pose;
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

public class KargalganEntity extends Monster implements GeoEntity {
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Boolean> DATA_Barrier = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> DATA_Phase = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_AI = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Boolean> DATA_Summon = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> DATA_GravityCounter = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_Push = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_CooldownCurseMagic = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_PushTimer = SynchedEntityData.defineId(KargalganEntity.class, EntityDataSerializers.INT);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";

	public KargalganEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.KARGALGAN.get(), world);
	}

	public KargalganEntity(EntityType<KargalganEntity> type, Level world) {
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
		this.entityData.define(TEXTURE, "kardalgan");
		this.entityData.define(DATA_Barrier, false);
		this.entityData.define(DATA_Phase, 1);
		this.entityData.define(DATA_AI, 0);
		this.entityData.define(DATA_Summon, false);
		this.entityData.define(DATA_GravityCounter, 0);
		this.entityData.define(DATA_Push, 0);
		this.entityData.define(DATA_CooldownCurseMagic, 0);
		this.entityData.define(DATA_PushTimer, 0);
	}

	public void setTexture(String texture) {
		this.entityData.set(TEXTURE, texture);
	}

	public String getTexture() {
		return this.entityData.get(TEXTURE);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Player.class, false, false));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, HunterEntity.class, false, false));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, ChoijongEntity.class, false, false));
		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, ChaHaeInEntity.class, false, false));
		this.targetSelector.addGoal(5, new NearestAttackableTargetGoal(this, BaekYoonhoEntity.class, false, false));
		this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 0, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return this.mob.getBbWidth() * this.mob.getBbWidth() + entity.getBbWidth();
			}
		});
		this.targetSelector.addGoal(7, new HurtByTargetGoal(this));
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
		KargalganEntityIsHurtProcedure.execute(this);
		if (source.is(DamageTypes.FALL))
			return false;
		return super.hurt(source, amount);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", this.getTexture());
		compound.putBoolean("DataBarrier", this.entityData.get(DATA_Barrier));
		compound.putInt("DataPhase", this.entityData.get(DATA_Phase));
		compound.putInt("DataAI", this.entityData.get(DATA_AI));
		compound.putBoolean("DataSummon", this.entityData.get(DATA_Summon));
		compound.putInt("DataGravityCounter", this.entityData.get(DATA_GravityCounter));
		compound.putInt("DataPush", this.entityData.get(DATA_Push));
		compound.putInt("DataCooldownCurseMagic", this.entityData.get(DATA_CooldownCurseMagic));
		compound.putInt("DataPushTimer", this.entityData.get(DATA_PushTimer));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
		if (compound.contains("DataBarrier"))
			this.entityData.set(DATA_Barrier, compound.getBoolean("DataBarrier"));
		if (compound.contains("DataPhase"))
			this.entityData.set(DATA_Phase, compound.getInt("DataPhase"));
		if (compound.contains("DataAI"))
			this.entityData.set(DATA_AI, compound.getInt("DataAI"));
		if (compound.contains("DataSummon"))
			this.entityData.set(DATA_Summon, compound.getBoolean("DataSummon"));
		if (compound.contains("DataGravityCounter"))
			this.entityData.set(DATA_GravityCounter, compound.getInt("DataGravityCounter"));
		if (compound.contains("DataPush"))
			this.entityData.set(DATA_Push, compound.getInt("DataPush"));
		if (compound.contains("DataCooldownCurseMagic"))
			this.entityData.set(DATA_CooldownCurseMagic, compound.getInt("DataCooldownCurseMagic"));
		if (compound.contains("DataPushTimer"))
			this.entityData.set(DATA_PushTimer, compound.getInt("DataPushTimer"));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		KargalganOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
		this.refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		return super.getDimensions(p_33597_).scale((float) 1);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0);
		builder = builder.add(Attributes.MAX_HEALTH, 200);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 3);
		builder = builder.add(Attributes.FOLLOW_RANGE, 64);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1);
		return builder;
	}

	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
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
			this.remove(KargalganEntity.RemovalReason.KILLED);
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
