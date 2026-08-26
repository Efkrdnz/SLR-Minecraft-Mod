package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.SlashEffectEntity;
import net.solocraft.network.ClassPassiveMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.network.compat.PacketDistributor;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
 * Server-authoritative runtime for the Juggernaut Tanker style.
 *
 * <p>Deliberately separate from {@link TankerSkillManager}, which owns the six
 * Sentinel abilities and has four dedicated regression suites. Nothing in that
 * tested combat math is touched here.</p>
 *
 * <p>Gigantification uses a reduced-scale player aura rather than a real body
 * scale change, the same approach that unblocked the Ravager transformations.
 * A genuine scale framework remains a future refinement, not a prerequisite.</p>
 *
 * <p>Access is by ownership only. No check reads class or style.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class JuggernautSkillManager {
	public static final String HEAVY_BLOW = "Heavy Blow";
	public static final String IRON_BODY = "Iron Body";
	public static final String SEISMIC_GRAPPLE = "Seismic Grapple";
	public static final String GIGANTIFICATION = "Gigantification";
	public static final String COLOSSUS_CHARGE = "Colossus Charge";
	public static final String MOUNTAIN_BREAKER = "Mountain Breaker";

	public static final String GIGANT_AURA = "juggernaut_mass";

	private static final List<String> MANAGED_SKILLS = List.of(
			HEAVY_BLOW, IRON_BODY, SEISMIC_GRAPPLE, GIGANTIFICATION,
			COLOSSUS_CHARGE, MOUNTAIN_BREAKER);

	private static final ResourceKey<DamageType> TANKER_DAMAGE = ResourceKey.create(
			Registries.DAMAGE_TYPE, ResourceLocation.parse("sololeveling:tanker"));
	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(
			Registries.ENTITY_TYPE, ResourceLocation.parse("soloboss"));

	private static final double POISE_MAX = 100.0D;
	private static final double GIGANT_POISE_COST = 35.0D;
	private static final double BREAKER_POISE_DISCOUNT = 40.0D;
	private static final int GRAPPLE_TICKS = 40;
	private static final int CHARGE_TICKS = 24;
	private static final Map<UUID, MassState> STATES = new ConcurrentHashMap<>();

	private JuggernautSkillManager() {
	}

	// ── dispatch ──────────────────────────────────────────────────────────────

	public static boolean isJuggernautSkill(String skill) {
		return skill != null && MANAGED_SKILLS.contains(skill.trim());
	}

	public static boolean activateSkill(ServerPlayer player, String requestedSkill) {
		if (player == null || !player.isAlive() || requestedSkill == null)
			return false;
		MassState state = state(player);
		return switch (requestedSkill.trim()) {
			case HEAVY_BLOW -> castHeavyBlow(player, state);
			case IRON_BODY -> castIronBody(player, state);
			case SEISMIC_GRAPPLE -> castSeismicGrapple(player, state);
			case GIGANTIFICATION -> castGigantification(player, state);
			case COLOSSUS_CHARGE -> castColossusCharge(player, state);
			case MOUNTAIN_BREAKER -> castMountainBreaker(player, state);
			default -> false;
		};
	}

	// ── abilities ─────────────────────────────────────────────────────────────

	/** Slow armored swing: modest damage, heavy knockback and guard pressure. */
	private static boolean castHeavyBlow(ServerPlayer player, MassState state) {
		if (!ready(player, HEAVY_BLOW))
			return false;
		LivingEntity target = findLookTarget(player, 4.0D);
		if (target == null)
			return fail(player, "No target");
		if (!ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.LOW)))
			return notEnoughMana(player);
		setCooldown(player, HEAVY_BLOW, 140);

		float damage = (float) (7.0D + strength(player) * 0.35D + vitality(player) * 0.25D
				+ weaponDamage(player) * 0.8D);
		if (hurt(player, target, damage) && !isBoss(target)) {
			Vec3 push = player.getLookAngle().normalize().scale(1.8D).add(0.0D, 0.35D, 0.0D);
			target.setDeltaMovement(push);
			target.hurtMarked = true;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true));
		}
		addPoise(player, state, 8.0D);
		spawnSlash(player, target.getBoundingBox().getCenter(), 0.0F, 1.0F, 100);
		play(player, SoundEvents.ANVIL_LAND, 0.6F, 0.8F);
		return true;
	}

	/** Short brace. A hit taken during the window converts into extra Poise. */
	private static boolean castIronBody(ServerPlayer player, MassState state) {
		if (!ready(player, IRON_BODY)
				|| !ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.MEDIUM)))
			return notEnoughMana(player);
		setCooldown(player, IRON_BODY, 240);
		long now = player.level().getGameTime();
		int duration = (int) Mth.clamp(80.0D + vitality(player) * 0.4D, 80.0D, 160.0D);
		state.ironBodyUntil = now + duration;
		state.ironBodyPerfectUntil = now + 12L;
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false));
		play(player, SoundEvents.IRON_GOLEM_REPAIR, 0.7F, 1.1F);
		message(player, "Iron Body");
		return true;
	}

	/** Pins a normal target in place; a boss is only slowed and pulled slightly. */
	private static boolean castSeismicGrapple(ServerPlayer player, MassState state) {
		if (!ready(player, SEISMIC_GRAPPLE))
			return false;
		LivingEntity target = findLookTarget(player, 7.0D);
		if (target == null)
			return fail(player, "No target");
		if (!ManaRules.spend(player, ManaRules.cost(player,
				isBoss(target) ? ManaRules.Band.HIGH : ManaRules.Band.MEDIUM)))
			return notEnoughMana(player);
		setCooldown(player, SEISMIC_GRAPPLE, 300);

		float damage = (float) (5.0D + strength(player) * 0.25D + vitality(player) * 0.2D);
		hurt(player, target, damage);
		if (isBoss(target) || target instanceof Player) {
			// Never a long forced animation against a boss or a player.
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2, false, true));
		} else {
			state.grappledTarget = target.getUUID();
			state.grappleUntil = player.level().getGameTime() + GRAPPLE_TICKS;
			target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, GRAPPLE_TICKS, 5, false, true));
		}
		addPoise(player, state, 10.0D);
		groundCracks(player, target.position(), 1.6D);
		play(player, SoundEvents.NETHERITE_BLOCK_BREAK, 0.7F, 0.7F);
		return true;
	}

	/**
	 * Maintained heavy form. Spends Poise to enter and Poise reduces its upkeep,
	 * so it rewards having actually held ground rather than just having mana.
	 */
	private static boolean castGigantification(ServerPlayer player, MassState state) {
		long now = player.level().getGameTime();
		if (state.gigantUntil >= now) {
			endGigantification(player, state);
			return true;
		}
		if (VesselManager.currentDefinition(player) != null)
			return fail(player, "A vessel transformation is already active");
		if (!ready(player, GIGANTIFICATION))
			return false;
		// Creative bypasses the Poise gate, matching mana and cooldowns.
		boolean free = ManaRules.isFree(player);
		if (!free && state.poise < GIGANT_POISE_COST)
			return fail(player, "Poise " + (int) state.poise + "/" + (int) GIGANT_POISE_COST);
		if (!ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.HIGH)))
			return notEnoughMana(player);

		if (!free)
			state.poise -= GIGANT_POISE_COST;
		int duration = (int) Mth.clamp(300.0D + vitality(player) * 0.8D, 300.0D, 500.0D);
		state.gigantUntil = now + duration;
		state.gigantNextDrain = now + 20L;
		setCooldown(player, GIGANTIFICATION, 500);
		syncPoise(player, state);

		PlayerAuraSystem.setContinuous(player, GIGANT_AURA, 0.85F);
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 1, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, 0, false, false));
		player.addEffect(new MobEffectInstance(MobEffects.HEALTH_BOOST, duration, 2, false, false));
		play(player, SoundEvents.RAVAGER_ROAR, 0.9F, 0.6F);
		message(player, "Gigantification");
		return true;
	}

	private static void endGigantification(ServerPlayer player, MassState state) {
		if (state.gigantUntil <= 0L)
			return;
		state.gigantUntil = 0L;
		state.gigantNextDrain = 0L;
		PlayerAuraSystem.clearContinuous(player);
		player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
		player.removeEffect(MobEffects.DAMAGE_BOOST);
		player.removeEffect(MobEffects.HEALTH_BOOST);
	}

	/** Armored rush that gathers normal enemies and releases them at the end. */
	private static boolean castColossusCharge(ServerPlayer player, MassState state) {
		if (!ready(player, COLOSSUS_CHARGE)
				|| !ManaRules.spend(player, ManaRules.cost(player, ManaRules.Band.HIGH)))
			return notEnoughMana(player);
		setCooldown(player, COLOSSUS_CHARGE, 420);
		state.chargeUntil = player.level().getGameTime() + CHARGE_TICKS;
		state.chargeGathered.clear();
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, CHARGE_TICKS, 2, false, false));
		play(player, SoundEvents.RAVAGER_STEP, 0.9F, 0.7F);
		return true;
	}

	/** Two-stage ground impact: an outward lift, then an inward collapse. */
	private static boolean castMountainBreaker(ServerPlayer player, MassState state) {
		if (!ready(player, MOUNTAIN_BREAKER))
			return false;
		// Poise can pay part of the cost, never all of it. Creative pays neither.
		boolean free = ManaRules.isFree(player);
		boolean discounted = !free && state.poise >= BREAKER_POISE_DISCOUNT;
		int cost = ManaRules.cost(player, ManaRules.Band.APEX);
		if (discounted)
			cost = (int) (cost * 0.65D);
		if (!ManaRules.spend(player, cost))
			return notEnoughMana(player);
		if (discounted) {
			state.poise -= BREAKER_POISE_DISCOUNT;
			syncPoise(player, state);
		}
		setCooldown(player, MOUNTAIN_BREAKER, 1200);

		Vec3 center = player.position();
		double radius = 6.0D;
		float lift = (float) (10.0D + strength(player) * 0.4D);
		float collapse = (float) (22.0D + strength(player) * 0.9D + vitality(player) * 0.5D);

		// Stage one: fault lines and an outward lift.
		groundCracks(player, center, radius);
		for (LivingEntity target : nearbyTargets(player, center, radius, 8)) {
			if (hurt(player, target, lift) && !isBoss(target)) {
				target.setDeltaMovement(target.getDeltaMovement().add(0.0D, 0.55D, 0.0D));
				target.hurtMarked = true;
			}
		}
		play(player, SoundEvents.ANVIL_LAND, 1.0F, 0.6F);

		// Stage two: inward collapse a beat later.
		SololevelingMod.queueServerWork(16, () -> {
			if (!player.isAlive())
				return;
			for (LivingEntity target : nearbyTargets(player, center, radius, 8)) {
				target.invulnerableTime = 0;
				if (hurt(player, target, collapse) && !isBoss(target)) {
					Vec3 inward = center.subtract(target.position()).normalize().scale(0.9D);
					target.setDeltaMovement(inward.add(0.0D, -0.2D, 0.0D));
					target.hurtMarked = true;
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2, false, true));
				}
			}
			ServerLevel level = player.serverLevel();
			level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.3D, center.z,
					1, 0.0D, 0.0D, 0.0D, 0.0D);
			groundCracks(player, center, radius * 0.6D);
			play(player, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.55F);
		});
		return true;
	}

	// ── Mass and Poise passive ────────────────────────────────────────────────

	private static boolean ownsAnyManaged(ServerPlayer player) {
		String list = player.getCapability(
				SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.map(vars -> vars.Plist).orElse("");
		if (list == null || list.isBlank())
			return false;
		for (String entry : list.split(",")) {
			String token = entry.trim();
			while (token.startsWith("."))
				token = token.substring(1).trim();
			if (MANAGED_SKILLS.contains(token))
				return true;
		}
		return false;
	}

	private static void addPoise(ServerPlayer player, MassState state, double amount) {
		if (!ownsAnyManaged(player))
			return;
		double updated = Mth.clamp(state.poise + Math.min(20.0D, amount), 0.0D, POISE_MAX);
		if (Math.abs(updated - state.poise) < 0.01D)
			return;
		state.poise = updated;
		syncPoise(player, state);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onDamaged(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)
				|| player.level().isClientSide())
			return;
		MassState state = STATES.get(player.getUUID());
		if (state == null && ownsAnyManaged(player))
			state = state(player);
		if (state == null)
			return;
		long now = player.level().getGameTime();
		// Holding ground under pressure is what builds Mass.
		addPoise(player, state, 4.0D + vitality(player) * 0.02D);
		if (state.ironBodyPerfectUntil >= now) {
			state.ironBodyPerfectUntil = 0L;
			addPoise(player, state, 18.0D);
			event.setAmount(event.getAmount() * 0.45F);
			play(player, SoundEvents.ANVIL_PLACE, 0.9F, 1.6F);
		}
	}

	@SubscribeEvent
	public static void onKnockback(LivingKnockBackEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		MassState state = STATES.get(player.getUUID());
		if (state == null)
			return;
		long now = player.level().getGameTime();
		boolean braced = state.ironBodyUntil >= now || state.gigantUntil >= now;
		if (!braced)
			return;
		// Resisting displacement is itself a Poise source.
		event.setStrength(event.getStrength() * 0.25F);
		addPoise(player, state, 3.0D);
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		MassState state = STATES.get(player.getUUID());
		if (state == null)
			return;
		long now = player.level().getGameTime();

		// Self-healing: losing every Juggernaut ability retires the state and
		// the Poise bar without every caller having to remember.
		if (now % 40L == 0L && !ownsAnyManaged(player)) {
			resetPlayerState(player);
			return;
		}

		if (state.grappledTarget != null && state.grappleUntil < now)
			state.grappledTarget = null;
		if (state.grappledTarget != null && now % 4L == 0L) {
			Entity grappled = player.serverLevel().getEntity(state.grappledTarget);
			if (grappled instanceof LivingEntity living && living.isAlive()) {
				// Server owns both positions; the target is pinned, not teleported.
				living.setDeltaMovement(Vec3.ZERO);
				living.hurtMarked = true;
			} else {
				state.grappledTarget = null;
			}
		}

		if (state.chargeUntil >= now) {
			Vec3 forward = player.getLookAngle().normalize();
			player.setDeltaMovement(forward.x * 0.9D, player.getDeltaMovement().y,
					forward.z * 0.9D);
			player.hurtMarked = true;
			for (LivingEntity target : nearbyTargets(player, player.position(), 2.2D, 6)) {
				if (isBoss(target)) {
					hurt(player, target, (float) (2.0D + vitality(player) * 0.1D));
					continue;
				}
				if (state.chargeGathered.add(target.getUUID()))
					hurt(player, target, (float) (4.0D + strength(player) * 0.15D));
				// Gathered enemies are dragged along the front.
				target.setDeltaMovement(forward.scale(0.85D).add(0.0D, 0.05D, 0.0D));
				target.hurtMarked = true;
			}
		} else if (!state.chargeGathered.isEmpty()) {
			releaseCharge(player, state);
		}

		if (state.gigantUntil > 0L) {
			if (state.gigantUntil < now) {
				endGigantification(player, state);
			} else if (state.gigantNextDrain <= now) {
				state.gigantNextDrain = now + 20L;
				double reduction = state.poise >= 50.0D ? 0.6D : 1.0D;
				int upkeep = Math.max(1,
						(int) (ManaRules.cost(player, ManaRules.Band.HIGH) / 50.0D * reduction));
				if (!ManaRules.spend(player, upkeep))
					endGigantification(player, state);
			}
		}

		// Poise bleeds away out of combat rather than banking forever.
		if (state.poise > 0.0D && now % 40L == 0L
				&& player.getLastHurtByMob() == null && player.getLastHurtMob() == null) {
			state.poise = Math.max(0.0D, state.poise - 2.0D);
			syncPoise(player, state);
		}
	}

	private static void releaseCharge(ServerPlayer player, MassState state) {
		float damage = (float) (12.0D + strength(player) * 0.5D);
		for (UUID id : state.chargeGathered) {
			Entity entity = player.serverLevel().getEntity(id);
			if (!(entity instanceof LivingEntity living) || !living.isAlive())
				continue;
			living.invulnerableTime = 0;
			if (hurt(player, living, damage)) {
				Vec3 outward = living.position().subtract(player.position())
						.normalize().scale(1.5D);
				living.setDeltaMovement(outward.add(0.0D, 0.4D, 0.0D));
				living.hurtMarked = true;
			}
		}
		state.chargeGathered.clear();
		groundCracks(player, player.position(), 2.5D);
		play(player, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.8F);
	}

	// ── lifecycle ─────────────────────────────────────────────────────────────

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
		MassState removed = STATES.remove(player.getUUID());
		if (removed != null) {
			endGigantification(player, removed);
			removed.grappledTarget = null;
			removed.chargeGathered.clear();
		}
		// Sent unconditionally so a reset or class change cannot leave the
		// Poise bar latched on for the rest of the session.
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(8, ClassPassiveClientState.UNAVAILABLE));
	}

	// ── helpers ───────────────────────────────────────────────────────────────

	private static MassState state(ServerPlayer player) {
		return STATES.computeIfAbsent(player.getUUID(), ignored -> new MassState());
	}

	private static void syncPoise(ServerPlayer player, MassState state) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new ClassPassiveMessage(8, state.poise));
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

	private static boolean hurt(ServerPlayer player, LivingEntity target, float amount) {
		if (amount <= 0.0F || !MageCombatHelper.isValidTarget(player, target))
			return false;
		MassState state = state(player);
		DamageSource source = new DamageSource(player.level().registryAccess()
				.registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(TANKER_DAMAGE), player);
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

	private static List<LivingEntity> nearbyTargets(ServerPlayer player, Vec3 center,
			double radius, int cap) {
		List<LivingEntity> targets = new ArrayList<>(player.serverLevel().getEntitiesOfClass(
				LivingEntity.class, new AABB(center, center).inflate(radius),
				candidate -> MageCombatHelper.isValidTarget(player, candidate)));
		targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(center)));
		return targets.subList(0, Math.min(cap, targets.size()));
	}

	private static LivingEntity findLookTarget(ServerPlayer player, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 forward = player.getLookAngle().normalize();
		LivingEntity best = null;
		double bestDot = 0.80D;
		for (LivingEntity candidate : nearbyTargets(player,
				start.add(forward.scale(range * 0.5D)), range, 8)) {
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

	private static void groundCracks(ServerPlayer player, Vec3 center, double radius) {
		ServerLevel level = player.serverLevel();
		int points = 18;
		BlockPos below = BlockPos.containing(center.x, center.y - 0.5D, center.z);
		for (int index = 0; index < points; index++) {
			double radians = index * (Math.PI * 2.0D / points);
			level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
							ParticleTypes.BLOCK, level.getBlockState(below)),
					center.x + Math.cos(radians) * radius, center.y + 0.15D,
					center.z + Math.sin(radians) * radius, 2, 0.1D, 0.05D, 0.1D, 0.02D);
		}
	}

	private static boolean isBoss(LivingEntity target) {
		return !(target instanceof Player)
				&& (target.getType().is(BOSS_TAG) || target.getMaxHealth() >= 250.0F);
	}

	private static double strength(ServerPlayer player) {
		return Math.max(0.0D, TemporaryStatBonusManager.effectiveStrength(player));
	}

	private static double vitality(ServerPlayer player) {
		return Math.max(0.0D, TemporaryStatBonusManager.effectiveVitality(player));
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
		player.displayClientMessage(Component.literal(text)
				.withStyle(ChatFormatting.GOLD), true);
	}

	private static final class MassState {
		private double poise;
		private long ironBodyUntil;
		private long ironBodyPerfectUntil;
		private UUID grappledTarget;
		private long grappleUntil;
		private long gigantUntil;
		private long gigantNextDrain;
		private long chargeUntil;
		private final java.util.Set<UUID> chargeGathered = new java.util.LinkedHashSet<>();
		private boolean internalDamage;
	}
}
