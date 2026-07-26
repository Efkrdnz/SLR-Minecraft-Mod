package net.solocraft.entity;

import net.solocraft.dkc.event.EsilPermitClaimEvent;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PlayMessages;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * House Radiru's heir. Encounter ownership and outcome live on the entity so a
 * chunk unload cannot reset a surrender or allow its Entry Permit to be claimed
 * twice. Run progression itself is deliberately handled by the DKC manager via
 * {@link EsilPermitClaimEvent}.
 */
public class EsilRadiruEntity extends PathfinderMob {
	private static final String NBT_STATE = "RadiruState";
	private static final String NBT_OWNER = "RadiruOwner";
	private static final String NBT_PERMIT_CLAIMED = "RadiruPermitClaimed";
	private static final String NBT_PACT_DIALOGUE_INDEX = "radiru_esil_pact_dialogue_index";
	private static final String NBT_CASTLE_DIALOGUE_INDEX = "radiru_esil_castle_dialogue_index";
	private static final String NBT_DIALOGUE_AFTER = "radiru_esil_dialogue_after";
	private static final String[] PACT_DIALOGUE = {
			"dialogue.sololeveling.esil.pact.0",
			"dialogue.sololeveling.esil.pact.1",
			"dialogue.sololeveling.esil.pact.2",
			"dialogue.sololeveling.esil.pact.3",
			"dialogue.sololeveling.esil.pact.4",
			"dialogue.sololeveling.esil.pact.5"
	};
	private static final String[] CASTLE_DIALOGUE = {
			"dialogue.sololeveling.esil.castle.0",
			"dialogue.sololeveling.esil.castle.1",
			"dialogue.sololeveling.esil.castle.2",
			"dialogue.sololeveling.esil.castle.3",
			"dialogue.sololeveling.esil.castle.4",
			"dialogue.sololeveling.esil.castle.5"
	};

