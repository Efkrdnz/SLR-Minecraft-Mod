
package net.solocraft.entity;

import net.solocraft.procedures.BaranOnTickProcedure;
import net.solocraft.init.SololevelingModEntities;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
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
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;

public class BaranEntity extends Monster implements GeoEntity {
	// Synced fields for the state machine
	public static final EntityDataAccessor<String> DATA_state =
			SynchedEntityData.defineId(BaranEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> DATA_IA =
			SynchedEntityData.defineId(BaranEntity.class, EntityDataSerializers.INT);
	// Kept for BaranModel (texture switching) and EntityAnimationFactory (animation sync)
	public static final EntityDataAccessor<String> ANIMATION =
			SynchedEntityData.defineId(BaranEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE =
			SynchedEntityData.defineId(BaranEntity.class, EntityDataSerializers.STRING);

	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	public String animationprocedure = "empty";

	// Purple boss bar with 20-notch health overlay (fits a "demon king" feel)
	private final ServerBossEvent bossInfo = new ServerBossEvent(
			net.minecraft.network.chat.Component.literal("Demon King Baran"),
			ServerBossEvent.BossBarColor.PURPLE,
			ServerBossEvent.BossBarOverlay.NOTCHED_20);

	// ── Constructors ──────────────────────────────────────────────────────────

	public BaranEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.BARAN.get(), world);
	}

	public BaranEntity(EntityType<BaranEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setPersistenceRequired();
	}

	// ── Synced data ───────────────────────────────────────────────────────────

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_state, "idle");
		this.entityData.define(DATA_IA, 0);
		this.entityData.define(ANIMATION, "undefined");
		this.entityData.define(TEXTURE, "demonkingbaran");
	}

	public String getState() { return this.entityData.get(DATA_state); }
	public void setState(String s) { this.entityData.set(DATA_state, s); }
	public int getIA() { return this.entityData.get(DATA_IA); }
	public void setIA(int v) { this.entityData.set(DATA_IA, v); }

	public String getSyncedAnimation() { return this.entityData.get(ANIMATION); }
	public void setAnimation(String animation) { this.entityData.set(ANIMATION, animation); }
	public String getTexture() { return this.entityData.get(TEXTURE); }
	public void setTexture(String texture) { this.entityData.set(TEXTURE, texture); }

	// ── Network ───────────────────────────────────────────────────────────────

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	// ── Goals ─────────────────────────────────────────────────────────────────

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return 9.0; // ~3 block reach
			}
		});
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
	}

	// ── Type / removal ────────────────────────────────────────────────────────

	@Override
	public MobType getMobType() { return MobType.UNDEFINED; }

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) { return false; }

	@Override
	public boolean canAttack(LivingEntity target) {
		if (isWildKaiselin(target))
			return false;
		return super.canAttack(target);
	}

	// ── Sounds ────────────────────────────────────────────────────────────────

	@Override
	public SoundEvent getHurtSound(DamageSource ds) {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.hurt"));
	}

	@Override
	public SoundEvent getDeathSound() {
		return ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.generic.death"));
	}

	// ── Damage immunity ───────────────────────────────────────────────────────

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source.is(DamageTypes.IN_FIRE))   return false;
		if (source.is(DamageTypes.LIGHTNING_BOLT)) return false;
		if (isWildKaiselin(source.getEntity()) || isWildKaiselin(source.getDirectEntity()))
			return false;
		return super.hurt(source, amount);
	}

	private boolean isWildKaiselin(Entity entity) {
		return entity != null && entity.getType() == SololevelingModEntities.KAISELIN.get();
	}

	// ── Boss bar ──────────────────────────────────────────────────────────────

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

	// ── Tick ──────────────────────────────────────────────────────────────────

	@Override
	public void baseTick() {
		super.baseTick();
		this.refreshDimensions();
		// Run state-machine on server side only
		if (!this.level().isClientSide()) {
			BaranOnTickProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
		}
	}

	// ── NBT ───────────────────────────────────────────────────────────────────

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("baran_state", getState());
		compound.putBoolean("baran_phase2", getPersistentData().getBoolean("baran_phase2"));
		compound.putString("Texture", getTexture());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("baran_state"))
			setState(compound.getString("baran_state"));
		if (compound.getBoolean("baran_phase2"))
			getPersistentData().putBoolean("baran_phase2", true);
		if (compound.contains("Texture"))
			setTexture(compound.getString("Texture"));
	}

	// ── Death animation ───────────────────────────────────────────────────────

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime == 40) {
			this.remove(BaranEntity.RemovalReason.KILLED);
			this.dropExperience();
		}
	}

	// ── Dimensions ────────────────────────────────────────────────────────────

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return super.getDimensions(pose);
	}

	// ── Attributes ────────────────────────────────────────────────────────────

	public static void init() {}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.MAX_HEALTH, 300)
				.add(Attributes.ARMOR, 32)
				.add(Attributes.ARMOR_TOUGHNESS, 4)
				.add(Attributes.ATTACK_DAMAGE, 25)
				.add(Attributes.FOLLOW_RANGE, 40)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
	}

	// ── Animation ─────────────────────────────────────────────────────────────

	@SuppressWarnings("rawtypes")
	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			if (event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F)) {
				return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
			}
			return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
		}
		return PlayState.STOP;
	}

	@SuppressWarnings("rawtypes")
	private PlayState procedurePredicate(AnimationState event) {
		if (!animationprocedure.equals("empty") &&
				event.getController().getAnimationState() == AnimationController.State.STOPPED) {
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

	@SuppressWarnings({"rawtypes", "unchecked"})
	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		data.add(new AnimationController(this, "movement", 4, this::movementPredicate));
		data.add(new AnimationController(this, "procedure", 4, this::procedurePredicate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}
}
