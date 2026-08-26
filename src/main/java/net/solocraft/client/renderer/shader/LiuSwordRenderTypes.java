package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.VertexFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class LiuSwordRenderTypes extends RenderStateShard {
	private static ShaderInstance shader;
	private static final Map<ResourceLocation, RenderType> EFFECT_TYPES = new HashMap<>();

	private LiuSwordRenderTypes(String name, Runnable setup, Runnable clear) {
		super(name, setup, clear);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_liu_sword"),
				WorldShaderVertexFormat.NEW_ENTITY), loaded -> {
			shader = loaded;
			EFFECT_TYPES.clear();
		});
	}

	public static RenderType effect(ResourceLocation fallback) {
		if (shader == null)
			return RenderType.entityTranslucentEmissive(fallback);
		return EFFECT_TYPES.computeIfAbsent(fallback, texture -> {
			RenderType.CompositeState state = RenderType.CompositeState.builder()
					.setShaderState(new ShaderStateShard(() -> shader))
					.setTextureState(new TextureStateShard(texture, false, false))
					// NeoForge 1.21's ADDITIVE_TRANSPARENCY is ONE, ONE, which
					// bypasses alpha completely. Liu's effects encode their soft edge
					// and hand aura strength in alpha, so use normal alpha compositing.
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setCullState(NO_CULL)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false);
			return RenderType.create("liu_sword_vfx_" + texture.getPath().replace('/', '_'),
					WorldShaderVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1024, false, true, state);
		});
	}
}
