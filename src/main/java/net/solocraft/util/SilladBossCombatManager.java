package net.solocraft.util;

import net.solocraft.entity.SilladBossEntity;
import net.solocraft.entity.SilladBossEntity.Action;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative combat state machine for Sillad.
 *
 * <p>The implementation intentionally uses vanilla particles and sounds. That
 * keeps the preview readable with or without Iris shader packs and on macOS,
 * while each timeline still exposes an unambiguous wind-up, locked geometry,
 * impact, and recovery window.</p>
 */
public final class SilladBossCombatManager {
	private static final double ENCOUNTER_LEASH = 48.0D;
	private static final double PLAYER_SCAN_RANGE = 64.0D;
	private static final double PREFERRED_MINIMUM = 5.5D;
	private static final double PREFERRED_MAXIMUM = 13.0D;
	private static final String COOLDOWN_PREFIX = "slr_sillad_cd_";
	private static final String REWIND_IMMUNITY =
			"slr_sillad_rewind_immune_until";

	private static final Map<UUID, RuntimeState> STATES = new HashMap<>();
	private static final Map<UUID, FrostState> FROSTBITE = new HashMap<>();

	private SilladBossCombatManager() {
	}

	public static void tick(SilladBossEntity sillad) {
		if (sillad == null || !sillad.isAlive()
				|| !(sillad.level() instanceof ServerLevel level))
			return;
		RuntimeState state = STATES.computeIfAbsent(sillad.getUUID(),
				ignored -> new RuntimeState(sillad));
		long now = level.getGameTime();
		state.recentDamage *= 0.965F;
		SilladFrozenDomainManager.tick(sillad);
		SilladChillManager.tick(sillad);

		updateFrostbite(sillad, level, now);
		tickWhiteout(sillad, state, level, now);
		lockEncounterParticipants(sillad, level);
		LivingEntity target = chooseStrategicTarget(sillad, state, level, now);
		tickFrozenDomainPressure(sillad, level, now);

		int healthPhase = SilladBossRules.phaseForHealth(sillad.getHealth(),
				sillad.getMaxHealth());
		if (!sillad.isActing() && healthPhase > sillad.getCombatPhase()) {
			beginPhaseTransition(sillad, state,
					sillad.getCombatPhase() + 1, now);
		}

		if (sillad.isActing()) {
			tickActiveAction(sillad, state, level, target, now);
			return;
		}
		if (target == null) {
			sillad.setAggressive(false);
			sillad.getNavigation().stop();
			state.stalledTicks = 0;
			return;
		}

		if (sillad.getCombatPhase() >= 3) {
			if ((now + sillad.getId()) % 5L == 0L)
				emitSpiritualAura(level, sillad);
			if (state.whiteout == null && !sillad.hasUsedAbsoluteZero()
					&& (sillad.getHealth() <= sillad.getMaxHealth() * 0.25F
							|| now - sillad.getPhaseThreeStartedAt() >= 120L)) {
				sillad.markAbsoluteZeroUsed();
				beginAction(sillad, state, Action.ABSOLUTE_ZERO,
						target, now);
				return;
			}
		}

		boolean moving = maintainCombatPosition(sillad, state, target, now);
		trackStuckRecovery(sillad, state, target, moving, now);
		if (sillad.isActing() || now < state.nextDecisionAt)
			return;

		Action selected = selectAction(sillad, state, target, level, now);
		if (selected != Action.IDLE)
			beginAction(sillad, state, selected, target, now);
		else
			state.nextDecisionAt = now + decisionDelay(sillad);
	}

	/** Applies encounter scaling, hit caps, phase armor, and active Counter. */
	public static float modifyIncomingDamage(SilladBossEntity sillad,
			DamageSource source, float amount) {
		float adjusted = SilladBossRules.clampIncomingHit(amount);
		adjusted *= SilladBossRules.incomingDamageMultiplier(
				sillad.getEngagedPlayerCount());
		RuntimeState state = STATES.get(sillad.getUUID());
		if (sillad.getCombatAction() == Action.PHASE_TRANSITION
				|| sillad.getCombatAction() == Action.ABSOLUTE_ZERO)
			adjusted *= 0.50F;
		if (sillad.getCombatAction() == Action.FROST_COUNTER
				&& sillad.getActionTick() >= 8
				&& sillad.getActionTick() <= 28
				&& !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
			adjusted *= 0.35F;
			if (state != null && !state.counterTriggered) {
				state.counterTriggered = true;
				state.counterImpactTick = sillad.getActionTick() + 4;
				Entity attacker = strategicAttacker(sillad, source.getEntity());
				if (attacker instanceof LivingEntity living)
					state.counterAttackerId = living.getUUID();
			}
		}
		return Math.max(0.0F, adjusted);
	}

	/** Records threat after Minecraft accepted an incoming hit. */
	public static void recordIncomingDamage(SilladBossEntity sillad,
			DamageSource source, float amount) {
		RuntimeState state = STATES.computeIfAbsent(sillad.getUUID(),
				ignored -> new RuntimeState(sillad));
		Entity attacker = strategicAttacker(sillad, source.getEntity());
		if (attacker instanceof LivingEntity living) {
			state.threat.merge(living.getUUID(),
					Math.max(1.0F, amount) * 5.0F, Float::sum);
			state.retaliationUntil.put(living.getUUID(),
					sillad.level().getGameTime() + 240L);
			state.recentDamage += Math.max(0.0F, amount);
		}
	}

	public static void cleanup(SilladBossEntity sillad) {
		if (sillad == null)
			return;
		STATES.remove(sillad.getUUID());
		SilladFrozenDomainManager.cleanup(sillad);
		SilladIcePrisonManager.releaseAllForBoss(sillad);
		if (sillad.level() instanceof ServerLevel level) {
			List<UUID> remove = new ArrayList<>();
			for (Map.Entry<UUID, FrostState> entry : FROSTBITE.entrySet()) {
				FrostState frost = entry.getValue();
				if (!sillad.getUUID().equals(frost.ownerId))
					continue;
				Entity raw = level.getEntity(entry.getKey());
				if (raw instanceof LivingEntity living) {
					restoreFrozenTicks(living, frost);
					// The Monarch's winter dies with him; it must not follow the
					// party out of the arena.
					SilladChillManager.clear(living);
				}
				remove.add(entry.getKey());
			}
			remove.forEach(FROSTBITE::remove);
		}
	}

	private static void beginPhaseTransition(SilladBossEntity sillad,
			RuntimeState state, int nextPhase, long now) {
		state.transitionTargetPhase = Math.max(2, Math.min(3, nextPhase));
		state.resetActionRuntime();
		SilladFrozenDomainManager.requestExpansion(sillad,
				state.transitionTargetPhase);
		sillad.beginCombatAction(Action.PHASE_TRANSITION);
		play(sillad, SoundEvents.BEACON_ACTIVATE, 1.15F,
				state.transitionTargetPhase == 3 ? 0.58F : 0.78F);
		state.nextDecisionAt = now + (state.transitionTargetPhase == 3 ? 80L : 70L);
	}

	private static void beginAction(SilladBossEntity sillad,
			RuntimeState state, Action action, LivingEntity target, long now) {
		state.resetActionRuntime();
		state.targetId = target == null ? null : target.getUUID();
		state.targetPoint = target == null ? sillad.position()
				: target.position().add(0.0D, target.getBbHeight() * 0.45D, 0.0D);
		state.aim = horizontalDirection(sillad.position(), state.targetPoint,
				sillad.getLookAngle());
		state.actionOrigin = sillad.position();
		state.gapIndex = sillad.getRandom().nextInt(7);
		state.strafeDirection = sillad.getRandom().nextBoolean() ? 1 : -1;
		if (action == Action.FROST_STEP)
			state.stepDestination = findFrostStepDestination(sillad, target);
		sillad.beginCombatAction(action);
		setCooldown(sillad, action, now + cooldown(action));
		rememberAction(state, action);
		playWindup(sillad, action);
	}

	private static void finishAction(SilladBossEntity sillad,
			RuntimeState state, long now) {
		sillad.finishCombatAction();
		state.nextDecisionAt = Math.max(state.nextDecisionAt,
				now + decisionDelay(sillad));
		state.resetActionRuntime();
	}

	private static void tickActiveAction(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level,
			@Nullable LivingEntity currentTarget, long now) {
		int tick = sillad.advanceCombatAction();
		LivingEntity lockedTarget = resolveLiving(level, state.targetId);
		LivingEntity target = isValidCombatTarget(sillad, lockedTarget)
				? lockedTarget : currentTarget;
		switch (sillad.getCombatAction()) {
			case PHASE_TRANSITION -> tickPhaseTransition(sillad, state,
					level, tick, now);
			case FROST_CLEAVE -> tickFrostCleave(sillad, state, level,
					target, tick, now);
			case ICE_SPEAR -> tickIceSpear(sillad, state, level, target,
					tick, now);
			case FLASH_FREEZE -> tickFlashFreeze(sillad, state, level,
					target, tick, now);
			case FROZEN_PATH -> tickFrozenPath(sillad, state, level,
					target, tick, now);
			case FROST_COUNTER -> tickFrostCounter(sillad, state, level,
					target, tick, now);
			case STILLNESS_DECREE -> tickStillnessDecree(sillad, state,
					level, target, tick, now);
			case SPIRE_CAGE -> tickSpireCage(sillad, state, level, target,
					tick, now);
			case WHITEOUT_PROCESSION -> tickWhiteoutCast(sillad, state,
					level, target, tick, now);
			case WINTER_REMEMBERS -> tickWinterRemembers(sillad, state,
					level, target, tick, now);
			case CROWN_OF_WINTER -> tickCrownOfWinter(sillad, state,
					level, target, tick, now);
			case ABSOLUTE_ZERO -> tickAbsoluteZero(sillad, state, level,
					tick, now);
			case GLACIAL_EXECUTION -> tickGlacialExecution(sillad, state,
					level, target, tick, now);
			case FROST_STEP -> tickFrostStep(sillad, state, level, tick,
					now);
			case IDLE -> finishAction(sillad, state, now);
		}
	}

