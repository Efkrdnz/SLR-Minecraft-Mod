package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.init.SololevelingModMobEffects;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;

import javax.annotation.Nullable;

@EventBusSubscriber
public class WillPowerDamageProcedure {
	@SubscribeEvent
	public static void onEntityAttacked(LivingIncomingDamageEvent event) {
		Entity entity = event.getEntity();
		if (event != null && entity != null) {
			execute(event, entity.level(), entity.getX(), entity.getZ(), entity, event.getAmount());
		}
	}

	public static void execute(LevelAccessor world, double x, double z, Entity entity, double amount) {
		execute(null, world, x, z, entity, amount);
	}

	private static void execute(@Nullable ICancellableEvent event, LevelAccessor world, double x, double z, Entity entity, double amount) {
		if (entity == null)
			return;
		if (entity instanceof LivingEntity _livEnt0 && _livEnt0.hasEffect(SololevelingModMobEffects.WILL_POWER)) {
			{
				double _setval = (entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).wp + amount;
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					capability.wp = _setval;
					capability.syncPlayerVariables(entity);
				});
			}
			if ((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).wp / 4 <= (entity instanceof LivingEntity _livEnt ? _livEnt.getHealth() : -1)) {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(Component.literal(("Damage absorbed: " + Math.round((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).wp))),
							true);
			} else {
				if (entity instanceof Player _player && !_player.level().isClientSide())
					_player.displayClientMessage(
							Component.literal(("\u00A7cDamage absorbed: " + Math.round((entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).orElse(new SololevelingModVariables.PlayerVariables())).wp))), true);
			}
			if (event != null) {
				event.setCanceled(true);
			}
			if (world instanceof ServerLevel _level)
				_level.sendParticles(ParticleTypes.ASH, x, (entity.getY() + 1), z, 3, 0.05, 0.05, 0.05, 1);
		}
	}
}
