package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.chat.Component;
import net.solocraft.util.CooldownManager;

public class RulersHandProjectileHitsLivingEntityProcedure {
	public static void execute(Entity entity, Entity sourceentity) {
		if (entity == null || sourceentity == null)
			return;
		double amp = 0;
		amp = Math.sqrt(Math.pow(sourceentity.getX() - entity.getX(), 2) + Math.pow(sourceentity.getY() - entity.getY(), 2) + Math.pow(sourceentity.getZ() - entity.getZ(), 2));
		if ((sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP >= (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) * amp * 4) {
			entity.setDeltaMovement(new Vec3(((-2) * (amp / 5) * sourceentity.getLookAngle().x), ((-1) * (amp / 5) * sourceentity.getLookAngle().y), ((-2) * (amp / 5) * sourceentity.getLookAngle().z)));
			if (sourceentity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal(("\u00A73" + "Pulling " + entity.getDisplayName().getString())), true);
			{
				double _setval = (sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).MP
						- (entity instanceof LivingEntity _livEnt ? _livEnt.getMaxHealth() : -1) * amp * 4;
				sourceentity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.MP = _setval;
					capability.syncPlayerVariables(sourceentity);
				});
			}
			CooldownManager.set(sourceentity, "mana_refresh", 120);
			CooldownManager.set(sourceentity, "telekinesis", 200);
		} else {
			if (sourceentity instanceof Player _player && !_player.level().isClientSide())
				_player.displayClientMessage(Component.literal(("\u00A74" + "Not enough mana to pull " + entity.getDisplayName().getString())), true);
		}
	}
}
