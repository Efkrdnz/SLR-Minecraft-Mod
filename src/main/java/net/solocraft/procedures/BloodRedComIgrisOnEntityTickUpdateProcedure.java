package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;

public class BloodRedComIgrisOnEntityTickUpdateProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		double rand = 0;
		double hei = 0;
		double rand2 = 0;
		if ((entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1) > 0) {
			if (!((entity instanceof Mob _mobEnt ? (Entity) _mobEnt.getTarget() : null) == null)) {
				if (entity instanceof Mob _mob && _mob.getTarget() != null) {
					LivingEntity target = _mob.getTarget();
					double deltaX = target.getX() - entity.getX();
					double deltaZ = target.getZ() - entity.getZ();
					float targetYaw = (float) (Math.toDegrees(Math.atan2(deltaZ, deltaX))) - 90.0F;
					entity.setYRot(targetYaw);
					entity.yRotO = targetYaw;
					if (entity instanceof LivingEntity _livingEntity) {
						_livingEntity.yBodyRot = targetYaw;
						_livingEntity.yHeadRot = targetYaw;
					}
				}
				entity.getPersistentData().putDouble("MF", (entity.getPersistentData().getDouble("MF") + 1));
			} else {
				entity.getPersistentData().putDouble("MF", 0);
				entity.getPersistentData().putString("state", "idle");
			}
			if ((entity.getPersistentData().getString("state")).equals("idle")) {
				if (entity.getPersistentData().getDouble("MF") == 10) {
					IgrisStateChangerProcedure.execute(entity);
				}
			}
			if ((entity.getPersistentData().getString("state")).equals("spin")) {
				IgrisSpinProcedure.execute(world, x, y, z, entity);
			}
			if ((entity.getPersistentData().getString("state")).equals("stab")) {
				IgrisStabProcedure.execute(world, x, y, z, entity);
			}
			if ((entity.getPersistentData().getString("state")).equals("slam")) {
				IgrisSlamProcedure.execute(world, x, y, z, entity);
			}
			if ((entity.getPersistentData().getString("state")).equals("scream")) {
				IgrisScreamProcedure.execute(world, x, y, z, entity);
			}
		} else {
			entity.getPersistentData().putDouble("MF", 0);
		}
		if ((entity instanceof TamableAnimal _tamEnt ? _tamEnt.isTame() : false) && entity.getType().is(TagKey.create(Registries.ENTITY_TYPE, new ResourceLocation("shadows")))) {
			if (!((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null) == null)) {
				if (((entity instanceof TamableAnimal _tamEnt ? (Entity) _tamEnt.getOwner() : null).getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
						.orElse(new SololevelingModVariables.PlayerVariables())).Call4Death == true) {
					if (world instanceof ServerLevel _level)
						_level.sendParticles(ParticleTypes.SMOKE, (entity.getX()), (entity.getY()), (entity.getZ()), 30, 0.05, 0.05, 0.05, 1);
					if (!entity.level().isClientSide())
						entity.discard();
				}
			}
		}
	}
}
