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

@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ShadowSummonBackgroundRenderTypes {
	private static ShaderInstance shadowSummonBackgroundShader;

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_shadow_summon_background"), DefaultVertexFormat.POSITION_TEX),
					shader -> shadowSummonBackgroundShader = shader);
		} catch (Exception e) {
			shadowSummonBackgroundShader = null;
			SololevelingMod.LOGGER.warn("Failed to load Shadow Summon GUI background shader; using Java fallback.", e);
		}
	}

	public static ShaderInstance get() {
		return shadowSummonBackgroundShader;
	}
}
