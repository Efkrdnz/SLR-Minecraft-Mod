package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.VertexFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GlacialPursuitRenderTypes extends RenderStateShard {
	private static ShaderInstance shader;
	private static final Map<ResourceLocation, RenderType> EFFECT_CACHE = new HashMap<>();

	private GlacialPursuitRenderTypes(String name, Runnable setup, Runnable clear) {
		super(name, setup, clear);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
				new ResourceLocation(SololevelingMod.MODID, "rendertype_glacial_pursuit"),
				WorldShaderVertexFormat.NEW_ENTITY), loaded -> shader = loaded);
	}

	public static RenderType effect(ResourceLocation fallback) {
		if (shader == null)
			return RenderType.entityTranslucentEmissive(fallback);
		return EFFECT_CACHE.computeIfAbsent(fallback, GlacialPursuitRenderTypes::createEffect);
	}

	private static RenderType createEffect(ResourceLocation fallback) {
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(new ShaderStateShard(() -> shader))
				.setTextureState(new TextureStateShard(fallback, false, false))
				.setTransparencyState(ADDITIVE_TRANSPARENCY)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(false);
		return RenderType.create("glacial_pursuit", WorldShaderVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS, 512, false, true, state);
	}
}
