package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;
import net.solocraft.util.MageSpellRuntime;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.LevelAccessor;

public class WaterBulletProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity living)
			living.swing(InteractionHand.MAIN_HAND, true);
		CooldownManager.set(entity, "Water Slash", 60);
		MageSpellRuntime.startWaterSlash(world, entity);
	}
}
