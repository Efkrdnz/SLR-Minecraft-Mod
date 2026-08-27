package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.FlagOfProtectionEntity;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.network.ClassPassiveMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.procedures.TankerProgressionHelper;
import net.solocraft.procedures.TankerProgressionRules;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.solocraft.network.compat.PacketDistributor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative implementation of the provisional Tanker rework.
 *
 * <p>Legacy mob effects and entity registry entries intentionally remain
 * registered for save compatibility. This manager never activates them.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class TankerSkillManager {
	public static final String TAUNT = TankerProgressionRules.TAUNT;
	public static final String REINFORCEMENT = TankerProgressionRules.REINFORCEMENT;
	public static final String TANK_LEAP = TankerProgressionRules.TANK_LEAP;
	public static final String SHIELD_BASH = TankerProgressionRules.SHIELD_BASH;
	public static final String WILLPOWER = TankerProgressionRules.WILLPOWER;
	public static final String PROTECTION_MARK = TankerProgressionRules.PROTECTION_MARK;

	public static final List<String> UNLOCK_ORDER = TankerProgressionRules.MASTERY_ORDER;
	public static final Set<String> SKILLS = Set.copyOf(UNLOCK_ORDER);

	// Stable server-to-client handoff IDs. The VFX owner installs one sink.
	public static final byte VFX_LEAP_START = 0;
	public static final byte VFX_LEAP_LAND = 1;
	public static final byte VFX_TAUNT_RING = 2;
	public static final byte VFX_BASH_SWEEP = 3;
	public static final byte VFX_BASH_HIT = 4;
	public static final byte VFX_BASH_STRAIN_RELIEF = 5;
	public static final byte VFX_BRACE_START = 6;
	public static final byte VFX_BRACE_HIT = 7;
	public static final byte VFX_STANCE_START = 8;
	public static final byte VFX_STANCE_END = 9;
	public static final byte VFX_WILLPOWER_START = 10;
	public static final byte VFX_WILLPOWER_THRESHOLD = 11;
	public static final byte VFX_WILLPOWER_SETTLE = 12;
	public static final byte VFX_WILLPOWER_BREAK = 13;
	public static final byte VFX_MARK_DEPLOY = 14;
	public static final byte VFX_MARK_THRESHOLD = 15;
	public static final byte VFX_MARK_BREAK = 16;
	public static final byte VFX_MARK_CANCEL = 17;
	private static final int VFX_FLAG_ESSENTIAL = 1;
	private static final int VFX_FLAG_CONFIRMED_HIT = 1 << 1;
	private static final int VFX_FLAG_PVP = 1 << 2;

	// First-playtest balance constants.
	public static final int TAUNT_FLAT_COST = 100;
	public static final double TAUNT_PERCENT_COST = 0.015D;
	public static final int TAUNT_COOLDOWN = 240;
	public static final int TAUNT_REGEN_LOCK = 30;
	public static final int SHIELD_BASH_FLAT_COST = 180;
	public static final double SHIELD_BASH_PERCENT_COST = 0.025D;
	public static final int SHIELD_BASH_COOLDOWN = 160;
	public static final int SHIELD_BASH_REGEN_LOCK = 30;
	public static final int TANK_LEAP_FLAT_COST = 260;
	public static final double TANK_LEAP_PERCENT_COST = 0.04D;
	public static final int TANK_LEAP_COOLDOWN = 280;
	public static final int TANK_LEAP_REGEN_LOCK = 40;
	public static final int REINFORCEMENT_FLAT_COST = 400;
	public static final double REINFORCEMENT_PERCENT_COST = 0.06D;
	public static final int REINFORCEMENT_COOLDOWN = 440;
	public static final int REINFORCEMENT_REGEN_LOCK = 50;
	public static final int WILLPOWER_FLAT_COST = 650;
	public static final double WILLPOWER_PERCENT_COST = 0.09D;
	public static final int WILLPOWER_COOLDOWN = 900;
	public static final int WILLPOWER_REGEN_LOCK = 60;
	public static final int PROTECTION_MARK_FLAT_COST = 900;
	public static final double PROTECTION_MARK_PERCENT_COST = 0.12D;
	public static final int PROTECTION_MARK_COOLDOWN = 1200;
	public static final int PROTECTION_MARK_REGEN_LOCK = 60;

	private static final int TANKER_CLASS = 4;
	private static final int IRON_WALL_MAX = 10;
	private static final int IRON_WALL_DURATION = 200;
	private static final double IRON_WALL_PVE_PER_STACK = 0.02D;
	private static final double IRON_WALL_PVP_PER_STACK = 0.01D;
	private static final double TAUNT_RANGE_SQR = 12.0D * 12.0D;
	public static final int TAUNT_TARGET_CAP = 16;
	public static final int TAUNT_MOB_DURATION = 120;
	public static final int TAUNT_MAINTENANCE_INTERVAL = 10;
	private static final int TAUNT_BOSS_DURATION = 40;
	private static final int CHALLENGED_DURATION = 60;
	private static final double CHALLENGED_DAMAGE_MULTIPLIER = 0.85D;
	private static final double BASH_REACH = 3.6D;
	private static final int BASH_TRAVEL_TICKS = 4;
	public static final int SHIELD_BASH_TARGET_CAP = 1;
	private static final double LEAP_RANGE = 8.0D;
	private static final double LEAP_IMPACT_RADIUS = 5.0D;
	public static final int LEAP_DEADLINE = 12;
	public static final int LEAP_TARGET_CAP = 16;
	public static final int PERFECT_BRACE_DURATION = 12;
	public static final int REINFORCED_STANCE_DURATION = 80;
	public static final int MAX_REINFORCEMENT_PHASES = 1;
	public static final int WILLPOWER_DURATION = 160;
	public static final int WILLPOWER_PULSES = 4;
	public static final int WILLPOWER_PULSE_INTERVAL = 10;
	public static final int MAX_WILLPOWER_STATES = 1;
	private static final double WILLPOWER_RELIEF_FRACTION = 0.08D;
	private static final double MARK_RADIUS = 6.0D;
	private static final double MARK_RADIUS_SQR = MARK_RADIUS * MARK_RADIUS;
	public static final int MARK_DURATION = 240;
	public static final int MARK_MEMBERSHIP_INTERVAL = 10;
	public static final int MARK_BENEFICIARY_CAP = 8;
	public static final int MAX_PROTECTION_MARKS_PER_OWNER = 1;
	private static final double MARK_INTEGRITY_FRACTION = 0.75D;
	private static final double EPSILON = 1.0E-7D;

	private static final String IRON_STACKS_TAG = "sl_t_stacks";
	private static final String LEGACY_IRON_TIMER_TAG = "sl_t_timer";
	private static final String IRON_EXPIRES_TAG = "slr_tanker_iron_wall_expires_at";
	private static final String IRON_LAST_DAMAGE_TICK_TAG = "slr_tanker_iron_wall_last_damage_tick";
	private static final String MASTERY_LOCK = "mastery";
	private static final String MANA_REGEN_LOCK = "mana_refresh";
	private static final String WP_DEBT_TAG = "slr_tanker_willpower_debt";
	private static final String WP_MAX_HEALTH_TAG = "slr_tanker_willpower_max_health";
	private static final String WP_PULSES_TAG = "slr_tanker_willpower_pulses";

	private static final ResourceLocation CONTROL_SLOW_MODIFIER_ID =
			ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
					"attribute/tanker_control_slow");

	private static final Map<String, SkillBalance> BALANCE = Map.of(
			TAUNT, new SkillBalance(TAUNT_FLAT_COST, TAUNT_PERCENT_COST, TAUNT_COOLDOWN, TAUNT_REGEN_LOCK),
			SHIELD_BASH, new SkillBalance(SHIELD_BASH_FLAT_COST, SHIELD_BASH_PERCENT_COST,
					SHIELD_BASH_COOLDOWN, SHIELD_BASH_REGEN_LOCK),
			TANK_LEAP, new SkillBalance(TANK_LEAP_FLAT_COST, TANK_LEAP_PERCENT_COST,
					TANK_LEAP_COOLDOWN, TANK_LEAP_REGEN_LOCK),
			REINFORCEMENT, new SkillBalance(REINFORCEMENT_FLAT_COST, REINFORCEMENT_PERCENT_COST,
					REINFORCEMENT_COOLDOWN, REINFORCEMENT_REGEN_LOCK),
			WILLPOWER, new SkillBalance(WILLPOWER_FLAT_COST, WILLPOWER_PERCENT_COST,
					WILLPOWER_COOLDOWN, WILLPOWER_REGEN_LOCK),
			PROTECTION_MARK, new SkillBalance(PROTECTION_MARK_FLAT_COST, PROTECTION_MARK_PERCENT_COST,
					PROTECTION_MARK_COOLDOWN, PROTECTION_MARK_REGEN_LOCK));

	private static final Map<UUID, TankerState> STATES = new HashMap<>();
	private static final Map<UUID, ProtectionZone> MARKS = new HashMap<>();
	private static final Map<UUID, TauntClaim> TAUNT_CLAIMS = new HashMap<>();
	private static final Map<UUID, ChallengedState> CHALLENGED = new HashMap<>();
	private static final Map<UUID, SlowState> ACTIVE_SLOWS = new HashMap<>();

	private static final VfxSink NO_VFX = (level, event) -> {
	};
	private static volatile VfxSink vfxSink = NO_VFX;

	private TankerSkillManager() {
	}

	/**
	 * Narrow VFX-owner handoff. Gameplay never depends on a sink being installed.
	 */
	public static void installVfxSink(VfxSink sink) {
		vfxSink = sink == null ? NO_VFX : sink;
	}

	public static boolean isTankerSkill(String skill) {
		return TankerProgressionRules.isTankerSkill(skill);
	}

	public static String canonicalName(String skill) {
		return TankerProgressionRules.canonicalName(skill);
	}

	public static String canonicalizeSkillList(String encoded) {
		return TankerProgressionRules.canonicalizeSkillList(encoded);
	}

	public static List<String> entitlementsForRank(int rank) {
		return TankerProgressionRules.entitlementsForRank(rank);
	}

	public static String firstMissingSkill(String encoded) {
		return TankerProgressionRules.firstMissingSkill(encoded);
	}

	public static boolean hasSkill(Entity entity, String requestedSkill) {
		String skill = canonicalName(requestedSkill);
		if (entity == null || !SKILLS.contains(skill))
			return false;
		return TankerProgressionRules.hasSkill(variables(entity).Plist, skill);
	}

	/**
	 * Main dispatcher handoff. All validation precedes the cast transaction.
	 */
	public static boolean activateSkill(ServerPlayer player, String requestedSkill) {
		if (player == null || !player.isAlive())
			return false;
		reconcileTanker(player);
		String skill = canonicalName(requestedSkill);
		if (!SKILLS.contains(skill))
			return false;
		if (!hasSkill(player, skill)) {
			message(player, "You have not learned " + skill + ".");
			return false;
		}
		if (CooldownManager.isOnCooldown(player, skill)) {
			message(player, skill + " is on cooldown for "
					+ CooldownManager.getRemainingSeconds(player, skill) + "s.");
			return false;
		}

		return switch (skill) {
			case TAUNT -> castTaunt(player);
			case SHIELD_BASH -> castShieldBash(player);
			case TANK_LEAP -> castTankLeap(player);
			case REINFORCEMENT -> castReinforcement(player);
			case WILLPOWER -> castWillpower(player);
			case PROTECTION_MARK -> castProtectionMark(player);
			default -> false;
		};
	}

	/**
	 * Progression-owner handoff for rank/class assignment and save migration.
	 * Existing learned skills are never removed.
	 */
	public static void reconcileTanker(ServerPlayer player) {
		if (player == null)
			return;
		TankerProgressionHelper.reconcileRankEntitlements(player);
		boolean tanker = isTanker(player);

		clearLegacyCancellationState(player);
		player.getPersistentData().remove(LEGACY_IRON_TIMER_TAG);
		if (!tanker)
			clearIronWall(player);
	}

	/**
	 * Discards one player's Tanker combat session without settling Willpower.
	 * Reset is not combat damage, so retained strain must not become delayed
	 * damage pulses after the new character state has been installed.
	 */
	public static void resetPlayerState(ServerPlayer player) {
		if (player == null)
			return;
		UUID playerId = player.getUUID();
		TankerState state = STATES.remove(playerId);
		if (state != null) {
			clearMovement(player, state);
			clearTaunts(player, state);
			endReinforcement(player, state, player.level().getGameTime(), true);
			state.willpower = null;
		}
		removeMark(playerId, MarkEnd.CANCEL);
		removeChallengesFor(playerId);
		removeOwnedSlows(playerId);
		removeSlow(player);
		clearPersistedWillpower(player);
		clearIronWall(player);
		clearLegacyCancellationState(player);
	}

	private static void clearLegacyCancellationState(ServerPlayer player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(vars -> {
			boolean changed = false;
			if (vars.inv || vars.leapjump || Math.abs(vars.wp) > EPSILON || vars.shieldbash) {
				vars.inv = false;
				vars.leapjump = false;
				vars.wp = 0.0D;
				vars.shieldbash = false;
				changed = true;
			}
			if (changed)
				vars.syncPlayerVariables(player);
		});

		stripLegacyEffects(player);
	}

	/**
	 * Progression-owner handoff for all six Tanker runestones.
	 */
	public static boolean learnFromRunestone(Entity entity, ItemStack stack, String requestedSkill) {
		if (!(entity instanceof ServerPlayer player) || stack == null || stack.isEmpty())
			return false;
		String skill = canonicalName(requestedSkill);
		if (!SKILLS.contains(skill))
			return false;
		boolean alreadyKnown = hasSkill(player, skill);
		TankerProgressionHelper.learnFromRunestone(player, stack, skill);
		return !alreadyKnown && hasSkill(player, skill);
	}

	/**
	 * Progression-owner handoff for deterministic generic Tanker mastery.
	 *
	 * @return the granted canonical skill, or an empty string when complete
	 */
	public static String grantNextMasterySkill(ServerPlayer player) {
		if (player == null)
			return "";
		reconcileTanker(player);
		if (!TankerProgressionHelper.isTanker(player))
			return "";
		return TankerProgressionHelper.grantNextMasterySkill(player);
	}

	public static int manaCost(Entity entity, String requestedSkill) {
		String skill = canonicalName(requestedSkill);
		SkillBalance balance = BALANCE.get(skill);
		if (balance == null || entity instanceof Player player && player.isCreative())
			return 0;
		double maximumMana = Math.max(1000.0D, variables(entity).Mana);
		return beaconCost(maximumMana, balance.flatFloor, balance.maximumManaFraction);
	}

	public static int beaconCost(double maximumMana, int flatFloor, double maximumManaFraction) {
		return (int) Math.ceil(Math.max(flatFloor,
				Math.max(1000.0D, maximumMana) * maximumManaFraction));
	}

	public static List<UUID> boundedTargetIds(List<TargetOrder> candidates, int maximum) {
		if (candidates == null || candidates.isEmpty() || maximum <= 0)
			return List.of();
		return candidates.stream()
				.filter(Objects::nonNull)
				.filter(candidate -> candidate.targetId() != null)
				.sorted(Comparator
						.comparingDouble(TargetOrder::primaryOrder)
						.thenComparingDouble(TargetOrder::squaredDistance)
						.thenComparing(TargetOrder::targetId))
				.limit(maximum)
				.map(TargetOrder::targetId)
				.toList();
	}

	public static CleanupAction cleanupAction(TransientState state,
			CleanupReason reason, double unpaidStrain) {
		if (state != TransientState.WILLPOWER || !(unpaidStrain > EPSILON))
			return CleanupAction.CLEAR;
		return switch (Objects.requireNonNull(reason, "reason")) {
			case DEATH -> CleanupAction.CLEAR;
			case DIMENSION_CHANGE, CLASS_CHANGE ->
					CleanupAction.START_STRAIN_SETTLEMENT;
			case LOGOUT, SERVER_STOP ->
					CleanupAction.PERSIST_STRAIN_SETTLEMENT;
		};
	}

	private static boolean castTaunt(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 center = player.position();
		List<LivingEntity> accepted = level.getEntitiesOfClass(
						LivingEntity.class, player.getBoundingBox().inflate(12.0D),
						target -> validTauntTarget(player, target)
								&& target.distanceToSqr(center) <= TAUNT_RANGE_SQR
								&& player.hasLineOfSight(target))
				.stream()
				.sorted(distanceThenUuid(center))
				.limit(TAUNT_TARGET_CAP)
				.toList();
		if (accepted.isEmpty()) {
			message(player, "No valid targets are in Taunt range.");
			return false;
		}
		if (!canAfford(player, TAUNT))
			return false;

		long now = level.getGameTime();
		TankerState state = state(player);
		return commitCast(player, TAUNT, () -> {
			clearTaunts(player, state);
			for (LivingEntity target : accepted) {
				CombatCategory category = offensiveCategory(target);
				long expiry = now + switch (category) {
					case BOSS -> TAUNT_BOSS_DURATION;
					case PVP -> CHALLENGED_DURATION;
					case NORMAL -> TAUNT_MOB_DURATION;
				};
				TauntEntry entry = new TauntEntry(target.getUUID(), category, expiry);
				state.taunts.put(target.getUUID(), entry);
				TAUNT_CLAIMS.put(target.getUUID(),
						new TauntClaim(target.getUUID(), player.getUUID(), level, expiry));
				if (target instanceof ServerPlayer challenged) {
					CHALLENGED.put(challenged.getUUID(),
							new ChallengedState(player.getUUID(), level, now + CHALLENGED_DURATION));
				} else if (target instanceof Mob mob) {
					mob.setTarget(player);
				}
			}
			state.nextTauntMaintenance = now + TAUNT_MAINTENANCE_INTERVAL;
			int ironGrant = Math.min(3, 1 + (accepted.size() - 1) / 4);
			addIronWall(player, ironGrant, now);
		}, () -> {
			tryAwardMastery(player, MasteryTrigger.TAUNT);
			emit(player, VFX_TAUNT_RING, null, center, now, TAUNT_MOB_DURATION,
					accepted.size(), VFX_FLAG_ESSENTIAL);
		});
	}

	/**
	 * Lets autonomous tank companions respect an intentional player Taunt. A
	 * live manual claim always wins instead of allowing two tank systems to
	 * bounce one mob's target back and forth.
	 */
	public static boolean hasActiveTauntClaim(LivingEntity target) {
		if (target == null)
			return false;
		TauntClaim claim = TAUNT_CLAIMS.get(target.getUUID());
		if (claim == null)
			return false;
		long now = target.level().getGameTime();
		if (claim.level != target.level() || now >= claim.expiresAt) {
			TAUNT_CLAIMS.remove(target.getUUID(), claim);
			return false;
		}
		return true;
	}

	private static boolean castShieldBash(ServerPlayer player) {
		if (!player.onGround()) {
			message(player, "Shield Bash requires solid ground.");
			return false;
		}
		if (!player.getOffhandItem().is(RuntimeKeys.SHIELDS)) {
			message(player, "Equip a shield in your offhand.");
			return false;
		}
		TankerState state = state(player);
		if (state.leap != null || state.bash != null) {
			message(player, "You are already committed to a Tanker movement skill.");
			return false;
		}
		ServerLevel level = player.serverLevel();
		Vec3 direction = horizontalDirection(player);
		Vec3 start = player.position();
		Vec3 end = findSafeHorizontalEndpoint(level, player, start, direction, BASH_REACH);
		if (end == null || end.distanceToSqr(start) < 0.04D) {
			message(player, "There is no safe space to Shield Bash.");
			return false;
		}

		AABB swept = player.getBoundingBox()
				.minmax(player.getBoundingBox().move(end.subtract(start)))
				.inflate(0.45D, 0.25D, 0.45D);
		LivingEntity selected = level.getEntitiesOfClass(
						LivingEntity.class, swept, target -> validEnemy(player, target, false)
								&& forwardProjection(start, direction, target.position()) >= -0.25D
								&& forwardProjection(start, direction, target.position())
										<= start.distanceTo(end) + 1.0D)
				.stream()
				.sorted(Comparator
						.comparingDouble((LivingEntity target) ->
								forwardProjection(start, direction, target.position()))
						.thenComparingDouble(target -> target.distanceToSqr(start))
						.thenComparing(target -> target.getUUID().toString()))
				.findFirst()
				.orElse(null);
		if (!canAfford(player, SHIELD_BASH))
			return false;

		long now = level.getGameTime();
		UUID targetId = selected == null ? null : selected.getUUID();
		return commitCast(player, SHIELD_BASH, () -> {
			state.bash = new BashState(now, start, end, direction, targetId);
		}, () -> {
			emit(player, VFX_BASH_SWEEP, selected, start, now, BASH_TRAVEL_TICKS,
					100, VFX_FLAG_ESSENTIAL);
		});
	}

	private static boolean castTankLeap(ServerPlayer player) {
		if (!player.onGround()) {
			message(player, "Tank Leap requires solid ground.");
			return false;
		}
		TankerState state = state(player);
		if (state.leap != null || state.bash != null) {
			message(player, "You are already committed to a Tanker movement skill.");
			return false;
		}
		ServerLevel level = player.serverLevel();
		Vec3 direction = horizontalDirection(player);
		Vec3 start = player.position();
		Vec3 firstStep = direction.scale(LEAP_RANGE / LEAP_DEADLINE);
		if (!isLoadedAndWithinBorder(level, player.getBoundingBox().move(firstStep))
				|| !level.noCollision(player, player.getBoundingBox().move(firstStep))) {
			message(player, "There is no safe path for Tank Leap.");
			return false;
		}
		if (!canAfford(player, TANK_LEAP))
			return false;

		long now = level.getGameTime();
		return commitCast(player, TANK_LEAP, () -> {
			state.leap = new LeapState(level.dimension(), now, now + LEAP_DEADLINE,
					start, start, direction);
			player.fallDistance = 0.0F;
			player.setDeltaMovement(direction.scale(LEAP_RANGE / LEAP_DEADLINE)
					.add(0.0D, 0.58D, 0.0D));
			player.hurtMarked = true;
		}, () -> {
			emit(player, VFX_LEAP_START, null, start, now, LEAP_DEADLINE,
					100, VFX_FLAG_ESSENTIAL);
		});
	}

	private static boolean castReinforcement(ServerPlayer player) {
		TankerState state = state(player);
		long now = player.level().getGameTime();
		expireReinforcement(player, state, now);
		if (state.reinforcementPhase != ReinforcementPhase.NONE) {
			message(player, "Reinforcement is already active.");
			return false;
		}
		if (state.willpower != null && state.willpower.active) {
			message(player, "Reinforcement cannot start during Willpower.");
			return false;
		}
		if (!canAfford(player, REINFORCEMENT))
			return false;

		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		return commitCast(player, REINFORCEMENT, () -> {
			state.reinforcementPhase = ReinforcementPhase.PERFECT;
			state.reinforcementExpiresAt = now + PERFECT_BRACE_DURATION;
		}, () -> {
			emit(player, VFX_BRACE_START, null, origin, now,
					PERFECT_BRACE_DURATION, 100, VFX_FLAG_ESSENTIAL);
		});
	}

	private static boolean castWillpower(ServerPlayer player) {
		TankerState state = state(player);
		long now = player.level().getGameTime();
		expireReinforcement(player, state, now);
		if (state.reinforcementPhase != ReinforcementPhase.NONE) {
			message(player, "Willpower cannot start during Reinforcement.");
			return false;
		}
		if (state.willpower != null
				&& (state.willpower.active || state.willpower.settling)) {
			message(player, "Willpower is already active or settling.");
			return false;
		}
		if (!canAfford(player, WILLPOWER))
			return false;

		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		double maximumHealth = Math.max(1.0D, player.getMaxHealth());
		return commitCast(player, WILLPOWER, () -> {
			state.willpower = WillpowerState.active(now + WILLPOWER_DURATION, maximumHealth);
			persistWillpower(player, state.willpower);
		}, () -> {
			emit(player, VFX_WILLPOWER_START, null, origin, now,
					WILLPOWER_DURATION, 0, VFX_FLAG_ESSENTIAL);
		});
	}

	private static boolean castProtectionMark(ServerPlayer player) {
		ServerLevel level = player.serverLevel();
		Vec3 center = findGroundBelow(level, player);
		if (center == null) {
			message(player, "Protection Mark needs collision-safe ground.");
			return false;
		}
		if (!canAfford(player, PROTECTION_MARK))
			return false;

		long now = level.getGameTime();
		return commitCast(player, PROTECTION_MARK, () -> {
			removeMark(player.getUUID(), MarkEnd.CANCEL);
			ProtectionZone zone = new ProtectionZone(
					player.getUUID(), player.getId(), level, level.dimension(), center,
					now, now + MARK_DURATION,
					protectionMarkInitialIntegrity(Math.max(1.0D, player.getMaxHealth())),
					player.getYRot(), player.getXRot());
			MARKS.put(player.getUUID(), zone);
			refreshMarkBeneficiaries(player, zone, now);
		}, () -> {
			emit(player, VFX_MARK_DEPLOY, null, center, now, MARK_DURATION,
					0, VFX_FLAG_ESSENTIAL);
		});
	}

	private static boolean canAfford(ServerPlayer player, String skill) {
		int cost = manaCost(player, skill);
		if (player.isCreative() || variables(player).MP + EPSILON >= cost)
			return true;
		message(player, "Not enough MP. " + skill + " needs " + cost + ".");
		return false;
	}

	private static boolean commitCast(ServerPlayer player, String skill,
			Runnable stateCommit, Runnable feedbackCommit) {
		SkillBalance balance = BALANCE.get(skill);
		if (balance == null)
			return false;
		int cost = manaCost(player, skill);
		stateCommit.run();
		if (!player.isCreative() && cost > 0) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.ifPresent(vars -> {
						vars.MP = Math.max(0.0D, vars.MP - cost);
						vars.syncPlayerVariables(player);
					});
		}
		CooldownManager.setFullDuration(player, skill, balance.cooldownTicks);
		extendManaRegenLock(player, balance.regenLockTicks);
		feedbackCommit.run();
		message(player, "Using " + skill);
		return true;
	}

	private static void extendManaRegenLock(ServerPlayer player, int ticks) {
		int remaining = CooldownManager.getRemainingTicks(player, MANA_REGEN_LOCK);
		if (remaining < ticks)
			CooldownManager.setFullDuration(player, MANA_REGEN_LOCK, ticks);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onLivingHurt(LivingIncomingDamageEvent event) {
		if (event.getEntity().level().isClientSide() || event.getAmount() <= 0.0F)
			return;
		applyChallengedOutgoing(event);
		if (!(event.getEntity() instanceof ServerPlayer victim))
			return;
		if (event.getSource().is(RuntimeKeys.WILLPOWER_STRAIN_DAMAGE))
			return;

		CombatContext context = combatContext(victim, event.getSource());
		if (context == null)
			return;
		boolean tanker = isTanker(victim);
		MarkSelection mark = selectProtectionMark(victim, context.category);
		if (!tanker && mark == null)
			return;

		long now = victim.level().getGameTime();
		TankerState state = tanker ? state(victim) : null;
		if (state != null)
			expireReinforcement(victim, state, now);
		int priorIronStacks = tanker ? currentIronWall(victim, now) : 0;
		ReinforcementPhase phaseForHit = state == null
				? ReinforcementPhase.NONE : state.reinforcementPhase;

		double original = event.getAmount();
		double retainedWithoutMark = retainedDamage(original, context.category,
				priorIronStacks, 0.0D, phaseForHit);
		double retained = retainedWithoutMark;

		if (mark != null) {
			double requestedPrevention = protectionMarkRequestedPrevention(
					original, context.category, priorIronStacks,
					mark.reduction, phaseForHit);
			ProtectionFundingResult funding = calculateProtectionFunding(
					requestedPrevention, mark.zone.integrity, context.category);
			if (funding.preventedDamage() > EPSILON) {
				retained -= funding.preventedDamage();
				drainMark(mark, victim, funding);
			}
		}

		if (state != null && phaseForHit != ReinforcementPhase.NONE) {
			state.pendingKnockback = new PendingKnockback(now,
					knockbackMultiplier(phaseForHit, context.category));
			if (phaseForHit == ReinforcementPhase.PERFECT) {
				state.reinforcementPhase = ReinforcementPhase.STANCE;
				state.reinforcementExpiresAt = now + REINFORCED_STANCE_DURATION;
				emit(victim, VFX_BRACE_HIT, context.owner, victim.position(), now,
						6, 100, confirmedHitFlags(context.category));
				emit(victim, VFX_STANCE_START, null, victim.position(), now,
						REINFORCED_STANCE_DURATION, 100,
						VFX_FLAG_ESSENTIAL | pvpFlag(context.category));
			}
		}

		double finalAmount = retained;
		if (state != null && state.willpower != null && state.willpower.active)
			finalAmount = applyWillpower(victim, state.willpower, retained,
					context.category, now);
		event.setAmount((float) Math.max(0.0D, finalAmount));

		if (tanker && original > EPSILON) {
			grantDamageIronWall(victim, now);
			if (phaseForHit == ReinforcementPhase.PERFECT) {
				addIronWall(victim, 2, now);
				tryAwardMastery(victim, MasteryTrigger.REINFORCEMENT_BRACE);
			}
		}
	}

	/**
	 * Neutralizes save-compatible legacy Tanker cancellation state before the
	 * retired LivingAttack handlers can observe it. Damage is handled only in
	 * {@link #onLivingHurt(LivingIncomingDamageEvent)}.
	 */
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLivingAttack(LivingIncomingDamageEvent event) {
		if (!event.getEntity().level().isClientSide()
				&& event.getEntity() instanceof ServerPlayer player)
			clearLegacyCancellationState(player);
	}

	@SubscribeEvent
	public static void onKnockback(LivingKnockBackEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| player.level().isClientSide())
			return;
		TankerState state = STATES.get(player.getUUID());
		if (state == null || state.pendingKnockback == null)
			return;
		long now = player.level().getGameTime();
		if (state.pendingKnockback.gameTick != now) {
			state.pendingKnockback = null;
			return;
		}
		event.setStrength((float) (event.getStrength()
				* state.pendingKnockback.multiplier));
		state.pendingKnockback = null;
	}

	@SubscribeEvent
	public static void onFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| player.level().isClientSide())
			return;
		TankerState state = STATES.get(player.getUUID());
		if (state != null && state.leap != null
				&& state.leap.dimension.equals(player.level().dimension())) {
			event.setCanceled(true);
			player.fallDistance = 0.0F;
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false
				|| !(event.getEntity() instanceof ServerPlayer player))
			return;
		long now = player.level().getGameTime();
		boolean tanker = isTanker(player);
		if (player.tickCount % 20 == 0)
			reconcileTanker(player);

		if (tanker)
			currentIronWall(player, now);
		else
			clearIronWall(player);

		TankerState state = STATES.get(player.getUUID());
		if (state != null) {
			if (!tanker)
				clearActiveCombatForClassChange(player, state, now);
			tickLeap(player, state, now);
			tickBash(player, state, now);
			tickTaunts(player, state, now);
			expireReinforcement(player, state, now);
			tickWillpower(player, state, now);
			if (state.pendingKnockback != null
					&& state.pendingKnockback.gameTick < now)
				state.pendingKnockback = null;
			if (state.isEmpty())
				STATES.remove(player.getUUID());
		}

		ProtectionZone zone = MARKS.get(player.getUUID());
		if (zone != null) {
			if (!tanker || zone.level != player.level()
					|| now >= zone.expiresAt || !player.isAlive()) {
				removeMark(player.getUUID(), MarkEnd.CANCEL);
			} else if (now >= zone.nextMembershipUpdate) {
				refreshMarkBeneficiaries(player, zone, now);
			}
		}
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		tickSlows(event.getServer());
		pruneClaims(event.getServer());
		pruneChallenges(event.getServer());
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		reconcileTanker(player);
		loadPendingSettlement(player);
		if (isTanker(player))
			syncIronWall(player, currentIronWall(player, player.level().getGameTime()));
		else
			syncIronWall(player, 0);
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		reconcileTanker(player);
		clearPersistedWillpower(player);
		if (isTanker(player))
			syncIronWall(player, currentIronWall(player, player.level().getGameTime()));
		else
			syncIronWall(player, 0);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		TankerState state = STATES.remove(player.getUUID());
		if (state != null) {
			if (state.willpower != null && state.willpower.active)
				beginSettlement(player, state.willpower,
						player.level().getGameTime(), false);
			if (state.willpower != null)
				persistWillpower(player, state.willpower);
			clearTaunts(player, state);
		}
		removeMark(player.getUUID(), MarkEnd.CANCEL);
		removeChallengesFor(player.getUUID());
		removeSlow(player);
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		removeSlow(player);
		long now = player.level().getGameTime();
		TankerState state = STATES.get(player.getUUID());
		if (state != null) {
			clearMovement(player, state);
			clearTaunts(player, state);
			endReinforcement(player, state, now, true);
			if (state.willpower != null && state.willpower.active)
				beginSettlement(player, state.willpower, now, false);
		}
		removeMark(player.getUUID(), MarkEnd.CANCEL);
		removeChallengesFor(player.getUUID());
		clearIronWall(player);
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		LivingEntity entity = event.getEntity();
		removeSlow(entity);
		if (!(entity instanceof ServerPlayer player))
			return;
		TankerState state = STATES.remove(player.getUUID());
		if (state != null)
			clearTaunts(player, state);
		removeMark(player.getUUID(), MarkEnd.CANCEL);
		removeChallengesFor(player.getUUID());
		clearPersistedWillpower(player);
		clearIronWall(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLegacyFlagLoad(EntityJoinLevelEvent event) {
		if (!event.getLevel().isClientSide()
				&& event.getEntity() instanceof FlagOfProtectionEntity) {
			event.setCanceled(true);
			event.getEntity().discard();
		}
		if (!event.getLevel().isClientSide()
				&& event.getEntity() instanceof LivingEntity living) {
			SlowState slow = ACTIVE_SLOWS.get(living.getUUID());
			if (slow == null || slow.level != event.getLevel()
					|| !refreshSlowModifier(living, slow,
							event.getLevel().getGameTime())) {
				ACTIVE_SLOWS.remove(living.getUUID());
				removeSlowModifier(living);
			}
		}
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		for (Map.Entry<UUID, TankerState> entry : STATES.entrySet()) {
			WillpowerState willpower = entry.getValue().willpower;
			if (willpower == null)
				continue;
			ServerPlayer player = event.getServer().getPlayerList()
					.getPlayer(entry.getKey());
			if (player == null)
				continue;
			if (willpower.active)
				beginSettlement(player, willpower,
						player.level().getGameTime(), false);
			persistWillpower(player, willpower);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		for (SlowState slow : ACTIVE_SLOWS.values()) {
			Entity entity = slow.level.getEntity(slow.targetId);
			if (entity instanceof LivingEntity living)
				removeSlowModifier(living);
		}
		STATES.clear();
		MARKS.clear();
		TAUNT_CLAIMS.clear();
		CHALLENGED.clear();
		ACTIVE_SLOWS.clear();
	}

	private static void applyChallengedOutgoing(LivingIncomingDamageEvent event) {
		Entity owner = resolveSourceOwner(event.getSource());
		if (!(owner instanceof ServerPlayer attacker))
			return;
		ChallengedState challenged = CHALLENGED.get(attacker.getUUID());
		if (challenged == null)
			return;
		long now = attacker.level().getGameTime();
		ServerPlayer taunter = attacker.server.getPlayerList().getPlayer(challenged.taunterId);
		if (now >= challenged.expiresAt || challenged.level != attacker.level()
				|| taunter == null || !taunter.isAlive() || taunter.level() != attacker.level()
				|| MageCombatHelper.areAllied(attacker, taunter)) {
			CHALLENGED.remove(attacker.getUUID());
			return;
		}
		if (!event.getEntity().getUUID().equals(taunter.getUUID()))
			event.setAmount((float) (event.getAmount() * CHALLENGED_DAMAGE_MULTIPLIER));
	}

	private static double applyWillpower(ServerPlayer player, WillpowerState willpower,
			double retained, CombatCategory category, long now) {
		WillpowerHitResult result = calculateWillpowerHit(
				retained, willpower.strain, willpower.currentCap,
				willpower.maxHealthAtActivation, category);
		willpower.currentCap = result.activeCap();
		willpower.strain = result.resultingStrain();
		if (result.strainAdded() <= EPSILON) {
			if (result.endsWillpower())
				beginSettlement(player, willpower, now, true);
			return result.immediateDamage();
		}
		willpower.totalRetained += retained;
		willpower.totalImmediate += result.immediateDamage();
		persistWillpower(player, willpower);
		updateWillpowerThreshold(player, willpower, now);

		if (!willpower.masteryAwarded
				&& willpower.strain + willpower.shieldBashStrainRelief
						>= willpower.maxHealthAtActivation * 0.10D) {
			willpower.masteryAwarded = true;
			tryAwardMastery(player, MasteryTrigger.WILLPOWER_THRESHOLD);
		}
		if (result.endsWillpower())
			beginSettlement(player, willpower, now, true);
		return result.immediateDamage();
	}

	private static void beginSettlement(ServerPlayer player, WillpowerState willpower,
			long now, boolean broken) {
		if (!willpower.active)
			return;
		willpower.active = false;
		willpower.settling = willpower.strain > EPSILON;
		willpower.pulsesRemaining = willpower.settling ? WILLPOWER_PULSES : 0;
		willpower.nextPulseAt = willpower.settling ? now + WILLPOWER_PULSE_INTERVAL : 0L;
		persistWillpower(player, willpower);
		if (broken) {
			emit(player, VFX_WILLPOWER_BREAK, null, player.position(), now,
					8, vfxIntensityBand(thresholdIntensity(willpower)),
					VFX_FLAG_ESSENTIAL);
		}
	}

	private static void tickWillpower(ServerPlayer player, TankerState state, long now) {
		WillpowerState willpower = state.willpower;
		if (willpower == null)
			return;
		if (willpower.active && now >= willpower.activeExpiresAt)
			beginSettlement(player, willpower, now, false);
		if (!willpower.settling) {
			if (!willpower.active) {
				clearPersistedWillpower(player);
				state.willpower = null;
			}
			return;
		}
		if (now < willpower.nextPulseAt)
			return;
		if (willpower.pulsesRemaining <= 0 || willpower.strain <= EPSILON) {
			willpower.strain = 0.0D;
			willpower.settling = false;
			clearPersistedWillpower(player);
			state.willpower = null;
			return;
		}

		double pulse = willpower.pulsesRemaining == 1
				? willpower.strain
				: willpower.strain / willpower.pulsesRemaining;
		float enginePulse = (float) pulse;
		if (!player.hurt(strainDamageSource(player.serverLevel()), enginePulse)) {
			willpower.nextPulseAt = now + WILLPOWER_PULSE_INTERVAL;
			return;
		}
		willpower.strain = Math.max(0.0D, willpower.strain - enginePulse);
		willpower.pulsesRemaining--;
		willpower.nextPulseAt = now + WILLPOWER_PULSE_INTERVAL;
		if (!player.isAlive())
			return;
		persistWillpower(player, willpower);
		emit(player, VFX_WILLPOWER_SETTLE, null, player.position(), now,
				WILLPOWER_PULSE_INTERVAL,
				vfxIntensityBand(WILLPOWER_PULSES - willpower.pulsesRemaining),
				VFX_FLAG_ESSENTIAL);
		if (willpower.pulsesRemaining == 0 || willpower.strain <= EPSILON) {
			willpower.strain = 0.0D;
			willpower.settling = false;
			clearPersistedWillpower(player);
			state.willpower = null;
		}
	}

	private static void updateWillpowerThreshold(ServerPlayer player,
			WillpowerState willpower, long now) {
		int threshold = thresholdIntensity(willpower);
		while (willpower.lastThreshold < threshold) {
			willpower.lastThreshold++;
			emit(player, VFX_WILLPOWER_THRESHOLD, null, player.position(), now,
					8, vfxIntensityBand(willpower.lastThreshold), 0);
		}
	}

	private static int thresholdIntensity(WillpowerState willpower) {
		if (willpower.currentCap <= EPSILON)
			return 4;
		return Mth.clamp((int) Math.floor(
				(willpower.strain / willpower.currentCap) * 4.0D + EPSILON), 0, 4);
	}

	private static void tickLeap(ServerPlayer player, TankerState state, long now) {
		LeapState leap = state.leap;
		if (leap == null)
			return;
		if (!player.isAlive() || !isTanker(player)
				|| !leap.dimension.equals(player.level().dimension())) {
			clearLeap(player, state);
			return;
		}
		player.fallDistance = 0.0F;
		double horizontalDistance = horizontalDistance(player.position(), leap.start);
		boolean landed = now > leap.startedAt && player.onGround();
		boolean collision = player.horizontalCollision;
		boolean timeout = now >= leap.deadline || horizontalDistance >= LEAP_RANGE - 0.05D;
		if (landed || timeout) {
			landLeap(player, state, now);
			return;
		}
		if (collision) {
			restoreLeapLastSafe(player, leap);
			landLeap(player, state, now);
			return;
		}

		Vec3 horizontalStep = leap.direction.scale(LEAP_RANGE / LEAP_DEADLINE);
		AABB nextBox = player.getBoundingBox().move(horizontalStep)
				.move(0.0D, Math.min(0.6D, player.getDeltaMovement().y), 0.0D);
		if (!isLoadedAndWithinBorder(player.serverLevel(), nextBox)
				|| !player.serverLevel().noCollision(player, nextBox)) {
			restoreLeapLastSafe(player, leap);
			landLeap(player, state, now);
			return;
		}
		leap.lastSafe = player.position();
		double remaining = Math.max(0.0D, LEAP_RANGE - horizontalDistance);
		Vec3 step = leap.direction.scale(Math.min(LEAP_RANGE / LEAP_DEADLINE, remaining));
		player.setDeltaMovement(step.x, player.getDeltaMovement().y, step.z);
		player.hurtMarked = true;
	}

	private static void restoreLeapLastSafe(ServerPlayer player, LeapState leap) {
		if (player == null || leap == null || leap.lastSafe == null)
			return;
		player.setPos(leap.lastSafe.x, leap.lastSafe.y, leap.lastSafe.z);
		player.hurtMarked = true;
	}

	private static void landLeap(ServerPlayer player, TankerState state, long now) {
		LeapState leap = state.leap;
		if (leap == null)
			return;
		state.leap = null;
		player.fallDistance = 0.0F;
		player.setDeltaMovement(0.0D, Math.min(0.0D, player.getDeltaMovement().y), 0.0D);
		player.hurtMarked = true;
		ServerLevel level = player.serverLevel();
		Vec3 origin = player.position();
		List<LivingEntity> targets = level.getEntitiesOfClass(
						LivingEntity.class,
						new AABB(origin, origin).inflate(LEAP_IMPACT_RADIUS),
						target -> validEnemy(player, target, true)
								&& target.distanceToSqr(origin)
										<= LEAP_IMPACT_RADIUS * LEAP_IMPACT_RADIUS)
				.stream()
				.sorted(distanceThenUuid(origin))
				.limit(LEAP_TARGET_CAP)
				.toList();
		int hits = 0;
		for (LivingEntity target : targets) {
			CombatCategory category = offensiveCategory(target);
			float damage = (float) (tankerPower(player) * switch (category) {
				case NORMAL -> 1.10D;
				case BOSS -> 0.80D;
				case PVP -> 0.60D;
			});
			if (!dealTankerDamage(player, target, damage))
				continue;
			leap.hitLedger.add(target.getUUID());
			hits++;
			if (category == CombatCategory.NORMAL) {
				Vec3 pull = origin.subtract(target.position());
				Vec3 horizontal = new Vec3(pull.x, 0.0D, pull.z);
				if (horizontal.lengthSqr() > EPSILON) {
					double strength = Math.min(1.1D, horizontal.length());
					Vec3 impulse = horizontal.normalize().scale(strength);
					target.push(impulse.x, 0.15D, impulse.z);
					target.hurtMarked = true;
				}
			} else if (category == CombatCategory.PVP) {
				applySlow(player, target, 0.20D, 8, level);
			}
		}
		if (hits > 0)
			addIronWall(player, 1, now);
		SololevelingModVariables.PlayerVariables variables = variables(player);
		AbilityDestructionManager.impact(player,
				AbilityDestructionManager.Profile.TANKER_SLAM, origin,
				TemporaryStatBonusManager.effectiveStrength(player)
						+ variables.Vitality * 0.5D
						+ player.getAttributeValue(Attributes.ATTACK_DAMAGE) * 14.0D,
				false);
		emit(player, VFX_LEAP_LAND, null, origin, now, 10,
				hits, VFX_FLAG_ESSENTIAL
						| (hits > 0 ? VFX_FLAG_CONFIRMED_HIT : 0));
	}

	private static void clearLeap(ServerPlayer player, TankerState state) {
		if (state.leap == null)
			return;
		state.leap = null;
		player.fallDistance = 0.0F;
	}

	private static void tickBash(ServerPlayer player, TankerState state, long now) {
		BashState bash = state.bash;
		if (bash == null)
			return;
		if (!player.isAlive() || !isTanker(player)) {
			state.bash = null;
			return;
		}
		int elapsed = (int) Math.max(1L, now - bash.startedAt + 1L);
		double progress = Mth.clamp(elapsed / (double) BASH_TRAVEL_TICKS, 0.0D, 1.0D);
		Vec3 desired = bash.start.lerp(bash.end, progress);
		Vec3 movement = new Vec3(
				desired.x - player.getX(), 0.0D, desired.z - player.getZ());
		AABB next = player.getBoundingBox().move(movement);
		boolean safe = isLoadedAndWithinBorder(player.serverLevel(), next)
				&& player.serverLevel().noCollision(player, next);
		if (safe && movement.lengthSqr() > EPSILON) {
			player.move(MoverType.SELF, movement);
			player.hurtMarked = true;
		}

		if (!bash.hit && bash.targetId != null) {
			Entity entity = player.serverLevel().getEntity(bash.targetId);
			if (entity instanceof LivingEntity target
					&& validEnemy(player, target, false)
					&& (player.getBoundingBox().inflate(0.7D).intersects(target.getBoundingBox())
							|| progress >= 1.0D && player.distanceToSqr(target) <= 4.0D))
				performBashHit(player, state, bash, target, now);
		} else if (!bash.hit) {
			// No victim was locked at cast time. The charge still happens, so it
			// has to connect with whatever it actually runs into -- otherwise a
			// bash aimed a few degrees off its target is a dash that deals
			// nothing and reads as the skill being broken.
			LivingEntity struck = player.serverLevel()
					.getEntitiesOfClass(LivingEntity.class,
							player.getBoundingBox().inflate(0.7D),
							candidate -> validEnemy(player, candidate, false))
					.stream()
					.min(java.util.Comparator.comparingDouble(player::distanceToSqr))
					.orElse(null);
			if (struck != null)
				performBashHit(player, state, bash, struck, now);
		}
		if (!safe || progress >= 1.0D)
			state.bash = null;
	}

	private static void performBashHit(ServerPlayer player, TankerState state,
			BashState bash, LivingEntity target, long now) {
		bash.hit = true;
		CombatCategory category = offensiveCategory(target);
		float damage = (float) (tankerPower(player) * switch (category) {
			case NORMAL -> 0.70D;
			case BOSS -> 0.50D;
			case PVP -> 0.40D;
		});
		if (!dealTankerDamage(player, target, damage))
			return;

		switch (category) {
			case NORMAL -> {
				if (target instanceof Mob mob) {
					target.stopUsingItem();
					mob.getNavigation().stop();
					mob.setAggressive(false);
				}
				applySlow(player, target, 0.80D, 18, player.serverLevel());
				target.knockback(0.75D,
						player.getX() - target.getX(), player.getZ() - target.getZ());
			}
			case BOSS -> applySlow(player, target, 0.20D, 6, player.serverLevel());
			case PVP -> {
				applySlow(player, target, 0.25D, 8, player.serverLevel());
				target.knockback(0.25D,
						player.getX() - target.getX(), player.getZ() - target.getZ());
			}
		}
		if (isTauntedBy(player, state, target, now))
			refreshIronWall(player, now);

		double relief = 0.0D;
		WillpowerState willpower = state.willpower;
		if (willpower != null && willpower.active) {
			StrainReliefResult reliefResult = calculateShieldBashRelief(
					willpower.strain, willpower.maxHealthAtActivation,
					willpower.reliefUsed);
			relief = reliefResult.removedStrain();
			willpower.strain = reliefResult.remainingStrain();
			willpower.reliefUsed = reliefResult.reliefUsed();
			if (relief > EPSILON) {
				willpower.shieldBashStrainRelief += relief;
				persistWillpower(player, willpower);
			}
		}
		emit(player, VFX_BASH_HIT, target, target.position(), now, 6,
				100, confirmedHitFlags(category));
		if (relief > EPSILON) {
			emit(player, VFX_BASH_STRAIN_RELIEF, target, target.position(), now,
					8, vfxIntensityBand(thresholdIntensity(willpower)),
					VFX_FLAG_CONFIRMED_HIT);
		}
	}

	private static void tickTaunts(ServerPlayer player, TankerState state, long now) {
		if (state.taunts.isEmpty() || now < state.nextTauntMaintenance)
			return;
		state.nextTauntMaintenance = now + TAUNT_MAINTENANCE_INTERVAL;
		Iterator<Map.Entry<UUID, TauntEntry>> iterator = state.taunts.entrySet().iterator();
		while (iterator.hasNext()) {
			TauntEntry entry = iterator.next().getValue();
			TauntClaim claim = TAUNT_CLAIMS.get(entry.targetId);
			if (claim == null || !claim.taunterId.equals(player.getUUID())
					|| claim.level != player.level() || now >= entry.expiresAt) {
				iterator.remove();
				continue;
			}
			Entity entity = player.serverLevel().getEntity(entry.targetId);
			if (!(entity instanceof LivingEntity target)
					|| !target.isAlive() || target.distanceToSqr(player) > TAUNT_RANGE_SQR
					|| !validTauntTarget(player, target)) {
				iterator.remove();
				TAUNT_CLAIMS.remove(entry.targetId, claim);
				if (entry.category == CombatCategory.PVP)
					CHALLENGED.remove(entry.targetId);
				continue;
			}
			if (target instanceof Mob mob) {
				if (entry.category == CombatCategory.NORMAL) {
					mob.setTarget(player);
				} else if (entry.category == CombatCategory.BOSS) {
					LivingEntity current = mob.getTarget();
					if (current == null || !current.isAlive() || !mob.canAttack(current))
						mob.setTarget(player);
				}
			}
		}
	}

	private static void clearTaunts(ServerPlayer player, TankerState state) {
		if (state == null || state.taunts.isEmpty())
			return;
		for (UUID targetId : state.taunts.keySet()) {
			TauntClaim claim = TAUNT_CLAIMS.get(targetId);
			if (claim != null && claim.taunterId.equals(player.getUUID()))
				TAUNT_CLAIMS.remove(targetId);
			ChallengedState challenged = CHALLENGED.get(targetId);
			if (challenged != null && challenged.taunterId.equals(player.getUUID()))
				CHALLENGED.remove(targetId);
		}
		state.taunts.clear();
	}

	private static boolean isTauntedBy(ServerPlayer player, TankerState state,
			LivingEntity target, long now) {
		TauntEntry entry = state.taunts.get(target.getUUID());
		TauntClaim claim = TAUNT_CLAIMS.get(target.getUUID());
		return entry != null && now < entry.expiresAt && claim != null
				&& claim.taunterId.equals(player.getUUID());
	}

	private static void expireReinforcement(ServerPlayer player,
			TankerState state, long now) {
		if (state.reinforcementPhase == ReinforcementPhase.NONE
				|| now < state.reinforcementExpiresAt)
			return;
		endReinforcement(player, state, now, false);
	}

	private static void endReinforcement(ServerPlayer player, TankerState state,
			long now, boolean canceled) {
		ReinforcementPhase old = state.reinforcementPhase;
		state.reinforcementPhase = ReinforcementPhase.NONE;
		state.reinforcementExpiresAt = 0L;
		state.pendingKnockback = null;
		if (old == ReinforcementPhase.STANCE) {
			emit(player, VFX_STANCE_END, null, player.position(), now,
					6, canceled ? 0 : 100, VFX_FLAG_ESSENTIAL);
		}
	}

	private static void clearActiveCombatForClassChange(ServerPlayer player,
			TankerState state, long now) {
		clearMovement(player, state);
		clearTaunts(player, state);
		endReinforcement(player, state, now, true);
		if (state.willpower != null && state.willpower.active)
			beginSettlement(player, state.willpower, now, false);
		removeMark(player.getUUID(), MarkEnd.CANCEL);
		removeChallengesFor(player.getUUID());
	}

	private static void clearMovement(ServerPlayer player, TankerState state) {
		clearLeap(player, state);
		state.bash = null;
	}

	private static void refreshMarkBeneficiaries(ServerPlayer owner,
			ProtectionZone zone, long now) {
		zone.nextMembershipUpdate = now + MARK_MEMBERSHIP_INTERVAL;
		LinkedHashSet<UUID> beneficiaries = new LinkedHashSet<>();
		if (validMarkPlayer(owner, owner, zone))
			beneficiaries.add(owner.getUUID());
		String party = party(owner);
		if (!party.isBlank()) {
			List<ServerPlayer> candidates = zone.level.getEntitiesOfClass(
							ServerPlayer.class,
							new AABB(zone.center, zone.center).inflate(MARK_RADIUS),
							player -> player != owner && validMarkPlayer(owner, player, zone)
									&& party.equals(party(player)))
					.stream()
					.sorted(Comparator
							.comparingDouble((ServerPlayer player) ->
									player.distanceToSqr(zone.center))
							.thenComparing(player -> player.getUUID().toString()))
					.limit(MARK_BENEFICIARY_CAP - 1L)
					.toList();
			for (ServerPlayer candidate : candidates)
				beneficiaries.add(candidate.getUUID());
		}
		zone.beneficiaries = beneficiaries;
	}

	private static MarkSelection selectProtectionMark(ServerPlayer victim,
			CombatCategory category) {
		List<MarkSelection> eligible = new ArrayList<>();
		for (ProtectionZone zone : MARKS.values()) {
			if (zone.level != victim.level() || zone.integrity <= EPSILON
					|| victim.level().getGameTime() >= zone.expiresAt
					|| !zone.beneficiaries.contains(victim.getUUID()))
				continue;
			ServerPlayer owner = victim.server.getPlayerList().getPlayer(zone.ownerId);
			if (owner == null || !isTanker(owner)
					|| !validMarkPlayer(owner, victim, zone))
				continue;
			boolean isOwner = victim.getUUID().equals(zone.ownerId);
			double reduction = markReduction(isOwner, category);
			eligible.add(new MarkSelection(zone, reduction, isOwner));
		}
		return eligible.stream()
				.sorted(Comparator
						.comparingDouble((MarkSelection selection) -> selection.reduction)
						.reversed()
						.thenComparing(Comparator
								.comparingDouble((MarkSelection selection) ->
										selection.zone.integrity)
								.reversed())
						.thenComparing(selection -> selection.zone.ownerId.toString()))
				.findFirst()
				.orElse(null);
	}

	private static void drainMark(MarkSelection mark, ServerPlayer victim,
			ProtectionFundingResult funding) {
		ProtectionZone zone = mark.zone;
		double before = zone.integrity;
		zone.integrity = funding.remainingIntegrity();
		if (!mark.owner && !zone.allyMasteryTriggered) {
			zone.allyMasteryTriggered = true;
			ServerPlayer owner = victim.server.getPlayerList().getPlayer(zone.ownerId);
			if (owner != null)
				tryAwardMastery(owner, MasteryTrigger.PROTECTION_MARK_ALLY);
		}
		double maximum = zone.initialIntegrity;
		if (maximum > EPSILON) {
			int previousBand = integrityBand(before / maximum);
			int currentBand = integrityBand(zone.integrity / maximum);
			while (previousBand < currentBand) {
				previousBand++;
				emit(zone, VFX_MARK_THRESHOLD, victim, victim.level().getGameTime(),
						8, vfxIntensityBand(previousBand), 0);
			}
		}
		if (zone.integrity <= EPSILON)
			removeMark(zone.ownerId, MarkEnd.BREAK);
	}

	private static int integrityBand(double ratio) {
		if (ratio <= 0.25D + EPSILON)
			return 3;
		if (ratio <= 0.50D + EPSILON)
			return 2;
		if (ratio <= 0.75D + EPSILON)
			return 1;
		return 0;
	}

	private static void removeMark(UUID ownerId, MarkEnd reason) {
		ProtectionZone removed = MARKS.remove(ownerId);
		if (removed == null)
			return;
		byte type = reason == MarkEnd.BREAK ? VFX_MARK_BREAK : VFX_MARK_CANCEL;
		emit(removed, type, null, removed.level.getGameTime(), 8,
				reason == MarkEnd.BREAK ? 255 : 0, VFX_FLAG_ESSENTIAL);
	}

	private static void applySlow(ServerPlayer owner, LivingEntity target,
			double fraction, int durationTicks, ServerLevel level) {
		if (owner == null || target == null || durationTicks <= 0
				|| fraction <= 0.0D)
			return;
		long now = level.getGameTime();
		long expiry = level.getGameTime() + durationTicks;
		SlowState state = ACTIVE_SLOWS.get(target.getUUID());
		if (state == null || state.level != level) {
			if (state != null)
				removeSlowModifier(target);
			state = new SlowState(target.getUUID(), level);
			ACTIVE_SLOWS.put(target.getUUID(), state);
		}
		SlowContribution old = state.contributions.get(owner.getUUID());
		if (old != null && old.expiresAt > now) {
			fraction = Math.max(fraction, old.fraction);
			expiry = Math.max(expiry, old.expiresAt);
		}
		state.contributions.put(owner.getUUID(),
				new SlowContribution(expiry, fraction));
		if (!refreshSlowModifier(target, state, now))
			ACTIVE_SLOWS.remove(target.getUUID());
	}

	private static void tickSlows(MinecraftServer server) {
		Iterator<Map.Entry<UUID, SlowState>> iterator = ACTIVE_SLOWS.entrySet().iterator();
		while (iterator.hasNext()) {
			SlowState slow = iterator.next().getValue();
			Entity entity = slow.level.getEntity(slow.targetId);
			long now = slow.level.getGameTime();
			if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
				if (entity instanceof LivingEntity living)
					removeSlowModifier(living);
				iterator.remove();
			} else if (!refreshSlowModifier(living, slow, now)) {
				iterator.remove();
			}
		}
	}

	private static void removeOwnedSlows(UUID ownerId) {
		Iterator<Map.Entry<UUID, SlowState>> iterator =
				ACTIVE_SLOWS.entrySet().iterator();
		while (iterator.hasNext()) {
			SlowState slow = iterator.next().getValue();
			if (slow.contributions.remove(ownerId) == null)
				continue;
			Entity entity = slow.level.getEntity(slow.targetId);
			if (entity instanceof LivingEntity living) {
				if (!refreshSlowModifier(living, slow,
						slow.level.getGameTime()))
					iterator.remove();
			} else if (slow.contributions.isEmpty()) {
				iterator.remove();
			}
		}
	}

	private static void removeSlow(Entity entity) {
		if (!(entity instanceof LivingEntity living))
			return;
		ACTIVE_SLOWS.remove(entity.getUUID());
		removeSlowModifier(living);
	}

	private static void removeSlowModifier(LivingEntity living) {
		AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null)
			speed.removeModifier(CONTROL_SLOW_MODIFIER_ID);
	}

	private static boolean refreshSlowModifier(LivingEntity living,
			SlowState state, long now) {
		state.contributions.entrySet().removeIf(entry ->
				entry.getValue().expiresAt <= now);
		if (state.contributions.isEmpty()) {
			removeSlowModifier(living);
			return false;
		}
		double fraction = state.contributions.values().stream()
				.mapToDouble(SlowContribution::fraction)
				.max().orElse(0.0D);
		AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null)
			return false;
		AttributeModifier current = speed.getModifier(CONTROL_SLOW_MODIFIER_ID);
		double clamped = Mth.clamp(fraction, 0.0D, 0.95D);
		if (current == null || Math.abs(current.amount() + clamped) > EPSILON) {
			speed.removeModifier(CONTROL_SLOW_MODIFIER_ID);
			speed.addTransientModifier(new AttributeModifier(
					CONTROL_SLOW_MODIFIER_ID, -clamped,
					AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		}
		return true;
	}

	private static void pruneClaims(MinecraftServer server) {
		Iterator<Map.Entry<UUID, TauntClaim>> iterator = TAUNT_CLAIMS.entrySet().iterator();
		while (iterator.hasNext()) {
			TauntClaim claim = iterator.next().getValue();
			if (claim.level.getGameTime() >= claim.expiresAt
					|| claim.level.getEntity(claim.targetId) == null)
				iterator.remove();
		}
	}

	private static void pruneChallenges(MinecraftServer server) {
		Iterator<Map.Entry<UUID, ChallengedState>> iterator = CHALLENGED.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ChallengedState> entry = iterator.next();
			ChallengedState challenged = entry.getValue();
			ServerPlayer target = server.getPlayerList().getPlayer(entry.getKey());
			ServerPlayer taunter = server.getPlayerList().getPlayer(challenged.taunterId);
			if (challenged.level.getGameTime() >= challenged.expiresAt
					|| target == null || taunter == null || !target.isAlive()
					|| !taunter.isAlive() || target.level() != challenged.level
					|| taunter.level() != challenged.level)
				iterator.remove();
		}
	}

	private static void removeChallengesFor(UUID playerId) {
		CHALLENGED.entrySet().removeIf(entry ->
				entry.getKey().equals(playerId)
						|| entry.getValue().taunterId.equals(playerId));
		TAUNT_CLAIMS.entrySet().removeIf(entry ->
				entry.getValue().taunterId.equals(playerId));
	}

	private static boolean dealTankerDamage(ServerPlayer player,
			LivingEntity target, float amount) {
		if (amount <= 0.0F || !validEnemy(player, target, false))
			return false;
		boolean masteryAvailable = !CooldownManager.isOnCooldown(player, MASTERY_LOCK);
		if (masteryAvailable)
			CooldownManager.setFullDuration(player, MASTERY_LOCK, 10);
		boolean damaged = target.hurt(tankerDamageSource(player.serverLevel(), player), amount);
		if (masteryAvailable) {
			if (damaged)
				addMasteryCredit(player, MasteryTrigger.TANKER_DAMAGE);
			else
				CooldownManager.clear(player, MASTERY_LOCK);
		}
		return damaged;
	}

	private static boolean tryAwardMastery(ServerPlayer player, MasteryTrigger trigger) {
		if (player == null || !isTanker(player)
				|| CooldownManager.isOnCooldown(player, MASTERY_LOCK))
			return false;
		CooldownManager.setFullDuration(player, MASTERY_LOCK, 10);
		addMasteryCredit(player, trigger);
		return true;
	}

	private static void addMasteryCredit(ServerPlayer player, MasteryTrigger trigger) {
		final boolean[] unlock = {false};
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(vars -> {
					vars.progression_tanker += 1.0D;
					double threshold = vars.progression_multiplier_tanker * 7.0D;
					if (vars.progression_tanker > threshold) {
						vars.progression_tanker = 0.0D;
						vars.progression_multiplier_tanker += 1.0D;
						unlock[0] = true;
					}
					vars.syncPlayerVariables(player);
				});
		if (unlock[0])
			grantNextMasterySkill(player);
	}

	private static void stripLegacyEffects(ServerPlayer player) {
		// wp is cleared before WILL_POWER removal because that legacy effect's
		// removal callback otherwise deals surprise feedback damage.
		removeEffect(player, SololevelingModMobEffects.FORTIFY);
		removeEffect(player, SololevelingModMobEffects.WILL_POWER);
		removeEffect(player, SololevelingModMobEffects.SHIELD_BASH_EFFECT);
		removeEffect(player, SololevelingModMobEffects.WILLPOWER_COOLDOWN);
		removeEffect(player, SololevelingModMobEffects.SHIELD_BASH_COOLDOWN);
		removeEffect(player, SololevelingModMobEffects.TAUNT_COOLDOWN);
	}

	private static void removeEffect(LivingEntity entity, Holder<MobEffect> effect) {
		if (effect != null && entity.hasEffect(effect))
			entity.removeEffect(effect);
	}

	private static int currentIronWall(ServerPlayer player, long now) {
		CompoundTag data = player.getPersistentData();
		int stacks = Mth.clamp(data.getInt(IRON_STACKS_TAG), 0, IRON_WALL_MAX);
		if (stacks > 0 && now >= data.getLong(IRON_EXPIRES_TAG)) {
			data.putInt(IRON_STACKS_TAG, 0);
			data.remove(IRON_EXPIRES_TAG);
			data.remove(IRON_LAST_DAMAGE_TICK_TAG);
			syncIronWall(player, 0);
			return 0;
		}
		if (data.getInt(IRON_STACKS_TAG) != stacks)
			data.putInt(IRON_STACKS_TAG, stacks);
		return stacks;
	}

	private static void grantDamageIronWall(ServerPlayer player, long now) {
		CompoundTag data = player.getPersistentData();
		if (data.contains(IRON_LAST_DAMAGE_TICK_TAG)
				&& data.getLong(IRON_LAST_DAMAGE_TICK_TAG) == now)
			return;
		data.putLong(IRON_LAST_DAMAGE_TICK_TAG, now);
		addIronWall(player, 1, now);
	}

	private static void addIronWall(ServerPlayer player, int amount, long now) {
		if (amount <= 0 || !isTanker(player))
			return;
		CompoundTag data = player.getPersistentData();
		int oldStacks = currentIronWall(player, now);
		int stacks = Mth.clamp(oldStacks + amount, 0, IRON_WALL_MAX);
		data.putInt(IRON_STACKS_TAG, stacks);
		data.putLong(IRON_EXPIRES_TAG, now + IRON_WALL_DURATION);
		if (stacks != oldStacks)
			syncIronWall(player, stacks);
	}

	private static void refreshIronWall(ServerPlayer player, long now) {
		CompoundTag data = player.getPersistentData();
		if (currentIronWall(player, now) > 0)
			data.putLong(IRON_EXPIRES_TAG, now + IRON_WALL_DURATION);
	}

	private static void clearIronWall(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		boolean changed = data.getInt(IRON_STACKS_TAG) != 0;
		data.putInt(IRON_STACKS_TAG, 0);
		data.remove(IRON_EXPIRES_TAG);
		data.remove(IRON_LAST_DAMAGE_TICK_TAG);
		data.remove(LEGACY_IRON_TIMER_TAG);
		if (changed)
			syncIronWall(player, 0);
	}

	private static void syncIronWall(ServerPlayer player, int stacks) {
		SololevelingMod.PACKET_HANDLER.send(
				PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(2, Mth.clamp(stacks, 0, IRON_WALL_MAX)));
	}

	private static void persistWillpower(ServerPlayer player, WillpowerState willpower) {
		CompoundTag data = player.getPersistentData();
		if (willpower == null || willpower.strain <= EPSILON
				&& !willpower.active && !willpower.settling) {
			clearPersistedWillpower(player);
			return;
		}
		data.putDouble(WP_DEBT_TAG, Math.max(0.0D, willpower.strain));
		data.putDouble(WP_MAX_HEALTH_TAG, willpower.maxHealthAtActivation);
		data.putInt(WP_PULSES_TAG, willpower.settling
				? Mth.clamp(willpower.pulsesRemaining, 1, WILLPOWER_PULSES)
				: WILLPOWER_PULSES);
	}

	private static void loadPendingSettlement(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		double debt = Math.max(0.0D, data.getDouble(WP_DEBT_TAG));
		if (debt <= EPSILON) {
			clearPersistedWillpower(player);
			return;
		}
		int pulses = Mth.clamp(data.getInt(WP_PULSES_TAG), 1, WILLPOWER_PULSES);
		double maximumHealth = Math.max(1.0D,
				data.getDouble(WP_MAX_HEALTH_TAG));
		TankerState state = state(player);
		state.willpower = WillpowerState.settling(
				debt, maximumHealth, pulses,
				player.level().getGameTime() + WILLPOWER_PULSE_INTERVAL);
	}

	private static void clearPersistedWillpower(ServerPlayer player) {
		CompoundTag data = player.getPersistentData();
		data.remove(WP_DEBT_TAG);
		data.remove(WP_MAX_HEALTH_TAG);
		data.remove(WP_PULSES_TAG);
	}

	private static CombatContext combatContext(ServerPlayer victim, DamageSource source) {
		if (source == null || source.is(RuntimeKeys.WILLPOWER_STRAIN_DAMAGE)
				|| source.is(DamageTypes.FALL) || source.is(DamageTypes.DROWN)
				|| source.is(DamageTypes.STARVE)
				|| source.is(DamageTypes.FELL_OUT_OF_WORLD)
				|| source.is(DamageTypes.GENERIC_KILL))
			return null;
		Entity owner = resolveSourceOwner(source);
		if (owner == null || owner == victim || owner.getUUID().equals(victim.getUUID())
				|| MageCombatHelper.areAllied(owner, victim))
			return null;
		if (owner instanceof Player attacker && !attacker.canHarmPlayer(victim))
			return null;
		CombatCategory category = owner instanceof Player
				? CombatCategory.PVP
				: owner instanceof LivingEntity living && isBoss(living)
						? CombatCategory.BOSS : CombatCategory.NORMAL;
		return new CombatContext(owner, category);
	}

	private static Entity resolveSourceOwner(DamageSource source) {
		if (source == null)
			return null;
		Entity entity = source.getEntity();
		if (entity == null)
			entity = source.getDirectEntity();
		return resolveOwner(entity);
	}

	private static Entity resolveOwner(Entity entity) {
		Entity current = entity;
		for (int depth = 0; depth < 4 && current != null; depth++) {
			Entity owner = null;
			if (current instanceof Projectile projectile)
				owner = projectile.getOwner();
			if (owner == null && current instanceof OwnableEntity ownable)
				owner = ownable.getOwner();
			if (owner == null) {
				UUID shadowOwner = ShadowMonarchManager.getShadowOwnerUUID(current);
				if (shadowOwner != null && current.getServer() != null)
					owner = current.getServer().getPlayerList().getPlayer(shadowOwner);
			}
			if (owner == null || owner == current)
				break;
			current = owner;
		}
		return current;
	}

	private static boolean validEnemy(ServerPlayer source, LivingEntity target,
			boolean requireLineOfSight) {
		if (source == null || target == null || target == source
				|| target instanceof ArmorStand || !target.isAlive()
				|| !target.isAttackable() || target.isInvulnerable())
			return false;
		if (!MageCombatHelper.isValidTarget(source, target))
			return false;
		if (requireLineOfSight && !source.hasLineOfSight(target))
			return false;
		return true;
	}

	private static boolean validTauntTarget(ServerPlayer source, LivingEntity target) {
		if (!validEnemy(source, target, false))
			return false;
		if (target instanceof ServerPlayer)
			return true;
		return target instanceof Mob mob && mob.canAttack(source);
	}

	private static boolean validMarkPlayer(ServerPlayer owner, ServerPlayer candidate,
			ProtectionZone zone) {
		if (owner == null || candidate == null || zone == null
				|| !candidate.isAlive() || candidate.isCreative()
				|| candidate.isSpectator() || candidate.level() != zone.level
				|| candidate.distanceToSqr(zone.center) > MARK_RADIUS_SQR)
			return false;
		if (candidate == owner)
			return true;
		String ownerParty = party(owner);
		return !ownerParty.isBlank() && ownerParty.equals(party(candidate));
	}

	private static boolean isBoss(LivingEntity entity) {
		return !(entity instanceof Player)
				&& (entity.getType().is(RuntimeKeys.BOSS_TAG)
						|| entity.getMaxHealth() >= 250.0F);
	}

	private static CombatCategory offensiveCategory(LivingEntity target) {
		if (target instanceof Player)
			return CombatCategory.PVP;
		return isBoss(target) ? CombatCategory.BOSS : CombatCategory.NORMAL;
	}

	private static double tankerPower(ServerPlayer player) {
		SololevelingModVariables.PlayerVariables vars = variables(player);
		return Math.max(4.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE)
				+ TemporaryStatBonusManager.effectiveStrength(player) / 14.0D
				+ vars.Vitality / 28.0D);
	}

	public static double ironWallReduction(int stacks, CombatCategory category) {
		Objects.requireNonNull(category, "category");
		double perStack = category == CombatCategory.PVP
				? IRON_WALL_PVP_PER_STACK : IRON_WALL_PVE_PER_STACK;
		double maximum = category == CombatCategory.PVP ? 0.10D : 0.20D;
		return Math.min(maximum, Mth.clamp(stacks, 0, IRON_WALL_MAX) * perStack);
	}

	public static double reinforcementReduction(ReinforcementPhase phase,
			CombatCategory category) {
		Objects.requireNonNull(category, "category");
		if (phase == null)
			phase = ReinforcementPhase.NONE;
		return switch (phase) {
			case PERFECT -> switch (category) {
				case NORMAL -> 0.60D;
				case BOSS -> 0.50D;
				case PVP -> 0.35D;
			};
			case STANCE -> switch (category) {
				case NORMAL -> 0.25D;
				case BOSS -> 0.20D;
				case PVP -> 0.15D;
			};
			case NONE -> 0.0D;
		};
	}

	public static double knockbackMultiplier(ReinforcementPhase phase,
			CombatCategory category) {
		Objects.requireNonNull(category, "category");
		if (phase == ReinforcementPhase.PERFECT)
			return category == CombatCategory.PVP ? 0.50D : 0.20D;
		if (phase == ReinforcementPhase.STANCE)
			return category == CombatCategory.PVP ? 0.70D : 0.50D;
		return 1.0D;
	}

	public static double customReductionCap(CombatCategory category) {
		Objects.requireNonNull(category, "category");
		return switch (category) {
			case NORMAL -> 0.65D;
			case BOSS -> 0.55D;
			case PVP -> 0.45D;
		};
	}

	public static double retainedDamage(double incomingDamage, CombatCategory category,
			int ironWallStacks, double protectionMarkReduction,
			ReinforcementPhase reinforcementPhase) {
		double incoming = finiteNonNegative(incomingDamage);
		double mark = Mth.clamp(finiteNonNegative(protectionMarkReduction), 0.0D, 1.0D);
		double layered = incoming
				* (1.0D - ironWallReduction(ironWallStacks, category))
				* (1.0D - mark)
				* (1.0D - reinforcementReduction(reinforcementPhase, category));
		return Math.max(incoming * (1.0D - customReductionCap(category)), layered);
	}

	private static double delayedShare(CombatCategory category) {
		return switch (category) {
			case NORMAL -> 0.50D;
			case BOSS -> 0.40D;
			case PVP -> 0.30D;
		};
	}

	private static double strainCapFraction(CombatCategory category) {
		return switch (category) {
			case NORMAL -> 0.40D;
			case BOSS -> 0.35D;
			case PVP -> 0.25D;
		};
	}

	public static WillpowerHitResult calculateWillpowerHit(double retainedDamage,
			double currentStrain, double currentCap, double maxHealthAtActivation,
			CombatCategory category) {
		Objects.requireNonNull(category, "category");
		double retained = finiteNonNegative(retainedDamage);
		double strain = finiteNonNegative(currentStrain);
		double maximumHealth = finiteNonNegative(maxHealthAtActivation);
		double priorCap = Double.isFinite(currentCap)
				? Math.max(0.0D, currentCap) : Double.POSITIVE_INFINITY;
		double activeCap = Math.min(priorCap,
				maximumHealth * strainCapFraction(category));
		if (strain + EPSILON >= activeCap) {
			return new WillpowerHitResult(
					retained, 0.0D, strain, activeCap, true);
		}
		double requestedDelay = retained * delayedShare(category);
		double delayed = Math.min(requestedDelay,
				Math.max(0.0D, activeCap - strain));
		double resultingStrain = strain + delayed;
		return new WillpowerHitResult(
				retained - delayed,
				delayed,
				resultingStrain,
				activeCap,
				resultingStrain + EPSILON >= activeCap);
	}

	public static StrainReliefResult calculateShieldBashRelief(double currentStrain,
			double maxHealthAtActivation, boolean reliefAlreadyUsed) {
		double strain = finiteNonNegative(currentStrain);
		if (reliefAlreadyUsed || strain <= EPSILON)
			return new StrainReliefResult(0.0D, strain, reliefAlreadyUsed);
		double relief = Math.min(strain,
				finiteNonNegative(maxHealthAtActivation) * WILLPOWER_RELIEF_FRACTION);
		if (relief <= EPSILON)
			return new StrainReliefResult(0.0D, strain, false);
		return new StrainReliefResult(relief, strain - relief, true);
	}

	public static double[] settlementPulses(double unpaidStrain) {
		double debt = finiteNonNegative(unpaidStrain);
		double[] pulses = new double[WILLPOWER_PULSES];
		double regularPulse = debt / WILLPOWER_PULSES;
		for (int index = 0; index < WILLPOWER_PULSES - 1; index++)
			pulses[index] = regularPulse;
		pulses[WILLPOWER_PULSES - 1] =
				Math.max(0.0D, debt - regularPulse * (WILLPOWER_PULSES - 1));
		return pulses;
	}

	public static double markReduction(boolean owner, CombatCategory category) {
		Objects.requireNonNull(category, "category");
		if (owner) {
			return switch (category) {
				case NORMAL -> 0.12D;
				case BOSS -> 0.10D;
				case PVP -> 0.08D;
			};
		}
		return switch (category) {
			case NORMAL -> 0.18D;
			case BOSS -> 0.15D;
			case PVP -> 0.10D;
		};
	}

	public static double protectionMarkInitialIntegrity(double ownerMaxHealth) {
		return finiteNonNegative(ownerMaxHealth) * MARK_INTEGRITY_FRACTION;
	}

	public static double protectionMarkDrainMultiplier(CombatCategory category) {
		Objects.requireNonNull(category, "category");
		return category == CombatCategory.BOSS ? 1.5D : 1.0D;
	}

	public static double protectionMarkRequestedPrevention(double incomingDamage,
			CombatCategory category, int ironWallStacks, double nominalMarkReduction,
			ReinforcementPhase reinforcementPhase) {
		double withoutMark = retainedDamage(incomingDamage, category,
				ironWallStacks, 0.0D, reinforcementPhase);
		double withMark = retainedDamage(incomingDamage, category,
				ironWallStacks, nominalMarkReduction, reinforcementPhase);
		return Math.max(0.0D, withoutMark - withMark);
	}

	public static ProtectionFundingResult calculateProtectionFunding(
			double requestedPrevention, double remainingIntegrity,
			CombatCategory category) {
		double requested = finiteNonNegative(requestedPrevention);
		double integrity = finiteNonNegative(remainingIntegrity);
		double multiplier = protectionMarkDrainMultiplier(category);
		double prevented = Math.min(requested, integrity / multiplier);
		double spent = Math.min(integrity, prevented * multiplier);
		double remaining = Math.max(0.0D, integrity - spent);
		return new ProtectionFundingResult(
				prevented,
				spent,
				remaining,
				requested > EPSILON && remaining <= EPSILON);
	}

	private static double finiteNonNegative(double value) {
		return Double.isFinite(value) ? Math.max(0.0D, value) : 0.0D;
	}

	private static Vec3 horizontalDirection(Entity entity) {
		Vec3 look = entity.getLookAngle();
		Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
		if (horizontal.lengthSqr() <= EPSILON) {
			double radians = Math.toRadians(entity.getYRot());
			horizontal = new Vec3(-Math.sin(radians), 0.0D, Math.cos(radians));
		}
		return horizontal.normalize();
	}

	private static Vec3 findSafeHorizontalEndpoint(ServerLevel level,
			ServerPlayer player, Vec3 start, Vec3 direction, double maximumDistance) {
		Vec3 lastSafe = start;
		for (double distance = 0.15D; distance <= maximumDistance + EPSILON;
				distance += 0.15D) {
			Vec3 candidate = start.add(direction.scale(Math.min(distance, maximumDistance)));
			AABB moved = player.getBoundingBox().move(candidate.subtract(start));
			if (!isLoadedAndWithinBorder(level, moved)
					|| !level.noCollision(player, moved))
				break;
			lastSafe = candidate;
		}
		return lastSafe;
	}

	private static boolean isLoadedAndWithinBorder(ServerLevel level, AABB box) {
		BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
		BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
		return level.hasChunkAt(min) && level.hasChunkAt(max)
				&& level.getWorldBorder().isWithinBounds(min)
				&& level.getWorldBorder().isWithinBounds(max);
	}

	private static Vec3 findGroundBelow(ServerLevel level, ServerPlayer player) {
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
				Mth.floor(player.getX()), Mth.floor(player.getY() - 0.05D),
				Mth.floor(player.getZ()));
		for (int offset = 0; offset <= 8; offset++) {
			cursor.setY(Mth.floor(player.getY() - 0.05D) - offset);
			if (!level.hasChunkAt(cursor)
					|| !level.getWorldBorder().isWithinBounds(cursor))
				return null;
			BlockState block = level.getBlockState(cursor);
			var shape = block.getCollisionShape(level, cursor);
			if (shape.isEmpty())
				continue;
			double top = cursor.getY() + shape.max(Direction.Axis.Y);
			Vec3 center = new Vec3(player.getX(), top + 0.01D, player.getZ());
			AABB markerSpace = new AABB(
					center.x - 0.25D, center.y, center.z - 0.25D,
					center.x + 0.25D, center.y + 1.0D, center.z + 0.25D);
			return level.noCollision(markerSpace) ? center : null;
		}
		return null;
	}

	private static double forwardProjection(Vec3 start, Vec3 direction, Vec3 point) {
		return point.subtract(start).dot(direction);
	}

	private static double horizontalDistance(Vec3 first, Vec3 second) {
		double x = first.x - second.x;
		double z = first.z - second.z;
		return Math.sqrt(x * x + z * z);
	}

	private static Comparator<LivingEntity> distanceThenUuid(Vec3 origin) {
		return Comparator
				.comparingDouble((LivingEntity target) -> target.distanceToSqr(origin))
				.thenComparing(target -> target.getUUID().toString());
	}

	private static DamageSource tankerDamageSource(ServerLevel level,
			ServerPlayer player) {
		return new DamageSource(level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(RuntimeKeys.TANKER_DAMAGE), player);
	}

	private static DamageSource strainDamageSource(ServerLevel level) {
		return new DamageSource(level.registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE)
				.getHolderOrThrow(RuntimeKeys.WILLPOWER_STRAIN_DAMAGE));
	}

	private static TankerState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), ignored -> new TankerState());
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(
						SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static boolean isTanker(Entity entity) {
		if (entity == null)
			return false;
		SololevelingModVariables.PlayerVariables vars = variables(entity);
		if (isTanker(vars))
			return true;
		for (String skill : SKILLS) {
			if (TankerProgressionRules.hasSkill(vars.Plist, skill))
				return true;
		}
		return false;
	}

	private static boolean isTanker(SololevelingModVariables.PlayerVariables vars) {
		return vars != null && (int) Math.round(vars.Classes) == TANKER_CLASS;
	}

	private static String party(Entity entity) {
		String party = variables(entity).party;
		return party == null ? "" : party.trim();
	}

	private static void message(ServerPlayer player, String text) {
		player.displayClientMessage(Component.literal(text), true);
	}

	private static int pvpFlag(CombatCategory category) {
		return category == CombatCategory.PVP ? VFX_FLAG_PVP : 0;
	}

	private static int confirmedHitFlags(CombatCategory category) {
		return VFX_FLAG_ESSENTIAL | VFX_FLAG_CONFIRMED_HIT | pvpFlag(category);
	}

	private static int vfxIntensityBand(int band) {
		return Mth.clamp(band, 0, 4) == 4
				? 255
				: Mth.clamp(band, 0, 4) * 64;
	}

	private static void emit(ServerPlayer owner, byte type, Entity target,
			Vec3 origin, long startTick, int duration, int intensity, int flags) {
		if (owner == null || !(owner.level() instanceof ServerLevel level))
			return;
		int targetId = target == null ? -1 : target.getId();
		vfxSink.emit(level, new VfxEvent(
				type, owner.getId(), targetId,
				origin.x, origin.y, origin.z,
				packRotation(owner.getYRot()), packRotation(owner.getXRot()),
				startTick, Math.max(0, duration),
				stableSeed(owner.getUUID(), type, startTick, targetId),
				Mth.clamp(intensity, 0, 255), flags & 0xFF));
	}

	private static void emit(ProtectionZone zone, byte type, Entity target,
			long startTick, int duration, int intensity, int flags) {
		int targetId = target == null ? -1 : target.getId();
		vfxSink.emit(zone.level, new VfxEvent(
				type, zone.ownerEntityId, targetId,
				zone.center.x, zone.center.y, zone.center.z,
				packRotation(zone.yaw), packRotation(zone.pitch),
				startTick, Math.max(0, duration),
				stableSeed(zone.ownerId, type, startTick, targetId),
				Mth.clamp(intensity, 0, 255), flags & 0xFF));
	}

	private static short packRotation(float degrees) {
		return (short) Mth.floor(Mth.wrapDegrees(degrees) * 65536.0F / 360.0F);
	}

	private static int stableSeed(UUID owner, byte type, long tick, int targetId) {
		return Objects.hash(owner.getMostSignificantBits(),
				owner.getLeastSignificantBits(), type, tick, targetId);
	}

	@FunctionalInterface
	public interface VfxSink {
		void emit(ServerLevel level, VfxEvent event);
	}

	public record VfxEvent(
			byte eventType,
			int ownerEntityId,
			int targetEntityId,
			double x,
			double y,
			double z,
			short yaw,
			short pitch,
			long serverStartTick,
			int duration,
			int seed,
			int intensity,
			int flags) {
	}

	public enum CombatCategory {
		NORMAL,
		BOSS,
		PVP
	}

	public enum ReinforcementPhase {
		NONE,
		PERFECT,
		STANCE
	}

	public enum TransientState {
		LEAP,
		SHIELD_BASH,
		TAUNT,
		REINFORCEMENT,
		WILLPOWER,
		PROTECTION_MARK,
		CHALLENGED,
		CONTROL_SLOW
	}

	public enum CleanupReason {
		DEATH,
		DIMENSION_CHANGE,
		CLASS_CHANGE,
		LOGOUT,
		SERVER_STOP
	}

	public enum CleanupAction {
		CLEAR,
		START_STRAIN_SETTLEMENT,
		PERSIST_STRAIN_SETTLEMENT
	}

	private enum MarkEnd {
		BREAK,
		CANCEL
	}

	private enum MasteryTrigger {
		TAUNT,
		TANKER_DAMAGE,
		REINFORCEMENT_BRACE,
		WILLPOWER_THRESHOLD,
		PROTECTION_MARK_ALLY
	}

	/**
	 * Defers registry-key construction until a live-server mechanic needs it.
	 * This keeps the public balance/migration helpers usable by pure regression
	 * checks without bootstrapping Minecraft registries.
	 */
	private static final class RuntimeKeys {
		private static final ResourceKey<DamageType> TANKER_DAMAGE =
				ResourceKey.create(Registries.DAMAGE_TYPE,
						ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "tanker"));
		private static final ResourceKey<DamageType> WILLPOWER_STRAIN_DAMAGE =
				ResourceKey.create(Registries.DAMAGE_TYPE,
						ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "willpower_strain"));
		private static final TagKey<net.minecraft.world.entity.EntityType<?>> BOSS_TAG =
				TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse("soloboss"));
		private static final TagKey<net.minecraft.world.item.Item> SHIELDS =
				ItemTags.create(ResourceLocation.fromNamespaceAndPath("minecraft", "shields"));

		private RuntimeKeys() {
		}
	}

	private record SkillBalance(
			int flatFloor,
			double maximumManaFraction,
			int cooldownTicks,
			int regenLockTicks) {
	}

	public record TargetOrder(
			UUID targetId,
			double primaryOrder,
			double squaredDistance) {
	}

	public record WillpowerHitResult(
			double immediateDamage,
			double strainAdded,
			double resultingStrain,
			double activeCap,
			boolean endsWillpower) {
	}

	public record StrainReliefResult(
			double removedStrain,
			double remainingStrain,
			boolean reliefUsed) {
	}

	public record ProtectionFundingResult(
			double preventedDamage,
			double integritySpent,
			double remainingIntegrity,
			boolean breaksField) {
	}

	private record CombatContext(Entity owner, CombatCategory category) {
	}

	private record MarkSelection(
			ProtectionZone zone,
			double reduction,
			boolean owner) {
	}

	private record PendingKnockback(long gameTick, double multiplier) {
	}

	private static final class TankerState {
		private LeapState leap;
		private BashState bash;
		private final LinkedHashMap<UUID, TauntEntry> taunts = new LinkedHashMap<>();
		private long nextTauntMaintenance;
		private ReinforcementPhase reinforcementPhase = ReinforcementPhase.NONE;
		private long reinforcementExpiresAt;
		private PendingKnockback pendingKnockback;
		private WillpowerState willpower;

		private boolean isEmpty() {
			return leap == null && bash == null && taunts.isEmpty()
					&& reinforcementPhase == ReinforcementPhase.NONE
					&& pendingKnockback == null && willpower == null;
		}
	}

	private static final class LeapState {
		private final ResourceKey<Level> dimension;
		private final long startedAt;
		private final long deadline;
		private final Vec3 start;
		private Vec3 lastSafe;
		private final Vec3 direction;
		private final LinkedHashSet<UUID> hitLedger = new LinkedHashSet<>();

		private LeapState(ResourceKey<Level> dimension, long startedAt,
				long deadline, Vec3 start, Vec3 lastSafe, Vec3 direction) {
			this.dimension = dimension;
			this.startedAt = startedAt;
			this.deadline = deadline;
			this.start = start;
			this.lastSafe = lastSafe;
			this.direction = direction;
		}
	}

	private static final class BashState {
		private final long startedAt;
		private final Vec3 start;
		private final Vec3 end;
		private final Vec3 direction;
		private final UUID targetId;
		private boolean hit;

		private BashState(long startedAt, Vec3 start, Vec3 end,
				Vec3 direction, UUID targetId) {
			this.startedAt = startedAt;
			this.start = start;
			this.end = end;
			this.direction = direction;
			this.targetId = targetId;
		}
	}

	private record TauntEntry(
			UUID targetId,
			CombatCategory category,
			long expiresAt) {
	}

	private static final class TauntClaim {
		private final UUID targetId;
		private final UUID taunterId;
		private final ServerLevel level;
		private final long expiresAt;

		private TauntClaim(UUID targetId, UUID taunterId, ServerLevel level, long expiresAt) {
			this.targetId = targetId;
			this.taunterId = taunterId;
			this.level = level;
			this.expiresAt = expiresAt;
		}
	}

	private record ChallengedState(
			UUID taunterId,
			ServerLevel level,
			long expiresAt) {
	}

	private static final class WillpowerState {
		private boolean active;
		private boolean settling;
		private long activeExpiresAt;
		private double maxHealthAtActivation;
		private double currentCap;
		private double strain;
		private boolean reliefUsed;
		private double shieldBashStrainRelief;
		private boolean masteryAwarded;
		private int lastThreshold;
		private int pulsesRemaining;
		private long nextPulseAt;
		private double totalRetained;
		private double totalImmediate;

		private static WillpowerState active(long expiresAt, double maximumHealth) {
			WillpowerState state = new WillpowerState();
			state.active = true;
			state.activeExpiresAt = expiresAt;
			state.maxHealthAtActivation = maximumHealth;
			state.currentCap = maximumHealth * 0.40D;
			return state;
		}

		private static WillpowerState settling(double debt, double maximumHealth,
				int pulses, long nextPulseAt) {
			WillpowerState state = new WillpowerState();
			state.settling = true;
			state.strain = debt;
			state.maxHealthAtActivation = maximumHealth;
			state.currentCap = maximumHealth * 0.40D;
			state.pulsesRemaining = pulses;
			state.nextPulseAt = nextPulseAt;
			return state;
		}
	}

	private static final class ProtectionZone {
		private final UUID ownerId;
		private final int ownerEntityId;
		private final ServerLevel level;
		private final ResourceKey<Level> dimension;
		private final Vec3 center;
		private final long startedAt;
		private final long expiresAt;
		private final double initialIntegrity;
		private final float yaw;
		private final float pitch;
		private double integrity;
		private long nextMembershipUpdate;
		private LinkedHashSet<UUID> beneficiaries = new LinkedHashSet<>();
		private boolean allyMasteryTriggered;

		private ProtectionZone(UUID ownerId, int ownerEntityId,
				ServerLevel level, ResourceKey<Level> dimension, Vec3 center,
				long startedAt, long expiresAt, double integrity,
				float yaw, float pitch) {
			this.ownerId = ownerId;
			this.ownerEntityId = ownerEntityId;
			this.level = level;
			this.dimension = dimension;
			this.center = center;
			this.startedAt = startedAt;
			this.expiresAt = expiresAt;
			this.integrity = integrity;
			this.initialIntegrity = integrity;
			this.yaw = yaw;
			this.pitch = pitch;
		}
	}

	private static final class SlowState {
		private final UUID targetId;
		private final ServerLevel level;
		private final Map<UUID, SlowContribution> contributions = new HashMap<>();

		private SlowState(UUID targetId, ServerLevel level) {
			this.targetId = targetId;
			this.level = level;
		}
	}

	private record SlowContribution(long expiresAt, double fraction) {
	}
}
