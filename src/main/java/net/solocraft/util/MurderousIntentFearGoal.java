package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.dungeon.runtime.DungeonLevelHelper;
import net.solocraft.network.SololevelingModVariables;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.UUID;

/**
 * Short-lived, cast-scoped panic behavior for lower-rank mobs struck by
 * Murderous Intent. Goals are attached only to affected mobs and removed when
 * the four-second fear window ends, avoiding a global entity-tick scan.
 */
public final class MurderousIntentFearGoal extends Goal {
	private static final double FLEE_SPEED = 1.65D;
	private static final double MAX_CASTER_DISTANCE_SQR = 48.0D * 48.0D;

	private final PathfinderMob mob;
	private final UUID casterId;
	private final long expiresAt;
	private long nextPathTick;

	private MurderousIntentFearGoal(PathfinderMob mob, ServerPlayer caster, int durationTicks) {
		this.mob = mob;
		this.casterId = caster.getUUID();
		this.expiresAt = mob.level().getGameTime() + durationTicks;
		setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
	}

	/** Returns true when the target qualified for and received the panic behavior. */
	public static boolean apply(ServerPlayer caster, PathfinderMob mob, int durationTicks) {
		if (caster == null || mob == null || durationTicks <= 0 || mob.isNoAi()
				|| caster.isAlliedTo(mob) || mob.isAlliedTo(caster)
				|| ShadowMonarchManager.isOwnedShadow(mob, caster)
				|| mob instanceof TamableAnimal tame && caster.getUUID().equals(tame.getOwnerUUID()))
			return false;

		SololevelingModVariables.PlayerVariables vars = caster
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		double casterLevel = DungeonLevelHelper.playerLevel(caster);
		double targetLevel = DungeonLevelHelper.levelOf(mob);
		boolean weaker = casterLevel > 0.0D && targetLevel > 0.0D
				? targetLevel < casterLevel
				: CombatRankHelper.rankOf(mob) < Math.max(0, Math.min(6, (int) Math.round(vars.HunterRank)));
		if (!weaker)
			return false;

		MurderousIntentFearGoal goal = new MurderousIntentFearGoal(mob, caster, durationTicks);
		mob.goalSelector.addGoal(0, goal);
		SololevelingMod.queueServerWork(caster.server, durationTicks,
				() -> mob.goalSelector.removeGoal(goal));
		return true;
	}

	@Override
	public boolean canUse() {
		ServerPlayer caster = caster();
		return caster != null && mob.isAlive() && mob.distanceToSqr(caster) <= MAX_CASTER_DISTANCE_SQR
				&& chooseEscape(caster);
	}

	@Override
	public boolean canContinueToUse() {
		ServerPlayer caster = caster();
		return caster != null && mob.isAlive() && mob.level().getGameTime() < expiresAt
				&& mob.distanceToSqr(caster) <= MAX_CASTER_DISTANCE_SQR;
	}

	@Override
	public void start() {
		dropCombatTarget();
		nextPathTick = 0L;
	}

	@Override
	public void tick() {
		ServerPlayer caster = caster();
		if (caster == null)
			return;
		dropCombatTarget();
		long now = mob.level().getGameTime();
		if (mob.getNavigation().isDone() || now >= nextPathTick) {
			nextPathTick = now + 8L + Math.floorMod(mob.getId(), 4);
			chooseEscape(caster);
		}
	}

	@Override
	public void stop() {
		mob.getNavigation().stop();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	private void dropCombatTarget() {
		mob.setTarget(null);
		mob.setAggressive(false);
	}

	private boolean chooseEscape(LivingEntity caster) {
		double currentDistance = mob.distanceToSqr(caster);
		for (int attempt = 0; attempt < 4; attempt++) {
			Vec3 escape = DefaultRandomPos.getPosAway(mob, 16, 7, caster.position());
			if (escape != null && escape.distanceToSqr(caster.position()) > currentDistance
					&& mob.getNavigation().moveTo(escape.x, escape.y, escape.z, FLEE_SPEED))
				return true;
		}

		Vec3 away = mob.position().subtract(caster.position());
		away = new Vec3(away.x, 0.0D, away.z);
		if (away.lengthSqr() < 1.0E-5D)
			away = new Vec3(1.0D, 0.0D, 0.0D);
		Vec3 fallback = mob.position().add(away.normalize().scale(12.0D));
		mob.getMoveControl().setWantedPosition(fallback.x, fallback.y, fallback.z, FLEE_SPEED);
		return true;
	}

	private ServerPlayer caster() {
		if (!(mob.level() instanceof ServerLevel level) || mob.level().getGameTime() >= expiresAt)
			return null;
		ServerPlayer caster = level.getServer().getPlayerList().getPlayer(casterId);
		return caster != null && caster.level() == level && caster.isAlive() && !caster.isSpectator()
				? caster : null;
	}
}