	private static void tickPhaseTransition(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, int tick, long now) {
		int duration = state.transitionTargetPhase == 3 ? 60 : 50;
		if ((tick & 1) == 0) {
			double domainRadius = SilladBossRules.frozenDomainRadius(
					state.transitionTargetPhase);
			double radius = 1.5D
					+ (domainRadius - 1.5D) * tick / duration;
			spawnRing(level, sillad.position().add(0.0D, 0.12D, 0.0D),
					radius, Math.max(18, Mth.ceil(radius * 2.2D)),
					ParticleTypes.SNOWFLAKE);
		}
		if (tick % 10 == 0)
			level.sendParticles(ParticleTypes.END_ROD, sillad.getX(),
					sillad.getEyeY(), sillad.getZ(), 12, 0.6D, 0.8D,
					0.6D, 0.035D);
		if (tick < duration)
			return;
		sillad.setCombatPhase(state.transitionTargetPhase);
		if (state.transitionTargetPhase == 3) {
			if (sillad.getAttribute(Attributes.MOVEMENT_SPEED) != null)
				sillad.getAttribute(Attributes.MOVEMENT_SPEED)
						.setBaseValue(0.46D);
			play(sillad, SoundEvents.WARDEN_SONIC_BOOM, 1.0F, 1.65F);
		} else {
			if (sillad.getAttribute(Attributes.MOVEMENT_SPEED) != null)
				sillad.getAttribute(Attributes.MOVEMENT_SPEED)
						.setBaseValue(0.43D);
			play(sillad, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.7F);
		}
		finishAction(sillad, state, now + 20L);
	}

