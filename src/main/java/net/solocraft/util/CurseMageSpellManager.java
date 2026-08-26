package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.HealerVfxEntity;
import net.solocraft.network.SololevelingModVariables;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Curse Mage — the fifth Mage style.
 *
 * <p>Where the other four styles decide their effect at cast time, Curse Mage
 * separates <em>delivery</em> from <em>payload</em>. One skill arms a curse through
 * a radial wheel; the rest are delivery vehicles that apply whatever is armed. Four
 * deliveries across six curses is twenty-four outcomes from ten authored pieces.
 *
 * <p>The cooldown consequence follows from that split: a lockout on the ability
 * would be meaningless when the ability is only a carrier, so <b>the cooldown
 * belongs to the curse</b> and scales with how it was delivered. Spreading one
 * curse over a crowd locks it out far longer than placing it on a single target,
 * which is what makes stacking several curses safe to allow.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class CurseMageSpellManager {
	public static final String CURSE_WEAVE = "Curse Weave";
	public static final String HEX_BOLT = "Hex Bolt";
	public static final String MALEFIC_BURST = "Malefic Burst";
	public static final String CREEPING_MIASMA = "Creeping Miasma";
	public static final String VECTOR_OF_RUIN = "Vector of Ruin";
	public static final String CULLING = "Culling";

	public static final Set<String> CURSE_SKILLS = Set.of(CURSE_WEAVE, HEX_BOLT,
			MALEFIC_BURST, CREEPING_MIASMA, VECTOR_OF_RUIN, CULLING);
	/** Spells that run the mage aiming ring and cast on key release. */
	public static final Set<String> QTE_SKILLS = Set.of(MALEFIC_BURST, CREEPING_MIASMA, CULLING);
	/** Spells that fire immediately on key press. */
	public static final Set<String> INSTANT_SKILLS = Set.of(HEX_BOLT, VECTOR_OF_RUIN);

	private static final int PRIMARY_COLOR = 0xA05CFF;
	private static final int SECONDARY_COLOR = 0xD8A0FF;
	private static final double[] COST_MULTIPLIER = {0.0D, 1.0D, 1.10D, 1.20D, 1.30D, 1.40D};
	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE,
			ResourceLocation.parse("soloboss"));

	/**
	 * A small floor between deliveries so a full wheel cannot be dumped in one
	 * tick. Set to zero for the pure "only curses have cooldowns" model.
	 */
	public static final int GLOBAL_COOLDOWN = 15;
	private static final String GLOBAL_COOLDOWN_KEY = "curse_delivery";

	private static final double BOLT_RANGE = 26.0D;
	private static final double BURST_RANGE = 24.0D;
	private static final double MIASMA_RANGE = 20.0D;
	private static final double CULLING_RADIUS = 12.0D;
	private static final int MIASMA_LIFETIME = 160;
	private static final double MIASMA_RADIUS = 4.0D;

	private static final Map<UUID, ProxyGrant> PROXIES = new ConcurrentHashMap<>();
	private static final List<MiasmaField> ACTIVE_FIELDS = new ArrayList<>();

	private CurseMageSpellManager() {
	}

	public static boolean isCurseSkill(String skill) {
		return skill != null && CURSE_SKILLS.contains(skill);
	}

	public static boolean isQteSkill(String skill) {
		return skill != null && QTE_SKILLS.contains(skill);
	}

	public static boolean isInstantSkill(String skill) {
		return skill != null && INSTANT_SKILLS.contains(skill);
	}

	/**
	 * Output stage from effective Intelligence, on the same 30/55/80/110 ladder the
	 * other Mage styles and Healer use so the whole game shares one curve.
	 */
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

	public static String stageName(int stage) {
		return switch (stage) {
			case 1 -> "Ill Omen";
			case 2 -> "Malediction";
			case 3 -> "Affliction";
			case 4 -> "Anathema";
			case 5 -> "Calamity";
			default -> "Dormant";
		};
	}

	// ── Armed curse ───────────────────────────────────────────────────────────

	/** The curse the wheel currently has armed, defaulting to the starter curse. */
	public static CurseType armedCurse(Entity caster) {
		return CurseType.byKey(variables(caster).armedCurse);
	}

	public static void setArmedCurse(Entity caster, CurseType curse) {
		if (caster == null || curse == null)
			return;
		caster.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(data -> {
					data.armedCurse = curse.key();
					data.syncPlayerVariables(caster);
				});
	}

	/** Curses this player has earned, in roster order. Drives the wheel contents. */
	public static List<CurseType> unlockedCurses(Entity caster) {
		int tier = MageSpellProgression.tierForRank(variables(caster).HunterRank);
		List<CurseType> unlocked = new ArrayList<>();
		for (CurseType curse : CurseType.values()) {
			if (curse.unlockTier() <= tier)
				unlocked.add(curse);
		}
		if (unlocked.isEmpty())
			unlocked.add(CurseType.WITHERING);
		return unlocked;
	}

	// ── Casting ───────────────────────────────────────────────────────────────

	public static boolean cast(Entity caster, String skill, QTEResult qteResult) {
		if (!(caster.level() instanceof ServerLevel level) || !isCurseSkill(skill))
			return false;
		// The wheel commits through its own validated packet, exactly as Frozen
		// Architecture does. There is nothing to cast here.
		if (CURSE_WEAVE.equals(skill))
			return false;
		if (caster instanceof Player && !MageSpellProgression.canCastLearnedSkill(caster, skill)) {
			message(caster, "You have not learned this Curse skill.");
			return false;
		}
		if (CooldownManager.isOnCooldown(caster, GLOBAL_COOLDOWN_KEY)) {
			message(caster, "Still weaving.");
			return false;
		}

		int stage = outputStage(caster);
		CurseType curse = armedCurse(caster);
		CurseType.CurseDelivery delivery = deliveryFor(skill);
		// Culling spends curses rather than applying one, so it is the only skill
		// that does not answer to a curse lockout.
		if (delivery != null && CooldownManager.isOnCooldown(caster, curse.cooldownKey())) {
			message(caster, curse.displayName() + " is still recovering.");
			return false;
		}

		QTEResult result = qteResult == null ? QTEResult.MISS : qteResult;
		int cost = manaCost(caster, skill, stage, result);
		if (!(caster instanceof Player player && player.isCreative())
				&& variables(caster).MP < cost) {
			message(caster, "Not enough MP! Need " + cost + ".");
			return false;
		}

		boolean cast = switch (skill) {
			case HEX_BOLT -> castHexBolt(level, caster, curse, stage);
			case MALEFIC_BURST -> castMaleficBurst(level, caster, curse, stage);
			case CREEPING_MIASMA -> castCreepingMiasma(level, caster, curse, stage);
			case VECTOR_OF_RUIN -> castVectorOfRuin(level, caster, curse, stage);
			case CULLING -> castCulling(level, caster, stage);
			default -> false;
		};
		if (!cast)
			return false;

		if (cost > 0)
			deductMana(caster, cost);
		if (delivery != null)
			CooldownManager.set(caster, curse.cooldownKey(), curse.cooldownTicks(delivery));
		if (CULLING.equals(skill))
			CooldownManager.set(caster, "curse_culling", cullingCooldown(stage));
		if (GLOBAL_COOLDOWN > 0)
			CooldownManager.set(caster, GLOBAL_COOLDOWN_KEY, GLOBAL_COOLDOWN);
		CooldownManager.set(caster, "mana_refresh", 40);
		return true;
	}

	/** No-mana, no-QTE mirror used by generated hunters. */
	public static boolean castNpc(Entity caster, String skill) {
		if (!(caster.level() instanceof ServerLevel level) || !isCurseSkill(skill)
				|| CURSE_WEAVE.equals(skill))
			return false;
		if (CooldownManager.isOnCooldown(caster, skill))
			return false;
		int stage = outputStage(caster);
		CurseType curse = armedCurse(caster);
		boolean cast = switch (skill) {
			case HEX_BOLT -> castHexBolt(level, caster, curse, stage);
			case MALEFIC_BURST -> castMaleficBurst(level, caster, curse, stage);
			case CREEPING_MIASMA -> castCreepingMiasma(level, caster, curse, stage);
			case CULLING -> castCulling(level, caster, stage);
			default -> false;
		};
		if (cast)
			CooldownManager.set(caster, skill, 80);
		return cast;
	}

	/** Which delivery a skill uses, or null when it applies no curse. */
	public static CurseType.CurseDelivery deliveryFor(String skill) {
		return switch (skill) {
			case HEX_BOLT -> CurseType.CurseDelivery.DIRECT;
			case MALEFIC_BURST -> CurseType.CurseDelivery.AREA;
			case CREEPING_MIASMA -> CurseType.CurseDelivery.FIELD;
			case VECTOR_OF_RUIN -> CurseType.CurseDelivery.PROXY;
			default -> null;
		};
	}

	// ── Deliveries ────────────────────────────────────────────────────────────

	private static boolean castHexBolt(ServerLevel level, Entity caster, CurseType curse,
			int stage) {
		LivingEntity target = findLookTarget(level, caster, BOLT_RANGE);
		if (target == null) {
			message(caster, "No target in sight.");
			return false;
		}
		applyCurse(level, caster, target, curse, stage);
		MageCombatHelper.hurt(level, caster, target, boltDamage(caster, stage));
		HealerVfxEntity.beam(level, caster.getEyePosition(),
				target.getBoundingBox().getCenter(), stage, curse.accentColor(), 12);
		play(level, caster, SoundEvents.EVOKER_CAST_SPELL, 0.7F, 1.4F);
		return true;
	}

	private static boolean castMaleficBurst(ServerLevel level, Entity caster, CurseType curse,
			int stage) {
		Vec3 center = aimPoint(level, caster, BURST_RANGE);
		double radius = 3.5D + stage * 0.5D;
		List<LivingEntity> hit = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(radius),
				candidate -> MageCombatHelper.isValidTarget(caster, candidate));
		if (hit.isEmpty()) {
			message(caster, "Nothing within the burst.");
			return false;
		}
		for (LivingEntity target : hit) {
			applyCurse(level, caster, target, curse, stage);
			MageCombatHelper.hurt(level, caster, target, burstDamage(caster, stage));
		}
		HealerVfxEntity.wave(level, center, stage, (float) radius, curse.accentColor(), 18);
		play(level, caster, SoundEvents.EVOKER_PREPARE_ATTACK, 0.8F, 0.8F);
		return true;
	}

	private static boolean castCreepingMiasma(ServerLevel level, Entity caster, CurseType curse,
			int stage) {
		Vec3 center = groundBelow(level, aimPoint(level, caster, MIASMA_RANGE));
		double radius = MIASMA_RADIUS + stage * 0.35D;
		int lifetime = MIASMA_LIFETIME + stage * 20;
		ACTIVE_FIELDS.add(new MiasmaField(level, caster.getUUID(), center, radius, curse, stage,
				level.getGameTime() + lifetime));
		HealerVfxEntity.field(level, center, stage, (float) radius, curse.accentColor(), lifetime);
		play(level, caster, SoundEvents.EVOKER_PREPARE_SUMMON, 0.7F, 0.7F);
		return true;
	}

	private static boolean castVectorOfRuin(ServerLevel level, Entity caster, CurseType curse,
			int stage) {
		if (!(caster instanceof ServerPlayer weaver))
			return false;
		List<LivingEntity> allies = alliesFor(weaver, stage);
		if (allies.isEmpty()) {
			message(caster, "No allies in range.");
			return false;
		}
		int charges = 2 + stage;
		long expiry = level.getGameTime() + 400L + stage * 40L;
		LinkedHashSet<UUID> carriers = new LinkedHashSet<>();
		for (LivingEntity ally : allies) {
			carriers.add(ally.getUUID());
			HealerVfxEntity.sigil(level, ally.getBoundingBox().getCenter(), stage, 0.9F,
					curse.accentColor(), 40);
		}
		PROXIES.put(weaver.getUUID(), new ProxyGrant(weaver.getUUID(), carriers, curse, stage,
				charges, expiry));
		message(caster, "Vector of Ruin: allies carry " + curse.displayName() + ".");
		play(level, caster, SoundEvents.BEACON_POWER_SELECT, 0.7F, 1.2F);
		return true;
	}

	private static boolean castCulling(ServerLevel level, Entity caster, int stage) {
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(caster.position(), caster.position()).inflate(CULLING_RADIUS),
				candidate -> MageCombatHelper.isValidTarget(caster, candidate));
		int detonated = 0;
		for (LivingEntity target : nearby) {
			List<CurseType> carried = CurseState.activeCursesFrom(target, caster);
			if (carried.isEmpty())
				continue;
			// The payoff for stacking: damage rises with how much the target is
			// carrying, and every curse is spent doing it.
			float damage = cullingDamage(caster, stage, carried.size());
			MageCombatHelper.hurt(level, caster, target, damage);
			for (CurseType curse : carried)
				CurseState.clear(target, curse);
			HealerVfxEntity.sigil(level, target.getBoundingBox().getCenter(), stage,
					1.2F, PRIMARY_COLOR, 24);
			detonated++;
		}
		if (detonated == 0) {
			message(caster, "Nothing nearby carries your curses.");
			return false;
		}
		play(level, caster, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.6F);
		return true;
	}

	// ── Curse application and payloads ────────────────────────────────────────

	private static void applyCurse(ServerLevel level, Entity caster, LivingEntity target,
			CurseType curse, int stage) {
		int duration = (int) Math.round(curse.baseDurationTicks() * (1.0D + stage * 0.12D));
		// Control-style curses are resisted by tougher targets, expressed as a
		// shorter hold rather than a weaker one.
		if (curse == CurseType.LEADEN)
			duration = (int) Math.max(20L, Math.round(duration * controlFactor(caster, target)));
		CurseState.apply(caster, target, curse, duration, stage);
	}


	/** Blight jumps to a fresh host when its carrier dies. */
	@SubscribeEvent
	public static void onCursedDeath(LivingDeathEvent event) {
		LivingEntity victim = event.getEntity();
		if (victim.level().isClientSide() || !(victim.level() instanceof ServerLevel level))
			return;
		if (!CurseState.has(victim, CurseType.BLIGHT))
			return;
		UUID ownerId = CurseState.ownerOf(victim, CurseType.BLIGHT);
		ServerPlayer owner = ownerId == null ? null
				: level.getServer().getPlayerList().getPlayer(ownerId);
		CurseState.clear(victim, CurseType.BLIGHT);
		if (owner == null)
			return;
		Vec3 center = victim.getBoundingBox().getCenter();
		LivingEntity host = level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(8.0D),
				candidate -> MageCombatHelper.isValidTarget(owner, candidate)
						&& !CurseState.has(candidate, CurseType.BLIGHT))
				.stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)))
				.orElse(null);
		if (host == null)
			return;
		applyCurse(level, owner, host, CurseType.BLIGHT, outputStage(owner));
		HealerVfxEntity.beam(level, center, host.getBoundingBox().getCenter(),
				outputStage(owner), CurseType.BLIGHT.accentColor(), 14);
	}

	// ── Vector of Ruin proxy ──────────────────────────────────────────────────

	/**
	 * An ally's hit lands the weaver's curse.
	 *
	 * <p>Post rather than Pre: this must observe the hit, never change its damage.
	 * Attribution runs through {@link #owningPlayer} so a pet, projectile or shadow
	 * belonging to a carrier still counts as that carrier swinging.
	 */
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onAllyHit(LivingDamageEvent.Post event) {
		LivingEntity victim = event.getEntity();
		if (victim.level().isClientSide() || !(victim.level() instanceof ServerLevel level)
				|| PROXIES.isEmpty())
			return;
		ServerPlayer carrier = owningPlayer(event.getSource().getEntity());
		if (carrier == null)
			return;
		long now = level.getGameTime();
		for (ProxyGrant grant : PROXIES.values()) {
			if (grant.expiry < now || grant.charges <= 0
					|| !grant.carriers.contains(carrier.getUUID()))
				continue;
			ServerPlayer weaver = level.getServer().getPlayerList().getPlayer(grant.ownerId);
			if (weaver == null || !MageCombatHelper.isValidTarget(weaver, victim))
				continue;
			// The curse and its lockout belong to the weaver, not the ally who
			// happened to swing.
			if (CooldownManager.isOnCooldown(weaver, grant.curse.cooldownKey()))
				continue;
			applyCurse(level, weaver, victim, grant.curse, grant.stage);
			CooldownManager.set(weaver, grant.curse.cooldownKey(),
					grant.curse.cooldownTicks(CurseType.CurseDelivery.PROXY));
			grant.charges--;
			HealerVfxEntity.sigil(level, victim.getBoundingBox().getCenter(), grant.stage,
					0.8F, grant.curse.accentColor(), 18);
			return;
		}
	}

	/** Canonical attribution: unwraps pets, projectiles and shadows to their owner. */
	private static ServerPlayer owningPlayer(Entity source) {
		if (source instanceof ServerPlayer player)
			return player;
		if (source instanceof net.minecraft.world.entity.TamableAnimal tame
				&& tame.getOwner() instanceof ServerPlayer owner)
			return owner;
		if (source instanceof net.minecraft.world.entity.projectile.Projectile projectile
				&& projectile.getOwner() != null)
			return owningPlayer(projectile.getOwner());
		if (source != null) {
			UUID ownerId = ShadowMonarchManager.getShadowOwnerUUID(source);
			if (ownerId != null && source.getServer() != null)
				return source.getServer().getPlayerList().getPlayer(ownerId);
		}
		return null;
	}

	private static List<LivingEntity> alliesFor(ServerPlayer weaver, int stage) {
		double radius = 10.0D + stage;
		List<LivingEntity> allies = new ArrayList<>();
		for (LivingEntity candidate : weaver.serverLevel().getEntitiesOfClass(LivingEntity.class,
				weaver.getBoundingBox().inflate(radius),
				living -> HealerSkillManager.isValidAlly(weaver, living))) {
			allies.add(candidate);
			if (allies.size() >= 4)
				break;
		}
		if (allies.isEmpty())
			allies.add(weaver);
		return allies;
	}

	// ── Ticking ───────────────────────────────────────────────────────────────

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		for (Iterator<MiasmaField> iterator = ACTIVE_FIELDS.iterator(); iterator.hasNext();) {
			if (iterator.next().tick())
				iterator.remove();
		}
		long now = event.getServer().overworld().getGameTime();
		PROXIES.values().removeIf(grant -> grant.expiry < now || grant.charges <= 0);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			PROXIES.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		PROXIES.clear();
		ACTIVE_FIELDS.clear();
	}

	public static void resetPlayerState(ServerPlayer player) {
		if (player == null)
			return;
		PROXIES.remove(player.getUUID());
		ACTIVE_FIELDS.removeIf(field -> field.ownerId.equals(player.getUUID()));
		for (CurseType curse : CurseType.values())
			CooldownManager.clear(player, curse.cooldownKey());
		CooldownManager.clear(player, GLOBAL_COOLDOWN_KEY);
		CurseState.clearAll(player);
	}

	// ── Numbers ───────────────────────────────────────────────────────────────

	public static int manaCost(Entity caster, String skill, int stage, QTEResult result) {
		double percent = switch (skill) {
			case HEX_BOLT -> 0.0035D;
			case MALEFIC_BURST -> 0.040D;
			case CREEPING_MIASMA -> 0.080D;
			case VECTOR_OF_RUIN -> 0.090D;
			case CULLING -> 0.180D;
			default -> 0.0D;
		};
		if (percent <= 0.0D)
			return 0;
		double maximumMana = ManaRules.maximumManaFor(MageCombatHelper.intelligence(caster));
		double qte = isQteSkill(skill)
				? MageQTEHelper.getManaCostMultiplier(result == null ? QTEResult.MISS : result,
						MageCombatHelper.intelligence(caster))
				: 1.0D;
		return Math.max(0, (int) OrbOfAvariceManager.adjustManaCost(caster, maximumMana * percent
				* COST_MULTIPLIER[Mth.clamp(stage, 1, 5)] * qte));
	}

	/**
	 * Deliveries answer to the curse lockout, not their own, so this only reports
	 * the shared anti-spam floor. Culling is the exception: it spends curses rather
	 * than applying one, so it carries a real cooldown of its own.
	 */
	public static int cooldownTicks(String skill) {
		return CULLING.equals(skill) ? cullingCooldown(1) : GLOBAL_COOLDOWN;
	}

	private static int cullingCooldown(int stage) {
		return Math.max(200, 420 - stage * 20);
	}

	private static float boltDamage(Entity caster, int stage) {
		return (float) ((3.0D + MageCombatHelper.intelligence(caster) * 0.045D)
				* (1.0D + stage * 0.08D));
	}

	private static float burstDamage(Entity caster, int stage) {
		return (float) ((4.0D + MageCombatHelper.intelligence(caster) * 0.05D)
				* (1.0D + stage * 0.08D));
	}

	private static float cullingDamage(Entity caster, int stage, int curseCount) {
		double base = 6.0D + MageCombatHelper.intelligence(caster) * 0.07D;
		return (float) (base * (1.0D + stage * 0.10D) * (0.7D + 0.45D * curseCount));
	}

	// ── Tooltips ──────────────────────────────────────────────────────────────

	public static List<Component> tooltip(Entity caster, String skill) {
		int stage = outputStage(caster);
		CurseType armed = armedCurse(caster);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(stageName(stage) + " Output - Stage " + stage)
				.withStyle(style -> style.withColor(PRIMARY_COLOR).withBold(true)));
		lines.add(Component.literal(description(skill)).withStyle(ChatFormatting.GRAY));
		CurseType.CurseDelivery delivery = deliveryFor(skill);
		if (delivery != null) {
			lines.add(Component.literal("Armed: " + armed.displayName() + "  |  "
					+ armed.displayName() + " lockout "
					+ String.format("%.1fs", armed.cooldownTicks(delivery) / 20.0D))
					.withStyle(style -> style.withColor(armed.accentColor())));
			lines.add(Component.literal(armed.description())
					.withStyle(style -> style.withColor(SECONDARY_COLOR)));
		}
		lines.add(Component.literal("Mana: " + manaCost(caster, skill, stage, QTEResult.MISS))
				.withStyle(ChatFormatting.DARK_GRAY));
		return lines;
	}

	private static String description(String skill) {
		return switch (skill) {
			case CURSE_WEAVE -> "Hold to open the curse wheel; release to arm your choice.";
			case HEX_BOLT -> "Spit the armed curse into a single target at range.";
			case MALEFIC_BURST -> "Detonate the armed curse across everything near your aim.";
			case CREEPING_MIASMA -> "Leave a lingering field that curses whatever walks in.";
			case VECTOR_OF_RUIN -> "Allies carry your curse; their hits land it for you.";
			case CULLING -> "Detonate every curse you have placed nearby at once.";
			default -> "Bind misfortune into a working.";
		};
	}

	// ── Helpers ───────────────────────────────────────────────────────────────

	private static LivingEntity findLookTarget(ServerLevel level, Entity caster, double range) {
		Vec3 start = caster.getEyePosition();
		Vec3 end = start.add(safeDirection(caster.getLookAngle()).scale(range));
		BlockHitResult blockHit = level.clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		if (blockHit.getType() != HitResult.Type.MISS)
			end = blockHit.getLocation();
		AABB search = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.5D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(caster, start, end, search,
				target -> target instanceof LivingEntity living
						&& MageCombatHelper.isValidTarget(caster, living), start.distanceToSqr(end));
		return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
	}

	private static Vec3 aimPoint(ServerLevel level, Entity caster, double range) {
		Vec3 start = caster.getEyePosition();
		Vec3 end = start.add(safeDirection(caster.getLookAngle()).scale(range));
		BlockHitResult blockHit = level.clip(new ClipContext(start, end,
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		Vec3 clipped = blockHit.getType() == HitResult.Type.MISS ? end : blockHit.getLocation();
		AABB search = caster.getBoundingBox().expandTowards(clipped.subtract(start)).inflate(1.2D);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, start, clipped, search,
				target -> target instanceof LivingEntity living
						&& MageCombatHelper.isValidTarget(caster, living),
				start.distanceToSqr(clipped));
		return entityHit == null ? clipped : entityHit.getEntity().getBoundingBox().getCenter();
	}

	private static Vec3 groundBelow(ServerLevel level, Vec3 point) {
		int startY = Mth.floor(point.y + 2.0D);
		for (int offset = 0; offset < 18; offset++) {
			net.minecraft.core.BlockPos cursor = new net.minecraft.core.BlockPos(
					Mth.floor(point.x), startY - offset, Mth.floor(point.z));
			if (!level.getBlockState(cursor).isAir()
					&& !level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty())
				return new Vec3(point.x, cursor.getY() + 1.015D, point.z);
		}
		return point;
	}

	private static Vec3 safeDirection(Vec3 direction) {
		return direction.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
	}

	/** Level-scaled control resistance, mirroring the Arcane convention. */
	private static double controlFactor(Entity caster, LivingEntity target) {
		double casterLevel = caster instanceof Player
				? variables(caster).Level : caster.getPersistentData().getDouble("Level");
		double targetLevel = target instanceof Player
				? variables(target).Level : target.getPersistentData().getDouble("Level");
		double factor = Mth.clamp(1.0D - Math.max(0.0D, targetLevel - casterLevel) * 0.025D,
				0.25D, 1.0D);
		if (isBoss(target))
			factor *= 0.35D;
		else if (target instanceof Player)
			factor *= 0.55D;
		return Mth.clamp(factor, 0.16D, 1.0D);
	}

	private static boolean isBoss(LivingEntity target) {
		return !(target instanceof Player)
				&& (target.getType().is(BOSS_TAG) || target.getMaxHealth() >= 250.0F);
	}

	private static void play(ServerLevel level, Entity caster, net.minecraft.sounds.SoundEvent sound,
			float volume, float pitch) {
		level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), sound,
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

	// ── State ─────────────────────────────────────────────────────────────────

	private static final class ProxyGrant {
		private final UUID ownerId;
		private final Set<UUID> carriers;
		private final CurseType curse;
		private final int stage;
		private int charges;
		private final long expiry;

		private ProxyGrant(UUID ownerId, Set<UUID> carriers, CurseType curse, int stage,
				int charges, long expiry) {
			this.ownerId = ownerId;
			this.carriers = carriers;
			this.curse = curse;
			this.stage = stage;
			this.charges = charges;
			this.expiry = expiry;
		}
	}

	private static final class MiasmaField {
		private final ServerLevel level;
		private final UUID ownerId;
		private final Vec3 center;
		private final double radius;
		private final CurseType curse;
		private final int stage;
		private final long expiry;

		private MiasmaField(ServerLevel level, UUID ownerId, Vec3 center, double radius,
				CurseType curse, int stage, long expiry) {
			this.level = level;
			this.ownerId = ownerId;
			this.center = center;
			this.radius = radius;
			this.curse = curse;
			this.stage = stage;
			this.expiry = expiry;
		}

		/** @return true once the field has expired and should be dropped */
		private boolean tick() {
			if (level.getGameTime() >= expiry)
				return true;
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
			if (owner == null)
				return true;
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
					new AABB(center, center).inflate(radius),
					candidate -> MageCombatHelper.isValidTarget(owner, candidate)
							&& !CurseState.isOwnedBy(candidate, curse, owner))) {
				applyCurse(level, owner, target, curse, stage);
			}
			return false;
		}
	}
}
