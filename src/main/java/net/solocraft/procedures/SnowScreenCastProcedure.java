package net.solocraft.procedures;

import net.solocraft.SololevelingMod;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.solocraft.util.CooldownManager;

public class SnowScreenCastProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (!world.isClientSide()) {
			if (!CooldownManager.isOnCooldown(entity, "job_4")) {
				if (!entity.getPersistentData().getBoolean("snowscreen")) {
					CooldownManager.set(entity, "job_4", 800);
					entity.getPersistentData().putBoolean("snowscreen", true);
					entity.getPersistentData().putDouble("SnowX", (entity.getX()));
					entity.getPersistentData().putDouble("SnowY", (entity.getY()));
					entity.getPersistentData().putDouble("SnowZ", (entity.getZ()));
					SololevelingMod.queueServerWork(200, () -> {
						entity.getPersistentData().putBoolean("snowscreen", false);
						entity.getPersistentData().putDouble("SnowX", 0);
						entity.getPersistentData().putDouble("SnowY", 0);
						entity.getPersistentData().putDouble("SnowZ", 0);
					});
				} else {
					if (entity instanceof Player _player && !_player.level().isClientSide())
						_player.displayClientMessage(Component.literal("ability already in use!"), true);
				}
			}
		}
	}
}
