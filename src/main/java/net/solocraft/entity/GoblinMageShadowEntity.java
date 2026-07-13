
package net.solocraft.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.solocraft.procedures.IsNotBerserkProcedure;
import net.solocraft.procedures.IsBerserkProcedure;
import net.solocraft.procedures.GoblinMageShadowOnEntityTickUpdateProcedure;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class GoblinMageShadowEntity extends TamableAnimal implements GeoEntity {
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(GoblinMageShadowEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(GoblinMageShadowEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(GoblinMageShadowEntity.class, EntityDataSerializers.STRING);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";

	public GoblinMageShadowEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.GOBLIN_MAGE_SHADOW.get(), world);
	}

	public GoblinMageShadowEntity(EntityType<GoblinMageShadowEntity> type, Level world) {
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
		this.entityData.define(TEXTURE, "goblin_mage_shadow");
	}

	public void setTexture(String texture) {
		this.entityData.set(TEXTURE, texture);
	}

	public String getTexture() {
		return this.entityData.get(TEXTURE);
	}

	@Override
	protected float getStandingEyeHeight(Pose poseIn, EntityDimensions sizeIn) {
		return 1.8F;
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return 14.44;
			}
		});
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, GoblinArcherEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, GoblinClubEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(5, new NearestAttackableTargetGoal(this, GoblinMageEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(6, new NearestAttackableTargetGoal(this, DKnight1Entity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(7, new NearestAttackableTargetGoal(this, DKnight2Entity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(8, new NearestAttackableTargetGoal(this, DKnight3Entity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(9, new NearestAttackableTargetGoal(this, OrcEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(10, new NearestAttackableTargetGoal(this, KasakaEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(11, new NearestAttackableTargetGoal(this, MiniGemGolemEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(12, new NearestAttackableTargetGoal(this, GemGolemEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(13, new NearestAttackableTargetGoal(this, BeruBossEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(14, new NearestAttackableTargetGoal(this, IgrisEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(15, new OwnerHurtTargetGoal(this));
		this.goalSelector.addGoal(16, new FollowOwnerGoal(this, 1, (float) 30, (float) 5, false) {
			@Override
			public boolean canUse() {
				double x = GoblinMageShadowEntity.this.getX();
				double y = GoblinMageShadowEntity.this.getY();
				double z = GoblinMageShadowEntity.this.getZ();
				Entity entity = GoblinMageShadowEntity.this;
				Level world = GoblinMageShadowEntity.this.level();
				return super.canUse() && IsNotBerserkProcedure.execute(entity);
			}
		});
		this.goalSelector.addGoal(17, new RandomStrollGoal(this, 0.5));
		this.goalSelector.addGoal(18, new OwnerHurtByTargetGoal(this));
		this.goalSelector.addGoal(19, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(20, new FloatGoal(this));
		this.goalSelector.addGoal(21, new OpenDoorGoal(this, true));
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
	public InteractionResult mobInteract(Player sourceentity, InteractionHand hand) {
		ItemStack itemstack = sourceentity.getItemInHand(hand);
		InteractionResult retval = InteractionResult.sidedSuccess(this.level().isClientSide());
		Item item = itemstack.getItem();
		if (itemstack.getItem() instanceof SpawnEggItem) {
			retval = super.mobInteract(sourceentity, hand);
		} else if (this.level().isClientSide()) {
			retval = (this.isTame() && this.isOwnedBy(sourceentity) || this.isFood(itemstack)) ? InteractionResult.sidedSuccess(this.level().isClientSide()) : InteractionResult.PASS;
		} else {
			if (this.isTame()) {
				if (this.isOwnedBy(sourceentity)) {
					if (item.isEdible() && this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
						this.usePlayerItem(sourceentity, hand, itemstack);
						this.heal((float) item.getFoodProperties().getNutrition());
						retval = InteractionResult.sidedSuccess(this.level().isClientSide());
					} else if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
						this.usePlayerItem(sourceentity, hand, itemstack);
						this.heal(4);
						retval = InteractionResult.sidedSuccess(this.level().isClientSide());
					} else {
						retval = super.mobInteract(sourceentity, hand);
					}
				}
			} else if (this.isFood(itemstack)) {
				this.usePlayerItem(sourceentity, hand, itemstack);
				if (this.random.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, sourceentity)) {
					this.tame(sourceentity);
					this.level().broadcastEntityEvent(this, (byte) 7);
				} else {
					this.level().broadcastEntityEvent(this, (byte) 6);
				}
				this.setPersistenceRequired();
				retval = InteractionResult.sidedSuccess(this.level().isClientSide());
			} else {
				retval = super.mobInteract(sourceentity, hand);
				if (retval == InteractionResult.SUCCESS || retval == InteractionResult.CONSUME)
					this.setPersistenceRequired();
			}
		}
		return retval;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		GoblinMageShadowOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
		this.refreshDimensions();
	}

	@Override
	public EntityDimensions getDimensions(Pose p_33597_) {
		return super.getDimensions(p_33597_).scale((float) 1);
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
		GoblinMageShadowEntity retval = SololevelingModEntities.GOBLIN_MAGE_SHADOW.get().create(serverWorld);
		retval.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(retval.blockPosition()), MobSpawnType.BREEDING, null, null);
		return retval;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return List.of().contains(stack.getItem());
	}

	@Override
	public void aiStep() {
		super.aiStep();
		this.updateSwingTime();
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.1);
		builder = builder.add(Attributes.MAX_HEALTH, 18);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 0);
		builder = builder.add(Attributes.FOLLOW_RANGE, 32);
		return builder;
	}

	private PlayState movementPredicate(AnimationState event) {
		if (this.animationprocedure.equals("empty")) {
			if ((event.isMoving() || !(event.getLimbSwingAmount() > -0.15F && event.getLimbSwingAmount() < 0.15F))

			) {
				return event.setAndContinue(RawAnimation.begin().thenLoop("walk"));
			}
			if (this.isDeadOrDying()) {
				return event.setAndContinue(RawAnimation.begin().thenPlay("death"));
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
			return event.setAndContinue(RawAnimation.begin().thenPlay("attack"));
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
		if (this.deathTime == 80) {
			this.remove(GoblinMageShadowEntity.RemovalReason.KILLED);
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
