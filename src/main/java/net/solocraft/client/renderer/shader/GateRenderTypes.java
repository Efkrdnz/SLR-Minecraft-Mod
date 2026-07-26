package net.solocraft.client.renderer.shader;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.HashMap;
import java.util.Map;

/**
 * Full-bright gate rendering that also writes depth. Vanilla's translucent
 * emissive entity type does not write depth, allowing water rendered later in
 * the frame to appear over gate pixels that are actually in front of it.
 */
public final class GateRenderTypes extends RenderStateShard {
	private static final Map<ResourceLocation, RenderType> EMISSIVE_TYPES = new HashMap<>();

	private GateRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	public static RenderType emissive(ResourceLocation texture) {
		return EMISSIVE_TYPES.computeIfAbsent(texture, resourceLocation -> {
			RenderType.CompositeState state = RenderType.CompositeState.builder()
					.setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER)
					.setTextureState(new TextureStateShard(resourceLocation, false, false))
					.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
					.setDepthTestState(LEQUAL_DEPTH_TEST)
					.setCullState(NO_CULL)
					.setLightmapState(LIGHTMAP)
					.setOverlayState(OVERLAY)
					.setWriteMaskState(COLOR_DEPTH_WRITE)
					.createCompositeState(false);
			return RenderType.create("gate_emissive_" + resourceLocation.getPath().replace('/', '_'),
					DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
		});
	}
}
