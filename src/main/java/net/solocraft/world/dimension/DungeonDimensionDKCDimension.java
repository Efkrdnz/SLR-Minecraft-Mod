package net.solocraft.world.dimension;

import net.solocraft.SololevelingMod;
import net.solocraft.client.dimension.DkcDimensionSpecialEffects;
import net.solocraft.dkc.DkcFloorRegistry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

/** Registration bridge for the shared Demon King's Castle dimension. */
public class DungeonDimensionDKCDimension {
	private DungeonDimensionDKCDimension() {
	}

	@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class DimensionSpecialEffectsHandler {
		private DimensionSpecialEffectsHandler() {
		}

		@SubscribeEvent
		@OnlyIn(Dist.CLIENT)
		public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
			event.register(DkcFloorRegistry.SHARED_DIMENSION.location(), new DkcDimensionSpecialEffects());
		}
	}
}
