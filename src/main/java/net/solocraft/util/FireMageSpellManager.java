package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.FireMageVfxEntity;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Server-authoritative mechanics for the staged Fire Mage spell set. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class FireMageSpellManager {
	public static final String FLAME_WEAVING = "Flame Weaving";
	public static final String IGNITION_ORB = "Ignition Orb";
	public static final String INFERNO_LANCE = "Inferno Lance";
	public static final String FLASHFIRE = "Flashfire";
	public static final String CREMATION = "Cremation";
	public static final String FURNACE_DOMINION = "Furnace Dominion";
	public static final String HEAVENFALL = "Heavenfall";

	public static final Set<String> FIRE_SKILLS = Set.of(
			FLAME_WEAVING, IGNITION_ORB, INFERNO_LANCE, FLASHFIRE,
			CREMATION, FURNACE_DOMINION, HEAVENFALL);
	public static final Set<String> QTE_SKILLS = Set.of(
			IGNITION_ORB, INFERNO_LANCE, FLASHFIRE, CREMATION,
			FURNACE_DOMINION, HEAVENFALL);

	private static final String SCORCH_DATA = "sl_fire_scorch";
	private static final String FLASHFIRE_GUARD = "sl_flashfire_guard_until";
	private static final double[] COST_MULTIPLIER = {0.0D, 1.0D, 1.10D, 1.20D, 1.30D, 1.40D};
	private static final double CREMATION_BASE_MANA_PERCENT = 0.025D;
	private static final double CREMATION_MANA_PER_DAMAGE = 4.0D;

	private static final List<FireProjectile> ACTIVE_PROJECTILES = new ArrayList<>();
	private static final List<FlashfireCast> ACTIVE_DASHES = new ArrayList<>();
	private static final List<CremationCast> ACTIVE_CREMATIONS = new ArrayList<>();
	private static final List<FurnaceCast> ACTIVE_FURNACES = new ArrayList<>();
	private static final List<HeavenfallCast> ACTIVE_HEAVENFALL = new ArrayList<>();
	private static final List<OrbCollapse> ACTIVE_COLLAPSES = new ArrayList<>();
	private static final List<DelayedBurst> DELAYED_BURSTS = new ArrayList<>();

	private FireMageSpellManager() {
	}

	public static boolean isFireSkill(String skill) {
		return FIRE_SKILLS.contains(skill);
	}

	public static boolean isQteSkill(String skill) {
		return QTE_SKILLS.contains(skill);
	}

	/**
	 * Spell evolution is driven only by Intelligence. Hunter Rank may affect which
	 * spells an evaluated Mage starts with, but it never caps an owned spell.
	 */
	public static int outputStage(Entity caster) {
		if (caster == null)
			return 0;
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		if (intelligence >= 110.0D)
			return 5;
		else if (intelligence >= 80.0D)
			return 4;
		else if (intelligence >= 55.0D)
			return 3;
		else if (intelligence >= 30.0D)
			return 2;
		return 1;
	}

	public static String stageName(int stage) {
		return switch (Mth.clamp(stage, 0, 5)) {
			case 1 -> "Ember";
			case 2 -> "Blaze";
			case 3 -> "Inferno";
			case 4 -> "Cataclysm";
			case 5 -> "Ultimate Weapon";
			default -> "Dormant";
		};
	}

	public static List<Component> tooltip(Entity caster, String skill) {
		int stage = outputStage(caster);
		ArrayList<Component> lines = new ArrayList<>();
		lines.add(Component.literal(skill).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		lines.add(Component.literal(switch (skill) {
			case FLAME_WEAVING -> "Fast magical fire weaving; every third hit applies Scorch.";
			case IGNITION_ORB -> "A volatile projectile that collapses into an expanding blast.";
			case INFERNO_LANCE -> "A piercing solar lance that consumes Scorch for bonus damage.";
			case FLASHFIRE -> "Rush as living flame, redirecting and burning enemies in your path.";
			case CREMATION -> "Consume Scorch to execute linked targets in sequence.";
			case FURNACE_DOMINION -> "Create a furnace field that pulls, burns, and finally erupts.";
			case HEAVENFALL -> "Call descending fire; Ultimate output becomes the Day of Ruin.";
			default -> "Fire magic.";
		}).withStyle(ChatFormatting.GRAY));
		lines.add(Component.literal("Output: " + stageName(stage))
				.withStyle(stage >= 5 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
		if (stage > 0 && CREMATION.equals(skill)) {
			lines.add(Component.literal("Mana: Dynamic  |  Cooldown: "
					+ String.format("%.1fs", cooldownTicks(skill) / 20.0D))
					.withStyle(ChatFormatting.DARK_GRAY));
			lines.add(Component.literal("Cost rises with each target and Scorch stack detonated.")
					.withStyle(ChatFormatting.RED));
		} else if (stage > 0) {
			int cost = manaCost(caster, skill, stage, QTEResult.MISS);
			lines.add(Component.literal((cost == 0 ? "No mana cost" : "Mana: " + cost)
					+ "  |  Cooldown: " + String.format("%.1fs", cooldownTicks(skill) / 20.0D))
					.withStyle(ChatFormatting.DARK_GRAY));
		} else {
			lines.add(Component.literal("No Intelligence data is available.")
					.withStyle(ChatFormatting.DARK_RED));
		}
		return lines;
	}

	/** Called on the logical server after QTE evaluation. */
	public static boolean cast(Entity caster, String skill, QTEResult qteResult) {
		if (!(caster.level() instanceof ServerLevel level) || !isFireSkill(skill))
			return false;
		int stage = outputStage(caster);
		if (stage <= 0) {
			message(caster, "Your fire output is too weak to form a spell.");
			return false;
		}
		if (CooldownManager.isOnCooldown(caster, skill)) {
			message(caster, "Ability on cooldown!");
			return false;
		}

		List<UUID> cremationTargets = List.of();
		if (CREMATION.equals(skill)) {
			cremationTargets = findCremationTargets(level, caster, stage);
			if (cremationTargets.isEmpty()) {
				message(caster, "No Scorched target is in range.");
				return false;
			}
		}

		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		float cremationInitialDamage = (float) (2.0D + intelligence * 0.03D);
		float cremationStackDamage = (float) (4.0D + intelligence * 0.07D);
		QTEResult resolvedQte = qteResult == null ? QTEResult.MISS : qteResult;
		int cost = CREMATION.equals(skill)
				? cremationManaCost(level, caster, stage, resolvedQte, cremationTargets,
						cremationInitialDamage, cremationStackDamage)
				: manaCost(caster, skill, stage, resolvedQte);
		SololevelingModVariables.PlayerVariables data = variables(caster);
		if (!(caster instanceof Player player && player.isCreative()) && data.MP < cost) {
			message(caster, "Not enough MP! Need " + cost + ".");
			return false;
		}

		if (cost > 0)
			deductMana(caster, cost);
		CooldownManager.set(caster, skill, cooldownTicks(skill));
		CooldownManager.set(caster, "mana_refresh", 40);

		switch (skill) {
			case FLAME_WEAVING -> startWeaving(level, caster, stage,
					(float) (1.5D + intelligence * 0.035D));
			case IGNITION_ORB -> startOrb(level, caster, stage,
					(float) (4.0D + intelligence * 0.08D));
			case INFERNO_LANCE -> startLance(level, caster, stage,
					(float) (6.0D + intelligence * 0.11D));
			case FLASHFIRE -> ACTIVE_DASHES.add(new FlashfireCast(level, caster, stage,
					(float) (3.0D + intelligence * 0.05D)));
			case CREMATION -> ACTIVE_CREMATIONS.add(new CremationCast(level, caster, stage,
					cremationInitialDamage, cremationStackDamage, cremationTargets));
			case FURNACE_DOMINION -> ACTIVE_FURNACES.add(new FurnaceCast(level, caster, stage,
					(float) (1.2D + intelligence / 50.0D),
					(float) (5.0D + intelligence / 12.0D)));
			case HEAVENFALL -> ACTIVE_HEAVENFALL.add(new HeavenfallCast(level, caster, stage,
					(float) (24.0D + intelligence / 3.0D),
					(float) (6.0D + intelligence / 10.0D)));
			default -> {
				return false;
			}
		}
		return true;
	}

	/** Intelligence-scaled spell dispatch for generated Mage hunters. */
	public static boolean castNpc(Entity caster, String skill) {
		if (!(caster.level() instanceof ServerLevel level) || !isFireSkill(skill))
			return false;
		int stage = outputStage(caster);
		if (stage <= 0 || CooldownManager.isOnCooldown(caster, skill))
			return false;
		double intelligence = Math.max(10.0D, MageCombatHelper.intelligence(caster));
		boolean cast = true;
		switch (skill) {
			case FLAME_WEAVING -> startWeaving(level, caster, stage,
					(float) (1.5D + intelligence * 0.035D));
			case IGNITION_ORB -> startOrb(level, caster, stage,
					(float) (4.0D + intelligence * 0.08D));
			case INFERNO_LANCE -> startLance(level, caster, stage,
					(float) (6.0D + intelligence * 0.11D));
			case FLASHFIRE -> ACTIVE_DASHES.add(new FlashfireCast(level, caster, stage,
					(float) (3.0D + intelligence * 0.05D)));
			case CREMATION -> {
				List<UUID> targets = findCremationTargets(level, caster, stage);
				if (targets.isEmpty())
					cast = false;
				else
					ACTIVE_CREMATIONS.add(new CremationCast(level, caster, stage,
							(float) (2.0D + intelligence * 0.03D),
							(float) (4.0D + intelligence * 0.07D), targets));
			}
			case FURNACE_DOMINION -> ACTIVE_FURNACES.add(new FurnaceCast(level, caster, stage,
					(float) (1.2D + intelligence / 50.0D),
					(float) (5.0D + intelligence / 12.0D)));
			case HEAVENFALL -> ACTIVE_HEAVENFALL.add(new HeavenfallCast(level, caster, stage,
					(float) (24.0D + intelligence / 3.0D),
					(float) (6.0D + intelligence / 10.0D)));
			default -> cast = false;
		}
		if (cast)
			CooldownManager.set(caster, skill, cooldownTicks(skill));
		return cast;
	}

	public static int manaCost(Entity caster, String skill, int stage, QTEResult result) {
		if (FLAME_WEAVING.equals(skill) || caster instanceof Player player && player.isCreative())
			return 0;
		double basePercent = switch (skill) {
			case IGNITION_ORB -> 0.03D;
			case INFERNO_LANCE -> 0.045D;
			case FLASHFIRE -> 0.035D;
			case CREMATION -> CREMATION_BASE_MANA_PERCENT;
			case FURNACE_DOMINION -> 0.11D;
			case HEAVENFALL -> 0.18D;
			default -> 0.0D;
		};
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		double maximumMana = 1000.0D + intelligence * 100.0D;
		double qte = MageQTEHelper.getManaCostMultiplier(result == null ? QTEResult.MISS : result, intelligence);
		return OrbOfAvariceManager.adjustManaCost(caster, maximumMana * basePercent
				* COST_MULTIPLIER[Mth.clamp(stage, 1, 5)] * qte);
	}

	private static int cremationManaCost(ServerLevel level, Entity caster, int stage, QTEResult result,
			List<UUID> targets, float initialDamage, float stackDamage) {
		double projectedDamage = 0.0D;
		for (UUID targetId : targets) {
			Entity entity = level.getEntity(targetId);
			if (!(entity instanceof LivingEntity target) || !target.isAlive()
					|| !MageCombatHelper.isValidTarget(caster, target))
				continue;
			int stacks = getScorch(level, caster, target);
			if (stacks > 0)
				projectedDamage += initialDamage + stackDamage * stacks;
		}
		double intelligence = Math.max(0.0D, MageCombatHelper.intelligence(caster));
		double maximumMana = 1000.0D + intelligence * 100.0D;
		double activationCost = maximumMana * CREMATION_BASE_MANA_PERCENT
				* COST_MULTIPLIER[Mth.clamp(stage, 1, 5)];
		double outputCost = projectedDamage * CREMATION_MANA_PER_DAMAGE;
		double qte = MageQTEHelper.getManaCostMultiplier(result, intelligence);
		return Math.max(1, OrbOfAvariceManager.adjustManaCost(caster,
				(activationCost + outputCost) * qte));
	}

	private static int cooldownTicks(String skill) {
		return switch (skill) {
			case FLAME_WEAVING -> 9;
			case IGNITION_ORB -> 70;
			case INFERNO_LANCE -> 110;
			case FLASHFIRE -> 160;
			case CREMATION -> 240;
			case FURNACE_DOMINION -> 480;
			case HEAVENFALL -> 1200;
			default -> 20;
		};
	}

	private static void startWeaving(ServerLevel level, Entity caster, int stage, float damage) {
		Vec3 direction = safeDirection(caster.getLookAngle());
		Vec3 origin = caster.getEyePosition().add(direction.scale(0.65D));
		double speed = 4.2D + stage * 0.18D;
		double range = 18.0D + stage * 5.0D;
		double radius = 0.28D + stage * 0.13D;
		ACTIVE_PROJECTILES.add(new FireProjectile(level, caster, ProjectileKind.WEAVING, stage,
				damage, origin, direction, speed, range, radius,
				0.14F + stage * 0.065F, 2.7F + stage * 1.05F));
		play(level, origin, SoundEvents.BLAZE_SHOOT, 0.55F, 1.45F + stage * 0.05F);
	}

	private static void startOrb(ServerLevel level, Entity caster, int stage, float damage) {
		Vec3 direction = safeDirection(caster.getLookAngle());
		Vec3 origin = caster.getEyePosition().add(direction.scale(0.8D));
		double speed = 1.15D + stage * 0.2D;
		double range = 20.0D + stage * 7.0D;
		double radius = 0.50D + stage * 0.17D;
		ACTIVE_PROJECTILES.add(new FireProjectile(level, caster, ProjectileKind.ORB, stage,
				damage, origin, direction, speed, range, radius,
				0.48F + stage * 0.23F, 2.5F + stage * 0.65F));
		play(level, origin, SoundEvents.FIRECHARGE_USE, 0.85F, 0.82F + stage * 0.05F);
	}

	private static void startLance(ServerLevel level, Entity caster, int stage, float damage) {
		Vec3 direction = safeDirection(caster.getLookAngle());
		Vec3 origin = caster.getEyePosition().add(direction.scale(1.0D));
		double speed = 4.1D + stage * 0.42D;
		double range = 26.0D + stage * 10.0D;
		double radius = 0.48D + stage * 0.34D;
		ACTIVE_PROJECTILES.add(new FireProjectile(level, caster, ProjectileKind.LANCE, stage,
				damage, origin, direction, speed, range, radius,
				0.22F + stage * 0.19F, 6.5F + stage * 2.45F));
		play(level, origin, SoundEvents.BLAZE_SHOOT, 1.0F, 0.58F + stage * 0.05F);
	}

	@SubscribeEvent
	public static void onServerTick(TickEvent.ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END)
			return;
		tick(ACTIVE_PROJECTILES);
		tick(ACTIVE_DASHES);
		tick(ACTIVE_CREMATIONS);
		tick(ACTIVE_FURNACES);
		tick(ACTIVE_HEAVENFALL);
		tick(ACTIVE_COLLAPSES);
		tick(DELAYED_BURSTS);
	}

	private static <T extends ActiveCast> void tick(List<T> casts) {
		Iterator<T> iterator = casts.iterator();
		while (iterator.hasNext()) {
			T cast = iterator.next();
			if (cast.tick())
				iterator.remove();
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void reduceFlashfireDamage(LivingHurtEvent event) {
		LivingEntity target = event.getEntity();
		if (!target.level().isClientSide()
				&& target.getPersistentData().getLong(FLASHFIRE_GUARD) >= target.level().getGameTime())
			event.setAmount(event.getAmount() * 0.50F);
	}

	private interface ActiveCast {
		boolean tick();
	}

	private enum ProjectileKind {
		WEAVING,
		ORB,
		LANCE
	}

	private static final class FireProjectile implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final ProjectileKind kind;
		private final int stage;
		private final float damage;
		private final Vec3 direction;
		private final double speed;
		private final double hitRadius;
		private final Set<UUID> hitTargets = new HashSet<>();
		private final List<Vec3> hitPositions = new ArrayList<>();
		private final FireMageVfxEntity visual;
		private Vec3 position;
		private Vec3 impactPosition;
		private double remaining;
		private boolean finished;

		private FireProjectile(ServerLevel level, Entity caster, ProjectileKind kind, int stage,
				float damage, Vec3 origin, Vec3 direction, double speed, double range,
				double hitRadius, float visualScale, float visualLength) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.kind = kind;
			this.stage = stage;
			this.damage = damage;
			this.position = origin;
			this.direction = safeDirection(direction);
			this.speed = speed;
			this.remaining = range;
			this.hitRadius = hitRadius;
			int lifetime = Mth.ceil(range / speed) + 8;
			int style = kind == ProjectileKind.WEAVING ? FireMageVfxEntity.FLAME_WEAVING
					: kind == ProjectileKind.ORB ? FireMageVfxEntity.IGNITION_ORB
					: FireMageVfxEntity.INFERNO_LANCE;
			this.visual = spawnVfx(level, caster, origin.x, origin.y, origin.z, style, stage,
					visualScale, visualLength, lifetime, yawFor(direction), pitchFor(direction));
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (caster == null || !caster.isAlive() || caster.level() != level)
				return finish(null, false);
			if (finished || remaining <= 0.01D)
				return finish(caster, true);

			double step = Math.min(speed, remaining);
			Vec3 start = position;
			Vec3 intendedEnd = start.add(direction.scale(step));
			if (!level.hasChunkAt(net.minecraft.core.BlockPos.containing(intendedEnd)))
				return finish(caster, true);
			BlockHitResult blockHit = level.clip(new ClipContext(start, intendedEnd,
					ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			boolean blocked = blockHit.getType() == HitResult.Type.BLOCK;
			Vec3 end = blocked ? blockHit.getLocation() : intendedEnd;
			boolean stopOnEntity = processTargets(caster, start, end);
			position = stopOnEntity && impactPosition != null ? impactPosition : end;
			remaining -= start.distanceTo(position);
			if (visual.isAlive()) {
				visual.moveTo(position.x, position.y, position.z, yawFor(direction), pitchFor(direction));
				visual.hasImpulse = true;
			}
			return blocked || stopOnEntity || remaining <= 0.01D ? finish(caster, true) : false;
		}

		private boolean processTargets(Entity caster, Vec3 start, Vec3 end) {
			AABB search = new AABB(start, end).inflate(hitRadius + 2.0D);
			List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, search,
					target -> MageCombatHelper.isValidTarget(caster, target));
			targets.sort(Comparator.comparingDouble(target -> target.position().distanceToSqr(start)));
			int pierceLimit = kind == ProjectileKind.WEAVING
					? (stage < 3 ? 1 : Math.min(4, stage - 1)) : Integer.MAX_VALUE;
			for (LivingEntity target : targets) {
				if (hitTargets.contains(target.getUUID()) || !intersects(target, start, end, hitRadius))
					continue;
				hitTargets.add(target.getUUID());
				hitPositions.add(target.getBoundingBox().getCenter());
				if (kind == ProjectileKind.WEAVING) {
					if (MageCombatHelper.hurt(level, caster, target, damage)) {
						int hits = caster.getPersistentData().getInt("sl_flame_weaving_hits") + 1;
						if (hits >= 3) {
							addScorch(level, caster, target, 1);
							hits = 0;
						}
						caster.getPersistentData().putInt("sl_flame_weaving_hits", hits);
					}
					if (hitTargets.size() >= pierceLimit)
						return true;
				} else if (kind == ProjectileKind.ORB) {
					if (stage >= 3)
						MageCombatHelper.hurt(level, caster, target, damage * 0.35F);
					else {
						impactPosition = target.getBoundingBox().getCenter();
						return true;
					}
				} else {
					int consumed = stage >= 3 ? consumeScorch(level, caster, target) : 0;
					MageCombatHelper.hurt(level, caster, target, damage * (1.0F + consumed * 0.14F));
					addScorch(level, caster, target, 1);
					if (stage >= 5)
						DELAYED_BURSTS.add(new DelayedBurst(level, caster, target.getBoundingBox().getCenter(),
								8, 2.8D, damage * 0.38F, stage, FireMageVfxEntity.ORB_IMPACT, false));
				}
			}
			return false;
		}

		private boolean finish(Entity caster, boolean resolve) {
			if (finished)
				return true;
			finished = true;
			if (visual.isAlive())
				visual.discard();
			if (resolve && caster != null) {
				if (kind == ProjectileKind.ORB)
					resolveOrbImpact(level, caster, position, stage, damage);
				else if (kind == ProjectileKind.LANCE && stage >= 5 && hitPositions.isEmpty())
					DELAYED_BURSTS.add(new DelayedBurst(level, caster, position, 8, 3.0D,
							damage * 0.32F, stage, FireMageVfxEntity.ORB_IMPACT, false));
			}
			return true;
		}
	}

	private static final class FlashfireCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final float damage;
		private final Set<UUID> hitTargets = new HashSet<>();
		private final FireMageVfxEntity visual;
		private Vec3 direction;
		private Vec3 previous;
		private int age;
		private final int duration;

		private FlashfireCast(ServerLevel level, Entity caster, int stage, float damage) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.stage = stage;
			this.damage = damage;
			this.direction = safeDirection(caster.getLookAngle());
			this.previous = caster.position();
			this.duration = stage >= 5 ? 22 : stage >= 3 ? 12 : 7 + stage;
			this.visual = spawnVfx(level, caster, caster.getX(), caster.getY() + caster.getBbHeight() * 0.5D,
					caster.getZ(), FireMageVfxEntity.FLASHFIRE, stage, 0.65F + stage * 0.12F,
					3.0F + stage * 1.2F, duration + 7, yawFor(direction), pitchFor(direction));
			play(level, caster.position(), SoundEvents.FIRECHARGE_USE, 1.0F, 1.3F);
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (!(caster instanceof LivingEntity living) || !caster.isAlive() || caster.level() != level)
				return finish();
			age++;
			if (stage >= 5)
				direction = safeDirection(direction.scale(0.72D).add(safeDirection(caster.getLookAngle()).scale(0.28D)));
			else if (stage >= 3 && age == 7)
				direction = safeDirection(caster.getLookAngle());
			double speed = 0.92D + stage * 0.13D;
			Vec3 intended = caster.position().add(direction.scale(speed));
			BlockHitResult hit = level.clip(new ClipContext(caster.position().add(0, 0.25D, 0),
					intended.add(0, 0.25D, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
			if (hit.getType() == HitResult.Type.BLOCK)
				return finish();

			caster.setDeltaMovement(direction.scale(speed).add(0.0D, Math.max(-0.08D, direction.y * 0.35D), 0.0D));
			living.hurtMarked = true;
			living.fallDistance = 0.0F;
			if (stage >= 5)
				caster.getPersistentData().putLong(FLASHFIRE_GUARD, level.getGameTime() + 2L);
			damageAlongPath(caster, previous, intended);
			previous = intended;
			if (visual.isAlive()) {
				visual.moveTo(caster.getX(), caster.getY() + caster.getBbHeight() * 0.5D, caster.getZ(),
						yawFor(direction), pitchFor(direction));
				visual.hasImpulse = true;
			}
			if (stage >= 2 && age % 3 == 0)
				spawnVfx(level, caster, caster.getX(), caster.getY() + 0.15D, caster.getZ(),
						FireMageVfxEntity.FLASHFIRE, stage, 0.34F + stage * 0.05F,
						1.5F + stage * 0.35F, 8, yawFor(direction), pitchFor(direction));
			return age >= duration ? finish() : false;
		}

		private void damageAlongPath(Entity caster, Vec3 start, Vec3 end) {
			AABB area = new AABB(start, end).inflate(1.15D + stage * 0.16D);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
					candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
				if (!hitTargets.add(target.getUUID()))
					continue;
				MageCombatHelper.hurt(level, caster, target, damage);
				if (stage >= 2)
					addScorch(level, caster, target, 1);
				if (stage >= 4)
					DELAYED_BURSTS.add(new DelayedBurst(level, caster, target.getBoundingBox().getCenter(),
							6, 2.2D, damage * 0.45F, stage, FireMageVfxEntity.ORB_IMPACT, false));
			}
		}

		private boolean finish() {
			if (visual.isAlive())
				visual.discard();
			return true;
		}
	}

	private static final class CremationCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final float initialDamage;
		private final float stackDamage;
		private final List<UUID> targets;
		private int age;
		private int index;
		private Vec3 previousPosition;

		private CremationCast(ServerLevel level, Entity caster, int stage, float initialDamage,
				float stackDamage, List<UUID> targets) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.stage = stage;
			this.initialDamage = initialDamage;
			this.stackDamage = stackDamage;
			this.targets = List.copyOf(targets);
			this.previousPosition = caster.getEyePosition();
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (caster == null || !caster.isAlive() || caster.level() != level)
				return true;
			age++;
			int interval = stage >= 5 ? 2 : 3;
			if (age % interval != 1)
				return false;
			while (index < targets.size()) {
				Entity entity = level.getEntity(targets.get(index++));
				if (!(entity instanceof LivingEntity target) || !target.isAlive()
						|| !MageCombatHelper.isValidTarget(caster, target))
					continue;
				int stacks = consumeScorch(level, caster, target);
				if (stacks <= 0)
					continue;
				Vec3 targetPosition = target.getBoundingBox().getCenter();
				spawnLink(level, caster, previousPosition, targetPosition, stage);
				previousPosition = targetPosition;
				spawnVfx(level, caster, target.getX(), target.getY(), target.getZ(),
						FireMageVfxEntity.CREMATION, stage,
						Math.max(1.0F, target.getBbWidth() * 1.25F),
						Math.max(2.0F, target.getBbHeight() * 1.45F), 18 + stage * 2, 0.0F, 0.0F);
				MageCombatHelper.hurt(level, caster, target, initialDamage + stackDamage * stacks);
				if (stage >= 4)
					spreadScorch(level, caster, target, 2 + (stage >= 5 ? 2 : 0));
				play(level, targetPosition, SoundEvents.GENERIC_EXPLODE, 0.55F, 1.25F + stage * 0.04F);
				break;
			}
			return index >= targets.size();
		}
	}

	private static final class FurnaceCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final float pulseDamage;
		private final float finalDamage;
		private final Vec3 center;
		private final double radius;
		private final FireMageVfxEntity visual;
		private int age;

		private FurnaceCast(ServerLevel level, Entity caster, int stage, float pulseDamage, float finalDamage) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.stage = stage;
			this.pulseDamage = pulseDamage;
			this.finalDamage = finalDamage;
			this.center = groundTarget(level, caster, 24.0D + stage * 4.0D);
			this.radius = new double[] {0, 5, 7, 9, 12, 16}[stage];
			this.visual = spawnVfx(level, caster, center.x, center.y, center.z,
					FireMageVfxEntity.FURNACE_DOMINION, stage, (float) radius,
					3.2F + stage * 1.15F, 88, 0.0F, 0.0F);
			play(level, center, SoundEvents.BLAZE_AMBIENT, 1.0F, 0.55F);
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (caster == null || !caster.isAlive() || caster.level() != level)
				return finish();
			age++;
			if (stage >= 4 && age % 2 == 0)
				burnProjectiles(caster);
			if (age % 10 == 0 && age <= 80)
				pulse(caster, age / 10);
			if (age >= 82) {
				blast(level, caster, center, radius * 0.76D, finalDamage, stage, true);
				spawnVfx(level, caster, center.x, center.y, center.z,
						FireMageVfxEntity.HEAVENFALL_IMPACT, stage, (float) (radius * 0.72D),
						5.0F + stage * 1.7F, 24, 0.0F, 0.0F);
				play(level, center, SoundEvents.GENERIC_EXPLODE, 1.35F, 0.72F);
				return finish();
			}
			return false;
		}

		private void pulse(Entity caster, int pulse) {
			AABB area = new AABB(center, center).inflate(radius, Math.max(4.0D, radius * 0.55D), radius);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
					candidate -> MageCombatHelper.isValidTarget(caster, candidate)
							&& candidate.distanceToSqr(center) <= radius * radius)) {
				MageCombatHelper.hurt(level, caster, target, pulseDamage);
				if (pulse % 2 == 0)
					addScorch(level, caster, target, 1);
				if (stage >= 3) {
					Vec3 pull = center.subtract(target.position());
					if (pull.lengthSqr() > 0.2D) {
						target.setDeltaMovement(target.getDeltaMovement().scale(0.62D).add(pull.normalize().scale(0.16D + stage * 0.018D)));
						target.hurtMarked = true;
					}
				}
			}
			play(level, center, SoundEvents.FIRE_EXTINGUISH, 0.55F, 0.65F + pulse * 0.035F);
		}

		private void burnProjectiles(Entity caster) {
			AABB area = new AABB(center, center).inflate(radius, radius, radius);
			for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, area)) {
				Entity owner = projectile.getOwner();
				if (owner == caster || owner != null && !MageCombatHelper.isValidTarget(caster, owner))
					continue;
				projectile.discard();
			}
		}

		private boolean finish() {
			if (visual.isAlive())
				visual.discard();
			return true;
		}
	}

	private static final class HeavenfallCast implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final int stage;
		private final float impactDamage;
		private final float followupDamage;
		private final Vec3 center;
		private final double radius;
		private final int delay;
		private final List<FallingVisual> meteors = new ArrayList<>();
		private int age;

		private HeavenfallCast(ServerLevel level, Entity caster, int stage, float impactDamage, float followupDamage) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.stage = stage;
			this.impactDamage = impactDamage;
			this.followupDamage = followupDamage;
			this.center = groundTarget(level, caster, 38.0D + stage * 14.0D);
			this.radius = new double[] {0, 5, 7, 9, 12, 18}[stage];
			this.delay = new int[] {0, 28, 32, 36, 42, 54}[stage];
			createMeteors(caster);
			play(level, center, SoundEvents.WITHER_SPAWN, stage >= 5 ? 1.2F : 0.65F, 1.35F);
		}

		private void createMeteors(Entity caster) {
			int count = switch (stage) {
				case 1 -> 1;
				case 2 -> 3;
				case 3 -> 5;
				case 4 -> 1;
				default -> 6;
			};
			for (int i = 0; i < count; i++) {
				double angle = Math.PI * 2.0D * i / Math.max(1, count) + level.getRandom().nextDouble() * 0.35D;
				double offset = i == 0 || stage == 4 ? 0.0D : (2.0D + level.getRandom().nextDouble() * radius * 0.48D);
				Vec3 target = center.add(Math.cos(angle) * offset, 0.0D, Math.sin(angle) * offset);
				double height = 22.0D + stage * 7.0D + level.getRandom().nextDouble() * 8.0D;
				Vec3 start = target.add(-10.0D - stage * 2.0D, height, -6.0D + i * 1.6D);
				float scale = (float) (i == 0 ? 0.95D + stage * 0.68D : 0.48D + stage * 0.22D);
				if (stage == 5 && i == 0)
					scale = 6.4F;
				Vec3 direction = safeDirection(target.subtract(start));
				FireMageVfxEntity visual = spawnVfx(level, caster, start.x, start.y, start.z,
						FireMageVfxEntity.HEAVENFALL, stage, scale,
						6.0F + stage * 2.7F, delay + 8, yawFor(direction), pitchFor(direction),
						stage >= 5 ? 0x8D1708 : 0xD73B09, 0xFFD34A);
				meteors.add(new FallingVisual(visual, start, target));
			}
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (caster == null || !caster.isAlive() || caster.level() != level)
				return finish();
			age++;
			float progress = Mth.clamp(age / (float) delay, 0.0F, 1.0F);
			float eased = progress * progress * (3.0F - 2.0F * progress);
			for (FallingVisual meteor : meteors) {
				Vec3 position = meteor.start.lerp(meteor.target, eased);
				if (meteor.visual.isAlive()) {
					Vec3 direction = safeDirection(meteor.target.subtract(position));
					meteor.visual.moveTo(position.x, position.y, position.z, yawFor(direction), pitchFor(direction));
					meteor.visual.hasImpulse = true;
				}
			}
			if (age < delay)
				return false;

			for (FallingVisual meteor : meteors) {
				if (meteor.visual.isAlive())
					meteor.visual.discard();
				spawnVfx(level, caster, meteor.target.x, meteor.target.y, meteor.target.z,
						FireMageVfxEntity.HEAVENFALL_IMPACT, stage,
						(float) (radius * (meteor.target.equals(center) ? 1.0D : 0.43D)),
						5.0F + stage * 3.0F, 26 + stage * 4, 0.0F, 0.0F);
			}
			blast(level, caster, center, radius, impactDamage, stage, true);
			DELAYED_BURSTS.add(new DelayedBurst(level, caster, center, 14,
					radius * 1.16D, followupDamage, stage, FireMageVfxEntity.HEAVENFALL_IMPACT, true));
			if (stage >= 5) {
				for (int i = 0; i < 4; i++) {
					double angle = Math.PI * 0.5D * i;
					Vec3 connected = center.add(Math.cos(angle) * radius * 0.72D, 0.0D,
							Math.sin(angle) * radius * 0.72D);
					DELAYED_BURSTS.add(new DelayedBurst(level, caster, connected, 4 + i * 2,
							radius * 0.42D, followupDamage * 0.55F, stage,
							FireMageVfxEntity.ORB_IMPACT, false));
				}
			}
			play(level, center, SoundEvents.GENERIC_EXPLODE, stage >= 5 ? 2.0F : 1.2F, 0.48F + stage * 0.035F);
			return finish();
		}

		private boolean finish() {
			for (FallingVisual meteor : meteors) {
				if (meteor.visual.isAlive())
					meteor.visual.discard();
			}
			return true;
		}
	}

	private static final class FallingVisual {
		private final FireMageVfxEntity visual;
		private final Vec3 start;
		private final Vec3 target;

		private FallingVisual(FireMageVfxEntity visual, Vec3 start, Vec3 target) {
			this.visual = visual;
			this.start = start;
			this.target = target;
		}
	}

	private static final class OrbCollapse implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final Vec3 center;
		private final int stage;
		private final double radius;
		private final float damage;
		private int age;

		private OrbCollapse(ServerLevel level, Entity caster, Vec3 center, int stage, double radius, float damage) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.center = center;
			this.stage = stage;
			this.radius = radius;
			this.damage = damage;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (caster == null || !caster.isAlive() || caster.level() != level)
				return true;
			age++;
			AABB area = new AABB(center, center).inflate(radius);
			for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
					candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
				Vec3 pull = center.subtract(target.getBoundingBox().getCenter());
				if (pull.lengthSqr() > 0.16D) {
					target.setDeltaMovement(target.getDeltaMovement().scale(0.72D).add(pull.normalize().scale(0.12D)));
					target.hurtMarked = true;
				}
			}
			if (age < 18)
				return false;
			blast(level, caster, center, radius, damage, stage, true);
			play(level, center, SoundEvents.GENERIC_EXPLODE, 1.45F, 0.58F);
			return true;
		}
	}

	private static final class DelayedBurst implements ActiveCast {
		private final ServerLevel level;
		private final UUID casterId;
		private final Vec3 center;
		private final int delay;
		private final double radius;
		private final float damage;
		private final int stage;
		private final int style;
		private final boolean scorch;
		private int age;

		private DelayedBurst(ServerLevel level, Entity caster, Vec3 center, int delay,
				double radius, float damage, int stage, int style, boolean scorch) {
			this.level = level;
			this.casterId = caster.getUUID();
			this.center = center;
			this.delay = Math.max(1, delay);
			this.radius = radius;
			this.damage = damage;
			this.stage = stage;
			this.style = style;
			this.scorch = scorch;
		}

		@Override
		public boolean tick() {
			Entity caster = level.getEntity(casterId);
			if (caster == null || !caster.isAlive() || caster.level() != level)
				return true;
			if (++age < delay)
				return false;
			blast(level, caster, center, radius, damage, stage, scorch);
			spawnVfx(level, caster, center.x, center.y, center.z, style, stage,
					(float) radius, 3.0F + stage * 1.2F, 18 + stage * 2, 0.0F, 0.0F);
			play(level, center, SoundEvents.GENERIC_EXPLODE, 0.75F, 0.78F + stage * 0.04F);
			return true;
		}
	}

	private static void resolveOrbImpact(ServerLevel level, Entity caster, Vec3 center, int stage, float damage) {
		double radius = 2.4D + stage * 1.25D;
		int lifetime = stage >= 5 ? 34 : 18 + stage * 2;
		spawnVfx(level, caster, center.x, center.y, center.z,
				FireMageVfxEntity.ORB_IMPACT, stage, (float) radius,
				2.5F + stage * 0.75F, lifetime, 0.0F, 0.0F);
		if (stage >= 5) {
			ACTIVE_COLLAPSES.add(new OrbCollapse(level, caster, center, stage, radius, damage));
			return;
		}
		blast(level, caster, center, radius, damage, stage, true);
		if (stage == 4) {
			for (int i = 0; i < 3; i++) {
				double angle = Math.PI * 2.0D * i / 3.0D;
				Vec3 point = center.add(Math.cos(angle) * radius * 0.58D, 0.0D,
						Math.sin(angle) * radius * 0.58D);
				DELAYED_BURSTS.add(new DelayedBurst(level, caster, point, 4 + i * 3,
						2.6D, damage * 0.20F, stage, FireMageVfxEntity.ORB_IMPACT, false));
			}
		}
		play(level, center, SoundEvents.GENERIC_EXPLODE, 1.0F, 0.72F + stage * 0.045F);
	}

	private static void blast(ServerLevel level, Entity caster, Vec3 center, double radius,
			float damage, int stage, boolean scorch) {
		AABB area = new AABB(center, center).inflate(radius, Math.max(2.5D, radius * 0.72D), radius);
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
				candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
			Vec3 targetCenter = target.getBoundingBox().getCenter();
			double distance = targetCenter.distanceTo(center);
			if (distance > radius + Math.max(target.getBbWidth(), target.getBbHeight()) * 0.45D)
				continue;
			float falloff = (float) Mth.clamp(1.15D - distance / Math.max(0.1D, radius) * 0.35D, 0.72D, 1.0D);
			MageCombatHelper.hurt(level, caster, target, damage * falloff);
			if (scorch)
				addScorch(level, caster, target, stage >= 5 ? 2 : 1);
			Vec3 push = targetCenter.subtract(center);
			if (push.lengthSqr() > 0.01D) {
				target.setDeltaMovement(target.getDeltaMovement().add(push.normalize().scale(0.11D + stage * 0.025D)));
				target.hurtMarked = true;
			}
		}
	}

	private static List<UUID> findCremationTargets(ServerLevel level, Entity caster, int stage) {
		double radius = 14.0D + stage * 5.0D;
		Vec3 eye = caster.getEyePosition();
		Vec3 look = safeDirection(caster.getLookAngle());
		AABB area = caster.getBoundingBox().inflate(radius);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
				target -> MageCombatHelper.isValidTarget(caster, target) && getScorch(level, caster, target) > 0);
		candidates.sort(Comparator.comparingDouble(target -> aimScore(eye, look, target.getBoundingBox().getCenter())));
		int limit = switch (stage) {
			case 1 -> 1;
			case 2 -> 2;
			case 3 -> 6;
			case 4 -> 10;
			default -> 16;
		};
		return candidates.stream().limit(limit).map(Entity::getUUID).toList();
	}

	private static double aimScore(Vec3 eye, Vec3 look, Vec3 point) {
		Vec3 offset = point.subtract(eye);
		double forward = Math.max(0.0D, offset.dot(look));
		Vec3 nearest = eye.add(look.scale(forward));
		return point.distanceToSqr(nearest) * 3.0D + offset.lengthSqr() * 0.018D
				+ (offset.dot(look) < 0.0D ? 10000.0D : 0.0D);
	}

	private static void spreadScorch(ServerLevel level, Entity caster, LivingEntity origin, int limit) {
		int spread = 0;
		for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
				origin.getBoundingBox().inflate(5.0D), candidate -> MageCombatHelper.isValidTarget(caster, candidate))) {
			if (target == origin || getScorch(level, caster, target) > 0)
				continue;
			addScorch(level, caster, target, 1);
			if (++spread >= limit)
				break;
		}
	}

	private static FireMageVfxEntity spawnVfx(ServerLevel level, Entity caster,
			double x, double y, double z, int style, int stage, float scale, float length,
			int lifetime, float yaw, float pitch) {
		return spawnVfx(level, caster, x, y, z, style, stage, scale, length, lifetime,
				yaw, pitch, 0xFF5A0A, 0xFFD34A);
	}

	private static FireMageVfxEntity spawnVfx(ServerLevel level, Entity caster,
			double x, double y, double z, int style, int stage, float scale, float length,
			int lifetime, float yaw, float pitch, int primaryColor, int secondaryColor) {
		if (OrbOfAvariceManager.isHeldBy(caster)) {
			primaryColor = OrbOfAvariceManager.BLUE_FIRE_PRIMARY;
			secondaryColor = OrbOfAvariceManager.BLUE_FIRE_SECONDARY;
		}
		return FireMageVfxEntity.spawn(level, x, y, z, style, stage, scale, length,
				lifetime, yaw, pitch, primaryColor, secondaryColor);
	}

	private static void spawnLink(ServerLevel level, Entity caster, Vec3 start, Vec3 end, int stage) {
		Vec3 delta = end.subtract(start);
		if (delta.lengthSqr() < 0.04D)
			return;
		Vec3 direction = delta.normalize();
		Vec3 midpoint = start.add(delta.scale(0.5D));
		spawnVfx(level, caster, midpoint.x, midpoint.y, midpoint.z,
				FireMageVfxEntity.INFERNO_LANCE, stage, 0.12F + stage * 0.035F,
				(float) delta.length() * 0.55F, 9, yawFor(direction), pitchFor(direction));
	}

	private static void addScorch(ServerLevel level, Entity caster, LivingEntity target, int amount) {
		if (amount <= 0)
			return;
		CompoundTag all = target.getPersistentData().getCompound(SCORCH_DATA);
		String key = caster.getUUID().toString();
		CompoundTag mark = all.getCompound(key);
		int current = mark.getLong("Expiry") >= level.getGameTime() ? mark.getInt("Stacks") : 0;
		mark.putInt("Stacks", Mth.clamp(current + amount, 0, 3));
		mark.putLong("Expiry", level.getGameTime() + 160L);
		all.put(key, mark);
		target.getPersistentData().put(SCORCH_DATA, all);
		spawnVfx(level, caster, target.getX(), target.getY() + target.getBbHeight() * 0.55D,
				target.getZ(), FireMageVfxEntity.SCORCH, outputStage(caster),
				Math.max(0.45F, target.getBbWidth() * 0.58F),
				Math.max(0.8F, target.getBbHeight() * 0.45F), 10, 0.0F, 0.0F);
	}

	private static int getScorch(ServerLevel level, Entity caster, LivingEntity target) {
		CompoundTag all = target.getPersistentData().getCompound(SCORCH_DATA);
		String key = caster.getUUID().toString();
		if (!all.contains(key))
			return 0;
		CompoundTag mark = all.getCompound(key);
		if (mark.getLong("Expiry") < level.getGameTime()) {
			all.remove(key);
			target.getPersistentData().put(SCORCH_DATA, all);
			return 0;
		}
		return Mth.clamp(mark.getInt("Stacks"), 0, 3);
	}

	private static int consumeScorch(ServerLevel level, Entity caster, LivingEntity target) {
		int stacks = getScorch(level, caster, target);
		if (stacks <= 0)
			return 0;
		CompoundTag all = target.getPersistentData().getCompound(SCORCH_DATA);
		all.remove(caster.getUUID().toString());
		target.getPersistentData().put(SCORCH_DATA, all);
		return stacks;
	}

	private static Vec3 groundTarget(ServerLevel level, Entity caster, double range) {
		Vec3 eye = caster.getEyePosition();
		Vec3 end = eye.add(safeDirection(caster.getLookAngle()).scale(range));
		BlockHitResult forward = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, caster));
		Vec3 point = forward.getType() == HitResult.Type.BLOCK ? forward.getLocation() : end;
		BlockHitResult down = level.clip(new ClipContext(point.add(0.0D, 5.0D, 0.0D),
				point.add(0.0D, -24.0D, 0.0D), ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, caster));
		return down.getType() == HitResult.Type.BLOCK ? down.getLocation().add(0.0D, 0.04D, 0.0D) : point;
	}

	private static boolean intersects(LivingEntity target, Vec3 start, Vec3 end, double radius) {
		Vec3 segment = end.subtract(start);
		double lengthSq = segment.lengthSqr();
		Vec3 center = target.getBoundingBox().getCenter();
		double t = lengthSq < 1.0E-7D ? 0.0D
				: Mth.clamp(center.subtract(start).dot(segment) / lengthSq, 0.0D, 1.0D);
		Vec3 nearest = start.add(segment.scale(t));
		double allowance = radius + Math.max(target.getBbWidth(), target.getBbHeight() * 0.42D);
		return center.distanceToSqr(nearest) <= allowance * allowance;
	}

	private static Vec3 safeDirection(Vec3 direction) {
		return direction == null || direction.lengthSqr() < 1.0E-6D
				? new Vec3(0.0D, 0.0D, 1.0D) : direction.normalize();
	}

	private static float yawFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
	}

	private static float pitchFor(Vec3 direction) {
		return (float) Math.toDegrees(Math.asin(-Mth.clamp(direction.y, -1.0D, 1.0D)));
	}

	private static void play(ServerLevel level, Vec3 position, net.minecraft.sounds.SoundEvent sound,
			float volume, float pitch) {
		level.playSound(null, position.x, position.y, position.z, sound, SoundSource.PLAYERS,
				volume, pitch);
	}

	private static void message(Entity caster, String text) {
		if (caster instanceof Player player)
			player.displayClientMessage(Component.literal(text), true);
	}

	private static void deductMana(Entity caster, int amount) {
		caster.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(data -> {
			data.MP = Math.max(0.0D, data.MP - amount);
			data.syncPlayerVariables(caster);
		});
	}

	private static SololevelingModVariables.PlayerVariables variables(Entity caster) {
		return caster.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}
}
