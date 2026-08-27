package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

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

@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SwordBeamProjectileRenderTypes extends RenderStateShard {
	private static ShaderInstance projectileShader;

	private SwordBeamProjectileRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_sword_beam_projectile"), WorldShaderVertexFormat.NEW_ENTITY), shader -> projectileShader = shader);
	}

	public static RenderType projectile(ResourceLocation texture) {
		if (projectileShader == null) {
			return RenderType.entityTranslucentEmissive(texture);
		}
		RenderType.CompositeState state = RenderType.CompositeState.builder().setShaderState(new ShaderStateShard(() -> projectileShader)).setTextureState(new TextureStateShard(texture, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false);
		return RenderType.create("sword_beam_projectile", WorldShaderVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 512, false, true, state);
	}
}
