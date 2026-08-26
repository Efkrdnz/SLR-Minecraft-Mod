package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.SlashEffectEntity;
import net.solocraft.network.ClassPassiveMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.network.compat.PacketDistributor;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative runtime for the Fighter styles.
 *
 * <p>Nothing here checks a player's class or style. Access is decided entirely
 * by ownership: if a hunter learned the ability, they can cast it, and the
 * resource that ability feeds builds for them too. Style trees only decide what
 * an evaluation hands out.</p>
 *
 * <p>Impact deliberately lives on the <em>target</em>, keyed by caster UUID,
 * the same ownership shape Fire Mage's Scorch uses, so two Strikers cannot
 * steal each other's guard progress.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class FighterSkillManager {
	// Existing kit
	public static final String GROUND_SLAM = "Ground Slam";
	public static final String CROSS_STRIKE = "Cross Strike";
	public static final String SLASH_DASH = "Slash Dash";
	public static final String SWORD_BEAM = "Sword Beam";
	public static final String SLASH_FURY = "Slash Fury";
	public static final String SWORD_DANCE = "Sword Dance";
	public static final String SWORD_OF_LIGHT = "Sword of Light";
	// Striker
	public static final String IRON_KNUCKLE = "Iron Knuckle";
	public static final String BREAKER_COMBO = "Breaker Combo";
	public static final String METEOR_FIST = "Meteor Fist";
	public static final String TITANS_BARRAGE = "Titan's Barrage";
	// Swordsman
	public static final String RADIANT_EXECUTION = "Radiant Execution";
	// Ravager
	public static final String MAGICAL_EYE = "Magical Eye";
	public static final String CLAW_STRIKES = "Claw Strikes";
	public static final String BEAST_SENSE = "Beast Sense";
	public static final String PARTIAL_TRANSFORMATION = "Partial Transformation";
	public static final String PREDATOR_RUSH = "Predator Rush";
	public static final String FULL_BEAST_TRANSFORMATION = "Full Beast Transformation";

	/** Abilities this manager owns outright. The legacy seven stay elsewhere. */
	private static final List<String> MANAGED_SKILLS = List.of(
			IRON_KNUCKLE, BREAKER_COMBO, METEOR_FIST, TITANS_BARRAGE,
			RADIANT_EXECUTION, MAGICAL_EYE, CLAW_STRIKES, BEAST_SENSE,
			PARTIAL_TRANSFORMATION, PREDATOR_RUSH, FULL_BEAST_TRANSFORMATION);

	/** Abilities that stack Impact on whatever they hit. */
	private static final List<String> IMPACT_SOURCES = List.of(
			GROUND_SLAM, CROSS_STRIKE, IRON_KNUCKLE, BREAKER_COMBO,
			METEOR_FIST, TITANS_BARRAGE);
	/** Abilities that can spend Sword Tempo. */
	private static final List<String> TEMPO_SPENDERS = List.of(
			SLASH_FURY, SWORD_DANCE, SWORD_OF_LIGHT, RADIANT_EXECUTION);
	/** Abilities that make Feral meaningful. */
	private static final List<String> FERAL_OWNERS = List.of(
			CLAW_STRIKES, BEAST_SENSE, PARTIAL_TRANSFORMATION, PREDATOR_RUSH,
			FULL_BEAST_TRANSFORMATION);

	private static final ResourceKey<DamageType> FIGHTER_DAMAGE = ResourceKey.create(
			Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:fighter"));
	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(
			Registries.ENTITY_TYPE, ResourceLocation.parse("soloboss"));

	private static final String IMPACT_DATA = "slr_fighter_impact";
	private static final String REND_DATA = "slr_fighter_rend";
	public static final String PARTIAL_AURA = "ravager_partial";
	public static final String FULL_AURA = "ravager_full";

	private static final int IMPACT_CAP = 5;
	private static final long IMPACT_DECAY_TICKS = 120L;
	private static final long GUARD_BROKEN_TICKS = 80L;
	private static final long GUARD_BROKEN_LOCKOUT = 200L;
	private static final int REND_CAP = 3;
	private static final long REND_DURATION = 240L;
	private static final int MAX_TEMPO = 5;
	private static final float TEMPO_MULTIPLIER = 1.3F;
	private static final double FERAL_MAX = 100.0D;
	private static final double PARTIAL_FERAL_COST = 40.0D;
	private static final double FULL_FERAL_COST = 80.0D;

	// Radiant Execution. Documented as a B-rank line strike, so it is tuned to sit
	// well under Titan's Barrage, the Apex capstone it was previously beating by a
	// wide margin on both burst and sustained damage.
	private static final int RADIANT_COOLDOWN = 300;
	private static final double RADIANT_BASE = 11.0D;
	private static final double RADIANT_STRENGTH_SCALE = 0.32D;
	private static final double RADIANT_WEAPON_SCALE = 0.85D;
	private static final double RADIANT_PERCEPTION_SCALE = 0.18D;
	/** The line loses bite after the first body it passes through. */
	private static final double RADIANT_SECOND_TARGET_SHARE = 0.45D;
	private static final float RADIANT_EXECUTE_BONUS = 1.15F;

	private static final Map<UUID, CombatState> STATES = new ConcurrentHashMap<>();

	private FighterSkillManager() {
	}

	// ── dispatch ──────────────────────────────────────────────────────────────

	public static boolean isFighterSkill(String skill) {
		return skill != null && MANAGED_SKILLS.contains(skill.trim());
	}

	public static boolean activateSkill(ServerPlayer player, String requestedSkill) {
		if (player == null || !player.isAlive() || requestedSkill == null)
			return false;
		String skill = requestedSkill.trim();
		CombatState state = state(player);
		return switch (skill) {
			case IRON_KNUCKLE -> castIronKnuckle(player, state);
			case BREAKER_COMBO -> castBreakerCombo(player, state);
			case METEOR_FIST -> castMeteorFist(player, state);
			case TITANS_BARRAGE -> castTitansBarrage(player, state);
			case RADIANT_EXECUTION -> castRadiantExecution(player, state);
			case MAGICAL_EYE -> castMagicalEye(player, state);
			case CLAW_STRIKES -> castClawStrikes(player, state);
			case BEAST_SENSE -> castBeastSense(player, state);
			case PARTIAL_TRANSFORMATION -> castTransformation(player, state, 1);
			case FULL_BEAST_TRANSFORMATION -> castTransformation(player, state, 2);
			case PREDATOR_RUSH -> castPredatorRush(player, state);
			default -> false;
		};
	}

	// ── Striker ───────────────────────────────────────────────────────────────

	/**
	 * Hold to brace and charge one armored punch. Cost commits in three steps;
	 * an interrupted early charge only pays for what it committed.
	 */
	private static boolean castIronKnuckle(ServerPlayer player, CombatState state) {
		long now = player.level().getGameTime();
		if (state.knuckleChargeFrom > 0L) {
			releaseIronKnuckle(player, state, now);
			return true;
		}
		if (!ready(player, IRON_KNUCKLE))
			return false;
		int step = Math.max(1, ManaRules.cost(player, ManaRules.Band.MEDIUM) / 3);
		if (!ManaRules.spend(player, step))
			return notEnoughMana(player);
		state.knuckleChargeFrom = now;
		state.knuckleStepsPaid = 1;
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1, false, false));
		play(player, SoundEvents.ANVIL_LAND, 0.35F, 1.8F);
		return true;
	}

	private static void releaseIronKnuckle(ServerPlayer player, CombatState state, long now) {
		long held = now - state.knuckleChargeFrom;
		double agility = Math.max(0.0D, TemporaryStatBonusManager.effectiveAgility(player));
		long perStep = (long) Mth.clamp(28.0D - agility * 0.08D, 16.0D, 28.0D);
		int reached = (int) Mth.clamp(1 + held / perStep, 1, 3);
		float multiplier = switch (reached) {
			case 3 -> 1.9F;
			case 2 -> 1.45F;
			default -> 1.0F;
		};

		state.knuckleChargeFrom = 0L;
		state.knuckleStepsPaid = 0;
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		setCooldown(player, IRON_KNUCKLE, 200);

		LivingEntity target = findLookTarget(player, 4.0D);
		if (target != null) {
			float damage = (float) ((9.0D + strength(player) * 0.55D
					+ weaponDamage(player) * 1.1D) * multiplier);
			if (hurt(player, target, damage)) {
				addImpact(player, target, reached >= 3 ? 3 : 1);
				spawnSlash(player, target.getBoundingBox().getCenter(), 0.0F,
						0.6F + reached * 0.2F, 100);
			}
		}
		play(player, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, 1.0F + reached * 0.15F);
	}

	/**
	 * Input-timed three-part combo. Missing a window ends it safely, and only
	 * the steps that actually landed are paid for.
	 */
	private static boolean castBreakerCombo(ServerPlayer player, CombatState state) {
		long now = player.level().getGameTime();
		boolean continuing = state.comboStep > 0 && state.comboWindowUntil >= now;
		if (!continuing) {
			if (!ready(player, BREAKER_COMBO))
				return false;
			state.comboStep = 0;
		}
		int step = Math.max(1, ManaRules.cost(player, ManaRules.Band.HIGH) / 3);
		if (!ManaRules.spend(player, step))
			return notEnoughMana(player);

		state.comboStep++;
		state.comboWindowUntil = now + 9L;
		LivingEntity target = findLookTarget(player, 4.0D);
		double budget = 14.0D + strength(player) * 0.70D + weaponDamage(player) * 1.3D;
		float portion = switch (state.comboStep) {
			case 1 -> 0.25F;
			case 2 -> 0.30F;
			default -> 0.45F;
		};
		if (target != null && hurt(player, target, (float) (budget * portion))) {
			addImpact(player, target, 1);
			spawnSlash(player, target.getBoundingBox().getCenter(),
					state.comboStep == 2 ? -55.0F : 0.0F, 0.7F, 100);
			if (state.comboStep >= 3) {
				// Finisher: knock away, or spike downward when looking down.
				boolean spike = player.getXRot() > 25.0F;
				Vec3 push = spike ? new Vec3(0.0D, -1.2D, 0.0D)
						: player.getLookAngle().normalize().scale(1.4D).add(0.0D, 0.25D, 0.0D);
				if (!isBoss(target)) {
					target.setDeltaMovement(push);
					target.hurtMarked = true;
				}
				if (spike)
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 3, false, true));
			}
		}
		play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.9F + state.comboStep * 0.2F);

		if (state.comboStep >= 3) {
			state.comboStep = 0;
			state.comboWindowUntil = 0L;
			setCooldown(player, BREAKER_COMBO, 240);
		}
		return true;
	}

	/** Collision-safe leap to a point, then impact plus a small shockwave. */
	private static boolean castMeteorFist(ServerPlayer player, CombatState state) {
		if (!ready(player, METEOR_FIST))
			return false;
		LivingEntity target = findLookTarget(player, 12.0D);
		Vec3 aim = target != null ? target.position()
				: player.position().add(player.getLookAngle().normalize().scale(8.0D));
		Vec3 destination = furthestSafePosition(player, player.position(), aim);
		if (destination.distanceToSqr(player.position()) < 1.0D) {
			message(player, "No safe landing");
			return false;
		}
		double distance = Math.sqrt(destination.distanceToSqr(player.position()));
		double load = Mth.clamp(1.0D + 0.15D * (distance / 4.0D), 1.0D, 1.45D);
		int cost = (int) (ManaRules.cost(player, ManaRules.Band.HIGH) * load);
		if (!ManaRules.spend(player, cost))
			return notEnoughMana(player);

		player.teleportTo(destination.x, destination.y, destination.z);
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;
		setCooldown(player, METEOR_FIST, 400);

		float direct = (float) (16.0D + strength(player) * 0.65D + weaponDamage(player));
		float wave = (float) (6.0D + vitality(player) * 0.30D);
		if (target != null && target.distanceToSqr(player) <= 16.0D && hurt(player, target, direct)) {
			addImpact(player, target, 3);
			spawnSlash(player, target.getBoundingBox().getCenter(), 0.0F, 1.1F, 100);
		}
		int hits = 0;
		for (LivingEntity nearby : nearbyTargets(player, destination, 3.5D)) {
			if (hits >= 6)
				break;
			if (nearby == target)
				continue;
			if (hurt(player, nearby, wave))
				addImpact(player, nearby, 1);
			hits++;
		}
		if (player.serverLevel() != null)
			player.serverLevel().sendParticles(
					net.minecraft.core.particles.ParticleTypes.EXPLOSION,
					destination.x, destination.y + 0.2D, destination.z, 1, 0, 0, 0, 0);
		play(player, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.4F);
		return true;
	}

	/** Bounded rapid-strike apex. Bosses are tracked, never frozen. */
	private static boolean castTitansBarrage(ServerPlayer player, CombatState state) {
		if (!ready(player, TITANS_BARRAGE))
			return false;
		LivingEntity target = findLookTarget(player, 6.0D);
		if (target == null) {
			message(player, "No target");
			return false;
		}
		if (!ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.APEX)))
			return notEnoughMana(player);

		setCooldown(player, TITANS_BARRAGE, 1200);
		UUID targetId = target.getUUID();
		double budget = 30.0D + strength(player) * 1.2D
				+ TemporaryStatBonusManager.effectiveAgility(player) * 0.3D
				+ weaponDamage(player) * 1.6D;
		int strikes = 12;
		for (int index = 0; index < strikes; index++) {
			final int step = index;
			SololevelingMod.queueServerWork(index * 5, () -> {
				if (!player.isAlive())
					return;
				Entity entity = player.serverLevel().getEntity(targetId);
				if (!(entity instanceof LivingEntity living) || !validTarget(player, living))
					return;
				// Rising curve across the sequence rather than a flat split.
				float portion = (float) ((0.4D + step * 0.1D) / strikes);
				living.invulnerableTime = 0;
				hurt(player, living, (float) (budget * portion));
				spawnSlash(player, living.getBoundingBox().getCenter(),
						step % 2 == 0 ? 35.0F : -35.0F, 0.55F, 100);
				play(player, SoundEvents.PLAYER_ATTACK_STRONG, 0.5F, 1.2F + step * 0.05F);
				if (step == strikes - 1) {
					float finisher = (float) (12.0D + vitality(player) * 0.4D);
					living.invulnerableTime = 0;
					if (hurt(player, living, finisher)) {
						applyGuardBroken(player, living);
						if (!isBoss(living)) {
							living.setDeltaMovement(player.getLookAngle().normalize()
									.scale(1.6D).add(0.0D, 0.5D, 0.0D));
							living.hurtMarked = true;
						}
					}
					spawnSlash(player, living.getBoundingBox().getCenter(), 0.0F, 1.4F, 101);
					play(player, SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 1.1F);
				}
			});
		}
		return true;
	}

	// ── Swordsman ─────────────────────────────────────────────────────────────

	/** Narrow precision line cut through up to two targets. */
	private static boolean castRadiantExecution(ServerPlayer player, CombatState state) {
		if (!ready(player, RADIANT_EXECUTION))
			return false;
		if (!ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.HIGH)))
			return notEnoughMana(player);
		setCooldown(player, RADIANT_EXECUTION, RADIANT_COOLDOWN);
		play(player, SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.6F);

		float tempoBonus = consumeTempo(player, state);
		Vec3 origin = player.getEyePosition();
		Vec3 forward = player.getLookAngle().normalize();
		// 14-tick draw, then one cut.
		SololevelingMod.queueServerWork(14, () -> {
			if (!player.isAlive())
				return;
			double budget = RADIANT_BASE + strength(player) * RADIANT_STRENGTH_SCALE
					+ weaponDamage(player) * RADIANT_WEAPON_SCALE
					+ perception(player) * RADIANT_PERCEPTION_SCALE;
			List<LivingEntity> line = new ArrayList<>();
			for (LivingEntity candidate : nearbyTargets(player, origin.add(forward.scale(5.0D)), 6.0D)) {
				Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(origin);
				if (toTarget.normalize().dot(forward) > 0.93D && toTarget.length() <= 10.0D)
					line.add(candidate);
			}
			line.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(origin)));
			int hits = 0;
			for (LivingEntity victim : line) {
				if (hits >= 2)
					break;
				// The cut spends one budget down the line rather than paying it in
				// full to everyone it touches. Handing each target the whole budget
				// made a B-rank ability out-damage the class's Apex capstone.
				double share = hits == 0 ? 1.0D : RADIANT_SECOND_TARGET_SHARE;
				float damage = (float) (budget * share * tempoBonus);
				// Execution bonus against the badly wounded, capped.
				if (victim.getHealth() / Math.max(1.0F, victim.getMaxHealth()) < 0.3F)
					damage *= RADIANT_EXECUTE_BONUS;
				hurt(player, victim, damage);
				spawnSlash(player, victim.getBoundingBox().getCenter(), 0.0F, 1.3F, 102);
				hits++;
			}
			play(player, SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 0.8F);
		});
		return true;
	}

	// ── Ravager ───────────────────────────────────────────────────────────────

	private static boolean castMagicalEye(ServerPlayer player, CombatState state) {
		if (!ready(player, MAGICAL_EYE))
			return false;
		if (!ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.NOMINAL)))
			return notEnoughMana(player);
		setCooldown(player, MAGICAL_EYE, 200);
		double radius = Mth.clamp(10.0D + perception(player) * 0.05D, 10.0D, 16.0D);
		int revealed = 0;
		for (LivingEntity target : nearbyTargets(player, player.position(), radius)) {
			if (revealed >= 8)
				break;
			if (target.getHealth() / Math.max(1.0F, target.getMaxHealth()) >= 0.7F)
				continue;
			target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0, false, false));
			revealed++;
		}
		state.magicalEyeUntil = player.level().getGameTime() + 120L;
		play(player, SoundEvents.ENDERMAN_STARE, 0.5F, 1.6F);
		return true;
	}

	/** Three-swipe chain applying Rend. Looking down focuses the final tear. */
	private static boolean castClawStrikes(ServerPlayer player, CombatState state) {
		if (!ready(player, CLAW_STRIKES))
			return false;
		if (!ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.LOW)))
			return notEnoughMana(player);
		setCooldown(player, CLAW_STRIKES, 120);

		double budget = 9.0D + strength(player) * 0.4D + weaponDamage(player);
		float[] portions = { 0.30F, 0.30F, 0.40F };
		for (int swipe = 0; swipe < 3; swipe++) {
			final int index = swipe;
			SololevelingMod.queueServerWork(swipe * 5, () -> {
				if (!player.isAlive())
					return;
				boolean focused = index == 2 && player.getXRot() > 20.0F;
				List<LivingEntity> targets = nearbyTargets(player,
						player.position().add(player.getLookAngle().normalize().scale(2.0D)),
						focused ? 2.0D : 3.2D);
				int hits = 0;
				int cap = focused ? 1 : 3;
				for (LivingEntity target : targets) {
					if (hits >= cap)
						break;
					target.invulnerableTime = 0;
					float damage = (float) (budget * portions[index] * (focused ? 1.3D : 0.7D));
					if (hurt(player, target, damage))
						addRend(player, target, focused ? 2 : 1);
					hits++;
				}
				spawnSlash(player, player.position().add(0.0D, 1.0D, 0.0D)
						.add(player.getLookAngle().normalize().scale(1.8D)),
						index * 40.0F - 40.0F, 0.75F, 100);
				play(player, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.5F + index * 0.15F);
			});
		}
		return true;
	}

	private static boolean castBeastSense(ServerPlayer player, CombatState state) {
		if (!ready(player, BEAST_SENSE))
			return false;
		if (!ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.MEDIUM)))
			return notEnoughMana(player);
		setCooldown(player, BEAST_SENSE, 300);
		double agility = Math.max(0.0D, TemporaryStatBonusManager.effectiveAgility(player));
		int duration = (int) Mth.clamp(100.0D + agility * 0.2D, 100.0D, 160.0D);
		state.beastSenseUntil = player.level().getGameTime() + duration;
		player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 1, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 0, false, false));
		play(player, SoundEvents.WOLF_GROWL, 0.6F, 1.3F);
		return true;
	}

	/**
	 * Partial and full beast forms. Both are maintained states with a per-second
	 * drain that Feral reduces; the visual is the vessel spiritualization aura at
	 * reduced scale rather than a model swap.
	 */
	private static boolean castTransformation(ServerPlayer player, CombatState state, int stage) {
		long now = player.level().getGameTime();
		if (state.formStage == stage) {
			endTransformation(player, state);
			return true;
		}
		if (VesselManager.currentDefinition(player) != null) {
			message(player, "A vessel transformation is already active");
			return false;
		}
		String skill = stage == 2 ? FULL_BEAST_TRANSFORMATION : PARTIAL_TRANSFORMATION;
		if (!ready(player, skill))
			return false;
		// Creative bypasses resource gates the same way it bypasses mana and
		// cooldowns; grinding a meter to test a transformation is backwards.
		double required = stage == 2 ? FULL_FERAL_COST : PARTIAL_FERAL_COST;
		boolean free = ManaRules.isFree(player);
		if (!free && state.feral < required) {
			message(player, "Feral " + (int) state.feral + "/" + (int) required);
			return false;
		}
		ManaRules.Band band = stage == 2 ? ManaRules.Band.APEX : ManaRules.Band.HIGH;
		if (!ManaRules.spend(player, ManaRules.cost(player, band)))
			return notEnoughMana(player);

		if (!free)
			state.feral -= required;
		state.formStage = stage;
		int duration = stage == 2
				? (int) Mth.clamp(240.0D + vitality(player) * 0.6D, 240.0D, 400.0D)
				: 400;
		state.formUntil = now + duration;
		state.formNextDrain = now + 20L;
		setCooldown(player, skill, stage == 2 ? 1200 : 400);
		syncFeral(player, state);

		PlayerAuraSystem.setContinuous(player, stage == 2 ? FULL_AURA : PARTIAL_AURA,
				stage == 2 ? 1.0F : 0.65F);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration,
				stage == 2 ? 1 : 0, false, false));
		if (stage == 2)
			player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false));
		play(player, SoundEvents.RAVAGER_ROAR, 0.9F, stage == 2 ? 0.7F : 1.1F);
		return true;
	}

	private static void endTransformation(ServerPlayer player, CombatState state) {
		if (state.formStage == 0)
			return;
		state.formStage = 0;
		state.formUntil = 0L;
		state.formNextDrain = 0L;
		PlayerAuraSystem.clearContinuous(player);
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
	}

	/** Bound to a wounded or Rended target through safe waypoints, then maul. */
	private static boolean castPredatorRush(ServerPlayer player, CombatState state) {
		if (!ready(player, PREDATOR_RUSH))
			return false;
		LivingEntity target = findLookTarget(player, 16.0D);
		if (target == null) {
			message(player, "No target");
			return false;
		}
		Vec3 approach = target.position().subtract(
				player.getLookAngle().normalize().scale(1.2D));
		Vec3 destination = furthestSafePosition(player, player.position(), approach);
		if (destination.distanceToSqr(player.position()) < 1.0D) {
			message(player, "No safe approach");
			return false;
		}
		double distance = Math.sqrt(destination.distanceToSqr(player.position()));
		int cost = (int) (ManaRules.cost(player, ManaRules.Band.HIGH)
				* Mth.clamp(1.0D + distance / 40.0D, 1.0D, 1.4D));
		if (!ManaRules.spend(player, cost))
			return notEnoughMana(player);

		setCooldown(player, PREDATOR_RUSH, 400);
		player.teleportTo(destination.x, destination.y, destination.z);
		player.setDeltaMovement(Vec3.ZERO);
		player.hurtMarked = true;

		float maul = (float) (20.0D + strength(player) * 0.8D + weaponDamage(player) * 1.2D);
		if (getRend(player, target) >= REND_CAP)
			maul *= 1.3F;
		if (hurt(player, target, maul)) {
			addRend(player, target, 1);
			spawnSlash(player, target.getBoundingBox().getCenter(), 0.0F, 1.25F, 100);
		}
		play(player, SoundEvents.RAVAGER_ATTACK, 0.9F, 1.2F);
		return true;
	}

	// ── passives ──────────────────────────────────────────────────────────────

	private static boolean ownsAny(ServerPlayer player, List<String> skills) {
		String list = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(vars -> vars.Plist).orElse("");
		if (list == null || list.isBlank())
			return false;
		for (String entry : list.split(",")) {
			String token = entry.trim();
			while (token.startsWith("."))
				token = token.substring(1).trim();
			if (skills.contains(token))
				return true;
		}
		return false;
	}

	/** Impact stacks live on the target, keyed by caster, capped and expiring. */
	private static void addImpact(ServerPlayer player, LivingEntity target, int amount) {
		long now = player.level().getGameTime();
		CompoundTag root = target.getPersistentData().getCompound(IMPACT_DATA);
		String key = player.getUUID().toString();
		CompoundTag entry = root.getCompound(key);
		int stacks = entry.getLong("Expires") >= now ? entry.getInt("Stacks") : 0;
		stacks = Math.min(IMPACT_CAP, stacks + amount);
		entry.putInt("Stacks", stacks);
		entry.putLong("Expires", now + IMPACT_DECAY_TICKS);
		root.put(key, entry);
		target.getPersistentData().put(IMPACT_DATA, root);

		if (stacks >= IMPACT_CAP) {
			applyGuardBroken(player, target);
			entry.putInt("Stacks", 0);
			root.put(key, entry);
			target.getPersistentData().put(IMPACT_DATA, root);
		}
	}

	private static void applyGuardBroken(ServerPlayer player, LivingEntity target) {
		long now = player.level().getGameTime();
		CompoundTag data = target.getPersistentData();
		if (data.getLong("slr_guard_broken_until") + GUARD_BROKEN_LOCKOUT > now)
			return;
		data.putLong("slr_guard_broken_until", now + GUARD_BROKEN_TICKS);
		if (isBoss(target) || target instanceof Player) {
			target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,
					(int) GUARD_BROKEN_TICKS, 0, false, true));
		} else {
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3, false, true));
		}
		play(player, SoundEvents.SHIELD_BREAK, 0.8F, 1.1F);
	}

	private static void addRend(ServerPlayer player, LivingEntity target, int amount) {
		long now = player.level().getGameTime();
		CompoundTag data = target.getPersistentData();
		int stacks = data.getLong(REND_DATA + "_until") >= now ? data.getInt(REND_DATA) : 0;
		data.putInt(REND_DATA, Math.min(REND_CAP, stacks + amount));
		data.putLong(REND_DATA + "_until", now + REND_DURATION);
	}

	private static int getRend(ServerPlayer player, LivingEntity target) {
		CompoundTag data = target.getPersistentData();
		return data.getLong(REND_DATA + "_until") >= player.level().getGameTime()
				? data.getInt(REND_DATA) : 0;
	}

	private static void addTempo(ServerPlayer player, CombatState state, String action) {
		if (!ownsAny(player, TEMPO_SPENDERS))
			return;
		long now = player.level().getGameTime();
		Long last = state.lastTempoActions.get(action);
		if (last != null && now - last < 6L)
			return;
		state.lastTempoActions.put(action, now);
		if (state.tempo < MAX_TEMPO) {
			state.tempo++;
			syncTempo(player, state);
		}
		state.nextTempoDecay = now + 100L;
	}

	/** Multiplies an already-computed budget. Never a separate damage instance. */
	private static float consumeTempo(ServerPlayer player, CombatState state) {
		if (state.tempo < MAX_TEMPO)
			return 1.0F;
		state.tempo = 0;
		state.nextTempoDecay = Long.MAX_VALUE;
		syncTempo(player, state);
		play(player, SoundEvents.PLAYER_ATTACK_STRONG, 0.7F, 1.5F);
		return TEMPO_MULTIPLIER;
	}

	private static void addFeral(ServerPlayer player, CombatState state, double amount) {
		if (!ownsAny(player, FERAL_OWNERS))
			return;
		double updated = Mth.clamp(state.feral + Math.min(15.0D, amount), 0.0D, FERAL_MAX);
		if (Math.abs(updated - state.feral) < 0.01D)
			return;
		state.feral = updated;
		syncFeral(player, state);
	}

	// ── events ────────────────────────────────────────────────────────────────

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingHurt(LivingIncomingDamageEvent event) {
		if (event.getSource().getEntity() instanceof ServerPlayer attacker
				&& !attacker.level().isClientSide()) {
			// Created on first eligible hit, not just on first cast, so Tempo
			// and Feral build from ordinary combat the way they should.
			CombatState state = STATES.get(attacker.getUUID());
			if (state == null && isEligible(attacker))
				state = state(attacker);
			if (state != null && !state.internalDamage) {
				LivingEntity target = event.getEntity();
				if (validTarget(attacker, target)) {
					int rend = getRend(attacker, target);
					if (rend > 0)
						event.setAmount(event.getAmount() * (1.0F + 0.08F * rend));
					if (isSwordLike(attacker))
						addTempo(attacker, state, "sword-hit");
					addFeral(attacker, state, 1.5D + strength(attacker) * 0.01D);
					addDrive(attacker, state, event.getAmount());
				}
			}
		}
		if (event.getEntity() instanceof ServerPlayer victim
				&& !victim.level().isClientSide()) {
			CombatState state = STATES.get(victim.getUUID());
			if (state == null && isEligible(victim))
				state = state(victim);
			if (state != null)
				addFeral(victim, state, 3.0D + vitality(victim) * 0.02D);
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		CombatState state = STATES.get(player.getUUID());
		if (state == null)
			return;
		long now = player.level().getGameTime();

		// Self-healing: losing the abilities that feed these resources — by
		// reset, class change, or any other path — retires the state and the
		// bars without needing every caller to remember to do it.
		if (now % 40L == 0L && !isEligible(player)) {
			resetPlayerState(player);
			return;
		}

		if (state.tempo > 0 && state.nextTempoDecay <= now) {
			state.tempo--;
			state.nextTempoDecay = state.tempo > 0 ? now + 30L : Long.MAX_VALUE;
			syncTempo(player, state);
		}
		if (state.comboStep > 0 && state.comboWindowUntil < now) {
			state.comboStep = 0;
			state.comboWindowUntil = 0L;
		}
		// A charge held far past its final step releases on its own rather than
		// leaving the player slowed forever.
		if (state.knuckleChargeFrom > 0L && now - state.knuckleChargeFrom > 120L)
			releaseIronKnuckle(player, state, now);
		if (state.formStage > 0) {
			if (state.formUntil < now) {
				endTransformation(player, state);
			} else if (state.formNextDrain <= now) {
				state.formNextDrain = now + 20L;
				ManaRules.Band band = state.formStage == 2
						? ManaRules.Band.APEX : ManaRules.Band.HIGH;
				// Feral above 60 makes the form cheaper to hold.
				double reduction = state.feral >= 60.0D ? 0.6D : 1.0D;
				int upkeep = Math.max(1,
						(int) (ManaRules.cost(player, band) / 50.0D * reduction));
				if (!ManaRules.spend(player, upkeep))
					endTransformation(player, state);
			}
		}
		if (state.feral > 0.0D && now % 20L == 0L && player.getLastHurtByMob() == null
				&& player.getLastHurtMob() == null) {
			state.feral = Math.max(0.0D, state.feral - 2.0D);
			syncFeral(player, state);
		}
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player && isEligible(player))
			state(player);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			resetPlayerState(player);
	}

	@SubscribeEvent
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			resetPlayerState(player);
	}

	@SubscribeEvent
	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			resetPlayerState(player);
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			resetPlayerState(player);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		STATES.clear();
	}

	/** Clears one player's Fighter runtime. Safe to call repeatedly. */
	public static void resetPlayerState(ServerPlayer player) {
		if (player == null)
			return;
		CombatState removed = STATES.remove(player.getUUID());
		if (removed != null && removed.formStage > 0)
			endTransformation(player, removed);
		// Always tell the client to hide these bars, even when no state
		// existed. A reset or class change otherwise leaves an ability-driven
		// bar latched on for the rest of the session.
		hideBars(player);
	}

	private static void hideBars(ServerPlayer player) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(6, ClassPassiveClientState.UNAVAILABLE));
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(7, ClassPassiveClientState.UNAVAILABLE));
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private static boolean isEligible(ServerPlayer player) {
		return ownsAny(player, IMPACT_SOURCES) || ownsAny(player, TEMPO_SPENDERS)
				|| ownsAny(player, FERAL_OWNERS);
	}

	private static CombatState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), ignored -> new CombatState());
	}

	private static void addDrive(ServerPlayer player, CombatState state, float damage) {
		double gain = Math.min(12.0D, 2.0D + strength(player) * 0.02D + damage * 0.2D);
		state.drive = Mth.clamp(state.drive + gain, 0.0D, 100.0D);
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(1, state.drive));
	}

	private static void syncTempo(ServerPlayer player, CombatState state) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(6, state.tempo));
	}

	private static void syncFeral(ServerPlayer player, CombatState state) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(7, state.feral));
	}

	private static boolean ready(ServerPlayer player, String key) {
		if (player.isCreative()) {
			CooldownManager.clear(player, key);
			return true;
		}
		if (!CooldownManager.isOnCooldown(player, key))
			return true;
		message(player, key + "  "
				+ Math.max(1, (CooldownManager.getRemainingTicks(player, key) + 19) / 20) + "s");
		return false;
	}

	private static void setCooldown(ServerPlayer player, String key, int ticks) {
		if (player.isCreative())
			CooldownManager.clear(player, key);
		else
			CooldownManager.setFullDuration(player, key, ticks);
		CooldownManager.set(player, "mana_refresh", 40);
	}

	private static boolean notEnoughMana(ServerPlayer player) {
		message(player, "Not enough MP");
		return false;
	}

	private static boolean hurt(ServerPlayer player, LivingEntity target, float amount) {
		if (amount <= 0.0F || !validTarget(player, target))
			return false;
		CombatState state = state(player);
		DamageSource source = new DamageSource(player.level().registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FIGHTER_DAMAGE), player);
		state.internalDamage = true;
		try {
			boolean hurt = target.hurt(source, Math.max(0.5F, amount));
			if (hurt)
				target.setLastHurtByPlayer(player);
			return hurt;
		} finally {
			state.internalDamage = false;
		}
	}

	private static List<LivingEntity> nearbyTargets(ServerPlayer player, Vec3 center, double radius) {
		List<LivingEntity> targets = new ArrayList<>(player.serverLevel().getEntitiesOfClass(
				LivingEntity.class, new AABB(center, center).inflate(radius),
				candidate -> validTarget(player, candidate)));
		targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(center)));
		return targets;
	}

	private static LivingEntity findLookTarget(ServerPlayer player, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 forward = player.getLookAngle().normalize();
		LivingEntity best = null;
		double bestDot = 0.82D;
		for (LivingEntity candidate : nearbyTargets(player, start.add(forward.scale(range * 0.5D)), range)) {
			Vec3 toTarget = candidate.getBoundingBox().getCenter().subtract(start);
			if (toTarget.length() > range)
				continue;
			double dot = toTarget.normalize().dot(forward);
			if (dot > bestDot) {
				bestDot = dot;
				best = candidate;
			}
		}
		return best;
	}

	private static boolean validTarget(ServerPlayer player, LivingEntity target) {
		return MageCombatHelper.isValidTarget(player, target);
	}

	private static Vec3 furthestSafePosition(ServerPlayer player, Vec3 start, Vec3 intended) {
		Vec3 delta = intended.subtract(start);
		for (int step = 10; step >= 0; step--) {
			Vec3 candidate = start.add(delta.scale(step / 10.0D));
			if (isSafePosition(player, candidate))
				return candidate;
		}
		return start;
	}

	private static boolean isSafePosition(ServerPlayer player, Vec3 position) {
		net.minecraft.core.BlockPos blockPos = net.minecraft.core.BlockPos.containing(position);
		ServerLevel level = player.serverLevel();
		if (!level.hasChunkAt(blockPos) || !level.getWorldBorder().isWithinBounds(blockPos))
			return false;
		return level.noCollision(player,
				player.getBoundingBox().move(position.subtract(player.position())));
	}

	private static boolean isBoss(LivingEntity target) {
		return !(target instanceof Player)
				&& (target.getType().is(BOSS_TAG) || target.getMaxHealth() >= 250.0F);
	}

	private static boolean isSwordLike(ServerPlayer player) {
		return player.getMainHandItem().getItem() instanceof net.minecraft.world.item.SwordItem;
	}

	private static double strength(ServerPlayer player) {
		return Math.max(0.0D, TemporaryStatBonusManager.effectiveStrength(player));
	}

	private static double vitality(ServerPlayer player) {
		return Math.max(0.0D, TemporaryStatBonusManager.effectiveVitality(player));
	}

	private static double perception(ServerPlayer player) {
		return Math.max(0.0D, TemporaryStatBonusManager.effectivePerception(player));
	}

	private static double weaponDamage(ServerPlayer player) {
		return Math.max(1.0D, player.getAttributeValue(Attributes.ATTACK_DAMAGE));
	}

	private static void spawnSlash(ServerPlayer player, Vec3 center, float roll,
			float scale, int variant) {
		SlashEffectEntity.spawn(player.serverLevel(), player,
				center.x, center.y, center.z, player.getYRot(), player.getXRot(),
				roll, scale, 0.0F, variant);
	}

	private static void play(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
			float volume, float pitch) {
		player.level().playSound(null, player.blockPosition(), sound,
				SoundSource.PLAYERS, volume, pitch);
	}

	private static void message(ServerPlayer player, String text) {
		player.displayClientMessage(net.minecraft.network.chat.Component.literal(text)
				.withStyle(net.minecraft.ChatFormatting.GOLD), true);
	}

	private static final class CombatState {
		private double drive;
		private int tempo;
		private long nextTempoDecay = Long.MAX_VALUE;
		private final Map<String, Long> lastTempoActions = new java.util.LinkedHashMap<>();
		private long knuckleChargeFrom;
		private int knuckleStepsPaid;
		private int comboStep;
		private long comboWindowUntil;
		private double feral;
		private int formStage;
		private long formUntil;
		private long formNextDrain;
		private long beastSenseUntil;
		private long magicalEyeUntil;
		private boolean internalDamage;
	}
}
