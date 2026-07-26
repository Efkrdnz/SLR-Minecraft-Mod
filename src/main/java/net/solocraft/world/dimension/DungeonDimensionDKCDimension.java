package net.solocraft.world.dimension;

import net.solocraft.SololevelingMod;
import net.solocraft.client.dimension.DkcDimensionSpecialEffects;
import net.solocraft.dkc.DkcFloorRegistry;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registration bridge for the shared Demon King's Castle dimension. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public class DungeonDimensionDKCDimension {
	private DungeonDimensionDKCDimension() {
	}

	@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
