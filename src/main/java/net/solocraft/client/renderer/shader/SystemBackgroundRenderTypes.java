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
 * Registers the animated "System" GUI background core shader.
 *
 * Mirrors the mod's existing slash-effect shader pattern: if the shader fails
 * to compile/load for any reason the instance simply stays {@code null} and
 * callers fall back to a pure-Java animated background, so this can never crash
 * the menu or resource reload.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SystemBackgroundRenderTypes {
	private static ShaderInstance systemBackgroundShader;

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(SololevelingMod.MODID, "rendertype_system_background"), DefaultVertexFormat.POSITION_TEX),
					shader -> systemBackgroundShader = shader);
		} catch (Exception e) {
			// leave the shader null -> Java fallback is used
			systemBackgroundShader = null;
			SololevelingMod.LOGGER.warn("Failed to load System GUI background shader; using Java fallback.", e);
		}
	}

	/** @return the compiled shader, or {@code null} if unavailable (use the Java fallback). */
	public static ShaderInstance get() {
		return systemBackgroundShader;
	}
}
