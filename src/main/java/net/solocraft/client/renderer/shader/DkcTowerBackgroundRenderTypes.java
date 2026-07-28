package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

/**
 * Registers the Demon King's Castle tower-screen background shader.
 *
 * The instance deliberately remains nullable: a broken or unsupported shader
 * resource must never prevent the quest screen from opening.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DkcTowerBackgroundRenderTypes {
	private static ShaderInstance shader;

	private DkcTowerBackgroundRenderTypes() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		shader = null;
		try {
			event.registerShader(
					new ShaderInstance(event.getResourceProvider(), new ResourceLocation(SololevelingMod.MODID, "rendertype_dkc_tower_background"),
							DefaultVertexFormat.POSITION_TEX),
					loaded -> shader = loaded);
		} catch (Exception exception) {
			SololevelingMod.LOGGER.warn("Failed to load the DKC tower background shader; using the screen fallback.", exception);
		}
	}

	/** @return the compiled shader, or {@code null} when the screen should use its fallback. */
	public static ShaderInstance get() {
		return shader;
	}
}
