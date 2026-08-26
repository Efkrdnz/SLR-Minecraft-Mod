package net.solocraft.entity;

import net.solocraft.entity.ai.ShadowCommandTargetGoal;
import net.solocraft.entity.ai.ShadowFollowOwnerGoal;
import net.solocraft.entity.ai.ShadowIronCombatGoal;
import net.solocraft.entity.ai.ShadowIronCombatPolicy;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.util.ShadowIronCombatManager;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import net.solocraft.network.compat.NetworkHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.UUID;

/** Shadow Iron: a grounded, command-aware guardian and crowd tank. */
public class ShadowIronEntity extends TamableAnimal implements GeoEntity {
	private static final EntityDataAccessor<String> ACTION =
			SynchedEntityData.defineId(ShadowIronEntity.class,
					EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> DOMAIN_BOOSTED =
			SynchedEntityData.defineId(ShadowIronEntity.class,
					EntityDataSerializers.BOOLEAN);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private int actionTick;
	private UUID actionTargetId;
	private UUID blockedAttackerId;
	private boolean counterAttack;
	private boolean blockConnected;
	private boolean rescueBlock;
	private long nextAttackAt;
	private long nextBlockAt;
	private long nextRoarAt;
	private long nextInterceptAt;
	private long fortifyUntil;
	private int fortifyTargets;

	public ShadowIronEntity(EntityType<? extends ShadowIronEntity> type,
			Level level) {
		super(type, level);
		setPersistenceRequired();
		setPathfindingMalus(PathType.WATER, 0.0F);
		setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.NONE.serializedName);
		builder.define(DOMAIN_BOOSTED, false);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		GroundPathNavigation navigation = new GroundPathNavigation(this, level);
		navigation.setCanOpenDoors(true);
		navigation.setCanPassDoors(true);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.targetSelector.addGoal(0, new ShadowCommandTargetGoal(this));
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new ShadowIronCombatGoal(this));
		this.goalSelector.addGoal(2, new ShadowFollowOwnerGoal(this));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.28D)
				.add(Attributes.MAX_HEALTH, 140.0D)
				.add(Attributes.ARMOR, 18.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 4.0D)
				.add(Attributes.ATTACK_DAMAGE, 8.0D)
				.add(Attributes.FOLLOW_RANGE, 40.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9D)
				.add(Attributes.ATTACK_KNOCKBACK, 0.7D);
	}

	public static void init() {
	}

	public Action getCombatAction() {
		return Action.fromSerializedName(this.entityData.get(ACTION));
	}

	public boolean isActionIdle() {
		return getCombatAction() == Action.NONE;
	}

	public boolean isActing() {
		return !isActionIdle();
	}

	public boolean isDomainBoosted() {
		return this.entityData.get(DOMAIN_BOOSTED);
	}

	public void setDomainBoosted(boolean boosted) {
		this.entityData.set(DOMAIN_BOOSTED, boosted);
	}

	public int getActionTick() {
		return actionTick;
	}

	public boolean beginAttack(LivingEntity target, boolean counter) {
		if (level().isClientSide() || !isActionIdle() || target == null
				|| !target.isAlive() || !canAttackNow())
			return false;
		this.counterAttack = counter;
		this.actionTargetId = target.getUUID();
		this.nextAttackAt = level().getGameTime()
				+ ShadowIronCombatPolicy.ATTACK_COOLDOWN_TICKS;
		beginAction(Action.ATTACK);
		return true;
	}

	public boolean beginBlock(boolean rescue) {
		if (level().isClientSide() || !canBlockNow()
				|| !isActionIdle() && (!rescue
						|| getCombatAction() == Action.BLOCK))
			return false;
		if (rescue && !isActionIdle())
			clearAction();
		this.rescueBlock = rescue;
		this.blockConnected = false;
		this.blockedAttackerId = null;
		this.nextBlockAt = level().getGameTime()
				+ ShadowIronCombatPolicy.BLOCK_COOLDOWN_TICKS;
		beginAction(Action.BLOCK);
		return true;
	}

	public boolean beginRoar() {
		if (level().isClientSide() || !isActionIdle() || !canRoarNow())
			return false;
		this.nextRoarAt = level().getGameTime()
				+ ShadowIronCombatPolicy.ROAR_COOLDOWN_TICKS;
		beginAction(Action.ROAR);
		return true;
	}

	private void beginAction(Action action) {
		this.actionTick = 0;
		this.entityData.set(ACTION, action.serializedName);
		this.getNavigation().stop();
		this.triggerAnim("action", action.serializedName);
	}

	private void clearAction() {
		this.entityData.set(ACTION, Action.NONE.serializedName);
		this.actionTick = 0;
		this.actionTargetId = null;
		this.counterAttack = false;
		this.rescueBlock = false;
	}

	private void tickCombatAction() {
		if (level().isClientSide() || isActionIdle())
			return;
		actionTick++;
		Action action = getCombatAction();
		if (action == Action.ATTACK
				&& actionTick == ShadowIronCombatPolicy.ATTACK_IMPACT_TICK) {
			LivingEntity target = findLiving(actionTargetId);
			ShadowIronCombatManager.performCleave(this, target, counterAttack);
		}
		if (action == Action.ROAR
				&& actionTick == ShadowIronCombatPolicy.ROAR_PULSE_TICK)
			ShadowIronCombatManager.performRoar(this);

		if (action == Action.ATTACK
				&& actionTick >= ShadowIronCombatPolicy.ATTACK_END_TICK) {
			clearAction();
			return;
		}
		if (action == Action.ROAR
				&& actionTick >= ShadowIronCombatPolicy.ROAR_END_TICK) {
			clearAction();
			return;
		}
		if (action == Action.BLOCK
				&& actionTick >= ShadowIronCombatPolicy.BLOCK_END_TICK) {
			UUID counterTarget = blockConnected ? blockedAttackerId : null;
			clearAction();
			blockConnected = false;
			blockedAttackerId = null;
			LivingEntity attacker = findLiving(counterTarget);
			if (attacker != null && canAttackNow())
				beginAttack(attacker, true);
		}
	}

	@Nullable
	private LivingEntity findLiving(@Nullable UUID id) {
		if (id == null || !(level() instanceof ServerLevel serverLevel))
			return null;
		return serverLevel.getEntity(id) instanceof LivingEntity living
				&& living.isAlive() ? living : null;
	}

	public boolean canAttackNow() {
		return level().getGameTime() >= nextAttackAt;
	}

	public boolean canBlockNow() {
		return level().getGameTime() >= nextBlockAt;
	}

	public boolean canRoarNow() {
		return level().getGameTime() >= nextRoarAt;
	}

	public boolean canInterceptNow() {
		return level().getGameTime() >= nextInterceptAt;
	}

	public void setNextInterceptAt(long gameTick) {
		this.nextInterceptAt = Math.max(this.nextInterceptAt, gameTick);
	}

	public void fortifyFromTaunt(int targets) {
		this.fortifyTargets = Math.max(0,
				Math.min(ShadowIronCombatPolicy.TAUNT_TARGET_CAP, targets));
		this.fortifyUntil = level().getGameTime()
				+ ShadowIronCombatPolicy.NORMAL_TAUNT_TICKS;
	}

	public float fortificationReduction() {
		if (level().getGameTime() >= fortifyUntil)
			return 0.0F;
		return ShadowIronCombatPolicy.fortificationReduction(fortifyTargets);
	}

	public boolean isShieldActive() {
		if (getCombatAction() != Action.BLOCK)
			return false;
		return rescueBlock || actionTick >= ShadowIronCombatPolicy.BLOCK_ACTIVE_START_TICK
				&& actionTick <= ShadowIronCombatPolicy.BLOCK_ACTIVE_END_TICK;
	}

	public boolean canBlockSource(DamageSource source) {
		if (!isShieldActive() || source == null
				|| source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return false;
		Vec3 sourcePosition = source.getSourcePosition();
		if (sourcePosition == null)
			return false;
		Vec3 towardSource = sourcePosition.subtract(position())
				.multiply(1.0D, 0.0D, 1.0D);
		if (towardSource.lengthSqr() < 1.0E-5D)
			return true;
		Vec3 forward = Vec3.directionFromRotation(0.0F, getYRot())
				.multiply(1.0D, 0.0D, 1.0D).normalize();
		return forward.dot(towardSource.normalize()) >= 0.35D;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN))
			return false;
		float adjusted = Math.max(0.0F, amount);
		boolean blocked = canBlockSource(source);
		if (blocked) {
			boolean projectile = source.getDirectEntity()
					instanceof net.minecraft.world.entity.projectile.Projectile;
			boolean boss = ShadowIronCombatManager.isBossDamageSource(source);
			adjusted *= 1.0F - ShadowIronCombatPolicy.blockReduction(projectile, boss);
		}
		adjusted *= 1.0F - fortificationReduction();
		boolean hurt = super.hurt(source, adjusted);
		if (hurt && blocked && !level().isClientSide()) {
			blockConnected = true;
			if (source.getEntity() instanceof LivingEntity attacker)
				blockedAttackerId = attacker.getUUID();
			ShadowIronCombatManager.onShieldBlock(this, source);
		}
		return hurt;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		updateSwingTime();
		if (!level().isClientSide()) {
			boolean domainBoosted = hasEffect(
					SololevelingModMobEffects.DOMAIN_BOOST);
			if (isDomainBoosted() != domainBoosted)
				setDomainBoosted(domainBoosted);
			tickCombatAction();
			ShadowIronCombatManager.tickIron(this);
		}
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).withEyeHeight(3.05F);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_DAMAGE;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.IRON_GOLEM_STEP, 0.38F,
				0.72F + random.nextFloat() * 0.12F);
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Nullable
	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
		return SololevelingModEntities.SHADOW_IRON.get().create(level);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putLong("IronNextAttack", nextAttackAt);
		tag.putLong("IronNextBlock", nextBlockAt);
		tag.putLong("IronNextRoar", nextRoarAt);
		tag.putLong("IronNextIntercept", nextInterceptAt);
		tag.putLong("IronFortifyUntil", fortifyUntil);
		tag.putInt("IronFortifyTargets", fortifyTargets);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		nextAttackAt = tag.getLong("IronNextAttack");
		nextBlockAt = tag.getLong("IronNextBlock");
		nextRoarAt = tag.getLong("IronNextRoar");
		nextInterceptAt = tag.getLong("IronNextIntercept");
		fortifyUntil = tag.getLong("IronFortifyUntil");
		fortifyTargets = tag.getInt("IronFortifyTargets");
		clearAction();
	}

	private PlayState movementPredicate(AnimationState<ShadowIronEntity> state) {
		if (!isActionIdle())
			return PlayState.STOP;
		if (state.isMoving()
				|| Math.abs(state.getLimbSwingAmount()) > 0.15F)
			return state.setAndContinue(RawAnimation.begin().thenLoop("walk"));
		return state.setAndContinue(RawAnimation.begin().thenLoop("idle"));
	}

	private PlayState actionPredicate(AnimationState<ShadowIronEntity> state) {
		return PlayState.STOP;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "movement", 4,
				this::movementPredicate));
		AnimationController<ShadowIronEntity> actionController =
				new AnimationController<>(this, "action", 0,
						this::actionPredicate);
		actionController.triggerableAnim(Action.ATTACK.serializedName,
				RawAnimation.begin().thenPlay("attack"));
		actionController.triggerableAnim(Action.BLOCK.serializedName,
				RawAnimation.begin().thenPlay("block"));
		actionController.triggerableAnim(Action.ROAR.serializedName,
				RawAnimation.begin().thenPlay("roar"));
		controllers.add(actionController);
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return cache;
	}

	public enum Action {
		NONE("none"),
		ATTACK("attack"),
		BLOCK("block"),
		ROAR("roar");

		private final String serializedName;

		Action(String serializedName) {
			this.serializedName = serializedName;
		}

		private static Action fromSerializedName(String value) {
			if (value == null)
				return NONE;
			String normalized = value.trim().toLowerCase(Locale.ROOT);
			for (Action action : values()) {
				if (action.serializedName.equals(normalized))
					return action;
			}
			return NONE;
		}
	}
}
