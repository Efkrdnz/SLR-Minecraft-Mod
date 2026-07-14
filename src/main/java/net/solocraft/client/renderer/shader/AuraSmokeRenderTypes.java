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

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Render types for the world-space aura smoke/ember field.
 *
 * <p>Two variants share one shader source: soft translucent smoke that gently
 * occludes, and additive embers that glow. Both fall back to
 * {@link RenderType#entityTranslucentEmissive} when the custom shader is
	 * unavailable.</p>
 */
@Mod.EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = Mod.EventBusSubscriber.Bus.MOD,
		value = Dist.CLIENT
)
public final class AuraSmokeRenderTypes extends RenderStateShard {
	private static final Map<ResourceLocation, RenderType> SMOKE_TYPES = new HashMap<>();
	private static final Map<ResourceLocation, RenderType> EMBER_TYPES = new HashMap<>();
	private static final Map<ResourceLocation, RenderType> FALLBACK_TYPES = new HashMap<>();

	private static ShaderInstance smokeShader;
	private static ShaderInstance emberShader;

	private AuraSmokeRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(
				new ShaderInstance(
						event.getResourceProvider(),
						new ResourceLocation(SololevelingMod.MODID, "aura_smoke"),
						WorldShaderVertexFormat.NEW_ENTITY
				),
				shader -> {
					smokeShader = shader;
					SMOKE_TYPES.clear();
				}
		);

		event.registerShader(
				new ShaderInstance(
						event.getResourceProvider(),
						new ResourceLocation(SololevelingMod.MODID, "aura_smoke_add"),
						WorldShaderVertexFormat.NEW_ENTITY
				),
				shader -> {
					emberShader = shader;
					EMBER_TYPES.clear();
				}
		);
	}

	/** Whether the procedural smoke shader can be used (false forces the fallback). */
	public static boolean usesCustomShader() {
		return smokeShader != null && emberShader != null;
	}

	/** Soft, gently occluding translucent smoke. */
	public static RenderType smoke(ResourceLocation texture) {
		if (!usesCustomShader()) {
			return fallback(texture);
		}

		return SMOKE_TYPES.computeIfAbsent(texture, resourceLocation ->
				build("aura_smoke_", resourceLocation, smokeShader, false));
	}

	/** Bright additive embers that read as glowing energy. */
	public static RenderType ember(ResourceLocation texture) {
		if (!usesCustomShader()) {
			return fallback(texture);
		}

		return EMBER_TYPES.computeIfAbsent(texture, resourceLocation ->
				build("aura_ember_", resourceLocation, emberShader, true));
	}

	private static RenderType fallback(ResourceLocation texture) {
		return FALLBACK_TYPES.computeIfAbsent(texture, RenderType::entityTranslucentEmissive);
	}

	private static RenderType build(
			String namePrefix,
			ResourceLocation texture,
			ShaderInstance shader,
			boolean additive
	) {
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(new ShaderStateShard(() -> shader))
				.setTextureState(new TextureStateShard(texture, false, false))
				.setTransparencyState(additive ? ADDITIVE_TRANSPARENCY : TRANSLUCENT_TRANSPARENCY)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(false);

		return RenderType.create(
				namePrefix + texture.getPath().replace('/', '_'),
				WorldShaderVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS,
				2048,
				false,
				true,
				state
		);
	}
}
