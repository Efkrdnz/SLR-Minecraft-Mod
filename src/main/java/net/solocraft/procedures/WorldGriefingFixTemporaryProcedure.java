package net.solocraft.procedures;

import net.solocraft.init.SololevelingModGameRules;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStartedEvent;

import net.minecraft.world.level.LevelAccessor;

@Mod.EventBusSubscriber
public class WorldGriefingFixTemporaryProcedure {
	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		execute(event.getServer().overworld());
	}

	public static void execute(LevelAccessor world) {
		if (world != null && world.getServer() != null)
			world.getLevelData().getGameRules().getRule(SololevelingModGameRules.SOLO_WORLD_GRIEFING).set(true, world.getServer());
	}
}
