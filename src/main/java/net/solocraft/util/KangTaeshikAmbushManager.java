package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.KangTaeshikEntity;
import net.solocraft.init.SololevelingModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class KangTaeshikAmbushManager {
	public static final String AMBUSH_TAG = "SLRKangTaeshikAmbush";
	public static final String OWNER_TAG = "SLRKangTaeshikTarget";

	private static final String PENDING_TAG = "SLRKangTaeshikAmbushPending";
	private static final String COOLDOWN_TAG = "SLRKangTaeshikAmbushCooldown";
	private static final double AMBUSH_CHANCE = 0.10D;
	private static final long AMBUSH_COOLDOWN = 10L * 60L * 20L;

	private KangTaeshikAmbushManager() {
	}

	public static void trySchedule(ServerPlayer player, Entity defeatedBoss) {
		if (player == null || defeatedBoss == null || !player.isAlive() || !SystemPlayerAccess.hasSystem(player))
			return;
		if (!isEligibleDungeonBoss(defeatedBoss) || player.getPersistentData().getBoolean(PENDING_TAG))
			return;
		long now = player.level().getGameTime();
		if (now < player.getPersistentData().getLong(COOLDOWN_TAG) || player.getRandom().nextDouble() >= AMBUSH_CHANCE)
			return;

		player.getPersistentData().putBoolean(PENDING_TAG, true);
		SololevelingMod.queueServerWork(25, () -> spawnAmbush(player));
	}

	public static boolean isAmbush(KangTaeshikEntity kang) {
		return kang != null && kang.getPersistentData().getBoolean(AMBUSH_TAG) && kang.getPersistentData().hasUUID(OWNER_TAG);
	}

	public static UUID ownerId(KangTaeshikEntity kang) {
		return isAmbush(kang) ? kang.getPersistentData().getUUID(OWNER_TAG) : null;
	}

	private static boolean isEligibleDungeonBoss(Entity boss) {
		ResourceLocation dimension = boss.level().dimension().location();
		if (!"sololeveling".equals(dimension.getNamespace()) || !dimension.getPath().startsWith("dungeon_dimension"))
			return false;
		if ("dungeon_dimension_igris".equals(dimension.getPath()))
			return false;
		return CombatRankHelper.rankOf(boss) <= 4;
	}

	private static void spawnAmbush(ServerPlayer player) {
		player.getPersistentData().remove(PENDING_TAG);
		if (!player.isAlive() || player.isRemoved() || !(player.level() instanceof ServerLevel level) || !SystemPlayerAccess.hasSystem(player))
			return;
		if (UrgentQuestManager.hasActiveQuest(player))
			return;
		BlockPos spawnPos = findSafeSpawn(level, player);
		if (spawnPos == null)
			return;

		KangTaeshikEntity kang = SololevelingModEntities.KANG_TAESHIK.get().create(level);
		if (kang == null)
			return;
		kang.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D, player.getYRot(), 0.0F);
		kang.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.EVENT, null, null);
		kang.getPersistentData().putBoolean(AMBUSH_TAG, true);
		kang.getPersistentData().putUUID(OWNER_TAG, player.getUUID());
		kang.setPersistenceRequired();
		if (!level.addFreshEntity(kang))
			return;
		if (!UrgentQuestManager.startKangAmbushQuest(player, kang)) {
			kang.discard();
			return;
		}

		kang.setTarget(player);
		kang.getNavigation().moveTo(player, 1.35D);
		Vec3 leap = player.position().subtract(kang.position());
		if (leap.lengthSqr() > 0.001D) {
			leap = leap.normalize();
			kang.setDeltaMovement(leap.x * 0.85D, 0.32D, leap.z * 0.85D);
		}
		player.getPersistentData().putLong(COOLDOWN_TAG, level.getGameTime() + AMBUSH_COOLDOWN);
	}

	private static BlockPos findSafeSpawn(ServerLevel level, ServerPlayer player) {
		Vec3 forward = new Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
		if (forward.lengthSqr() < 0.001D)
			forward = new Vec3(0.0D, 0.0D, 1.0D);
		forward = forward.normalize();
		Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
		Vec3[] offsets = {
				forward.scale(-6.0D), forward.scale(-8.0D),
				right.scale(6.0D), right.scale(-6.0D),
				forward.scale(-5.0D).add(right.scale(3.0D)),
				forward.scale(-5.0D).add(right.scale(-3.0D))
		};
		for (Vec3 offset : offsets) {
			int baseX = (int) Math.floor(player.getX() + offset.x);
			int baseZ = (int) Math.floor(player.getZ() + offset.z);
			for (int dy = 2; dy >= -4; dy--) {
				BlockPos feet = new BlockPos(baseX, player.getBlockY() + dy, baseZ);
				if (isSafeStandingPos(level, feet))
					return feet;
			}
		}
		return null;
	}

	private static boolean isSafeStandingPos(ServerLevel level, BlockPos feet) {
		BlockPos floor = feet.below();
		return level.hasChunkAt(feet)
				&& level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
				&& level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
				&& level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
				&& level.getFluidState(feet).isEmpty()
				&& level.getFluidState(feet.above()).isEmpty();
	}
}
