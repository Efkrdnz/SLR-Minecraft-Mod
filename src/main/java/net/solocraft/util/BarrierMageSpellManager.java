package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.BarrierVfxEntity;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.player.Player;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative mechanics for the staged Barrier Mage spell set. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class BarrierMageSpellManager {
	public static final String FRACTURE_BOLT = "Fracture Bolt";
	public static final String PRISM_RAMPART = "Prism Rampart";
	public static final String REPULSION_FRAME = "Repulsion Frame";
	public static final String SEALING_PRISM = "Sealing Prism";
	public static final String MIRROR_WARD = "Mirror Ward";
	public static final String RESONANT_COLLAPSE = "Resonant Collapse";
	public static final String ABSOLUTE_BASTION = "Absolute Bastion";

	public static final int NORMAL_PRIMARY = 0x62DFFF;
	public static final int NORMAL_SECONDARY = 0xF2FFFF;
	public static final int ORB_PRIMARY = 0x2448FF;
	public static final int ORB_SECONDARY = 0xB31552;

	public static final Set<String> BARRIER_SKILLS = Set.of(FRACTURE_BOLT, PRISM_RAMPART,
			REPULSION_FRAME, SEALING_PRISM, MIRROR_WARD, RESONANT_COLLAPSE, ABSOLUTE_BASTION);
	public static final Set<String> QTE_SKILLS = Set.of(SEALING_PRISM, RESONANT_COLLAPSE,
			ABSOLUTE_BASTION);
	public static final Set<String> INSTANT_SKILLS = Set.of(FRACTURE_BOLT, PRISM_RAMPART,
			REPULSION_FRAME, MIRROR_WARD);

	private static final String FRACTURE_PREFIX = "sl_barrier_fracture_";
	private static final String FRACTURE_UNTIL_SUFFIX = "_until";
	private static final double[] COST_MULTIPLIER = {0.0D, 1.0D, 1.10D, 1.20D, 1.30D, 1.40D};
	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(Registries.ENTITY_TYPE,
			new ResourceLocation("soloboss"));

	private static final List<BoltCast> ACTIVE_BOLTS = new ArrayList<>();
	private static final List<RepulsionCast> ACTIVE_REPULSIONS = new ArrayList<>();
	private static final List<PrisonCast> ACTIVE_PRISONS = new ArrayList<>();
	private static final List<MirrorCast> ACTIVE_MIRRORS = new ArrayList<>();
	private static final List<CollapseCast> ACTIVE_COLLAPSES = new ArrayList<>();
	private static final List<BastionCast> ACTIVE_BASTIONS = new ArrayList<>();

	private BarrierMageSpellManager() {
	}

	public static boolean isBarrierSkill(String skill) {
		return BARRIER_SKILLS.contains(skill);
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
			case 1 -> "Facet";
			case 2 -> "Reinforced";
			case 3 -> "Prismatic";
			case 4 -> "Citadel";
			case 5 -> "Absolute";
			default -> "Dormant";
		};
	}

	public static List<Component> tooltip(Entity caster, String skill) {
		int stage = outputStage(caster);
		ArrayList<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
		lines.add(Component.literal(switch (skill) {
			case FRACTURE_BOLT -> "Launch a fast barrier shard that marks enemies with Fracture.";
			case PRISM_RAMPART -> "Raise a breakable wall that intercepts hostile attacks and projectiles.";
			case REPULSION_FRAME -> "Drive a moving barrier frame through enemies and control their position.";
			case SEALING_PRISM -> "Enclose targets inside breakable prisons that consume Fracture for control.";
			case MIRROR_WARD -> "Catch incoming pressure and return it as capped retaliatory shards.";
			case RESONANT_COLLAPSE -> "Collapse constructs and Fracture marks into one controlled detonation.";
			case ABSOLUTE_BASTION -> "Construct a fortress domain that shields allies and corrals enemies.";
			default -> "Barrier magic.";
		}).withStyle(ChatFormatting.GRAY));
		lines.add(Component.literal("Output: " + stageName(stage))
				.withStyle(stage >= 5 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.AQUA));
		if (RESONANT_COLLAPSE.equals(skill)) {
			lines.add(Component.literal("Mana: Dynamic  |  Cooldown: 16.0s")
					.withStyle(ChatFormatting.DARK_GRAY));
			lines.add(Component.literal("Cost rises with projected targets and stored Resonance.")
					.withStyle(ChatFormatting.DARK_AQUA));
		} else {
			lines.add(Component.literal("Mana: " + manaCost(caster, skill, stage, QTEResult.MISS)
					+ "  |  Cooldown: " + String.format("%.1fs", cooldownTicks(skill) / 20.0D))
					.withStyle(ChatFormatting.DARK_GRAY));
		}
		if (OrbOfAvariceManager.isHeldBy(caster))
			lines.add(Component.literal("Orb: hardened construct, amplified output, +50% mana.")
					.withStyle(ChatFormatting.BLUE));
		return lines;
	}

	public static boolean cast(Entity caster, String skill, QTEResult qteResult) {
		if (!(caster.level() instanceof ServerLevel level) || !isBarrierSkill(skill))
			return false;
		if (CooldownManager.isOnCooldown(caster, skill)) {
			message(caster, "Ability on cooldown!");
			return false;
		}

		int stage = outputStage(caster);
		QTEResult result = qteResult == null ? QTEResult.MISS : qteResult;
		List<LivingEntity> prisonTargets = List.of();
		CollapseContext collapse = null;
		if (SEALING_PRISM.equals(skill)) {
			prisonTargets = findPrisonTargets(level, caster, stage);
			if (prisonTargets.isEmpty()) {
				message(caster, "No valid target can be sealed.");
				return false;
			}
		} else if (RESONANT_COLLAPSE.equals(skill)) {
			collapse = findCollapseContext(level, caster, stage);
			if (collapse.isEmpty()) {
				message(caster, "No construct or Fractured target can resonate.");
				return false;
			}
		}

		int cost = collapse == null ? manaCost(caster, skill, stage, result)
				: collapseManaCost(caster, stage, result, collapse);
		SololevelingModVariables.PlayerVariables data = variables(caster);
		if (!(caster instanceof Player player && player.isCreative()) && data.MP < cost) {
			message(caster, "Not enough MP! Need " + cost + ".");
			return false;
		}

		if (cost > 0)
			deductMana(caster, cost);
		CooldownManager.set(caster, skill, cooldownTicks(skill));
		CooldownManager.set(caster, "mana_refresh", 40);

		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		switch (skill) {
			case FRACTURE_BOLT -> startBolt(level, caster, stage,
					(float) (1.5D + intelligence * 0.035D));
			case PRISM_RAMPART -> startRampart(level, caster, stage);
			case REPULSION_FRAME -> startRepulsion(level, caster, stage,
					(float) (3.0D + intelligence * 0.05D));
			case SEALING_PRISM -> startPrisons(level, caster, stage, prisonTargets,
					(float) (3.0D + intelligence * 0.045D));
			case MIRROR_WARD -> startMirror(level, caster, stage);
			case RESONANT_COLLAPSE -> startCollapse(level, caster, stage, collapse);
			case ABSOLUTE_BASTION -> startBastion(level, caster, stage,
					(float) (1.0D + intelligence * 0.012D),
					(float) (6.0D + intelligence * 0.06D));
			default -> {
				return false;
			}
		}
		return true;
	}

	public static boolean castNpc(Entity caster, String skill) {
		if (!(caster.level() instanceof ServerLevel level) || !isBarrierSkill(skill)
				|| CooldownManager.isOnCooldown(caster, skill))
			return false;
		Entity target = caster instanceof Mob mob ? mob.getTarget() : null;
		if (target == null && !MIRROR_WARD.equals(skill) && !PRISM_RAMPART.equals(skill))
			return false;
		int stage = outputStage(caster);
		double intelligence = Math.max(10.0D, MageCombatHelper.intelligence(caster));
		boolean cast = true;
		switch (skill) {
			case FRACTURE_BOLT -> startBolt(level, caster, stage,
					(float) (1.5D + intelligence * 0.035D));
			case PRISM_RAMPART -> startRampart(level, caster, stage);
			case REPULSION_FRAME -> startRepulsion(level, caster, stage,
					(float) (3.0D + intelligence * 0.05D));
			case SEALING_PRISM -> {
				List<LivingEntity> targets = findPrisonTargets(level, caster, stage);
				if (targets.isEmpty())
					cast = false;
				else
					startPrisons(level, caster, stage, targets,
							(float) (3.0D + intelligence * 0.045D));
			}
			case MIRROR_WARD -> startMirror(level, caster, stage);
			case RESONANT_COLLAPSE -> {
				CollapseContext context = findCollapseContext(level, caster, stage);
				if (context.isEmpty())
					cast = false;
				else
					startCollapse(level, caster, stage, context);
			}
			case ABSOLUTE_BASTION -> startBastion(level, caster, stage,
					(float) (1.0D + intelligence * 0.012D),
					(float) (6.0D + intelligence * 0.06D));
			default -> cast = false;
		}
		if (cast)
			CooldownManager.set(caster, skill, cooldownTicks(skill));
		return cast;
	}

	public static int manaCost(Entity caster, String skill, int stage, QTEResult result) {
		if (caster instanceof Player player && player.isCreative())
			return 0;
		double basePercent = switch (skill) {
			case FRACTURE_BOLT -> 0.0035D;
			case PRISM_RAMPART -> 0.04D;
			case REPULSION_FRAME -> 0.0325D;
			case SEALING_PRISM -> 0.06D;
			case MIRROR_WARD -> 0.075D;
			case RESONANT_COLLAPSE -> 0.03D;
			case ABSOLUTE_BASTION -> 0.16D;
			default -> 0.0D;
		};
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		double maximumMana = 1000.0D + intelligence * 100.0D;
		double qte = MageQTEHelper.getManaCostMultiplier(result == null ? QTEResult.MISS : result,
				intelligence);
		return Math.max(0, OrbOfAvariceManager.adjustManaCost(caster, maximumMana * basePercent
				* COST_MULTIPLIER[Mth.clamp(stage, 1, 5)] * qte));
	}

	private static int collapseManaCost(Entity caster, int stage, QTEResult result,
			CollapseContext context) {
		if (caster instanceof Player player && player.isCreative())
			return 0;
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		double maximumMana = 1000.0D + intelligence * 100.0D;
		double baseDamage = 3.0D + intelligence * 0.05D;
		double stackDamage = 0.75D + intelligence * 0.0125D;
		double projectedDamage = context.targetIds.size() * baseDamage;
		for (int stacks : context.targetStacks.values())
			projectedDamage += stacks * stackDamage;
		double raw = maximumMana * 0.03D * COST_MULTIPLIER[Mth.clamp(stage, 1, 5)]
				+ projectedDamage * 1.5D;
		raw = Math.min(maximumMana * 0.20D, raw);
		raw *= MageQTEHelper.getManaCostMultiplier(result, intelligence);
		return Math.max(1, OrbOfAvariceManager.adjustManaCost(caster, raw));
	}

	private static int cooldownTicks(String skill) {
		return switch (skill) {
			case FRACTURE_BOLT -> 10;
			case PRISM_RAMPART -> 120;
			case REPULSION_FRAME -> 100;
			case SEALING_PRISM -> 240;
			case MIRROR_WARD -> 360;
			case RESONANT_COLLAPSE -> 320;
			case ABSOLUTE_BASTION -> 1000;
			default -> 20;
		};
	}

	private static void startBolt(ServerLevel level, Entity caster, int stage, float damage) {
		Vec3 direction = safeDirection(caster.getLookAngle());
		Vec3 origin = caster.getEyePosition().add(direction.scale(0.75D));
		double speed = 3.5D + stage * 0.28D;
		double range = 24.0D + stage * 5.0D;
		double radius = 0.24D + stage * 0.10D;
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		BarrierVfxEntity effect = spawn(level, origin, BarrierVfxEntity.FRACTURE_BOLT, stage,
				(float) (radius * (stage >= 5 ? 2.3D : 1.0D)),
				(float) (2.8D + stage * 1.2D), (int) Math.ceil(range / speed) + 8,
				yawFor(direction), pitchFor(direction), caster, null, orb, 0.0F, false);
		ACTIVE_BOLTS.add(new BoltCast(level, caster.getUUID(), effect.getUUID(), stage, damage,
				origin, direction, range, speed, radius, orb));
		play(level, origin, SoundEvents.AMETHYST_BLOCK_CHIME, 0.75F, 1.55F);
	}

	private static void startRampart(ServerLevel level, Entity caster, int stage) {
		enforceConstructLimit(level, caster, stage);
		Vec3 center = groundTarget(level, caster, 9.0D + stage * 2.0D);
		float halfWidth = new float[] {0.0F, 1.5F, 2.5F, 3.0F, 3.6F, 5.0F}[stage];
		float height = new float[] {0.0F, 2.5F, 3.0F, 3.5F, 4.0F, 5.4F}[stage];
		int lifetime = 80 + stage * 20;
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		float integrity = constructIntegrity(caster, 24.0D, 0.55D, orb);
		spawn(level, center, BarrierVfxEntity.PRISM_RAMPART, stage, halfWidth, height, lifetime,
				caster.getYRot(), 0.0F, caster, null, orb, integrity, true);
		play(level, center, SoundEvents.AMETHYST_CLUSTER_PLACE, 1.0F, 0.72F + stage * 0.04F);
	}

	private static void startRepulsion(ServerLevel level, Entity caster, int stage, float damage) {
		Vec3 direction = safeDirection(caster.getLookAngle().multiply(1.0D, 0.35D, 1.0D));
		Vec3 origin = caster.position().add(0.0D, 0.15D, 0.0D).add(direction.scale(1.2D));
		double distance = new double[] {0.0D, 6.0D, 8.0D, 10.0D, 12.0D, 14.0D}[stage];
		float halfWidth = 1.1F + stage * 0.42F;
		float height = 2.0F + stage * 0.34F;
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		BarrierVfxEntity effect = spawn(level, origin, BarrierVfxEntity.REPULSION_FRAME, stage,
				halfWidth, height, 24, yawFor(direction), pitchFor(direction), caster, null, orb,
				0.0F, false);
		ACTIVE_REPULSIONS.add(new RepulsionCast(level, caster.getUUID(), effect.getUUID(), stage,
				damage, origin, direction, distance, 1.35D + stage * 0.08D, halfWidth, orb));
		play(level, origin, SoundEvents.IRON_DOOR_CLOSE, 0.7F, 1.55F);
	}

	private static void startPrisons(ServerLevel level, Entity caster, int stage,
			List<LivingEntity> targets, float totalDamage) {
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		for (LivingEntity target : targets) {
			int stacks = consumeFracture(level, caster, target, 3);
			double factor = controlFactor(caster, target);
			int duration = (int) Math.round(new int[] {0, 28, 40, 46, 52, 60}[stage] * factor
					+ stacks * (isBoss(target) ? 2 : 5));
			duration = Math.max(isBoss(target) ? 10 : 14, duration);
			float scale = Math.max(0.85F, target.getBbWidth() * 0.72F + 0.55F);
			float height = Math.max(1.5F, target.getBbHeight() + 0.45F);
			float integrity = constructIntegrity(caster, 12.0D, 0.35D, orb);
			BarrierVfxEntity effect = spawn(level, target.position().add(0.0D, 0.04D, 0.0D),
					BarrierVfxEntity.SEALING_PRISM, stage, scale, height, duration + 8,
					target.getYRot(), 0.0F, caster, target, orb, integrity, true);
			MageCombatHelper.hurt(level, caster, target, totalDamage * 0.40F);
			ACTIVE_PRISONS.add(new PrisonCast(level, caster.getUUID(), target.getUUID(),
					effect.getUUID(), stage, duration, totalDamage * 0.60F,
					target.position(), isBoss(target), target instanceof Player));
		}
		play(level, targets.get(0).position(), SoundEvents.BEACON_ACTIVATE, 0.65F, 1.7F);
	}

	private static void startMirror(ServerLevel level, Entity caster, int stage) {
		for (MirrorCast existing : new ArrayList<>(ACTIVE_MIRRORS)) {
			if (existing.ownerId.equals(caster.getUUID()) && existing.level == level)
				existing.finishRequested = true;
		}
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		float integrity = constructIntegrity(caster, 16.0D, 0.40D, orb);
		int duration = new int[] {0, 20, 25, 30, 35, 45}[stage];
		BarrierVfxEntity effect = spawn(level,
				caster.position().add(0.0D, 0.04D, 0.0D),
				BarrierVfxEntity.MIRROR_WARD, stage, 1.25F + stage * 0.16F,
				Math.max(2.0F, caster.getBbHeight() + 0.7F), duration + 8, caster.getYRot(),
				0.0F, caster, null, orb, integrity, true);
		ACTIVE_MIRRORS.add(new MirrorCast(level, caster.getUUID(), effect.getUUID(), stage,
				duration, orb));
		play(level, caster.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F, 1.35F);
	}

	private static void startCollapse(ServerLevel level, Entity caster, int stage,
			CollapseContext context) {
		for (UUID constructId : context.constructIds) {
			Entity entity = level.getEntity(constructId);
			if (entity instanceof BarrierVfxEntity construct)
				construct.dissolve();
		}
		ACTIVE_COLLAPSES.add(new CollapseCast(level, caster.getUUID(), stage, context));
		play(level, caster.position(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), 1.0F, 1.35F);
	}

	private static void startBastion(ServerLevel level, Entity caster, int stage,
			float pulseDamage, float finalDamage) {
		enforceConstructLimit(level, caster, stage);
		Vec3 center = groundTarget(level, caster, 16.0D + stage * 4.0D);
		float radius = new float[] {0.0F, 7.0F, 9.0F, 12.0F, 15.0F, 20.0F}[stage];
		float height = 5.0F + stage * 2.2F;
		int duration = new int[] {0, 120, 140, 160, 180, 200}[stage];
		boolean orb = OrbOfAvariceManager.isHeldBy(caster);
		float integrity = constructIntegrity(caster, 70.0D, 1.20D, orb);
		BarrierVfxEntity effect = spawn(level, center, BarrierVfxEntity.ABSOLUTE_BASTION,
				stage, radius, height, duration + 10, caster.getYRot(), 0.0F, caster, null, orb,
				integrity, true);
		ACTIVE_BASTIONS.add(new BastionCast(level, caster.getUUID(), effect.getUUID(), stage,
				duration, pulseDamage, finalDamage, radius));
		play(level, center, SoundEvents.BEACON_POWER_SELECT, 1.2F, 0.58F);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		tick(ACTIVE_BOLTS);
		tick(ACTIVE_REPULSIONS);
		tick(ACTIVE_PRISONS);
		tick(ACTIVE_MIRRORS);
		tick(ACTIVE_COLLAPSES);
		tick(ACTIVE_BASTIONS);
	}

	private static <T extends ActiveCast> void tick(List<T> casts) {
		Iterator<T> iterator = casts.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().tick())
				iterator.remove();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onLivingHurt(LivingHurtEvent event) {
		LivingEntity victim = event.getEntity();
		if (!(victim.level() instanceof ServerLevel level) || event.getAmount() <= 0.0F)
			return;
		Vec3 sourcePosition = event.getSource().getSourcePosition();
		if (sourcePosition == null && event.getSource().getDirectEntity() != null)
			sourcePosition = event.getSource().getDirectEntity().getBoundingBox().getCenter();

		if (sourcePosition != null) {
			List<BarrierVfxEntity> constructs = level.getEntitiesOfClass(BarrierVfxEntity.class,
					victim.getBoundingBox().inflate(64.0D), construct -> construct.isActive()
							&& construct.isBlockingConstruct());
			constructs.sort(Comparator.comparingDouble(construct -> construct.distanceToSqr(victim)));
			for (BarrierVfxEntity construct : constructs) {
				Entity owner = construct.getOwnerEntity(level);
				if (owner == null || !MageCombatHelper.areAllied(owner, victim))
					continue;
				boolean blocks = construct.getStyle() == BarrierVfxEntity.ABSOLUTE_BASTION
						? !construct.containsInBastion(sourcePosition)
								&& construct.containsInBastion(victim.getBoundingBox().getCenter())
						: construct.intersectsSegment(sourcePosition, victim.getBoundingBox().getCenter());
				if (!blocks)
					continue;
				float blocked = construct.absorbDamage(event.getAmount());
				applyBlockedAmount(event, blocked);
				if (event.getAmount() <= 0.001F)
					return;
				break;
			}
		}

		for (MirrorCast mirror : ACTIVE_MIRRORS) {
			if (mirror.level != level || !mirror.ownerId.equals(victim.getUUID())
					|| mirror.finishRequested || sourcePosition == null)
				continue;
			BarrierVfxEntity effect = mirror.effect();
			if (effect == null || !effect.isActive() || !mirror.facesSource(victim, sourcePosition))
				continue;
			float requested = event.getAmount() * 0.55F;
			float blocked = effect.absorbDamage(requested);
			if (blocked <= 0.0F)
				continue;
			applyBlockedAmount(event, blocked);
			Entity attacker = event.getSource().getEntity();
			if (attacker == null)
				attacker = event.getSource().getDirectEntity();
			if (attacker != null && attacker != victim)
				mirror.record(attacker.getUUID(), blocked);
			mirror.hits++;
			if (mirror.hits >= mirror.hitLimit() || !effect.isAlive())
				mirror.finishRequested = true;
			return;
		}
	}

	private static void applyBlockedAmount(LivingHurtEvent event, float blocked) {
		if (blocked <= 0.0F)
			return;
		float remaining = Math.max(0.0F, event.getAmount() - blocked);
		event.setAmount(remaining);
		if (remaining <= 0.001F)
			event.setCanceled(true);
	}

	public static void onProjectileBlocked(BarrierVfxEntity construct, Vec3 impact) {
		if (!(construct.level() instanceof ServerLevel level))
			return;
		spawn(level, impact, BarrierVfxEntity.IMPACT, construct.getStage(),
				0.55F + construct.getStage() * 0.11F, 1.0F, 12, construct.getYRot(), 0.0F,
				construct.getOwnerEntity(level), null, construct.isOrbAmplified(), 0.0F, false);
		play(level, impact, SoundEvents.AMETHYST_BLOCK_HIT, 0.65F, 1.65F);
	}

	public static void onConstructBroken(BarrierVfxEntity construct) {
		if (!(construct.level() instanceof ServerLevel level))
			return;
		spawn(level, construct.position(), BarrierVfxEntity.IMPACT, construct.getStage(),
				Math.max(0.7F, construct.getScale() * 0.65F),
				Math.max(1.0F, construct.getLength() * 0.55F), 16, construct.getYRot(), 0.0F,
				construct.getOwnerEntity(level), null, construct.isOrbAmplified(), 0.0F, false);
		play(level, construct.position(), SoundEvents.GLASS_BREAK, 1.0F, 0.75F);
	}

	private static final class BoltCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID effectId;
		private final int stage;
		private final float damage;
		private final Set<UUID> hitTargets = new HashSet<>();
		private final double speed;
		private final double radius;
		private final boolean orb;
		private Vec3 position;
		private final Vec3 direction;
		private double remaining;
		private boolean hitPrimary;

		private BoltCast(ServerLevel level, UUID casterId, UUID effectId, int stage, float damage,
				Vec3 position, Vec3 direction, double remaining, double speed, double radius,
				boolean orb) {
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
			this.orb = orb;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			Entity rawEffect = level.getEntity(effectId);
			if (caster == null || !caster.isAlive() || !(rawEffect instanceof BarrierVfxEntity effect))
				return true;
			double travel = Math.min(speed, remaining);
			Vec3 intended = position.add(direction.scale(travel));
			BlockHitResult blockHit = level.clip(new ClipContext(position, intended,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			boolean struckBlock = blockHit.getType() != HitResult.Type.MISS;
			Vec3 next = struckBlock ? blockHit.getLocation() : intended;
			AABB area = new AABB(position, next).inflate(radius);
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
					target -> MageCombatHelper.isValidTarget(caster, target)
							&& !hitTargets.contains(target.getUUID()));
			targets.sort(Comparator.comparingDouble(target -> target.distanceToSqr(position)));
			int limit = stage <= 2 ? 1 : stage == 3 ? 3 : stage == 4 ? 5 : 8;
			for (LivingEntity target : targets) {
				if (hitTargets.size() >= limit)
					break;
				hitTarget(caster, target);
				if (stage <= 2) {
					if (stage == 2)
						ricochet(caster, target);
					next = target.getBoundingBox().getCenter();
					struckBlock = true;
					break;
				}
			}
			position = next;
			remaining -= position.distanceTo(effect.position());
			effect.setPos(position.x, position.y, position.z);
			if (struckBlock || remaining <= 0.001D || hitTargets.size() >= limit) {
				finish(caster, effect);
				return true;
			}
			return false;
		}

		private void hitTarget(Entity caster, LivingEntity target) {
			hitTargets.add(target.getUUID());
			MageCombatHelper.hurt(level, caster, target, damage);
			addFracture(level, caster, target, stage >= 5 && !hitPrimary ? 2 : 1, stage);
			hitPrimary = true;
			spawn(level, target.getBoundingBox().getCenter(), BarrierVfxEntity.IMPACT, stage,
					0.45F + stage * 0.10F, 1.0F, 11, target.getYRot(), 0.0F,
					caster, target, orb, 0.0F, false);
		}

		private void ricochet(Entity caster, LivingEntity first) {
			List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
					first.getBoundingBox().inflate(4.0D), target -> MageCombatHelper.isValidTarget(caster, target)
							&& target != first && !hitTargets.contains(target.getUUID()));
			nearby.sort(Comparator.comparingDouble(first::distanceToSqr));
			if (nearby.isEmpty())
				return;
			LivingEntity second = nearby.get(0);
			Vec3 start = first.getBoundingBox().getCenter();
			Vec3 end = second.getBoundingBox().getCenter();
			Vec3 delta = end.subtract(start);
			spawn(level, start.add(delta.scale(0.5D)), BarrierVfxEntity.RETURN_SHARD, stage,
					0.18F, (float) delta.length(), 9, yawFor(delta), pitchFor(delta), caster,
					second, orb, 0.0F, false);
			hitTarget(caster, second);
		}

		private void finish(Entity caster, BarrierVfxEntity effect) {
			effect.dissolve();
			if (stage >= 4) {
				float integrity = constructIntegrity(caster, 7.0D, 0.10D, orb);
				spawn(level, position.add(0.0D, -0.65D, 0.0D), BarrierVfxEntity.SHARD_PLATE,
						stage, stage >= 5 ? 1.15F : 0.78F, stage >= 5 ? 2.2F : 1.5F,
						30, yawFor(direction), 0.0F, caster, null, orb, integrity, true);
			}
		}
	}

	private static final class RepulsionCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID effectId;
		private final int stage;
		private final float damage;
		private final Set<UUID> hitTargets = new HashSet<>();
		private final double speed;
		private final float halfWidth;
		private final boolean orb;
		private Vec3 position;
		private Vec3 direction;
		private double remaining;
		private int age;

		private RepulsionCast(ServerLevel level, UUID casterId, UUID effectId, int stage,
				float damage, Vec3 position, Vec3 direction, double remaining, double speed,
				float halfWidth, boolean orb) {
			this.level = level;
			this.casterId = casterId;
			this.effectId = effectId;
			this.stage = stage;
			this.damage = damage;
			this.position = position;
			this.direction = direction;
			this.remaining = remaining;
			this.speed = speed;
			this.halfWidth = halfWidth;
			this.orb = orb;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			Entity rawEffect = level.getEntity(effectId);
			if (caster == null || !caster.isAlive() || !(rawEffect instanceof BarrierVfxEntity effect))
				return true;
			age++;
			if (stage >= 4 && age <= 6) {
				Vec3 aimed = safeDirection(caster.getLookAngle().multiply(1.0D, 0.35D, 1.0D));
				direction = safeDirection(direction.scale(0.68D).add(aimed.scale(0.32D)));
			}
			double travel = Math.min(speed, remaining);
			Vec3 intended = position.add(direction.scale(travel));
			BlockHitResult blockHit = level.clip(new ClipContext(position, intended,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			Vec3 next = blockHit.getType() == HitResult.Type.MISS ? intended : blockHit.getLocation();
			AABB area = new AABB(position, next).inflate(halfWidth, 1.7D + stage * 0.18D,
					halfWidth);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
					candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
				if (!hitTargets.add(target.getUUID()))
					continue;
				MageCombatHelper.hurt(level, caster, target, damage);
				int consumed = stage >= 3 ? consumeFracture(level, caster, target, 1) : 0;
				double factor = controlFactor(caster, target);
				double force = (0.75D + stage * 0.13D) * factor * (orb ? 1.30D : 1.0D);
				target.setDeltaMovement(direction.scale(force).add(0.0D, 0.10D * factor, 0.0D));
				target.hurtMarked = true;
				if (consumed > 0) {
					int duration = isBoss(target) ? 6 : target instanceof Player ? 8 : 12 + stage * 2;
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration,
							isBoss(target) ? 2 : 8, false, false));
				}
			}
			remaining -= position.distanceTo(next);
			position = next;
			effect.setPos(position.x, position.y, position.z);
			effect.setYRot(yawFor(direction));
			effect.setXRot(pitchFor(direction));
			if (blockHit.getType() != HitResult.Type.MISS || remaining <= 0.001D) {
				spawn(level, position, BarrierVfxEntity.IMPACT, stage, halfWidth * 0.85F,
						2.0F, 13, effect.getYRot(), effect.getXRot(), caster, null, orb, 0.0F, false);
				effect.dissolve();
				return true;
			}
			return false;
		}
	}

	private static final class PrisonCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID targetId;
		private final UUID effectId;
		private final int stage;
		private final int duration;
		private final float releaseDamage;
		private final Vec3 anchor;
		private final boolean boss;
		private final boolean player;
		private int age;

		private PrisonCast(ServerLevel level, UUID casterId, UUID targetId, UUID effectId,
				int stage, int duration, float releaseDamage, Vec3 anchor, boolean boss,
				boolean player) {
			this.level = level;
			this.casterId = casterId;
			this.targetId = targetId;
			this.effectId = effectId;
			this.stage = stage;
			this.duration = duration;
			this.releaseDamage = releaseDamage;
			this.anchor = anchor;
			this.boss = boss;
			this.player = player;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			Entity targetEntity = level.getEntity(targetId);
			Entity effectEntity = level.getEntity(effectId);
			if (caster == null || !caster.isAlive() || !(targetEntity instanceof LivingEntity target))
				return finish(null, null, false);
			if (!(effectEntity instanceof BarrierVfxEntity effect) || !effect.isActive())
				return true;
			age++;
			if (boss || player) {
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6,
						boss ? 2 : 4, false, false));
			} else {
				target.setDeltaMovement(0.0D, Math.min(0.0D, target.getDeltaMovement().y), 0.0D);
				target.hurtMarked = true;
				if (stage < 4) {
					double y = target.getY();
					target.setPos(anchor.x, y, anchor.z);
				}
				target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6, 10,
						false, false));
				target.addEffect(new MobEffectInstance(MobEffects.JUMP, 6, 128, false, false));
			}
			if (age >= duration)
				return finish(caster, target, true);
			return false;
		}

		private boolean finish(Entity caster, LivingEntity target, boolean release) {
			Entity rawEffect = level.getEntity(effectId);
			if (rawEffect instanceof BarrierVfxEntity effect)
				effect.dissolve();
			if (release && caster != null && target != null && target.isAlive()) {
				MageCombatHelper.hurt(level, caster, target, releaseDamage);
				spawn(level, target.getBoundingBox().getCenter(), BarrierVfxEntity.RESONANT_COLLAPSE,
						stage, Math.max(1.0F, target.getBbWidth()), target.getBbHeight(), 14,
						target.getYRot(), 0.0F, caster, target,
						OrbOfAvariceManager.isHeldBy(caster), 0.0F, false);
				play(level, target.position(), SoundEvents.GLASS_BREAK, 0.8F, 1.2F);
			}
			return true;
		}
	}

	private static final class MirrorCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID ownerId;
		private final UUID effectId;
		private final int stage;
		private final int duration;
		private final boolean orb;
		private final Map<UUID, Float> storedByAttacker = new HashMap<>();
		private int age;
		private int hits;
		private boolean finishRequested;

		private MirrorCast(ServerLevel level, UUID ownerId, UUID effectId, int stage,
				int duration, boolean orb) {
			this.level = level;
			this.ownerId = ownerId;
			this.effectId = effectId;
			this.stage = stage;
			this.duration = duration;
			this.orb = orb;
		}

		private BarrierVfxEntity effect() {
			Entity entity = level.getEntity(effectId);
			return entity instanceof BarrierVfxEntity effect ? effect : null;
		}

		private int hitLimit() {
			return new int[] {0, 1, 2, 3, 4, 5}[stage];
		}

		private boolean facesSource(LivingEntity owner, Vec3 source) {
			if (stage >= 4)
				return true;
			Vec3 toward = source.subtract(owner.getEyePosition()).multiply(1.0D, 0.0D, 1.0D);
			if (toward.lengthSqr() < 0.001D)
				return true;
			double dot = safeDirection(owner.getLookAngle().multiply(1.0D, 0.0D, 1.0D))
					.dot(toward.normalize());
			return dot >= (stage == 1 ? 0.35D : stage == 2 ? 0.0D : -0.15D);
		}

		private void record(UUID attacker, float blocked) {
			storedByAttacker.merge(attacker, blocked, Float::sum);
		}

		@Override
		public boolean tick() {
			Entity owner = level.getEntity(ownerId);
			BarrierVfxEntity effect = effect();
			if (owner == null || !owner.isAlive()) {
				if (effect != null)
					effect.dissolve();
				return true;
			}
			age++;
			if (finishRequested || age >= duration || effect == null || !effect.isActive()) {
				release(owner, effect);
				return true;
			}
			return false;
		}

		private void release(Entity owner, BarrierVfxEntity effect) {
			if (effect != null)
				effect.dissolve();
			float cap = (float) (4.0D + MageCombatHelper.intelligence(owner) * 0.06D);
			for (Map.Entry<UUID, Float> entry : storedByAttacker.entrySet()) {
				Entity attacker = level.getEntity(entry.getKey());
				if (!(attacker instanceof LivingEntity living) || !living.isAlive()
						|| !MageCombatHelper.isValidTarget(owner, living))
					continue;
				float returned = Math.min(cap, entry.getValue() * 0.50F);
				hurtWithoutOrbDouble(level, owner, living, returned);
				Vec3 start = owner.getBoundingBox().getCenter();
				Vec3 end = living.getBoundingBox().getCenter();
				Vec3 delta = end.subtract(start);
				spawn(level, start.add(delta.scale(0.5D)), BarrierVfxEntity.RETURN_SHARD,
						stage, 0.18F + stage * 0.035F, (float) delta.length(), 12,
						yawFor(delta), pitchFor(delta), owner, living, orb, 0.0F, false);
				spawn(level, end, BarrierVfxEntity.IMPACT, stage, 0.65F, 1.0F, 11,
						living.getYRot(), 0.0F, owner, living, orb, 0.0F, false);
			}
			play(level, owner.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.7F);
		}
	}

	private static final class CollapseCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final List<Vec3> centers;
		private final Set<UUID> markedTargets;
		private final float resonance;
		private final int delay;
		private int age;

		private CollapseCast(ServerLevel level, UUID casterId, int stage,
				CollapseContext context) {
			this.level = level;
			this.casterId = casterId;
			this.stage = stage;
			this.centers = List.copyOf(context.centers);
			this.markedTargets = Set.copyOf(context.targetIds);
			this.resonance = context.resonance;
			this.delay = stage >= 5 ? 10 : stage >= 4 ? 7 : 3;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (caster == null || !caster.isAlive())
				return true;
			age++;
			if (stage >= 4 && age < delay)
				pullTargets(caster);
			if (age < delay)
				return false;
			detonate(caster);
			return true;
		}

		private void pullTargets(Entity caster) {
			for (LivingEntity target : collectTargets(caster)) {
				Vec3 nearest = nearestCenter(target.position());
				Vec3 pull = nearest.subtract(target.position()).multiply(1.0D, 0.15D, 1.0D);
				if (pull.lengthSqr() > 0.05D) {
					double factor = controlFactor(caster, target);
					target.setDeltaMovement(target.getDeltaMovement().scale(0.72D)
							.add(pull.normalize().scale(0.12D * factor)));
					target.hurtMarked = true;
				}
			}
		}

		private void detonate(Entity caster) {
			double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
			float baseDamage = (float) (3.0D + intelligence * 0.05D);
			float stackDamage = (float) (0.75D + intelligence * 0.0125D);
			float resonanceCap = (float) (4.0D + intelligence * 0.045D);
			float resonanceBonus = Math.min(resonanceCap, resonance);
			for (LivingEntity target : collectTargets(caster)) {
				int stacks = getFracture(level, caster, target);
				MageCombatHelper.hurt(level, caster, target, baseDamage + stackDamage * stacks);
				if (resonanceBonus > 0.0F)
					hurtWithoutOrbDouble(level, caster, target, resonanceBonus);
				clearFracture(caster, target);
			}
			boolean orb = OrbOfAvariceManager.isHeldBy(caster);
			if (stage >= 5) {
				Vec3 origin = centers.isEmpty() ? caster.position() : average(centers);
				spawn(level, origin, BarrierVfxEntity.RESONANT_COLLAPSE, stage, 18.0F,
						8.0F, 28, caster.getYRot(), 0.0F, caster, null, orb, 0.0F, false);
			}
			int visualLimit = Math.min(12, centers.size());
			for (int i = 0; i < visualLimit; i++)
				spawn(level, centers.get(i), BarrierVfxEntity.RESONANT_COLLAPSE, stage,
						3.0F + stage * 0.75F, 3.0F + stage, 18, caster.getYRot(), 0.0F,
						caster, null, orb, 0.0F, false);
			play(level, centers.isEmpty() ? caster.position() : centers.get(0),
					SoundEvents.GENERIC_EXPLODE, stage >= 5 ? 1.6F : 0.9F,
					stage >= 5 ? 0.55F : 1.15F);
		}

		private Set<LivingEntity> collectTargets(Entity caster) {
			LinkedHashSet<LivingEntity> result = new LinkedHashSet<>();
			double radius = 3.0D + stage * 0.75D;
			for (Vec3 center : centers)
				result.addAll(level.getEntitiesOfClass(LivingEntity.class,
						new AABB(center, center).inflate(radius),
						target -> MageCombatHelper.isValidTarget(caster, target)));
			for (UUID targetId : markedTargets) {
				Entity entity = level.getEntity(targetId);
				if (entity instanceof LivingEntity living && MageCombatHelper.isValidTarget(caster, living))
					result.add(living);
			}
			return result;
		}

		private Vec3 nearestCenter(Vec3 point) {
			return centers.stream().min(Comparator.comparingDouble(point::distanceToSqr))
					.orElse(point);
		}
	}

	private static final class BastionCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final UUID effectId;
		private final int stage;
		private final int duration;
		private final float pulseDamage;
		private final float finalDamage;
		private final float radius;
		private final Set<UUID> inside = new HashSet<>();
		private int age;

		private BastionCast(ServerLevel level, UUID casterId, UUID effectId, int stage,
				int duration, float pulseDamage, float finalDamage, float radius) {
			this.level = level;
			this.casterId = casterId;
			this.effectId = effectId;
			this.stage = stage;
			this.duration = duration;
			this.pulseDamage = pulseDamage;
			this.finalDamage = finalDamage;
			this.radius = radius;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			Entity rawEffect = level.getEntity(effectId);
			if (caster == null || !caster.isAlive() || !(rawEffect instanceof BarrierVfxEntity effect)
					|| !effect.isActive())
				return true;
			age++;
			if (age % 4 == 0 && stage >= 3)
				trackEntry(caster, effect);
			if (stage >= 2 && age % 40 == 0 && age < duration)
				pulse(caster, effect);
			if (age >= duration) {
				finish(caster, effect);
				return true;
			}
			return false;
		}

		private List<LivingEntity> targets(Entity caster, BarrierVfxEntity effect) {
			return level.getEntitiesOfClass(LivingEntity.class,
					new AABB(effect.position(), effect.position()).inflate(radius, effect.getLength(), radius),
					target -> effect.containsInBastion(target.getBoundingBox().getCenter())
							&& MageCombatHelper.isValidTarget(caster, target));
		}

		private void trackEntry(Entity caster, BarrierVfxEntity effect) {
			Set<UUID> now = new HashSet<>();
			for (LivingEntity target : targets(caster, effect)) {
				now.add(target.getUUID());
				if (!inside.contains(target.getUUID())) {
					addFracture(level, caster, target, 1, stage);
					Vec3 inward = effect.position().subtract(target.position())
							.multiply(1.0D, 0.0D, 1.0D);
					if (inward.lengthSqr() > 0.01D) {
						target.setDeltaMovement(target.getDeltaMovement().add(inward.normalize()
								.scale(0.18D * controlFactor(caster, target))));
						target.hurtMarked = true;
					}
				}
			}
			inside.clear();
			inside.addAll(now);
		}

		private void pulse(Entity caster, BarrierVfxEntity effect) {
			for (LivingEntity target : targets(caster, effect)) {
				MageCombatHelper.hurt(level, caster, target, pulseDamage);
				Vec3 inward = effect.position().subtract(target.position())
						.multiply(1.0D, 0.12D, 1.0D);
				if (inward.lengthSqr() > 0.01D) {
					target.setDeltaMovement(target.getDeltaMovement().scale(0.62D)
							.add(inward.normalize().scale(0.24D * controlFactor(caster, target))));
					target.hurtMarked = true;
				}
				if (stage >= 4 && getFracture(level, caster, target) > 0)
					target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,
							isBoss(target) ? 8 : 14, isBoss(target) ? 2 : 7, false, false));
			}
			spawn(level, effect.position(), BarrierVfxEntity.IMPACT, stage, radius,
					2.5F + stage, 14, effect.getYRot(), 0.0F, caster, null,
					effect.isOrbAmplified(), 0.0F, false);
			play(level, effect.position(), SoundEvents.BEACON_AMBIENT, 0.55F, 1.35F);
		}

		private void finish(Entity caster, BarrierVfxEntity effect) {
			for (LivingEntity target : targets(caster, effect))
				MageCombatHelper.hurt(level, caster, target, finalDamage);
			spawn(level, effect.position(), BarrierVfxEntity.RESONANT_COLLAPSE, stage,
					radius, effect.getLength(), 28, effect.getYRot(), 0.0F, caster, null,
					effect.isOrbAmplified(), 0.0F, false);
			effect.dissolve();
			play(level, effect.position(), SoundEvents.GENERIC_EXPLODE, 1.3F, 0.68F);
		}
	}

	private interface ActiveCast {
		boolean tick();
	}

	private static List<LivingEntity> findPrisonTargets(ServerLevel level, Entity caster,
			int stage) {
		LivingEntity primary = findLookTarget(level, caster, 18.0D + stage * 4.0D);
		if (primary == null)
			return List.of();
		ArrayList<LivingEntity> targets = new ArrayList<>();
		targets.add(primary);
		if (stage < 3)
			return targets;
		int maximum = stage >= 5 ? 6 : 3;
		List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
				primary.getBoundingBox().inflate(stage >= 5 ? 6.0D : 4.5D),
				target -> target != primary && MageCombatHelper.isValidTarget(caster, target)
						&& (stage >= 5 || getFracture(level, caster, target) > 0));
		nearby.sort(Comparator.comparingDouble(primary::distanceToSqr));
		for (LivingEntity target : nearby) {
			if (targets.size() >= maximum)
				break;
			targets.add(target);
		}
		return targets;
	}

	private static LivingEntity findLookTarget(ServerLevel level, Entity caster, double range) {
		Vec3 start = caster.getEyePosition();
		Vec3 end = start.add(safeDirection(caster.getLookAngle()).scale(range));
		HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, caster));
		if (blockHit.getType() != HitResult.Type.MISS)
			end = blockHit.getLocation();
		AABB search = caster.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.2D);
		EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(caster, start, end, search,
				target -> target instanceof LivingEntity living
						&& MageCombatHelper.isValidTarget(caster, living), start.distanceToSqr(end));
		return entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
	}

	private static CollapseContext findCollapseContext(ServerLevel level, Entity caster, int stage) {
		double range = new double[] {0.0D, 12.0D, 16.0D, 24.0D, 32.0D, 40.0D}[stage];
		int constructLimit = stage == 1 ? 1 : stage == 2 ? 2 : Integer.MAX_VALUE;
		int targetLimit = stage == 1 ? 2 : stage == 2 ? 4 : stage == 3 ? 8
				: stage == 4 ? 14 : 24;
		List<BarrierVfxEntity> constructs = level.getEntitiesOfClass(BarrierVfxEntity.class,
				caster.getBoundingBox().inflate(range), construct -> construct.isActive()
						&& construct.getOwnerId().filter(caster.getUUID()::equals).isPresent()
						&& (construct.countsTowardConstructLimit()
								|| construct.getStyle() == BarrierVfxEntity.SHARD_PLATE));
		constructs.sort(Comparator.comparingDouble(caster::distanceToSqr));
		if (constructs.size() > constructLimit)
			constructs = new ArrayList<>(constructs.subList(0, constructLimit));

		List<LivingEntity> marked = level.getEntitiesOfClass(LivingEntity.class,
				caster.getBoundingBox().inflate(range), target -> MageCombatHelper.isValidTarget(caster, target)
						&& getFracture(level, caster, target) > 0);
		marked.sort(Comparator.comparingDouble(caster::distanceToSqr));
		if (marked.size() > targetLimit)
			marked = new ArrayList<>(marked.subList(0, targetLimit));

		ArrayList<UUID> constructIds = new ArrayList<>();
		ArrayList<Vec3> centers = new ArrayList<>();
		float resonance = 0.0F;
		for (BarrierVfxEntity construct : constructs) {
			constructIds.add(construct.getUUID());
			centers.add(construct.position());
			resonance += construct.getResonance();
		}
		LinkedHashSet<UUID> targetIds = new LinkedHashSet<>();
		Map<UUID, Integer> stacks = new HashMap<>();
		for (LivingEntity target : marked) {
			targetIds.add(target.getUUID());
			stacks.put(target.getUUID(), getFracture(level, caster, target));
			if (centers.isEmpty())
				centers.add(target.getBoundingBox().getCenter());
		}
		return new CollapseContext(constructIds, centers, targetIds, stacks, resonance);
	}

	private static void enforceConstructLimit(ServerLevel level, Entity caster, int stage) {
		int maximum = new int[] {0, 1, 1, 2, 2, 3}[stage];
		List<BarrierVfxEntity> constructs = level.getEntitiesOfClass(BarrierVfxEntity.class,
				caster.getBoundingBox().inflate(160.0D), construct -> construct.isActive()
						&& construct.countsTowardConstructLimit()
						&& construct.getOwnerId().filter(caster.getUUID()::equals).isPresent());
		constructs.sort(Comparator.comparingInt((BarrierVfxEntity construct) -> construct.tickCount)
				.reversed());
		while (constructs.size() >= maximum) {
			BarrierVfxEntity oldest = constructs.remove(0);
			oldest.dissolve();
		}
	}

	private static int addFracture(ServerLevel level, Entity caster, LivingEntity target,
			int amount, int stage) {
		String key = fractureKey(caster);
		int stacks = Mth.clamp(getFracture(level, caster, target) + amount, 0, 3);
		target.getPersistentData().putInt(key, stacks);
		target.getPersistentData().putLong(key + FRACTURE_UNTIL_SUFFIX, level.getGameTime() + 160L);
		spawn(level, target.getBoundingBox().getCenter(), BarrierVfxEntity.FRACTURE_MARK, stage,
				0.42F + stacks * 0.12F, Math.max(1.0F, target.getBbHeight()), 18,
				target.getYRot(), 0.0F, caster, target, OrbOfAvariceManager.isHeldBy(caster),
				0.0F, false);
		return stacks;
	}

	private static int getFracture(ServerLevel level, Entity caster, LivingEntity target) {
		String key = fractureKey(caster);
		if (target.getPersistentData().getLong(key + FRACTURE_UNTIL_SUFFIX) < level.getGameTime()) {
			target.getPersistentData().remove(key);
			target.getPersistentData().remove(key + FRACTURE_UNTIL_SUFFIX);
			return 0;
		}
		return Mth.clamp(target.getPersistentData().getInt(key), 0, 3);
	}

	private static int consumeFracture(ServerLevel level, Entity caster, LivingEntity target,
			int maximum) {
		int current = getFracture(level, caster, target);
		int consumed = Math.min(current, Math.max(0, maximum));
		int remaining = current - consumed;
		String key = fractureKey(caster);
		if (remaining <= 0)
			clearFracture(caster, target);
		else
			target.getPersistentData().putInt(key, remaining);
		return consumed;
	}

	private static void clearFracture(Entity caster, LivingEntity target) {
		String key = fractureKey(caster);
		target.getPersistentData().remove(key);
		target.getPersistentData().remove(key + FRACTURE_UNTIL_SUFFIX);
	}

	private static String fractureKey(Entity caster) {
		return FRACTURE_PREFIX + caster.getUUID().toString().replace("-", "");
	}

	private static float constructIntegrity(Entity caster, double base, double coefficient,
			boolean orb) {
		double value = base + Math.max(0.0D, MageCombatHelper.intelligence(caster)) * coefficient;
		return (float) (value * (orb ? 1.5D : 1.0D));
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

	private static Vec3 groundTarget(ServerLevel level, Entity caster, double range) {
		Vec3 start = caster.getEyePosition();
		Vec3 intended = start.add(safeDirection(caster.getLookAngle()).scale(range));
		BlockHitResult hit = level.clip(new ClipContext(start, intended, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, caster));
		Vec3 point = hit.getType() == HitResult.Type.MISS ? intended : hit.getLocation();
		int startY = Mth.floor(Math.max(point.y + 2.0D, caster.getY() + 1.0D));
		BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(Mth.floor(point.x), startY,
				Mth.floor(point.z));
		for (int offset = 0; offset < 12; offset++) {
			cursor.setY(startY - offset);
			BlockState state = level.getBlockState(cursor);
			if (!state.isAir() && !state.getCollisionShape(level, cursor).isEmpty())
				return new Vec3(point.x, cursor.getY() + 1.01D, point.z);
		}
		return new Vec3(point.x, caster.getY(), point.z);
	}

	private static BarrierVfxEntity spawn(ServerLevel level, Vec3 position, int style, int stage,
			float scale, float length, int lifetime, float yaw, float pitch, Entity owner,
			Entity target, boolean orb, float integrity, boolean active) {
		return BarrierVfxEntity.spawn(level, position, style, stage, scale, length, lifetime,
				yaw, pitch, owner, target, orb, integrity, active);
	}

	private static void hurtWithoutOrbDouble(ServerLevel level, Entity caster,
			LivingEntity target, float amount) {
		float adjusted = OrbOfAvariceManager.isHeldBy(caster) ? amount * 0.5F : amount;
		MageCombatHelper.hurt(level, caster, target, adjusted);
	}

	private static Vec3 safeDirection(Vec3 direction) {
		return direction.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
	}

	private static float yawFor(Vec3 direction) {
		return (float) (Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG);
	}

	private static float pitchFor(Vec3 direction) {
		return (float) (-Mth.atan2(direction.y,
				Math.sqrt(direction.x * direction.x + direction.z * direction.z)) * Mth.RAD_TO_DEG);
	}

	private static Vec3 average(List<Vec3> values) {
		Vec3 sum = Vec3.ZERO;
		for (Vec3 value : values)
			sum = sum.add(value);
		return values.isEmpty() ? sum : sum.scale(1.0D / values.size());
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

	private static final class CollapseContext {
		private final List<UUID> constructIds;
		private final List<Vec3> centers;
		private final Set<UUID> targetIds;
		private final Map<UUID, Integer> targetStacks;
		private final float resonance;

		private CollapseContext(List<UUID> constructIds, List<Vec3> centers,
				Set<UUID> targetIds, Map<UUID, Integer> targetStacks, float resonance) {
			this.constructIds = List.copyOf(constructIds);
			this.centers = List.copyOf(centers);
			this.targetIds = Set.copyOf(targetIds);
			this.targetStacks = Map.copyOf(targetStacks);
			this.resonance = resonance;
		}

		private boolean isEmpty() {
			return constructIds.isEmpty() && targetIds.isEmpty();
		}
	}
}
