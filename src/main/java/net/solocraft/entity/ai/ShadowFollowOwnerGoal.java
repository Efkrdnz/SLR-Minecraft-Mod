package net.solocraft.entity.ai;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;

/**
 * A command-aware owner follow goal shared by every ground shadow.
 */
public final class ShadowFollowOwnerGoal extends FollowOwnerGoal {
	private final TamableAnimal shadow;

	public ShadowFollowOwnerGoal(TamableAnimal shadow) {
		super(shadow, 1.4D, 8.0F, 3.0F);
		this.shadow = shadow;
	}

	@Override
	public boolean canUse() {
		return ShadowMonarchManager.shouldFollowOwner(shadow)
				&& !hasLiveTarget()
				&& super.canUse();
	}

	@Override
	public boolean canContinueToUse() {
		return ShadowMonarchManager.shouldFollowOwner(shadow)
				&& !hasLiveTarget()
				&& super.canContinueToUse();
	}

	private boolean hasLiveTarget() {
		return shadow.getTarget() != null && shadow.getTarget().isAlive();
	}
}
