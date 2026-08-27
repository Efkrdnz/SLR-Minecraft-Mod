
package net.solocraft.world.dimension;

import net.solocraft.SololevelingMod;
import net.solocraft.client.dimension.SnowDungeonSpecialEffects;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;

public class DungeonDimensionSnowDimension {
	private static final ResourceLocation EFFECTS_ID = ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "dungeon_dimension_snow");

	private DungeonDimensionSnowDimension() {
	}

	@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class DimensionSpecialEffectsHandler {
		private DimensionSpecialEffectsHandler() {
		}

		@SubscribeEvent
		@OnlyIn(Dist.CLIENT)
		public static void registerDimensionSpecialEffects(RegisterDimensionSpecialEffectsEvent event) {
			event.register(EFFECTS_ID, new SnowDungeonSpecialEffects());
		}
	}
}
