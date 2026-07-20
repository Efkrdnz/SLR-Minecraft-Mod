package net.solocraft.procedures;

import net.solocraft.util.CooldownManager;
import net.solocraft.util.MageSpellRuntime;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;

public class AirVacuumsProcedure {
	public static void execute(LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (CooldownManager.isOnCooldown(entity, "Curse Sphere")) {
			if (entity instanceof Player player && !player.level().isClientSide())
				player.displayClientMessage(Component.literal("Ability on cooldown!"), true);
			return;
		}
		CooldownManager.set(entity, "Curse Sphere", 100);
		MageSpellRuntime.startCurseSphere(world, entity);
	}
}
