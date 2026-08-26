package net.solocraft.client.renderer.shader;

import net.solocraft.SololevelingMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

/**
 * Registers the Demon King's Castle tower-screen background shader.
 *
 * The instance deliberately remains nullable: a broken or unsupported shader
 * resource must never prevent the quest screen from opening.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DkcTowerBackgroundRenderTypes {
	private static ShaderInstance shader;

	private DkcTowerBackgroundRenderTypes() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		shader = null;
		try {
			event.registerShader(
					new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID, "rendertype_dkc_tower_background"),
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
