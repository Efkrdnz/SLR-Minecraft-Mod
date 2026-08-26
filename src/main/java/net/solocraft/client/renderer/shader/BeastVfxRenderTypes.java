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
import java.util.function.Supplier;

@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BeastVfxRenderTypes extends RenderStateShard {
	private static final ResourceLocation FALLBACK = ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "textures/particle/slashgood1.png");
	private static ShaderInstance additiveShader;
	private static ShaderInstance surfaceShader;

	private static final RenderType ADDITIVE = create("beast_vfx_additive", () -> additiveShader,
			ADDITIVE_TRANSPARENCY, COLOR_WRITE);
	private static final RenderType SURFACE = create("beast_vfx_surface", () -> surfaceShader,
			TRANSLUCENT_TRANSPARENCY, COLOR_WRITE);

	private BeastVfxRenderTypes(String name, Runnable setup, Runnable clear) {
		super(name, setup, clear);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_beast_vfx"),
				WorldShaderVertexFormat.NEW_ENTITY), loaded -> additiveShader = loaded);
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_beast_surface"),
				WorldShaderVertexFormat.NEW_ENTITY), loaded -> surfaceShader = loaded);
	}

	public static RenderType additive() {
		return additiveShader == null ? RenderType.entityTranslucentEmissive(FALLBACK) : ADDITIVE;
	}

	public static RenderType surface() {
		return surfaceShader == null ? RenderType.entityTranslucent(FALLBACK) : SURFACE;
	}

	private static RenderType create(String name, Supplier<ShaderInstance> shaderSupplier,
			TransparencyStateShard transparency, WriteMaskStateShard writeMask) {
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(new ShaderStateShard(shaderSupplier))
				.setTextureState(new TextureStateShard(FALLBACK, false, false))
				.setTransparencyState(transparency)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(writeMask)
				.createCompositeState(false);
		return RenderType.create(name, WorldShaderVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS, 1024, false, true, state);
	}
}
