package net.solocraft.entity.ai;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Makes the shared shadow command coordinator authoritative over generated
 * per-entity target goals. Holding the TARGET flag prevents a low-priority
 * nearest-mob or retaliation goal from silently replacing Follow, Protect,
 * Default, or Clear Dungeon assignments.
 */
public final class ShadowCommandTargetGoal extends Goal {
	private final Mob shadow;

	public ShadowCommandTargetGoal(Mob shadow) {
		this.shadow = shadow;
		setFlags(EnumSet.of(Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		return hasLiveOwner();
	}

	@Override
	public boolean canContinueToUse() {
		return hasLiveOwner();
	}

	@Override
	public void start() {
		ShadowMonarchManager.tickShadowTargeting(shadow);
	}

	@Override
	public void tick() {
		ShadowMonarchManager.tickShadowTargeting(shadow);
	}

	@Override
	public void stop() {
		shadow.setTarget(null);
		shadow.getNavigation().stop();
	}

	private boolean hasLiveOwner() {
		Player owner = ShadowMonarchManager.getShadowOwnerPlayer(shadow);
		return ShadowMonarchManager.isShadowEntity(shadow)
				&& owner != null && owner.isAlive();
	}
}
