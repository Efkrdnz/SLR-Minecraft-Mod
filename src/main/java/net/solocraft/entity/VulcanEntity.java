
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

import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public class VulcanEntity extends Monster implements GeoEntity {
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(VulcanEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(VulcanEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(VulcanEntity.class, EntityDataSerializers.STRING);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";
	private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), ServerBossEvent.BossBarColor.RED, ServerBossEvent.BossBarOverlay.PROGRESS);
	private int guardTicks;
	private int guardCooldown = 35;
	private int dodgeCooldown;
	private int hammerSlamCooldown = 65;
	private int chargeCooldown = 75;
	private int moltenBurstCooldown = 95;
	private int attackState;
	private int stateTicks;
	private boolean stateHit;
	private Vec3 chargeDirection = Vec3.ZERO;
	private Vec3 markedPosition = Vec3.ZERO;

	public VulcanEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.VULCAN.get(), world);
	}

	public VulcanEntity(EntityType<VulcanEntity> type, Level world) {
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
		this.entityData.define(TEXTURE, "vulcan-texture");
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
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return 9;
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
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
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.IN_FIRE))
			return false;
		if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
			if (this.guardTicks > 0) {
				playGuardEffects();
				return super.hurt(source, amount * 0.25F);
			}
			float guardChance = source.getDirectEntity() != attacker ? 0.45F : 0.30F;
			if (this.guardCooldown <= 0 && amount >= 5.0F && this.random.nextFloat() < guardChance) {
				this.guardTicks = 18;
				this.guardCooldown = 75;
				playGuardEffects();
				return super.hurt(source, amount * 0.35F);
			}
			if (this.dodgeCooldown <= 0 && this.distanceTo(attacker) < 7.0F && this.random.nextFloat() < 0.18F) {
				this.dodgeCooldown = 45;
				sideStep(attacker, 0.85D);
				return super.hurt(source, amount * 0.65F);
			}
		}
		return super.hurt(source, amount);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", this.getTexture());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		this.refreshDimensions();
		tickVulcanBossAi();
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		return super.getDimensions(p_33597_).scale((float) 1);
	}

	@Override
	public boolean canChangeDimensions() {
		return false;
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossInfo.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossInfo.removePlayer(player);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		this.bossInfo.removeAllPlayers();
	}

	@Override
	public void customServerAiStep() {
		super.customServerAiStep();
		this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.27);
		builder = builder.add(Attributes.MAX_HEALTH, 340);
		builder = builder.add(Attributes.ARMOR, 18);
		builder = builder.add(Attributes.ARMOR_TOUGHNESS, 8);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 24);
		builder = builder.add(Attributes.FOLLOW_RANGE, 42);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1);
		return builder;
	}

	private void tickVulcanBossAi() {
		if (this.level().isClientSide || this.isNoAi())
			return;
		if (this.guardTicks > 0)
			this.guardTicks--;
		if (this.guardCooldown > 0)
			this.guardCooldown--;
		if (this.dodgeCooldown > 0)
			this.dodgeCooldown--;
		if (this.hammerSlamCooldown > 0)
			this.hammerSlamCooldown--;
		if (this.chargeCooldown > 0)
			this.chargeCooldown--;
		if (this.moltenBurstCooldown > 0)
			this.moltenBurstCooldown--;

		LivingEntity target = getOrFindTarget();
		if (target == null)
			return;
		this.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (this.attackState != 0) {
			tickAttackState(target);
			return;
		}

		double distance = this.distanceTo(target);
		if (this.hammerSlamCooldown <= 0 && distance < 8.5D) {
			beginHammerSlam();
		} else if (this.moltenBurstCooldown <= 0 && distance > 5.0D && distance < 22.0D && this.hasLineOfSight(target)) {
			beginMoltenBurst(target);
		} else if (this.chargeCooldown <= 0 && distance > 8.0D) {
			beginCharge(target);
		}
	}

	private LivingEntity getOrFindTarget() {
		LivingEntity target = this.getTarget();
		if (target != null && target.isAlive())
			return target;
		Player nearest = this.level().getNearestPlayer(this, 42.0D);
		if (nearest != null && nearest.isAlive()) {
			this.setTarget(nearest);
			return nearest;
		}
		return null;
	}

	private void beginHammerSlam() {
		this.attackState = 1;
		this.stateTicks = 0;
		this.stateHit = false;
		this.hammerSlamCooldown = 80 + this.random.nextInt(45);
		this.animationprocedure = "ground smash";
		this.getNavigation().stop();
		this.level().playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.6F, 0.55F);
	}

	private void beginCharge(LivingEntity target) {
		Vec3 direction = horizontalDirectionTo(target);
		if (direction.lengthSqr() < 0.001D)
			return;
		this.attackState = 2;
		this.stateTicks = 0;
		this.stateHit = false;
		this.chargeDirection = direction;
		this.chargeCooldown = 95 + this.random.nextInt(45);
		this.level().playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.4F, 0.65F);
	}

	private void beginMoltenBurst(LivingEntity target) {
		this.attackState = 3;
		this.stateTicks = 0;
		this.stateHit = false;
		this.markedPosition = target.position();
		this.moltenBurstCooldown = 125 + this.random.nextInt(55);
		this.animationprocedure = "ground smash";
		this.getNavigation().stop();
		this.level().playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.2F, 0.45F);
	}

	private void tickAttackState(LivingEntity target) {
		this.stateTicks++;
		if (this.attackState == 1) {
			tickHammerSlam();
		} else if (this.attackState == 2) {
			tickCharge();
		} else if (this.attackState == 3) {
			tickMoltenBurst(target);
		}
	}

	private void tickHammerSlam() {
		this.getNavigation().stop();
		if (this.level() instanceof ServerLevel serverLevel && this.stateTicks <= 12)
			serverLevel.sendParticles(ParticleTypes.CRIT, this.getX(), this.getY() + 0.1D, this.getZ(), 12, 2.5D, 0.08D, 2.5D, 0.08D);
		if (!this.stateHit && this.stateTicks >= 14) {
			this.stateHit = true;
			dealSlam(7.5D, 24.0F, 1.1D, 0.55D, ParticleTypes.LAVA);
			this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.8F, 0.6F);
		}
		if (this.stateTicks >= 30)
			resetAttackState();
	}

	private void tickCharge() {
		if (this.stateTicks >= 4 && this.stateTicks <= 18) {
			this.setDeltaMovement(this.chargeDirection.x * 0.95D, this.getDeltaMovement().y, this.chargeDirection.z * 0.95D);
			this.hasImpulse = true;
			if (this.level() instanceof ServerLevel serverLevel) {
				serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.35D, this.getZ(), 6, 0.7D, 0.2D, 0.7D, 0.025D);
				serverLevel.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.55D, this.getZ(), 3, 0.45D, 0.15D, 0.45D, 0.02D);
			}
			damageNearbyOnce("vulcan_charge_hit_", 2.1D, 16.0F, 1.0D, 0.25D);
		}
		if (this.stateTicks >= 26) {
			clearHitFlags("vulcan_charge_hit_");
			resetAttackState();
		}
	}

	private void tickMoltenBurst(LivingEntity target) {
		this.getNavigation().stop();
		if (target != null && target.isAlive() && this.stateTicks < 8)
			this.markedPosition = target.position();
		if (this.level() instanceof ServerLevel serverLevel && this.stateTicks <= 14) {
			double radius = 1.5D + this.stateTicks * 0.12D;
			for (int i = 0; i < 14; i++) {
				double angle = (i / 14.0D) * Math.PI * 2.0D;
				serverLevel.sendParticles(ParticleTypes.SMOKE, this.markedPosition.x + Math.cos(angle) * radius, this.markedPosition.y + 0.1D,
						this.markedPosition.z + Math.sin(angle) * radius, 1, 0.08D, 0.04D, 0.08D, 0.0D);
			}
		}
		if (!this.stateHit && this.stateTicks >= 16) {
			this.stateHit = true;
			eruptMarkedPosition();
		}
		if (this.stateTicks >= 28)
			resetAttackState();
	}

	private void eruptMarkedPosition() {
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.LAVA, this.markedPosition.x, this.markedPosition.y + 0.35D, this.markedPosition.z, 35, 1.5D, 0.6D, 1.5D, 0.25D);
			serverLevel.sendParticles(ParticleTypes.FLAME, this.markedPosition.x, this.markedPosition.y + 0.35D, this.markedPosition.z, 70, 2.0D, 0.35D, 2.0D, 0.06D);
			serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.markedPosition.x, this.markedPosition.y + 0.35D, this.markedPosition.z, 2, 0.4D, 0.15D, 0.4D, 0.0D);
		}
		this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 0.7F);
		for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(24.0D),
				entity -> entity != this && entity.isAlive() && entity.position().distanceTo(this.markedPosition) <= 4.0D)) {
			nearby.hurt(this.damageSources().mobAttack(this), 18.0F);
			nearby.setSecondsOnFire(5);
			Vec3 direction = nearby.position().subtract(this.markedPosition).multiply(1.0D, 0.0D, 1.0D);
			if (direction.lengthSqr() > 0.001D)
				pushEntity(nearby, direction.normalize(), 0.7D, 0.35D);
		}
	}

	private void damageNearbyOnce(String keyPrefix, double radius, float damage, double horizontalKnockback, double verticalKnockback) {
		for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
				entity -> entity != this && entity.isAlive() && entity.distanceTo(this) <= radius + entity.getBbWidth())) {
			String hitKey = keyPrefix + nearby.getUUID();
			if (this.getPersistentData().getBoolean(hitKey))
				continue;
			nearby.hurt(this.damageSources().mobAttack(this), damage);
			Vec3 direction = nearby.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
			if (direction.lengthSqr() < 0.001D)
				direction = this.chargeDirection;
			pushEntity(nearby, direction.normalize(), horizontalKnockback, verticalKnockback);
			this.getPersistentData().putBoolean(hitKey, true);
		}
	}

	private void dealSlam(double radius, float damage, double horizontalKnockback, double verticalKnockback, net.minecraft.core.particles.SimpleParticleType particle) {
		if (this.level() instanceof ServerLevel serverLevel) {
			for (int ring = 0; ring < 3; ring++) {
				double ringRadius = radius * (0.35D + ring * 0.25D);
				for (int i = 0; i < 22; i++) {
					double angle = (i / 22.0D) * Math.PI * 2.0D;
					serverLevel.sendParticles(particle, this.getX() + Math.cos(angle) * ringRadius, this.getY() + 0.15D,
							this.getZ() + Math.sin(angle) * ringRadius, 2, 0.12D, 0.08D, 0.12D, 0.02D);
				}
			}
			serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.4D, this.getZ(), 3, 1.0D, 0.25D, 1.0D, 0.0D);
		}
		for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
				entity -> entity != this && entity.isAlive() && entity.distanceTo(this) <= radius)) {
			nearby.hurt(this.damageSources().mobAttack(this), damage);
			nearby.setSecondsOnFire(3);
			Vec3 direction = nearby.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
			if (direction.lengthSqr() > 0.001D)
				pushEntity(nearby, direction.normalize(), horizontalKnockback, verticalKnockback);
		}
	}

	private Vec3 horizontalDirectionTo(LivingEntity target) {
		Vec3 direction = target.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D);
		return direction.lengthSqr() < 0.001D ? Vec3.ZERO : direction.normalize();
	}

	private void pushEntity(LivingEntity entity, Vec3 direction, double horizontal, double vertical) {
		entity.setDeltaMovement(direction.x * horizontal, vertical, direction.z * horizontal);
		entity.hurtMarked = true;
	}

	private void sideStep(LivingEntity attacker, double speed) {
		Vec3 away = this.position().subtract(attacker.position()).multiply(1.0D, 0.0D, 1.0D);
		if (away.lengthSqr() < 0.001D)
			away = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
		Vec3 side = new Vec3(-away.z, 0.0D, away.x).normalize();
		if (this.random.nextBoolean())
			side = side.scale(-1.0D);
		this.setDeltaMovement(side.x * speed, 0.12D, side.z * speed);
		this.hasImpulse = true;
		if (this.level() instanceof ServerLevel serverLevel)
			serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.6D, this.getZ(), 8, 0.5D, 0.25D, 0.5D, 0.03D);
	}

	private void playGuardEffects() {
		this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2F, 0.55F);
		if (this.level() instanceof ServerLevel serverLevel) {
			serverLevel.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(), 6, 0.55D, 0.45D, 0.55D, 0.05D);
			serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(), 10, 0.75D, 0.45D, 0.75D, 0.03D);
		}
	}

	private void resetAttackState() {
		this.attackState = 0;
		this.stateTicks = 0;
		this.stateHit = false;
	}

	private void clearHitFlags(String prefix) {
		java.util.Set<String> toRemove = new java.util.HashSet<>();
		for (String key : this.getPersistentData().getAllKeys()) {
			if (key.startsWith(prefix))
				toRemove.add(key);
		}
		toRemove.forEach(this.getPersistentData()::remove);
	}

	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			if ((event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F))

			) {
				return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
			}
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
			this.remove(VulcanEntity.RemovalReason.KILLED);
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
