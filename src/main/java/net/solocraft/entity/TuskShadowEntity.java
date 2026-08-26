
package net.solocraft.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoEntity;

import net.solocraft.procedures.TuskShadowOnEntityTickUpdateProcedure;
import net.solocraft.procedures.IsBerserkProcedure;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.ai.ShadowCommandTargetGoal;
import net.solocraft.entity.ai.ShadowFollowOwnerGoal;
import net.solocraft.entity.ai.TuskShadowCombatGoal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.solocraft.network.compat.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.damagesource.DamageTypes;
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
import net.minecraft.nbt.CompoundTag;

import java.util.List;

public class TuskShadowEntity extends TamableAnimal implements GeoEntity {
	public static final EntityDataAccessor<Boolean> SHOOT = SynchedEntityData.defineId(TuskShadowEntity.class, EntityDataSerializers.BOOLEAN);
	public static final EntityDataAccessor<String> ANIMATION = SynchedEntityData.defineId(TuskShadowEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> TEXTURE = SynchedEntityData.defineId(TuskShadowEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> DATA_IA = SynchedEntityData.defineId(TuskShadowEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_prot = SynchedEntityData.defineId(TuskShadowEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_fireball = SynchedEntityData.defineId(TuskShadowEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<String> DATA_state = SynchedEntityData.defineId(TuskShadowEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> DATA_smash = SynchedEntityData.defineId(TuskShadowEntity.class, EntityDataSerializers.INT);
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private boolean swinging;
	private boolean lastloop;
	private long lastSwing;
	public String animationprocedure = "empty";

	public TuskShadowEntity(EntityType<TuskShadowEntity> type, Level world) {
		super(type, world);
		xpReward = 0;
		setNoAi(false);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(SHOOT, false);
		builder.define(ANIMATION, "undefined");
		builder.define(TEXTURE, "kardalgan_shadow");
		builder.define(DATA_IA, 0);
		builder.define(DATA_prot, 0);
		builder.define(DATA_fireball, 0);
		builder.define(DATA_state, "idle");
		builder.define(DATA_smash, 0);
	}

	public void setTexture(String texture) {
		this.entityData.set(TEXTURE, texture);
	}

	public String getTexture() {
		return this.entityData.get(TEXTURE);
	}

	public void setCombatState(String state) {
		String next = state == null || state.isBlank() ? "idle" : state;
		if (!next.equals(this.entityData.get(DATA_state)))
			this.entityData.set(DATA_state, next);
	}

	public String getCombatState() {
		return this.entityData.get(DATA_state);
	}
@Override
	protected void registerGoals() {
		super.registerGoals();
		this.targetSelector.addGoal(0, new ShadowCommandTargetGoal(this));
		this.goalSelector.addGoal(1, new TuskShadowCombatGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, GreenOrcEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, HighOrcEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, GoblinArcherEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(5, new NearestAttackableTargetGoal(this, GoblinClubEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(6, new NearestAttackableTargetGoal(this, GoblinMageEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(7, new NearestAttackableTargetGoal(this, DKnight1Entity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(8, new NearestAttackableTargetGoal(this, DKnight2Entity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(9, new NearestAttackableTargetGoal(this, DKnight3Entity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(10, new NearestAttackableTargetGoal(this, OrcEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(11, new NearestAttackableTargetGoal(this, KasakaEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(12, new NearestAttackableTargetGoal(this, MiniGemGolemEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(13, new NearestAttackableTargetGoal(this, GemGolemEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(14, new NearestAttackableTargetGoal(this, BeruBossEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(15, new NearestAttackableTargetGoal(this, IgrisEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(16, new NearestAttackableTargetGoal(this, KargalganEntity.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(17, new NearestAttackableTargetGoal(this, Monster.class, false, false) {
			@Override
			public boolean canUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canUse() && IsBerserkProcedure.execute(entity) && !net.solocraft.util.ShadowMonarchManager.isShadowEntity(this.target);
			}

			@Override
			public boolean canContinueToUse() {
				double x = TuskShadowEntity.this.getX();
				double y = TuskShadowEntity.this.getY();
				double z = TuskShadowEntity.this.getZ();
				Entity entity = TuskShadowEntity.this;
				Level world = TuskShadowEntity.this.level();
				return super.canContinueToUse() && IsBerserkProcedure.execute(entity) && !net.solocraft.util.ShadowMonarchManager.isShadowEntity(this.target);
			}
		});
		this.goalSelector.addGoal(2, new ShadowFollowOwnerGoal(this));
		this.goalSelector.addGoal(20, new RandomStrollGoal(this, 0.25));
		this.goalSelector.addGoal(22, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(23, new FloatGoal(this));
		this.goalSelector.addGoal(24, new OpenDoorGoal(this, true));
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
		if (source.is(DamageTypes.FALL))
			return false;
		return super.hurt(source, amount);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("Texture", this.getTexture());
		compound.putInt("DataIA", this.entityData.get(DATA_IA));
		compound.putInt("Dataprot", this.entityData.get(DATA_prot));
		compound.putInt("Datafireball", this.entityData.get(DATA_fireball));
		compound.putString("Datastate", this.entityData.get(DATA_state));
		compound.putInt("Datasmash", this.entityData.get(DATA_smash));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("Texture"))
			this.setTexture(compound.getString("Texture"));
		if (compound.contains("DataIA"))
			this.entityData.set(DATA_IA, compound.getInt("DataIA"));
		if (compound.contains("Dataprot"))
			this.entityData.set(DATA_prot, compound.getInt("Dataprot"));
		if (compound.contains("Datafireball"))
			this.entityData.set(DATA_fireball, compound.getInt("Datafireball"));
		if (compound.contains("Datastate"))
			this.entityData.set(DATA_state, compound.getString("Datastate"));
		if (compound.contains("Datasmash"))
			this.entityData.set(DATA_smash, compound.getInt("Datasmash"));
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
					if (item.getFoodProperties(itemstack, this) != null && this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
						this.usePlayerItem(sourceentity, hand, itemstack);
						this.heal((float) item.getFoodProperties(itemstack, this).nutrition());
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
				if (this.random.nextInt(3) == 0 && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, sourceentity)) {
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
		TuskShadowOnEntityTickUpdateProcedure.execute(this.level(), this);
		this.refreshDimensions();
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose p_33597_) {
		return super.getDefaultDimensions(p_33597_).scale((float) 1).withEyeHeight(2.4F);
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
		TuskShadowEntity retval = SololevelingModEntities.TUSK_SHADOW.get().create(serverWorld);
		retval.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(retval.blockPosition()), MobSpawnType.BREEDING, null);
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
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 150);
		builder = builder.add(Attributes.ARMOR, 0);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 6);
		builder = builder.add(Attributes.FOLLOW_RANGE, 48);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 1);
		return builder;
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
			this.remove(TuskShadowEntity.RemovalReason.KILLED);
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
