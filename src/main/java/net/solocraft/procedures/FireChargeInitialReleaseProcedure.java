package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.solocraft.util.CooldownManager;

public class FireChargeInitialReleaseProcedure {
	public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
		if (entity == null)
			return;
		if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).FireVar == 0) {
			FireReleaseSpreadProcedure.execute(world, x, y, z, entity);
			CooldownManager.set(entity, "mana_refresh", 120);
			CooldownManager.set(entity, "job_1", 180);
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).FireVar == 1) {
			{
				double _setval = entity.getX();
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.FX = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				double _setval = entity.getY();
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.FY = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				double _setval = entity.getZ();
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.FZ = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			{
				boolean _setval = true;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.FRing = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).firecharge > 0
					&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).firecharge <= 20) {
				{
					double _setval = 1;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.firestr = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				CooldownManager.set(entity, "mana_refresh", 270);
				CooldownManager.set(entity, "job_1", 300);
				{
					double _setval = 120;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.FireRingTimer = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).firecharge > 20
					&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).firecharge <= 40) {
				CooldownManager.set(entity, "mana_refresh", 350);
				{
					double _setval = 2;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.firestr = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				CooldownManager.set(entity, "job_1", 360);
				{
					double _setval = 150;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.FireRingTimer = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).firecharge > 40
					&& (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).firecharge <= 60) {
				CooldownManager.set(entity, "job_1", 420);
				CooldownManager.set(entity, "mana_refresh", 400);
				{
					double _setval = 3;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.firestr = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
				{
					double _setval = 200;
					entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
						capability.FireRingTimer = _setval;
						capability.syncPlayerVariables(entity);
					});
				}
			}
		} else if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).FireVar == 2) {
			CooldownManager.set(entity, "job_1", 240);
			FireReleaseBeamProcedure.execute(world, x, y, z, entity);
			CooldownManager.set(entity, "mana_refresh", 200);
		}
	}
}
