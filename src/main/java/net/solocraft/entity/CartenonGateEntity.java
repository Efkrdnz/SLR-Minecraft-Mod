package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.CartenonTempleManager;

import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** A persistent, access-controlled hidden gate leading to one Cartenon instance. */
public class CartenonGateEntity extends PathfinderMob implements GeoEntity {
	private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
	private final Set<UUID> allowedPlayers = new LinkedHashSet<>();
	private UUID ownerId;
	private int instanceId;

	public CartenonGateEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.CARTENON_GATE.get(), level);
	}

	public CartenonGateEntity(EntityType<CartenonGateEntity> type, Level level) {
		super(type, level);
		setNoAi(true);
		setNoGravity(true);
		setInvulnerable(true);
		setPersistenceRequired();
		xpReward = 0;
	}

	public void configure(UUID ownerId, Collection<UUID> allowedPlayers, int instanceId) {
		this.ownerId = ownerId;
		this.instanceId = Math.max(1, instanceId);
		this.allowedPlayers.clear();
		if (allowedPlayers != null)
			this.allowedPlayers.addAll(allowedPlayers);
		if (ownerId != null)
			this.allowedPlayers.add(ownerId);
	}

	public boolean isAllowed(UUID playerId) {
		return playerId != null && allowedPlayers.contains(playerId);
	}

	public int getInstanceId() {
		return instanceId;
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public InteractionResult mobInteract(net.minecraft.world.entity.player.Player player, InteractionHand hand) {
		if (!level().isClientSide() && player instanceof ServerPlayer serverPlayer)
			CartenonTempleManager.enterGate(serverPlayer, this);
		return InteractionResult.sidedSuccess(level().isClientSide());
	}

	@Override
	public void tick() {
		super.tick();
		setDeltaMovement(Vec3.ZERO);
		fallDistance = 0.0F;
		setNoGravity(true);
		if (level() instanceof ServerLevel serverLevel && tickCount % 4 == 0) {
			double phase = tickCount * 0.11D;
			serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.35D, getZ(),
					4, 0.58D, 1.0D, 0.18D, 0.02D);
			serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
					getX() + Math.sin(phase) * 0.56D, getY() + 0.35D + (tickCount % 28) * 0.065D,
					getZ() + Math.cos(phase) * 0.16D, 1, 0.02D, 0.03D, 0.02D, 0.0D);
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	protected void doPush(Entity entity) {
	}

	@Override
	protected void pushEntities() {
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		if (ownerId != null)
			tag.putUUID("Owner", ownerId);
		tag.putInt("CartenonInstance", instanceId);
		ListTag allowed = new ListTag();
		for (UUID playerId : allowedPlayers) {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("Player", playerId);
			allowed.add(entry);
		}
		tag.put("AllowedPlayers", allowed);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		ownerId = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
		instanceId = Math.max(1, tag.getInt("CartenonInstance"));
		allowedPlayers.clear();
		ListTag allowed = tag.getList("AllowedPlayers", Tag.TAG_COMPOUND);
		for (int i = 0; i < allowed.size(); i++) {
			CompoundTag entry = allowed.getCompound(i);
			if (entry.hasUUID("Player"))
				allowedPlayers.add(entry.getUUID("Player"));
		}
		if (ownerId != null)
			allowedPlayers.add(ownerId);
	}

	public static void init() {
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 1.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.0D)
				.add(Attributes.FOLLOW_RANGE, 1.0D);
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
		controllers.add(new AnimationController<>(this, "idle", 0,
				state -> state.setAndContinue(RawAnimation.begin().thenLoop("idle"))));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return animationCache;
	}
}
