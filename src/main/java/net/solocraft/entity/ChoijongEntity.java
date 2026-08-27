
package net.solocraft.entity;

import net.solocraft.procedures.ChoijongRightClickedOnEntityProcedure;
import net.solocraft.procedures.ChoijongOnEntityTickUpdateProcedure;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.FireMageSpellManager;
import net.solocraft.util.NamedHunterCombatManager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.solocraft.network.compat.NetworkHooks;

import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.tags.BlockTags;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;

public class ChoijongEntity extends PathfinderMob implements RangedAttackMob {
	public static final EntityDataAccessor<Integer> DATA_IA = SynchedEntityData.defineId(ChoijongEntity.class, EntityDataSerializers.INT);
	public static final EntityDataAccessor<Integer> DATA_backoff = SynchedEntityData.defineId(ChoijongEntity.class, EntityDataSerializers.INT);

	public ChoijongEntity(EntityType<ChoijongEntity> type, Level world) {
		super(type, world);
		getAttribute(Attributes.STEP_HEIGHT).setBaseValue(0.6f);
		xpReward = 35;
		setNoAi(false);
		setCustomName(Component.literal("Choi Jong-In"));
		setCustomNameVisible(true);
		setPersistenceRequired();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_IA, 0);
		builder.define(DATA_backoff, 0);
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Monster.class, false, false));
		this.goalSelector.addGoal(2, new RandomStrollGoal(this, 1));
		this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(5, new FloatGoal(this));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public net.minecraft.world.phys.Vec3 getVehicleAttachmentPoint(net.minecraft.world.entity.Entity vehicle) {
		return super.getVehicleAttachmentPoint(vehicle).add(0.0D, 0.35D, 0.0D);
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
	public boolean hurt(DamageSource damagesource, float amount) {
		if (damagesource.is(DamageTypes.IN_FIRE))
			return false;
		return super.hurt(damagesource, amount);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag compound) {
		super.addAdditionalSaveData(compound);
		compound.putInt("DataIA", this.entityData.get(DATA_IA));
		compound.putInt("Databackoff", this.entityData.get(DATA_backoff));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		if (compound.contains("DataIA"))
			this.entityData.set(DATA_IA, compound.getInt("DataIA"));
		if (compound.contains("Databackoff"))
			this.entityData.set(DATA_backoff, compound.getInt("Databackoff"));
	}

	@Override
	public InteractionResult mobInteract(Player sourceentity, InteractionHand hand) {
		ItemStack itemstack = sourceentity.getItemInHand(hand);
		InteractionResult retval = InteractionResult.sidedSuccess(this.level().isClientSide());
		super.mobInteract(sourceentity, hand);
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		Entity entity = this;
		Level world = this.level();

		ChoijongRightClickedOnEntityProcedure.execute(world, x, y, z, entity, sourceentity);
		return retval;
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!this.level().isClientSide()) {
			NamedHunterCombatManager.tick(this);
			ChoijongOnEntityTickUpdateProcedure.execute(this.level(), this);
		}
	}

	@Override
	public void performRangedAttack(LivingEntity target, float flval) {
		FireMageSpellManager.castNpc(this, FireMageSpellManager.FLAME_WEAVING);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.34);
		builder = builder.add(Attributes.MAX_HEALTH, 240);
		builder = builder.add(Attributes.ARMOR, 8);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 12);
		builder = builder.add(Attributes.FOLLOW_RANGE, 64);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.2);
		return builder;
	}
}
