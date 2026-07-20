
package net.solocraft.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.solocraft.procedures.StatueOfGodOnInitialEntitySpawnProcedure;
import net.solocraft.procedures.StatueOfGodOnEntityTickUpdateProcedure;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public class StatueOfGodEntity extends Monster implements GeoEntity {
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(StatueOfGodEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(StatueOfGodEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(StatueOfGodEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> DATA_state = SynchedEntityData.defineId(StatueOfGodEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Boolean> DATA_smiled = SynchedEntityData.defineId(StatueOfGodEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<Integer> DATA_default_x = SynchedEntityData.defineId(StatueOfGodEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_default_y = SynchedEntityData.defineId(StatueOfGodEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_default_z = SynchedEntityData.defineId(StatueOfGodEntity.class, EntityDataSerializers.INT);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private boolean dimensionsInitialized;
	private long lastSwing;
	public String animationprocedure = "empty";

	public StatueOfGodEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.STATUE_OF_GOD.get(), world);
	}

	public StatueOfGodEntity(EntityType<StatueOfGodEntity> type, Level world) {
		super(type, world);
		xpReward = 100;
		setNoAi(false);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(SHOOT, false);
		this.entityData.define(ANIMATION, "undefined");
		this.entityData.define(TEXTURE, "statue_of_god");
		this.entityData.define(DATA_state, "throne");
		this.entityData.define(DATA_smiled, true);
		this.entityData.define(DATA_default_x, 0);
		this.entityData.define(DATA_default_y, 0);
		this.entityData.define(DATA_default_z, 0);
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
				return 25;
			}
		});
	}

	private boolean isValidCombatTarget(LivingEntity candidate) {
		return candidate instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator();
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
		if (source.is(DamageTypes.CACTUS) || amount < 25.0F || isSharpDamage(source))
			return false;
		return super.hurt(source, amount);
	}

	private static boolean isSharpDamage(DamageSource source) {
		if (source.getDirectEntity() instanceof AbstractArrow)
			return true;
		if (!(source.getEntity() instanceof LivingEntity attacker))
			return false;
		return isSharpWeapon(attacker.getMainHandItem()) || isSharpWeapon(attacker.getOffhandItem());
	}

	private static boolean isSharpWeapon(ItemStack stack) {
		if (stack.getItem() instanceof SwordItem
				|| stack.getItem() instanceof AxeItem
				|| stack.getItem() instanceof TridentItem)
			return true;
		var itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
		if (itemId == null)
			return false;
		String path = itemId.getPath();
		return path.contains("sword") || path.contains("dagger") || path.contains("knife")
				|| path.contains("blade") || path.contains("katana") || path.contains("spear")
				|| path.contains("scythe") || path.contains("sai") || path.contains("trident");
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata, tag);
		StatueOfGodOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", this.getTexture());
		compound.putString("Datastate", this.entityData.get(DATA_state));
		compound.putBoolean("Datasmiled", this.entityData.get(DATA_smiled));
		compound.putInt("Datadefault_x", this.entityData.get(DATA_default_x));
		compound.putInt("Datadefault_y", this.entityData.get(DATA_default_y));
		compound.putInt("Datadefault_z", this.entityData.get(DATA_default_z));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
		if (compound.contains("Datastate"))
			this.entityData.set(DATA_state, compound.getString("Datastate"));
		if (compound.contains("Datasmiled"))
			this.entityData.set(DATA_smiled, compound.getBoolean("Datasmiled"));
		if (compound.contains("Datadefault_x"))
			this.entityData.set(DATA_default_x, compound.getInt("Datadefault_x"));
		if (compound.contains("Datadefault_y"))
			this.entityData.set(DATA_default_y, compound.getInt("Datadefault_y"));
		if (compound.contains("Datadefault_z"))
			this.entityData.set(DATA_default_z, compound.getInt("Datadefault_z"));
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!dimensionsInitialized) {
			refreshDimensions();
			dimensionsInitialized = true;
		}
		StatueOfGodOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (!this.level().isClientSide() && this.getPersistentData().getString("state").equals("aggresive")
				&& isValidCombatTarget(this.getTarget()))
			faceTarget(this.getTarget());
	}

	public void faceTarget(LivingEntity target) {
		if (target == null)
			return;
		double dx = target.getX() - this.getX();
		double dz = target.getZ() - this.getZ();
		if (dx * dx + dz * dz < 1.0E-6D)
			return;
		faceYaw((float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F);
	}

	public void faceYaw(float yaw) {
		float wrappedYaw = Mth.wrapDegrees(yaw);
		this.setYRot(wrappedYaw);
		this.yRotO = wrappedYaw;
		this.yBodyRot = wrappedYaw;
		this.yBodyRotO = wrappedYaw;
		this.yHeadRot = wrappedYaw;
		this.yHeadRotO = wrappedYaw;
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		// The rendered Blockbench model is roughly 23 blocks tall at its 3x render scale.
		// Keep the width around the body instead of including the full throne silhouette.
		return EntityDimensions.scalable(5.25F, 23.25F);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.4);
		builder = builder.add(Attributes.MAX_HEALTH, 700);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 85);
		builder = builder.add(Attributes.FOLLOW_RANGE, 256);
		return builder;
	}

	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			String state = this.entityData.get(DATA_state);
			if (state.equals("aggresive")) {
				if (isActuallyMoving()) {
					return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
				}
				return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
			}
			// If the synced procedure animation arrives a frame after the waking
			// state, remain seated instead of flashing into the standing idle pose.
			return event.setAndContinue(RawAnimation.begin().thenLoop("sitting"));
		}
		return PlayState.STOP;
	}

	private boolean isActuallyMoving() {
		double dx = this.getX() - this.xOld;
		double dz = this.getZ() - this.zOld;
		return dx * dx + dz * dz > 1.0E-6D
				|| this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-5D;
	}

	private PlayState procedurePredicate(AnimationState event) {
		if (animationprocedure.equals("empty")) {
			lastloop = false;
			return PlayState.STOP;
		}
		if (!lastloop) {
			lastloop = true;
			event.getController().forceAnimationReset();
			return event.setAndContinue(RawAnimation.begin().thenPlay(this.animationprocedure));
		}
		if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
			this.animationprocedure = "empty";
			lastloop = false;
			event.getController().forceAnimationReset();
			return PlayState.STOP;
		}
		return PlayState.CONTINUE;
	}

	@Override
	protected void tickDeath() {
		++this.deathTime;
		if (this.deathTime == 1) {
			this.remove(StatueOfGodEntity.RemovalReason.KILLED);
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
