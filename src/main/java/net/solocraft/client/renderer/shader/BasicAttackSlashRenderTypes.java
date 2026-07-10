package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;
import net.solocraft.entity.BasicAttackSlashEntity;

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
public class BasicAttackSlashRenderTypes extends RenderStateShard {
	private static ShaderInstance fistShader;
	private static ShaderInstance swordShader;
	private static ShaderInstance daggerShader;
	private static ShaderInstance dualDaggerShader;

	private BasicAttackSlashRenderTypes(String name, Runnable setupState, Runnable clearState) {
		super(name, setupState, clearState);
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(SololevelingMod.MODID, "rendertype_basic_slash_fist"), DefaultVertexFormat.NEW_ENTITY), shader -> fistShader = shader);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(SololevelingMod.MODID, "rendertype_basic_slash_sword"), DefaultVertexFormat.NEW_ENTITY), shader -> swordShader = shader);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(SololevelingMod.MODID, "rendertype_basic_slash_dagger"), DefaultVertexFormat.NEW_ENTITY), shader -> daggerShader = shader);
		event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(SololevelingMod.MODID, "rendertype_basic_slash_dual_dagger"), DefaultVertexFormat.NEW_ENTITY), shader -> dualDaggerShader = shader);
	}

	public static RenderType slash(int style, ResourceLocation texture) {
		ShaderInstance shader = switch (style) {
			case BasicAttackSlashEntity.STYLE_FIST -> fistShader;
			case BasicAttackSlashEntity.STYLE_DAGGER -> daggerShader;
			case BasicAttackSlashEntity.STYLE_DUAL_DAGGER -> dualDaggerShader;
			default -> swordShader;
		};
		if (shader == null || IrisCompat.isShaderPackInUse()) {
			return RenderType.entityTranslucentEmissive(texture);
		}
		String name = switch (style) {
			case BasicAttackSlashEntity.STYLE_FIST -> "basic_slash_fist";
			case BasicAttackSlashEntity.STYLE_DAGGER -> "basic_slash_dagger";
			case BasicAttackSlashEntity.STYLE_DUAL_DAGGER -> "basic_slash_dual_dagger";
			default -> "basic_slash_sword";
		};
		RenderType.CompositeState state = RenderType.CompositeState.builder().setShaderState(new ShaderStateShard(() -> shader)).setTextureState(new TextureStateShard(texture, false, false))
				.setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_WRITE).createCompositeState(false);
		return RenderType.create(name, DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, false, true, state);
	}
}
