package net.solocraft.client.renderer;

import net.solocraft.SololevelingMod;
import net.solocraft.client.aura.ClientPlayerAuraManager;
import net.solocraft.client.aura.ClientPlayerAuraManager.AuraInstance;
import net.solocraft.client.aura.PlayerAuraRegistry;
import net.solocraft.client.renderer.shader.AntaresVfxRenderTypes;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import com.mojang.blaze3d.vertex.VertexConsumer;

/** Persistent third-person wings and horns while Monarch Manifestation is on. */
@EventBusSubscriber(modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class AntaresManifestationRenderer {
	private AntaresManifestationRenderer() {
	}

	@SubscribeEvent
	public static void renderManifestation(RenderPlayerEvent.Post event) {
		Minecraft minecraft = Minecraft.getInstance();
		Player player = event.getEntity();
		if (minecraft.level == null || player.isSpectator()
				|| (player == minecraft.player
						&& minecraft.options.getCameraType().isFirstPerson()))
			return;

		float envelope = 0.0F;
		long now = minecraft.level.getGameTime();
		for (AuraInstance aura : ClientPlayerAuraManager.activeFor(player.getId())) {
			if (!PlayerAuraRegistry.ANTARES_MANIFESTATION.id()
					.equals(aura.auraId()))
				continue;
			envelope = Math.max(envelope,
					aura.envelope(event.getPartialTick(), now) * aura.intensity());
		}
		if (envelope <= 0.02F)
			return;

		float age = player.tickCount + event.getPartialTick();
		float spread = Mth.clamp(envelope, 0.12F, 1.0F)
				* (0.97F + Mth.sin(age * 0.12F) * 0.035F);
		int light = LevelRenderer.getLightColor(minecraft.level,
				player.blockPosition());

		VertexConsumer surface = DeferredWorldShaderRenderer.buffer(
				event.getMultiBufferSource(), AntaresVfxRenderTypes.surface());
		AntaresVfxRenderer.drawWingPair(surface, event.getPoseStack().last(),
				new AntaresVfxRenderer.FrameBudget(160), player.getYRot(), spread,
				Mth.clamp(envelope, 0.0F, 1.0F),
				AntaresVfxRenderer.Pass.SURFACE, light);

		if (minecraft.options.particles().get() != ParticleStatus.MINIMAL) {
			VertexConsumer emissive = DeferredWorldShaderRenderer.buffer(
					event.getMultiBufferSource(), AntaresVfxRenderTypes.emissive());
			AntaresVfxRenderer.drawWingPair(emissive,
					event.getPoseStack().last(),
					new AntaresVfxRenderer.FrameBudget(160), player.getYRot(), spread,
					Mth.clamp(envelope, 0.0F, 1.0F),
					AntaresVfxRenderer.Pass.EMISSIVE, light);
		}
	}
}
