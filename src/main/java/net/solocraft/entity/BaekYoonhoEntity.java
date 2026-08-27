
package net.solocraft.entity;

import net.solocraft.procedures.BaekYoonhoRightClickedOnEntityProcedure;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.NamedHunterCombatManager;

import net.minecraft.core.registries.BuiltInRegistries;
import net.solocraft.network.compat.NetworkHooks;

import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.solocraft.entity.ai.LegacyMeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.chat.Component;

public class BaekYoonhoEntity extends PathfinderMob {

	public BaekYoonhoEntity(EntityType<BaekYoonhoEntity> type, Level world) {
		super(type, world);
		getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1f);
		xpReward = 40;
		setNoAi(false);
		setCustomName(Component.literal("Baek Yoonho"));
		setCustomNameVisible(true);
		setPersistenceRequired();
		refreshDimensions();
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		this.targetSelector.addGoal(0, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Monster.class, false, false));
		this.goalSelector.addGoal(2, new LegacyMeleeAttackGoal(this, 1.2, false) {
			@Override
			protected double getAttackReachSqr(LivingEntity entity) {
				return 5.76;
			}
		});
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
	public InteractionResult mobInteract(Player sourceentity, InteractionHand hand) {
		ItemStack itemstack = sourceentity.getItemInHand(hand);
		InteractionResult retval = InteractionResult.sidedSuccess(this.level().isClientSide());
		super.mobInteract(sourceentity, hand);
		double x = this.getX();
		double y = this.getY();
		double z = this.getZ();
		Entity entity = this;
		Level world = this.level();

		BaekYoonhoRightClickedOnEntityProcedure.execute(world, x, y, z, entity, sourceentity);
		return retval;
	}

	@Override
	public EntityDimensions getDefaultDimensions(Pose pose) {
		return super.getDefaultDimensions(pose).scale(1.2f);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!this.level().isClientSide())
			NamedHunterCombatManager.tick(this);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		AttributeSupplier.Builder builder = Mob.createMobAttributes();
		builder = builder.add(Attributes.MOVEMENT_SPEED, 0.41);
		builder = builder.add(Attributes.MAX_HEALTH, 340);
		builder = builder.add(Attributes.ARMOR, 34);
		builder = builder.add(Attributes.ATTACK_DAMAGE, 27);
		builder = builder.add(Attributes.FOLLOW_RANGE, 64);
		builder = builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.55);
		builder = builder.add(Attributes.ATTACK_KNOCKBACK, 1.0);
		return builder;
	}
}
