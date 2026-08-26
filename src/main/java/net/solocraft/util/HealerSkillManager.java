package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.HealerVfxEntity;
import net.solocraft.network.ClassPassiveMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.compat.PacketDistributor;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
 * Server-authoritative Healer runtime with Intelligence-milestone staging.
 *
 * <p>Every ability resolves an output stage from effective Intelligence using
 * the same thresholds the Mage managers use, and each stage <em>changes what
 * the ability does</em> rather than only scaling its numbers. A stage-one Heal
 * Beam is a single heal; a stage-five Heal Beam channels, cleanses, chains and
 * leaves a regeneration mark.</p>
 *
 * <p>Two long-standing defects are fixed by construction here. Healing never
 * schedules per-step tasks with unbounded entity scans, and a blank party
 * string resolves to "no party" rather than matching every other unaffiliated
 * entity — the old procedures buffed hostile mobs when the caster had no
 * party.</p>
 *
 * <p>Nothing checks class or style. Ownership decides access.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class HealerSkillManager {
	// Saint — focused support
	public static final String HEAL_BEAM = "Heal Beam";
	public static final String HASTE_BUFF = "Haste Buff";
	public static final String PURIFICATION = "Purification";
	public static final String PHYSICAL_BUFF = "Physical Buff";
	public static final String OVERHEAL = "Overheal";
	public static final String BLESSING_MARK = "Blessing Mark";
	// Shepherd — area support
	public static final String HEALING_PULSE = "Healing Pulse";
	public static final String CAMOUFLAGE = "Camouflage";
	public static final String PURIFYING_WAVE = "Purifying Wave";
	public static final String GUARDIAN_STEP = "Guardian Step";
	public static final String SANCTUARY = "Sanctuary";
	public static final String SECOND_WIND = "Second Wind";
	// Universal blessings
	public static final String GUARDIAN_WARD = "Guardian Ward";
	public static final String MANA_FONT = "Mana Font";
	public static final String VITALITY_SURGE = "Vitality Surge";
	public static final String DIVINE_FAVOR = "Divine Favor";

	private static final List<String> MANAGED_SKILLS = List.of(
			HEAL_BEAM, HASTE_BUFF, PURIFICATION, PHYSICAL_BUFF, OVERHEAL,
			BLESSING_MARK, HEALING_PULSE, CAMOUFLAGE, PURIFYING_WAVE,
			GUARDIAN_STEP, SANCTUARY, SECOND_WIND, GUARDIAN_WARD, MANA_FONT,
			VITALITY_SURGE, DIVINE_FAVOR);

	/** Debuffs a cleanse may remove, most disabling first. */
	private static final List<Holder<MobEffect>> CLEANSABLE = List.of(
			MobEffects.MOVEMENT_SLOWDOWN, MobEffects.BLINDNESS, MobEffects.WEAKNESS,
			MobEffects.POISON, MobEffects.WITHER, MobEffects.CONFUSION,
			MobEffects.DIG_SLOWDOWN, MobEffects.HUNGER, MobEffects.LEVITATION);

	private static final int ALLY_CAP = 5;
	private static final long BLESSING_DURATION = 1200L;
	private static final long SANCTUARY_DURATION = 200L;
	private static final Map<UUID, HealerState> STATES = new ConcurrentHashMap<>();

	private HealerSkillManager() {
	}

	// ── Intelligence staging ──────────────────────────────────────────────────

	/**
	 * Output stage from effective Intelligence, matching the Mage thresholds so
	 * the whole game shares one progression curve.
	 */
	public static int outputStage(Entity healer) {
		if (healer == null)
			return 0;
		double intelligence = Math.max(0.0D,
				TemporaryStatBonusManager.effectiveIntelligence(healer));
		if (intelligence >= 110.0D)
			return 5;
		if (intelligence >= 80.0D)
			return 4;
		if (intelligence >= 55.0D)
			return 3;
		if (intelligence >= 30.0D)
			return 2;
		return 1;
	}

	public static String stageName(int stage) {
		return switch (Mth.clamp(stage, 0, 5)) {
			case 1 -> "Tending";
			case 2 -> "Mending";
			case 3 -> "Restoring";
			case 4 -> "Radiant";
			case 5 -> "Divine";
			default -> "Dormant";
		};
	}

	/** Healing budget. Intelligence enters output here and cost nowhere. */
	private static float healBudget(ServerPlayer healer, int stage, double base,
			double weight) {
		double intelligence = Math.max(0.0D,
				TemporaryStatBonusManager.effectiveIntelligence(healer));
		double perception = Math.max(0.0D,
				TemporaryStatBonusManager.effectivePerception(healer));
		return (float) ((base + intelligence * weight + perception * 0.04D)
				* (0.85D + stage * 0.15D));
	}

	// ── dispatch ──────────────────────────────────────────────────────────────

	public static boolean isHealerSkill(String skill) {
		return skill != null && MANAGED_SKILLS.contains(skill.trim());
	}

	public static boolean activateSkill(ServerPlayer player, String requestedSkill) {
		if (player == null || !player.isAlive() || requestedSkill == null)
			return false;
		String skill = requestedSkill.trim();
		int stage = outputStage(player);
		HealerState state = state(player);
		return switch (skill) {
			case HEAL_BEAM -> castHealBeam(player, state, stage);
			case HASTE_BUFF -> castHasteBuff(player, state, stage);
			case PURIFICATION -> castPurification(player, state, stage);
			case PHYSICAL_BUFF -> castPhysicalBuff(player, state, stage);
			case OVERHEAL -> castOverheal(player, state, stage);
			case BLESSING_MARK -> castBlessingMark(player, state, stage);
			case HEALING_PULSE -> castHealingPulse(player, state, stage);
			case CAMOUFLAGE -> castCamouflage(player, state, stage);
			case PURIFYING_WAVE -> castPurifyingWave(player, state, stage);
			case GUARDIAN_STEP -> castGuardianStep(player, state, stage);
			case SANCTUARY -> castSanctuary(player, state, stage);
			case SECOND_WIND -> castSecondWind(player, state, stage);
			case GUARDIAN_WARD -> castGuardianWard(player, state, stage);
			case MANA_FONT -> castManaFont(player, state, stage);
			case VITALITY_SURGE -> castVitalitySurge(player, state, stage);
			case DIVINE_FAVOR -> castDivineFavor(player, state, stage);
			default -> false;
		};
	}

	// ── Saint ─────────────────────────────────────────────────────────────────

	/**
	 * Stage 1 heals once. Stage 2 adds recovery ticks, 3 cleanses, 4 chains to a
	 * second wounded ally, 5 leaves a regeneration mark.
	 */
	private static boolean castHealBeam(ServerPlayer healer, HealerState state, int stage) {
		LivingEntity target = healer.isShiftKeyDown() ? healer : lookAlly(healer, 24.0D);
		if (target == null)
			return fail(healer, "No ally in sight");
		if (!ready(healer, HEAL_BEAM)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.LOW)))
			return notEnoughMana(healer);
		setCooldown(healer, HEAL_BEAM, 120);

		float amount = healBudget(healer, stage, 6.0D, 0.18D);
		if (healer.isShiftKeyDown())
			amount *= 0.7F;
		applyHeal(healer, state, target, amount);
		beam(healer, target);

		if (stage >= 2)
			target.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
					60 + stage * 20, stage >= 4 ? 1 : 0, false, false));
		if (stage >= 3)
			cleanse(target, 1);
		if (stage >= 4) {
			LivingEntity chained = lowestAlly(healer, target.position(), 8.0D, target);
			if (chained != null) {
				applyHeal(healer, state, chained, amount * 0.5F);
				beam(healer, chained);
			}
		}
		if (stage >= 5)
			target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false));
		play(healer, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.4F);
		return true;
	}

	/** Stage 3 widens to two allies; stage 5 adds jump and step assistance. */
	private static boolean castHasteBuff(ServerPlayer healer, HealerState state, int stage) {
		List<LivingEntity> allies = buffTargets(healer, stage >= 3 ? 2 : 1, 20.0D);
		if (!ready(healer, HASTE_BUFF)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.MEDIUM)))
			return notEnoughMana(healer);
		setCooldown(healer, HASTE_BUFF, 300);

		int duration = 200 + stage * 60;
		for (LivingEntity ally : allies) {
			ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration,
					stage >= 4 ? 1 : 0, false, false));
			if (stage >= 2)
				ally.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, duration, 0, false, false));
			if (stage >= 5)
				ally.addEffect(new MobEffectInstance(MobEffects.JUMP, duration, 0, false, false));
			sparkle(healer, ally);
		}
		play(healer, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.7F);
		return true;
	}

	/** Removal count rises with stage; 3 heals per removal, 5 cleanses two allies. */
	private static boolean castPurification(ServerPlayer healer, HealerState state, int stage) {
		// buffTargets never returns empty — it falls back to the caster — so a
		// solo healer can always cleanse themselves.
		List<LivingEntity> allies = buffTargets(healer, stage >= 5 ? 2 : 1, 20.0D);
		int removals = Math.min(4, 1 + stage / 2);
		int cleansed = 0;
		for (LivingEntity ally : allies)
			cleansed += countCleansable(ally, removals)
					+ countPurifiableCurses(ally, stage);
		// A cleanse with nothing to remove is free, per the spec's preflight rule.
		if (cleansed == 0)
			return fail(healer, "Nothing to purify");
		if (!ready(healer, PURIFICATION)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.MEDIUM)))
			return notEnoughMana(healer);
		setCooldown(healer, PURIFICATION, 200);

		for (LivingEntity ally : allies) {
			// Purification is the dedicated curse-breaker, so it spends its budget
			// on curses first and mundane debuffs with whatever is left.
			int removed = purifyCurses(healer, ally, stage, removals);
			removed += cleanse(ally, Math.max(0, removals - removed));
			if (stage >= 3 && removed > 0)
				applyHeal(healer, state, ally, healBudget(healer, stage, 2.0D, 0.06D) * removed);
			if (stage >= 4 && removed > 0)
				ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false));
			sigil(healer, ally, CLEANSE_COLOR, 24);
		}
		play(healer, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.7F, 1.5F);
		return true;
	}

	/** Stage 2 adds armor, 3 widens to two allies, 5 adds knockback resistance. */
	private static boolean castPhysicalBuff(ServerPlayer healer, HealerState state, int stage) {
		List<LivingEntity> allies = buffTargets(healer, stage >= 3 ? 2 : 1, 20.0D);
		if (!ready(healer, PHYSICAL_BUFF)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.HIGH)))
			return notEnoughMana(healer);
		setCooldown(healer, PHYSICAL_BUFF, 400);

		int duration = 300 + stage * 60;
		for (LivingEntity ally : allies) {
			ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration,
					stage >= 4 ? 1 : 0, false, false));
			if (stage >= 2)
				ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false));
			if (stage >= 5)
				ally.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, duration, 1, false, false));
			sparkle(healer, ally);
		}
		play(healer, SoundEvents.ANVIL_USE, 0.5F, 1.6F);
		return true;
	}

	/**
	 * Converts recent effective overhealing into absorption on a real ally, not
	 * blanket absorption on every entity nearby.
	 */
	private static boolean castOverheal(ServerPlayer healer, HealerState state, int stage) {
		// Sneak self-casts; otherwise the aimed ally, falling back to self.
		LivingEntity target = healer.isShiftKeyDown() ? healer : lookAlly(healer, 20.0D);
		if (target == null)
			target = healer;
		if (!ready(healer, OVERHEAL)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.HIGH)))
			return notEnoughMana(healer);
		setCooldown(healer, OVERHEAL, 400);

		// Real excess healing feeds the shield, capped by stage.
		double cap = 4.0D + stage * 4.0D;
		float shield = (float) Mth.clamp(state.storedOverheal * 0.6D, 4.0D, cap);
		state.storedOverheal = 0.0D;
		int amplifier = Mth.clamp((int) (shield / 4.0F), 0, 4);
		target.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
				200 + stage * 40, amplifier, false, false));
		if (stage >= 3) {
			LivingEntity second = lowestAlly(healer, target.position(), 8.0D, target);
			if (second != null)
				second.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
						160, Math.max(0, amplifier - 1), false, false));
		}
		if (stage >= 5)
			target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1, false, false));
		sparkle(healer, target);
		play(healer, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8F, 0.9F);
		return true;
	}

	/**
	 * A persistent mark on one ally that banks a share of this healer's output
	 * and releases it automatically when that ally drops below a threshold.
	 */
	private static boolean castBlessingMark(ServerPlayer healer, HealerState state, int stage) {
		// Sneak marks yourself, so a solo healer can still bank a rescue.
		LivingEntity target = healer.isShiftKeyDown() ? healer : lookAlly(healer, 20.0D);
		if (target == null)
			return fail(healer, "No ally in sight");
		if (!ready(healer, BLESSING_MARK)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.APEX)))
			return notEnoughMana(healer);
		setCooldown(healer, BLESSING_MARK, 1200);

		state.blessedAlly = target.getUUID();
		state.blessingUntil = healer.level().getGameTime() + BLESSING_DURATION;
		state.blessingStored = healBudget(healer, stage, 8.0D, 0.22D);
		state.blessingReleases = stage >= 5 ? 2 : 1;
		target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0, false, false));
		sparkle(healer, target);
		play(healer, SoundEvents.BELL_BLOCK, 0.8F, 1.5F);
		message(healer, "Blessing Mark  " + stageName(stage));
		return true;
	}

	// ── Shepherd ──────────────────────────────────────────────────────────────

	/** Caster-centred pulse. Stage 3 cleanses, stage 5 pulses a second time. */
	private static boolean castHealingPulse(ServerPlayer healer, HealerState state, int stage) {
		if (!ready(healer, HEALING_PULSE)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.MEDIUM)))
			return notEnoughMana(healer);
		setCooldown(healer, HEALING_PULSE, 160);
		pulse(healer, state, stage, 1.0F);
		if (stage >= 5)
			SololevelingMod.queueServerWork(20, () -> {
				if (healer.isAlive())
					pulse(healer, state, stage, 0.6F);
			});
		return true;
	}

	private static void pulse(ServerPlayer healer, HealerState state, int stage, float scale) {
		float amount = healBudget(healer, stage, 4.0D, 0.12D) * scale;
		for (LivingEntity ally : alliesAround(healer, healer.position(), 6.0D, ALLY_CAP)) {
			applyHeal(healer, state, ally, amount);
			if (stage >= 3)
				cleanse(ally, 1);
			sparkle(healer, ally);
		}
		applyHeal(healer, state, healer, amount);
		ring(healer, healer.position(), 6.0D);
		play(healer, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.2F);
	}

	/** Threat reduction and concealment. Stage 4 conceals a nearby ally too. */
	private static boolean castCamouflage(ServerPlayer healer, HealerState state, int stage) {
		if (!ready(healer, CAMOUFLAGE)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.LOW)))
			return notEnoughMana(healer);
		setCooldown(healer, CAMOUFLAGE, 240);
		int duration = 60 + stage * 20;
		healer.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration, 0, false, false));
		healer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false));
		for (net.minecraft.world.entity.Mob mob : healer.serverLevel().getEntitiesOfClass(
				net.minecraft.world.entity.Mob.class,
				healer.getBoundingBox().inflate(12.0D),
				candidate -> candidate.getTarget() == healer))
			mob.setTarget(null);
		if (stage >= 4) {
			LivingEntity ally = lowestAlly(healer, healer.position(), 6.0D, healer);
			if (ally != null)
				ally.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, duration / 2, 0, false, false));
		}
		play(healer, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.6F, 1.4F);
		return true;
	}

	/** Wave cleanse plus a smaller heal. Removal count rises with stage. */
	private static boolean castPurifyingWave(ServerPlayer healer, HealerState state, int stage) {
		if (!ready(healer, PURIFYING_WAVE)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.MEDIUM)))
			return notEnoughMana(healer);
		setCooldown(healer, PURIFYING_WAVE, 300);
		int removals = Math.min(3, 1 + stage / 2);
		float amount = healBudget(healer, stage, 3.0D, 0.08D);
		for (LivingEntity ally : alliesAround(healer, healer.position(), 8.0D, ALLY_CAP)) {
			int lifted = purifyCurses(healer, ally, stage, removals);
			cleanse(ally, Math.max(0, removals - lifted));
			applyHeal(healer, state, ally, amount);
			if (stage >= 4)
				ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 80, 0, false, false));
			sigil(healer, ally, CLEANSE_COLOR, 20);
		}
		// The caster is inside their own wave. Cleansing them but not healing
		// them made this useless for a healer playing alone.
		int selfLifted = purifyCurses(healer, healer, stage, removals);
		cleanse(healer, Math.max(0, removals - selfLifted));
		applyHeal(healer, state, healer, amount);
		sigil(healer, healer, CLEANSE_COLOR, 20);
		HealerVfxEntity.wave(healer.serverLevel(), healer.position(), stage,
				8.0F, CLEANSE_COLOR, 20);
		play(healer, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.1F);
		return true;
	}

	/** Dash to an ally and shield both. Fails free when no safe spot exists. */
	private static boolean castGuardianStep(ServerPlayer healer, HealerState state, int stage) {
		LivingEntity ally = lookAlly(healer, 16.0D);
		if (ally == null)
			return fail(healer, "No ally in sight");
		Vec3 destination = safeNear(healer, ally.position());
		if (destination == null)
			return fail(healer, "No safe landing");
		if (!ready(healer, GUARDIAN_STEP)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.MEDIUM)))
			return notEnoughMana(healer);
		setCooldown(healer, GUARDIAN_STEP, 240);

		healer.teleportTo(destination.x, destination.y, destination.z);
		healer.setDeltaMovement(Vec3.ZERO);
		healer.hurtMarked = true;
		int amplifier = Mth.clamp(stage - 1, 0, 3);
		healer.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, amplifier, false, false));
		ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, amplifier, false, false));
		if (stage >= 4) {
			applyHeal(healer, state, ally, healBudget(healer, stage, 4.0D, 0.10D));
			applyHeal(healer, state, healer, healBudget(healer, stage, 4.0D, 0.10D));
		}
		sparkle(healer, ally);
		play(healer, SoundEvents.ENDER_PEARL_THROW, 0.6F, 1.5F);
		return true;
	}

	/** A stationary healing field. Stage 4 adds resistance inside it. */
	private static boolean castSanctuary(ServerPlayer healer, HealerState state, int stage) {
		if (!ready(healer, SANCTUARY)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.HIGH)))
			return notEnoughMana(healer);
		setCooldown(healer, SANCTUARY, 600);
		state.sanctuaryCenter = healer.position();
		state.sanctuaryUntil = healer.level().getGameTime() + SANCTUARY_DURATION;
		state.sanctuaryNextPulse = healer.level().getGameTime() + 20L;
		state.sanctuaryStage = stage;
		// One persistent field entity for the whole duration.
		field(healer, state.sanctuaryCenter, 6.0D, (int) SANCTUARY_DURATION);
		play(healer, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.3F);
		message(healer, "Sanctuary  " + stageName(stage));
		return true;
	}

	/** Emergency party recovery weighted by how hurt each ally is. */
	private static boolean castSecondWind(ServerPlayer healer, HealerState state, int stage) {
		if (!ready(healer, SECOND_WIND)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.APEX)))
			return notEnoughMana(healer);
		setCooldown(healer, SECOND_WIND, 1200);
		float base = healBudget(healer, stage, 10.0D, 0.28D);
		List<LivingEntity> allies = alliesAround(healer, healer.position(), 12.0D, ALLY_CAP);
		allies.add(healer);
		for (LivingEntity ally : allies) {
			float missing = ally.getMaxHealth() - ally.getHealth();
			applyHeal(healer, state, ally, base + Math.min(base, missing * 0.35F));
			cleanse(ally, 1);
			ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,
					80 + stage * 20, 0, false, false));
			if (stage >= 4)
				ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1, false, false));
			sparkle(healer, ally);
		}
		ring(healer, healer.position(), 12.0D);
		play(healer, SoundEvents.TOTEM_USE, 0.9F, 1.1F);
		return true;
	}

	// ── universal blessings ───────────────────────────────────────────────────

	private static boolean castGuardianWard(ServerPlayer healer, HealerState state, int stage) {
		List<LivingEntity> allies = buffTargets(healer, stage >= 3 ? 3 : 1, 20.0D);
		if (!ready(healer, GUARDIAN_WARD)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.MEDIUM)))
			return notEnoughMana(healer);
		setCooldown(healer, GUARDIAN_WARD, 300);
		int duration = 200 + stage * 40;
		for (LivingEntity ally : allies) {
			ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration,
					stage >= 4 ? 1 : 0, false, false));
			if (stage >= 2)
				ally.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, duration, 0, false, false));
			if (stage >= 5)
				ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 1, false, false));
			sparkle(healer, ally);
		}
		play(healer, SoundEvents.SHIELD_BLOCK, 0.7F, 1.4F);
		return true;
	}

	private static boolean castManaFont(ServerPlayer healer, HealerState state, int stage) {
		List<LivingEntity> allies = buffTargets(healer, stage >= 3 ? 2 : 1, 20.0D);
		if (!ready(healer, MANA_FONT)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.MEDIUM)))
			return notEnoughMana(healer);
		setCooldown(healer, MANA_FONT, 400);
		double restore = 60.0D + stage * 40.0D;
		for (LivingEntity ally : allies) {
			if (ally instanceof Player recipient)
				recipient.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.ifPresent(vars -> {
							vars.MP = Math.min(ManaRules.maximumMana(recipient), vars.MP + restore);
							vars.syncPlayerVariables(recipient);
						});
			if (stage >= 4)
				ally.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 200, 0, false, false));
			sparkle(healer, ally);
		}
		play(healer, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.9F);
		return true;
	}

	private static boolean castVitalitySurge(ServerPlayer healer, HealerState state, int stage) {
		List<LivingEntity> allies = buffTargets(healer, stage >= 4 ? 2 : 1, 20.0D);
		if (!ready(healer, VITALITY_SURGE)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.HIGH)))
			return notEnoughMana(healer);
		setCooldown(healer, VITALITY_SURGE, 500);
		int duration = 300 + stage * 60;
		for (LivingEntity ally : allies) {
			ally.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, duration,
					Mth.clamp(stage - 1, 0, 3), false, false));
			applyHeal(healer, state, ally, healBudget(healer, stage, 6.0D, 0.15D));
			if (stage >= 3)
				ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 0, false, false));
			sparkle(healer, ally);
		}
		play(healer, SoundEvents.PLAYER_LEVELUP, 0.5F, 1.6F);
		return true;
	}

	/** The capstone blessing: several buffs at once, widening with stage. */
	private static boolean castDivineFavor(ServerPlayer healer, HealerState state, int stage) {
		List<LivingEntity> allies = buffTargets(healer, Math.min(ALLY_CAP, 1 + stage), 20.0D);
		if (!ready(healer, DIVINE_FAVOR)
				|| !ManaRules.spend(healer, ManaRules.cost(healer, ManaRules.Band.APEX)))
			return notEnoughMana(healer);
		setCooldown(healer, DIVINE_FAVOR, 900);
		int duration = 240 + stage * 60;
		for (LivingEntity ally : allies) {
			ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false));
			ally.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duration, 0, false, false));
			ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false));
			if (stage >= 3)
				ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration / 2, 0, false, false));
			if (stage >= 5)
				ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, 2, false, false));
			applyHeal(healer, state, ally, healBudget(healer, stage, 5.0D, 0.12D));
			sparkle(healer, ally);
		}
		ring(healer, healer.position(), 10.0D);
		play(healer, SoundEvents.TOTEM_USE, 0.8F, 1.4F);
		return true;
	}

	// ── ally resolution ───────────────────────────────────────────────────────

	/**
	 * The D-6 fix. A blank party string means "no party", never "everyone with
	 * no party", and a hostile entity is never an ally regardless.
	 */
	public static boolean isValidAlly(ServerPlayer healer, LivingEntity candidate) {
		if (candidate == null || !candidate.isAlive())
			return false;
		if (candidate == healer)
			return true;
		if (candidate instanceof Player player && player.isSpectator())
			return false;
		String healerParty = healer.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(vars -> vars.party).orElse("");
		if (healerParty != null && !healerParty.isBlank()) {
			String otherParty = candidate.getCapability(
					SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.map(vars -> vars.party).orElse("");
			if (healerParty.equals(otherParty))
				return true;
		}
		// Owned shadows, tamed companions and explicit vanilla alliance.
		return MageCombatHelper.areAllied(healer, candidate);
	}

	private static LivingEntity lookAlly(ServerPlayer healer, double range) {
		Vec3 start = healer.getEyePosition();
		Vec3 forward = healer.getLookAngle().normalize();
		LivingEntity best = null;
		double bestDot = 0.85D;
		for (LivingEntity candidate : healer.serverLevel().getEntitiesOfClass(
				LivingEntity.class, healer.getBoundingBox().inflate(range),
				other -> other != healer && isValidAlly(healer, other))) {
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

	/**
	 * Aimed ally first, otherwise the most wounded allies in range.
	 *
	 * <p>Sneaking is an explicit self-cast, matching Heal Beam. A support
	 * player should never have to aim away from the fight to buff themselves,
	 * and without this a buff silently landed on whoever happened to be under
	 * the crosshair.</p>
	 */
	private static List<LivingEntity> buffTargets(ServerPlayer healer, int cap, double range) {
		if (healer.isShiftKeyDown())
			return List.of(healer);
		List<LivingEntity> result = new ArrayList<>();
		LivingEntity aimed = lookAlly(healer, range);
		if (aimed != null)
			result.add(aimed);
		for (LivingEntity ally : alliesAround(healer, healer.position(), range, cap)) {
			if (result.size() >= cap)
				break;
			if (!result.contains(ally))
				result.add(ally);
		}
		if (result.isEmpty())
			result.add(healer);
		return result.subList(0, Math.min(cap, result.size()));
	}

	private static List<LivingEntity> alliesAround(ServerPlayer healer, Vec3 center,
			double radius, int cap) {
		List<LivingEntity> allies = new ArrayList<>(healer.serverLevel().getEntitiesOfClass(
				LivingEntity.class, new AABB(center, center).inflate(radius),
				candidate -> candidate != healer && isValidAlly(healer, candidate)));
		allies.sort(Comparator.comparingDouble(
				ally -> ally.getHealth() - ally.getMaxHealth()));
		return new ArrayList<>(allies.subList(0, Math.min(cap, allies.size())));
	}

	private static LivingEntity lowestAlly(ServerPlayer healer, Vec3 center, double radius,
			LivingEntity exclude) {
		for (LivingEntity ally : alliesAround(healer, center, radius, ALLY_CAP)) {
			if (ally != exclude && ally.getHealth() < ally.getMaxHealth())
				return ally;
		}
		return null;
	}

	// ── healing and cleansing ─────────────────────────────────────────────────

	/** Applies a heal and banks the genuinely wasted portion for Overheal. */
	private static void applyHeal(ServerPlayer healer, HealerState state,
			LivingEntity target, float amount) {
		if (amount <= 0.0F || target == null)
			return;
		float missing = Math.max(0.0F, target.getMaxHealth() - target.getHealth());
		float effective = Math.min(amount, missing);
		float excess = amount - effective;
		if (effective > 0.0F)
			target.heal(effective);
		state.storedOverheal = Math.min(120.0D, state.storedOverheal + excess);
		if (effective > 0.0F)
			state.resonance = Math.min(5, state.resonance + 1);
		syncResonance(healer, state);
	}

	private static int countCleansable(LivingEntity target, int limit) {
		int found = 0;
		for (Holder<MobEffect> effect : CLEANSABLE) {
			if (found >= limit)
				break;
			if (target.hasEffect(effect))
				found++;
		}
		return found;
	}

	private static int cleanse(LivingEntity target, int limit) {
		int removed = 0;
		for (Holder<MobEffect> effect : CLEANSABLE) {
			if (removed >= limit)
				break;
			if (target.hasEffect(effect)) {
				target.removeEffect(effect);
				removed++;
			}
		}
		return removed;
	}

	/**
	 * Curses a healer of this stage is able to strip.
	 *
	 * <p>Deliberately separate from the ordinary cleanse: only the dedicated
	 * purifiers may touch curses, so a passive Sanctuary tick or a Blessing Mark
	 * reaction cannot quietly wipe a Doom mark someone spent an Apex cast placing.
	 */
	private static int countPurifiableCurses(LivingEntity target, int purifierStage) {
		int found = 0;
		for (CurseType curse : CurseState.activeCurses(target)) {
			if (CurseType.canPurify(purifierStage, CurseState.stageOf(target, curse)))
				found++;
		}
		return found;
	}

	/**
	 * Strips what this healer can reach and reports what resisted.
	 *
	 * <p>A curse woven two Intelligence stages above the healer does not come off
	 * at all. Its power was frozen when it was cast, so it keeps resisting even
	 * after its weaver has died or wandered off -- which is the normal case when a
	 * generated Curse hunter is the one who placed it.
	 */
	private static int purifyCurses(ServerPlayer healer, LivingEntity target,
			int purifierStage, int limit) {
		int removed = 0;
		int resisted = 0;
		CurseType strongest = null;
		for (CurseType curse : CurseState.activeCurses(target)) {
			int curseStage = CurseState.stageOf(target, curse);
			if (!CurseType.canPurify(purifierStage, curseStage)) {
				resisted++;
				if (strongest == null || curseStage > CurseState.stageOf(target, strongest))
					strongest = curse;
				continue;
			}
			if (removed >= limit)
				continue;
			CurseState.clear(target, curse);
			removed++;
		}
		if (resisted > 0 && strongest != null)
			healer.displayClientMessage(Component.literal(strongest.displayName()
					+ " resists - the weave is beyond your Intelligence.")
					.withStyle(ChatFormatting.DARK_PURPLE), true);
		return removed;
	}

	// ── events ────────────────────────────────────────────────────────────────

	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onAllyDamaged(LivingIncomingDamageEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim.level().isClientSide())
			return;
		for (HealerState state : STATES.values()) {
			if (!victim.getUUID().equals(state.blessedAlly) || state.blessingReleases <= 0)
				continue;
			float after = victim.getHealth() - event.getAmount();
			if (after > victim.getMaxHealth() * 0.3F)
				continue;
			ServerPlayer owner = victim.getServer() == null ? null
					: victim.getServer().getPlayerList().getPlayer(state.ownerId);
			if (owner == null)
				continue;
			victim.heal(state.blessingStored);
			cleanse(victim, 1);
			victim.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 1, false, false));
			state.blessingReleases--;
			if (state.blessingReleases <= 0) {
				state.blessedAlly = null;
				state.blessingUntil = 0L;
			}
			owner.level().playSound(null, victim.blockPosition(), SoundEvents.BELL_BLOCK,
					SoundSource.PLAYERS, 0.9F, 1.8F);
			break;
		}
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer healer))
			return;
		HealerState state = STATES.get(healer.getUUID());
		if (state == null)
			return;
		long now = healer.level().getGameTime();

		if (state.blessedAlly != null && state.blessingUntil < now) {
			state.blessedAlly = null;
			state.blessingUntil = 0L;
			state.blessingReleases = 0;
		}
		// One bounded field tick, not a scheduled task per step.
		if (state.sanctuaryUntil > 0L) {
			if (state.sanctuaryUntil < now) {
				state.sanctuaryUntil = 0L;
				state.sanctuaryCenter = null;
			} else if (state.sanctuaryNextPulse <= now && state.sanctuaryCenter != null) {
				state.sanctuaryNextPulse = now + 20L;
				float amount = healBudget(healer, state.sanctuaryStage, 2.5D, 0.07D);
				for (LivingEntity ally : alliesAround(healer, state.sanctuaryCenter, 6.0D, ALLY_CAP)) {
					applyHeal(healer, state, ally, amount);
					if (state.sanctuaryStage >= 4)
						ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false));
				}
				if (healer.position().distanceToSqr(state.sanctuaryCenter) <= 36.0D)
					applyHeal(healer, state, healer, amount);
				ring(healer, state.sanctuaryCenter, 6.0D);
			}
		}
		if (state.resonance > 0 && now % 200L == 0L) {
			state.resonance--;
			syncResonance(healer, state);
		}
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

	public static void resetPlayerState(ServerPlayer player) {
		if (player == null)
			return;
		HealerState removed = STATES.remove(player.getUUID());
		if (removed == null)
			return;
		removed.resonance = 0;
		removed.storedOverheal = 0.0D;
		removed.blessedAlly = null;
		removed.sanctuaryUntil = 0L;
		syncResonance(player, removed);
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private static HealerState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), id -> {
			HealerState created = new HealerState();
			created.ownerId = id;
			return created;
		});
	}

	private static void syncResonance(ServerPlayer player, HealerState state) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(3, state.resonance));
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

	private static boolean fail(ServerPlayer player, String reason) {
		message(player, reason);
		return false;
	}

	private static boolean notEnoughMana(ServerPlayer player) {
		message(player, "Not enough MP");
		return false;
	}

	private static Vec3 safeNear(ServerPlayer player, Vec3 target) {
		ServerLevel level = player.serverLevel();
		for (double offset : new double[] { 1.2D, 1.8D, 2.4D }) {
			for (int angle = 0; angle < 8; angle++) {
				double radians = angle * Math.PI / 4.0D;
				Vec3 candidate = target.add(Math.cos(radians) * offset, 0.0D,
						Math.sin(radians) * offset);
				if (level.hasChunkAt(net.minecraft.core.BlockPos.containing(candidate))
						&& level.noCollision(player, player.getBoundingBox()
								.move(candidate.subtract(player.position()))))
					return candidate;
			}
		}
		return null;
	}

	// ── visuals ───────────────────────────────────────────────────────────────
	// One carrier entity per effect. No scheduled per-tick particle work: the
	// old Heal Beam queued hundreds of delayed tasks for a single cast.

	/** Soft green-white restoration. */
	private static final int HEAL_COLOR = 0x57E08C;
	/** Gold blessing and absorption. */
	private static final int BLESS_COLOR = 0xFFE68C;
	/** Cyan cleansing. */
	private static final int CLEANSE_COLOR = 0x8CF0FF;
	/** Mint field perimeter. */
	private static final int FIELD_COLOR = 0x8CFFC6;

	private static void beam(ServerPlayer healer, LivingEntity target) {
		HealerVfxEntity.beam(healer.serverLevel(), healer.getEyePosition(),
				target.getBoundingBox().getCenter(), outputStage(healer),
				HEAL_COLOR, 14);
	}

	/** Ground sigil under a recipient; its ring count reads as the stage. */
	private static void sparkle(ServerPlayer healer, LivingEntity target) {
		sigil(healer, target, BLESS_COLOR, 24);
	}

	private static void sigil(ServerPlayer healer, LivingEntity target, int color,
			int lifetime) {
		HealerVfxEntity.sigil(healer.serverLevel(),
				new Vec3(target.getX(), target.getY() + 0.05D, target.getZ()),
				outputStage(healer), 1.1F, color, lifetime);
	}

	private static void ring(ServerPlayer healer, Vec3 center, double radius) {
		HealerVfxEntity.wave(healer.serverLevel(), center, outputStage(healer),
				(float) radius, FIELD_COLOR, 20);
	}

	private static void field(ServerPlayer healer, Vec3 center, double radius,
			int lifetime) {
		HealerVfxEntity.field(healer.serverLevel(), center, outputStage(healer),
				(float) radius, FIELD_COLOR, lifetime);
	}

	private static void play(ServerPlayer player, net.minecraft.sounds.SoundEvent sound,
			float volume, float pitch) {
		player.level().playSound(null, player.blockPosition(), sound,
				SoundSource.PLAYERS, volume, pitch);
	}

	private static void message(ServerPlayer player, String text) {
		player.displayClientMessage(Component.literal(text)
				.withStyle(ChatFormatting.GREEN), true);
	}

	private static final class HealerState {
		private UUID ownerId;
		private int resonance;
		private double storedOverheal;
		private UUID blessedAlly;
		private long blessingUntil;
		private float blessingStored;
		private int blessingReleases;
		private Vec3 sanctuaryCenter;
		private long sanctuaryUntil;
		private long sanctuaryNextPulse;
		private int sanctuaryStage;
	}
}
