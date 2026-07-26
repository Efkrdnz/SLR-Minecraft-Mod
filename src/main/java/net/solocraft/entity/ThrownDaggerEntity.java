package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.DaggerThrowManager;
import net.solocraft.util.EntityHighlightSystem;
import net.solocraft.util.RulersAuthorityManager;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** A visible spinning dagger. Physical instances carry an escrow token; Rush instances are spectral copies. */
public class ThrownDaggerEntity extends Projectile {
	private static final String OWNER_GLOW_SOURCE = "dagger:owner";
	private static final int OWNER_GLOW_COLOR = 0x7FE8FF;
	private static final int OWNER_GLOW_DURATION_TICKS = 18;
	private static final int OWNER_GLOW_PRIORITY = EntityHighlightSystem.PRIORITY_DUNGEON_BOSS + 80;
	private static final EntityDataAccessor<ItemStack> ITEM = SynchedEntityData.defineId(ThrownDaggerEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Optional<UUID>> OWNER_ID = SynchedEntityData.defineId(ThrownDaggerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Optional<UUID>> TOKEN = SynchedEntityData.defineId(ThrownDaggerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
	private static final EntityDataAccessor<Boolean> SPECTRAL = SynchedEntityData.defineId(ThrownDaggerEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> RETURNING = SynchedEntityData.defineId(ThrownDaggerEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(ThrownDaggerEntity.class, EntityDataSerializers.INT);
	private final Map<UUID, Integer> hitTicks = new HashMap<>();
	private long rulerControlTick = Long.MIN_VALUE;
	private boolean recoveredDiscard;

	public ThrownDaggerEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.THROWN_DAGGER.get(), level);
	}

	public ThrownDaggerEntity(EntityType<? extends ThrownDaggerEntity> type, Level level) {
		super(type, level);
		this.noPhysics = true;
	}

	@Override
	protected void defineSynchedData() {
		this.entityData.define(ITEM, ItemStack.EMPTY);
		this.entityData.define(OWNER_ID, Optional.empty());
		this.entityData.define(TOKEN, Optional.empty());
		this.entityData.define(SPECTRAL, false);
		this.entityData.define(RETURNING, false);
		this.entityData.define(DELAY, 0);
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	public static ThrownDaggerEntity createPhysical(ServerPlayer owner, ItemStack item, UUID token, Vec3 origin, Vec3 velocity) {
		ThrownDaggerEntity dagger = base(owner, item, origin, velocity);
		dagger.entityData.set(TOKEN, Optional.of(token));
		return dagger;
	}

	public static ThrownDaggerEntity createSpectral(ServerPlayer owner, ItemStack item, Vec3 origin, Vec3 velocity, int delay) {
		ThrownDaggerEntity dagger = base(owner, item, origin, velocity);
		dagger.entityData.set(SPECTRAL, true);
		dagger.entityData.set(DELAY, Math.max(0, delay));
		return dagger;
	}

	private static ThrownDaggerEntity base(ServerPlayer owner, ItemStack item, Vec3 origin, Vec3 velocity) {
		ThrownDaggerEntity dagger = new ThrownDaggerEntity(SololevelingModEntities.THROWN_DAGGER.get(), owner.level());
		ItemStack visual = item.copy();
		visual.setCount(1);
		dagger.entityData.set(ITEM, visual);
		dagger.entityData.set(OWNER_ID, Optional.of(owner.getUUID()));
		dagger.setOwner(owner);
		dagger.moveTo(origin.x, origin.y, origin.z, owner.getYRot(), owner.getXRot());
		dagger.setDeltaMovement(velocity);
		return dagger;
	}

	public ItemStack getDaggerStack() {
		return this.entityData.get(ITEM);
	}

	public boolean isPhysical() {
		return !this.entityData.get(SPECTRAL);
	}

	public boolean isSpectral() {
		return this.entityData.get(SPECTRAL);
	}

	public boolean isReturning() {
		return this.entityData.get(RETURNING);
	}

	public UUID getEscrowToken() {
		return this.entityData.get(TOKEN).orElse(null);
	}

	public UUID getOwnerId() {
		return this.entityData.get(OWNER_ID).orElse(null);
	}

	public boolean isOwnedBy(ServerPlayer player) {
		return player != null && player.getUUID().equals(getOwnerId());
	}

	public boolean beginReturn() {
		if (isPhysical() && this.level() instanceof ServerLevel level
				&& !RulersAuthorityManager.hasAuthority(owner(level)))
			return false;
		this.entityData.set(RETURNING, true);
		this.setNoGravity(true);
		return true;
	}

	public void markRulersControlled(long gameTime) {
		this.rulerControlTick = gameTime + 6L;
		this.entityData.set(RETURNING, false);
	}

	public void onRulersReleased() {
		this.rulerControlTick = this.level().getGameTime();
	}

	public void discardAsRecovered() {
		this.recoveredDiscard = true;
		this.discard();
	}

	@Override
	public boolean isPickable() {
		return true;
	}

	@Override
	public float getPickRadius() {
		return 0.65F;
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide && this.level() instanceof ServerLevel level) {
			ServerPlayer owner = owner(level);
			if (owner == null || !owner.isAlive()) {
				if (isSpectral() || this.tickCount > 200)
					this.discard();
				return;
			}
			refreshOwnerGlow(owner);
			if (isPhysical()) {
				DaggerThrowManager.register(this);
				if (!DaggerThrowManager.isAuthorized(owner, getEscrowToken())) {
					this.discard();
					return;
				}
			}
			if (this.tickCount <= this.entityData.get(DELAY))
				return;
			boolean controlled = rulerControlTick >= level.getGameTime();
			if (isPhysical() && isReturning() && !RulersAuthorityManager.hasAuthority(owner)) {
				this.entityData.set(RETURNING, false);
				this.setNoGravity(false);
			}
			if (isReturning() && !controlled) {
				Vec3 destination = owner.getEyePosition().add(0.0D, -0.25D, 0.0D);
				Vec3 toOwner = destination.subtract(this.position());
				if (toOwner.lengthSqr() <= 2.25D) {
					this.setDeltaMovement(Vec3.ZERO);
					if (isSpectral()) {
						this.discard();
					} else if (this.tickCount % 20 == 0 && DaggerThrowManager.completeReturn(owner, getEscrowToken(), this)) {
						this.discard();
					}
					return;
				}
				this.setDeltaMovement(toOwner.normalize().scale(isSpectral() ? 3.0D : 2.5D));
			}

			Vec3 start = this.position();
			Vec3 end = start.add(this.getDeltaMovement());
			BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
			// A recalled dagger is magical and may phase back through the wall it embedded in.
			Vec3 travelEnd = !isReturning() && blockHit.getType() == HitResult.Type.BLOCK ? blockHit.getLocation() : end;
			hitTargets(level, owner, start, travelEnd, controlled);
			if (blockHit.getType() == HitResult.Type.BLOCK && !isReturning()) {
				this.setPos(travelEnd);
				this.setDeltaMovement(Vec3.ZERO);
				if (isSpectral())
					beginReturn();
				else if (!controlled && this.tickCount > 80)
					beginReturn();
				return;
			}
			this.setPos(travelEnd);
			if (!controlled && !isReturning())
				this.setDeltaMovement(this.getDeltaMovement().scale(0.995D).add(0.0D, isSpectral() ? 0.0D : -0.012D, 0.0D));
			if (!controlled && ((isSpectral() && this.tickCount > 30) || (isPhysical() && this.tickCount > 90)))
				beginReturn();
			if (this.tickCount > (isSpectral() ? 90 : 600)) {
				if (isSpectral())
					this.discard();
				else if (!beginReturn())
					this.discard();
			}
		} else if (this.tickCount > this.entityData.get(DELAY)) {
			this.setPos(this.getX() + this.getDeltaMovement().x, this.getY() + this.getDeltaMovement().y,
					this.getZ() + this.getDeltaMovement().z);
		}
	}

	private void hitTargets(ServerLevel level, ServerPlayer owner, Vec3 start, Vec3 end, boolean controlled) {
		AABB path = new AABB(start, end).inflate(isSpectral() ? 0.42D : 0.5D);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, path,
				candidate -> validTarget(owner, candidate))) {
			int lastHit = hitTicks.getOrDefault(target.getUUID(), Integer.MIN_VALUE / 2);
			if (this.tickCount - lastHit < (controlled ? 10 : 1000))
				continue;
			hitTicks.put(target.getUUID(), this.tickCount);
			DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
					.getHolderOrThrow(ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation("sololeveling:assassin"))), this, owner);
			float damage = isSpectral() ? DaggerThrowManager.rushDamage(owner) : DaggerThrowManager.physicalDamage(owner);
			if (target.hurt(source, damage)) {
				target.invulnerableTime = 0;
				level.playSound(null, BlockPos.containing(target.position()), SoundEvents.PLAYER_ATTACK_CRIT,
						SoundSource.PLAYERS, 0.7F, isSpectral() ? 1.45F : 1.1F);
				if (isPhysical()) {
					ItemStack dagger = getDaggerStack().copy();
					dagger.hurtAndBreak(1, owner, broken -> { });
					this.entityData.set(ITEM, dagger);
					DaggerThrowManager.updateEscrowItem(owner, getEscrowToken(), dagger);
					if (dagger.isEmpty()) {
						this.discard();
						return;
					}
				}
			}
			if (!controlled)
				beginReturn();
		}
	}

	private boolean validTarget(ServerPlayer owner, LivingEntity candidate) {
		if (candidate == owner || !candidate.isAlive() || candidate.isSpectator())
			return false;
		if (candidate instanceof TamableAnimal tame && tame.isOwnedBy(owner))
			return false;
		return !(candidate instanceof ServerPlayer other) || owner.canHarmPlayer(other);
	}

	private ServerPlayer owner(ServerLevel level) {
		if (level == null)
			return null;
		UUID id = getOwnerId();
		ServerPlayer owner = id == null ? null : level.getServer().getPlayerList().getPlayer(id);
		return owner != null && owner.level() == this.level() ? owner : null;
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag) {
		this.entityData.set(ITEM, ItemStack.of(tag.getCompound("Item")));
		if (tag.hasUUID("Owner")) {
			this.entityData.set(OWNER_ID, Optional.of(tag.getUUID("Owner")));
		}
		this.entityData.set(TOKEN, tag.hasUUID("Token") ? Optional.of(tag.getUUID("Token")) : Optional.empty());
		this.entityData.set(SPECTRAL, tag.getBoolean("Spectral"));
		this.entityData.set(RETURNING, tag.getBoolean("Returning"));
		this.entityData.set(DELAY, tag.getInt("Delay"));
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag) {
		tag.put("Item", getDaggerStack().save(new CompoundTag()));
		UUID owner = getOwnerId();
		if (owner != null)
			tag.putUUID("Owner", owner);
		UUID token = getEscrowToken();
		if (token != null)
			tag.putUUID("Token", token);
		tag.putBoolean("Spectral", isSpectral());
		tag.putBoolean("Returning", isReturning());
		tag.putInt("Delay", this.entityData.get(DELAY));
	}

	@Override
	public void remove(RemovalReason reason) {
		if (!this.level().isClientSide && !recoveredDiscard)
			DaggerThrowManager.unregister(this);
		if (!this.level().isClientSide && this.level() instanceof ServerLevel level) {
			ServerPlayer owner = owner(level);
			if (owner != null)
				EntityHighlightSystem.hide(owner, this, OWNER_GLOW_SOURCE);
		}
		super.remove(reason);
	}

	private void refreshOwnerGlow(ServerPlayer owner) {
		if (owner != null && this.tickCount % 8 == 0)
			EntityHighlightSystem.show(owner, this, OWNER_GLOW_SOURCE, OWNER_GLOW_COLOR,
					OWNER_GLOW_DURATION_TICKS, OWNER_GLOW_PRIORITY);
	}
}
