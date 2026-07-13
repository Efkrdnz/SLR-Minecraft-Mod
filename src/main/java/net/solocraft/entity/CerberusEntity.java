
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;

public class CerberusEntity extends Monster implements GeoEntity {
	private static final String DEFAULTS_FIXED_TAG = "slr_cerberus_defaults_fixed";
	private static final double BASE_MAX_HEALTH = 260.0D;
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(CerberusEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(CerberusEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(CerberusEntity.class, EntityDataSerializers.STRING);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";
	private final ServerBossEvent bossInfo = new ServerBossEvent(this.getDisplayName(), ServerBossEvent.BossBarColor.RED, ServerBossEvent.BossBarOverlay.NOTCHED_10);
	private int fireBreathCooldown = 70;
	private int fireBreathTicks;
	private int dashCooldown = 45;
	private int dashTicks;
	private int slamCooldown = 95;
	private int slamTicks;
	private int dodgeCooldown;
	private int blockTicks;
	private boolean slamLanded;
	private Vec3 dashDirection = Vec3.ZERO;

	public CerberusEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.CERBERUS.get(), world);
	}

	public CerberusEntity(EntityType<CerberusEntity> type, Level world) {
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
		this.entityData.define(TEXTURE, "cerberus");
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
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
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
		if (!this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker && attacker != this) {
			if (this.blockTicks > 0) {
				playDefenseEffects();
				return super.hurt(source, amount * 0.35F);
			}
			if (this.dodgeCooldown <= 0 && this.distanceTo(attacker) < 8.0F && this.random.nextFloat() < 0.22F) {
				this.dodgeCooldown = 42;
				sideStep(attacker, 1.15D);
				playDefenseEffects();
				return super.hurt(source, amount * 0.45F);
			}
			if (amount >= 7.0F && this.random.nextFloat() < 0.28F) {
				this.blockTicks = 12;
				playDefenseEffects();
				return super.hurt(source, amount * 0.5F);
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
		repairLegacyStructureData();
		this.refreshDimensions();
		tickCerberusBossAi();
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		return super.getDimensions(p_33597_).scale((float) 3);
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
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.34);
		builder = builder.add(Attributes.MAX_HEALTH, 260);
		builder = builder.add(Attributes.ARMOR, 28);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 26);
		builder = builder.add(Attributes.FOLLOW_RANGE, 48);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1);
		return builder;
	}

	private void repairLegacyStructureData() {
		if (this.level().isClientSide() || this.getPersistentData().getBoolean(DEFAULTS_FIXED_TAG))
			return;
		this.getPersistentData().putBoolean(DEFAULTS_FIXED_TAG, true);
		AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth != null && maxHealth.getBaseValue() < BASE_MAX_HEALTH)
			maxHealth.setBaseValue(BASE_MAX_HEALTH);
		if (this.getHealth() < this.getMaxHealth())
			this.setHealth(this.getMaxHealth());
	}

	private void tickCerberusBossAi() {
		if (this.level().isClientSide || this.isNoAi())
			return;
		if (this.fireBreathCooldown > 0)
			this.fireBreathCooldown--;
		if (this.dashCooldown > 0)
			this.dashCooldown--;
		if (this.slamCooldown > 0)
			this.slamCooldown--;
		if (this.dodgeCooldown > 0)
			this.dodgeCooldown--;
		if (this.blockTicks > 0)
			this.blockTicks--;

		LivingEntity target = getOrFindTarget();
		if (target == null)
			return;
		this.getLookControl().setLookAt(target, 35.0F, 35.0F);

		if (this.fireBreathTicks > 0) {
			tickFireBreath(target);
			return;
		}
		if (this.dashTicks > 0) {
			tickDash();
			return;
		}
		if (this.slamTicks > 0) {
			tickJumpSlam();
			return;
		}

		double distance = net.solocraft.util.CombatRangeHelper.surfaceDistance(this, target);
		if (this.fireBreathCooldown <= 0 && distance > 4.0D && distance < 18.0D && this.hasLineOfSight(target)) {
			beginFireBreath();
		} else if (this.slamCooldown <= 0 && distance > 5.0D && distance < 16.0D) {
			beginJumpSlam(target);
		} else if (this.dashCooldown <= 0 && distance > 7.0D) {
			beginDash(target);
		}
	}

	private LivingEntity getOrFindTarget() {
		LivingEntity target = this.getTarget();
		if (target != null && target.isAlive())
			return target;
		Player nearest = this.level().getNearestPlayer(this, 48.0D);
		if (nearest != null && nearest.isAlive()) {
			this.setTarget(nearest);
			return nearest;
		}
		return null;
	}

	private void beginFireBreath() {
		this.fireBreathTicks = 42;
		this.fireBreathCooldown = 115 + this.random.nextInt(45);
		this.animationprocedure = "roar";
		this.getNavigation().stop();
		this.level().playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.8F, 0.55F);
	}

	private void tickFireBreath(LivingEntity target) {
		this.fireBreathTicks--;
		this.getNavigation().stop();
		this.getLookControl().setLookAt(target, 45.0F, 45.0F);
		Vec3 direction = horizontalDirectionTo(target);
		if (direction.lengthSqr() < 0.001D)
			direction = this.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 origin = this.position().add(direction.scale(2.2D)).add(0.0D, this.getBbHeight() * 0.62D, 0.0D);
		if (this.level() instanceof ServerLevel serverLevel) {
			for (int i = 1; i <= 12; i++) {
				Vec3 center = origin.add(direction.scale(i * 0.85D));
				double spread = 0.12D + i * 0.035D;
				serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 3, spread, spread * 0.5D, spread, 0.035D);
				if (i % 3 == 0)
					serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 1, spread, spread * 0.4D, spread, 0.01D);
			}
		}
		if (this.fireBreathTicks % 4 == 0)
			damageFireCone(origin, direction);
	}

	private void beginDash(LivingEntity target) {
		Vec3 direction = horizontalDirectionTo(target);
		if (direction.lengthSqr() < 0.001D)
			return;
		this.dashDirection = direction;
		this.dashTicks = 16;
		this.dashCooldown = 70 + this.random.nextInt(35);
		this.animationprocedure = "bite";
		this.level().playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ATTACK, SoundSource.HOSTILE, 1.4F, 0.65F);
	}

	private void tickDash() {
		this.dashTicks--;
		if (this.dashTicks > 4) {
			this.setDeltaMovement(this.dashDirection.x * 1.15D, this.getDeltaMovement().y, this.dashDirection.z * 1.15D);
			this.hasImpulse = true;
			if (this.level() instanceof ServerLevel serverLevel)
				serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 0.4D, this.getZ(), 5, 0.6D, 0.25D, 0.6D, 0.02D);
			damageNearbyOnce("cerberus_dash_hit_", 2.4D, 18.0F, 0.8D, 0.2D);
		}
		if (this.dashTicks <= 0)
			clearHitFlags("cerberus_dash_hit_");
	}

	private void beginJumpSlam(LivingEntity target) {
		Vec3 direction = horizontalDirectionTo(target);
		this.slamTicks = 28;
		this.slamCooldown = 120 + this.random.nextInt(55);
		this.slamLanded = false;
		this.animationprocedure = "roar";
		this.setDeltaMovement(direction.x * 0.65D, 0.82D, direction.z * 0.65D);
		this.hasImpulse = true;
		this.level().playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.6F, 0.7F);
	}

	private void tickJumpSlam() {
		int age = 28 - this.slamTicks;
		this.slamTicks--;
		if (age == 10)
			this.setDeltaMovement(this.getDeltaMovement().x * 0.6D, -0.95D, this.getDeltaMovement().z * 0.6D);
		if (!this.slamLanded && (this.onGround() || age >= 17)) {
			this.slamLanded = true;
			dealSlam(6.5D, 28.0F, 1.25D, 0.65D, ParticleTypes.FLAME);
			this.level().playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 1.4F, 0.75F);
		}
	}

	private void damageFireCone(Vec3 origin, Vec3 direction) {
		for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(16.0D),
				entity -> entity != this && entity.isAlive())) {
			Vec3 toEntity = nearby.position().add(0.0D, nearby.getBbHeight() * 0.5D, 0.0D).subtract(origin);
			double distance = toEntity.length();
			if (distance > 15.0D || distance < 0.2D)
				continue;
			Vec3 normalized = toEntity.multiply(1.0D, 0.0D, 1.0D).normalize();
			if (normalized.dot(direction) < 0.62D)
				continue;
			nearby.hurt(this.damageSources().mobAttack(this), 6.0F);
			nearby.setSecondsOnFire(4);
			pushEntity(nearby, direction, 0.22D, 0.04D);
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
				direction = this.dashDirection;
			pushEntity(nearby, direction.normalize(), horizontalKnockback, verticalKnockback);
			this.getPersistentData().putBoolean(hitKey, true);
		}
	}

	private void dealSlam(double radius, float damage, double horizontalKnockback, double verticalKnockback, net.minecraft.core.particles.SimpleParticleType particle) {
		if (this.level() instanceof ServerLevel serverLevel) {
			for (int ring = 0; ring < 3; ring++) {
				double ringRadius = radius * (0.35D + ring * 0.25D);
				for (int i = 0; i < 20; i++) {
					double angle = (i / 20.0D) * Math.PI * 2.0D;
					serverLevel.sendParticles(particle, this.getX() + Math.cos(angle) * ringRadius, this.getY() + 0.15D,
							this.getZ() + Math.sin(angle) * ringRadius, 2, 0.12D, 0.08D, 0.12D, 0.01D);
				}
			}
			serverLevel.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.4D, this.getZ(), 2, 0.8D, 0.25D, 0.8D, 0.0D);
		}
		for (LivingEntity nearby : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(radius),
				entity -> entity != this && entity.isAlive() && entity.distanceTo(this) <= radius)) {
			nearby.hurt(this.damageSources().mobAttack(this), damage);
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
		this.setDeltaMovement(side.x * speed, 0.18D, side.z * speed);
		this.hasImpulse = true;
	}

	private void playDefenseEffects() {
		this.level().playSound(null, this.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.0F, 0.65F);
		if (this.level() instanceof ServerLevel serverLevel)
			serverLevel.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + this.getBbHeight() * 0.55D, this.getZ(), 10, 0.7D, 0.45D, 0.7D, 0.04D);
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
				return event.setAndContinue(RawAnimation.begin().thenLoop("running"));
			}
			return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
		}
		return PlayState.STOP;
	}

	private PlayState attackingPredicate(AnimationState event) {
		double d1 = this.getX() - this.xOld;
		double d0 = this.getZ() - this.zOld;
		float velocity = (float) Math.sqrt(d1 * d1 + d0 * d0);
		if (getAttackAnim(event.getPartialTick()) > 0f && !this.swinging) {
			this.swinging = true;
			this.lastSwing = level().getGameTime();
		}
		if (this.swinging && this.lastSwing + 7L <= level().getGameTime()) {
			this.swinging = false;
		}
		if (this.swinging && event.getController().getAnimationState() == AnimationController.State.STOPPED) {
			event.getController().forceAnimationReset();
			return event.setAndContinue(RawAnimation.begin().thenPlay("bite"));
		}
		return PlayState.CONTINUE;
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
			this.remove(CerberusEntity.RemovalReason.KILLED);
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
		data.add(new AnimationController<>(this, "attacking", 4, this::attackingPredicate));
		data.add(new AnimationController<>(this, "procedure", 4, this::procedurePredicate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
