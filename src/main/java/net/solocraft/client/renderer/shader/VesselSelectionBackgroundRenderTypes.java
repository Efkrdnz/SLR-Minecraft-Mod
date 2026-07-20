package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

/** Registers the identity-reactive background used by the vessel selection screen. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VesselSelectionBackgroundRenderTypes {
	private static ShaderInstance shader;

	private VesselSelectionBackgroundRenderTypes() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					new ResourceLocation(SololevelingMod.MODID, "rendertype_vessel_selection_background"), DefaultVertexFormat.POSITION_TEX),
					loaded -> shader = loaded);
		} catch (Exception exception) {
			shader = null;
			SololevelingMod.LOGGER.warn("Failed to load the vessel selection background shader; using Java fallback.", exception);
		}
	}

	public static ShaderInstance get() {
		return shader;
	}
}
