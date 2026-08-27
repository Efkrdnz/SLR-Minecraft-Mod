package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;
import net.solocraft.client.aura.PlayerAuraDefinition;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.MOD,
		value = Dist.CLIENT
)
public final class PlayerAuraRenderTypes extends RenderStateShard {
	private static final Map<ResourceLocation, RenderType> NORMAL_AURA_TYPES = new HashMap<>();
	private static final Map<ResourceLocation, RenderType> FALLBACK_TYPES = new HashMap<>();

	private static ShaderInstance auraShader;

	private PlayerAuraRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(
				new ShaderInstance(
						event.getResourceProvider(),
						ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_player_aura"),
						WorldShaderVertexFormat.NEW_ENTITY
				),
				shader -> {
					auraShader = shader;
					NORMAL_AURA_TYPES.clear();
				}
		);
	}

	public static RenderType aura(PlayerAuraDefinition definition) {
		return normalAura(definition.fallbackTexture());
	}

	public static RenderType aura(ResourceLocation fallbackTexture) {
		return normalAura(fallbackTexture);
	}

	private static RenderType normalAura(ResourceLocation texture) {
		return shaderType(
				texture,
				auraShader,
				NORMAL_AURA_TYPES,
				"player_aura_",
				false
		);
	}

	private static RenderType shaderType(
			ResourceLocation texture,
			ShaderInstance shader,
			Map<ResourceLocation, RenderType> cache,
			String namePrefix,
			boolean additive
	) {
		if (shader == null) {
			return FALLBACK_TYPES.computeIfAbsent(
					texture,
					RenderType::entityTranslucentEmissive
			);
		}

		return cache.computeIfAbsent(texture, resourceLocation -> {
			RenderType.CompositeState state = RenderType.CompositeState.builder()
					.setShaderState(new ShaderStateShard(() -> shader))
					.setTextureState(new TextureStateShard(resourceLocation, false, false))
					.setTransparencyState(
							additive
									? ADDITIVE_TRANSPARENCY
									: TRANSLUCENT_TRANSPARENCY
					)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setCullState(NO_CULL)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false);

			return RenderType.create(
					namePrefix + resourceLocation.getPath().replace('/', '_'),
					WorldShaderVertexFormat.NEW_ENTITY,
					VertexFormat.Mode.QUADS,
					2048,
					false,
					true,
					state
			);
		});
	}
}
