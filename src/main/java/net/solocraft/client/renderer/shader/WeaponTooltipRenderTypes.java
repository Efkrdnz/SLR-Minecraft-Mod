package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

/** Registers the configurable shader shared by every weapon tooltip. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeaponTooltipRenderTypes {
	private static ShaderInstance tooltipShader;

	private WeaponTooltipRenderTypes() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					new ResourceLocation(SololevelingMod.MODID, "rendertype_weapon_tooltip"),
					DefaultVertexFormat.POSITION_TEX), shader -> tooltipShader = shader);
		} catch (Exception exception) {
			tooltipShader = null;
			SololevelingMod.LOGGER.warn("Failed to load the weapon tooltip shader; using static backgrounds.", exception);
		}
	}

	public static ShaderInstance get() {
		return tooltipShader;
	}
}