	private static void tickFrostCleave(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 6) {
			combatFootwork(sillad, state, target, 0.10D, 0.025D);
			trackAim(sillad, state, target);
		} else if (tick >= 13 && tick <= 21)
			combatFootwork(sillad, state, target, 0.065D, -0.025D);
		if (tick >= 5 && tick <= 9)
			showCone(level, sillad.position().add(0.0D, 1.0D, 0.0D),
					state.aim, 4.6D, 62.0D, ParticleTypes.SNOWFLAKE);
		if (tick == 10) {
			sillad.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
			for (LivingEntity victim : targetsInCone(sillad,
					sillad.position(), state.aim, 4.6D, 68.0D)) {
				if (damage(sillad, victim, 20.0F, 1, true, false))
					knockAway(victim, sillad.position(), 0.45D, 0.12D);
			}
			play(sillad, SoundEvents.PLAYER_ATTACK_SWEEP, 1.1F, 0.62F);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK,
					sillad.getX() + state.aim.x * 2.0D,
					sillad.getY() + 1.0D,
					sillad.getZ() + state.aim.z * 2.0D,
					3, 0.8D, 0.35D, 0.8D, 0.0D);
		}
		if (tick >= 26)
			finishAction(sillad, state, now);
	}

	private static void tickIceSpear(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 13) {
			combatFootwork(sillad, state, target, 0.085D, 0.018D);
			trackAim(sillad, state, target);
		} else if (tick == 14) {
			trackAim(sillad, state, target);
		} else if (tick >= 23 && tick <= 40 || tick >= 50 && tick <= 62)
			combatFootwork(sillad, state, target, 0.070D, -0.010D);
		Vec3 start = sillad.getEyePosition().add(0.0D, -0.25D, 0.0D);
		Vec3 end = clippedEndpoint(level, sillad, start,
				start.add(state.aim.scale(26.0D)));
		state.spearEndpoint = end;
		if (tick >= 15 && tick <= 20)
			spawnLine(level, start, end, ParticleTypes.SNOWFLAKE,
					Math.max(8, 30 - tick), 0.02D);
		if (tick == 20) {
			hitLine(sillad, state, start, end, 0.85D, 22.0F, 2,
					true, false, false);
			play(sillad, SoundEvents.TRIDENT_THROW.value(), 1.15F, 0.72F);
			burst(level, end, 22);
		}
		if (tick >= 42 && tick <= 48 && tick % 2 == 0)
			spawnLine(level, end, start, ParticleTypes.END_ROD, 22, 0.01D);
		if (tick == 48) {
			hitLine(sillad, state, end, start, 0.75D, 9.0F, 1,
					false, false, true);
			play(sillad, SoundEvents.TRIDENT_RETURN, 1.0F, 1.35F);
		}
		if (tick >= 72)
			finishAction(sillad, state, now);
	}

	private static void tickFlashFreeze(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 10) {
			combatFootwork(sillad, state, target, 0.075D, -0.015D);
			trackAim(sillad, state, target);
		} else if (tick >= 20 && tick <= 32)
			combatFootwork(sillad, state, target, 0.080D, -0.025D);
		if (tick >= 11 && tick <= 17)
			showCone(level, sillad.position().add(0.0D, 0.15D, 0.0D),
					state.aim, 10.0D, 48.0D,
					tick % 2 == 0 ? ParticleTypes.END_ROD
							: ParticleTypes.SNOWFLAKE);
		if (tick == 18) {
			for (LivingEntity victim : targetsInCone(sillad,
					sillad.position(), state.aim, 10.0D, 52.0D))
				damage(sillad, victim, 16.0F, 3, true, true);
			play(sillad, SoundEvents.GLASS_PLACE, 1.25F, 0.58F);
			for (int ring = 2; ring <= 10; ring += 2)
				spawnArc(level, sillad.position().add(0.0D, 0.12D, 0.0D),
						state.aim, ring, 52.0D, ParticleTypes.SNOWFLAKE);
		}
		if (tick >= 40)
			finishAction(sillad, state, now);
	}

	private static void tickFrozenPath(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 7)
			trackAim(sillad, state, target);
		if (tick == 8) {
			Vec3 start = sillad.position().add(0.0D, 0.25D, 0.0D);
			state.pathEndpoint = clippedEndpoint(level, sillad, start,
					start.add(state.aim.scale(11.0D)));
		}
		if (tick <= 8) {
			Vec3 end = state.pathEndpoint != null ? state.pathEndpoint
					: sillad.position().add(state.aim.scale(11.0D));
			spawnLine(level, sillad.position().add(0.0D, 0.1D, 0.0D),
					new Vec3(end.x, sillad.getY() + 0.1D, end.z),
					ParticleTypes.END_ROD, 18, 0.0D);
		}
		if (tick >= 9 && tick <= 25) {
			Vec3 before = sillad.position();
			Vec3 movement = state.aim.scale(0.68D);
			sillad.move(MoverType.SELF, movement);
			Vec3 after = sillad.position();
			if (after.distanceToSqr(before) < 0.025D)
				state.pathBlocked = true;
			level.sendParticles(ParticleTypes.SNOWFLAKE, after.x,
					after.y + 0.2D, after.z, 8, 0.55D, 0.16D,
					0.55D, 0.025D);
			for (LivingEntity victim : nearbyTargets(sillad, after, 1.8D)) {
				if (state.primaryHits.add(victim.getUUID()))
					damage(sillad, victim, 18.0F, 2, true, false);
			}
			if (state.pathBlocked)
				sillad.getNavigation().stop();
		}
		if (tick == 9)
			play(sillad, SoundEvents.PLAYER_SPLASH_HIGH_SPEED, 1.0F, 1.5F);
		if (tick >= 38)
			finishAction(sillad, state, now);
	}

	private static void tickFrostCounter(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 7) {
			combatFootwork(sillad, state, target, 0.065D, -0.02D);
			trackAim(sillad, state, target);
		}
		if (tick >= 8 && tick <= 28 && (tick & 1) == 0)
			spawnRing(level, sillad.position().add(0.0D, 1.0D, 0.0D),
					1.45D, 14, ParticleTypes.END_ROD);
		if (state.counterTriggered && tick == state.counterImpactTick) {
			LivingEntity attacker = resolveLiving(level,
					state.counterAttackerId);
			if (attacker != null)
				trackAim(sillad, state, attacker);
			for (LivingEntity victim : targetsInCone(sillad,
					sillad.position(), state.aim, 7.0D, 65.0D))
				damage(sillad, victim, 20.0F, 2, true, true);
			play(sillad, SoundEvents.SHIELD_BLOCK, 1.3F, 0.55F);
			spawnArc(level, sillad.position().add(0.0D, 0.8D, 0.0D),
					state.aim, 6.5D, 65.0D, ParticleTypes.END_ROD);
		}
		if (tick >= (state.counterTriggered ? 44 : 36))
			finishAction(sillad, state, now);
	}

	private static void tickStillnessDecree(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 7) {
			combatFootwork(sillad, state, target, 0.06D, -0.018D);
			trackAim(sillad, state, target);
		} else if (tick <= 28)
			combatFootwork(sillad, state, target, 0.040D, -0.010D);
		Vec3 center = sillad.position().add(0.0D, 1.7D, 0.0D)
				.add(state.aim.scale(2.2D));
		if (tick >= 8 && tick <= 28) {
			showStillnessPlane(level, center, state.aim);
			captureProjectiles(sillad, state, level, center);
		}
		if (tick == 36 && state.capturedProjectiles > 0) {
			LivingEntity victim = target != null ? target
					: nearestPlayer(sillad, level);
			if (victim != null) {
				Vec3 start = center;
				Vec3 end = victim.getBoundingBox().getCenter();
				spawnLine(level, start, end, ParticleTypes.END_ROD, 18, 0.03D);
				damage(sillad, victim,
						Math.min(18.0F, state.capturedProjectiles * 6.0F),
						1, false, true);
			}
			play(sillad, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.5F);
		}
		if (tick >= 54)
			finishAction(sillad, state, now);
	}

	private static void tickSpireCage(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 24)
			combatFootwork(sillad, state, target, 0.050D,
					tick <= 9 ? 0.012D : -0.008D);
		if (tick <= 9 && target != null) {
			state.targetPoint = target.position();
			trackAim(sillad, state, target);
		}
		Vec3 center = new Vec3(state.targetPoint.x,
				state.targetPoint.y, state.targetPoint.z);
		if (tick == 8) {
			state.prisonTargets.clear();
			for (LivingEntity companion : prisonCandidates(sillad, state,
					level, now))
				state.prisonTargets.add(companion.getUUID());
		}
		if (tick >= 10 && tick <= 27 && (tick & 1) == 0)
			showSpireColumns(level, center, state.gapIndex, false);
		if (tick >= 10 && tick <= 27 && (tick & 1) == 0) {
			for (UUID targetId : state.prisonTargets) {
				LivingEntity companion = resolveLiving(level, targetId);
				if (companion == null)
					continue;
				spawnRing(level, companion.position().add(0.0D, 0.08D, 0.0D),
						Math.max(0.9D, companion.getBbWidth() * 0.85D),
						12, tick % 4 == 0 ? ParticleTypes.END_ROD
								: ParticleTypes.SNOWFLAKE);
				if (tick % 4 == 0)
					spawnLine(level, sillad.getEyePosition(),
							companion.getBoundingBox().getCenter(),
							ParticleTypes.SNOWFLAKE, 10, 0.01D);
			}
		}
		if (tick == 28) {
			List<Vec3> spires = spirePositions(center, state.gapIndex);
			showSpireColumns(level, center, state.gapIndex, true);
			for (LivingEntity victim : nearbyTargets(sillad, center, 7.0D)) {
				boolean touches = spires.stream().anyMatch(point ->
						horizontalDistanceSqr(victim.position(), point) <= 2.0D
							&& Math.abs(victim.getY() - point.y) <= 4.0D);
				if (touches && damage(sillad, victim, 20.0F, 2,
						true, true))
					victim.setDeltaMovement(victim.getDeltaMovement().add(
							0.0D, 0.55D, 0.0D));
			}
			List<LivingEntity> captured = new ArrayList<>();
			Set<UUID> stillEligible = new HashSet<>();
			for (LivingEntity companion : prisonCandidates(sillad, state,
					level, now))
				stillEligible.add(companion.getUUID());
			for (UUID targetId : state.prisonTargets) {
				LivingEntity companion = resolveLiving(level, targetId);
				if (companion != null && stillEligible.contains(targetId))
					captured.add(companion);
			}
			int prisoned = SilladIcePrisonManager.captureWave(sillad, captured);
			if (prisoned > 0)
				play(sillad, SoundEvents.WARDEN_HEARTBEAT, 1.05F, 1.42F);
			level.playSound(null, BlockPos.containing(center),
					SoundEvents.GLASS_BREAK, SoundSource.HOSTILE,
					1.25F, 0.55F);
		}
		if (tick >= 64)
			finishAction(sillad, state, now);
	}

	private static void tickWhiteoutCast(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 14) {
			combatFootwork(sillad, state, target, 0.060D, 0.010D);
			trackAim(sillad, state, target);
		} else if (tick <= 24)
			combatFootwork(sillad, state, target, 0.045D, -0.008D);
		if (tick >= 15 && tick <= 24 && tick % 2 == 0) {
			Vec3 preview = sillad.position().add(state.aim.scale(2.0D));
			showWhiteoutPlane(level, preview, state.aim);
		}
		if (tick == 25) {
			state.whiteout = new WhiteoutState(
					sillad.position().add(state.aim.scale(2.0D)),
					state.aim, now, now + 90L);
			play(sillad, SoundEvents.POWDER_SNOW_BREAK, 1.25F, 0.52F);
		}
		if (tick >= 40)
			finishAction(sillad, state, now);
	}

	private static void tickWinterRemembers(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 60)
			combatFootwork(sillad, state, target, 0.045D, -0.006D);
		if (tick <= 12)
			trackAim(sillad, state, target);
		if (tick >= 12 && tick <= 60 && tick % 4 == 0 && target != null) {
			if (sillad.hasLineOfSight(target)) {
				state.winterSamples.add(new WinterSample(target.position(),
						target.getYRot(), target.getXRot(),
						target.getDeltaMovement()));
				state.winterLostSightTicks = 0;
			} else {
				state.winterLostSightTicks += 4;
			}
		}
		if (tick >= 12 && target != null && !sillad.hasLineOfSight(target))
			state.winterLostSightTicks++;
		if (state.winterLostSightTicks >= 10) {
			level.sendParticles(ParticleTypes.SMOKE, sillad.getX(),
					sillad.getEyeY(), sillad.getZ(), 8, 0.25D, 0.35D,
					0.25D, 0.02D);
			finishAction(sillad, state, now);
			return;
		}
		if (tick >= 12 && tick <= 68 && target != null && tick % 3 == 0)
			spawnLine(level, sillad.getEyePosition(),
					target.getBoundingBox().getCenter(), ParticleTypes.END_ROD,
					10, 0.0D);
		if (tick == 60 && target != null) {
			spawnRing(level, target.position().add(0.0D, 0.1D, 0.0D),
					2.5D, 24, ParticleTypes.SNOWFLAKE);
			play(sillad, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.9F, 1.7F);
		}
		if (tick == 72 && target != null)
			resolveWinterRemembers(sillad, state, level, target);
		if (tick >= 85)
			finishAction(sillad, state, now);
	}

	private static void tickCrownOfWinter(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (tick <= 48)
			combatFootwork(sillad, state, target, 0.050D, -0.008D);
		int[] locks = {10, 20, 30, 40};
		int[] fires = {18, 28, 38, 48};
		for (int index = 0; index < locks.length; index++) {
			if (tick == locks[index]) {
				trackAim(sillad, state, target);
				double offset = switch (index) {
					case 0 -> -18.0D;
					case 1 -> 18.0D;
					case 2 -> -7.0D;
					default -> 7.0D;
				};
				state.lanceDirections[index] = rotateY(state.aim,
						Math.toRadians(offset));
			}
			Vec3 direction = state.lanceDirections[index];
			if (direction != null && tick > locks[index]
					&& tick < fires[index]) {
				Vec3 start = sillad.getEyePosition();
				spawnLine(level, start, start.add(direction.scale(24.0D)),
						ParticleTypes.SNOWFLAKE, 18, 0.0D);
			}
			if (direction != null && tick == fires[index]) {
				Vec3 start = sillad.getEyePosition();
				Vec3 end = clippedEndpoint(level, sillad, start,
						start.add(direction.scale(24.0D)));
				hitCrownLine(sillad, state, start, end);
				spawnLine(level, start, end, ParticleTypes.END_ROD, 30, 0.04D);
				play(sillad, SoundEvents.TRIDENT_THUNDER.value(), 0.65F,
						1.65F + index * 0.08F);
			}
		}
		if (tick >= 68)
			finishAction(sillad, state, now);
	}

	private static void tickAbsoluteZero(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, int tick, long now) {
		Vec3 center = state.actionOrigin;
		if (tick <= 50 && tick % 2 == 0) {
			double radius = 12.0D * tick / 50.0D;
			spawnRing(level, center.add(0.0D, 0.12D, 0.0D), radius,
					Math.max(16, Mth.ceil(radius * 5.0D)),
					tick % 6 == 0 ? ParticleTypes.END_ROD
							: ParticleTypes.SNOWFLAKE);
		}
		if (tick >= 50 && tick <= 110) {
			if (tick % 3 == 0)
				spawnRing(level, center.add(0.0D, 0.15D, 0.0D),
						12.0D, 42, ParticleTypes.SNOWFLAKE);
			if (tick % 10 == 0) {
				state.primaryHits.clear();
				int pulse = (tick - 50) / 10;
				for (LivingEntity victim : nearbyTargets(sillad, center, 12.0D))
					if (state.primaryHits.add(victim.getUUID()))
						damage(sillad, victim, 4.0F,
								pulse % 2 == 1 ? 1 : 0, false, true);
				play(sillad, SoundEvents.SCULK_CATALYST_BLOOM, 0.72F,
						1.45F - pulse * 0.07F);
			}
		}
		if (tick == 130) {
			state.primaryHits.clear();
			for (LivingEntity victim : nearbyTargets(sillad, center, 12.0D))
				if (state.primaryHits.add(victim.getUUID()))
					damage(sillad, victim, 28.0F, 2, true, true);
			for (int radius = 2; radius <= 12; radius += 2)
				spawnRing(level, center.add(0.0D, 0.3D, 0.0D), radius,
						32, ParticleTypes.END_ROD);
			level.sendParticles(ParticleTypes.SNOWFLAKE, center.x,
					center.y + 1.0D, center.z, 180, 6.0D, 1.4D,
					6.0D, 0.15D);
			play(sillad, SoundEvents.GENERIC_EXPLODE.value(), 1.35F, 1.65F);
		}
		if (tick >= 165)
			finishAction(sillad, state, now);
	}

	/**
	 * A completed Freeze gives Sillad one clearly telegraphed true-frost
	 * thrust. The target may still break line of sight during the wind-up; if
	 * it lands, the dedicated damage type bypasses armor without bypassing
	 * resistance, invulnerability, or normal Forge damage events.
	 */
	private static void tickGlacialExecution(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target,
			int tick, long now) {
		if (target == null || !isExecutionReady(sillad, target, now)) {
			level.sendParticles(ParticleTypes.SMOKE, sillad.getX(),
					sillad.getEyeY(), sillad.getZ(), 5, 0.20D, 0.30D,
					0.20D, 0.015D);
			finishAction(sillad, state, now);
			return;
		}
		if (tick <= 16) {
			combatFootwork(sillad, state, target, 0.055D, -0.010D);
			trackAim(sillad, state, target);
			state.targetPoint = target.getBoundingBox().getCenter();
		}
		Vec3 targetCenter = tick <= 16
				? target.getBoundingBox().getCenter() : state.targetPoint;
		if (tick >= 5 && tick <= 22 && (tick & 1) == 0) {
			double collapse = Math.max(0.75D, 3.4D - tick * 0.11D);
			spawnRing(level, new Vec3(targetCenter.x,
					target.getY() + 0.08D, targetCenter.z), collapse,
					20, tick >= 17 ? ParticleTypes.END_ROD
							: ParticleTypes.SNOWFLAKE);
			spawnLine(level, sillad.getEyePosition(), targetCenter,
					tick >= 17 ? ParticleTypes.END_ROD
							: ParticleTypes.SNOWFLAKE,
					14, 0.0D);
		}
		if (tick == 12) {
			play(sillad, SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.58F);
			if (target instanceof ServerPlayer player)
				player.displayClientMessage(Component.translatable(
						"message.sololeveling.sillad.glacial_execution"), true);
		}
		if (tick == 18)
			play(sillad, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F, 1.72F);
		if (tick == 24) {
			Vec3 start = sillad.getEyePosition();
			Vec3 end = target.getBoundingBox().getCenter();
			spawnLine(level, start, end, ParticleTypes.END_ROD, 38, 0.055D);
			boolean landed = sillad.distanceToSqr(target) <= 30.0D * 30.0D
					&& sillad.hasLineOfSight(target)
					&& trueFrostExecution(sillad, target, now);
			if (landed) {
				knockAway(target, sillad.position(), 0.80D, 0.18D);
				level.sendParticles(ParticleTypes.SNOWFLAKE, end.x, end.y,
						end.z, 90, 0.75D, 0.90D, 0.75D, 0.12D);
				level.sendParticles(ParticleTypes.END_ROD, end.x, end.y,
						end.z, 32, 0.48D, 0.60D, 0.48D, 0.06D);
				play(sillad, SoundEvents.GLASS_BREAK, 1.45F, 0.48F);
				play(sillad, SoundEvents.TRIDENT_THUNDER.value(), 0.92F, 1.72F);
			} else {
				burst(level, clippedEndpoint(level, sillad, start, end), 18);
				play(sillad, SoundEvents.TRIDENT_HIT_GROUND, 0.75F, 1.6F);
			}
		}
		if (tick >= 42)
			finishAction(sillad, state, now);
	}

	private static void tickFrostStep(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, int tick, long now) {
		Vec3 destination = state.stepDestination;
		if (destination == null) {
			finishAction(sillad, state, now);
			return;
		}
		if (tick <= 8) {
			level.sendParticles(ParticleTypes.SNOWFLAKE,
					state.actionOrigin.x, state.actionOrigin.y + 1.0D,
					state.actionOrigin.z, 5, 0.35D, 0.65D, 0.35D, 0.03D);
			level.sendParticles(ParticleTypes.END_ROD, destination.x,
					destination.y + 1.0D, destination.z, 5, 0.35D, 0.65D,
					0.35D, 0.02D);
		}
		if (tick == 8 && isSafePosition(level, sillad, destination)) {
			sillad.teleportTo(destination.x, destination.y, destination.z);
			sillad.fallDistance = 0.0F;
			play(sillad, SoundEvents.ENDERMAN_TELEPORT, 0.9F, 1.8F);
			state.stalledTicks = 0;
		}
		if (tick >= 14)
			finishAction(sillad, state, now);
	}

	private static Action selectAction(SilladBossEntity sillad,
			RuntimeState state, LivingEntity target, ServerLevel level,
			long now) {
		double distance = CombatRangeHelper.surfaceDistance(sillad, target);
		boolean lineOfSight = sillad.hasLineOfSight(target);
		int phase = sillad.getCombatPhase();
		List<Candidate> choices = new ArrayList<>();
		if (lineOfSight && distance <= 30.0D
				&& isExecutionReady(sillad, target, now)
				&& cooldownUntil(sillad, Action.GLACIAL_EXECUTION) <= now)
			return Action.GLACIAL_EXECUTION;

		addCandidate(choices, sillad, state, Action.FROST_CLEAVE,
				now, distance <= 4.8D ? 9.0D : 0.0D);
		addCandidate(choices, sillad, state, Action.ICE_SPEAR, now,
				lineOfSight && distance >= 5.0D && distance <= 26.0D
						? 7.0D + (target.getY() > sillad.getY() + 2.0D ? 2.0D : 0.0D)
						: 0.0D);
		int stacks = frostbiteStacks(sillad, target);
		addCandidate(choices, sillad, state, Action.FLASH_FREEZE, now,
				lineOfSight && distance <= 11.0D
						? 6.0D + (stacks >= 2 ? 2.0D : 0.0D)
						: 0.0D);
		addCandidate(choices, sillad, state, Action.FROZEN_PATH, now,
				(!lineOfSight ? 8.0D : distance > 15.0D ? 6.0D
						: state.stalledTicks >= 20 ? 11.0D : 2.0D));

		if (phase >= 2) {
			double stepScore = state.stalledTicks >= 12 ? 11.0D
					: distance < 4.0D ? 7.0D
					: distance > 17.0D ? 5.5D
					: state.recentDamage >= 36.0F ? 4.5D : 1.8D;
			addCandidate(choices, sillad, state, Action.FROST_STEP,
					now, stepScore);
			addCandidate(choices, sillad, state, Action.FROST_COUNTER,
					now, 4.0D + Math.min(6.0D, state.recentDamage / 18.0D));
			int incoming = countIncomingProjectiles(sillad, level);
			addCandidate(choices, sillad, state, Action.STILLNESS_DECREE,
					now, incoming > 0 ? 7.0D + Math.min(4, incoming) * 2.0D : 0.0D);
			if (state.whiteout == null) {
				int clustered = nearbyTargets(sillad, target.position(), 5.0D).size();
				int companions = prisonCandidates(sillad, state, level, now).size();
				if (SilladIcePrisonManager.activeCountForBoss(
						sillad.getUUID()) == 0)
					addCandidate(choices, sillad, state, Action.SPIRE_CAGE,
							now, 5.0D
									+ Math.min(3, Math.max(0, clustered - 1)) * 2.0D
									+ Math.min(4, companions) * 2.5D);
				addCandidate(choices, sillad, state, Action.WHITEOUT_PROCESSION,
						now, 4.0D + (incoming > 0 ? 2.0D : 0.0D)
								+ (sillad.getEngagedPlayerCount() > 1 ? 2.0D : 0.0D));
			}
		}
		if (phase >= 3 && state.whiteout == null) {
			boolean rewindReady = target.getPersistentData()
					.getLong(REWIND_IMMUNITY) <= now;
			addCandidate(choices, sillad, state, Action.WINTER_REMEMBERS,
					now, rewindReady && lineOfSight ? 5.0D : 0.0D);
			addCandidate(choices, sillad, state, Action.CROWN_OF_WINTER,
					now, 5.0D + (sillad.getEngagedPlayerCount() > 1 ? 2.0D : 0.0D));
		}
		if (choices.isEmpty())
			return Action.IDLE;
		choices.sort(Comparator.comparingDouble(Candidate::score).reversed());
		int limit = Math.min(3, choices.size());
		double total = 0.0D;
		for (int index = 0; index < limit; index++)
			total += choices.get(index).score;
		double roll = sillad.getRandom().nextDouble() * total;
		for (int index = 0; index < limit; index++) {
			roll -= choices.get(index).score;
			if (roll <= 0.0D)
				return choices.get(index).action;
		}
		return choices.get(0).action;
	}

	private static void addCandidate(List<Candidate> candidates,
			SilladBossEntity sillad, RuntimeState state, Action action,
			long now, double baseScore) {
		if (baseScore <= 0.0D || cooldownUntil(sillad, action) > now)
			return;
		double repetition = 1.0D;
		int index = 0;
		for (Action recent : state.recentActions) {
			if (recent == action)
				repetition *= index == 0 ? 0.05D : 0.35D;
			index++;
		}
		double score = baseScore * repetition
				* (0.90D + sillad.getRandom().nextDouble() * 0.20D);
		if (score > 0.05D)
			candidates.add(new Candidate(action, score));
	}

	private static boolean maintainCombatPosition(SilladBossEntity sillad,
			RuntimeState state, LivingEntity target, long now) {
		sillad.getLookControl().setLookAt(target, 50.0F, 50.0F);
		double homeDistance = sillad.position().distanceToSqr(
				Vec3.atCenterOf(sillad.getEncounterHome()));
		if (homeDistance > ENCOUNTER_LEASH * ENCOUNTER_LEASH) {
			if (now >= state.nextPathAt) {
				state.nextPathAt = now + 6L + Math.floorMod(sillad.getId(), 3);
				sillad.getNavigation().moveTo(
						sillad.getEncounterHome().getX() + 0.5D,
						sillad.getEncounterHome().getY(),
						sillad.getEncounterHome().getZ() + 0.5D, 1.40D);
			}
			return true;
		}
		double distance = CombatRangeHelper.surfaceDistance(sillad, target);
		boolean lineOfSight = sillad.hasLineOfSight(target);
		if (distance > PREFERRED_MAXIMUM || !lineOfSight) {
			if (now >= state.nextPathAt) {
				state.nextPathAt = now + 6L + Math.floorMod(sillad.getId(), 3);
				sillad.getNavigation().moveTo(target, 1.36D);
			}
			if (sillad.isInWaterOrBubble() && (sillad.tickCount & 3) == 0)
				sillad.getJumpControl().jump();
			return true;
		}
		if (distance < PREFERRED_MINIMUM) {
			if (now >= state.nextPathAt) {
				state.nextPathAt = now + 7L;
				Vec3 retreat = DefaultRandomPos.getPosAway(sillad, 10, 5,
						target.position());
				if (retreat != null)
					sillad.getNavigation().moveTo(retreat.x, retreat.y,
							retreat.z, 1.32D);
			}
			return true;
		}
		if (distance >= PREFERRED_MINIMUM && distance <= PREFERRED_MAXIMUM) {
			if (now >= state.nextStrafeAt
					|| sillad.getNavigation().isDone()) {
				state.nextStrafeAt = now + 8L + sillad.getRandom().nextInt(8);
				Vec3 forward = horizontalDirection(sillad.position(),
						target.position(), sillad.getLookAngle());
				if (sillad.getRandom().nextFloat() < 0.22F)
					state.strafeDirection *= -1;
				Vec3 side = new Vec3(-forward.z, 0.0D, forward.x)
						.scale(state.strafeDirection * 5.8D);
				double radial = distance > 9.5D ? 1.8D
						: distance < 6.5D ? -1.8D : 0.0D;
				Vec3 node = sillad.position().add(side)
						.add(forward.scale(radial));
				BlockPos nodePos = BlockPos.containing(node);
				var path = sillad.getNavigation().createPath(nodePos, 1);
				if (path != null && path.canReach())
					sillad.getNavigation().moveTo(path, 1.20D);
			}
			return !sillad.getNavigation().isDone();
		}
		return false;
	}

	private static void trackStuckRecovery(SilladBossEntity sillad,
			RuntimeState state, LivingEntity target, boolean expectsMovement,
			long now) {
		double moved = sillad.position().distanceToSqr(state.lastPosition);
		state.lastPosition = sillad.position();
		if (!expectsMovement || moved > 0.025D) {
			state.stalledTicks = Math.max(0, state.stalledTicks - 3);
			return;
		}
		state.stalledTicks++;
		if (state.stalledTicks == 15) {
			sillad.getNavigation().stop();
			state.nextPathAt = 0L;
			Vec3 alternate = DefaultRandomPos.getPosTowards(sillad, 9, 5,
					target.position(), Math.PI / 2.0D);
			if (alternate != null)
				sillad.getNavigation().moveTo(alternate.x, alternate.y,
						alternate.z, 1.38D);
		} else if (state.stalledTicks == 30) {
			sillad.getJumpControl().jump();
			sillad.getNavigation().stop();
			Vec3 side = new Vec3(-state.aim.z, 0.0D, state.aim.x)
					.scale(sillad.getRandom().nextBoolean() ? 6.0D : -6.0D);
			sillad.getNavigation().moveTo(sillad.getX() + side.x,
					sillad.getY(), sillad.getZ() + side.z, 1.42D);
		} else if (state.stalledTicks >= 50
				&& cooldownUntil(sillad, Action.FROST_STEP) <= now) {
			beginAction(sillad, state, Action.FROST_STEP, target, now);
		}
	}

	private static LivingEntity chooseStrategicTarget(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, long now) {
		state.retaliationUntil.entrySet().removeIf(entry ->
				entry.getValue() < now);
		List<LivingEntity> valid = level.getEntitiesOfClass(LivingEntity.class,
				sillad.getBoundingBox().inflate(PLAYER_SCAN_RANGE),
				candidate -> isValidCombatTarget(sillad, candidate));
		for (LivingEntity candidate : valid)
			state.threat.merge(candidate.getUUID(), 0.025F, Float::sum);
		state.threat.keySet().removeIf(id -> valid.stream()
				.noneMatch(candidate -> candidate.getUUID().equals(id)));
		if (valid.isEmpty()) {
			sillad.setTarget(null);
			return null;
		}
		LivingEntity selectedTarget = sillad.getTarget();
		LivingEntity current = selectedTarget != null
				&& valid.contains(selectedTarget) ? selectedTarget : null;
		LivingEntity best = valid.stream().max(Comparator.comparingDouble(candidate ->
				state.threat.getOrDefault(candidate.getUUID(), 0.0F)
						+ Math.max(0.0D, 18.0D
								- Math.sqrt(candidate.distanceToSqr(sillad))) * 0.15D
						+ (candidate instanceof Mob mob && mob.getTarget() == sillad
								? 8.0D : 0.0D)
						+ (state.retaliationUntil.getOrDefault(candidate.getUUID(), 0L)
								>= now ? 12.0D : 0.0D))).orElse(valid.get(0));
		if (current != null && now < state.targetLockedUntil) {
			float currentThreat = state.threat.getOrDefault(current.getUUID(), 0.0F)
					+ 20.0F;
			float challenger = state.threat.getOrDefault(best.getUUID(), 0.0F);
			if (challenger < currentThreat * 1.5F)
				best = current;
		}
		if (current != best)
			state.targetLockedUntil = now + 80L;
		sillad.setTarget(best);
		return best;
	}

	private static void lockEncounterParticipants(SilladBossEntity sillad,
			ServerLevel level) {
		int nearby = 0;
		for (ServerPlayer player : level.players())
			if (isValidCombatTarget(sillad, player)
					&& player.distanceToSqr(sillad)
							<= PLAYER_SCAN_RANGE * PLAYER_SCAN_RANGE)
				nearby++;
		sillad.lockEngagedPlayerCount(nearby);
	}

	private static void tickWhiteout(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, long now) {
		WhiteoutState whiteout = state.whiteout;
		if (whiteout == null)
			return;
		if (now > whiteout.expiresAt) {
			state.whiteout = null;
			return;
		}
		Vec3 center = whiteout.origin.add(whiteout.direction.scale(
				(now - whiteout.startedAt) * 0.18D));
		if (now % 3L == 0L)
			showWhiteoutPlane(level, center, whiteout.direction);
		Vec3 right = new Vec3(-whiteout.direction.z, 0.0D,
				whiteout.direction.x);
		for (LivingEntity target : nearbyTargets(sillad, center, 5.5D)) {
			Vec3 local = target.getBoundingBox().getCenter().subtract(center);
			if (Math.abs(local.dot(whiteout.direction)) > 0.95D
					|| Math.abs(local.dot(right)) > 3.6D
					|| Math.abs(local.y) > 3.0D
					|| !whiteout.hitTargets.add(target.getUUID()))
				continue;
			damage(sillad, target, 8.0F, 1, false, true);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					30, 0, false, true, true));
		}
		AABB search = new AABB(center, center).inflate(4.5D, 3.5D, 4.5D);
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
				search, Entity::isAlive)) {
			if (!isHostileProjectile(sillad, projectile)
					|| !whiteout.slowedProjectiles.add(projectile.getUUID()))
				continue;
			Vec3 local = projectile.position().subtract(center);
			if (Math.abs(local.dot(whiteout.direction)) <= 1.0D
					&& Math.abs(local.dot(right)) <= 3.7D) {
				projectile.setDeltaMovement(projectile.getDeltaMovement().scale(0.70D));
				projectile.hurtMarked = true;
			}
		}
	}

	private static void updateFrostbite(SilladBossEntity sillad,
			ServerLevel level, long now) {
		List<UUID> remove = new ArrayList<>();
		for (Map.Entry<UUID, FrostState> entry : FROSTBITE.entrySet()) {
			FrostState frost = entry.getValue();
			if (!sillad.getUUID().equals(frost.ownerId))
				continue;
			Entity raw = level.getEntity(entry.getKey());
			if (!(raw instanceof LivingEntity target) || !target.isAlive()) {
				remove.add(entry.getKey());
				continue;
			}
			if (frost.stacks > 0 && now > frost.stacksExpireAt)
				frost.stacks = SilladBossRules.clampFrostbite(0);
			if (now <= frost.frozenUntil) {
				Vec3 velocity = target.getDeltaMovement();
				target.setDeltaMovement(velocity.x * 0.08D,
						target.onGround() && velocity.y > 0.0D ? 0.0D : velocity.y,
						velocity.z * 0.08D);
				target.hurtMarked = true;
				if (target instanceof Mob mob)
					mob.getNavigation().stop();
				target.setTicksFrozen(Math.max(target.getTicksFrozen(), 100));
			}
			if ((now & 7L) == 0L && (frost.stacks > 0
					|| now <= frost.brittleUntil))
				level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(),
						target.getEyeY(), target.getZ(),
						Math.max(2, frost.stacks), 0.24D, 0.34D,
						0.24D, 0.018D);
			if (frost.hadFreeze && now > frost.frozenUntil
					&& !frost.restored) {
				restoreFrozenTicks(target, frost);
				frost.restored = true;
			}
			if (frost.stacks <= 0 && now > frost.brittleUntil
					&& now > frost.freezeImmuneUntil
					&& now > frost.executionReadyUntil)
				remove.add(entry.getKey());
		}
		remove.forEach(FROSTBITE::remove);
	}

	private static void tickFrozenDomainPressure(SilladBossEntity sillad,
			ServerLevel level, long now) {
		int interval = SilladBossRules.frozenDomainPulseInterval(
				sillad.getCombatPhase());
		if (Math.floorMod(now + sillad.getId(), interval) != 0L)
			return;
		double radius = SilladBossRules.frozenDomainRadius(
				sillad.getCombatPhase());
		for (LivingEntity target : nearbyTargets(sillad, sillad.position(),
				radius)) {
			applyFrostbite(sillad, target, 1, now);
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					interval + 10, target instanceof Player ? 0 : 1,
					false, true, true));
		}
		spawnRing(level, sillad.position().add(0.0D, 0.14D, 0.0D),
				radius, Math.max(24, Mth.ceil(radius * 2.0D)),
				ParticleTypes.SNOWFLAKE);
		play(sillad, SoundEvents.POWDER_SNOW_STEP, 0.42F,
				sillad.getCombatPhase() >= 3 ? 0.62F : 0.78F);
	}

	private static boolean damage(SilladBossEntity sillad,
			LivingEntity target, float baseDamage, int frostbite,
			boolean allowShatter, boolean magic) {
		if (!isValidCombatTarget(sillad, target))
			return false;
		long now = sillad.level().getGameTime();
		float amount = baseDamage * SilladBossRules.outgoingDamageMultiplier(
				sillad.getEngagedPlayerCount());
		if (sillad.isSpiritualized())
			amount *= 1.10F;
		if (!(target instanceof Player)
				&& ShadowMonarchManager.getShadowOwnerUUID(target) != null)
			amount *= 0.65F;
		FrostState state = FROSTBITE.get(target.getUUID());
		if (allowShatter && state != null
				&& sillad.getUUID().equals(state.ownerId)
				&& now >= state.brittleAt && now <= state.brittleUntil) {
			amount = amount * 1.25F + 4.0F;
			if (target instanceof Player)
				amount = Math.min(32.0F, amount);
			state.brittleUntil = now - 1L;
			emitShatter(sillad, target);
		}
		DamageSource source = magic
				? sillad.damageSources().indirectMagic(sillad, sillad)
				: sillad.damageSources().mobAttack(sillad);
		target.invulnerableTime = 0;
		boolean hurt = target.hurt(source, Math.max(0.5F, amount));
		if (hurt && frostbite > 0)
			applyFrostbite(sillad, target, frostbite, now);
		return hurt;
	}

	private static boolean trueFrostExecution(SilladBossEntity sillad,
			LivingEntity target, long now) {
		if (!isValidCombatTarget(sillad, target)
				|| !isExecutionReady(sillad, target, now))
			return false;
		float amount = SilladBossRules.trueFrostExecutionDamage(
				target.getMaxHealth(), target instanceof Player,
				sillad.getCombatPhase(), sillad.getEngagedPlayerCount());
		if (amount <= 0.0F)
			return false;
		target.invulnerableTime = 0;
		boolean hurt = target.hurt(SilladDamageTypes.trueFrost(
				sillad.level(), sillad), amount);
		if (!hurt)
			return false;
		FrostState frost = FROSTBITE.get(target.getUUID());
		if (frost != null && sillad.getUUID().equals(frost.ownerId)) {
			frost.executionReadyUntil = now - 1L;
			frost.brittleUntil = now - 1L;
		}
		emitShatter(sillad, target);
		return true;
	}

	/**
	 * Entry point for the ambient cold reaching its limit.
	 *
	 * <p>Freezing solid from the arena and freezing solid from a hit are the
	 * same event, so this routes into the existing frostbite path at full
	 * strength rather than duplicating the root, brittle and execution timers.
	 */
	public static void forceGlacialBreak(SilladBossEntity sillad, LivingEntity target) {
		if (sillad == null || target == null || sillad.level().isClientSide())
			return;
		applyFrostbite(sillad, target, SilladBossRules.MAX_FROSTBITE,
				sillad.level().getGameTime());
	}

	private static void applyFrostbite(SilladBossEntity sillad,
			LivingEntity target, int amount, long now) {
		FrostState frost = FROSTBITE.get(target.getUUID());
		if (frost == null || !sillad.getUUID().equals(frost.ownerId)) {
			frost = new FrostState(sillad.getUUID(), target.getTicksFrozen());
			FROSTBITE.put(target.getUUID(), frost);
		}
		if (now <= frost.frozenUntil)
			return;
		boolean freezeImmune = now <= frost.freezeImmuneUntil;
		frost.stacks = SilladBossRules.clampFrostbite(
				SilladBossRules.addFrostbite(frost.stacks, amount,
						freezeImmune));
		frost.stacksExpireAt = now + SilladBossRules.FROSTBITE_LIFETIME;
		target.setTicksFrozen(Math.max(target.getTicksFrozen(),
				frost.stacks * 20));
		if (frost.stacks < SilladBossRules.MAX_FROSTBITE)
			return;
		frost.stacks = 0;
		int rootTicks = target instanceof Player
				? SilladBossRules.PLAYER_ROOT_TICKS
				: SilladBossRules.NON_PLAYER_ROOT_TICKS;
		frost.frozenUntil = now + rootTicks;
		frost.freezeImmuneUntil = now
				+ SilladBossRules.FREEZE_IMMUNITY_TICKS;
		frost.brittleAt = now + SilladBossRules.BRITTLE_DELAY_TICKS;
		frost.brittleUntil = frost.brittleAt
				+ SilladBossRules.BRITTLE_DURATION_TICKS;
		frost.executionReadyUntil = now
				+ SilladBossRules.EXECUTION_WINDOW_TICKS;
		frost.hadFreeze = true;
		frost.restored = false;
		target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
				rootTicks, 9, false, true, true));
		if (sillad.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(),
					target.getEyeY(), target.getZ(), 34, 0.4D, 0.55D,
					0.4D, 0.06D);
			level.playSound(null, target.blockPosition(), SoundEvents.GLASS_PLACE,
					SoundSource.HOSTILE, 1.0F, 1.42F);
		}
	}

	private static int frostbiteStacks(SilladBossEntity sillad,
			LivingEntity target) {
		FrostState frost = FROSTBITE.get(target.getUUID());
		if (frost == null || !sillad.getUUID().equals(frost.ownerId)
				|| sillad.level().getGameTime() > frost.stacksExpireAt)
			return 0;
		return SilladBossRules.clampFrostbite(frost.stacks);
	}

	private static boolean isExecutionReady(SilladBossEntity sillad,
			LivingEntity target, long now) {
		FrostState frost = FROSTBITE.get(target.getUUID());
		return frost != null && sillad.getUUID().equals(frost.ownerId)
				&& now <= frost.executionReadyUntil;
	}

	private static void resolveWinterRemembers(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, LivingEntity target) {
		if (target.isPassenger() || target.isVehicle()
				|| target.getPersistentData().getLong(REWIND_IMMUNITY)
						> level.getGameTime())
			return;
		WinterSample selected = null;
		for (WinterSample sample : state.winterSamples) {
			if (target.position().distanceTo(sample.position) <= 10.0D
					&& isSafePosition(level, target, sample.position)) {
				selected = sample;
				break;
			}
		}
		if (selected == null)
			return;
		Vec3 before = target.position();
		if (target instanceof ServerPlayer player)
			player.connection.teleport(selected.position.x,
					selected.position.y, selected.position.z,
					selected.yaw, selected.pitch);
		else
			target.teleportTo(selected.position.x, selected.position.y,
					selected.position.z);
		target.setYRot(selected.yaw);
		target.setXRot(selected.pitch);
		target.setDeltaMovement(selected.velocity);
		target.fallDistance = 0.0F;
		target.hurtMarked = true;
		target.getPersistentData().putLong(REWIND_IMMUNITY,
				level.getGameTime() + 160L);
		damage(sillad, target, 14.0F, 2, false, true);
		spawnLine(level, before, selected.position, ParticleTypes.END_ROD,
				24, 0.02D);
		play(sillad, SoundEvents.ENDERMAN_TELEPORT, 0.9F, 1.75F);
	}

	private static void hitLine(SilladBossEntity sillad, RuntimeState state,
			Vec3 start, Vec3 end, double width, float amount, int frostbite,
			boolean shatter, boolean magic, boolean secondary) {
		Set<UUID> ledger = secondary ? state.secondaryHits : state.primaryHits;
		for (LivingEntity target : targetsAlongLine(sillad, start, end, width))
			if (ledger.add(target.getUUID()))
				damage(sillad, target, amount, frostbite, shatter, magic);
	}

	private static void hitCrownLine(SilladBossEntity sillad,
			RuntimeState state, Vec3 start, Vec3 end) {
		for (LivingEntity target : targetsAlongLine(sillad, start, end, 0.75D)) {
			int hits = state.crownHits.getOrDefault(target.getUUID(), 0);
			if (hits >= 2)
				continue;
			state.crownHits.put(target.getUUID(), hits + 1);
			damage(sillad, target, 14.0F, 1, true, true);
		}
	}

	private static List<LivingEntity> targetsAlongLine(SilladBossEntity sillad,
			Vec3 start, Vec3 end, double width) {
		AABB search = new AABB(start, end).inflate(width + 1.5D);
		List<LivingEntity> targets = sillad.level().getEntitiesOfClass(
				LivingEntity.class, search,
				candidate -> isValidCombatTarget(sillad, candidate));
		targets.removeIf(target -> distanceToSegmentSqr(
				target.getBoundingBox().getCenter(), start, end)
						> square(width + target.getBbWidth() * 0.5D));
		targets.sort(Comparator.comparingDouble(target ->
				target.distanceToSqr(start)));
		return targets;
	}

	private static List<LivingEntity> targetsInCone(SilladBossEntity sillad,
			Vec3 origin, Vec3 direction, double range, double angleDegrees) {
		double cosine = Math.cos(Math.toRadians(angleDegrees));
		List<LivingEntity> targets = nearbyTargets(sillad, origin, range + 1.5D);
		targets.removeIf(target -> {
			Vec3 to = target.getBoundingBox().getCenter().subtract(origin);
			Vec3 flat = new Vec3(to.x, 0.0D, to.z);
			return flat.lengthSqr() > square(range + target.getBbWidth() * 0.5D)
					|| flat.lengthSqr() > 1.0E-5D
							&& flat.normalize().dot(direction) < cosine;
		});
		return targets;
	}

	private static List<LivingEntity> nearbyTargets(SilladBossEntity sillad,
			Vec3 center, double radius) {
		List<LivingEntity> targets = sillad.level().getEntitiesOfClass(
				LivingEntity.class, new AABB(center, center).inflate(radius),
				candidate -> isValidCombatTarget(sillad, candidate));
		targets.sort(Comparator.comparingDouble(target ->
				target.distanceToSqr(center)));
		if (targets.size() > 32)
			return new ArrayList<>(targets.subList(0, 32));
		return targets;
	}

	private static List<LivingEntity> prisonCandidates(
			SilladBossEntity sillad, RuntimeState state, ServerLevel level,
			long now) {
		int limit = sillad.getCombatPhase() >= 3 ? 5 : 3;
		List<LivingEntity> candidates = level.getEntitiesOfClass(
				LivingEntity.class, sillad.getBoundingBox().inflate(18.0D, 8.0D,
						18.0D), target -> {
					if (!SilladIcePrisonManager.canCapture(sillad, target))
						return false;
					if (!sillad.hasLineOfSight(target))
						return false;
					return target instanceof Mob mob && mob.getTarget() == sillad
							|| state.retaliationUntil.getOrDefault(
									target.getUUID(), 0L) >= now;
				});
		candidates.sort(Comparator
				.comparingDouble((LivingEntity target) ->
						target.distanceToSqr(sillad))
				.thenComparing(Comparator.comparingDouble(
						LivingEntity::getMaxHealth).reversed()));
		if (candidates.size() > limit)
			return new ArrayList<>(candidates.subList(0, limit));
		return candidates;
	}

	private static boolean isValidCombatTarget(SilladBossEntity sillad,
			@Nullable LivingEntity target) {
		if (target == null || target == sillad || !target.isAlive()
				|| target.level() != sillad.level()
				|| SilladIcePrisonManager.isImprisoned(target))
			return false;
		if (target instanceof Player player)
			return !player.isCreative() && !player.isSpectator();
		UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(target);
		if (ownerId != null) {
			Player owner = sillad.level().getPlayerByUUID(ownerId);
			return owner != null && owner.isAlive() && !owner.isCreative()
					&& !owner.isSpectator();
		}
		if (target instanceof Mob mob && mob.getTarget() == sillad)
			return true;
		RuntimeState state = STATES.get(sillad.getUUID());
		return state != null && state.retaliationUntil.getOrDefault(
				target.getUUID(), 0L) >= sillad.level().getGameTime();
	}

	@Nullable
	private static Entity strategicAttacker(SilladBossEntity sillad,
			@Nullable Entity attacker) {
		if (attacker == null)
			return null;
		if (attacker instanceof Projectile projectile
				&& projectile.getOwner() != null)
			return projectile.getOwner();
		return attacker;
	}

	private static void captureProjectiles(SilladBossEntity sillad,
			RuntimeState state, ServerLevel level, Vec3 center) {
		if (state.capturedProjectiles >= 3)
			return;
		Vec3 right = new Vec3(-state.aim.z, 0.0D, state.aim.x);
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
				new AABB(center, center).inflate(4.2D, 3.2D, 4.2D),
				Entity::isAlive)) {
			if (state.capturedProjectiles >= 3
					|| state.capturedIds.contains(projectile.getUUID())
					|| !isHostileProjectile(sillad, projectile))
				continue;
			Vec3 local = projectile.position().subtract(center);
			if (Math.abs(local.dot(state.aim)) > 0.9D
					|| Math.abs(local.dot(right)) > 3.5D
					|| Math.abs(local.y) > 2.8D)
				continue;
			state.capturedIds.add(projectile.getUUID());
			state.capturedProjectiles++;
			if (projectile instanceof ThrownTrident) {
				projectile.setDeltaMovement(projectile.getDeltaMovement()
						.scale(-0.20D));
				projectile.hurtMarked = true;
			} else {
				projectile.discard();
			}
			level.sendParticles(ParticleTypes.END_ROD, projectile.getX(),
					projectile.getY(), projectile.getZ(), 12, 0.2D, 0.2D,
					0.2D, 0.03D);
			play(sillad, SoundEvents.AMETHYST_BLOCK_HIT, 0.75F,
					1.2F + state.capturedProjectiles * 0.15F);
		}
	}

	private static boolean isHostileProjectile(SilladBossEntity sillad,
			Projectile projectile) {
		Entity owner = projectile.getOwner();
		if (owner instanceof Player player)
			return !player.isCreative() && !player.isSpectator();
		return owner instanceof LivingEntity living
				&& isValidCombatTarget(sillad, living);
	}

	private static int countIncomingProjectiles(SilladBossEntity sillad,
			ServerLevel level) {
		int count = 0;
		for (Projectile projectile : level.getEntitiesOfClass(Projectile.class,
				sillad.getBoundingBox().inflate(12.0D), Entity::isAlive)) {
			if (!isHostileProjectile(sillad, projectile))
				continue;
			Vec3 toBoss = sillad.getBoundingBox().getCenter()
					.subtract(projectile.position());
			if (projectile.getDeltaMovement().dot(toBoss) > 0.03D)
				count++;
		}
		return count;
	}

	private static void showSpireColumns(ServerLevel level, Vec3 center,
			int gapIndex, boolean erupt) {
		for (Vec3 position : spirePositions(center, gapIndex)) {
			for (int height = 0; height <= (erupt ? 8 : 4); height++)
				level.sendParticles(erupt ? ParticleTypes.END_ROD
						: ParticleTypes.SNOWFLAKE,
						position.x, position.y + height * 0.52D, position.z,
						erupt ? 3 : 1, 0.12D, 0.12D, 0.12D,
						erupt ? 0.045D : 0.0D);
		}
	}

	private static List<Vec3> spirePositions(Vec3 center, int gapIndex) {
		List<Vec3> positions = new ArrayList<>();
		for (int index = 0; index < 7; index++) {
			if (index == gapIndex)
				continue;
			double angle = Math.PI * 2.0D * index / 7.0D;
			positions.add(center.add(Math.cos(angle) * 4.5D, 0.0D,
					Math.sin(angle) * 4.5D));
		}
		return positions;
	}

	private static void showStillnessPlane(ServerLevel level, Vec3 center,
			Vec3 direction) {
		Vec3 right = new Vec3(-direction.z, 0.0D, direction.x);
		for (int horizontal = -6; horizontal <= 6; horizontal++)
			for (int vertical = -2; vertical <= 5; vertical += 2) {
				Vec3 point = center.add(right.scale(horizontal * 0.55D))
						.add(0.0D, vertical * 0.45D, 0.0D);
				level.sendParticles(ParticleTypes.END_ROD, point.x, point.y,
						point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
			}
	}

	private static void showWhiteoutPlane(ServerLevel level, Vec3 center,
			Vec3 direction) {
		Vec3 right = new Vec3(-direction.z, 0.0D, direction.x);
		for (int horizontal = -7; horizontal <= 7; horizontal++) {
			Vec3 point = center.add(right.scale(horizontal * 0.5D));
			level.sendParticles(ParticleTypes.SNOWFLAKE, point.x,
					point.y + 1.8D, point.z, 3, 0.20D, 1.65D,
					0.20D, 0.025D);
		}
		level.sendParticles(ParticleTypes.CLOUD, center.x,
				center.y + 1.4D, center.z, 5, 2.8D, 1.1D, 0.3D, 0.01D);
	}

	private static void showCone(ServerLevel level, Vec3 origin,
			Vec3 direction, double range, double angle,
			ParticleOptions particle) {
		for (double distance = 2.0D; distance <= range; distance += 2.0D)
			spawnArc(level, origin, direction, distance, angle, particle);
	}

	private static void spawnArc(ServerLevel level, Vec3 origin,
			Vec3 direction, double radius, double angleDegrees,
			ParticleOptions particle) {
		int samples = Math.max(7, Mth.ceil(radius * 2.0D));
		for (int index = 0; index <= samples; index++) {
			double angle = Math.toRadians(-angleDegrees
					+ angleDegrees * 2.0D * index / samples);
			Vec3 ray = rotateY(direction, angle);
			Vec3 point = origin.add(ray.scale(radius));
			level.sendParticles(particle, point.x, point.y, point.z,
					1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	private static void spawnRing(ServerLevel level, Vec3 center,
			double radius, int points, ParticleOptions particle) {
		if (radius <= 0.05D) {
			level.sendParticles(particle, center.x, center.y, center.z,
					1, 0.0D, 0.0D, 0.0D, 0.0D);
			return;
		}
		for (int index = 0; index < points; index++) {
			double angle = Math.PI * 2.0D * index / points;
			level.sendParticles(particle,
					center.x + Math.cos(angle) * radius, center.y,
					center.z + Math.sin(angle) * radius,
					1, 0.0D, 0.0D, 0.0D, 0.0D);
		}
	}

	private static void spawnLine(ServerLevel level, Vec3 start, Vec3 end,
			ParticleOptions particle, int points, double speed) {
		int samples = Math.max(2, points);
		for (int index = 0; index <= samples; index++) {
			Vec3 point = start.lerp(end, index / (double) samples);
			level.sendParticles(particle, point.x, point.y, point.z,
					1, 0.025D, 0.025D, 0.025D, speed);
		}
	}

	private static void burst(ServerLevel level, Vec3 center, int count) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, center.x, center.y,
				center.z, count, 0.45D, 0.45D, 0.45D, 0.08D);
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y,
				center.z, Math.max(4, count / 4), 0.30D, 0.30D,
				0.30D, 0.04D);
	}

	private static void emitShatter(SilladBossEntity sillad,
			LivingEntity target) {
		if (!(sillad.level() instanceof ServerLevel level))
			return;
		Vec3 center = target.getBoundingBox().getCenter();
		burst(level, center, 44);
		level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK,
				SoundSource.HOSTILE, 1.1F, 1.45F);
	}

	private static void emitSpiritualAura(ServerLevel level,
			SilladBossEntity sillad) {
		level.sendParticles(ParticleTypes.SNOWFLAKE, sillad.getX(),
				sillad.getY() + 1.0D, sillad.getZ(), 8, 0.65D,
				1.0D, 0.65D, 0.018D);
		spawnRing(level, sillad.position().add(0.0D, 0.08D, 0.0D),
				2.2D, 12, ParticleTypes.END_ROD);
	}

	private static void combatFootwork(SilladBossEntity sillad,
			RuntimeState state, @Nullable LivingEntity target,
			double lateralSpeed, double forwardSpeed) {
		if (target == null || !target.isAlive())
			return;
		Vec3 forward = horizontalDirection(sillad.position(), target.position(),
				sillad.getLookAngle());
		Vec3 side = new Vec3(-forward.z, 0.0D, forward.x)
				.scale(lateralSpeed * state.strafeDirection);
		double distance = CombatRangeHelper.surfaceDistance(sillad, target);
		double radial = distance < 4.0D ? -Math.abs(forwardSpeed)
				: distance > 11.0D ? Math.abs(forwardSpeed) : forwardSpeed;
		Vec3 before = sillad.position();
		sillad.move(MoverType.SELF, side.add(forward.scale(radial)));
		if (sillad.position().distanceToSqr(before) < 0.001D)
			state.strafeDirection *= -1;
	}

	private static void trackAim(SilladBossEntity sillad,
			RuntimeState state, @Nullable LivingEntity target) {
		if (target == null)
			return;
		state.targetPoint = target.getBoundingBox().getCenter();
		state.aim = horizontalDirection(sillad.position(), state.targetPoint,
				state.aim);
		faceDirection(sillad, state.aim);
		sillad.getLookControl().setLookAt(target, 70.0F, 70.0F);
	}

	private static void faceDirection(SilladBossEntity sillad,
			Vec3 direction) {
		float yaw = (float) (Mth.atan2(direction.z, direction.x)
				* 180.0D / Math.PI) - 90.0F;
		sillad.setYRot(yaw);
		sillad.yBodyRot = yaw;
		sillad.yHeadRot = yaw;
	}

	private static Vec3 horizontalDirection(Vec3 origin, Vec3 target,
			Vec3 fallback) {
		Vec3 direction = new Vec3(target.x - origin.x, 0.0D,
				target.z - origin.z);
		if (direction.lengthSqr() < 1.0E-5D) {
			direction = new Vec3(fallback.x, 0.0D, fallback.z);
			if (direction.lengthSqr() < 1.0E-5D)
				direction = new Vec3(0.0D, 0.0D, 1.0D);
		}
		return direction.normalize();
	}

	private static Vec3 rotateY(Vec3 vector, double angle) {
		double cosine = Math.cos(angle);
		double sine = Math.sin(angle);
		return new Vec3(vector.x * cosine - vector.z * sine, 0.0D,
				vector.x * sine + vector.z * cosine).normalize();
	}

	private static Vec3 clippedEndpoint(ServerLevel level, Entity source,
			Vec3 start, Vec3 intended) {
		BlockHitResult hit = level.clip(new ClipContext(start, intended,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source));
		return hit.getType() == HitResult.Type.MISS ? intended
				: hit.getLocation();
	}

	@Nullable
	private static Vec3 findFrostStepDestination(SilladBossEntity sillad,
			@Nullable LivingEntity target) {
		if (!(sillad.level() instanceof ServerLevel level) || target == null)
			return null;
		double[] angles = {70.0D, -70.0D, 105.0D, -105.0D, 145.0D,
				-145.0D, 35.0D, -35.0D};
		Vec3 forward = horizontalDirection(target.position(), sillad.position(),
				sillad.getLookAngle());
		for (double angle : angles) {
			Vec3 direction = rotateY(forward, Math.toRadians(angle));
			for (double radius : new double[]{8.0D, 10.0D, 12.0D}) {
				Vec3 horizontal = target.position().add(direction.scale(radius));
				BlockPos column = BlockPos.containing(horizontal);
				if (!level.hasChunkAt(column))
					continue;
				int surface = level.getHeight(
						Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
						column.getX(), column.getZ());
				Vec3 candidate = new Vec3(column.getX() + 0.5D, surface,
						column.getZ() + 0.5D);
				if (candidate.distanceToSqr(Vec3.atCenterOf(
						sillad.getEncounterHome())) <= square(ENCOUNTER_LEASH)
						&& isSafePosition(level, sillad, candidate))
					return candidate;
			}
		}
		return null;
	}

	private static boolean isSafePosition(ServerLevel level,
			LivingEntity entity, Vec3 position) {
		BlockPos feet = BlockPos.containing(position);
		BlockPos below = feet.below();
		if (!level.hasChunkAt(feet)
				|| !level.getWorldBorder().isWithinBounds(feet)
				|| position.y <= level.getMinBuildHeight() + 1
				|| position.y >= level.getMaxBuildHeight() - 2
				|| !level.getFluidState(feet).isEmpty()
				|| !level.getFluidState(below).isEmpty()
				|| !level.getBlockState(below).isFaceSturdy(level, below,
						Direction.UP))
			return false;
		AABB moved = entity.getBoundingBox().move(
				position.subtract(entity.position()));
		return level.noCollision(entity, moved);
	}

	private static void knockAway(LivingEntity target, Vec3 origin,
			double horizontalStrength, double verticalStrength) {
		Vec3 away = target.position().subtract(origin)
				.multiply(1.0D, 0.0D, 1.0D);
		if (away.lengthSqr() < 1.0E-5D)
			away = new Vec3(0.0D, 0.0D, 1.0D);
		away = away.normalize().scale(horizontalStrength);
		target.setDeltaMovement(target.getDeltaMovement().add(away.x,
				verticalStrength, away.z));
		target.hurtMarked = true;
	}

	private static void restoreFrozenTicks(LivingEntity target,
			FrostState state) {
		if (target.getTicksFrozen() <= 120)
			target.setTicksFrozen(Math.min(target.getTicksFrozen(),
					state.previousFrozenTicks));
	}

	private static double distanceToSegmentSqr(Vec3 point, Vec3 start,
			Vec3 end) {
		Vec3 segment = end.subtract(start);
		double length = segment.lengthSqr();
		if (length < 1.0E-8D)
			return point.distanceToSqr(start);
		double t = Mth.clamp(point.subtract(start).dot(segment) / length,
				0.0D, 1.0D);
		return point.distanceToSqr(start.add(segment.scale(t)));
	}

	private static double horizontalDistanceSqr(Vec3 first, Vec3 second) {
		double x = first.x - second.x;
		double z = first.z - second.z;
		return x * x + z * z;
	}

	@Nullable
	private static LivingEntity resolveLiving(ServerLevel level,
			@Nullable UUID id) {
		if (id == null)
			return null;
		return level.getEntity(id) instanceof LivingEntity living
				&& living.isAlive() ? living : null;
	}

	@Nullable
	private static LivingEntity nearestPlayer(SilladBossEntity sillad,
			ServerLevel level) {
		return level.players().stream()
				.filter(player -> isValidCombatTarget(sillad, player))
				.min(Comparator.comparingDouble(player ->
						player.distanceToSqr(sillad)))
				.orElse(null);
	}

	private static long cooldownUntil(SilladBossEntity sillad, Action action) {
		return sillad.getPersistentData().getLong(COOLDOWN_PREFIX
				+ action.serializedName());
	}

	private static void setCooldown(SilladBossEntity sillad, Action action,
			long until) {
		sillad.getPersistentData().putLong(COOLDOWN_PREFIX
				+ action.serializedName(), until);
	}

	private static int cooldown(Action action) {
		return switch (action) {
			case FROST_CLEAVE -> 28;
			case ICE_SPEAR -> 110;
			case FLASH_FREEZE -> 130;
			case FROZEN_PATH -> 85;
			case FROST_COUNTER -> 190;
			case STILLNESS_DECREE -> 180;
			case SPIRE_CAGE -> 240;
			case WHITEOUT_PROCESSION -> 240;
			case WINTER_REMEMBERS -> 220;
			case CROWN_OF_WINTER -> 180;
			case GLACIAL_EXECUTION -> 40;
			case FROST_STEP -> 90;
			case ABSOLUTE_ZERO -> Integer.MAX_VALUE;
			default -> 0;
		};
	}

	private static int decisionDelay(SilladBossEntity sillad) {
		return switch (sillad.getCombatPhase()) {
			case 1 -> 8 + sillad.getRandom().nextInt(6);
			case 2 -> 6 + sillad.getRandom().nextInt(6);
			default -> 5 + sillad.getRandom().nextInt(5);
		};
	}

	private static void rememberAction(RuntimeState state, Action action) {
		if (action == Action.FROST_STEP || action == Action.PHASE_TRANSITION)
			return;
		state.recentActions.addFirst(action);
		while (state.recentActions.size() > 2)
			state.recentActions.removeLast();
	}

	private static void playWindup(SilladBossEntity sillad, Action action) {
		SoundEvent sound = switch (action) {
			case FROST_CLEAVE -> SoundEvents.PLAYER_ATTACK_SWEEP;
			case ICE_SPEAR, CROWN_OF_WINTER,
					GLACIAL_EXECUTION -> SoundEvents.TRIDENT_RIPTIDE_1.value();
			case FLASH_FREEZE, FROZEN_PATH -> SoundEvents.POWDER_SNOW_BREAK;
			case FROST_COUNTER -> SoundEvents.SHIELD_BLOCK;
			case STILLNESS_DECREE -> SoundEvents.EVOKER_CAST_SPELL;
			case SPIRE_CAGE -> SoundEvents.WARDEN_HEARTBEAT;
			case WHITEOUT_PROCESSION -> SoundEvents.PHANTOM_FLAP;
			case WINTER_REMEMBERS -> SoundEvents.RESPAWN_ANCHOR_CHARGE;
			case ABSOLUTE_ZERO -> SoundEvents.BEACON_POWER_SELECT;
			case FROST_STEP -> SoundEvents.ENDERMAN_TELEPORT;
			default -> SoundEvents.AMETHYST_BLOCK_CHIME;
		};
		play(sillad, sound, action == Action.ABSOLUTE_ZERO ? 1.2F : 0.85F,
				action == Action.ABSOLUTE_ZERO ? 0.52F : 1.05F);
	}

	private static void play(SilladBossEntity sillad, SoundEvent sound,
			float volume, float pitch) {
		sillad.level().playSound(null, sillad.blockPosition(), sound,
				SoundSource.HOSTILE, volume, pitch);
	}

	private static double square(double value) {
		return value * value;
	}

	private record Candidate(Action action, double score) {
	}

	private record WinterSample(Vec3 position, float yaw, float pitch,
			Vec3 velocity) {
	}

	private static final class RuntimeState {
		private final Map<UUID, Float> threat = new HashMap<>();
		private final Map<UUID, Long> retaliationUntil = new HashMap<>();
		private final Deque<Action> recentActions = new ArrayDeque<>();
		private final Set<UUID> primaryHits = new HashSet<>();
		private final Set<UUID> secondaryHits = new HashSet<>();
		private final Set<UUID> capturedIds = new HashSet<>();
		private final Map<UUID, Integer> crownHits = new HashMap<>();
		private final List<WinterSample> winterSamples = new ArrayList<>();
		private final List<UUID> prisonTargets = new ArrayList<>();
		private final Vec3[] lanceDirections = new Vec3[4];

		private Vec3 lastPosition;
		private Vec3 actionOrigin = Vec3.ZERO;
		private Vec3 targetPoint = Vec3.ZERO;
		private Vec3 aim = new Vec3(0.0D, 0.0D, 1.0D);
		private Vec3 spearEndpoint;
		private Vec3 pathEndpoint;
		private Vec3 stepDestination;
		private UUID targetId;
		private UUID counterAttackerId;
		private WhiteoutState whiteout;
		private long nextDecisionAt;
		private long nextPathAt;
		private long nextStrafeAt;
		private long targetLockedUntil;
		private int stalledTicks;
		private int transitionTargetPhase;
		private int gapIndex;
		private int capturedProjectiles;
		private int counterImpactTick;
		private int winterLostSightTicks;
		private int strafeDirection = 1;
		private float recentDamage;
		private boolean counterTriggered;
		private boolean pathBlocked;

		private RuntimeState(SilladBossEntity sillad) {
			lastPosition = sillad.position();
		}

		private void resetActionRuntime() {
			primaryHits.clear();
			secondaryHits.clear();
			capturedIds.clear();
			crownHits.clear();
			winterSamples.clear();
			prisonTargets.clear();
			for (int index = 0; index < lanceDirections.length; index++)
				lanceDirections[index] = null;
			targetId = null;
			counterAttackerId = null;
			spearEndpoint = null;
			pathEndpoint = null;
			stepDestination = null;
			capturedProjectiles = 0;
			counterImpactTick = 0;
			winterLostSightTicks = 0;
			counterTriggered = false;
			pathBlocked = false;
		}
	}

	private static final class FrostState {
		private final UUID ownerId;
		private final int previousFrozenTicks;
		private int stacks;
		private long stacksExpireAt;
		private long frozenUntil = -1L;
		private long freezeImmuneUntil = -1L;
		private long brittleAt = -1L;
		private long brittleUntil = -1L;
		private long executionReadyUntil = -1L;
		private boolean hadFreeze;
		private boolean restored;

		private FrostState(UUID ownerId, int previousFrozenTicks) {
			this.ownerId = ownerId;
			this.previousFrozenTicks = previousFrozenTicks;
		}
	}

	private static final class WhiteoutState {
		private final Vec3 origin;
		private final Vec3 direction;
		private final long startedAt;
		private final long expiresAt;
		private final Set<UUID> hitTargets = new HashSet<>();
		private final Set<UUID> slowedProjectiles = new HashSet<>();

		private WhiteoutState(Vec3 origin, Vec3 direction, long startedAt,
				long expiresAt) {
			this.origin = origin;
			this.direction = direction;
			this.startedAt = startedAt;
			this.expiresAt = expiresAt;
		}
	}
}
