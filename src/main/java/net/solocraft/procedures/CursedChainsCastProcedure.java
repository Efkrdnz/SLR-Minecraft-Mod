package net.solocraft.procedures;

import net.solocraft.entity.CursedChainsEntity;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.util.MageCombatHelper;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CursedChainsCastProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (!(world instanceof ServerLevel level) || entity == null)
			return;
		Vec3 start = entity.getEyePosition();
		Vec3 end = start.add(entity.getLookAngle().normalize().scale(15.0D));
		HitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
		if (blockHit.getType() != HitResult.Type.MISS)
			end = blockHit.getLocation();
		AABB search = entity.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(entity, start, end, search,
				candidate -> MageCombatHelper.isValidTarget(entity, candidate), start.distanceToSqr(end));
		if (hit == null)
			return;
		spawnChains(level, hit.getEntity());
	}

	private static void spawnChains(ServerLevel level, Entity target) {
		Entity chains = SololevelingModEntities.CURSED_CHAINS.get().create(level);
		if (chains == null)
			return;
		chains.moveTo(target.getX(), target.getY(), target.getZ(), level.getRandom().nextFloat() * 360.0F, 0.0F);
		if (chains instanceof Mob mob)
			mob.finalizeSpawn(level, level.getCurrentDifficultyAt(chains.blockPosition()), MobSpawnType.MOB_SUMMONED, null, null);
		if (chains instanceof CursedChainsEntity cursedChains)
			cursedChains.getEntityData().set(CursedChainsEntity.DATA_target, target.getDisplayName().getString());
		level.addFreshEntity(chains);
	}
}
