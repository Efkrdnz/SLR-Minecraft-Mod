
package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.MageCombatHelper;

import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.network.PlayMessages;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

@OnlyIn(value = Dist.CLIENT, _interface = ItemSupplier.class)
public class ManaArrowEntity extends AbstractArrow implements ItemSupplier {
	public static final ItemStack PROJECTILE_ITEM = new ItemStack(Blocks.AIR);
	private static final EntityDataAccessor<Integer> RANGER_STAGE =
			SynchedEntityData.defineId(ManaArrowEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> ORDINARY_RANGER_ARROW =
			SynchedEntityData.defineId(ManaArrowEntity.class, EntityDataSerializers.BOOLEAN);
	private UUID rangerTargetId;
	private double rangerInitialDistance;
	private double rangerTravelled;
	private boolean rangerGuidanceActive;

	public ManaArrowEntity(PlayMessages.SpawnEntity packet, Level world) {
		super(SololevelingModEntities.MANA_ARROW.get(), world);
		this.pickup = Pickup.DISALLOWED;
	}

	public ManaArrowEntity(EntityType<? extends ManaArrowEntity> type, Level world) {
		super(type, world);
		this.pickup = Pickup.DISALLOWED;
	}

	public ManaArrowEntity(EntityType<? extends ManaArrowEntity> type, double x, double y, double z, Level world) {
		super(type, x, y, z, world);
		this.pickup = Pickup.DISALLOWED;
	}

	public ManaArrowEntity(EntityType<? extends ManaArrowEntity> type, LivingEntity entity, Level world) {
		super(type, entity, world);
		this.pickup = Pickup.DISALLOWED;
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		this.entityData.define(RANGER_STAGE, 0);
		this.entityData.define(ORDINARY_RANGER_ARROW, false);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public ItemStack getItem() {
		return PROJECTILE_ITEM;
	}

	@Override
	protected ItemStack getPickupItem() {
		return PROJECTILE_ITEM;
	}

	@Override
	protected void doPostHurtEffects(LivingEntity entity) {
		super.doPostHurtEffects(entity);
		entity.setArrowCount(entity.getArrowCount() - 1);
	}

	@Override
	public void tick() {
		if (!this.level().isClientSide() && getRangerStage() == 3)
			tickSeekingArc();
		if (getRangerStage() == 2)
			this.setNoGravity(true);

		Vec3 before = this.position();
		super.tick();
		this.rangerTravelled += before.distanceTo(this.position());

		if (this.inGround)
			this.discard();
		else if (this.tickCount > 160)
			this.discard();
	}

	/**
	 * Retains vanilla critical-hit damage on the server without emitting the
	 * inherited critical-arrow trail on clients.
	 */
	@Override
	public boolean isCritArrow() {
		return !this.level().isClientSide() && super.isCritArrow();
	}

	@Override
	public boolean displayFireAnimation() {
		return false;
	}

	public void configureRangerShot(int stage, UUID targetId, double initialDistance, boolean ordinary) {
		int clampedStage = Math.max(0, Math.min(3, stage));
		this.entityData.set(RANGER_STAGE, clampedStage);
		this.entityData.set(ORDINARY_RANGER_ARROW, ordinary);
		this.rangerTargetId = targetId;
		this.rangerInitialDistance = Math.max(0.0D, initialDistance);
		this.rangerTravelled = 0.0D;
		this.rangerGuidanceActive = false;
		this.pickup = Pickup.DISALLOWED;
		this.setNoGravity(clampedStage == 2);
	}

	public int getRangerStage() {
		return this.entityData.get(RANGER_STAGE);
	}

	public boolean isOrdinaryRangerArrow() {
		return this.entityData.get(ORDINARY_RANGER_ARROW);
	}

	private void tickSeekingArc() {
		if (!(this.level() instanceof ServerLevel serverLevel) || this.rangerTargetId == null)
			return;
		Entity rawTarget = serverLevel.getEntity(this.rangerTargetId);
		if (!(rawTarget instanceof LivingEntity target) || !target.isAlive()
				|| target.level() != this.level() || !MageCombatHelper.isValidTarget(this.getOwner(), target)) {
			this.rangerTargetId = null;
			return;
		}

		Vec3 current = this.getDeltaMovement();
		Vec3 targetCenter = target.getBoundingBox().getCenter();
		double targetDistance = targetCenter.distanceTo(this.position());
		if (!this.rangerGuidanceActive) {
			double activationDistance = Math.max(2.0D, this.rangerInitialDistance * 0.08D);
			boolean closeTarget = targetDistance <= 18.0D;
			boolean naturalApex = this.tickCount > 0 && current.y <= 0.0D;
			boolean launchTravelled = this.rangerTravelled >= activationDistance;
			if (!closeTarget && !naturalApex && !launchTravelled && this.tickCount < 2) {
				this.setNoGravity(false);
				return;
			}
			this.rangerGuidanceActive = true;
		}

		// Once guidance begins, gravity must not drag the arrow below a target while it
		// turns. A short ballistic launch is retained above for the Seeking visual.
		this.setNoGravity(true);
		double speed = Mth.clamp(current.length(), 1.6D, 4.5D);
		double leadTicks = Mth.clamp(targetDistance / Math.max(1.0D, speed), 0.0D, 5.0D);
		double leadScale = targetDistance < 10.0D ? 0.30D : 0.75D;
		Vec3 aim = targetCenter.add(target.getDeltaMovement().scale(leadTicks * leadScale));
		if (isGuidancePathBlocked(aim)) {
			if (!isGuidancePathBlocked(targetCenter))
				aim = targetCenter;
			else {
				Vec3 targetEyes = target.getEyePosition();
				if (isGuidancePathBlocked(targetEyes))
					return;
				aim = targetEyes;
			}
		}

		Vec3 desired = aim.subtract(this.position());
		if (desired.lengthSqr() < 0.0001D)
			return;
		desired = desired.normalize();
		Vec3 currentDirection = current.lengthSqr() > 0.0001D ? current.normalize() : desired;
		double turn = target instanceof Player ? 0.18D : 0.28D;
		if (targetDistance < 12.0D)
			turn += 0.14D;
		if (targetDistance < 6.0D)
			turn += 0.18D;
		if (currentDirection.dot(desired) < 0.65D)
			turn += 0.12D;
		turn = Mth.clamp(turn, 0.0D, 0.68D);

		double guidedSpeed = speed;
		if (targetDistance < 6.0D)
			guidedSpeed = Math.max(1.6D, Math.min(speed, targetDistance * 0.55D + 0.8D));
		Vec3 steered = rotateToward(currentDirection, desired, turn);
		if (steered.lengthSqr() > 0.0001D) {
			this.setDeltaMovement(steered.normalize().scale(guidedSpeed));
			this.hasImpulse = true;
		}
	}

	private boolean isGuidancePathBlocked(Vec3 aim) {
		BlockHitResult obstruction = this.level().clip(new ClipContext(this.position(), aim,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return obstruction.getType() == HitResult.Type.BLOCK
				&& obstruction.getLocation().distanceToSqr(this.position()) + 0.25D
						< aim.distanceToSqr(this.position());
	}

	private static Vec3 rotateToward(Vec3 current, Vec3 desired, double turnFraction) {
		double dot = Mth.clamp(current.dot(desired), -1.0D, 1.0D);
		if (dot > 0.9999D)
			return desired;

		double angle = Math.acos(dot);
		double turnAngle = Math.min(angle,
				Math.max(Math.toRadians(4.0D), angle * Mth.clamp(turnFraction, 0.0D, 1.0D)));
		Vec3 axis = current.cross(desired);
		if (axis.lengthSqr() < 0.000001D) {
			Vec3 reference = Math.abs(current.y) < 0.9D
					? new Vec3(0.0D, 1.0D, 0.0D)
					: new Vec3(1.0D, 0.0D, 0.0D);
			axis = current.cross(reference);
		}
		axis = axis.normalize();
		return current.scale(Math.cos(turnAngle))
				.add(axis.cross(current).scale(Math.sin(turnAngle)))
				.normalize();
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		Entity owner = this.getOwner();
		if (getRangerStage() > 0 && owner == null)
			return false;
		if (owner != null && entity instanceof LivingEntity)
			return MageCombatHelper.isValidTarget(owner, entity) && super.canHitEntity(entity);
		return super.canHitEntity(entity);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("RangerStage", getRangerStage());
		tag.putBoolean("OrdinaryRangerArrow", isOrdinaryRangerArrow());
		tag.putDouble("RangerInitialDistance", this.rangerInitialDistance);
		tag.putDouble("RangerTravelled", this.rangerTravelled);
		tag.putBoolean("RangerGuidanceActive", this.rangerGuidanceActive);
		if (this.rangerTargetId != null)
			tag.putUUID("RangerTarget", this.rangerTargetId);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		this.entityData.set(RANGER_STAGE, Math.max(0, Math.min(3, tag.getInt("RangerStage"))));
		this.entityData.set(ORDINARY_RANGER_ARROW, tag.getBoolean("OrdinaryRangerArrow"));
		this.rangerInitialDistance = Math.max(0.0D, tag.getDouble("RangerInitialDistance"));
		this.rangerTravelled = Math.max(0.0D, tag.getDouble("RangerTravelled"));
		this.rangerGuidanceActive = tag.getBoolean("RangerGuidanceActive");
		this.rangerTargetId = tag.hasUUID("RangerTarget") ? tag.getUUID("RangerTarget") : null;
		this.pickup = Pickup.DISALLOWED;
		this.setNoGravity(getRangerStage() == 2 || (getRangerStage() == 3 && this.rangerGuidanceActive));
	}

	public static ManaArrowEntity shoot(Level world, LivingEntity entity, RandomSource source) {
		return shoot(world, entity, source, 2f, 3, 1);
	}

	public static ManaArrowEntity shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
		ManaArrowEntity entityarrow = new ManaArrowEntity(SololevelingModEntities.MANA_ARROW.get(), entity, world);
		entityarrow.shoot(entity.getViewVector(1).x, entity.getViewVector(1).y, entity.getViewVector(1).z, power * 2, 0);
		entityarrow.setSilent(true);
		entityarrow.setCritArrow(false);
		entityarrow.setBaseDamage(damage);
		entityarrow.setKnockback(knockback);
		world.addFreshEntity(entityarrow);
		world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.amethyst_block.break")), SoundSource.PLAYERS, 1, 1f / (random.nextFloat() * 0.5f + 1) + (power / 2));
		return entityarrow;
	}

	public static ManaArrowEntity shoot(LivingEntity entity, LivingEntity target) {
		ManaArrowEntity entityarrow = new ManaArrowEntity(SololevelingModEntities.MANA_ARROW.get(), entity, entity.level());
		double dx = target.getX() - entity.getX();
		double dy = target.getY() + target.getEyeHeight() - 1.1;
		double dz = target.getZ() - entity.getZ();
		entityarrow.shoot(dx, dy - entityarrow.getY() + Math.hypot(dx, dz) * 0.2F, dz, 2f * 2, 12.0F);
		entityarrow.setSilent(true);
		entityarrow.setBaseDamage(3);
		entityarrow.setKnockback(1);
		entityarrow.setCritArrow(false);
		entity.level().addFreshEntity(entityarrow);
		entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("block.amethyst_block.break")), SoundSource.PLAYERS, 1,
				1f / (RandomSource.create().nextFloat() * 0.5f + 1));
		return entityarrow;
	}
}
