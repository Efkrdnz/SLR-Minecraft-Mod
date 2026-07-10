package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.FireArrowEntity;
import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Mth;
import net.minecraft.server.level.ServerLevel;
import net.solocraft.util.CooldownManager;

public class FireArrowCastProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		double inteligence = 0;
		double delay = 0;
		if (!world.isClientSide()) {
			if (!entity.isShiftKeyDown()) {
				CooldownManager.set(entity, "Fire Rain", 100);
				inteligence = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).Intelligence;
				entity.getPersistentData().putDouble("RainOfFire", 0);
				entity.getPersistentData().putDouble("RainOfFireX", (entity.getLookAngle().x));
				entity.getPersistentData().putDouble("RainOfFireY", (entity.getLookAngle().y));
				entity.getPersistentData().putDouble("RainOfFireZ", (entity.getLookAngle().z));
				for (int index0 = 0; index0 < 30; index0++) {
					SololevelingMod.queueServerWork((int) delay, () -> {
						entity.getPersistentData().putDouble("RainOfFire", (entity.getPersistentData().getDouble("RainOfFire") + 1.5));
						for (int index1 = 0; index1 < Mth.nextInt(RandomSource.create(), 2, 5); index1++) {
							if (world instanceof ServerLevel projectileLevel) {
								Projectile _entityToSpawn = new Object() {
									public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
										AbstractArrow entityToSpawn = new FireArrowEntity(SololevelingModEntities.FIRE_ARROW.get(), level);
										entityToSpawn.setOwner(shooter);
										entityToSpawn.setBaseDamage(damage);
										entityToSpawn.setKnockback(knockback);
										entityToSpawn.setSilent(true);
										return entityToSpawn;
									}
								}.getArrow(projectileLevel, entity, 0, 1);
								_entityToSpawn.setPos(
										(entity.getX() + entity.getPersistentData().getDouble("RainOfFireX") * 3 + entity.getPersistentData().getDouble("RainOfFire") * entity.getPersistentData().getDouble("RainOfFireX")
												+ Mth.nextDouble(RandomSource.create(), -4, 4)),
										(entity.getY() + 1.6 + entity.getPersistentData().getDouble("RainOfFire") * entity.getPersistentData().getDouble("RainOfFireY") + 10), (entity.getZ() + entity.getPersistentData().getDouble("RainOfFireX") * 3
												+ entity.getPersistentData().getDouble("RainOfFire") * entity.getPersistentData().getDouble("RainOfFireZ") + Mth.nextDouble(RandomSource.create(), -4, 4)));
								_entityToSpawn.shoot(0, (-1), 0, 1, 30);
								projectileLevel.addFreshEntity(_entityToSpawn);
							}
						}
					});
					delay = delay + 1;
				}
			} else {
				for (int index2 = 0; index2 < 33; index2++) {
					SololevelingMod.queueServerWork((int) delay, () -> {
						for (int index3 = 0; index3 < Mth.nextInt(RandomSource.create(), 3, 7); index3++) {
							if (world instanceof ServerLevel projectileLevel) {
								Projectile _entityToSpawn = new Object() {
									public Projectile getArrow(Level level, Entity shooter, float damage, int knockback) {
										AbstractArrow entityToSpawn = new FireArrowEntity(SololevelingModEntities.FIRE_ARROW.get(), level);
										entityToSpawn.setOwner(shooter);
										entityToSpawn.setBaseDamage(damage);
										entityToSpawn.setKnockback(knockback);
										entityToSpawn.setSilent(true);
										return entityToSpawn;
									}
								}.getArrow(projectileLevel, entity, 0, 1);
								_entityToSpawn.setPos((entity.getX() + Mth.nextDouble(RandomSource.create(), -12, 12)), (entity.getY() + 10), (entity.getZ() + Mth.nextDouble(RandomSource.create(), -12, 12)));
								_entityToSpawn.shoot(0, (-1), 0, 1, 30);
								projectileLevel.addFreshEntity(_entityToSpawn);
							}
						}
					});
					delay = delay + 1;
				}
			}
		}
	}
}
