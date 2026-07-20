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
import net.solocraft.util.WhiteFlameMonarchManager;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

import javax.annotation.Nullable;

public class DemonKnightEntity extends Monster implements GeoEntity {
	private static final int TEXTURE_VARIANT_COUNT = 3;
	private static final float MIN_VISUAL_SCALE = 0.94F;
	private static final float MAX_VISUAL_SCALE = 1.08F;
	private static final String VISUAL_SCALE_TAG = "DemonKnightVisualScale";

	// synced variant (0, 1, 2) — determines which texture is shown
	public static final EntityDataAccessor<Integer> VARIANT =
			SynchedEntityData.defineId(DemonKnightEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Float> VISUAL_SCALE =
			SynchedEntityData.defineId(DemonKnightEntity.class, EntityDataSerializers.FLOAT);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private long lastSwing;
	public String animationprocedure = "empty";

	public DemonKnightEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.DEMON_KNIGHT.get(), world);
	}

	public DemonKnightEntity(EntityType<DemonKnightEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(VARIANT, 0);
		this.entityData.define(VISUAL_SCALE, 1.0F);
	}

	public int getVariant() {
		return this.entityData.get(VARIANT);
	}

	public void setVariant(int variant) {
		this.entityData.set(VARIANT, Math.max(0, Math.min(TEXTURE_VARIANT_COUNT - 1, variant)));
	}

	public float getVisualScale() {
		return this.entityData.get(VISUAL_SCALE);
	}

	private void setVisualScale(float scale) {
		this.entityData.set(VISUAL_SCALE, Math.max(MIN_VISUAL_SCALE, Math.min(MAX_VISUAL_SCALE, scale)));
	}

	public void randomizeAppearance() {
		this.setVariant(this.random.nextInt(TEXTURE_VARIANT_COUNT));
		this.setVisualScale(MIN_VISUAL_SCALE + this.random.nextFloat() * (MAX_VISUAL_SCALE - MIN_VISUAL_SCALE));
	}

	/** Kept for existing DKC spawn code; now randomizes the complete appearance. */
	public void randomizeVariant() {
		this.randomizeAppearance();
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return 7.0; // slightly longer reach than demon (6.25)
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 0.6));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
				candidate -> !this.getPersistentData().hasUUID(WhiteFlameMonarchManager.SUMMON_OWNER)
						&& candidate instanceof Player player && !player.isCreative() && !player.isSpectator()));
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
	public boolean isAlliedTo(Entity other) {
		if (super.isAlliedTo(other))
			return true;
		if (!this.getPersistentData().hasUUID(WhiteFlameMonarchManager.SUMMON_OWNER))
			return false;
		UUID owner = this.getPersistentData().getUUID(WhiteFlameMonarchManager.SUMMON_OWNER);
		return owner.equals(other.getUUID())
				|| other.getPersistentData().hasUUID(WhiteFlameMonarchManager.SUMMON_OWNER)
				&& owner.equals(other.getPersistentData().getUUID(WhiteFlameMonarchManager.SUMMON_OWNER));
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return source.getEntity() == null || !this.isAlliedTo(source.getEntity())
				? super.hurt(source, amount)
				: false;
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		return !this.isAlliedTo(target) && super.doHurtTarget(target);
	}

	@Override
	protected void dropAllDeathLoot(DamageSource source) {
		if (!this.getPersistentData().getBoolean("mowf_no_loot"))
			super.dropAllDeathLoot(source);
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
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty,
			MobSpawnType reason, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		SpawnGroupData spawnData = super.finalizeSpawn(world, difficulty, reason, livingdata, tag);
		this.randomizeAppearance();
		return spawnData;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("Variant", this.getVariant());
		compound.putFloat(VISUAL_SCALE_TAG, this.getVisualScale());
		// preserve DKC tracking data
		compound.putDouble("dkc_floor_number", this.getPersistentData().getDouble("dkc_floor_number"));
		compound.putString("dkc_spawned_by", this.getPersistentData().getString("dkc_spawned_by"));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Variant"))
			this.setVariant(compound.getInt("Variant"));
		else
			this.setVariant(this.random.nextInt(TEXTURE_VARIANT_COUNT));
		if (compound.contains(VISUAL_SCALE_TAG))
			this.setVisualScale(compound.getFloat(VISUAL_SCALE_TAG));
		else
			this.setVisualScale(MIN_VISUAL_SCALE + this.random.nextFloat() * (MAX_VISUAL_SCALE - MIN_VISUAL_SCALE));
		if (compound.contains("dkc_floor_number"))
			this.getPersistentData().putDouble("dkc_floor_number", compound.getDouble("dkc_floor_number"));
		if (compound.contains("dkc_spawned_by"))
			this.getPersistentData().putString("dkc_spawned_by", compound.getString("dkc_spawned_by"));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		this.refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose p) {
		return super.getDimensions(p).scale(1.0f);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MAX_HEALTH, 100);         // tankier than demon (60)
		builder = builder.add(Attributes.ARMOR, 12);               // heavy armor
		builder = builder.add(Attributes.ARMOR_TOUGHNESS, 4);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 20);       // hits harder than demon (12)
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.22);    // slower than demon (0.3)
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.8); // very resistant to knockback
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 0.5);
		return builder;
	}

	// ── GeckoLib animations ─────────────────────────────────────────────────

	private PlayState movementPredicate(AnimationState<DemonKnightEntity> event) {
		if (this.animationprocedure.equals("empty")) {
			if (event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F)) {
				return event.setAndContinue(RawAnimation.begin().thenLoop("run"));
			}
			return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
		}
		return PlayState.STOP;
	}

	private PlayState attackingPredicate(AnimationState<DemonKnightEntity> event) {
		if (this.getAttackAnim(event.getPartialTick()) > 0.0F && !this.swinging) {
			this.swinging = true;
			this.lastSwing = this.level().getGameTime();
		}
		if (this.swinging && this.lastSwing + 7L <= this.level().getGameTime())
			this.swinging = false;
		if (this.swinging && event.getController().getAnimationState() == AnimationController.State.STOPPED) {
			event.getController().forceAnimationReset();
			return event.setAndContinue(RawAnimation.begin().thenPlay("attack"));
		}
		return PlayState.CONTINUE;
	}

	private PlayState procedurePredicate(AnimationState<DemonKnightEntity> event) {
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
			this.remove(DemonKnightEntity.RemovalReason.KILLED);
			this.dropExperience();
		}
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		data.add(new AnimationController<>(this, "movement", 4, this::movementPredicate));
		data.add(new AnimationController<>(this, "attacking", 2, this::attackingPredicate));
		data.add(new AnimationController<>(this, "procedure", 4, this::procedurePredicate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
