package net.solocraft.util;

import net.solocraft.dungeon.runtime.DungeonMobLevelAdapter;
import net.solocraft.entity.ShadowIronEntity;
import net.solocraft.entity.ai.ShadowIronCombatPolicy;
import net.solocraft.party.PartyService;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-authoritative combat, Taunt, guardian rescue, VFX, and sound logic. */
@EventBusSubscriber
public final class ShadowIronCombatManager {
	public static final String TAUNT_HIGHLIGHT_SOURCE_PREFIX =
			"shadow:iron_taunt:";
	public static final int TAUNT_HIGHLIGHT_PRIORITY = 325;

	private static final TagKey<EntityType<?>> BOSS_TAG = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("minecraft", "soloboss"));
	private static final TagKey<EntityType<?>> TAUNT_IMMUNE_TAG = TagKey.create(
			Registries.ENTITY_TYPE,
			ResourceLocation.fromNamespaceAndPath("sololeveling", "iron_taunt_immune"));
	private static final double TAUNT_RANGE_SQR =
			ShadowIronCombatPolicy.TAUNT_RANGE
					* ShadowIronCombatPolicy.TAUNT_RANGE;
	private static final Map<UUID, TauntState> TAUNTS = new HashMap<>();
	private static final Map<UUID, RescueState> RESCUES = new HashMap<>();
	private static final List<RingBurst> RINGS = new ArrayList<>();

	private ShadowIronCombatManager() {
	}

	public static void tickIron(ShadowIronEntity iron) {
		if (iron == null || iron.level().isClientSide() || !iron.isAlive()
				|| iron.getCombatAction() == ShadowIronEntity.Action.BLOCK
				|| !iron.canInterceptNow()
				|| Math.floorMod(iron.tickCount + iron.getId(), 2) != 0)
			return;
		tryGuardianRescue(iron);
	}

	public static LivingEntity findGuardianThreat(ShadowIronEntity iron) {
		if (iron == null || iron.level().isClientSide())
			return null;
		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(iron);
		if (owner == null || !owner.isAlive())
			return null;
		String command = ShadowMonarchManager.currentShadowCommand(iron);
		if (!ShadowMonarchManager.COMMAND_DEFAULT.equals(command)
				&& !ShadowMonarchManager.COMMAND_PROTECT.equals(command))
			return null;
		double range = ShadowIronCombatPolicy.GUARDIAN_SCAN_RANGE;
		return iron.level().getEntitiesOfClass(Mob.class,
				owner.getBoundingBox().inflate(range, range * 0.5D, range),
				mob -> mob.isAlive() && mob.getTarget() == owner
						&& ShadowMonarchManager.canShadowDamage(iron, mob)
						&& (owner.hasLineOfSight(mob)
								|| iron.hasLineOfSight(mob)))
				.stream()
				.min(Comparator.comparingDouble(owner::distanceToSqr))
				.orElse(null);
	}

	public static boolean shouldRoar(ShadowIronEntity iron) {
		if (iron == null || !iron.canRoarNow()
				|| Math.floorMod(iron.tickCount + iron.getId(), 10) != 0)
			return false;
		String command = ShadowMonarchManager.currentShadowCommand(iron);
		if (ShadowMonarchManager.COMMAND_FOLLOW.equals(command))
			return false;
		List<Mob> candidates = tauntCandidates(iron);
		if (candidates.isEmpty())
			return false;
		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(iron);
		long ownerThreats = owner == null ? 0L : candidates.stream()
				.filter(mob -> mob.getTarget() == owner).count();
		double ownerHealth = owner == null ? 1.0D
				: owner.getHealth() / Math.max(1.0F, owner.getMaxHealth());
		if (ShadowMonarchManager.COMMAND_PROTECT.equals(command))
			return ownerThreats >= 1L || candidates.size() >= 3;
		if (ShadowMonarchManager.COMMAND_BERSERK.equals(command)
				|| ShadowMonarchManager.COMMAND_CLEAR_DUNGEON.equals(command))
			return candidates.size() >= 3;
		return ownerThreats >= 2L || ownerHealth <= 0.50D && ownerThreats >= 1L
				|| candidates.size() >= 4;
	}

	public static boolean shouldBrace(ShadowIronEntity iron,
			LivingEntity target) {
		if (iron == null || target == null || !iron.canBlockNow()
				|| iron.distanceToSqr(target) > 4.5D * 4.5D)
			return false;
		if (target.getAttackAnim(0.0F) > 0.05F)
			return true;
		int nearby = iron.level().getEntitiesOfClass(Mob.class,
				iron.getBoundingBox().inflate(4.0D, 2.5D, 4.0D),
				mob -> ShadowMonarchManager.canShadowDamage(iron, mob)).size();
		return nearby >= 2 && Math.floorMod(iron.tickCount + iron.getId(), 20) == 0;
	}

	public static void tryGuardianChallenge(ShadowIronEntity iron,
			LivingEntity target) {
		if (!(target instanceof Mob mob) || mob.getType().is(TAUNT_IMMUNE_TAG))
			return;
		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(iron);
		if (owner == null || mob.getTarget() != owner)
			return;
		String command = ShadowMonarchManager.currentShadowCommand(iron);
		if (!ShadowMonarchManager.COMMAND_DEFAULT.equals(command)
				&& !ShadowMonarchManager.COMMAND_PROTECT.equals(command))
			return;
		claimTaunt(iron, mob,
				ShadowIronCombatPolicy.passiveChallengeDuration(isBoss(mob)));
	}

	public static void performCleave(ShadowIronEntity iron,
			LivingEntity primary, boolean counter) {
		if (iron == null || !(iron.level() instanceof ServerLevel level))
			return;
		level.playSound(null, iron.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK,
				SoundSource.NEUTRAL, counter ? 1.0F : 0.85F,
				counter ? 0.68F : 0.78F);
		level.playSound(null, iron.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
				SoundSource.NEUTRAL, 0.55F, counter ? 0.72F : 0.82F);
		if (!isMeleeTarget(iron, primary)) {
			spawnSweep(level, iron.position().add(0.0D, 1.8D, 0.0D));
			return;
		}

		float primaryDamage = ShadowIronCombatPolicy.primaryDamage(
				iron.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
				counter);
		if (hurtFromIron(iron, primary, primaryDamage)) {
			primary.knockback(counter ? 0.65D : 0.35D,
					iron.getX() - primary.getX(), iron.getZ() - primary.getZ());
			spawnHit(level, primary.getBoundingBox().getCenter(), ironColor(iron));
		}

		Vec3 forward = Vec3.directionFromRotation(0.0F, iron.getYRot())
				.multiply(1.0D, 0.0D, 1.0D).normalize();
		List<LivingEntity> secondary = level.getEntitiesOfClass(LivingEntity.class,
				iron.getBoundingBox().inflate(3.2D, 2.5D, 3.2D),
				candidate -> candidate != primary
						&& ShadowMonarchManager.canShadowDamage(iron, candidate)
						&& inForwardArc(iron, candidate, forward))
				.stream().sorted(Comparator.comparingDouble(iron::distanceToSqr))
				.limit(2).toList();
		float splash = ShadowIronCombatPolicy.secondaryDamage(
				iron.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
		for (LivingEntity target : secondary) {
			if (hurtFromIron(iron, target, splash)) {
				target.knockback(0.25D, iron.getX() - target.getX(),
						iron.getZ() - target.getZ());
				spawnHit(level, target.getBoundingBox().getCenter(), ironColor(iron));
			}
		}
		spawnSweep(level, primary.getBoundingBox().getCenter());
	}

	public static void performRoar(ShadowIronEntity iron) {
		if (iron == null || !(iron.level() instanceof ServerLevel level))
			return;
		int accepted = 0;
		for (Mob target : tauntCandidates(iron)) {
			boolean boss = isBoss(target);
			boolean elite = isElite(target);
			if (claimTaunt(iron, target,
					ShadowIronCombatPolicy.tauntDuration(boss, elite)))
				accepted++;
		}
		iron.fortifyFromTaunt(accepted);
		int color = ironColor(iron);
		RINGS.add(new RingBurst(level, iron.position(), color, 0));
		level.playSound(null, iron.blockPosition(), SoundEvents.RAVAGER_ROAR,
				SoundSource.NEUTRAL, 1.25F, 0.62F);
		level.playSound(null, iron.blockPosition(), SoundEvents.WITHER_AMBIENT,
				SoundSource.NEUTRAL, 0.42F, 1.35F);
		spawnDust(level, iron.position().add(0.0D, 2.1D, 0.0D), color,
				16, 0.85D, 0.055D);
	}

	public static void onShieldBlock(ShadowIronEntity iron,
			DamageSource source) {
		if (iron == null || !(iron.level() instanceof ServerLevel level))
			return;
		Vec3 forward = Vec3.directionFromRotation(0.0F, iron.getYRot())
				.multiply(1.0D, 0.0D, 1.0D).normalize();
		Vec3 shield = iron.position().add(forward.scale(0.72D))
				.add(0.0D, 1.85D, 0.0D);
		level.sendParticles(ParticleTypes.ELECTRIC_SPARK, shield.x, shield.y,
				shield.z, 8, 0.28D, 0.42D, 0.28D, 0.08D);
		spawnDust(level, shield, ironColor(iron), 8, 0.28D, 0.03D);
		level.playSound(null, iron.blockPosition(), SoundEvents.SHIELD_BLOCK,
				SoundSource.NEUTRAL, 1.0F, 0.72F);
		level.playSound(null, iron.blockPosition(), SoundEvents.ANVIL_LAND,
				SoundSource.NEUTRAL, 0.24F, 1.45F);
	}

	public static boolean isBossDamageSource(DamageSource source) {
		if (source == null)
			return false;
		if (source.getEntity() instanceof LivingEntity living)
			return isBoss(living);
		if (source.getDirectEntity() instanceof Projectile projectile
				&& projectile.getOwner() instanceof LivingEntity owner)
			return isBoss(owner);
		return false;
	}

	public static boolean isBoss(LivingEntity target) {
		if (target == null || target instanceof Player)
			return false;
		DungeonMobLevelAdapter.MobRole role =
				DungeonMobLevelAdapter.MobRole.fromString(target.getPersistentData()
						.getString(DungeonMobLevelAdapter.ROLE_TAG));
		return role == DungeonMobLevelAdapter.MobRole.BOSS
				|| target.getType().is(BOSS_TAG) || target.getMaxHealth() >= 250.0F;
	}

	@SubscribeEvent
	public static void onTargetChange(LivingChangeTargetEvent event) {
		if (!(event.getEntity() instanceof Mob mob))
			return;
		TauntState state = TAUNTS.get(mob.getUUID());
		if (state == null || !state.hardLock
				|| state.level.getGameTime() >= state.expiresAt)
			return;
		Entity entity = state.level.getEntity(state.ironId);
		if (entity instanceof ShadowIronEntity iron && iron.isAlive()
				&& event.getNewAboutToBeSetTarget() != iron)
			event.setNewAboutToBeSetTarget(iron);
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void onOwnerHurt(LivingIncomingDamageEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer owner)
				|| event.getAmount() <= 0.0F)
			return;
		RescueState rescue = RESCUES.get(owner.getUUID());
		if (rescue == null || rescue.level != owner.level()
				|| rescue.level.getGameTime() >= rescue.expiresAt
				|| !matchesThreat(event.getSource(), rescue.threatId)
				|| event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))
			return;
		Entity entity = rescue.level.getEntity(rescue.ironId);
		if (!(entity instanceof ShadowIronEntity iron) || !iron.isAlive()
				|| iron.distanceToSqr(owner) > 3.25D * 3.25D
				|| !iron.canBlockSource(event.getSource())) {
			RESCUES.remove(owner.getUUID());
			return;
		}
		boolean boss = isBossDamageSource(event.getSource());
		float original = event.getAmount();
		float redirected = original
				* ShadowIronCombatPolicy.redirectedDamageFraction(boss);
		RESCUES.remove(owner.getUUID());
		if (!iron.hurt(event.getSource(), redirected))
			return;
		event.setAmount(original * ShadowIronCombatPolicy.ownerDamageFraction(boss));
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		tickTaunts();
		tickRings();
		RESCUES.entrySet().removeIf(entry -> {
			RescueState state = entry.getValue();
			return state.level.getGameTime() >= state.expiresAt
					|| !(state.level.getEntity(state.ironId)
							instanceof ShadowIronEntity iron) || !iron.isAlive();
		});
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		TAUNTS.clear();
		RESCUES.clear();
		RINGS.clear();
	}

	private static List<Mob> tauntCandidates(ShadowIronEntity iron) {
		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(iron);
		return iron.level().getEntitiesOfClass(Mob.class,
				iron.getBoundingBox().inflate(ShadowIronCombatPolicy.TAUNT_RANGE,
						6.0D, ShadowIronCombatPolicy.TAUNT_RANGE),
				mob -> mob.distanceToSqr(iron) <= TAUNT_RANGE_SQR
						&& !mob.getType().is(TAUNT_IMMUNE_TAG)
						&& ShadowMonarchManager.canShadowDamage(iron, mob)
						&& iron.getSensing().hasLineOfSight(mob)
						&& mob.canAttack(iron))
				.stream().sorted(Comparator
						.comparingInt((Mob mob) -> mob.getTarget() == owner ? 0 : 1)
						.thenComparingDouble(iron::distanceToSqr)
						.thenComparing(mob -> mob.getUUID().toString()))
				.limit(ShadowIronCombatPolicy.TAUNT_TARGET_CAP).toList();
	}

	private static boolean claimTaunt(ShadowIronEntity iron, Mob target,
			int durationTicks) {
		if (iron == null || target == null || durationTicks <= 0
				|| !(iron.level() instanceof ServerLevel level)
				|| TankerSkillManager.hasActiveTauntClaim(target))
			return false;
		long now = level.getGameTime();
		TauntState existing = TAUNTS.get(target.getUUID());
		if (existing != null && now < existing.expiresAt) {
			if (!existing.ironId.equals(iron.getUUID()))
				return false;
			if (existing.expiresAt >= now + durationTicks)
				return false;
		}
		UUID previous = existing != null && existing.ironId.equals(iron.getUUID())
				? existing.previousTargetId
				: target.getTarget() == null ? null : target.getTarget().getUUID();
		boolean hardLock = !isBoss(target);
		int color = ironColor(iron);
		String source = TAUNT_HIGHLIGHT_SOURCE_PREFIX
				+ iron.getStringUUID().replace("-", "").substring(0, 12);
		List<UUID> viewers = showHighlight(iron, target, source, color,
				durationTicks);
		TauntState state = new TauntState(level, target.getUUID(),
				iron.getUUID(), previous, now + durationTicks, hardLock,
				source, viewers, color);
		TAUNTS.put(target.getUUID(), state);
		target.setTarget(iron);
		spawnDust(level, target.getBoundingBox().getCenter(), color,
				8, Math.max(0.28D, target.getBbWidth() * 0.35D), 0.025D);
		level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(),
				target.getY() + target.getBbHeight() * 0.65D, target.getZ(),
				3, target.getBbWidth() * 0.25D,
				target.getBbHeight() * 0.18D, target.getBbWidth() * 0.25D,
				0.015D);
		return true;
	}

	private static void tickTaunts() {
		Iterator<TauntState> iterator = TAUNTS.values().iterator();
		while (iterator.hasNext()) {
			TauntState state = iterator.next();
			Entity targetEntity = state.level.getEntity(state.targetId);
			Entity ironEntity = state.level.getEntity(state.ironId);
			long now = state.level.getGameTime();
			if (!(targetEntity instanceof Mob target) || !target.isAlive()
					|| !(ironEntity instanceof ShadowIronEntity iron)
					|| !iron.isAlive() || now >= state.expiresAt) {
				finishTaunt(state, targetEntity instanceof Mob mob ? mob : null,
						ironEntity instanceof ShadowIronEntity shadow ? shadow : null);
				iterator.remove();
				continue;
			}
			if (state.hardLock && (target.tickCount & 3) == 0)
				target.setTarget(iron);
			else if (!state.hardLock
					&& (target.getTarget() == null || !target.getTarget().isAlive()))
				target.setTarget(iron);
			if (now % 10L == Math.floorMod(target.getId(), 10))
				spawnDust(state.level,
						target.position().add(0.0D,
								target.getBbHeight() * 0.72D, 0.0D),
						state.color, 2,
						Math.max(0.18D, target.getBbWidth() * 0.28D), 0.005D);
		}
	}

	private static void finishTaunt(TauntState state, Mob target,
			ShadowIronEntity iron) {
		for (UUID viewerId : state.viewerIds) {
			ServerPlayer viewer = state.level.getServer().getPlayerList()
					.getPlayer(viewerId);
			if (viewer != null)
				EntityHighlightSystem.hide(viewer, state.targetId,
						state.level.dimension(), state.highlightSource);
		}
		if (target == null || iron == null || target.getTarget() != iron)
			return;
		Entity previous = state.previousTargetId == null ? null
				: state.level.getEntity(state.previousTargetId);
		if (previous instanceof LivingEntity living && living.isAlive()
				&& target.canAttack(living))
			target.setTarget(living);
		else
			target.setTarget(null);
	}

	private static List<UUID> showHighlight(ShadowIronEntity iron, Mob target,
			String source, int color, int durationTicks) {
		Player rawOwner = ShadowMonarchManager.getShadowOwnerPlayer(iron);
		if (!(rawOwner instanceof ServerPlayer owner))
			return List.of();
		List<UUID> viewers = new ArrayList<>();
		for (ServerPlayer viewer : PartyService.onlineMembers(owner)) {
			if (viewer.level() != target.level())
				continue;
			EntityHighlightSystem.show(viewer, target, source, color,
					durationTicks, TAUNT_HIGHLIGHT_PRIORITY);
			viewers.add(viewer.getUUID());
		}
		return List.copyOf(viewers);
	}

	private static void tryGuardianRescue(ShadowIronEntity iron) {
		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(iron);
		if (!(owner instanceof ServerPlayer serverOwner) || !owner.isAlive()
				|| !iron.canBlockNow())
			return;
		String command = ShadowMonarchManager.currentShadowCommand(iron);
		boolean protect = ShadowMonarchManager.COMMAND_PROTECT.equals(command);
		if (!protect && !ShadowMonarchManager.COMMAND_DEFAULT.equals(command))
			return;

		Projectile projectile = findIncomingProjectile(iron, owner);
		LivingEntity threat = projectile == null ? findGuardianThreat(iron) : null;
		Entity threatEntity = projectile != null ? projectile : threat;
		if (threatEntity == null)
			return;
		double threatDistance = threatEntity.distanceTo(owner);
		double healthRatio = owner.getHealth()
				/ Math.max(1.0F, owner.getMaxHealth());
		if (!ShadowIronCombatPolicy.shouldEmergencyIntercept(protect,
				healthRatio, threatDistance, projectile != null))
			return;
		Vec3 destination = safeGuardPosition(iron, owner,
				threatEntity.position());
		if (destination == null)
			return;

		Vec3 oldPosition = iron.position();
		float facing = (float) (Mth.atan2(threatEntity.getZ() - destination.z,
				threatEntity.getX() - destination.x) * Mth.RAD_TO_DEG) - 90.0F;
		iron.teleportTo(destination.x, destination.y, destination.z);
		iron.setYRot(facing);
		iron.setYHeadRot(facing);
		iron.setYBodyRot(facing);
		if (!iron.beginBlock(true)) {
			iron.teleportTo(oldPosition.x, oldPosition.y, oldPosition.z);
			return;
		}
		long now = iron.level().getGameTime();
		iron.setNextInterceptAt(now + (protect
				? ShadowIronCombatPolicy.PROTECT_INTERCEPT_COOLDOWN_TICKS
				: ShadowIronCombatPolicy.DEFAULT_INTERCEPT_COOLDOWN_TICKS));
		RESCUES.put(owner.getUUID(), new RescueState(serverOwner.serverLevel(),
				owner.getUUID(), iron.getUUID(), threatEntity.getUUID(), now + 14L));
		if (threat instanceof Mob mob)
			claimTaunt(iron, mob,
					ShadowIronCombatPolicy.passiveChallengeDuration(isBoss(mob)));
		playInterceptEffects(serverOwner.serverLevel(), oldPosition, destination,
				ironColor(iron));
	}

	private static Projectile findIncomingProjectile(ShadowIronEntity iron,
			Player owner) {
		AABB futureTarget = owner.getBoundingBox().inflate(0.45D);
		return owner.level().getEntitiesOfClass(Projectile.class,
				owner.getBoundingBox().inflate(10.0D), projectile -> {
					if (!projectile.isAlive())
						return false;
					Entity projectileOwner = projectile.getOwner();
					if (projectileOwner == null
							|| MageCombatHelper.areAllied(iron, projectileOwner))
						return false;
					Vec3 motion = projectile.getDeltaMovement();
					return motion.lengthSqr() > 0.0025D
							&& futureTarget.clip(projectile.position(),
									projectile.position().add(motion.scale(6.0D)))
									.isPresent();
				}).stream().min(Comparator.comparingDouble(owner::distanceToSqr))
				.orElse(null);
	}

	private static Vec3 safeGuardPosition(ShadowIronEntity iron, Player owner,
			Vec3 threatPosition) {
		Vec3 towardThreat = threatPosition.subtract(owner.position())
				.multiply(1.0D, 0.0D, 1.0D);
		if (towardThreat.lengthSqr() < 1.0E-5D)
			towardThreat = Vec3.directionFromRotation(0.0F, owner.getYRot())
					.multiply(1.0D, 0.0D, 1.0D);
		towardThreat = towardThreat.normalize();
		double[] angles = {0.0D, 35.0D, -35.0D, 70.0D, -70.0D};
		double[] distances = {1.45D, 1.25D, 1.65D};
		for (double distance : distances) {
			for (double angle : angles) {
				Vec3 direction = towardThreat.yRot((float) Math.toRadians(angle));
				for (double yOffset : new double[]{0.0D, 1.0D, -1.0D}) {
					Vec3 candidate = owner.position().add(direction.scale(distance))
							.add(0.0D, yOffset, 0.0D);
					if (isSafeGuardPosition(iron, owner, candidate))
						return candidate;
				}
			}
		}
		return null;
	}

	private static boolean isSafeGuardPosition(ShadowIronEntity iron,
			Player owner, Vec3 candidate) {
		if (!(iron.level() instanceof ServerLevel level))
			return false;
		BlockPos feet = BlockPos.containing(candidate);
		BlockPos floorPos = BlockPos.containing(candidate.x,
				candidate.y - 0.05D, candidate.z);
		if (!level.hasChunkAt(feet)
				|| !level.getWorldBorder().isWithinBounds(feet)
				|| !level.getFluidState(feet).isEmpty())
			return false;
		BlockState floor = level.getBlockState(floorPos);
		if (!floor.isFaceSturdy(level, floorPos, Direction.UP))
			return false;
		AABB moved = iron.getBoundingBox().move(candidate.subtract(iron.position()));
		return !moved.intersects(owner.getBoundingBox().inflate(0.05D))
				&& level.noCollision(iron, moved);
	}

	private static boolean matchesThreat(DamageSource source, UUID threatId) {
		if (source == null || threatId == null)
			return false;
		if (source.getDirectEntity() != null
				&& threatId.equals(source.getDirectEntity().getUUID()))
			return true;
		if (source.getEntity() != null
				&& threatId.equals(source.getEntity().getUUID()))
			return true;
		return source.getDirectEntity() instanceof Projectile projectile
				&& projectile.getOwner() != null
				&& threatId.equals(projectile.getOwner().getUUID());
	}

	private static boolean isMeleeTarget(ShadowIronEntity iron,
			LivingEntity target) {
		return target != null && target.isAlive()
				&& ShadowMonarchManager.canShadowDamage(iron, target)
				&& CombatRangeHelper.surfaceDistance(iron, target)
						<= ShadowIronCombatPolicy.MELEE_REACH + 0.7D
				&& iron.hasLineOfSight(target);
	}

	private static boolean hurtFromIron(ShadowIronEntity iron,
			LivingEntity target, float damage) {
		return damage > 0.0F && ShadowMonarchManager.canShadowDamage(iron, target)
				&& target.hurt(iron.level().damageSources().mobAttack(iron), damage);
	}

	private static boolean inForwardArc(ShadowIronEntity iron,
			LivingEntity target, Vec3 forward) {
		Vec3 toward = target.position().subtract(iron.position())
				.multiply(1.0D, 0.0D, 1.0D);
		return toward.lengthSqr() > 1.0E-5D
				&& forward.dot(toward.normalize()) >= 0.20D;
	}

	private static boolean isElite(LivingEntity target) {
		return DungeonMobLevelAdapter.MobRole.fromString(target.getPersistentData()
				.getString(DungeonMobLevelAdapter.ROLE_TAG))
				== DungeonMobLevelAdapter.MobRole.ELITE;
	}

	private static int ironColor(ShadowIronEntity iron) {
		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(iron);
		int selected = ShadowMonarchManager.glowColor(owner, "iron");
		if (selected != ShadowMonarchManager.NO_GLOW)
			return selected;
		return iron.isDomainBoosted()
				? 0xB75CFF : 0x43C8FF;
	}

	private static void playInterceptEffects(ServerLevel level, Vec3 from,
			Vec3 to, int color) {
		level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.2D, from.z,
				14, 0.35D, 0.75D, 0.35D, 0.08D);
		level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1.2D, to.z,
				18, 0.32D, 0.85D, 0.32D, 0.06D);
		spawnDust(level, to.add(0.0D, 1.75D, 0.0D), color,
				10, 0.38D, 0.025D);
		level.playSound(null, BlockPos.containing(from),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.65F, 0.62F);
		level.playSound(null, BlockPos.containing(to),
				SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.NEUTRAL, 0.95F, 0.72F);
	}

	private static void spawnSweep(ServerLevel level, Vec3 position) {
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, position.x,
				position.y, position.z, 2, 0.36D, 0.25D, 0.36D, 0.0D);
	}

	private static void spawnHit(ServerLevel level, Vec3 position, int color) {
		level.sendParticles(ParticleTypes.CRIT, position.x, position.y,
				position.z, 7, 0.3D, 0.45D, 0.3D, 0.08D);
		spawnDust(level, position, color, 5, 0.25D, 0.02D);
	}

	private static void spawnDust(ServerLevel level, Vec3 position, int rgb,
			int count, double spread, double speed) {
		Vector3f color = new Vector3f(((rgb >> 16) & 0xFF) / 255.0F,
				((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
		level.sendParticles(new DustParticleOptions(color, 1.05F), position.x,
				position.y, position.z, count, spread, spread, spread, speed);
	}

	private static void tickRings() {
		Iterator<RingBurst> iterator = RINGS.iterator();
		while (iterator.hasNext()) {
			RingBurst ring = iterator.next();
			if (ring.age >= 6) {
				iterator.remove();
				continue;
			}
			double radius = 2.0D + ring.age * 1.9D;
			Vector3f color = new Vector3f(((ring.color >> 16) & 0xFF) / 255.0F,
					((ring.color >> 8) & 0xFF) / 255.0F,
					(ring.color & 0xFF) / 255.0F);
			DustParticleOptions dust = new DustParticleOptions(color, 1.15F);
			for (int point = 0; point < 12; point++) {
				double angle = point * Math.PI * 2.0D / 12.0D;
				double x = ring.center.x + Math.cos(angle) * radius;
				double z = ring.center.z + Math.sin(angle) * radius;
				ring.level.sendParticles(dust, x, ring.center.y + 0.16D, z,
						1, 0.02D, 0.02D, 0.02D, 0.0D);
			}
			ring.age++;
		}
	}

	private static final class TauntState {
		private final ServerLevel level;
		private final UUID targetId;
		private final UUID ironId;
		private final UUID previousTargetId;
		private final long expiresAt;
		private final boolean hardLock;
		private final String highlightSource;
		private final List<UUID> viewerIds;
		private final int color;

		private TauntState(ServerLevel level, UUID targetId, UUID ironId,
				UUID previousTargetId, long expiresAt, boolean hardLock,
				String highlightSource, List<UUID> viewerIds, int color) {
			this.level = level;
			this.targetId = targetId;
			this.ironId = ironId;
			this.previousTargetId = previousTargetId;
			this.expiresAt = expiresAt;
			this.hardLock = hardLock;
			this.highlightSource = highlightSource;
			this.viewerIds = viewerIds;
			this.color = color;
		}
	}

	private record RescueState(ServerLevel level, UUID ownerId, UUID ironId,
			UUID threatId, long expiresAt) {
	}

	private static final class RingBurst {
		private final ServerLevel level;
		private final Vec3 center;
		private final int color;
		private int age;

		private RingBurst(ServerLevel level, Vec3 center, int color, int age) {
			this.level = level;
			this.center = center;
			this.color = color;
			this.age = age;
		}
	}
}
