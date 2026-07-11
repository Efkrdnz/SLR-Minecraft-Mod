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

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RulersAuthorityAuraRenderTypes extends RenderStateShard {
    private static ShaderInstance auraShader;

    private RulersAuthorityAuraRenderTypes(String name, Runnable setupState, Runnable clearState) {
        super(name, setupState, clearState);
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(),
                new ResourceLocation(SololevelingMod.MODID, "rendertype_rulers_authority_aura"),
                DefaultVertexFormat.NEW_ENTITY), shader -> auraShader = shader);
    }

    public static RenderType aura(ResourceLocation texture) {
        if (auraShader == null || IrisCompat.isShaderPackInUse())
            return RenderType.entityTranslucentEmissive(texture);

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
        return RenderType.create("rulers_authority_aura", DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS, 1024, false, true, state);
    }
}
