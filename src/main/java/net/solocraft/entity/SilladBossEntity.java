package net.solocraft.entity;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.init.SololevelingModItems;
import net.solocraft.util.ShadowMonarchManager;
import net.solocraft.util.SilladBossCombatManager;

import net.solocraft.network.compat.NetworkHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Developer-preview boss for Sillad, Monarch of Frost.
 *
 * <p>The entity deliberately has no natural placement and no spawn egg. Its
 * server-side admission is enforced by {@code SilladBossSpawnManager}; the
 * combat controller is authoritative and commits every attack to a readable
 * wind-up, impact, and recovery timeline.</p>
 */
public final class SilladBossEntity extends Monster {
	private static final EntityDataAccessor<Integer> ACTION =
			SynchedEntityData.defineId(SilladBossEntity.class,
					EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> ACTION_TICK =
			SynchedEntityData.defineId(SilladBossEntity.class,
					EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> PHASE =
			SynchedEntityData.defineId(SilladBossEntity.class,
					EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SPIRITUALIZED =
			SynchedEntityData.defineId(SilladBossEntity.class,
					EntityDataSerializers.BOOLEAN);

	private final ServerBossEvent bossInfo = new ServerBossEvent(
			Component.translatable("entity.sololeveling.sillad_boss"),
			ServerBossEvent.BossBarColor.BLUE,
			ServerBossEvent.BossBarOverlay.NOTCHED_10);

	private int engagedPlayerCount = 1;
	private boolean absoluteZeroUsed;
	private long phaseThreeStartedAt = -1L;
	private BlockPos encounterHome;

	public SilladBossEntity(EntityType<? extends SilladBossEntity> type,
			Level level) {
		super(type, level);
		xpReward = 250;
		getAttribute(Attributes.STEP_HEIGHT).setBaseValue(1.25F);
		setPersistenceRequired();
		setPathfindingMalus(PathType.WATER, 0.0F);
		setPathfindingMalus(PathType.WATER_BORDER, 0.0F);
		setPathfindingMalus(PathType.POWDER_SNOW, 0.0F);
		ensureFrostSpear();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(ACTION, Action.IDLE.ordinal());
		builder.define(ACTION_TICK, 0);
		builder.define(PHASE, 1);
		builder.define(SPIRITUALIZED, false);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		GroundPathNavigation navigation = new GroundPathNavigation(this, level);
		navigation.setCanOpenDoors(true);
		navigation.setCanPassDoors(true);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		super.registerGoals();
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class,
				24.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(0, new HurtByTargetGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this,
				Player.class, true));
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MOVEMENT_SPEED, 0.40D)
				.add(Attributes.MAX_HEALTH, 900.0D)
				.add(Attributes.ARMOR, 28.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 12.0D)
				.add(Attributes.ATTACK_DAMAGE, 24.0D)
				.add(Attributes.ATTACK_KNOCKBACK, 1.0D)
				.add(Attributes.FOLLOW_RANGE, 64.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
	}

	public static void init() {
		// Developer-preview entities intentionally have no SpawnPlacements entry.
	}

	public Action getCombatAction() {
		int index = entityData.get(ACTION);
		return index >= 0 && index < Action.values().length
				? Action.values()[index] : Action.IDLE;
	}

	public int getActionTick() {
		return entityData.get(ACTION_TICK);
	}

	public boolean isActing() {
		return getCombatAction() != Action.IDLE;
	}

	public void beginCombatAction(Action action) {
		if (level().isClientSide() || action == null)
			return;
		entityData.set(ACTION, action.ordinal());
		entityData.set(ACTION_TICK, 0);
		getNavigation().stop();
		setAggressive(true);
		updateBossBarName();
	}

	public int advanceCombatAction() {
		int next = getActionTick() + 1;
		entityData.set(ACTION_TICK, next);
		return next;
	}

	public void finishCombatAction() {
		entityData.set(ACTION, Action.IDLE.ordinal());
		entityData.set(ACTION_TICK, 0);
		updateBossBarName();
	}

	public int getCombatPhase() {
		return entityData.get(PHASE);
	}

	public void setCombatPhase(int phase) {
		int safe = Math.max(1, Math.min(3, phase));
		entityData.set(PHASE, safe);
		if (getAttribute(Attributes.MOVEMENT_SPEED) != null)
			getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(
					safe >= 3 ? 0.46D : safe == 2 ? 0.43D : 0.40D);
		if (safe >= 3) {
			entityData.set(SPIRITUALIZED, true);
			if (phaseThreeStartedAt < 0L)
				phaseThreeStartedAt = level().getGameTime();
		}
		updateBossBarName();
	}

	public boolean isSpiritualized() {
		return entityData.get(SPIRITUALIZED);
	}

	public int getEngagedPlayerCount() {
		return engagedPlayerCount;
	}

	public void lockEngagedPlayerCount(int count) {
		engagedPlayerCount = Math.max(engagedPlayerCount,
				Math.max(1, Math.min(4, count)));
	}

	public boolean hasUsedAbsoluteZero() {
		return absoluteZeroUsed;
	}

	public void markAbsoluteZeroUsed() {
		absoluteZeroUsed = true;
	}

	public long getPhaseThreeStartedAt() {
		return phaseThreeStartedAt;
	}

	public BlockPos getEncounterHome() {
		if (encounterHome == null)
			encounterHome = blockPosition();
		return encounterHome;
	}

	public void setEncounterHome(BlockPos home) {
		if (home != null)
			encounterHome = home.immutable();
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		if (target instanceof Player player)
			return player.isAlive() && !player.isCreative()
					&& !player.isSpectator() && super.canAttack(target);
		boolean ownedCompanion = ShadowMonarchManager
				.getShadowOwnerUUID(target) != null;
		boolean attackingSillad = target instanceof Mob mob
				&& mob.getTarget() == this;
		boolean retaliating = target == getLastHurtByMob();
		return (ownedCompanion || attackingSillad || retaliating)
				&& target.isAlive() && super.canAttack(target);
	}

	@Override
	public boolean doHurtTarget(Entity target) {
		// Every impact is owned by the combat timeline, never vanilla melee AI.
		return false;
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (source == null)
			return false;
		if (source.getEntity() == this || source.getDirectEntity() == this
				|| source.is(DamageTypes.FREEZE))
			return false;
		float adjusted = amount;
		if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			adjusted = SilladBossCombatManager.modifyIncomingDamage(this,
					source, amount);
		if (adjusted <= 0.0F)
			return false;
		boolean hurt = super.hurt(source, adjusted);
		if (hurt && !level().isClientSide())
			SilladBossCombatManager.recordIncomingDamage(this, source,
					adjusted);
		return hurt;
	}

	@Override
	public boolean canFreeze() {
		return false;
	}

	@Override
	public boolean canChangeDimensions(Level fromLevel, Level toLevel) {
		// Dimension transfer would be an unticketed fresh entity join.
		return false;
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected boolean shouldDespawnInPeaceful() {
		return false;
	}

	@Override
	protected void customServerAiStep() {
		super.customServerAiStep();
		SilladBossCombatManager.tick(this);
		bossInfo.setProgress(Math.max(0.0F,
				getHealth() / Math.max(1.0F, getMaxHealth())));
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		bossInfo.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		bossInfo.removePlayer(player);
	}

	@Override
	public void die(DamageSource source) {
		SilladBossCombatManager.cleanup(this);
		bossInfo.removeAllPlayers();
		super.die(source);
	}

	@Override
	public void remove(RemovalReason reason) {
		SilladBossCombatManager.cleanup(this);
		bossInfo.removeAllPlayers();
		super.remove(reason);
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.STRAY_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.STRAY_DEATH;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.STRAY_AMBIENT;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		playSound(SoundEvents.POWDER_SNOW_STEP, 0.35F,
				0.72F + random.nextFloat() * 0.12F);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt("SilladPhase", getCombatPhase());
		tag.putInt("SilladEngagedPlayers", engagedPlayerCount);
		tag.putBoolean("SilladAbsoluteZeroUsed", absoluteZeroUsed);
		tag.putLong("SilladPhaseThreeStarted", phaseThreeStartedAt);
		if (encounterHome != null)
			tag.putLong("SilladEncounterHome", encounterHome.asLong());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setCombatPhase(tag.contains("SilladPhase")
				? tag.getInt("SilladPhase") : 1);
		engagedPlayerCount = Math.max(1,
				Math.min(4, tag.getInt("SilladEngagedPlayers")));
		absoluteZeroUsed = tag.getBoolean("SilladAbsoluteZeroUsed");
		phaseThreeStartedAt = tag.contains("SilladPhaseThreeStarted")
				? tag.getLong("SilladPhaseThreeStarted") : -1L;
		if (tag.contains("SilladEncounterHome"))
			encounterHome = BlockPos.of(tag.getLong("SilladEncounterHome"));
		// Never resume an impact frame after chunk reload or server restart.
		finishCombatAction();
		ensureFrostSpear();
	}

	private void ensureFrostSpear() {
		if (!getMainHandItem().is(SololevelingModItems.ICE_SPEAR.get()))
			setItemSlot(EquipmentSlot.MAINHAND,
					new ItemStack(SololevelingModItems.ICE_SPEAR.get()));
		setDropChance(EquipmentSlot.MAINHAND, 0.0F);
	}

	private void updateBossBarName() {
		if (level().isClientSide())
			return;
		Component name = hasCustomName() ? getCustomName()
				: Component.translatable("entity.sololeveling.sillad_boss");
		if (getCombatAction() != Action.IDLE)
			name = name.copy().append(Component.literal("  |  "))
					.append(Component.translatable(getCombatAction().translationKey));
		bossInfo.setName(name);
	}

	@Override
	public void setCustomName(@Nullable Component name) {
		super.setCustomName(name);
		updateBossBarName();
	}

	public enum Action {
		IDLE("idle"),
		PHASE_TRANSITION("phase_transition"),
		FROST_CLEAVE("frost_cleave"),
		ICE_SPEAR("ice_spear"),
		FLASH_FREEZE("flash_freeze"),
		FROZEN_PATH("frozen_path"),
		FROST_COUNTER("frost_counter"),
		STILLNESS_DECREE("stillness_decree"),
		SPIRE_CAGE("spire_cage"),
		WHITEOUT_PROCESSION("whiteout_procession"),
		WINTER_REMEMBERS("winter_remembers"),
		CROWN_OF_WINTER("crown_of_winter"),
		ABSOLUTE_ZERO("absolute_zero"),
		GLACIAL_EXECUTION("glacial_execution"),
		FROST_STEP("frost_step");

		private final String serializedName;
		private final String translationKey;

		Action(String serializedName) {
			this.serializedName = serializedName;
			this.translationKey = "entity.sololeveling.sillad_boss.action."
					+ serializedName;
		}

		public String serializedName() {
			return serializedName;
		}

		public static Action byName(String value) {
			if (value == null)
				return IDLE;
			String normalized = value.trim().toLowerCase(Locale.ROOT);
			for (Action action : values())
				if (action.serializedName.equals(normalized))
					return action;
			return IDLE;
		}
	}
}
