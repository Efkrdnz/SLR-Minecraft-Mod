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
 * Body-fitted concealment shell used by Veil, Stealth and Zero Presence.
 *
 * <p>Translucent rather than additive on purpose: an additive shroud grows
 * brighter wherever layers overlap, which is the opposite of what concealment
 * should look like. Depth testing stays enabled for the same reason — a
 * concealment effect that draws through walls defeats itself.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class VeilShroudRenderTypes extends RenderStateShard {
	private static ShaderInstance shroudShader;

	private VeilShroudRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
				ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
						"rendertype_veil_shroud"),
				WorldShaderVertexFormat.NEW_ENTITY), shader -> shroudShader = shader);
	}

	public static RenderType shroud(ResourceLocation texture) {
		if (shroudShader == null)
			return RenderType.entityTranslucent(texture);
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(new ShaderStateShard(() -> shroudShader))
				.setTextureState(new TextureStateShard(texture, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(false);
		return RenderType.create("veil_shroud", WorldShaderVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS, 256, false, true, state);
	}
}
