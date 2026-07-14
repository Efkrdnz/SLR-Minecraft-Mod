package net.solocraft.client.renderer.shader;

import com.google.common.collect.ImmutableMap;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

/**
 * NEW_ENTITY-compatible layout with its own identity. Oculus 1.20.1 extends
 * the vanilla NEW_ENTITY singleton for shader-pack gbuffers; using an owned
 * format keeps late custom core-shader buffers on their declared layout.
 */
public final class WorldShaderVertexFormat {
	public static final VertexFormat NEW_ENTITY = new VertexFormat(
			ImmutableMap.<String, VertexFormatElement>builder()
					.put("Position", DefaultVertexFormat.ELEMENT_POSITION)
					.put("Color", DefaultVertexFormat.ELEMENT_COLOR)
					.put("UV0", DefaultVertexFormat.ELEMENT_UV0)
					.put("UV1", DefaultVertexFormat.ELEMENT_UV1)
					.put("UV2", DefaultVertexFormat.ELEMENT_UV2)
					.put("Normal", DefaultVertexFormat.ELEMENT_NORMAL)
					.put("Padding", DefaultVertexFormat.ELEMENT_PADDING)
					.build());

	private WorldShaderVertexFormat() {
	}
}
