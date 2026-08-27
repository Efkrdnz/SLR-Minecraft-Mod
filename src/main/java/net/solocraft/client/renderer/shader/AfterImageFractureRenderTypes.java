package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.VertexFormat;

import java.io.IOException;

/**
 * Brittle glass treatment for a Shadow Feint afterimage.
 *
 * <p>The Stealth decoy and the Shadow Feint afterimage are the same entity, but
 * only the feint can be attacked. Rendering the feint as fractured glass is
 * what tells an opponent which one is worth swinging at.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class AfterImageFractureRenderTypes extends RenderStateShard {
	private static ShaderInstance fractureShader;

	private AfterImageFractureRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
						"rendertype_afterimage_fracture"),
				WorldShaderVertexFormat.NEW_ENTITY), shader -> fractureShader = shader);
	}

	public static RenderType fracture(ResourceLocation texture) {
		if (fractureShader == null)
			return RenderType.entityTranslucent(texture);
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(new ShaderStateShard(() -> fractureShader))
				.setTextureState(new TextureStateShard(texture, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(false);
		return RenderType.create("afterimage_fracture", WorldShaderVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS, 256, false, true, state);
	}
}
