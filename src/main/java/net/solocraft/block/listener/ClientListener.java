package net.solocraft.block.listener;

import net.solocraft.init.SololevelingModBlockEntities;
import net.solocraft.block.renderer.InstanceDungeonKeyLoggerTileRenderer;
import net.solocraft.block.renderer.InstanceCoverTileRenderer;
import net.solocraft.block.renderer.HunterRankEvaluatorTileRenderer;
import net.solocraft.block.renderer.DungeonWallTileRenderer;
import net.solocraft.SololevelingMod;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientListener {
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer(SololevelingModBlockEntities.INSTANCE_DUNGEON_KEY_LOGGER.get(), context -> new InstanceDungeonKeyLoggerTileRenderer());
		event.registerBlockEntityRenderer(SololevelingModBlockEntities.INSTANCE_COVER.get(), context -> new InstanceCoverTileRenderer());
		event.registerBlockEntityRenderer(SololevelingModBlockEntities.HUNTER_RANK_EVALUATOR.get(), context -> new HunterRankEvaluatorTileRenderer());
		event.registerBlockEntityRenderer(SololevelingModBlockEntities.DUNGEON_WALL.get(), context -> new DungeonWallTileRenderer());
	}
}