	private static final EntityDataAccessor<Integer> DATA_STATE = SynchedEntityData.defineId(
			EsilRadiruEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_PERMIT_CLAIMED = SynchedEntityData.defineId(
			EsilRadiruEntity.class, EntityDataSerializers.BOOLEAN);

	private UUID encounterOwner;

	public EsilRadiruEntity(PlayMessages.SpawnEntity packet, Level level) {
		this(SololevelingModEntities.ESIL_RADIRU.get(), level);
	}

	public EsilRadiruEntity(EntityType<? extends EsilRadiruEntity> type, Level level) {
		super(type, level);
		setMaxUpStep(0.6F);
		xpReward = 0; // Route rewards are owner-scoped by the Floor 15 encounter.
		setPersistenceRequired();
		setCustomName(Component.translatable("entity.sololeveling.esil_radiru")
				.withStyle(ChatFormatting.LIGHT_PURPLE));
		setCustomNameVisible(true);
	}

	@Override
	protected void defineSynchedData() {
		super.defineSynchedData();
		entityData.define(DATA_STATE, EncounterState.SURRENDERED.id());
		entityData.define(DATA_PERMIT_CLAIMED, false);
	}

	@Override
	protected void registerGoals() {
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false) {
			@Override
			public boolean canUse() {
				return isHostile() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return isHostile() && super.canContinueToUse();
			}

			@Override
			protected double getAttackReachSqr(LivingEntity target) {
				return mob.getBbWidth() * mob.getBbWidth() + target.getBbWidth();
			}
		});
		goalSelector.addGoal(5, new RandomStrollGoal(this, 0.8D) {
			@Override
			public boolean canUse() {
				return isHostile() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return isHostile() && super.canContinueToUse();
			}
		});
		goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, true) {
			@Override
			public boolean canUse() {
				return isHostile() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return isHostile() && super.canContinueToUse();
			}
		});
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		boolean initialClaim = getEncounterState() == EncounterState.SURRENDERED && !isPermitClaimed();
		boolean replacementClaim = getEncounterState() == EncounterState.SANCTUARY && isPermitClaimed();
		if (hand != InteractionHand.MAIN_HAND || !initialClaim && !replacementClaim)
			return super.mobInteract(player, hand);

		// Let the client animate the interaction, but make every decision and state
		// mutation on the authoritative server thread.
		if (level().isClientSide())
			return InteractionResult.SUCCESS;
		if (!(player instanceof ServerPlayer serverPlayer))
			return InteractionResult.PASS;

		EsilPermitClaimEvent event = new EsilPermitClaimEvent(this, serverPlayer);
		boolean canceled = MinecraftForge.EVENT_BUS.post(event);
		if (!canceled && event.decision() == EsilPermitClaimEvent.Decision.GRANT) {
			markPermitClaimed();
			return InteractionResult.CONSUME;
		}
		if (canceled || event.decision() == EsilPermitClaimEvent.Decision.DENY)
			return InteractionResult.CONSUME;
		if (isSanctuaryResident() && isPermitClaimed()) {
			showSanctuaryDialogue(serverPlayer);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	private void showSanctuaryDialogue(ServerPlayer player) {
		long now = level().getGameTime();
		CompoundTag playerData = player.getPersistentData();
		if (now < playerData.getLong(NBT_DIALOGUE_AFTER))
			return;

		SololevelingModVariables.PlayerVariables vars = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		boolean castleConquered = vars.radiru_side_quest_unlocked;
		String[] dialogue = castleConquered ? CASTLE_DIALOGUE : PACT_DIALOGUE;
		String indexTag = castleConquered ? NBT_CASTLE_DIALOGUE_INDEX : NBT_PACT_DIALOGUE_INDEX;
		int index = Math.floorMod(playerData.getInt(indexTag), dialogue.length);
		playerData.putInt(indexTag, index + 1);
		playerData.putLong(NBT_DIALOGUE_AFTER, now + 12L);

		getLookControl().setLookAt(player, 30.0F, 30.0F);
		player.displayClientMessage(Component.translatable("dialogue.sololeveling.esil.speech",
				getDisplayName(), Component.translatable(dialogue[index])), false);
	}

	@Override
	public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
		return isHostile() && super.doHurtTarget(target);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean canBeLeashed(Player player) {
		return false;
	}

	@Override
	public boolean isPushable() {
		return isHostile();
	}

	@Override
	public Packet<ClientGamePacketListener> getAddEntityPacket() {
		return NetworkHooks.getEntitySpawningPacket(this);
	}

	@Override
	public MobType getMobType() {
		return MobType.UNDEFINED;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PLAYER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.PLAYER_DEATH;
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.putInt(NBT_STATE, getEncounterState().id());
		tag.putBoolean(NBT_PERMIT_CLAIMED, isPermitClaimed());
		if (encounterOwner != null)
			tag.putUUID(NBT_OWNER, encounterOwner);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		setEncounterState(tag.contains(NBT_STATE) ? EncounterState.fromId(tag.getInt(NBT_STATE))
				: EncounterState.SURRENDERED);
		setPermitClaimed(tag.getBoolean(NBT_PERMIT_CLAIMED));
		encounterOwner = tag.hasUUID(NBT_OWNER) ? tag.getUUID(NBT_OWNER) : null;
	}

	public EncounterState getEncounterState() {
		return EncounterState.fromId(entityData.get(DATA_STATE));
	}

	public void setEncounterState(EncounterState state) {
		EncounterState next = state == null ? EncounterState.SURRENDERED : state;
		entityData.set(DATA_STATE, next.id());
		setCustomNameVisible(next != EncounterState.HOSTILE);
		if (next != EncounterState.HOSTILE) {
			setTarget(null);
			setAggressive(false);
			getNavigation().stop();
		}
	}

	public boolean isHostile() {
		return getEncounterState() == EncounterState.HOSTILE;
	}

	public boolean isSurrendered() {
		return getEncounterState() == EncounterState.SURRENDERED;
	}

	public boolean isSanctuaryResident() {
		return getEncounterState() == EncounterState.SANCTUARY;
	}

	public Optional<UUID> getEncounterOwner() {
		return Optional.ofNullable(encounterOwner);
	}

	public void setEncounterOwner(UUID owner) {
		encounterOwner = owner;
		setPersistenceRequired();
	}

	public boolean isOwnedBy(Player player) {
		return player != null && encounterOwner != null && encounterOwner.equals(player.getUUID());
	}

	public boolean isPermitClaimed() {
		return entityData.get(DATA_PERMIT_CLAIMED);
	}

	/** Recovery hook for the encounter manager. */
	public void setPermitClaimed(boolean claimed) {
		entityData.set(DATA_PERMIT_CLAIMED, claimed);
		if (claimed)
			setEncounterState(EncounterState.SANCTUARY);
	}

	/** Atomically closes interaction and turns the surrendered NPC into a resident. */
	public void markPermitClaimed() {
		setPermitClaimed(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 240.0D)
				.add(Attributes.MOVEMENT_SPEED, 0.32D)
				.add(Attributes.ATTACK_DAMAGE, 18.0D)
				.add(Attributes.ARMOR, 12.0D)
				.add(Attributes.ARMOR_TOUGHNESS, 4.0D)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.2D)
				.add(Attributes.FOLLOW_RANGE, 40.0D);
	}

	public enum EncounterState {
		HOSTILE(0),
		SURRENDERED(1),
		SANCTUARY(2);

		private final int id;

		EncounterState(int id) {
			this.id = id;
		}

		public int id() {
			return id;
		}

		public static EncounterState fromId(int id) {
			return switch (id) {
				case 0 -> HOSTILE;
				case 2 -> SANCTUARY;
				default -> SURRENDERED;
			};
		}
	}
}
