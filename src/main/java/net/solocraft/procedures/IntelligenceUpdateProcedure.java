package net.solocraft.procedures;

import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.ManaRules;
import net.solocraft.util.TemporaryStatBonusManager;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class IntelligenceUpdateProcedure {
	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (true) {
			execute(event, event.getEntity().level(), event.getEntity());
		}
	}

	public static void execute(LevelAccessor world, Entity entity) {
		execute(null, world, entity);
	}

	private static void execute(@Nullable Event event, LevelAccessor world, Entity entity) {
		if (entity == null)
			return;
		if (entity instanceof Player player && player.isCreative()) {
			if (world.getLevelData().getGameTime() % 20 == 0) {
				entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
					if (capability.Mana != 1000000.0D || capability.MP != 1000000.0D) {
						capability.Mana = 1000000.0D;
						capability.MP = 1000000.0D;
						capability.syncPlayerVariables(entity);
					}
				});
			}
			return;
		}
		if (world.getLevelData().getGameTime() % 20 == 0) {
			entity.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
				double mana = ManaRules.maximumMana(entity);
				if (capability.Mana != mana) {
					capability.Mana = mana;
					capability.syncPlayerVariables(entity);
				}
			});
		}
	}
}
