package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.network.PlayMessages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;

import java.util.Comparator;
import java.util.UUID;

public class ShadowKaiselinEntity extends KaiselinEntity implements OwnableEntity {
	private static final String OWNER_TAG = "sl_shadow_owner";
	private static final String COMMAND_TAG = "sl_shadow_command";
	private static final TagKey<EntityType<?>> SHADOWS = TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows"));
	private static final EntityDimensions SHADOW_KAISEL_BODY_DIMENSIONS = EntityDimensions.scalable(2.25F, 1.85F);

	public ShadowKaiselinEntity(PlayMessages.SpawnEntity packet, Level world) {
		this(SololevelingModEntities.SHADOW_KAISELIN.get(), world);
	}

	public ShadowKaiselinEntity(EntityType<ShadowKaiselinEntity> type, Level world) {
		super(type, world);
		this.setKaiselinVariant("shadow_kaiselin", "Kaisel");
		this.refreshDimensions();
	}

	@Override
	protected LivingEntity getOrFindTarget() {
		if (this.isVehicle())
			return null;
		Player owner = getOwnerPlayer();
		String command = this.getPersistentData().getString(COMMAND_TAG);
		boolean clearDungeon = ShadowMonarchManager.COMMAND_CLEAR_DUNGEON.equals(command);
		if (command.isEmpty() || ShadowMonarchManager.COMMAND_DEFAULT.equals(command)) {
			LivingEntity target = ShadowMonarchManager.findDefaultCommandTarget(this, owner);
			this.setTarget(target);
			return target;
		}
		if (ShadowMonarchManager.COMMAND_FOLLOW.equals(command)) {
			this.setTarget(null);
			return null;
		}
		if (clearDungeon && owner != null && !ShadowMonarchManager.isInDungeon(owner)) {
			this.setTarget(null);
			return null;
		}
		if (ShadowMonarchManager.COMMAND_PROTECT.equals(command)) {
			LivingEntity threat = findOwnerThreat(owner);
			this.setTarget(threat);
			return threat;
		}
		LivingEntity ownerTarget = owner == null ? null : owner.getLastHurtMob();
		if (isValidCombatTarget(ownerTarget) && (!clearDungeon || hasLineOfSight(ownerTarget))) {
			this.setTarget(ownerTarget);
			return ownerTarget;
		}
		LivingEntity ownerAttacker = owner == null ? null : owner.getLastHurtByMob();
		if (isValidCombatTarget(ownerAttacker) && (!clearDungeon || hasLineOfSight(ownerAttacker))) {
			this.setTarget(ownerAttacker);
			return ownerAttacker;
		}
		LivingEntity current = this.getTarget();
		if (isValidCombatTarget(current) && (!clearDungeon || hasLineOfSight(current)))
			return current;
		this.setTarget(null);
		double range = clearDungeon ? 64.0D : 42.0D;
		LivingEntity nearest = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(range), target -> isValidCombatTarget(target) && (!clearDungeon || hasLineOfSight(target))).stream()
				.min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this))).orElse(null);
		if (nearest != null)
			this.setTarget(nearest);
		return nearest;
	}

	private LivingEntity findOwnerThreat(Player owner) {
		if (owner == null)
			return null;
		LivingEntity current = this.getTarget();
		if (current instanceof Mob mob && mob.getTarget() == owner && isValidCombatTarget(current))
			return current;
		LivingEntity attacker = owner.getLastHurtByMob();
		if (isValidCombatTarget(attacker))
			return attacker;
		return this.level().getEntitiesOfClass(Mob.class, owner.getBoundingBox().inflate(48.0D), mob -> mob.getTarget() == owner && isValidCombatTarget(mob)).stream()
				.min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this))).orElse(null);
	}

	@Override
	protected boolean isValidCombatTarget(LivingEntity target) {
		if (target == null || !target.isAlive() || target == this || target == this.getControllingPassenger())
			return false;
		if (target instanceof Player)
			return false;
		if (target.getType().is(SHADOWS))
			return false;
		if (target instanceof TamableAnimal tame && getOwnerUUID() != null && getOwnerUUID().equals(tame.getOwnerUUID()))
			return false;
		return target instanceof Monster || super.isValidCombatTarget(target);
	}

	@Override
	public void baseTick() {
		super.baseTick();
		if (!this.level().isClientSide() && !this.isVehicle() && this.getTarget() == null) {
			Player owner = getOwnerPlayer();
			if (owner != null && owner.isAlive()) {
				double distance = this.distanceTo(owner);
				if (distance > 48.0D) {
					this.teleportTo(owner.getX(), owner.getY() + 2.0D, owner.getZ());
				} else if (distance > 8.0D) {
					flyToward(owner.position().add(0.0D, 4.0D, 0.0D), 0.12D, 0.86D);
					turnToward(owner.position(), 10.0F);
				}
			}
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		if (isOwnedBy(player) && !player.isShiftKeyDown()) {
			if (!this.level().isClientSide()) {
				player.startRiding(this);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		return super.mobInteract(player, hand);
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return this.getPassengers().isEmpty() && passenger instanceof Player player && isOwnedBy(player);
	}

	@Override
	public LivingEntity getControllingPassenger() {
		Entity passenger = this.getFirstPassenger();
		return passenger instanceof LivingEntity living ? living : null;
	}

	@Override
	public double getPassengersRidingOffset() {
		return 2.15D;
	}

	@Override
	protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
		if (this.hasPassenger(passenger))
			moveFunction.accept(passenger, this.getX(), this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ());
	}

	@Override
	public void travel(Vec3 travelVector) {
		LivingEntity controller = this.getControllingPassenger();
		if (controller instanceof Player rider) {
			this.setNoGravity(true);
			this.setYRot(rider.getYRot());
			this.setXRot(Mth.clamp(rider.getXRot(), -55.0F, 55.0F) * 0.45F);
			this.yBodyRot = this.getYRot();
			this.yHeadRot = this.getYRot();
			this.setYHeadRot(this.getYRot());

			float forwardInput = rider.zza;
			float strafeInput = rider.xxa;
			Vec3 look = rider.getLookAngle();
			Vec3 flatForward = new Vec3(look.x, 0.0D, look.z);
			if (flatForward.lengthSqr() < 0.001D)
				flatForward = Vec3.directionFromRotation(0.0F, rider.getYRot()).multiply(1.0D, 0.0D, 1.0D);
			flatForward = flatForward.normalize();
			Vec3 right = new Vec3(-flatForward.z, 0.0D, flatForward.x);
			double pitchLift = -Mth.sin(rider.getXRot() * Mth.DEG_TO_RAD) * Math.max(0.0F, forwardInput) * 0.58D;
			double descend = rider.isShiftKeyDown() ? -0.42D : 0.0D;
			Vec3 desired = flatForward.scale(forwardInput * 0.95D).add(right.scale(-strafeInput * 0.55D)).add(0.0D, pitchLift + descend, 0.0D);
			Vec3 next = desired.lengthSqr() > 0.001D ? this.getDeltaMovement().scale(0.68D).add(desired.scale(0.32D)) : this.getDeltaMovement().scale(0.82D);
			this.setDeltaMovement(next);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.hasImpulse = true;
			return;
		}
		super.travel(travelVector);
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		Entity attacker = source.getEntity();
		if (attacker instanceof Player player && isOwnedBy(player))
			return false;
		if (ShadowMonarchManager.haveSameShadowOwner(this, attacker))
			return false;
		return super.hurt(source, amount);
	}

	@Override
	public boolean isAlliedTo(Entity entity) {
		if (super.isAlliedTo(entity))
			return true;
		if (entity instanceof Player player && isOwnedBy(player))
			return true;
		return ShadowMonarchManager.haveSameShadowOwner(this, entity);
	}

	@Override
	public void die(DamageSource source) {
		ShadowMonarchManager.dropStoredShadowInventory(this);
		Player owner = getOwnerPlayer();
		if (owner != null) {
			owner.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				capability.KaiselSpawned = Math.max(0, capability.KaiselSpawned - 1);
				capability.syncPlayerVariables(owner);
			});
		}
		super.die(source);
	}

	@Override
	public EntityDimensions getDimensions(Pose pose) {
		return SHADOW_KAISEL_BODY_DIMENSIONS;
	}

	@Override
	public void readAdditionalSaveData(CompoundTag compound) {
		super.readAdditionalSaveData(compound);
		this.setKaiselinVariant("shadow_kaiselin", "Kaisel");
	}

	private boolean isOwnedBy(Player player) {
		UUID owner = getOwnerUUID();
		return owner != null && player != null && owner.equals(player.getUUID());
	}

	@Override
	public UUID getOwnerUUID() {
		CompoundTag data = this.getPersistentData();
		return data.hasUUID(OWNER_TAG) ? data.getUUID(OWNER_TAG) : null;
	}

	@Override
	public LivingEntity getOwner() {
		UUID owner = getOwnerUUID();
		return owner == null ? null : this.level().getPlayerByUUID(owner);
	}

	private Player getOwnerPlayer() {
		LivingEntity owner = getOwner();
		return owner instanceof Player player ? player : null;
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.MAX_HEALTH, 240)
				.add(Attributes.ARMOR, 10)
				.add(Attributes.ARMOR_TOUGHNESS, 1)
				.add(Attributes.ATTACK_DAMAGE, 15)
				.add(Attributes.FOLLOW_RANGE, 48)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.65)
				.add(Attributes.FLYING_SPEED, 0.45);
	}
}
