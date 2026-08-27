package net.solocraft.client.renderer.shader;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * NEW_ENTITY-compatible layout with its own identity. Iris may extend the
 * vanilla NEW_ENTITY singleton while a shader pack is rendering; using an
 * owned format keeps late custom core-shader buffers on their declared layout.
 */
public final class WorldShaderVertexFormat {
	public static final VertexFormat NEW_ENTITY = VertexFormat.builder()
			.add("Position", VertexFormatElement.POSITION)
			.add("Color", VertexFormatElement.COLOR)
			.add("UV0", VertexFormatElement.UV0)
			.add("UV1", VertexFormatElement.UV1)
			.add("UV2", VertexFormatElement.UV2)
			.add("Normal", VertexFormatElement.NORMAL)
			.padding(1)
			.build();

	private WorldShaderVertexFormat() {
	}
}
