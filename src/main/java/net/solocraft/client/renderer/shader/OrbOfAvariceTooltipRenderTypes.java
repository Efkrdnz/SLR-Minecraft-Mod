package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

/**
 * Registers the Orb of Avarice appraisal background. It intentionally has its
 * own fragment program instead of borrowing one of the weapon theme branches.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OrbOfAvariceTooltipRenderTypes {
	private static ShaderInstance tooltipShader;

	private OrbOfAvariceTooltipRenderTypes() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_orb_of_avarice_tooltip"),
					DefaultVertexFormat.POSITION_TEX), shader -> tooltipShader = shader);
		} catch (Exception exception) {
			tooltipShader = null;
			SololevelingMod.LOGGER.warn(
					"Failed to load the Orb of Avarice tooltip shader; using the static appraisal background.",
					exception);
		}
	}

	public static ShaderInstance get() {
		return tooltipShader;
	}
}
