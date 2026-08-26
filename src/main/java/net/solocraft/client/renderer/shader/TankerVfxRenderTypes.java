package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.TankerVfxRenderer;

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

/**
 * The two depth-tested submissions used by every Tanker visual. Both descriptors
 * share one vertex/fragment program and the same white material texture.
 */
@EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.MOD,
		value = Dist.CLIENT
)
public final class TankerVfxRenderTypes extends RenderStateShard {
	private static final Logger LOGGER = LogUtils.getLogger();
	public static final ResourceLocation MATERIAL_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

	private static ShaderInstance surfaceShader;
	private static ShaderInstance emissiveShader;

	private static final RenderType SURFACE = create("tanker_vfx_surface",
			() -> surfaceShader, TRANSLUCENT_TRANSPARENCY);
	private static final RenderType EMISSIVE = create("tanker_vfx_emissive",
			() -> emissiveShader, ADDITIVE_TRANSPARENCY);

	private TankerVfxRenderTypes(String name, Runnable setup, Runnable clear) {
		super(name, setup, clear);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		// Only GPU-facing shader references reset. CPU-authored event timelines
		// remain valid and render through the vanilla fallbacks during replacement.
		surfaceShader = null;
		emissiveShader = null;
		TankerVfxRenderer.onResourceReload();

		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
							"rendertype_tanker_vfx_surface"),
					WorldShaderVertexFormat.NEW_ENTITY), loaded -> surfaceShader = loaded);
		} catch (IOException exception) {
			LOGGER.warn("[SoloLeveling] Tanker surface shader did not load; using the vanilla fallback.",
					exception);
		}
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
							"rendertype_tanker_vfx_emissive"),
					WorldShaderVertexFormat.NEW_ENTITY), loaded -> emissiveShader = loaded);
		} catch (IOException exception) {
			LOGGER.warn("[SoloLeveling] Tanker emissive shader did not load; using the vanilla fallback.",
					exception);
		}
	}

	public static RenderType surface() {
		return surfaceShader == null
				? RenderType.entityTranslucent(MATERIAL_TEXTURE)
				: SURFACE;
	}

	public static RenderType emissive() {
		return emissiveShader == null
				? RenderType.entityTranslucentEmissive(MATERIAL_TEXTURE)
				: EMISSIVE;
	}

	public static boolean usesCustomSurfaceShader() {
		return surfaceShader != null;
	}

	public static boolean usesCustomEmissiveShader() {
		return emissiveShader != null;
	}

	private static RenderType create(String name, Supplier<ShaderInstance> shaderSupplier,
			TransparencyStateShard transparency) {
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(new ShaderStateShard(shaderSupplier))
				.setTextureState(new TextureStateShard(MATERIAL_TEXTURE, false, false))
				.setTransparencyState(transparency)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(false);
		return RenderType.create(name, WorldShaderVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS, 16_384, false, true, state);
	}
}
