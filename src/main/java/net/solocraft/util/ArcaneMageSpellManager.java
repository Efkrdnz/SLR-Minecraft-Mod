package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.ArcaneVfxEntity;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative mechanics for the five-stage Arcane Mage spell set. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class ArcaneMageSpellManager {
	public static final String AETHER_BOLT = "Aether Bolt";
	public static final String VECTOR_STEP = "Vector Step";
	public static final String POLARITY_SPHERE = "Polarity Sphere";
	public static final String RUNIC_RELAY = "Runic Relay";
	public static final String ASTRAL_ARSENAL = "Astral Arsenal";
	public static final String DIMENSIONAL_REND = "Dimensional Rend";
	public static final String CONVERGENCE = "Grand Formula: Convergence";

	public static final int NORMAL_PRIMARY = 0x8A5CFF;
	public static final int NORMAL_SECONDARY = 0x47E6FF;
	public static final int ORB_PRIMARY = 0x2054FF;
	public static final int ORB_SECONDARY = 0xD31F55;

	public static final Set<String> ARCANE_SKILLS = Set.of(AETHER_BOLT, VECTOR_STEP,
			POLARITY_SPHERE, RUNIC_RELAY, ASTRAL_ARSENAL, DIMENSIONAL_REND, CONVERGENCE);
	public static final Set<String> QTE_SKILLS = Set.of(ASTRAL_ARSENAL, DIMENSIONAL_REND,
			CONVERGENCE);
	public static final Set<String> INSTANT_SKILLS = Set.of(AETHER_BOLT, VECTOR_STEP,
			POLARITY_SPHERE, RUNIC_RELAY);

	private static final double[] COST_MULTIPLIER = {0.0D, 1.0D, 1.10D, 1.20D, 1.30D, 1.40D};
	private static final int[] FORMULA_DURATION = {0, 200, 230, 260, 290, 320};
	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("soloboss"));

	private static final Map<UUID, FormulaState> FORMULAS = new HashMap<>();
	private static final Map<UUID, AnchorState> ANCHORS = new HashMap<>();
	private static final Map<UUID, RelayState> RELAYS = new HashMap<>();
	private static final Map<UUID, ArsenalState> ARSENALS = new HashMap<>();
	private static final List<BoltCast> ACTIVE_BOLTS = new ArrayList<>();
	private static final List<PolarityCast> ACTIVE_POLARITIES = new ArrayList<>();
	private static final List<RendCast> ACTIVE_RENDS = new ArrayList<>();
	private static final List<ConvergenceCast> ACTIVE_CONVERGENCES = new ArrayList<>();
	private static final List<DelayedBurst> DELAYED_BURSTS = new ArrayList<>();

	private ArcaneMageSpellManager() {
	}

	public static boolean isArcaneSkill(String skill) {
		return ARCANE_SKILLS.contains(skill);
	}

	public static boolean isQteSkill(String skill) {
		return QTE_SKILLS.contains(skill);
	}

	public static boolean isInstantSkill(String skill) {
		return INSTANT_SKILLS.contains(skill);
	}

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
		return switch (Mth.clamp(stage, 0, 5)) {
			case 1 -> "Trace";
			case 2 -> "Inscribed";
			case 3 -> "Runic Array";
			case 4 -> "Grand Formula";
			case 5 -> "Transcendent";
			default -> "Dormant";
		};
	}

	public static List<Component> tooltip(Entity caster, String skill) {
		int stage = outputStage(caster);
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal(stageName(stage) + " Output - Stage " + stage)
				.withStyle(style -> style.withColor(NORMAL_PRIMARY).withBold(true)));
		lines.add(Component.literal(description(skill)).withStyle(ChatFormatting.GRAY));
		lines.add(Component.literal(stageEffect(skill, stage))
				.withStyle(style -> style.withColor(NORMAL_SECONDARY)));
		lines.add(Component.literal("Arcane Formula: alternate spells to assemble 3 runes.")
				.withStyle(ChatFormatting.DARK_PURPLE));
		lines.add(Component.literal("Mana: " + manaCost(caster, skill, stage, QTEResult.MISS)
				+ "  |  Cooldown: " + String.format("%.1fs", cooldownTicks(skill, stage) / 20.0D))
				.withStyle(ChatFormatting.DARK_GRAY));
		if (OrbOfAvariceManager.isHeldBy(caster))
			lines.add(Component.literal("Orb: unstable cobalt-crimson output, x2 magic, +50% mana.")
					.withStyle(ChatFormatting.BLUE));
		return lines;
	}

	private static String description(String skill) {
		return switch (skill) {
			case AETHER_BOLT -> "Launch condensed raw mana with precision, correction, and ricochet growth.";
			case VECTOR_STEP -> "Rewrite your movement vector in a safe, collision-aware phase step.";
			case POLARITY_SPHERE -> "Create a controllable gravity node; sneak to reverse its polarity.";
			case RUNIC_RELAY -> "Link two runes so later Arcane attacks emerge from the distant glyph.";
			case ASTRAL_ARSENAL -> "Form orbiting mana blades that acquire targets or launch together on recast.";
			case DIMENSIONAL_REND -> "Cut space with a long plane; sneak for a shorter, wider rupture.";
			case CONVERGENCE -> "Assemble a battlefield formula that gathers, strikes, and collapses its center.";
			default -> "Shape raw magical law into a combat formula.";
		};
	}

	private static String stageEffect(String skill, int stage) {
		return switch (skill) {
			case AETHER_BOLT -> switch (stage) {
				case 1 -> "A fast single trace.";
				case 2 -> "Corrects toward nearby targets.";
				case 3 -> "Pierces and ricochets once.";
				case 4 -> "An inscribed triad follows the leading bolt.";
				default -> "A constellation line corrects through several targets.";
			};
			case VECTOR_STEP -> stage < 2 ? "A direct safe phase step."
					: stage < 4 ? "Leaves a recall anchor; sneak-cast to return."
					: "Curves toward aimed targets and leaves damaging afterimages.";
			case POLARITY_SPHERE -> stage < 3 ? "Pulls or repels bodies and loose projectiles."
					: stage < 5 ? "Suspends targets between repeated pressure pulses."
					: "Transcendent polarity can reverse an entire formation.";
			case RUNIC_RELAY -> stage < 3 ? "Routes bolts and cuts through one exit glyph."
					: stage < 5 ? "Routes the full Arcane set with a widened gate."
					: "A constellation relay branches attacks across three exits.";
			case ASTRAL_ARSENAL -> "Maintains " + (3 + stage) + " orbiting blades with smarter distribution at higher stages.";
			case DIMENSIONAL_REND -> stage < 3 ? "A clean spatial plane."
					: stage < 5 ? "Adds crossing cuts and a delayed spatial scar."
					: "The scar persists through a widened transcendent rupture.";
			case CONVERGENCE -> stage < 3 ? "Gather, vertex strikes, then zero-point collapse."
					: stage < 5 ? "Adds projectile curvature and chained vertex strikes."
					: "A sky formula crushes the battlefield against the ground array.";
			default -> "";
		};
	}

	public static boolean cast(Entity caster, String skill, QTEResult qteResult) {
		if (!(caster.level() instanceof ServerLevel level) || !isArcaneSkill(skill))
			return false;

		if (ASTRAL_ARSENAL.equals(skill) && releaseArsenal(level, caster))
			return true;
		if (VECTOR_STEP.equals(skill) && caster.isShiftKeyDown() && recallAnchor(level, caster))
			return true;
		if (CooldownManager.isOnCooldown(caster, skill)) {
			message(caster, "Ability on cooldown!");
			return false;
		}

		int stage = outputStage(caster);
		QTEResult result = qteResult == null ? QTEResult.MISS : qteResult;
		boolean overcast = formulaReady(level, caster);
		int cost = manaCost(caster, skill, stage, result);
		SololevelingModVariables.PlayerVariables data = variables(caster);
		if (!(caster instanceof Player player && player.isCreative()) && data.MP < cost) {
			message(caster, "Not enough MP! Need " + cost + ".");
			return false;
		}

		if (cost > 0)
			deductMana(caster, cost);
		CooldownManager.set(caster, skill, cooldownTicks(skill, stage));
		CooldownManager.set(caster, "mana_refresh", 40);

		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		boolean started = switch (skill) {
			case AETHER_BOLT -> startAether(level, caster, stage,
					(float) (1.5D + intelligence * 0.032D), overcast);
			case VECTOR_STEP -> startVector(level, caster, stage,
					(float) (1.0D + intelligence * 0.015D), overcast);
			case POLARITY_SPHERE -> startPolarity(level, caster, stage,
					(float) (2.5D + intelligence * 0.040D), overcast);
			case RUNIC_RELAY -> startRelay(level, caster, stage, overcast);
			case ASTRAL_ARSENAL -> startArsenal(level, caster, stage,
					(float) (6.0D + intelligence * 0.065D), overcast);
			case DIMENSIONAL_REND -> startRend(level, caster, stage,
					(float) (7.0D + intelligence * 0.075D), overcast);
			case CONVERGENCE -> startConvergence(level, caster, stage,
					(float) (14.0D + intelligence * 0.10D), overcast);
			default -> false;
		};
		if (started)
			recordFormula(level, caster, skill, stage, result, overcast);
		return started;
	}

	public static boolean castNpc(Entity caster, String skill) {
		if (!(caster.level() instanceof ServerLevel level) || !isArcaneSkill(skill)
				|| CooldownManager.isOnCooldown(caster, skill))
			return false;
		Entity target = caster instanceof Mob mob ? mob.getTarget() : null;
		if (target == null && !ASTRAL_ARSENAL.equals(skill))
			return false;
		int stage = outputStage(caster);
		double intelligence = Math.max(10.0D, MageCombatHelper.intelligence(caster));
		boolean overcast = formulaReady(level, caster);
		boolean started = switch (skill) {
			case AETHER_BOLT -> startAether(level, caster, stage,
					(float) (1.5D + intelligence * 0.032D), overcast);
			case VECTOR_STEP -> startVector(level, caster, stage,
					(float) (1.0D + intelligence * 0.015D), overcast);
			case POLARITY_SPHERE -> startPolarity(level, caster, stage,
					(float) (2.5D + intelligence * 0.040D), overcast);
			case RUNIC_RELAY -> startRelay(level, caster, stage, overcast);
			case ASTRAL_ARSENAL -> startArsenal(level, caster, stage,
					(float) (6.0D + intelligence * 0.065D), overcast);
			case DIMENSIONAL_REND -> startRend(level, caster, stage,
					(float) (7.0D + intelligence * 0.075D), overcast);
			case CONVERGENCE -> startConvergence(level, caster, stage,
					(float) (14.0D + intelligence * 0.10D), overcast);
			default -> false;
		};
		if (started) {
			CooldownManager.set(caster, skill, cooldownTicks(skill, stage));
			recordFormula(level, caster, skill, stage, QTEResult.GOOD, overcast);
		}
		return started;
	}

	public static int manaCost(Entity caster, String skill, int stage, QTEResult result) {
		if (caster instanceof Player player && player.isCreative())
			return 0;
		double basePercent = switch (skill) {
			case AETHER_BOLT -> 0.0035D;
			case VECTOR_STEP -> 0.020D;
			case POLARITY_SPHERE -> 0.040D;
			case RUNIC_RELAY -> 0.045D;
			case ASTRAL_ARSENAL -> 0.080D;
			case DIMENSIONAL_REND -> 0.100D;
			case CONVERGENCE -> 0.180D;
			default -> 0.0D;
		};
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		double maximumMana = 1000.0D + intelligence * 100.0D;
		double qte = isQteSkill(skill)
				? MageQTEHelper.getManaCostMultiplier(result == null ? QTEResult.MISS : result,
						intelligence) : 1.0D;
		return Math.max(0, OrbOfAvariceManager.adjustManaCost(caster, maximumMana * basePercent
				* COST_MULTIPLIER[Mth.clamp(stage, 1, 5)] * qte));
	}

	private static int cooldownTicks(String skill, int stage) {
		return switch (skill) {
			case AETHER_BOLT -> 9;
			case VECTOR_STEP -> stage >= 5 ? 40 : 80;
			case POLARITY_SPHERE -> 160;
			case RUNIC_RELAY -> 200;
			case ASTRAL_ARSENAL -> 280;
			case DIMENSIONAL_REND -> 360;
			case CONVERGENCE -> 1200;
			default -> 20;
		};
	}

	private static boolean startAether(ServerLevel level, Entity caster, int stage, float damage,
			boolean overcast) {
		Vec3 direction = safeDirection(caster.getLookAngle());
		Vec3 origin = routedOrigin(level, caster, direction);
		int count = overcast ? 3 : stage >= 4 ? 3 : 1;
		float splitDamage = damage * (count == 1 ? 1.0F : overcast ? 0.42F : 0.38F);
		for (int i = 0; i < count; i++) {
			float offset = count == 1 ? 0.0F : (i - 1) * (overcast ? 12.0F : 5.0F);
			Vec3 shotDirection = rotateYaw(direction, offset);
			spawnBolt(level, caster, origin, shotDirection, stage, splitDamage,
					overcast, i * (stage >= 4 && !overcast ? 3 : 1),
					ArcaneVfxEntity.AETHER_BOLT, 0.30D + stage * 0.045D,
					2.45D + stage * 0.18D, 24.0D + stage * 5.0D,
					stage >= 5 ? 3 : stage >= 3 ? 2 : 1);
		}
		RelayState relay = activeRelay(level, caster);
		if (relay != null && relay.branches > 1) {
			for (int branch = 1; branch < relay.branches; branch++) {
				Vec3 branchDirection = rotateYaw(direction, branch == 1 ? -18.0F : 18.0F);
				spawnBolt(level, caster, relay.exit.add(branchDirection.scale(0.45D)),
						branchDirection, stage, damage * 0.28F, false, branch * 2,
						ArcaneVfxEntity.AETHER_BOLT, 0.28D + stage * 0.04D,
						2.4D + stage * 0.16D, 20.0D + stage * 4.0D,
						stage >= 3 ? 2 : 1);
			}
		}
		play(level, origin, SoundEvents.AMETHYST_BLOCK_CHIME, 0.72F, overcast ? 1.85F : 1.55F);
		return true;
	}

	private static void spawnBolt(ServerLevel level, Entity caster, Vec3 origin, Vec3 direction,
			int stage, float damage, boolean overcast, int delay, int style, double radius,
			double speed, double range, int maxHits) {
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		ArcaneVfxEntity effect = spawn(level, origin, style, stage,
				(float) (radius * (overcast ? 1.35D : 1.0D)),
				(float) (2.2D + stage * 0.75D), (int) Math.ceil(range / speed) + delay + 12,
				yawFor(direction), pitchFor(direction), caster, null, orb, overcast);
		ACTIVE_BOLTS.add(new BoltCast(level, caster.getUUID(), effect.getUUID(), stage, damage,
				origin, safeDirection(direction), range, speed, radius, maxHits, delay, orb,
				style, overcast));
	}

	private static boolean startVector(ServerLevel level, Entity caster, int stage, float damage,
			boolean overcast) {
		Vec3 origin = caster.position();
		Vec3 direction = safeDirection(caster.getLookAngle());
		LivingEntity aimed = stage >= 3 ? findLookTarget(level, caster, 13.0D + stage * 2.0D) : null;
		if (aimed != null)
			direction = safeDirection(aimed.getBoundingBox().getCenter().subtract(caster.getEyePosition()));
		double distance = 5.0D + stage * 1.45D;
		Vec3 destination = safeTeleportPoint(level, caster, direction, distance);
		if (destination.distanceToSqr(origin) < 0.36D) {
			message(caster, "No safe vector can be formed.");
			return false;
		}

		if (stage >= 2)
			setAnchor(level, caster, origin, stage);
		caster.teleportTo(destination.x, destination.y, destination.z);
		Vec3 delta = destination.subtract(origin);
		Vec3 center = origin.add(destination).scale(0.5D).add(0.0D, caster.getBbHeight() * 0.45D, 0.0D);
		spawn(level, center, ArcaneVfxEntity.VECTOR_TRAIL, stage,
				0.60F + stage * 0.15F, (float) delta.length(), 18,
				yawFor(delta), pitchFor(delta), caster, aimed,
				OrbOfAvariceManager.isHeldBy(caster), overcast);

		AABB route = new AABB(origin, destination).inflate(1.0D + stage * 0.16D);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, route,
				candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
			MageCombatHelper.hurt(level, caster, target, damage);
			Vec3 push = safeDirection(target.position().subtract(origin));
			target.setDeltaMovement(target.getDeltaMovement().add(push.scale(0.18D
					* controlFactor(caster, target))));
		}
		if (overcast) {
			damageArea(level, caster, origin.add(0.0D, 0.8D, 0.0D), 2.8D, damage * 0.65F, 0.14D);
			damageArea(level, caster, destination.add(0.0D, 0.8D, 0.0D), 3.2D, damage * 0.85F, 0.22D);
			spawn(level, origin.add(0.0D, 0.8D, 0.0D), ArcaneVfxEntity.AETHER_IMPACT,
					stage, 2.4F, 1.0F, 18, 0.0F, 0.0F, caster, null,
					OrbOfAvariceManager.isHeldBy(caster), true);
			spawn(level, destination.add(0.0D, 0.8D, 0.0D), ArcaneVfxEntity.AETHER_IMPACT,
					stage, 2.8F, 1.0F, 18, 0.0F, 0.0F, caster, null,
					OrbOfAvariceManager.isHeldBy(caster), true);
		}
		play(level, center, SoundEvents.ENDERMAN_TELEPORT, 0.72F, 1.55F);
		return true;
	}

	private static boolean startPolarity(ServerLevel level, Entity caster, int stage, float damage,
			boolean overcast) {
		Vec3 center = aimPoint(level, caster, 12.0D + stage * 3.0D);
		float radius = 3.8F + stage * 0.9F + (overcast ? 1.8F : 0.0F);
		int duration = 34 + stage * 7 + (overcast ? 12 : 0);
		boolean repel = caster.isShiftKeyDown();
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		ArcaneVfxEntity effect = spawn(level, center, ArcaneVfxEntity.POLARITY_SPHERE,
				stage, radius, radius, duration + 8, caster.getYRot(), 0.0F, caster, null,
				orb, overcast);
		ACTIVE_POLARITIES.add(new PolarityCast(level, caster.getUUID(), effect.getUUID(),
				stage, center, radius, duration, damage, repel, overcast));
		play(level, center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.9F, repel ? 0.62F : 1.15F);
		return true;
	}

	private static boolean startRelay(ServerLevel level, Entity caster, int stage,
			boolean overcast) {
		RelayState previous = RELAYS.remove(caster.getUUID());
		if (previous != null)
			previous.discardEffects();
		Vec3 direction = safeDirection(caster.getLookAngle());
		Vec3 entry = caster.getEyePosition().add(direction.scale(1.45D));
		Vec3 exit = aimPoint(level, caster, 18.0D + stage * 4.0D);
		if (exit.distanceToSqr(entry) < 4.0D)
			exit = entry.add(direction.scale(4.0D));
		int duration = 120 + stage * 30;
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		List<UUID> effects = new ArrayList<>();
		effects.add(spawn(level, entry, ArcaneVfxEntity.RUNIC_RELAY, stage,
				1.0F + stage * 0.16F, 1.0F, duration + 8, yawFor(direction), pitchFor(direction),
				caster, null, orb, overcast).getUUID());
		effects.add(spawn(level, exit, ArcaneVfxEntity.RUNIC_RELAY, stage,
				1.25F + stage * 0.20F, 1.0F, duration + 8, yawFor(direction), pitchFor(direction),
				caster, null, orb, overcast).getUUID());
		Vec3 link = exit.subtract(entry);
		effects.add(spawn(level, entry, ArcaneVfxEntity.RELAY_BEAM, stage,
				0.30F + stage * 0.05F, (float) link.length(), duration + 8,
				yawFor(link), pitchFor(link), caster, null, orb, overcast).getUUID());
		int branches = overcast ? 3 : stage >= 5 ? 2 : 1;
		if (branches > 1) {
			for (int branch = 1; branch < branches; branch++) {
				Vec3 branchPos = exit.add(rotateYaw(direction, branch == 1 ? -28.0F : 28.0F)
						.scale(2.2D + stage * 0.25D));
				effects.add(spawn(level, branchPos, ArcaneVfxEntity.RUNIC_RELAY, stage,
						0.9F + stage * 0.13F, 1.0F, duration + 8,
						yawFor(direction), pitchFor(direction), caster, null, orb, true).getUUID());
			}
		}
		RELAYS.put(caster.getUUID(), new RelayState(level, caster.getUUID(), entry, exit,
				level.getGameTime() + duration, branches, effects));
		play(level, exit, SoundEvents.PORTAL_TRIGGER, 0.65F, 1.55F);
		return true;
	}

	private static boolean startArsenal(ServerLevel level, Entity caster, int stage, float damage,
			boolean overcast) {
		ArsenalState previous = ARSENALS.remove(caster.getUUID());
		if (previous != null)
			previous.discardEffect();
		int blades = 3 + stage + (overcast ? 3 : 0);
		int duration = 180 + stage * 20;
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		ArcaneVfxEntity effect = spawn(level,
				caster.position().add(0.0D, caster.getBbHeight() * 0.52D, 0.0D),
				ArcaneVfxEntity.ASTRAL_ARSENAL, stage, blades, 1.0F, duration + 10,
				caster.getYRot(), 0.0F, caster, null, orb, overcast);
		ARSENALS.put(caster.getUUID(), new ArsenalState(level, caster.getUUID(),
				effect.getUUID(), stage, blades, damage, level.getGameTime() + duration,
				orb, overcast));
		play(level, caster.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.8F, 1.35F);
		return true;
	}

	private static boolean releaseArsenal(ServerLevel level, Entity caster) {
		ArsenalState arsenal = ARSENALS.get(caster.getUUID());
		if (arsenal == null || arsenal.level != level || arsenal.remaining <= 0)
			return false;
		arsenal.releaseAll(caster);
		ARSENALS.remove(caster.getUUID());
		return true;
	}

	private static boolean startRend(ServerLevel level, Entity caster, int stage, float damage,
			boolean overcast) {
		Vec3 direction = safeDirection(caster.getLookAngle());
		Vec3 origin = routedOrigin(level, caster, direction);
		boolean wide = caster.isShiftKeyDown();
		float width = wide ? 3.0F + stage * 0.65F : 1.0F + stage * 0.38F;
		float length = wide ? 7.0F + stage : 10.0F + stage * 1.5F;
		double range = wide ? 15.0D + stage * 3.0D : 24.0D + stage * 6.0D;
		double speed = wide ? 2.8D + stage * 0.16D : 3.8D + stage * 0.20D;
		spawnRend(level, caster, origin, direction, stage, width, length, range, speed,
				damage, overcast, 0);
		if (overcast || stage >= 4) {
			float angle = overcast ? 13.0F : 7.0F;
			spawnRend(level, caster, origin, rotateYaw(direction, -angle), stage,
					width * 0.76F, length * 0.92F, range * 0.92D, speed,
					damage * (overcast ? 0.58F : 0.34F), overcast, overcast ? 2 : 5);
			if (overcast)
				spawnRend(level, caster, origin, rotateYaw(direction, angle), stage,
						width * 0.76F, length * 0.92F, range * 0.92D, speed,
						damage * 0.58F, true, 4);
		}
		play(level, origin, SoundEvents.TRIDENT_THROW, 1.0F, overcast ? 0.72F : 1.25F);
		return true;
	}

	private static void spawnRend(ServerLevel level, Entity caster, Vec3 origin, Vec3 direction,
			int stage, float width, float length, double range, double speed, float damage,
			boolean overcast, int delay) {
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		ArcaneVfxEntity effect = spawn(level, origin, ArcaneVfxEntity.DIMENSIONAL_REND,
				stage, width, length, (int) Math.ceil(range / speed) + delay + 14,
				yawFor(direction), pitchFor(direction), caster, null, orb, overcast);
		ACTIVE_RENDS.add(new RendCast(level, caster.getUUID(), effect.getUUID(), stage,
				origin, safeDirection(direction), range, speed, width, damage, delay, orb,
				overcast));
	}

	private static boolean startConvergence(ServerLevel level, Entity caster, int stage,
			float damage, boolean overcast) {
		Vec3 center = caster.isShiftKeyDown()
				? groundBelow(level, caster.position())
				: groundPoint(level, aimPoint(level, caster, 18.0D + stage * 4.0D));
		float radius = 6.0F + stage * 1.8F + (overcast ? 2.8F : 0.0F);
		int duration = 52 + stage * 6 + (overcast ? 12 : 0);
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		ArcaneVfxEntity ground = spawn(level, center, ArcaneVfxEntity.CONVERGENCE_GROUND,
				stage, radius, radius, duration + 12, caster.getYRot(), 0.0F, caster, null,
				orb, overcast);
		UUID skyId = null;
		if (stage >= 5 || overcast) {
			Vec3 sky = center.add(0.0D, 8.0D + stage * 1.6D, 0.0D);
			skyId = spawn(level, sky, ArcaneVfxEntity.CONVERGENCE_SKY, stage,
					radius * 0.9F, radius, duration + 12, caster.getYRot(), 180.0F,
					caster, null, orb, overcast).getUUID();
			spawn(level, center, ArcaneVfxEntity.CONVERGENCE_TETHER, stage,
					radius * 0.7F, (float) (sky.y - center.y), duration + 12,
					0.0F, -90.0F, caster, null, orb, overcast);
		}
		ACTIVE_CONVERGENCES.add(new ConvergenceCast(level, caster.getUUID(),
				ground.getUUID(), skyId, stage, center, radius, duration, damage, orb,
				overcast));
		play(level, center, SoundEvents.BEACON_ACTIVATE, 1.2F, 0.65F);
		return true;
	}

	private static boolean formulaReady(ServerLevel level, Entity caster) {
		FormulaState state = FORMULAS.get(caster.getUUID());
		if (state == null || state.level != level || state.expiresAt < level.getGameTime()) {
			clearFormula(caster.getUUID());
			return false;
		}
		return state.runes >= 3;
	}

	private static void recordFormula(ServerLevel level, Entity caster, String skill, int stage,
			QTEResult result, boolean consumedOvercast) {
		UUID casterId = caster.getUUID();
		if (consumedOvercast) {
			clearFormula(casterId);
			return;
		}
		FormulaState state = FORMULAS.computeIfAbsent(casterId,
				id -> new FormulaState(level, id));
		if (state.level != level) {
			clearFormula(casterId);
			state = new FormulaState(level, casterId);
			FORMULAS.put(casterId, state);
		}
		int gained = isQteSkill(skill)
				? result == QTEResult.PERFECT ? 2 : result == QTEResult.GOOD ? 1 : 0
				: 1;
		if (skill.equals(state.lastSkill))
			gained = 0;
		state.lastSkill = skill;
		state.stage = stage;
		state.expiresAt = level.getGameTime() + FORMULA_DURATION[stage];
		if (gained > 0)
			state.runes = Mth.clamp(state.runes + gained, 0, 3);
		refreshFormulaEffect(state, caster);
	}

	private static void refreshFormulaEffect(FormulaState state, Entity caster) {
		Entity old = state.effectId == null ? null : state.level.getEntity(state.effectId);
		if (old != null)
			old.discard();
		if (state.runes <= 0) {
			state.effectId = null;
			return;
		}
		ArcaneVfxEntity effect = spawn(state.level,
				caster.position().add(0.0D, caster.getBbHeight() * 0.52D, 0.0D),
				ArcaneVfxEntity.FORMULA_RUNES, state.stage, state.runes,
				1.0F, (int) Math.max(4L, state.expiresAt - state.level.getGameTime()),
				caster.getYRot(), 0.0F, caster, null, OrbOfAvariceManager.isHeldBy(caster),
				state.runes >= 3);
		state.effectId = effect.getUUID();
		if (state.runes >= 3)
			play(state.level, caster.position(), SoundEvents.ENCHANTMENT_TABLE_USE, 0.72F, 1.75F);
	}

	private static void clearFormula(UUID casterId) {
		FormulaState removed = FORMULAS.remove(casterId);
		if (removed == null || removed.effectId == null)
			return;
		Entity effect = removed.level.getEntity(removed.effectId);
		if (effect != null)
			effect.discard();
	}

	private static void setAnchor(ServerLevel level, Entity caster, Vec3 position, int stage) {
		AnchorState previous = ANCHORS.remove(caster.getUUID());
		if (previous != null)
			previous.discardEffect();
		int duration = 100 + stage * 10;
		ArcaneVfxEntity effect = spawn(level, position.add(0.0D, 0.08D, 0.0D),
				ArcaneVfxEntity.VECTOR_ANCHOR, stage, 1.1F + stage * 0.12F, 1.0F,
				duration + 5, caster.getYRot(), 0.0F, caster, null,
				OrbOfAvariceManager.isHeldBy(caster), false);
		ANCHORS.put(caster.getUUID(), new AnchorState(level, caster.getUUID(), position,
				level.getGameTime() + duration, effect.getUUID()));
	}

	private static boolean recallAnchor(ServerLevel level, Entity caster) {
		AnchorState anchor = ANCHORS.get(caster.getUUID());
		if (anchor == null || anchor.level != level || anchor.expiresAt < level.getGameTime()) {
			if (anchor != null) {
				anchor.discardEffect();
				ANCHORS.remove(caster.getUUID());
			}
			return false;
		}
		AABB moved = caster.getBoundingBox().move(anchor.position.subtract(caster.position()));
		if (!level.noCollision(caster, moved)) {
			message(caster, "The recall anchor is obstructed.");
			return true;
		}
		Vec3 origin = caster.position();
		caster.teleportTo(anchor.position.x, anchor.position.y, anchor.position.z);
		Vec3 link = anchor.position.subtract(origin);
		spawn(level, origin.add(0.0D, caster.getBbHeight() * 0.45D, 0.0D),
				ArcaneVfxEntity.VECTOR_TRAIL, outputStage(caster), 0.8F,
				(float) link.length(), 18, yawFor(link), pitchFor(link), caster, null,
				OrbOfAvariceManager.isHeldBy(caster), true);
		anchor.discardEffect();
		ANCHORS.remove(caster.getUUID());
		CooldownManager.set(caster, VECTOR_STEP, 30);
		play(level, anchor.position, SoundEvents.ENDERMAN_TELEPORT, 0.8F, 1.7F);
		return true;
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		tickList(ACTIVE_BOLTS);
		tickList(ACTIVE_POLARITIES);
		tickList(ACTIVE_RENDS);
		tickList(ACTIVE_CONVERGENCES);
		tickList(DELAYED_BURSTS);
		tickFormulaStates();
		tickAnchors();
		tickRelays();
		tickArsenals();
	}

	private static <T extends ActiveCast> void tickList(List<T> casts) {
		Iterator<T> iterator = casts.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().tick())
				iterator.remove();
		}
	}

	private static void tickFormulaStates() {
		Iterator<Map.Entry<UUID, FormulaState>> iterator = FORMULAS.entrySet().iterator();
		while (iterator.hasNext()) {
			FormulaState state = iterator.next().getValue();
			Entity caster = state.level.getEntity(state.casterId);
			if (caster == null || !caster.isAlive() || state.expiresAt < state.level.getGameTime()) {
				if (state.effectId != null) {
					Entity effect = state.level.getEntity(state.effectId);
					if (effect != null)
						effect.discard();
				}
				iterator.remove();
			}
		}
	}

	private static void tickAnchors() {
		Iterator<Map.Entry<UUID, AnchorState>> iterator = ANCHORS.entrySet().iterator();
		while (iterator.hasNext()) {
			AnchorState state = iterator.next().getValue();
			Entity caster = state.level.getEntity(state.casterId);
			if (caster == null || !caster.isAlive() || state.expiresAt < state.level.getGameTime()) {
				state.discardEffect();
				iterator.remove();
			}
		}
	}

	private static void tickRelays() {
		Iterator<Map.Entry<UUID, RelayState>> iterator = RELAYS.entrySet().iterator();
		while (iterator.hasNext()) {
			RelayState state = iterator.next().getValue();
			Entity caster = state.level.getEntity(state.casterId);
			if (caster == null || !caster.isAlive() || state.expiresAt < state.level.getGameTime()) {
				state.discardEffects();
				iterator.remove();
			}
		}
	}

	private static void tickArsenals() {
		Iterator<Map.Entry<UUID, ArsenalState>> iterator = ARSENALS.entrySet().iterator();
		while (iterator.hasNext()) {
			ArsenalState state = iterator.next().getValue();
			Entity caster = state.level.getEntity(state.casterId);
			if (caster == null || !caster.isAlive() || state.expiresAt < state.level.getGameTime()
					|| state.remaining <= 0) {
				state.discardEffect();
				iterator.remove();
				continue;
			}
			state.tick(caster);
		}
	}

	private static RelayState activeRelay(ServerLevel level, Entity caster) {
		RelayState relay = RELAYS.get(caster.getUUID());
		if (relay == null || relay.level != level || relay.expiresAt < level.getGameTime())
			return null;
		return relay;
	}

	private static Vec3 routedOrigin(ServerLevel level, Entity caster, Vec3 direction) {
		RelayState relay = activeRelay(level, caster);
		return relay == null ? caster.getEyePosition().add(direction.scale(0.75D))
				: relay.exit.add(direction.scale(0.55D));
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
						&& MageCombatHelper.isValidTarget(caster, living), start.distanceToSqr(clipped));
		return entityHit == null ? clipped : entityHit.getEntity().getBoundingBox().getCenter();
	}

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

	private static LivingEntity nearestTarget(ServerLevel level, Entity caster, Vec3 center,
			double radius, Set<UUID> excluded) {
		return level.getEntitiesOfClass(LivingEntity.class,
				new AABB(center, center).inflate(radius),
				candidate -> MageCombatHelper.isValidTarget(caster, candidate)
						&& (excluded == null || !excluded.contains(candidate.getUUID())))
				.stream().min(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)))
				.orElse(null);
	}

	private static Vec3 safeTeleportPoint(ServerLevel level, Entity caster, Vec3 direction,
			double distance) {
		Vec3 origin = caster.position();
		Vec3 desired = origin.add(direction.scale(distance));
		BlockHitResult hit = level.clip(new ClipContext(
				origin.add(0.0D, caster.getBbHeight() * 0.5D, 0.0D),
				desired.add(0.0D, caster.getBbHeight() * 0.5D, 0.0D),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
		if (hit.getType() != HitResult.Type.MISS)
			desired = hit.getLocation().subtract(direction.scale(0.65D))
					.subtract(0.0D, caster.getBbHeight() * 0.5D, 0.0D);
		Vec3 delta = desired.subtract(origin);
		for (int attempt = 0; attempt <= 12; attempt++) {
			Vec3 candidate = desired.subtract(safeDirection(delta).scale(attempt * 0.35D));
			AABB moved = caster.getBoundingBox().move(candidate.subtract(origin));
			if (level.noCollision(caster, moved))
				return candidate;
		}
		return origin;
	}

	private static Vec3 groundPoint(ServerLevel level, Vec3 point) {
		int startY = Mth.floor(point.y + 3.0D);
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(Mth.floor(point.x), startY,
				Mth.floor(point.z));
		for (int offset = 0; offset < 18; offset++) {
			cursor.setY(startY - offset);
			BlockState state = level.getBlockState(cursor);
			if (!state.isAir() && !state.getCollisionShape(level, cursor).isEmpty())
				return new Vec3(point.x, cursor.getY() + 1.015D, point.z);
		}
		return point;
	}

	private static Vec3 groundBelow(ServerLevel level, Vec3 point) {
		return groundPoint(level, point.add(0.0D, 2.0D, 0.0D));
	}

	private static void damageArea(ServerLevel level, Entity caster, Vec3 center, double radius,
			float damage, double push) {
		AABB area = new AABB(center, center).inflate(radius);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
				candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
			MageCombatHelper.hurt(level, caster, target, damage);
			if (push > 0.0D) {
				Vec3 direction = safeDirection(target.getBoundingBox().getCenter().subtract(center));
				target.setDeltaMovement(target.getDeltaMovement().add(direction.scale(push
						* controlFactor(caster, target))));
			}
		}
	}

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

	private static ArcaneVfxEntity spawn(ServerLevel level, Vec3 position, int style, int stage,
			float scale, float length, int lifetime, float yaw, float pitch, Entity owner,
			Entity target, boolean orb, boolean overcast) {
		return ArcaneVfxEntity.spawn(level, position, style, stage, scale, length, lifetime,
				yaw, pitch, owner, target, orb, overcast);
	}

	private static Vec3 safeDirection(Vec3 direction) {
		return direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
	}

	private static Vec3 rotateYaw(Vec3 direction, float degrees) {
		double radians = degrees * Mth.DEG_TO_RAD;
		double cosine = Math.cos(radians);
		double sine = Math.sin(radians);
		return safeDirection(new Vec3(direction.x * cosine - direction.z * sine,
				direction.y, direction.x * sine + direction.z * cosine));
	}

	private static float yawFor(Vec3 direction) {
		return (float) (Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG);
	}

	private static float pitchFor(Vec3 direction) {
		return (float) (-Mth.atan2(direction.y,
				Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * Mth.RAD_TO_DEG);
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

	private static final class BoltCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID effectId;
		private final int stage;
		private final float damage;
		private final double speed;
		private final double radius;
		private final int maxHits;
		private final boolean orb;
		private final int style;
		private final boolean overcast;
		private final Set<UUID> hitTargets = new HashSet<>();
		private Vec3 position;
		private Vec3 direction;
		private double remaining;
		private int delay;

		private BoltCast(ServerLevel level, UUID casterId, UUID effectId, int stage,
				float damage, Vec3 position, Vec3 direction, double remaining, double speed,
				double radius, int maxHits, int delay, boolean orb, int style,
				boolean overcast) {
			this.level = level;
			this.casterId = casterId;
			this.effectId = effectId;
			this.stage = stage;
			this.damage = damage;
			this.position = position;
			this.direction = direction;
			this.remaining = remaining;
			this.speed = speed;
			this.radius = radius;
			this.maxHits = maxHits;
			this.delay = delay;
			this.orb = orb;
			this.style = style;
			this.overcast = overcast;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			Entity rawEffect = level.getEntity(effectId);
			if (caster == null || !caster.isAlive() || !(rawEffect instanceof ArcaneVfxEntity effect))
				return true;
			if (delay-- > 0)
				return false;

			if (stage >= 2 || style == ArcaneVfxEntity.ASTRAL_BLADE) {
				LivingEntity correction = nearestTarget(level, caster, position,
						style == ArcaneVfxEntity.ASTRAL_BLADE ? 14.0D : 6.0D + stage,
						hitTargets);
				if (correction != null) {
					Vec3 wanted = safeDirection(correction.getBoundingBox().getCenter().subtract(position));
					double strength = style == ArcaneVfxEntity.ASTRAL_BLADE ? 0.28D
							: 0.055D + stage * 0.018D;
					direction = safeDirection(direction.scale(1.0D - strength).add(wanted.scale(strength)));
				}
			}

			double travel = Math.min(speed, remaining);
			Vec3 intended = position.add(direction.scale(travel));
			BlockHitResult blockHit = level.clip(new ClipContext(position, intended,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			boolean struckBlock = blockHit.getType() != HitResult.Type.MISS;
			Vec3 next = struckBlock ? blockHit.getLocation() : intended;
			AABB area = new AABB(position, next).inflate(radius);
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
					candidate -> MageCombatHelper.isValidTarget(caster, candidate)
							&& !hitTargets.contains(candidate.getUUID()));
			targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(position)));
			for (LivingEntity target : targets) {
				if (distanceToSegmentSqr(target.getBoundingBox().getCenter(), position, next)
						> radius * radius + target.getBbWidth() * target.getBbWidth() * 0.25D)
					continue;
				if (MageCombatHelper.hurt(level, caster, target, damage)) {
					hitTargets.add(target.getUUID());
					spawn(level, target.getBoundingBox().getCenter(), ArcaneVfxEntity.AETHER_IMPACT,
							stage, (float) (0.75D + radius * 1.5D), 1.0F, 14,
							0.0F, 0.0F, caster, target, orb, overcast);
					if (hitTargets.size() >= maxHits) {
						effect.discard();
						return true;
					}
					LivingEntity ricochet = nearestTarget(level, caster,
							target.getBoundingBox().getCenter(), 8.0D + stage * 1.5D, hitTargets);
					if (ricochet != null)
						direction = safeDirection(ricochet.getBoundingBox().getCenter()
								.subtract(target.getBoundingBox().getCenter()));
				}
			}

			position = next;
			remaining -= travel;
			effect.setVisualPose(position, yawFor(direction), pitchFor(direction));
			if (struckBlock || remaining <= 0.01D) {
				spawn(level, position, ArcaneVfxEntity.AETHER_IMPACT, stage,
						(float) (0.65D + radius * 1.35D), 1.0F, 12,
						0.0F, 0.0F, caster, null, orb, overcast);
				effect.discard();
				return true;
			}
			return false;
		}
	}

	private static final class PolarityCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID effectId;
		private final int stage;
		private final Vec3 center;
		private final float radius;
		private final int duration;
		private final float damage;
		private final boolean baseRepel;
		private final boolean overcast;
		private int age;

		private PolarityCast(ServerLevel level, UUID casterId, UUID effectId, int stage,
				Vec3 center, float radius, int duration, float damage, boolean baseRepel,
				boolean overcast) {
			this.level = level;
			this.casterId = casterId;
			this.effectId = effectId;
			this.stage = stage;
			this.center = center;
			this.radius = radius;
			this.duration = duration;
			this.damage = damage;
			this.baseRepel = baseRepel;
			this.overcast = overcast;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			Entity effect = level.getEntity(effectId);
			if (caster == null || !caster.isAlive() || effect == null)
				return true;
			age++;
			if ((age & 1) == 0) {
				boolean repel = overcast && age > duration * 0.70D ? true : baseRepel;
				AABB area = new AABB(center, center).inflate(radius);
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
						candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
					Vec3 delta = center.subtract(target.getBoundingBox().getCenter());
					double distance = Math.max(0.5D, delta.length());
					Vec3 force = safeDirection(repel ? delta.scale(-1.0D) : delta)
							.scale((0.10D + stage * 0.018D) * (1.0D - Math.min(0.82D, distance / radius))
									* controlFactor(caster, target));
					if (!repel && stage >= 3)
						force = force.add(0.0D, 0.035D * controlFactor(caster, target), 0.0D);
					target.setDeltaMovement(target.getDeltaMovement().scale(0.82D).add(force));
					if (stage >= 4)
						target.fallDistance = 0.0F;
				}
				if (stage >= 2) {
					for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area,
							candidate -> candidate.isAlive())) {
						Entity owner = projectile.getOwner();
						if (owner != null && MageCombatHelper.areAllied(caster, owner))
							continue;
						Vec3 delta = center.subtract(projectile.position());
						Vec3 force = safeDirection(repel ? delta.scale(-1.0D) : delta)
								.scale(0.065D + stage * 0.012D);
						projectile.setDeltaMovement(projectile.getDeltaMovement().add(force));
						projectile.hurtMarked = true;
					}
				}
			}
			if (age % 10 == 0)
				damageArea(level, caster, center, radius, damage * 0.16F,
						baseRepel ? 0.08D : 0.0D);
			if (age >= duration) {
				if (overcast) {
					damageArea(level, caster, center, radius * 1.08D, damage * 0.85F, 0.45D);
					spawn(level, center, ArcaneVfxEntity.ZERO_POINT, stage, radius,
							radius, 20, 0.0F, 0.0F, caster, null,
							OrbOfAvariceManager.isHeldBy(caster), true);
				}
				effect.discard();
				return true;
			}
			return false;
		}
	}

	private static final class RendCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID effectId;
		private final int stage;
		private final Vec3 direction;
		private final double speed;
		private final float width;
		private final float damage;
		private final boolean orb;
		private final boolean overcast;
		private final Set<UUID> hitTargets = new HashSet<>();
		private Vec3 position;
		private double remaining;
		private int delay;

		private RendCast(ServerLevel level, UUID casterId, UUID effectId, int stage,
				Vec3 position, Vec3 direction, double remaining, double speed, float width,
				float damage, int delay, boolean orb, boolean overcast) {
			this.level = level;
			this.casterId = casterId;
			this.effectId = effectId;
			this.stage = stage;
			this.position = position;
			this.direction = direction;
			this.remaining = remaining;
			this.speed = speed;
			this.width = width;
			this.damage = damage;
			this.delay = delay;
			this.orb = orb;
			this.overcast = overcast;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			Entity rawEffect = level.getEntity(effectId);
			if (caster == null || !caster.isAlive() || !(rawEffect instanceof ArcaneVfxEntity effect))
				return true;
			if (delay-- > 0)
				return false;
			double travel = Math.min(speed, remaining);
			Vec3 next = position.add(direction.scale(travel));
			AABB area = new AABB(position, next).inflate(width * 0.72D, width * 0.62D,
					width * 0.72D);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
					candidate -> MageCombatHelper.isValidTarget(caster, candidate)
							&& !hitTargets.contains(candidate.getUUID()))) {
				if (distanceToSegmentSqr(target.getBoundingBox().getCenter(), position, next)
						> width * width + target.getBbWidth() * target.getBbWidth() * 0.25D)
					continue;
				if (MageCombatHelper.hurt(level, caster, target, damage)) {
					hitTargets.add(target.getUUID());
					if (stage >= 4)
						target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 18,
								stage >= 5 ? 2 : 1, false, false));
				}
			}
			position = next;
			remaining -= travel;
			effect.setVisualPose(position, yawFor(direction), pitchFor(direction));
			if (remaining <= 0.01D) {
				if (stage >= 3) {
					spawn(level, position, ArcaneVfxEntity.SPATIAL_SCAR, stage,
							width * 1.35F, width * 2.4F, 28, yawFor(direction),
							pitchFor(direction), caster, null, orb, overcast);
					DELAYED_BURSTS.add(new DelayedBurst(level, casterId, position,
							width * 1.45D, damage * (overcast ? 0.55F : 0.34F), 12,
							stage, orb, overcast));
				}
				effect.discard();
				return true;
			}
			return false;
		}
	}

	private static final class ConvergenceCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID groundEffectId;
		private final UUID skyEffectId;
		private final int stage;
		private final Vec3 center;
		private final float radius;
		private final int duration;
		private final float damage;
		private final boolean orb;
		private final boolean overcast;
		private int age;

		private ConvergenceCast(ServerLevel level, UUID casterId, UUID groundEffectId,
				UUID skyEffectId, int stage, Vec3 center, float radius, int duration,
				float damage, boolean orb, boolean overcast) {
			this.level = level;
			this.casterId = casterId;
			this.groundEffectId = groundEffectId;
			this.skyEffectId = skyEffectId;
			this.stage = stage;
			this.center = center;
			this.radius = radius;
			this.duration = duration;
			this.damage = damage;
			this.orb = orb;
			this.overcast = overcast;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			Entity ground = level.getEntity(groundEffectId);
			if (caster == null || !caster.isAlive() || ground == null)
				return true;
			age++;
			AABB area = new AABB(center, center).inflate(radius, radius + 8.0D, radius);
			if ((age & 1) == 0) {
				for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
						candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
					Vec3 delta = center.add(0.0D, 0.35D, 0.0D)
							.subtract(target.getBoundingBox().getCenter());
					double factor = controlFactor(caster, target);
					Vec3 pull = safeDirection(delta).scale((0.055D + stage * 0.009D) * factor);
					if (stage >= 5 || overcast)
						pull = pull.add(0.0D, -0.035D * factor, 0.0D);
					target.setDeltaMovement(target.getDeltaMovement().scale(0.88D).add(pull));
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 8,
							stage >= 4 ? 2 : 1, false, false));
				}
				if (stage >= 3) {
					for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area,
							candidate -> candidate.isAlive())) {
						Entity owner = projectile.getOwner();
						if (owner != null && MageCombatHelper.areAllied(caster, owner))
							continue;
						Vec3 curve = safeDirection(center.subtract(projectile.position()))
								.scale(0.045D + stage * 0.008D);
						projectile.setDeltaMovement(projectile.getDeltaMovement().add(curve));
						projectile.hurtMarked = true;
					}
				}
			}
			if (age % 12 == 0) {
				damageArea(level, caster, center, radius, damage * 0.075F, 0.0D);
				List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
						candidate -> MageCombatHelper.isValidTarget(caster, candidate));
				candidates.sort(Comparator.comparingDouble(candidate -> candidate.distanceToSqr(center)));
				int strikes = Math.min(stage >= 4 ? 4 : 2, candidates.size());
				for (int i = 0; i < strikes; i++) {
					LivingEntity target = candidates.get(i);
					MageCombatHelper.hurt(level, caster, target, damage * 0.055F);
					spawn(level, target.getBoundingBox().getCenter(), ArcaneVfxEntity.AETHER_IMPACT,
							stage, 1.1F, 1.0F, 12, 0.0F, 0.0F, caster, target,
							orb, overcast);
				}
			}
			if (age >= duration) {
				damageArea(level, caster, center, radius * 1.08D,
						damage * (overcast ? 0.92F : 0.72F), 0.18D);
				spawn(level, center.add(0.0D, 0.35D, 0.0D), ArcaneVfxEntity.ZERO_POINT,
						stage, radius * 0.92F, radius, 28, 0.0F, 0.0F, caster, null,
						orb, overcast);
				play(level, center, SoundEvents.GENERIC_EXPLODE, 1.2F, 0.72F);
				ground.discard();
				if (skyEffectId != null) {
					Entity sky = level.getEntity(skyEffectId);
					if (sky != null)
						sky.discard();
				}
				return true;
			}
			return false;
		}
	}

	private static final class DelayedBurst implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final Vec3 center;
		private final double radius;
		private final float damage;
		private final int stage;
		private final boolean orb;
		private final boolean overcast;
		private int delay;

		private DelayedBurst(ServerLevel level, UUID casterId, Vec3 center, double radius,
				float damage, int delay, int stage, boolean orb, boolean overcast) {
			this.level = level;
			this.casterId = casterId;
			this.center = center;
			this.radius = radius;
			this.damage = damage;
			this.delay = delay;
			this.stage = stage;
			this.orb = orb;
			this.overcast = overcast;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (caster == null || !caster.isAlive())
				return true;
			if (--delay > 0)
				return false;
			damageArea(level, caster, center, radius, damage, 0.12D);
			spawn(level, center, ArcaneVfxEntity.ZERO_POINT, stage, (float) radius,
					(float) radius, 18, 0.0F, 0.0F, caster, null, orb, overcast);
			play(level, center, SoundEvents.AMETHYST_BLOCK_BREAK, 0.9F, 0.72F);
			return true;
		}
	}

	private static final class FormulaState {
		private final ServerLevel level;
		private final UUID casterId;
		private int runes;
		private int stage = 1;
		private long expiresAt;
		private String lastSkill = "";
		private UUID effectId;

		private FormulaState(ServerLevel level, UUID casterId) {
			this.level = level;
			this.casterId = casterId;
		}
	}

	private static final class AnchorState {
		private final ServerLevel level;
		private final UUID casterId;
		private final Vec3 position;
		private final long expiresAt;
		private final UUID effectId;

		private AnchorState(ServerLevel level, UUID casterId, Vec3 position, long expiresAt,
				UUID effectId) {
			this.level = level;
			this.casterId = casterId;
			this.position = position;
			this.expiresAt = expiresAt;
			this.effectId = effectId;
		}

		private void discardEffect() {
			Entity effect = level.getEntity(effectId);
			if (effect != null)
				effect.discard();
		}
	}

	private static final class RelayState {
		private final ServerLevel level;
		private final UUID casterId;
		private final Vec3 entry;
		private final Vec3 exit;
		private final long expiresAt;
		private final int branches;
		private final List<UUID> effectIds;

		private RelayState(ServerLevel level, UUID casterId, Vec3 entry, Vec3 exit,
				long expiresAt, int branches, List<UUID> effectIds) {
			this.level = level;
			this.casterId = casterId;
			this.entry = entry;
			this.exit = exit;
			this.expiresAt = expiresAt;
			this.branches = branches;
			this.effectIds = List.copyOf(effectIds);
		}

		private void discardEffects() {
			for (UUID id : effectIds) {
				Entity effect = level.getEntity(id);
				if (effect != null)
					effect.discard();
			}
		}
	}

	private static final class ArsenalState {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID effectId;
		private final int stage;
		private final int originalBlades;
		private final float totalDamage;
		private final long expiresAt;
		private final boolean orb;
		private final boolean overcast;
		private int remaining;
		private int age;

		private ArsenalState(ServerLevel level, UUID casterId, UUID effectId, int stage,
				int blades, float totalDamage, long expiresAt, boolean orb, boolean overcast) {
			this.level = level;
			this.casterId = casterId;
			this.effectId = effectId;
			this.stage = stage;
			this.originalBlades = blades;
			this.remaining = blades;
			this.totalDamage = totalDamage;
			this.expiresAt = expiresAt;
			this.orb = orb;
			this.overcast = overcast;
		}

		private void tick(Entity caster) {
			age++;
			int interval = Math.max(12, 28 - stage * 2);
			if (age % interval != 0)
				return;
			LivingEntity target = findLookTarget(level, caster, 24.0D + stage * 3.0D);
			if (target == null)
				target = nearestTarget(level, caster, caster.position(), 18.0D + stage * 2.0D, null);
			if (target != null)
				launchOne(caster, target, 0);
		}

		private void releaseAll(Entity caster) {
			LivingEntity primary = findLookTarget(level, caster, 30.0D + stage * 3.0D);
			for (int i = 0; remaining > 0; i++) {
				LivingEntity target = primary;
				if (stage >= 3 && i > 0) {
					LivingEntity alternate = nearestTarget(level, caster, caster.position(),
							22.0D + stage * 2.0D, null);
					if (alternate != null)
						target = alternate;
				}
				launchOne(caster, target, i);
			}
			if (overcast && primary != null) {
				DELAYED_BURSTS.add(new DelayedBurst(level, casterId,
						primary.getBoundingBox().getCenter(), 3.0D + stage * 0.4D,
						totalDamage * 0.38F, 14, stage, orb, true));
			}
			discardEffect();
		}

		private void launchOne(Entity caster, LivingEntity target, int delay) {
			if (remaining <= 0)
				return;
			Vec3 origin = caster.getBoundingBox().getCenter().add(0.0D,
					0.35D + (remaining % 3) * 0.24D, 0.0D);
			Vec3 direction = target == null ? safeDirection(caster.getLookAngle())
					: safeDirection(target.getBoundingBox().getCenter().subtract(origin));
			float bladeDamage = totalDamage / Math.max(1, originalBlades);
			spawnBolt(level, caster, origin, direction, stage, bladeDamage, overcast,
					delay, ArcaneVfxEntity.ASTRAL_BLADE, 0.34D + stage * 0.035D,
					2.65D + stage * 0.14D, 28.0D + stage * 4.0D,
					stage >= 5 ? 2 : 1);
			remaining--;
			Entity effect = level.getEntity(effectId);
			if (effect instanceof ArcaneVfxEntity arcane)
				arcane.setScale(Math.max(0.04F, remaining));
		}

		private void discardEffect() {
			Entity effect = level.getEntity(effectId);
			if (effect != null)
				effect.discard();
		}
	}

	private static double distanceToSegmentSqr(Vec3 point, Vec3 start, Vec3 end) {
		Vec3 segment = end.subtract(start);
		double lengthSqr = segment.lengthSqr();
		if (lengthSqr < 1.0E-8D)
			return point.distanceToSqr(start);
		double t = Mth.clamp(point.subtract(start).dot(segment) / lengthSqr, 0.0D, 1.0D);
		return point.distanceToSqr(start.add(segment.scale(t)));
	}
}
