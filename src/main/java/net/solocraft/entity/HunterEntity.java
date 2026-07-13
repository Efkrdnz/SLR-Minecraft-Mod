
package net.solocraft.entity;

import net.solocraft.procedures.IsNotInCombatProcedure;
import net.solocraft.procedures.IsNotAvoidingMeleeProcedure;
import net.solocraft.procedures.HunterRightClickedOnEntityProcedure;
import net.solocraft.procedures.HunterOnInitialEntitySpawnProcedure;
import net.solocraft.procedures.HunterOnEntityTickUpdateProcedure;
import net.solocraft.init.SololevelingModEntities;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;

import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
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
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;

public class HunterEntity extends TamableAnimal {
	public static final EntityDataAccessor<String> DATA_Rank = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> DATA_HunterClass = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<Integer> DATA_TopIn = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_TopOut = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_Bottom = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_Foot = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_Eyes = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_EyeBs = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_Hair = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_Mouth = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_IA = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_backoff = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<String> DATA_Allies = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.STRING);
	public static final EntityDataAccessor<String> DATA_Enemies = SynchedEntityData.defineId(HunterEntity.class, EntityDataSerializers.STRING);

	public HunterEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.HUNTER.get(), world);
	}

	public HunterEntity(EntityType<HunterEntity> type, Level world) {
		super(type, world);
		setMaxUpStep(0.6f);
		xpReward = 4;
		setNoAi(false);
		setCustomName(Component.literal("Hunter"));
		setCustomNameVisible(true);
		setPersistenceRequired();
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(DATA_Rank, "");
		this.entityData.define(DATA_HunterClass, "");
		this.entityData.define(DATA_TopIn, 0);
		this.entityData.define(DATA_TopOut, 0);
		this.entityData.define(DATA_Bottom, 0);
		this.entityData.define(DATA_Foot, 0);
		this.entityData.define(DATA_Eyes, 0);
		this.entityData.define(DATA_EyeBs, 0);
		this.entityData.define(DATA_Hair, 0);
		this.entityData.define(DATA_Mouth, 0);
		this.entityData.define(DATA_IA, 0);
		this.entityData.define(DATA_backoff, 0);
		this.entityData.define(DATA_Allies, "");
		this.entityData.define(DATA_Enemies, "");
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.getNavigation().getNodeEvaluator().setCanOpenDoors(true);
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Monster.class, true, true));
		this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				double reach = this.mob.getBbWidth() * 0.5D + entity.getBbWidth() * 0.5D + 1.25D;
				return reach * reach;
			}

			@Override
			public boolean canUse() {
				double x = HunterEntity.this.getX();
				double y = HunterEntity.this.getY();
				double z = HunterEntity.this.getZ();
				Entity entity = HunterEntity.this;
				Level world = HunterEntity.this.level();
				return super.canUse() && IsNotAvoidingMeleeProcedure.execute(entity);
			}

		});
		this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1, (float) 10, (float) 2, false) {
			@Override
			public boolean canUse() {
				double x = HunterEntity.this.getX();
				double y = HunterEntity.this.getY();
				double z = HunterEntity.this.getZ();
				Entity entity = HunterEntity.this;
				Level world = HunterEntity.this.level();
				return super.canUse() && IsNotInCombatProcedure.execute(entity);
			}

			@Override
			public boolean canContinueToUse() {
				double x = HunterEntity.this.getX();
				double y = HunterEntity.this.getY();
				double z = HunterEntity.this.getZ();
				Entity entity = HunterEntity.this;
				Level world = HunterEntity.this.level();
				return super.canContinueToUse() && IsNotInCombatProcedure.execute(entity);
			}
		});
		this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
		this.goalSelector.addGoal(7, new RandomStrollGoal(this, 1));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(9, new FloatGoal(this));
		this.goalSelector.addGoal(10, new OpenDoorGoal(this, false));
		this.goalSelector.addGoal(11, new OpenDoorGoal(this, true));
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
	public double getMyRidingOffset() {
		return -0.35D;
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
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData livingdata, @Nullable CompoundTag tag) {
		SpawnGroupData retval = super.finalizeSpawn(world, difficulty, reason, livingdata, tag);
		HunterOnInitialEntitySpawnProcedure.execute(this);
		return retval;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putString("DataRank", this.entityData.get(DATA_Rank));
		compound.putString("DataHunterClass", this.entityData.get(DATA_HunterClass));
		compound.putInt("DataTopIn", this.entityData.get(DATA_TopIn));
		compound.putInt("DataTopOut", this.entityData.get(DATA_TopOut));
		compound.putInt("DataBottom", this.entityData.get(DATA_Bottom));
		compound.putInt("DataFoot", this.entityData.get(DATA_Foot));
		compound.putInt("DataEyes", this.entityData.get(DATA_Eyes));
		compound.putInt("DataEyeBs", this.entityData.get(DATA_EyeBs));
		compound.putInt("DataHair", this.entityData.get(DATA_Hair));
		compound.putInt("DataMouth", this.entityData.get(DATA_Mouth));
		compound.putInt("DataIA", this.entityData.get(DATA_IA));
		compound.putInt("Databackoff", this.entityData.get(DATA_backoff));
		compound.putString("DataAllies", this.entityData.get(DATA_Allies));
		compound.putString("DataEnemies", this.entityData.get(DATA_Enemies));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DataRank"))
			this.entityData.set(DATA_Rank, compound.getString("DataRank"));
		if (compound.contains("DataHunterClass"))
			this.entityData.set(DATA_HunterClass, compound.getString("DataHunterClass"));
		if (compound.contains("DataTopIn"))
			this.entityData.set(DATA_TopIn, compound.getInt("DataTopIn"));
		if (compound.contains("DataTopOut"))
			this.entityData.set(DATA_TopOut, compound.getInt("DataTopOut"));
		if (compound.contains("DataBottom"))
			this.entityData.set(DATA_Bottom, compound.getInt("DataBottom"));
		if (compound.contains("DataFoot"))
			this.entityData.set(DATA_Foot, compound.getInt("DataFoot"));
		if (compound.contains("DataEyes"))
			this.entityData.set(DATA_Eyes, compound.getInt("DataEyes"));
		if (compound.contains("DataEyeBs"))
			this.entityData.set(DATA_EyeBs, compound.getInt("DataEyeBs"));
		if (compound.contains("DataHair"))
			this.entityData.set(DATA_Hair, compound.getInt("DataHair"));
		if (compound.contains("DataMouth"))
			this.entityData.set(DATA_Mouth, compound.getInt("DataMouth"));
		if (compound.contains("DataIA"))
			this.entityData.set(DATA_IA, compound.getInt("DataIA"));
		if (compound.contains("Databackoff"))
			this.entityData.set(DATA_backoff, compound.getInt("Databackoff"));
		if (compound.contains("DataAllies"))
			this.entityData.set(DATA_Allies, compound.getString("DataAllies"));
		if (compound.contains("DataEnemies"))
			this.entityData.set(DATA_Enemies, compound.getString("DataEnemies"));
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
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		Entity entity = this;
		Level world = this.level();

		HunterRightClickedOnEntityProcedure.execute(entity, sourceentity);
		return retval;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!this.level().isClientSide()) {
			HunterOnEntityTickUpdateProcedure.execute(this.level(), this.getX(), this.getY(), this.getZ(), this);
		}
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel serverWorld, AgeableMob ageable) {
		HunterEntity retval = SololevelingModEntities.HUNTER.get().create(serverWorld);
		retval.finalizeSpawn(serverWorld, serverWorld.getCurrentDifficultyAt(retval.blockPosition()), MobSpawnType.BREEDING, null, null);
		return retval;
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return Ingredient.of(new ItemStack(Items.COMMAND_BLOCK_MINECART)).test(stack);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.3);
		builder = builder.add(Attributes.MAX_HEALTH, 20);
		builder = builder.add(Attributes.ARMOR, 6);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 1);
		builder = builder.add(Attributes.FOLLOW_RANGE, 40);
		return builder;
	}
}
