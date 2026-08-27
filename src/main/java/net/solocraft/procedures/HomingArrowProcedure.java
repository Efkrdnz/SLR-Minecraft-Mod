package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.HomingFlameArrowEntity;
import net.solocraft.util.TemporaryStatBonusManager;

import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Entity;

public class HomingArrowProcedure {
	public static void execute(Entity entity) {
		if (entity == null)
			return;
		{
			Entity _shootFrom = entity;
			Level projectileLevel = _shootFrom.level();
			if (!projectileLevel.isClientSide()) {
				Projectile _entityToSpawn = new Object() {
					public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
						AbstractArrow entityToSpawn = new HomingFlameArrowEntity(SololevelingModEntities.HOMING_FLAME_ARROW.get(), level);
						entityToSpawn.setOwner(shooter);
						entityToSpawn.setBaseDamage(damage);
						net.solocraft.entity.LegacyProjectileCompat.setKnockback(entityToSpawn, knockback);
						entityToSpawn.setSilent(true);
						entityToSpawn.igniteForSeconds(100);
						entityToSpawn.setCritArrow(true);
						return entityToSpawn;
					}
				}.getArrow(projectileLevel, entity,
						(float) (3 + TemporaryStatBonusManager.effectiveIntelligence(entity) / 22), (int) 0.2);
				_entityToSpawn.setPos(_shootFrom.getX(), _shootFrom.getEyeY() - 0.1, _shootFrom.getZ());
				_entityToSpawn.shoot(_shootFrom.getLookAngle().x, _shootFrom.getLookAngle().y, _shootFrom.getLookAngle().z, 2, 0);
				projectileLevel.addFreshEntity(_entityToSpawn);
			}
		}
	}
}
