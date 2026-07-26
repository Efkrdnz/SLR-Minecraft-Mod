package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

/**
 * Registers the Orb of Avarice appraisal background. It intentionally has its
 * own fragment program instead of borrowing one of the weapon theme branches.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OrbOfAvariceTooltipRenderTypes {
	private static ShaderInstance tooltipShader;

	private OrbOfAvariceTooltipRenderTypes() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					new ResourceLocation(SololevelingMod.MODID, "rendertype_orb_of_avarice_tooltip"),
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
