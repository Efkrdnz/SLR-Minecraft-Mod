
package net.solocraft.world.dimension;

import net.solocraft.SololevelingMod;
import net.solocraft.client.dimension.SnowDungeonSpecialEffects;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.resources.ResourceLocation;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public class DungeonDimensionSnowDimension {
	private static final ResourceLocation EFFECTS_ID = new ResourceLocation(SololevelingMod.MODID, "dungeon_dimension_snow");

	private DungeonDimensionSnowDimension() {
	}

	@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
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
