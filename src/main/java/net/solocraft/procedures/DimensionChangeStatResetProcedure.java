package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.util.PlayerVitalSync;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@Mod.EventBusSubscriber
public class DimensionChangeStatResetProcedure {
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		execute(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		execute(null, entity);
	}

	private static void execute(@Nullable Event event, Entity entity) {
		if (!(entity instanceof ServerPlayer player))
			return;
		PlayerVitalSync.refreshClientState(player);
		SololevelingMod.queueServerWork(1, () -> {
			if (!player.isRemoved())
				PlayerVitalSync.refreshClientState(player);
		});
	}
}
