
package net.solocraft.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.solocraft.procedures.IgrisEntityIsHurtProcedure;
import net.solocraft.procedures.IgrisEntityDiesProcedure;
import net.solocraft.procedures.BloodRedComIgrisOnEntityTickUpdateProcedure;
import net.solocraft.procedures.BloodRedComIgrisDeathTimeIsReachedProcedure;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.CombatRangeHelper;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import java.util.EnumSet;

public class BloodRedComIgrisEntity extends Monster implements GeoEntity {

    public static final EntityDataAccessor<Boolean>  SHOOT      = SynchedEntityData.defineId(BloodRedComIgrisEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String>   ANIMATION  = SynchedEntityData.defineId(BloodRedComIgrisEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String>   TEXTURE    = SynchedEntityData.defineId(BloodRedComIgrisEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<String>   DATA_state = SynchedEntityData.defineId(BloodRedComIgrisEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Integer>  DATA_IA    = SynchedEntityData.defineId(BloodRedComIgrisEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public String animationprocedure = "empty";

    public BloodRedComIgrisEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(SololevelingModEntities.BLOOD_RED_COM_IGRIS.get(), world);
    }

    public BloodRedComIgrisEntity(EntityType<BloodRedComIgrisEntity> type, Level world) {
        super(type, world);
        xpReward = 0;
        setNoAi(false);
        setCustomName(Component.literal("§cBlood Red Commander Igris"));
        setCustomNameVisible(true);
        setPersistenceRequired();
    }

    // ── Synced data ───────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SHOOT,      false);
        this.entityData.define(ANIMATION,  "undefined");
        this.entityData.define(TEXTURE,    "igris_marcus");
        this.entityData.define(DATA_state, "idle");
        this.entityData.define(DATA_IA,    0);
    }

    public void setTexture(String texture) { this.entityData.set(TEXTURE, texture); }
    public String getTexture()             { return this.entityData.get(TEXTURE); }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        // 0 — float on water (no MOVE conflict)
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // 1 — circle / approach target at medium range
        this.goalSelector.addGoal(1, new IgrisCircleGoal(this));
        // 2 — melee fallback when very close or circle goal inactive
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.4, false) {
            @Override
            protected double getAttackReachSqr(LivingEntity target) {
                return 5.0;
            }
        });
        // 3 — wander when no target
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 0.5));
        // 4 — look around when idle
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false, false));
    }

    /**
     * Circles around or approaches the target smoothly.
     * Active when Igris is 4.5–15 blocks away and in idle state.
     * Randomly flips strafe direction and blends lateral with approach movement
     * so the fight feels dynamic rather than a straight-line charge.
     */
    private static final class IgrisCircleGoal extends Goal {

        private final BloodRedComIgrisEntity igris;
        private double strafeSign = 1.0;
        private int recalcTimer  = 0;

        IgrisCircleGoal(BloodRedComIgrisEntity igris) {
            this.igris = igris;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = igris.getTarget();
            if (t == null) return false;
            double dist = CombatRangeHelper.surfaceDistance(igris, t);
            return igris.getPersistentData().getString("state").equals("idle")
                    && dist > 4.5 && dist < 15.0;
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity t = igris.getTarget();
            if (t == null) return false;
            return igris.getPersistentData().getString("state").equals("idle")
                    && CombatRangeHelper.surfaceDistance(igris, t) < 15.0;
        }

        @Override
        public void start() {
            strafeSign   = igris.getRandom().nextBoolean() ? 1.0 : -1.0;
            recalcTimer  = 0;
        }

        @Override
        public void tick() {
            LivingEntity target = igris.getTarget();
            if (target == null) return;

            recalcTimer--;
            if (recalcTimer > 0) return;
            recalcTimer = 10 + igris.getRandom().nextInt(8);

            // Occasionally reverse the strafe direction for unpredictability
            if (igris.getRandom().nextFloat() < 0.22f) strafeSign = -strafeSign;

            double dist         = CombatRangeHelper.surfaceDistance(igris, target);
            Vec3   toTargetNorm = target.position().subtract(igris.position()).normalize();
            Vec3   lateral      = new Vec3(-toTargetNorm.z * strafeSign, 0, toTargetNorm.x * strafeSign);

            Vec3 dest;
            if (dist > 7.0) {
                // Far — blend approach + strafe to close the gap while circling
                dest = igris.position()
                        .add(toTargetNorm.scale(3.0))
                        .add(lateral.scale(2.0));
            } else {
                // Medium — orbit at ~5.5 blocks without rushing in
                dest = target.position()
                        .add(toTargetNorm.scale(-5.5))
                        .add(lateral.scale(3.5));
            }

            igris.getNavigation().moveTo(dest.x, dest.y, dest.z, 1.3);
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Override
    public MobType getMobType() { return MobType.UNDEFINED; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

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
        IgrisEntityIsHurtProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this, source.getEntity());
        if (source.is(DamageTypes.FALL))  return false;
        if (source.is(DamageTypes.DROWN)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        IgrisEntityDiesProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this, source.getEntity());
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("Texture",  this.getTexture());
        compound.putString("Datastate", this.entityData.get(DATA_state));
        compound.putInt("DataIA",       this.entityData.get(DATA_IA));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Texture"))    this.setTexture(compound.getString("Texture"));
        if (compound.contains("Datastate"))  this.entityData.set(DATA_state, compound.getString("Datastate"));
        if (compound.contains("DataIA"))     this.entityData.set(DATA_IA, compound.getInt("DataIA"));
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void baseTick() {
        super.baseTick();
        BloodRedComIgrisOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
        this.refreshDimensions();
    }

    @Override
    public EntityDimensions getDimensions(Pose p_33597_) {
        return super.getDimensions(p_33597_).scale(1f);
    }

    @Override
    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(BloodRedComIgrisEntity.RemovalReason.KILLED);
            this.dropExperience();
            BloodRedComIgrisDeathTimeIsReachedProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
        }
    }

    // ── Attributes ────────────────────────────────────────────────────────────

    public static void init() {}

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED,      0.58)
                .add(Attributes.MAX_HEALTH,          110)
                .add(Attributes.ARMOR,               24)
                .add(Attributes.ATTACK_DAMAGE,       0)
                .add(Attributes.FOLLOW_RANGE,        32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.75)
                .add(Attributes.ATTACK_KNOCKBACK,    1);
    }

    // ── Animation ─────────────────────────────────────────────────────────────

    private PlayState movementPredicate(AnimationState event) {
        if (this.animationprocedure.equals("empty")) {
            if (this.isAggressive() && event.isMoving())
                return event.setAndContinue(RawAnimation.begin().thenLoop("run"));
            if (event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F))
                return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        }
        return PlayState.STOP;
    }

    private PlayState procedurePredicate(AnimationState event) {
        if (!animationprocedure.equals("empty")
                && event.getController().getAnimationState() == AnimationController.State.STOPPED) {
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

    public String getSyncedAnimation() { return this.entityData.get(ANIMATION); }

    public void setAnimation(String animation) { this.entityData.set(ANIMATION, animation); }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement",  4, this::movementPredicate));
        data.add(new AnimationController<>(this, "procedure", 4, this::procedurePredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return this.cache; }
}
