package net.solocraft.util;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

public final class IgrisCombatTeleportHelper {
	private static final double[] GROUND_Y_OFFSETS = { 0.0D, 1.0D, -1.0D, 2.0D, -2.0D };
	private static final double[] ELEVATED_Y_OFFSETS = { 0.0D, -1.0D, -2.0D, 1.0D, -3.0D };
	private static final double BORDER_EPSILON = 0.001D;
	private static final double SUPPORT_CHECK_DEPTH = 0.08D;

	private IgrisCombatTeleportHelper() {
	}

	public static boolean tryDodgeAttacker(Entity igris, Entity attacker) {
		return tryTeleport(igris, attacker, Placement.DODGE_ATTACKER);
	}

	public static boolean tryMoveBehindTarget(Entity igris, Entity target) {
		return tryTeleport(igris, target, Placement.BEHIND_TARGET);
	}

	public static boolean tryElevatedReposition(Entity igris, Entity target) {
		return tryTeleport(igris, target, Placement.ELEVATED);
	}

	private static boolean tryTeleport(Entity igris, Entity anchor, Placement placement) {
		if (!(igris.level() instanceof ServerLevel level) || anchor.level() != level
				|| igris.isRemoved() || anchor.isRemoved())
			return false;

		double[] yOffsets = placement == Placement.ELEVATED ? ELEVATED_Y_OFFSETS : GROUND_Y_OFFSETS;
		for (Vec3 base : buildCandidates(igris, anchor, placement)) {
			for (double yOffset : yOffsets) {
				Vec3 destination = base.add(0.0D, yOffset, 0.0D);
				if (!isSafeDestination(level, igris, destination))
					continue;
				completeTeleport(igris, anchor, destination);
				return true;
			}
		}
		return false;
	}

	private static List<Vec3> buildCandidates(Entity igris, Entity anchor, Placement placement) {
		List<Vec3> candidates = new ArrayList<>(13);
		Vec3 forward = horizontalDirection(anchor.getLookAngle());
		if (forward.lengthSqr() < 0.0001D)
			forward = horizontalDirection(igris.position().subtract(anchor.position()));
		if (forward.lengthSqr() < 0.0001D)
			forward = new Vec3(0.0D, 0.0D, 1.0D);

		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3 primary = placement == Placement.DODGE_ATTACKER ? forward : forward.scale(-1.0D);
		double anchorY = anchor.getY() + (placement == Placement.ELEVATED ? 2.0D : 0.0D);
		Vec3 center = new Vec3(anchor.getX(), anchorY, anchor.getZ());
		double clearance = Math.max(1.5D, (igris.getBbWidth() + anchor.getBbWidth()) * 0.5D + 0.65D);
		if (placement == Placement.BEHIND_TARGET)
			clearance = Math.max(2.0D, clearance);

		if (placement == Placement.ELEVATED)
			candidates.add(center);
		addDirectionalCandidates(candidates, center, primary, right, clearance);
		addDirectionalCandidates(candidates, center, primary, right, clearance + 0.85D);
		return candidates;
	}

	private static void addDirectionalCandidates(List<Vec3> candidates, Vec3 center, Vec3 primary,
			Vec3 right, double distance) {
		candidates.add(center.add(primary.scale(distance)));
		candidates.add(center.add(primary.add(right.scale(0.65D)).normalize().scale(distance)));
		candidates.add(center.add(primary.add(right.scale(-0.65D)).normalize().scale(distance)));
		candidates.add(center.add(right.scale(distance)));
		candidates.add(center.add(right.scale(-distance)));
		candidates.add(center.add(primary.scale(-distance)));
	}

	private static boolean isSafeDestination(ServerLevel level, Entity igris, Vec3 destination) {
		AABB moved = igris.getBoundingBox().move(destination.subtract(igris.position()));
		if (moved.minY < level.getMinBuildHeight()
				|| moved.maxY > level.getMaxBuildHeight()
				|| !isLoadedAndInsideBorder(level, moved)
				|| !level.noCollision(igris, moved)
				|| level.containsAnyLiquid(moved))
			return false;

		BlockPos feet = BlockPos.containing(destination.x, destination.y + 0.01D, destination.z);
		BlockPos support = BlockPos.containing(destination.x, destination.y - 0.01D, destination.z);
		BlockState feetState = level.getBlockState(feet);
		BlockState supportState = level.getBlockState(support);
		if (isHazardous(feetState) || isHazardous(supportState)
				|| !level.getFluidState(feet).isEmpty()
				|| !level.getFluidState(support).isEmpty()
				|| supportState.getCollisionShape(level, support).isEmpty()
				|| level.noCollision(igris, moved.move(0.0D, -SUPPORT_CHECK_DEPTH, 0.0D)))
			return false;

		AABB occupied = moved.deflate(0.05D);
		return level.getEntitiesOfClass(LivingEntity.class, occupied,
				other -> other != igris && other.isAlive()).isEmpty();
	}

	private static boolean isLoadedAndInsideBorder(ServerLevel level, AABB bounds) {
		double minX = bounds.minX + BORDER_EPSILON;
		double maxX = bounds.maxX - BORDER_EPSILON;
		double minZ = bounds.minZ + BORDER_EPSILON;
		double maxZ = bounds.maxZ - BORDER_EPSILON;
		double minY = bounds.minY + BORDER_EPSILON;
		double maxY = bounds.maxY - BORDER_EPSILON;
		BlockPos[] corners = {
				BlockPos.containing(minX, minY, minZ),
				BlockPos.containing(minX, maxY, maxZ),
				BlockPos.containing(maxX, minY, minZ),
				BlockPos.containing(maxX, maxY, maxZ)
		};
		for (BlockPos corner : corners) {
			if (!level.hasChunkAt(corner) || !level.getWorldBorder().isWithinBounds(corner))
				return false;
		}
		return true;
	}

	private static boolean isHazardous(BlockState state) {
		return state.is(BlockTags.FIRE)
				|| state.is(Blocks.CACTUS)
				|| state.is(Blocks.MAGMA_BLOCK)
				|| state.is(Blocks.CAMPFIRE)
				|| state.is(Blocks.SOUL_CAMPFIRE)
				|| state.is(Blocks.SWEET_BERRY_BUSH)
				|| state.is(Blocks.WITHER_ROSE)
				|| state.is(Blocks.POWDER_SNOW);
	}

	private static void completeTeleport(Entity igris, Entity anchor, Vec3 destination) {
		if (igris instanceof Mob mob)
			mob.getNavigation().stop();
		igris.teleportTo(destination.x, destination.y, destination.z);
		igris.setDeltaMovement(Vec3.ZERO);
		igris.fallDistance = 0.0F;
		igris.setOnGround(true);
		igris.lookAt(EntityAnchorArgument.Anchor.EYES, anchor.getEyePosition());
	}

	private static Vec3 horizontalDirection(Vec3 direction) {
		Vec3 horizontal = new Vec3(direction.x, 0.0D, direction.z);
		return horizontal.lengthSqr() < 0.0001D ? Vec3.ZERO : horizontal.normalize();
	}

	private enum Placement {
		DODGE_ATTACKER,
		BEHIND_TARGET,
		ELEVATED
	}
}
