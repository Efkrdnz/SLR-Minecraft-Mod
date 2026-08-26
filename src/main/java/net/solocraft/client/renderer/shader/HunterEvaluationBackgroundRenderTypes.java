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

/** Registers the class-hued wave shader used by Hunter Evaluation. */
@EventBusSubscriber(modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HunterEvaluationBackgroundRenderTypes {
	private static ShaderInstance shader;

	private HunterEvaluationBackgroundRenderTypes() {
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) {
		try {
			event.registerShader(new ShaderInstance(event.getResourceProvider(),
					ResourceLocation.fromNamespaceAndPath(SololevelingMod.MODID,
							"rendertype_hunter_evaluation_background"),
					DefaultVertexFormat.POSITION_TEX),
					loaded -> shader = loaded);
		} catch (Exception exception) {
			shader = null;
			SololevelingMod.LOGGER.warn(
					"Failed to load Hunter Evaluation shader; using Java fallback.",
					exception);
		}
	}

	public static ShaderInstance get() {
		return shader;
	}
}
