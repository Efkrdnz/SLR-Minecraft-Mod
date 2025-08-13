package net.solocraft.block.listener;

import net.solocraft.init.SololevelingModBlockEntities;
import net.solocraft.block.renderer.InstanceDungeonKeyLoggerTileRenderer;
import net.solocraft.block.renderer.InstanceCoverTileRenderer;
import net.solocraft.block.renderer.HunterRankEvaluatorTileRenderer;
import net.solocraft.block.renderer.DungeonWallTileRenderer;
import net.solocraft.SololevelingMod;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
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
