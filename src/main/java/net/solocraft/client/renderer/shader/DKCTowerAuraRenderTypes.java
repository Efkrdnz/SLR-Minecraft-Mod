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

import com.mojang.blaze3d.vertex.VertexFormat;

import java.io.IOException;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class DKCTowerAuraRenderTypes extends RenderStateShard {
	private static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("sololeveling:textures/particle/mana_red.png");
	private static ShaderInstance towerAuraShader;
	private static RenderType towerAura;
	private static RenderType fallbackTowerAura;

	private DKCTowerAuraRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
				new ResourceLocation(SololevelingMod.MODID, "rendertype_dkc_tower_aura"),
				WorldShaderVertexFormat.NEW_ENTITY), shader -> towerAuraShader = shader);
	}

	public static RenderType towerAura() {
		if (towerAuraShader == null) {
			if (fallbackTowerAura == null)
				fallbackTowerAura = create("dkc_tower_aura_fallback", RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER);
			return fallbackTowerAura;
		}
		if (towerAura == null)
			towerAura = create("dkc_tower_aura", new ShaderStateShard(() -> towerAuraShader));
		return towerAura;
	}

	private static RenderType create(String name, ShaderStateShard shaderState) {
		RenderType.CompositeState state = RenderType.CompositeState.builder()
				.setShaderState(shaderState)
				.setTextureState(new TextureStateShard(FALLBACK_TEXTURE, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY)
				.setDepthTestState(LEQUAL_DEPTH_TEST)
				.setCullState(NO_CULL)
				.setLightmapState(LIGHTMAP)
				.setOverlayState(OVERLAY)
				.setWriteMaskState(COLOR_WRITE)
				.createCompositeState(false);
		return RenderType.create(name, WorldShaderVertexFormat.NEW_ENTITY,
				VertexFormat.Mode.QUADS, 16384, false, true, state);
	}
}
