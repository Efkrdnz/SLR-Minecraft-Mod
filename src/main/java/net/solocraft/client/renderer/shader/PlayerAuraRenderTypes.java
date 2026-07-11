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

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class PlayerAuraRenderTypes extends RenderStateShard {
	private static final Map<ResourceLocation, RenderType> SHADER_TYPES = new HashMap<>();
	private static final Map<ResourceLocation, RenderType> FALLBACK_TYPES = new HashMap<>();
	private static ShaderInstance auraShader;

	private PlayerAuraRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
				new ResourceLocation(SololevelingMod.MODID, "rendertype_player_aura"),
				DefaultVertexFormat.NEW_ENTITY), shader -> {
			auraShader = shader;
			SHADER_TYPES.clear();
		});
	}

	public static RenderType aura(ResourceLocation fallbackTexture) {
		if (auraShader == null || IrisCompat.isShaderPackInUse())
			return FALLBACK_TYPES.computeIfAbsent(fallbackTexture, RenderType::entityTranslucentEmissive);
		return SHADER_TYPES.computeIfAbsent(fallbackTexture, texture -> {
			RenderType.CompositeState state = RenderType.CompositeState.builder()
					.setShaderState(new ShaderStateShard(() -> auraShader))
					.setTextureState(new TextureStateShard(texture, false, false))
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setCullState(NO_CULL)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.setWriteMaskState(COLOR_WRITE)
					.createCompositeState(false);
			return RenderType.create("player_aura_" + texture.getPath().replace('/', '_'),
					DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 2048, false, true, state);
		});
	}

}
