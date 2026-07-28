package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.StormStateMessage;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative mechanics for the Storm Mage specialization.
 *
 * <p>The manager deliberately uses bounded server state and vanilla particles
 * instead of lightning entities, weather changes, block mutation, or chunk
 * tickets. Conductive marks, Lightning Rod, Voltage, Thunderhead, and Tempest
 * are private to each caster.</p>
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class StormMageSpellManager {
	public static final String STATIC_NEEDLE = "Static Needle";
	public static final String SLIPSTREAM = "Slipstream";
	public static final String THUNDERCLAP = "Thunderclap";
	public static final String LIGHTNING_ROD = "Lightning Rod";
	public static final String CHAIN_LIGHTNING = "Chain Lightning";
	public static final String THUNDERHEAD = "Thunderhead";
	public static final String SKYBREAKER = "Skybreaker";
	public static final String TEMPEST_INCARNATE = "Tempest Incarnate";

	public static final Set<String> STORM_SKILLS = Set.of(
			STATIC_NEEDLE, SLIPSTREAM, THUNDERCLAP, LIGHTNING_ROD,
			CHAIN_LIGHTNING, THUNDERHEAD, SKYBREAKER, TEMPEST_INCARNATE);
	public static final Set<String> QTE_SKILLS = Set.of(
			THUNDERHEAD, SKYBREAKER, TEMPEST_INCARNATE);
	public static final Set<String> INSTANT_SKILLS = Set.of(
			STATIC_NEEDLE, SLIPSTREAM, THUNDERCLAP, LIGHTNING_ROD,
			CHAIN_LIGHTNING);
	/** Stable presentation/grant order for Baran's partial Storm inheritance. */
	public static final List<String> BARAN_CROSSOVER_SKILL_ORDER = List.of(
			STATIC_NEEDLE, SLIPSTREAM, CHAIN_LIGHTNING);
	/** Membership view used by authorization checks. */
	public static final Set<String> BARAN_CROSSOVER_SKILLS =
			Set.copyOf(BARAN_CROSSOVER_SKILL_ORDER);

	private static final String VOLTAGE_TAG = "sl_storm_voltage";
	private static final String ROD_HIGHLIGHT_SOURCE = "storm:lightning_rod";
	private static final int ROD_HIGHLIGHT_COLOR = 0xFFD65A;
	private static final int ROD_HIGHLIGHT_PRIORITY =
			EntityHighlightSystem.PRIORITY_PERCEPTION + 70;
	private static final double[] COST_MULTIPLIER =
			{0.0D, 1.0D, 1.10D, 1.20D, 1.30D, 1.40D, 1.50D};
	private static final UUID TEMPEST_SPEED_MODIFIER =
			UUID.fromString("f457662b-592a-47ac-bbdb-b1c524af9ac2");
	private static final int CONDUCTIVE_DURATION = 120;
	private static final int ROD_DURATION = 240;
	private static final int TEMPEST_DURATION = 200;
	private static final double MAX_VOLTAGE = 100.0D;

	private static final Map<UUID, StormState> STATES = new HashMap<>();
	private static final List<NeedleCast> ACTIVE_NEEDLES = new ArrayList<>();
	private static final List<SlipstreamCast> ACTIVE_DASHES = new ArrayList<>();
	private static final List<SkybreakerCast> ACTIVE_SKYBREAKERS = new ArrayList<>();
	private static final List<EchoCast> ACTIVE_ECHOES = new ArrayList<>();

	private static volatile EffectiveStageBonusProvider stageBonusProvider = caster -> 0;

	private StormMageSpellManager() {
	}

	/**
	 * Cancels one player's Storm state and any in-flight casts immediately.
	 */
	public static void resetPlayerState(ServerPlayer player) {
		if (player == null)
			return;
		UUID playerId = player.getUUID();
		ACTIVE_NEEDLES.removeIf(cast -> playerId.equals(cast.casterId));
		boolean wasDashing = ACTIVE_DASHES.removeIf(cast -> playerId.equals(cast.casterId));
		ACTIVE_SKYBREAKERS.removeIf(cast -> playerId.equals(cast.casterId));
		ACTIVE_ECHOES.removeIf(cast -> playerId.equals(cast.casterId));
		removeState(player, true);
		if (wasDashing) {
			player.setDeltaMovement(Vec3.ZERO);
			player.hurtMarked = true;
			player.fallDistance = 0.0F;
		}
	}

	/**
	 * Integration hook for temporary transformations. Baran's Spiritualization
	 * is recognized directly; other transformations may supply the same
	 * temporary mastery bonus through this provider.
	 */
	@FunctionalInterface
	public interface EffectiveStageBonusProvider {
		int bonusStages(Entity caster);
	}

	public static void setEffectiveStageBonusProvider(EffectiveStageBonusProvider provider) {
		stageBonusProvider = provider == null ? caster -> 0 : provider;
	}

	public static void clearEffectiveStageBonusProvider() {
		stageBonusProvider = caster -> 0;
	}

	public static boolean isStormSkill(String skill) {
		return STORM_SKILLS.contains(skill);
	}

	public static boolean isQteSkill(String skill) {
		return QTE_SKILLS.contains(skill);
	}

	public static boolean isInstantSkill(String skill) {
		return INSTANT_SKILLS.contains(skill);
	}

	/** Base Intelligence output, intentionally limited to the normal five stages. */
	public static int outputStage(Entity caster) {
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
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

	/** Base stage plus a transformation-owned, temporary sixth-stage hook. */
	public static int effectiveOutputStage(Entity caster) {
		int base = outputStage(caster);
		boolean whiteFlame = WhiteFlameMonarchManager.isSpiritualized(caster);
		int bonus = whiteFlame ? 1 : 0;
		try {
			bonus = Math.max(bonus, Math.max(0, stageBonusProvider.bonusStages(caster)));
		} catch (RuntimeException ignored) {
			// A transformation integration must never break ordinary Storm casting.
		}
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		int maximum = whiteFlame && intelligence > 110.0D ? 6 : 5;
		return Mth.clamp(base + bonus, 1, maximum);
	}

	/**
	 * Native Storm Mages and the current White-Flame vessel own Storm runtime
	 * state. Generated non-player casters are authorized separately by castNpc.
	 */
	public static boolean hasStormAccess(Entity caster) {
		if (!(caster instanceof Player))
			return false;
		return WhiteFlameMonarchManager.isWhiteFlameVessel(caster)
				|| STORM_SKILLS.stream().anyMatch(skill -> MageSpellProgression.hasSkill(caster, skill));
	}

	public static String stageName(int stage) {
		return switch (Mth.clamp(stage, 0, 6)) {
			case 1 -> "Spark";
			case 2 -> "Current";
			case 3 -> "Surge";
			case 4 -> "Tempest";
			case 5 -> "Stormborn";
			case 6 -> "Sovereign Tempest";
			default -> "Dormant";
		};
	}

	public static List<Component> tooltip(Entity caster, String skill) {
		int base = outputStage(caster);
		int effective = effectiveOutputStage(caster);
		ArrayList<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
		lines.add(Component.literal(description(skill)).withStyle(ChatFormatting.GRAY));
		lines.add(Component.literal("Output: " + stageName(effective)
				+ (effective > base ? " (Spiritualized)" : ""))
				.withStyle(effective >= 6 ? ChatFormatting.GOLD : ChatFormatting.BLUE));
		lines.add(Component.literal(stageEffect(skill, effective)).withStyle(ChatFormatting.DARK_AQUA));
		lines.add(Component.literal("Mana: " + manaCost(caster, skill, effective, QTEResult.MISS)
				+ "  |  Cooldown: " + String.format(Locale.ROOT, "%.1fs",
						cooldownTicks(skill) / 20.0D))
				.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	private static String description(String skill) {
		return switch (skill) {
			case STATIC_NEEDLE -> "Fire a precise bolt that marks enemies Conductive.";
			case SLIPSTREAM -> "Ride a safe four-tick current in your movement direction.";
			case THUNDERCLAP -> "Peel nearby enemies away with a controlled thunder cone.";
			case LIGHTNING_ROD -> "Privately designate an aimed enemy as your routing root.";
			case CHAIN_LIGHTNING -> "Route a bounded lightning chain through visible enemies.";
			case THUNDERHEAD -> "Place a short-lived storm that delivers scheduled strikes.";
			case SKYBREAKER -> "Call a telegraphed vertical finisher onto a marked target or point.";
			case TEMPEST_INCARNATE -> "Wear the storm, accelerating Voltage and creating bounded echoes.";
			default -> "Control motion, marks, and lightning routes.";
		};
	}

	private static String stageEffect(String skill, int stage) {
		return switch (skill) {
			case STATIC_NEEDLE -> stage < 2 ? "Fast single-target mark."
					: stage < 3 ? "Extended precision range."
					: stage < 4 ? "Pierces once."
					: stage < 6 ? "Returns a delayed spark after impact."
					: "White-flame output pierces twice and strengthens its return.";
			case SLIPSTREAM -> stage < 2 ? "A collision-safe directional dash."
					: stage < 4 ? "Cleanses Slowness and can harvest a nearby mark."
					: stage < 6 ? "Stores one extra dash charge per cooldown cycle."
					: "Longer white-flame current with two mark harvests.";
			case THUNDERCLAP -> stage < 4 ? "A forward soft-control cone."
					: stage < 6 ? "Overcharge converts it into a radial peel."
					: "White-flame output widens the cone without hard-controlling players.";
			case LIGHTNING_ROD -> stage < 6 ? "A twelve-second private routing priority."
					: "A sixteen-second white-flame routing priority.";
			case CHAIN_LIGHTNING -> stage < 3 ? "A short controlled chain."
					: stage < 6 ? "Routes up to five unique visible targets."
					: "Routes six targets with reduced jump falloff.";
			case THUNDERHEAD -> stage < 4 ? "Five scheduled strikes over seven seconds."
					: stage < 6 ? "Six smarter strikes and an Overcharged final burst."
					: "The final scheduled strike may split once.";
			case SKYBREAKER -> stage < 4 ? "A focused, readable vertical finisher."
					: stage < 6 ? "Forks into nearby Conductive enemies."
					: "Leaves one reduced white-flame aftershock.";
			case TEMPEST_INCARNATE -> stage < 6
					? "Ten seconds of speed, enhanced Voltage, and every-third-cast echoes."
					: "Twelve seconds of white-flame motion; echo and Overcharge caps remain bounded.";
			default -> "";
		};
	}

	/** Called on the logical server after any required QTE resolves. */
	public static boolean cast(Entity caster, String skill, QTEResult qteResult) {
		if (!(caster.level() instanceof ServerLevel level) || !isStormSkill(skill))
			return false;
		if (caster instanceof Player && !hasStormAccess(caster)) {
			message(caster, "You do not have access to Storm magic.");
			return false;
		}
		if (caster instanceof Player
				&& !MageSpellProgression.canCastLearnedSkill(caster, skill)) {
			message(caster, "You have not learned this Storm skill.");
			return false;
		}

		int stage = effectiveOutputStage(caster);
		StormState state = stateFor(level, caster);
		boolean bonusSlipstream = SLIPSTREAM.equals(skill)
				&& CooldownManager.isOnCooldown(caster, skill)
				&& state.canSpendSlipstreamBonus(level.getGameTime());
		if (CooldownManager.isOnCooldown(caster, skill) && !bonusSlipstream) {
			message(caster, "Ability on cooldown!");
			return false;
		}

		CastTarget target = preflight(level, caster, skill, stage, state);
		if (!target.valid) {
			message(caster, target.failure);
			return false;
		}

		QTEResult result = qteResult == null ? QTEResult.MISS : qteResult;
		int cost = manaCost(caster, skill, stage, result);
		SololevelingModVariables.PlayerVariables data = variables(caster);
		if (!(caster instanceof Player player && player.isCreative()) && data.MP < cost) {
			message(caster, "Not enough MP! Need " + cost + ".");
			return false;
		}

		boolean overcharged = isOverchargeEligible(skill) && consumeOvercharge(state, level.getGameTime());
		boolean started = startSpell(level, caster, skill, stage, state, target, overcharged);
		if (!started)
			return false;

		if (cost > 0)
			deductMana(caster, cost);
		if (bonusSlipstream)
			state.slipstreamBonusAvailable = false;
		else {
			CooldownManager.set(caster, skill, cooldownTicks(skill));
			if (SLIPSTREAM.equals(skill) && (stage >= 4 || state.isTempestActive(level.getGameTime())))
				state.slipstreamBonusAvailable = true;
		}
		CooldownManager.set(caster, "mana_refresh", 40);
		return true;
	}

	/** Conservative no-mana dispatch for generated Storm Mage hunters. */
	public static boolean castNpc(Entity caster, String skill) {
		if (!(caster.level() instanceof ServerLevel level) || !isStormSkill(skill)
				|| CooldownManager.isOnCooldown(caster, skill))
			return false;
		if (caster instanceof Player && !hasStormAccess(caster))
			return false;
		int stage = effectiveOutputStage(caster);
		StormState state = stateFor(level, caster);
		CastTarget target = preflight(level, caster, skill, stage, state);
		if (!target.valid)
			return false;
		boolean overcharged = isOverchargeEligible(skill)
				&& consumeOvercharge(state, level.getGameTime());
		boolean started = startSpell(level, caster, skill, stage, state, target, overcharged);
		if (started)
			CooldownManager.set(caster, skill, cooldownTicks(skill));
		return started;
	}

	public static int manaCost(Entity caster, String skill, int stage, QTEResult result) {
		if (caster instanceof Player player && player.isCreative())
			return 0;
		double percent = switch (skill) {
			case STATIC_NEEDLE -> 0.0035D;
			case SLIPSTREAM -> 0.020D;
			case THUNDERCLAP -> 0.0325D;
			case LIGHTNING_ROD -> 0.040D;
			case CHAIN_LIGHTNING -> 0.080D;
			case THUNDERHEAD -> 0.110D;
			case SKYBREAKER -> 0.130D;
			case TEMPEST_INCARNATE -> 0.180D;
			default -> 0.0D;
		};
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		double maximumMana = 1000.0D + intelligence * 100.0D;
		double qte = isQteSkill(skill)
				? MageQTEHelper.getManaCostMultiplier(
						result == null ? QTEResult.MISS : result, intelligence)
				: 1.0D;
		return Math.max(0, OrbOfAvariceManager.adjustManaCost(caster, maximumMana * percent
				* COST_MULTIPLIER[Mth.clamp(stage, 1, 6)] * qte));
	}

	public static int cooldownTicks(String skill) {
		return switch (skill) {
			case STATIC_NEEDLE -> 9;
			case SLIPSTREAM -> 80;
			case THUNDERCLAP -> 120;
			case LIGHTNING_ROD -> 200;
			case CHAIN_LIGHTNING -> 280;
			case THUNDERHEAD -> 480;
			case SKYBREAKER -> 600;
			case TEMPEST_INCARNATE -> 1200;
			default -> 20;
		};
	}

	public static double getVoltage(Entity caster) {
		if (caster == null)
			return 0.0D;
		StormState state = STATES.get(caster.getUUID());
		return state == null ? caster.getPersistentData().getDouble(VOLTAGE_TAG) : state.voltage;
	}

	public static boolean isConductive(Entity caster, Entity target) {
		if (caster == null || target == null)
			return false;
		StormState state = STATES.get(caster.getUUID());
		return state != null && state.isConductive(target.getUUID(), caster.level().getGameTime());
	}

	public static UUID getLightningRodTarget(Entity caster) {
		if (caster == null)
			return null;
		StormState state = STATES.get(caster.getUUID());
		if (state == null || state.rodExpiresAt < caster.level().getGameTime())
			return null;
		return state.rodTarget;
	}

	private static boolean startSpell(ServerLevel level, Entity caster, String skill, int stage,
			StormState state, CastTarget target, boolean overcharged) {
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		double levelScale = systemLevelDamageFactor(caster);
		return switch (skill) {
			case STATIC_NEEDLE -> startNeedle(level, caster, stage,
					(float) ((1.4D + intelligence * 0.030D) * levelScale), overcharged);
			case SLIPSTREAM -> startSlipstream(level, caster, stage);
			case THUNDERCLAP -> castThunderclap(level, caster, stage,
					(float) ((3.0D + intelligence * 0.045D) * levelScale), state, overcharged);
			case LIGHTNING_ROD -> setLightningRod(level, caster, stage, state, target.entity);
			case CHAIN_LIGHTNING -> castChain(level, caster, stage,
					(float) ((5.0D + intelligence * 0.075D) * levelScale),
					state, target.entity, overcharged);
			case THUNDERHEAD -> startThunderhead(level, caster, stage,
					(float) ((3.0D + intelligence * 0.050D) * levelScale),
					state, target.point, overcharged);
			case SKYBREAKER -> startSkybreaker(level, caster, stage,
					(float) ((13.0D + intelligence * 0.110D) * levelScale),
					state, target, overcharged);
			case TEMPEST_INCARNATE -> startTempest(level, caster, stage, state);
			default -> false;
		};
	}

	private static CastTarget preflight(ServerLevel level, Entity caster, String skill, int stage,
			StormState state) {
		if (LIGHTNING_ROD.equals(skill)) {
			LivingEntity target = findLookTarget(level, caster, 28.0D + stage * 3.0D);
			return target == null ? CastTarget.fail("No visible enemy is under your aim.")
					: CastTarget.entity(target);
		}
		if (CHAIN_LIGHTNING.equals(skill)) {
			LivingEntity root = activeRodTarget(level, caster, state, 48.0D);
			if (root == null)
				root = findLookTarget(level, caster, 26.0D + stage * 3.0D);
			return root == null ? CastTarget.fail("No visible enemy can root the chain.")
					: CastTarget.entity(root);
		}
		if (THUNDERHEAD.equals(skill) || SKYBREAKER.equals(skill)) {
			LivingEntity rod = activeRodTarget(level, caster, state, 56.0D);
			Vec3 point = rod == null
					? groundAimPoint(level, caster, 24.0D + stage * 3.0D)
					: rod.position();
			if (point == null || !level.hasChunkAt(BlockPos.containing(point)))
				return CastTarget.fail("The storm cannot form in an unloaded or obstructed area.");
			return rod == null ? CastTarget.point(point) : CastTarget.both(rod, point);
		}
		return CastTarget.none();
	}

	private static boolean isOverchargeEligible(String skill) {
		return STATIC_NEEDLE.equals(skill) || THUNDERCLAP.equals(skill)
				|| CHAIN_LIGHTNING.equals(skill) || THUNDERHEAD.equals(skill)
				|| SKYBREAKER.equals(skill);
	}

	private static boolean consumeOvercharge(StormState state, long now) {
		if (state.voltage + 1.0E-4D < MAX_VOLTAGE)
			return false;
		if (state.isTempestActive(now) && state.tempestOvercharges >= 2)
			return false;
		state.voltage = 0.0D;
		if (state.isTempestActive(now))
			state.tempestOvercharges++;
		state.forceVoltageSync();
		return true;
	}

	private static boolean startNeedle(ServerLevel level, Entity caster, int stage,
			float damage, boolean overcharged) {
		Vec3 direction = safeDirection(caster.getLookAngle());
		double range = 24.0D + stage * 5.0D;
		ACTIVE_NEEDLES.add(new NeedleCast(level, caster, stage, damage, overcharged,
				caster.getEyePosition().add(direction.scale(0.55D)), direction, range));
		play(level, caster.position(), SoundEvents.TRIDENT_THROW, 0.65F, 1.85F);
		return true;
	}

	private static boolean startSlipstream(ServerLevel level, Entity caster, int stage) {
		Vec3 direction = safeHorizontalDirection(caster);
		double distance = 6.0D + stage * 0.7D;
		if (!hasSafeDashStep(level, caster, direction, Math.min(1.5D, distance / 4.0D))) {
			message(caster, "Slipstream is obstructed.");
			return false;
		}
		if (stage >= 2 && caster instanceof LivingEntity living)
			living.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		ACTIVE_DASHES.add(new SlipstreamCast(level, caster, stage, direction, distance));
		play(level, caster.position(), SoundEvents.TRIDENT_RIPTIDE_1, 0.75F, 1.65F);
		return true;
	}

	private static boolean castThunderclap(ServerLevel level, Entity caster, int stage,
			float damage, StormState state, boolean overcharged) {
		double radius = overcharged ? 6.5D + stage * 0.25D : 4.5D + stage * 0.45D;
		Vec3 look = safeHorizontalDirection(caster);
		List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
				caster.getBoundingBox().inflate(radius),
				candidate -> MageCombatHelper.isValidTarget(caster, candidate));
		targets.sort(Comparator.comparingDouble(caster::distanceToSqr));
		ArrayList<LivingEntity> hit = new ArrayList<>();
		for (LivingEntity target : targets) {
			if (hit.size() >= 12)
				break;
			Vec3 away = target.position().subtract(caster.position());
			Vec3 horizontal = new Vec3(away.x, 0.0D, away.z);
			if (!overcharged && horizontal.lengthSqr() > 1.0E-5D
					&& look.dot(horizontal.normalize()) < Math.cos(Math.toRadians(stage >= 6 ? 60.0D : 50.0D)))
				continue;
			if (!visible(level, caster.getEyePosition(), target))
				continue;
			if (!MageCombatHelper.hurt(level, caster, target, damage))
				continue;
			hit.add(target);
			applyThunderclapControl(caster, target, horizontal, state, level.getGameTime());
			sparkBurst(level, target.getBoundingBox().getCenter(), 5);
		}
		if (!hit.isEmpty()) {
			awardHits(level, caster, state, new HitLedger(), hit);
			recordDamagingCast(level, caster, state, hit.get(0), damage);
		}
		ringParticles(level, caster.position().add(0.0D, 0.7D, 0.0D), radius,
				overcharged ? 28 : 18);
		play(level, caster.position(), SoundEvents.GENERIC_EXPLODE, 0.8F,
				overcharged ? 1.25F : 1.65F);
		return true;
	}

	private static void applyThunderclapControl(Entity caster, LivingEntity target, Vec3 away,
			StormState state, long now) {
		boolean player = target instanceof Player;
		boolean boss = isBoss(target);
		if (!player && away.lengthSqr() > 1.0E-5D) {
			double strength = boss ? 0.15D : 0.52D;
			target.setDeltaMovement(target.getDeltaMovement().add(
					away.normalize().scale(strength)).add(0.0D, boss ? 0.02D : 0.10D, 0.0D));
			target.hurtMarked = true;
		}
		if (state.isConductive(target.getUUID(), now)) {
			int duration = player ? 8 : boss ? 7 : 24;
			int amplifier = player || boss ? 0 : 2;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
					duration, amplifier, false, false));
		}
	}

	private static boolean setLightningRod(ServerLevel level, Entity caster, int stage,
			StormState state, LivingEntity target) {
		if (target == null || !MageCombatHelper.isValidTarget(caster, target))
			return false;
		state.clearRod(caster);
		state.rodTarget = target.getUUID();
		state.rodExpiresAt = level.getGameTime() + (stage >= 6 ? 320L : ROD_DURATION);
		state.lastCombatAt = level.getGameTime();
		if (caster instanceof ServerPlayer player) {
			EntityHighlightSystem.show(player, target, ROD_HIGHLIGHT_SOURCE,
					ROD_HIGHLIGHT_COLOR, (int) (state.rodExpiresAt - level.getGameTime()),
					ROD_HIGHLIGHT_PRIORITY);
		}
		particleLine(level, caster.getEyePosition(), target.getBoundingBox().getCenter(),
				ParticleTypes.ELECTRIC_SPARK, 10);
		sparkBurst(level, target.getBoundingBox().getCenter(), 10);
		play(level, target.position(), SoundEvents.TRIDENT_RETURN, 0.8F, 1.7F);
		return true;
	}

	private static boolean castChain(ServerLevel level, Entity caster, int stage, float baseDamage,
			StormState state, LivingEntity root, boolean overcharged) {
		if (root == null || !MageCombatHelper.isValidTarget(caster, root)
				|| !visible(level, caster.getEyePosition(), root))
			return false;
		int maximum = stage >= 6 ? 6 : stage >= 3 ? 5 : stage == 2 ? 4 : 3;
		double falloff = stage >= 6 ? 0.88D : 0.82D;
		double budget = baseDamage * (stage >= 6 ? 2.95D : 2.65D);
		ArrayList<LivingEntity> order = buildChain(level, caster, state, root, maximum, overcharged);
		if (order.isEmpty())
			return false;
		double remaining = budget;
		ArrayList<LivingEntity> hit = new ArrayList<>();
		Vec3 previous = caster.getEyePosition();
		for (int index = 0; index < order.size() && remaining > 0.01D; index++) {
			LivingEntity target = order.get(index);
			float intended = (float) (baseDamage * Math.pow(falloff, index));
			float dealt = (float) Math.min(remaining, intended);
			dealt = pvpAdjusted(target, dealt);
			if (MageCombatHelper.hurt(level, caster, target, dealt)) {
				hit.add(target);
				remaining -= intended;
				particleLine(level, previous, target.getBoundingBox().getCenter(),
						index % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.END_ROD, 10);
				previous = target.getBoundingBox().getCenter();
			}
		}
		if (!hit.isEmpty()) {
			awardHits(level, caster, state, new HitLedger(), hit);
			recordDamagingCast(level, caster, state, hit.get(0), baseDamage);
		}
		play(level, root.position(), SoundEvents.TRIDENT_THUNDER, 0.75F,
				overcharged ? 1.35F : 1.75F);
		return !hit.isEmpty();
	}

	private static ArrayList<LivingEntity> buildChain(ServerLevel level, Entity caster,
			StormState state, LivingEntity root, int maximum, boolean branched) {
		ArrayList<LivingEntity> result = new ArrayList<>();
		result.add(root);
		Set<UUID> used = new HashSet<>();
		used.add(root.getUUID());
		LivingEntity firstCursor = root;
		LivingEntity secondCursor = root;
		for (int jump = 1; jump < maximum; jump++) {
			LivingEntity cursor = branched && jump % 2 == 0 ? secondCursor : firstCursor;
			LivingEntity next = bestChainTarget(level, caster, state, cursor, used);
			if (next == null && cursor != root)
				next = bestChainTarget(level, caster, state, root, used);
			if (next == null)
				break;
			result.add(next);
			used.add(next.getUUID());
			if (branched && jump % 2 == 0)
				secondCursor = next;
			else
				firstCursor = next;
		}
		return result;
	}

	private static LivingEntity bestChainTarget(ServerLevel level, Entity caster,
			StormState state, LivingEntity cursor, Set<UUID> used) {
		long now = level.getGameTime();
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
				cursor.getBoundingBox().inflate(9.0D),
				candidate -> !used.contains(candidate.getUUID())
						&& MageCombatHelper.isValidTarget(caster, candidate)
						&& visible(level, cursor.getEyePosition(), candidate));
		candidates.sort(Comparator
				.comparingInt((LivingEntity candidate) ->
						state.isConductive(candidate.getUUID(), now) ? 0 : 1)
				.thenComparingDouble(cursor::distanceToSqr));
		return candidates.isEmpty() ? null : candidates.get(0);
	}

	private static boolean startThunderhead(ServerLevel level, Entity caster, int stage,
			float strikeDamage, StormState state, Vec3 center, boolean overcharged) {
		if (center == null)
			return false;
		state.thunderhead = new ThunderheadState(level, caster, stage, center,
				strikeDamage, overcharged);
		cloudParticles(level, center.add(0.0D, 6.0D, 0.0D), 22);
		play(level, center, SoundEvents.BEACON_ACTIVATE, 0.65F, 0.8F);
		return true;
	}

	private static boolean startSkybreaker(ServerLevel level, Entity caster, int stage,
			float damage, StormState state, CastTarget target, boolean overcharged) {
		Vec3 center = target.point;
		if (center == null)
			return false;
		ACTIVE_SKYBREAKERS.add(new SkybreakerCast(level, caster, stage, damage,
				center, target.entity == null ? null : target.entity.getUUID(), overcharged));
		columnParticles(level, center, 8.0D, ParticleTypes.END_ROD, 14);
		play(level, center, SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.25F);
		return true;
	}

	private static boolean startTempest(ServerLevel level, Entity caster, int stage,
			StormState state) {
		long now = level.getGameTime();
		state.tempestExpiresAt = now + (stage >= 6 ? 240L : TEMPEST_DURATION);
		state.tempestDamagingCasts = 0;
		state.tempestEchoes = 0;
		state.tempestOvercharges = 0;
		state.slipstreamBonusAvailable = true;
		if (caster instanceof LivingEntity living)
			applyTempestSpeed(living, stage >= 6 ? 0.20D : 0.15D);
		ringParticles(level, caster.position().add(0.0D, 0.8D, 0.0D), 2.2D, 28);
		play(level, caster.position(), SoundEvents.TRIDENT_RIPTIDE_3, 1.0F, 1.35F);
		return true;
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		tickList(ACTIVE_NEEDLES);
		tickList(ACTIVE_DASHES);
		tickList(ACTIVE_SKYBREAKERS);
		tickList(ACTIVE_ECHOES);
		tickStates();
		if ((event.getServer().getTickCount() & 3) != 0)
			return;
		for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
			if (hasStormAccess(player)) {
				StormState state = stateFor(player.serverLevel(), player);
				state.syncHud(player, false);
			} else if (STATES.containsKey(player.getUUID())) {
				removeState(player, true);
			}
		}
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (hasStormAccess(player)) {
			StormState state = stateFor(player.serverLevel(), player);
			state.syncHud(player, true);
		} else {
			sendClearHud(player);
		}
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		removeState(event.getEntity(), true);
	}

	@SubscribeEvent
	public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
		removeState(event.getEntity(), true);
	}

	@SubscribeEvent
	public static void onDeath(LivingDeathEvent event) {
		removeState(event.getEntity(), true);
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		STATES.clear();
		ACTIVE_NEEDLES.clear();
		ACTIVE_DASHES.clear();
		ACTIVE_SKYBREAKERS.clear();
		ACTIVE_ECHOES.clear();
	}

	private static <T extends ActiveCast> void tickList(List<T> casts) {
		Iterator<T> iterator = casts.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().tick())
				iterator.remove();
		}
	}

	private static void tickStates() {
		Iterator<Map.Entry<UUID, StormState>> iterator = STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			StormState state = iterator.next().getValue();
			Entity caster = state.level.getEntity(state.casterId);
			if (caster == null || !caster.isAlive() || caster.level() != state.level
					|| caster instanceof Player && !hasStormAccess(caster)) {
				if (caster instanceof ServerPlayer player)
					sendClearHud(player);
				state.deactivate(caster);
				iterator.remove();
				continue;
			}
			state.tick(caster);
		}
	}

	private static StormState stateFor(ServerLevel level, Entity caster) {
		StormState current = STATES.get(caster.getUUID());
		if (current != null && current.level != level) {
			current.deactivate(caster);
			STATES.remove(caster.getUUID());
			current = null;
		}
		if (current == null) {
			current = new StormState(level, caster);
			STATES.put(caster.getUUID(), current);
		}
		return current;
	}

	private static void removeState(Entity entity, boolean clearClient) {
		if (entity == null)
			return;
		StormState state = STATES.remove(entity.getUUID());
		if (state != null)
			state.deactivate(entity);
		else if (entity instanceof LivingEntity living)
			removeTempestSpeed(living);
		if (entity instanceof ServerPlayer player) {
			EntityHighlightSystem.clearSource(player, ROD_HIGHLIGHT_SOURCE);
			if (clearClient)
				sendClearHud(player);
		}
		entity.getPersistentData().remove(VOLTAGE_TAG);
	}

	private static void sendClearHud(ServerPlayer player) {
		if (player == null || player.hasDisconnected())
			return;
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new StormStateMessage(false, 0, 1, false, false, false, false));
	}

	private static void awardHits(ServerLevel level, Entity caster, StormState state,
			HitLedger ledger, List<? extends LivingEntity> targets) {
		for (LivingEntity target : targets)
			awardHit(level, caster, state, ledger, target);
	}

	private static void awardHit(ServerLevel level, Entity caster, StormState state,
			HitLedger ledger, LivingEntity target) {
		if (!ledger.targets.add(target.getUUID()))
			return;
		double raw = ledger.targets.size() == 1 ? 8.0D : 2.0D;
		if (!ledger.rodAwarded && target.getUUID().equals(state.rodTarget)
				&& state.rodExpiresAt >= level.getGameTime()) {
			raw += 2.0D;
			ledger.rodAwarded = true;
		}
		double accepted = Math.min(raw, 14.0D - ledger.rawVoltage);
		if (accepted <= 0.0D)
			return;
		ledger.rawVoltage += accepted;
		state.addVoltage(accepted * (state.isTempestActive(level.getGameTime()) ? 1.5D : 1.0D));
		state.lastCombatAt = level.getGameTime();
	}

	private static void recordDamagingCast(ServerLevel level, Entity caster, StormState state,
			LivingEntity target, float representativeDamage) {
		long now = level.getGameTime();
		state.lastCombatAt = now;
		if (!state.isTempestActive(now))
			return;
		state.tempestDamagingCasts++;
		if (state.tempestDamagingCasts % 3 != 0 || state.tempestEchoes >= 5)
			return;
		state.tempestEchoes++;
		ACTIVE_ECHOES.add(new EchoCast(level, caster, target,
				Math.max(0.5F, representativeDamage * 0.35F), 6));
	}

	private static LivingEntity activeRodTarget(ServerLevel level, Entity caster,
			StormState state, double maximumRange) {
		if (state.rodTarget == null || state.rodExpiresAt < level.getGameTime())
			return null;
		Entity entity = level.getEntity(state.rodTarget);
		if (!(entity instanceof LivingEntity target)
				|| !MageCombatHelper.isValidTarget(caster, target)
				|| caster.distanceToSqr(target) > maximumRange * maximumRange
				|| !visible(level, caster.getEyePosition(), target)) {
			state.clearRod(caster);
			return null;
		}
		return target;
	}

	private static LivingEntity findLookTarget(ServerLevel level, Entity caster, double range) {
		Vec3 start = caster.getEyePosition();
		Vec3 end = start.add(safeDirection(caster.getLookAngle()).scale(range));
		BlockHitResult blockHit = level.clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		if (blockHit.getType() != HitResult.Type.MISS)
			end = blockHit.getLocation();
		AABB search = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.25D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, start, end, search,
				target -> target instanceof LivingEntity living
						&& MageCombatHelper.isValidTarget(caster, living),
				start.distanceToSqr(end));
		if (hit != null && hit.getEntity() instanceof LivingEntity living)
			return living;
		if (caster instanceof Mob mob && mob.getTarget() != null
				&& caster.distanceToSqr(mob.getTarget()) <= range * range
				&& MageCombatHelper.isValidTarget(caster, mob.getTarget())
				&& visible(level, start, mob.getTarget()))
			return mob.getTarget();
		return null;
	}

	private static Vec3 groundAimPoint(ServerLevel level, Entity caster, double range) {
		Vec3 start = caster.getEyePosition();
		Vec3 end = start.add(safeDirection(caster.getLookAngle()).scale(range));
		BlockHitResult sight = level.clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		Vec3 point = sight.getType() == HitResult.Type.MISS ? end : sight.getLocation();
		int startY = Mth.floor(Math.max(point.y + 2.0D, caster.getY() + 2.0D));
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(
				Mth.floor(point.x), startY, Mth.floor(point.z));
		for (int offset = 0; offset < 32; offset++) {
			cursor.setY(startY - offset);
			if (!level.hasChunkAt(cursor))
				return null;
			if (!level.getBlockState(cursor).isAir()
					&& !level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty())
				return new Vec3(point.x, cursor.getY() + 1.01D, point.z);
		}
		Vec3 fallback = new Vec3(point.x, caster.getY(), point.z);
		return level.hasChunkAt(BlockPos.containing(fallback)) ? fallback : null;
	}

	private static boolean visible(ServerLevel level, Vec3 start, LivingEntity target) {
		Vec3 end = target.getBoundingBox().getCenter();
		BlockHitResult hit = level.clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, target));
		return hit.getType() == HitResult.Type.MISS
				|| start.distanceToSqr(hit.getLocation()) + 0.20D >= start.distanceToSqr(end);
	}

	private static boolean hasSafeDashStep(ServerLevel level, Entity caster, Vec3 direction,
			double distance) {
		Vec3 destination = caster.position().add(direction.scale(distance));
		BlockPos targetPos = BlockPos.containing(destination);
		if (!level.hasChunkAt(targetPos) || !level.getWorldBorder().isWithinBounds(targetPos))
			return false;
		BlockHitResult hit = level.clip(new ClipContext(
				caster.position().add(0.0D, caster.getBbHeight() * 0.45D, 0.0D),
				destination.add(0.0D, caster.getBbHeight() * 0.45D, 0.0D),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		return hit.getType() == HitResult.Type.MISS
				&& level.noCollision(caster,
						caster.getBoundingBox().move(destination.subtract(caster.position())));
	}

	private static boolean canMaintainActiveCast(Entity caster, ServerLevel level) {
		return caster != null && caster.isAlive() && caster.level() == level
				&& (!(caster instanceof Player) || hasStormAccess(caster));
	}

	private static float pvpAdjusted(LivingEntity target, float damage) {
		return target instanceof Player ? damage * 0.65F : damage;
	}

	/**
	 * System Level contributes a modest, predictable damage bonus without
	 * replacing Intelligence as the spell-evolution stat.
	 */
	private static double systemLevelDamageFactor(Entity caster) {
		double systemLevel;
		if (caster instanceof Player)
			systemLevel = variables(caster).Level;
		else
			systemLevel = caster.getPersistentData().getDouble("Level");
		return 1.0D + Math.min(0.25D, Math.max(0.0D, systemLevel - 1.0D) * 0.002D);
	}

	private static boolean isBoss(LivingEntity target) {
		return !(target instanceof Player) && target.getMaxHealth() >= 250.0F;
	}

	private static Vec3 safeDirection(Vec3 value) {
		return value.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : value.normalize();
	}

	private static Vec3 safeHorizontalDirection(Entity caster) {
		Vec3 movement = caster.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
		if (movement.lengthSqr() > 0.015D)
			return movement.normalize();
		Vec3 look = caster.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
		return safeDirection(look);
	}

	private static void applyTempestSpeed(LivingEntity living, double amount) {
		var speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null)
			return;
		speed.removeModifier(TEMPEST_SPEED_MODIFIER);
		speed.addTransientModifier(new AttributeModifier(TEMPEST_SPEED_MODIFIER,
				"Storm Mage Tempest speed", amount, AttributeModifier.Operation.MULTIPLY_TOTAL));
	}

	private static void removeTempestSpeed(LivingEntity living) {
		var speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null)
			speed.removeModifier(TEMPEST_SPEED_MODIFIER);
	}

	private static void particleLine(ServerLevel level, Vec3 start, Vec3 end,
			ParticleOptions particle, int maximumPoints) {
		Vec3 delta = end.subtract(start);
		int points = Mth.clamp((int) Math.ceil(delta.length() * 1.5D), 2,
				Math.max(2, maximumPoints));
		for (int index = 0; index <= points; index++) {
			Vec3 point = start.add(delta.scale(index / (double) points));
			level.sendParticles(particle, point.x, point.y, point.z,
					1, 0.025D, 0.025D, 0.025D, 0.0D);
		}
	}

	private static void columnParticles(ServerLevel level, Vec3 base, double height,
			ParticleOptions particle, int points) {
		for (int index = 0; index < points; index++) {
			double y = base.y + height * index / Math.max(1.0D, points - 1.0D);
			level.sendParticles(particle, base.x, y, base.z,
					1, 0.08D, 0.02D, 0.08D, 0.0D);
		}
	}

	private static void ringParticles(ServerLevel level, Vec3 center, double radius, int points) {
		for (int index = 0; index < points; index++) {
			double angle = Math.PI * 2.0D * index / points;
			ParticleOptions particle = index % 3 == 0
					? ParticleTypes.END_ROD : ParticleTypes.ELECTRIC_SPARK;
			level.sendParticles(particle, center.x + Math.cos(angle) * radius,
					center.y, center.z + Math.sin(angle) * radius,
					1, 0.0D, 0.02D, 0.0D, 0.0D);
		}
	}

	private static void cloudParticles(ServerLevel level, Vec3 center, int count) {
		level.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
				Math.min(24, count), 2.2D, 0.35D, 2.2D, 0.015D);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y - 0.2D, center.z,
				Math.min(12, Math.max(3, count / 2)), 2.0D, 0.25D, 2.0D, 0.02D);
	}

	private static void sparkBurst(ServerLevel level, Vec3 center, int count) {
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, center.x, center.y, center.z,
				Math.min(16, count), 0.35D, 0.45D, 0.35D, 0.06D);
		level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
				Math.min(5, Math.max(1, count / 3)), 0.18D, 0.25D, 0.18D, 0.025D);
	}

	private static void play(ServerLevel level, Vec3 position,
			net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
		level.playSound(null, position.x, position.y, position.z, sound,
				SoundSource.PLAYERS, volume, pitch);
	}

	private static void deductMana(Entity caster, int amount) {
		caster.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(data -> {
					data.MP = Math.max(0.0D, data.MP - amount);
					data.syncPlayerVariables(caster);
				});
	}

	private static void message(Entity caster, String text) {
		if (caster instanceof Player player && !player.level().isClientSide())
			player.displayClientMessage(Component.literal(text), true);
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity entity) {
		return entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private interface ActiveCast {
		boolean tick();
	}

	private static final class StormState {
		private final ServerLevel level;
		private final UUID casterId;
		private final Map<UUID, Long> conductiveUntil = new HashMap<>();
		private Vec3 lastPosition;
		private double voltage;
		private double lastSyncedVoltage = -1.0D;
		private long lastVoltageSyncAt;
		private int lastHudVoltage = -1;
		private int lastHudStage = -1;
		private boolean lastHudOvercharged;
		private boolean lastHudRod;
		private boolean lastHudTempest;
		private boolean lastHudSpiritualized;
		private long lastHudAt = Long.MIN_VALUE / 2L;
		private long lastCombatAt = Long.MIN_VALUE / 2L;
		private UUID rodTarget;
		private long rodExpiresAt;
		private ThunderheadState thunderhead;
		private long tempestExpiresAt;
		private int tempestDamagingCasts;
		private int tempestEchoes;
		private int tempestOvercharges;
		private boolean slipstreamBonusAvailable;

		private StormState(ServerLevel level, Entity caster) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.lastPosition = caster.position();
			this.voltage = Mth.clamp(caster.getPersistentData().getDouble(VOLTAGE_TAG),
					0.0D, MAX_VOLTAGE);
		}

		private void tick(Entity caster) {
			long now = level.getGameTime();
			conductiveUntil.entrySet().removeIf(entry -> entry.getValue() < now
					|| level.getEntity(entry.getKey()) == null);
			Entity rod = rodTarget == null ? null : level.getEntity(rodTarget);
			if (rodExpiresAt < now || rodTarget != null
					&& (!(rod instanceof LivingEntity living) || !living.isAlive()))
				clearRod(caster);

			double moved = caster.position().subtract(lastPosition)
					.multiply(1.0D, 0.0D, 1.0D).length();
			lastPosition = caster.position();
			if (now - lastCombatAt <= 80L && moved > 0.015D)
				addVoltage(0.4D * (isTempestActive(now) ? 1.5D : 1.0D));
			else if (now - lastCombatAt > 80L && voltage > 0.0D)
				addVoltage(-0.30D);

			if (thunderhead != null && thunderhead.tick(caster))
				thunderhead = null;

			if (tempestExpiresAt > 0L) {
				if (now >= tempestExpiresAt) {
					tempestExpiresAt = 0L;
					slipstreamBonusAvailable = false;
					if (caster instanceof LivingEntity living)
						removeTempestSpeed(living);
				} else if (now % 5L == 0L) {
					ringParticles(level, caster.position().add(0.0D, 0.9D, 0.0D),
							0.75D, 8);
				}
			}
			if (!CooldownManager.isOnCooldown(caster, SLIPSTREAM) && !isTempestActive(now))
				slipstreamBonusAvailable = false;
			syncVoltage(caster, false);
		}

		private void addVoltage(double amount) {
			voltage = Mth.clamp(voltage + amount, 0.0D, MAX_VOLTAGE);
		}

		private void markConductive(UUID targetId, long now) {
			conductiveUntil.put(targetId, now + CONDUCTIVE_DURATION);
		}

		private boolean isConductive(UUID targetId, long now) {
			Long expires = conductiveUntil.get(targetId);
			if (expires == null)
				return false;
			if (expires < now) {
				conductiveUntil.remove(targetId);
				return false;
			}
			return true;
		}

		private boolean isTempestActive(long now) {
			return tempestExpiresAt > now;
		}

		private boolean canSpendSlipstreamBonus(long now) {
			return slipstreamBonusAvailable && (isTempestActive(now)
					|| effectiveOutputStage(level.getEntity(casterId)) >= 4);
		}

		private void clearRod(Entity caster) {
			if (rodTarget != null && caster instanceof ServerPlayer player)
				EntityHighlightSystem.hide(player, rodTarget, level.dimension(),
						ROD_HIGHLIGHT_SOURCE);
			rodTarget = null;
			rodExpiresAt = 0L;
		}

		private void forceVoltageSync() {
			lastSyncedVoltage = -1.0D;
			lastVoltageSyncAt = 0L;
		}

		private void syncVoltage(Entity caster, boolean force) {
			long now = level.getGameTime();
			boolean thresholdChanged = (voltage >= MAX_VOLTAGE) != (lastSyncedVoltage >= MAX_VOLTAGE);
			if (!force && !thresholdChanged && Math.abs(voltage - lastSyncedVoltage) < 2.0D
					&& now - lastVoltageSyncAt < 4L)
				return;
			caster.getPersistentData().putDouble(VOLTAGE_TAG, voltage);
			lastSyncedVoltage = voltage;
			lastVoltageSyncAt = now;
		}

		private void syncHud(ServerPlayer player, boolean force) {
			long now = level.getGameTime();
			int hudVoltage = Mth.clamp((int) Math.round(voltage), 0, 100);
			int hudStage = effectiveOutputStage(player);
			boolean hudOvercharged = voltage + 1.0E-4D >= MAX_VOLTAGE;
			boolean hudRod = rodTarget != null && rodExpiresAt >= now;
			boolean hudTempest = isTempestActive(now);
			boolean hudSpiritualized = WhiteFlameMonarchManager.isSpiritualized(player)
					&& hudStage > outputStage(player);
			boolean changed = Math.abs(hudVoltage - lastHudVoltage) >= 2
					|| hudOvercharged != lastHudOvercharged
					|| hudStage != lastHudStage
					|| hudRod != lastHudRod
					|| hudTempest != lastHudTempest
					|| hudSpiritualized != lastHudSpiritualized;
			if (!force && (!changed || now - lastHudAt < 4L))
				return;
			SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
					new StormStateMessage(true, hudVoltage, hudStage, hudOvercharged,
							hudRod, hudTempest, hudSpiritualized));
			lastHudVoltage = hudVoltage;
			lastHudStage = hudStage;
			lastHudOvercharged = hudOvercharged;
			lastHudRod = hudRod;
			lastHudTempest = hudTempest;
			lastHudSpiritualized = hudSpiritualized;
			lastHudAt = now;
		}

		private void deactivate(Entity caster) {
			if (caster instanceof LivingEntity living)
				removeTempestSpeed(living);
			if (caster != null)
				caster.getPersistentData().remove(VOLTAGE_TAG);
			conductiveUntil.clear();
			clearRod(caster);
			thunderhead = null;
			tempestExpiresAt = 0L;
		}
	}

	private static final class NeedleCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final float damage;
		private final boolean overcharged;
		private final Vec3 direction;
		private final double maximumRange;
		private final Set<UUID> hit = new HashSet<>();
		private final HitLedger voltageLedger = new HitLedger();
		private Vec3 position;
		private double travelled;
		private boolean recordedCast;
		private boolean forked;

		private NeedleCast(ServerLevel level, Entity caster, int stage, float damage,
				boolean overcharged, Vec3 position, Vec3 direction, double maximumRange) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.stage = stage;
			this.damage = damage;
			this.overcharged = overcharged;
			this.position = position;
			this.direction = direction;
			this.maximumRange = maximumRange;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (!canMaintainActiveCast(caster, level))
				return true;
			double speed = 2.7D + stage * 0.12D;
			Vec3 next = position.add(direction.scale(speed));
			if (!level.hasChunkAt(BlockPos.containing(next)))
				return true;
			BlockHitResult blockHit = level.clip(new ClipContext(position, next,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			Vec3 clipped = blockHit.getType() == HitResult.Type.MISS ? next : blockHit.getLocation();
			EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, position, clipped,
					new AABB(position, clipped).inflate(0.45D),
					target -> target instanceof LivingEntity living
							&& !hit.contains(target.getUUID())
							&& MageCombatHelper.isValidTarget(caster, living),
					position.distanceToSqr(clipped));
			if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
				hitTarget(caster, target);
				position = entityHit.getLocation().add(direction.scale(0.25D));
				int allowedHits = stage >= 6 ? 3 : stage >= 3 ? 2 : 1;
				if (hit.size() >= allowedHits)
					return true;
			} else {
				position = clipped;
			}
			level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
					position.x, position.y, position.z, 2,
					0.035D, 0.035D, 0.035D, 0.0D);
			travelled += speed;
			return blockHit.getType() != HitResult.Type.MISS || travelled >= maximumRange;
		}

		private void hitTarget(Entity caster, LivingEntity target) {
			if (!MageCombatHelper.hurt(level, caster, target, damage))
				return;
			hit.add(target.getUUID());
			StormState state = stateFor(level, caster);
			state.markConductive(target.getUUID(), level.getGameTime());
			awardHit(level, caster, state, voltageLedger, target);
			if (!recordedCast) {
				recordedCast = true;
				recordDamagingCast(level, caster, state, target, damage);
			}
			sparkBurst(level, target.getBoundingBox().getCenter(), 8);
			if (stage >= 4)
				ACTIVE_ECHOES.add(new EchoCast(level, caster, target,
						damage * (stage >= 6 ? 0.55F : 0.42F), 12));
			if (overcharged && !forked) {
				forked = true;
				LivingEntity fork = nearestConductive(level, caster, state, target.position(),
						8.0D, hit);
				if (fork != null && MageCombatHelper.hurt(level, caster, fork, damage * 0.55F)) {
					hit.add(fork.getUUID());
					awardHit(level, caster, state, voltageLedger, fork);
					particleLine(level, target.getBoundingBox().getCenter(),
							fork.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 9);
				}
			}
		}
	}

	private static final class SlipstreamCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final Vec3 direction;
		private final double step;
		private final Set<UUID> harvested = new HashSet<>();
		private int age;

		private SlipstreamCast(ServerLevel level, Entity caster, int stage,
				Vec3 direction, double distance) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.stage = stage;
			this.direction = direction;
			this.step = distance / 4.0D;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (!(caster instanceof LivingEntity living)
					|| !canMaintainActiveCast(caster, level))
				return true;
			age++;
			if (!hasSafeDashStep(level, caster, direction, step))
				return true;
			caster.setDeltaMovement(direction.scale(step));
			living.hurtMarked = true;
			living.fallDistance = 0.0F;
			Vec3 next = caster.position().add(direction.scale(step));
			particleLine(level, caster.position().add(0.0D, 0.7D, 0.0D),
					next.add(0.0D, 0.7D, 0.0D), ParticleTypes.ELECTRIC_SPARK, 5);
			harvestNearbyMark(caster);
			return age >= 4;
		}

		private void harvestNearbyMark(Entity caster) {
			StormState state = stateFor(level, caster);
			int maximum = stage >= 6 ? 2 : 1;
			if (harvested.size() >= maximum)
				return;
			List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
					caster.getBoundingBox().inflate(2.6D),
					target -> MageCombatHelper.isValidTarget(caster, target)
							&& !harvested.contains(target.getUUID())
							&& state.isConductive(target.getUUID(), level.getGameTime()));
			nearby.sort(Comparator.comparingDouble(caster::distanceToSqr));
			if (!nearby.isEmpty()) {
				LivingEntity target = nearby.get(0);
				harvested.add(target.getUUID());
				state.addVoltage(4.0D * (state.isTempestActive(level.getGameTime()) ? 1.5D : 1.0D));
				state.lastCombatAt = level.getGameTime();
				particleLine(level, target.getBoundingBox().getCenter(),
						caster.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 6);
			}
		}
	}

	private static final class ThunderheadState {
		private static final int[] FIVE_STRIKES = {10, 42, 74, 106, 138};
		private static final int[] SIX_STRIKES = {8, 34, 60, 86, 112, 138};

		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final Vec3 center;
		private final float damage;
		private final boolean overcharged;
		private final HitLedger voltageLedger = new HitLedger();
		private UUID previousTarget;
		private boolean recordedCast;
		private int age;
		private int strikeIndex;

		private ThunderheadState(ServerLevel level, Entity caster, int stage,
				Vec3 center, float damage, boolean overcharged) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.stage = stage;
			this.center = center;
			this.damage = damage;
			this.overcharged = overcharged;
		}

		private boolean tick(Entity caster) {
			age++;
			if (age % 20 == 0)
				cloudParticles(level, center.add(0.0D, 6.0D, 0.0D), 8);
			int[] schedule = stage >= 4 ? SIX_STRIKES : FIVE_STRIKES;
			if (strikeIndex < schedule.length && age >= schedule[strikeIndex]) {
				boolean finalStrike = strikeIndex == schedule.length - 1;
				strike(caster, finalStrike);
				strikeIndex++;
			}
			return age > schedule[schedule.length - 1] + 4;
		}

		private void strike(Entity caster, boolean finalStrike) {
			StormState state = stateFor(level, caster);
			List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class,
					new AABB(center, center).inflate(5.5D, 7.0D, 5.5D),
					target -> MageCombatHelper.isValidTarget(caster, target)
							&& visible(level, center.add(0.0D, 6.0D, 0.0D), target));
			candidates.sort(Comparator
					.comparingInt((LivingEntity target) ->
							target.getUUID().equals(previousTarget) ? 1 : 0)
					.thenComparingInt(target ->
							state.isConductive(target.getUUID(), level.getGameTime()) ? 0 : 1)
					.thenComparingDouble(target -> target.distanceToSqr(center)));
			int targetCount = finalStrike && stage >= 6 ? Math.min(2, candidates.size())
					: Math.min(1, candidates.size());
			ArrayList<LivingEntity> hit = new ArrayList<>();
			for (int index = 0; index < targetCount; index++) {
				LivingEntity target = candidates.get(index);
				float strikeDamage = index == 0 ? damage : damage * 0.60F;
				if (MageCombatHelper.hurt(level, caster, target, strikeDamage)) {
					hit.add(target);
					previousTarget = target.getUUID();
					columnParticles(level, target.position(), 7.5D,
							ParticleTypes.ELECTRIC_SPARK, 12);
					sparkBurst(level, target.getBoundingBox().getCenter(), 10);
				}
			}
			if (!hit.isEmpty()) {
				awardHits(level, caster, state, voltageLedger, hit);
				if (!recordedCast) {
					recordedCast = true;
					recordDamagingCast(level, caster, state, hit.get(0), damage);
				}
			} else {
				columnParticles(level, center, 7.0D, ParticleTypes.END_ROD, 8);
			}
			if (finalStrike && overcharged)
				finalBurst(caster, state, candidates);
			play(level, center, SoundEvents.TRIDENT_THUNDER, 0.70F, 1.45F);
		}

		private void finalBurst(Entity caster, StormState state,
				List<LivingEntity> strikeCandidates) {
			List<LivingEntity> targets = strikeCandidates.stream()
					.filter(target -> target.distanceToSqr(center) <= 4.25D * 4.25D)
					.sorted(Comparator.comparingDouble(target -> target.distanceToSqr(center)))
					.toList();
			ArrayList<LivingEntity> hit = new ArrayList<>();
			for (LivingEntity target : targets) {
				if (hit.size() >= 8)
					break;
				if (MageCombatHelper.hurt(level, caster, target, damage * 0.55F))
					hit.add(target);
			}
			awardHits(level, caster, state, voltageLedger, hit);
			ringParticles(level, center.add(0.0D, 0.4D, 0.0D), 4.25D, 24);
		}
	}

	private static final class SkybreakerCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final float damage;
		private final Vec3 center;
		private final UUID preferredTarget;
		private final boolean overcharged;
		private final HitLedger voltageLedger = new HitLedger();
		private int age;

		private SkybreakerCast(ServerLevel level, Entity caster, int stage, float damage,
				Vec3 center, UUID preferredTarget, boolean overcharged) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.stage = stage;
			this.damage = damage;
			this.center = center;
			this.preferredTarget = preferredTarget;
			this.overcharged = overcharged;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (!canMaintainActiveCast(caster, level))
				return true;
			age++;
			if (age == 8 || age == 14)
				columnParticles(level, center, 8.0D, ParticleTypes.END_ROD, 10);
			if (age == 16)
				detonate(caster);
			if (overcharged && (age == 24 || age == 32))
				echo(caster);
			if (stage >= 6 && !overcharged && age == 26)
				echo(caster);
			return age >= (overcharged ? 33 : stage >= 6 ? 27 : 17);
		}

		private void detonate(Entity caster) {
			ArrayList<LivingEntity> hit = new ArrayList<>();
			LivingEntity primary = null;
			if (preferredTarget != null && level.getEntity(preferredTarget) instanceof LivingEntity living
					&& MageCombatHelper.isValidTarget(caster, living)
					&& living.distanceToSqr(center) <= 16.0D)
				primary = living;
			if (primary == null) {
				primary = level.getEntitiesOfClass(LivingEntity.class,
						new AABB(center, center).inflate(2.75D, 5.0D, 2.75D),
						target -> MageCombatHelper.isValidTarget(caster, target))
						.stream().min(Comparator.comparingDouble(target -> target.distanceToSqr(center)))
						.orElse(null);
			}
			if (primary != null && MageCombatHelper.hurt(level, caster, primary, damage))
				hit.add(primary);

			StormState state = stateFor(level, caster);
			LivingEntity resolvedPrimary = primary;
			List<LivingEntity> forks = level.getEntitiesOfClass(LivingEntity.class,
					new AABB(center, center).inflate(6.0D),
					target -> target != resolvedPrimary
							&& MageCombatHelper.isValidTarget(caster, target)
							&& state.isConductive(target.getUUID(), level.getGameTime()));
			forks.sort(Comparator.comparingDouble(target -> target.distanceToSqr(center)));
			float forkBudget = damage * 0.45F;
			for (LivingEntity target : forks) {
				if (hit.size() >= 4 || forkBudget <= 0.0F)
					break;
				float forkDamage = Math.min(damage * 0.18F, forkBudget);
				forkBudget -= forkDamage;
				if (MageCombatHelper.hurt(level, caster, target, forkDamage)) {
					hit.add(target);
					particleLine(level, center.add(0.0D, 1.0D, 0.0D),
							target.getBoundingBox().getCenter(),
							ParticleTypes.ELECTRIC_SPARK, 8);
				}
			}
			if (!hit.isEmpty()) {
				awardHits(level, caster, state, voltageLedger, hit);
				recordDamagingCast(level, caster, state, hit.get(0), damage);
			}
			columnParticles(level, center, 10.0D, ParticleTypes.ELECTRIC_SPARK, 18);
			sparkBurst(level, center.add(0.0D, 1.0D, 0.0D), 16);
			play(level, center, SoundEvents.TRIDENT_THUNDER, 1.25F, 0.85F);
		}

		private void echo(Entity caster) {
			float echoDamage = damage * (overcharged ? 0.18F : 0.16F);
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class,
					new AABB(center, center).inflate(2.75D, 4.0D, 2.75D),
					target -> MageCombatHelper.isValidTarget(caster, target));
			targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(center)));
			if (!targets.isEmpty())
				MageCombatHelper.hurt(level, caster, targets.get(0),
						pvpAdjusted(targets.get(0), echoDamage));
			columnParticles(level, center, 7.0D, ParticleTypes.END_ROD, 9);
			play(level, center, SoundEvents.AMETHYST_BLOCK_CHIME, 0.65F, 1.45F);
		}
	}

	private static final class EchoCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID targetId;
		private final Vec3 fallback;
		private final float damage;
		private final int delay;
		private int age;

		private EchoCast(ServerLevel level, Entity caster, LivingEntity target,
				float damage, int delay) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.targetId = target.getUUID();
			this.fallback = target.getBoundingBox().getCenter();
			this.damage = damage;
			this.delay = delay;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (!canMaintainActiveCast(caster, level))
				return true;
			age++;
			if (age < delay)
				return false;
			Entity rawTarget = level.getEntity(targetId);
			if (rawTarget instanceof LivingEntity target
					&& MageCombatHelper.isValidTarget(caster, target)) {
				MageCombatHelper.hurt(level, caster, target, pvpAdjusted(target, damage));
				particleLine(level, target.getBoundingBox().getCenter().add(0.0D, 3.0D, 0.0D),
						target.getBoundingBox().getCenter(), ParticleTypes.END_ROD, 6);
				sparkBurst(level, target.getBoundingBox().getCenter(), 5);
			} else {
				sparkBurst(level, fallback, 3);
			}
			return true;
		}
	}

	private static LivingEntity nearestConductive(ServerLevel level, Entity caster,
			StormState state, Vec3 center, double radius, Set<UUID> excluded) {
		return level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(radius),
				target -> MageCombatHelper.isValidTarget(caster, target)
						&& !excluded.contains(target.getUUID())
						&& state.isConductive(target.getUUID(), level.getGameTime())
						&& visible(level, center, target))
				.stream().min(Comparator.comparingDouble(target -> target.distanceToSqr(center)))
				.orElse(null);
	}

	private static final class HitLedger {
		private final Set<UUID> targets = new LinkedHashSet<>();
		private double rawVoltage;
		private boolean rodAwarded;
	}

	private static final class CastTarget {
		private final boolean valid;
		private final String failure;
		private final LivingEntity entity;
		private final Vec3 point;

		private CastTarget(boolean valid, String failure, LivingEntity entity, Vec3 point) {
			this.valid = valid;
			this.failure = failure;
			this.entity = entity;
			this.point = point;
		}

		private static CastTarget none() {
			return new CastTarget(true, "", null, null);
		}

		private static CastTarget entity(LivingEntity entity) {
			return new CastTarget(true, "", entity, entity.getBoundingBox().getCenter());
		}

		private static CastTarget point(Vec3 point) {
			return new CastTarget(true, "", null, point);
		}

		private static CastTarget both(LivingEntity entity, Vec3 point) {
			return new CastTarget(true, "", entity, point);
		}

		private static CastTarget fail(String failure) {
			return new CastTarget(false, failure, null, null);
		}
	}

}
