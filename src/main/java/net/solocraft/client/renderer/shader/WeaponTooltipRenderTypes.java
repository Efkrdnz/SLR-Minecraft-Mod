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

/** Registers the configurable shader shared by every weapon tooltip. */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WeaponTooltipRenderTypes {
	private static ShaderInstance tooltipShader;

	private WeaponTooltipRenderTypes() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_weapon_tooltip"),
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
