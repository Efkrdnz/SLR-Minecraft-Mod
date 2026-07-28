package net.solocraft.procedures;

import net.solocraft.init.SololevelingModEntities;
import net.solocraft.entity.ShamanMagicEntity;
import net.solocraft.entity.GoblinMageEntity;
import net.solocraft.SololevelingMod;
import net.solocraft.util.CombatRangeHelper;

import net.minecraftforge.registries.ForgeRegistries;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.arguments.EntityAnchorArgument;

public class GoblinMageOnEntityTickUpdateProcedure {
	private static final int VOLLEY_SIZE = 3;
	private static final int VOLLEY_SHOT_INTERVAL_TICKS = 7;
	private static final int ATTACK_CYCLE_TICKS = 80;
	private static final float MAGIC_DAMAGE = 4.0F;

	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 0.05) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				entity.getPersistentData().putDouble("AL", (entity.getPersistentData().getDouble("AL") + 1));
				if (entity.getPersistentData().getBoolean("CanShoot")) {
					entity.getPersistentData().putDouble("MF", (entity.getPersistentData().getDouble("MF") + 1));
					if (entity instanceof LivingEntity _entity && !_entity.level().isClientSide())
						_entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 999, 90, false, false));
				} else {
					entity.getPersistentData().putDouble("MF", 0);
					if (entity instanceof LivingEntity _entity)
						_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
				}
				entity.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getX()), ((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getY() + 1.2),
						((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null).getZ())));
				Entity target = entity instanceof Mob _mobEnt ? _mobEnt.getTarget() : null;
				CombatRangeHelper.maintainRangedBand(entity, target, 4.5D, 14.0D, 0.95D);
				if (CombatRangeHelper.withinSurfaceRange(entity, target, 17.0D)
						&& entity instanceof Mob mob && target instanceof LivingEntity livingTarget
						&& mob.getSensing().hasLineOfSight(livingTarget)) {
					entity.getPersistentData().putBoolean("CanShoot", true);
				} else {
					entity.getPersistentData().putBoolean("CanShoot", false);
				}
			} else {
				entity.getPersistentData().putDouble("MF", 0);
				entity.getPersistentData().putBoolean("CanShoot", false);
				if (entity instanceof LivingEntity _entity)
					_entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
			}
			if (entity.getPersistentData().getDouble("MF") == 5) {
				if (entity instanceof GoblinMageEntity) {
					((GoblinMageEntity) entity).setAnimation("attack");
				}
			}
			if (entity.getPersistentData().getDouble("MF") == 13) {
				if (entity.level() instanceof ServerLevel serverLevel) {
					for (int shot = 1; shot <= VOLLEY_SIZE; shot++)
						SololevelingMod.queueServerWork(serverLevel.getServer(),
								shot * VOLLEY_SHOT_INTERVAL_TICKS, () -> fireBolt(entity));
				}
			}
			if (entity.getPersistentData().getDouble("MF") >= ATTACK_CYCLE_TICKS) {
				entity.getPersistentData().putDouble("MF", 0);
			}
		}
	}

	private static void fireBolt(Entity shooter) {
		if (!(shooter instanceof Mob mob) || !shooter.isAlive() || shooter.level().isClientSide())
			return;
		LivingEntity target = mob.getTarget();
		if (target == null || !target.isAlive() || target.level() != shooter.level()
				|| !mob.getSensing().hasLineOfSight(target)
				|| !CombatRangeHelper.withinSurfaceRange(shooter, target, 17.0D))
			return;

		Level projectileLevel = shooter.level();
		AbstractArrow bolt = new ShamanMagicEntity(SololevelingModEntities.SHAMAN_MAGIC.get(), projectileLevel);
		bolt.setOwner(shooter);
		bolt.setBaseDamage(MAGIC_DAMAGE);
		bolt.setKnockback(0);
		bolt.setSilent(true);
		bolt.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
		Vec3 aim = new Vec3(target.getX(), target.getY() + target.getBbHeight() * 0.65D, target.getZ())
				.subtract(bolt.position());
		bolt.shoot(aim.x, aim.y, aim.z, 0.25F, 0);
		projectileLevel.addFreshEntity(bolt);
		projectileLevel.playSound(null, shooter.blockPosition(),
				ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("entity.experience_orb.pickup")),
				SoundSource.NEUTRAL, 1, 0.5F);
	}
}
