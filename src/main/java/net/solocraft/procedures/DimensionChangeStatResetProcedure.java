package net.solocraft.procedures;

import net.solocraft.SololevelingMod;
import net.solocraft.util.PlayerVitalSync;
import net.solocraft.util.ShadowMonarchManager;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

@EventBusSubscriber
public class DimensionChangeStatResetProcedure {
	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			ShadowMonarchManager.dismissLoadedOwnedShadows(player,
					event.getFrom());
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
