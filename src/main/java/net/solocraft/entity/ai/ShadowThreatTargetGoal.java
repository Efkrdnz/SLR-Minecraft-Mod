package net.solocraft.entity.ai;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Lets hostile mobs respond to the Shadow Monarch's front line instead of
 * tunnelling the player forever. A shadow must build meaningful recent threat
 * before it pulls aggro, and the resulting lock prevents a lower-priority
 * nearest-player goal from immediately taking the target back.
 */
public final class ShadowThreatTargetGoal extends Goal {
	private static final int HITS_TO_PULL_AGGRO = 2;
	private static final int GROUP_HITS_TO_PULL_AGGRO = 3;
	private static final int THREAT_WINDOW_TICKS = 80;
	private static final int TARGET_LOCK_TICKS = 120;
	private static final float MINIMUM_DAMAGE_TO_PULL = 6.0F;
	private static final float MAX_HEALTH_DAMAGE_FRACTION = 0.10F;

	private final Mob mob;
	private final Map<UUID, Threat> recentThreat = new HashMap<>();
	private LivingEntity lockedTarget;
	private long lockedUntil;

	public ShadowThreatTargetGoal(Mob mob) {
		this.mob = mob;
		setFlags(EnumSet.of(Flag.TARGET));
	}

	/**
	 * Records a successful hit and immediately switches targets once that
	 * shadow's recent hit count or dealt damage crosses the pull threshold.
	 */
	public void recordSuccessfulHit(DamageSource source, float dealtDamage) {
		if (source == null || mob.level().isClientSide() || !mob.isAlive())
			return;
		LivingEntity attacker = findShadowAttacker(source);
		if (!isUsableTarget(attacker))
			return;

		long now = mob.level().getGameTime();
		pruneExpiredThreat(now);
		if (attacker == lockedTarget && now < lockedUntil) {
			lockedUntil = now + TARGET_LOCK_TICKS;
			mob.setTarget(attacker);
			return;
		}

		Threat threat = recentThreat.computeIfAbsent(attacker.getUUID(),
				ignored -> new Threat(attacker));
		if (now - threat.lastHitTick > THREAT_WINDOW_TICKS)
			threat.reset(attacker);
		threat.attacker = attacker;
		threat.hits++;
		threat.damage += Math.max(0.0F, dealtDamage);
		threat.lastHitTick = now;

		float damageThreshold = Math.max(MINIMUM_DAMAGE_TO_PULL,
				mob.getMaxHealth() * MAX_HEALTH_DAMAGE_FRACTION);
		int groupHits = 0;
		float groupDamage = 0.0F;
		Threat highestThreat = threat;
		for (Threat candidate : recentThreat.values()) {
			groupHits += candidate.hits;
			groupDamage += candidate.damage;
			if (candidate.score() > highestThreat.score())
				highestThreat = candidate;
		}
		if (threat.hits >= HITS_TO_PULL_AGGRO
				|| threat.damage >= damageThreshold
				|| groupHits >= GROUP_HITS_TO_PULL_AGGRO
				|| groupDamage >= damageThreshold * 1.5F) {
			lockedTarget = highestThreat.attacker;
			lockedUntil = now + TARGET_LOCK_TICKS;
			recentThreat.clear();
			mob.setTarget(lockedTarget);
		}
	}

	@Override
	public boolean canUse() {
		return hasActiveLock();
	}

	@Override
	public boolean canContinueToUse() {
		return hasActiveLock();
	}

	@Override
	public void start() {
		if (lockedTarget != null)
			mob.setTarget(lockedTarget);
	}

	@Override
	public void tick() {
		if (lockedTarget != null)
			mob.setTarget(lockedTarget);
	}

	@Override
	public void stop() {
		if (mob.getTarget() == lockedTarget)
			mob.setTarget(null);
		lockedTarget = null;
		lockedUntil = 0L;
	}

	private boolean hasActiveLock() {
		return mob.level().getGameTime() < lockedUntil
				&& isUsableTarget(lockedTarget);
	}

	private boolean isUsableTarget(LivingEntity target) {
		return target != null && target != mob && target.isAlive()
				&& target.isAttackable() && !target.isInvulnerable()
				&& (ShadowMonarchManager.isShadowEntity(target)
						|| ShadowMonarchManager.isTrackedShadowEntity(target));
	}

	private LivingEntity findShadowAttacker(DamageSource source) {
		LivingEntity attacker = asShadow(source.getEntity());
		if (attacker != null)
			return attacker;
		attacker = asShadow(source.getDirectEntity());
		if (attacker != null)
			return attacker;
		if (source.getDirectEntity() instanceof Projectile projectile)
			return asShadow(projectile.getOwner());
		return null;
	}

	private LivingEntity asShadow(Entity candidate) {
		if (!(candidate instanceof LivingEntity living))
			return null;
		return ShadowMonarchManager.isShadowEntity(living)
				|| ShadowMonarchManager.isTrackedShadowEntity(living)
						? living : null;
	}

	private void pruneExpiredThreat(long now) {
		Iterator<Threat> entries = recentThreat.values().iterator();
		while (entries.hasNext()) {
			Threat threat = entries.next();
			if (now - threat.lastHitTick > THREAT_WINDOW_TICKS
					|| threat.attacker == null
					|| !threat.attacker.isAlive())
				entries.remove();
		}
	}

	private static final class Threat {
		private LivingEntity attacker;
		private int hits;
		private float damage;
		private long lastHitTick;

		private Threat(LivingEntity attacker) {
			reset(attacker);
		}

		private void reset(LivingEntity attacker) {
			this.attacker = attacker;
			hits = 0;
			damage = 0.0F;
			lastHitTick = Long.MIN_VALUE;
		}

		private float score() {
			return damage + hits * (MINIMUM_DAMAGE_TO_PULL * 0.5F);
		}
	}
}
