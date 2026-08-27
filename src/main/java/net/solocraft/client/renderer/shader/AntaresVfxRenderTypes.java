package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.AntaresVfxClientState;

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
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.util.function.Supplier;

import org.slf4j.Logger;

/** Depth-tested surface and additive passes for Antares's destruction VFX. */
@EventBusSubscriber(modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class AntaresVfxRenderTypes extends RenderStateShard {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final ResourceLocation MATERIAL_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");
	private static ShaderInstance surfaceShader;
	private static ShaderInstance emissiveShader;

	private static final RenderType SURFACE = create("antares_vfx_surface",
			() -> surfaceShader, TRANSLUCENT_TRANSPARENCY);
	private static final RenderType EMISSIVE = create("antares_vfx_emissive",
			() -> emissiveShader, ADDITIVE_TRANSPARENCY);

	private AntaresVfxRenderTypes(String name, Runnable setup, Runnable clear) {
		super(name, setup, clear);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		surfaceShader = null;
		emissiveShader = null;
		AntaresVfxClientState.onResourceReload();
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
							"rendertype_antares_vfx_surface"),
					WorldShaderVertexFormat.NEW_ENTITY),
					loaded -> surfaceShader = loaded);
		} catch (IOException exception) {
			LOGGER.warn("[SoloLeveling] Antares surface shader failed to load; using the vanilla fallback.",
					exception);
		}
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
							"rendertype_antares_vfx_emissive"),
					WorldShaderVertexFormat.NEW_ENTITY),
					loaded -> emissiveShader = loaded);
		} catch (IOException exception) {
			LOGGER.warn("[SoloLeveling] Antares emissive shader failed to load; using the vanilla fallback.",
					exception);
		}
	}

	public static RenderType surface() {
		return surfaceShader == null
				? RenderType.entityTranslucent(MATERIAL_TEXTURE) : SURFACE;
	}

	public static RenderType emissive() {
		return emissiveShader == null
				? RenderType.entityTranslucentEmissive(MATERIAL_TEXTURE)
				: EMISSIVE;
	}

	private static RenderType create(String name,
			Supplier<ShaderInstance> shader,
			TransparencyStateShard transparency) {
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(new ShaderStateShard(shader))
				.setTextureState(new TextureStateShard(MATERIAL_TEXTURE,
						false, false))
				.setTransparencyState(transparency)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(false);
		return RenderType.create(name, WorldShaderVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS, 24_576, false, true, state);
	}
}
