package net.solocraft.client.aura;

import net.solocraft.SololevelingMod;
import net.solocraft.client.renderer.LiuSwordBeamRenderer;
import net.solocraft.client.renderer.shader.DeferredWorldShaderRenderer;
import net.solocraft.client.renderer.shader.LiuSwordRenderTypes;
import net.solocraft.util.LiuManifestationManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

@Mod.EventBusSubscriber(modid = SololevelingMod.MODID, value = Dist.CLIENT)
public final class LiuSwordHandAuraRenderer {
	private static final ResourceLocation FALLBACK = new ResourceLocation("sololeveling", "textures/particle/slashgood1.png");

	private LiuSwordHandAuraRenderer() {
	}

	@SubscribeEvent
	public static void renderThirdPerson(RenderPlayerEvent.Post event) {
		if (!(event.getEntity() instanceof AbstractClientPlayer player))
			return;
		if (player.isSpectator())
			return;
		PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
		if (LiuManifestationManager.isManifestedSword(player.getMainHandItem()))
			renderArmAura(event.getPoseStack(), event.getMultiBufferSource(), model,
					player.getMainArm(), event.getPartialTick(), player.tickCount, 1.0F);
		if (LiuManifestationManager.isManifestedSword(player.getOffhandItem()))
			renderArmAura(event.getPoseStack(), event.getMultiBufferSource(), model,
					player.getMainArm().getOpposite(), event.getPartialTick(), player.tickCount, 0.94F);
	}

	private static void renderArmAura(PoseStack stack, MultiBufferSource buffers,
			PlayerModel<AbstractClientPlayer> model, HumanoidArm arm, float partialTick, int tickCount,
			float intensity) {
		stack.pushPose();
		model.translateToHand(arm, stack);
		stack.mulPose(Axis.XP.rotationDegrees(-90.0F));
		stack.mulPose(Axis.YP.rotationDegrees(180.0F));
		float side = arm == HumanoidArm.LEFT ? -1.0F : 1.0F;
		stack.translate(side / 16.0F, 0.125F, -0.625F);
		stack.translate(0.0D, 0.0D, -0.18D);
		drawSwordGlow(stack, buffers, tickCount + partialTick, intensity);
		stack.popPose();
	}

	private static void drawSwordGlow(PoseStack stack, MultiBufferSource buffers, float age, float intensity) {
		VertexConsumer vertices = DeferredWorldShaderRenderer.buffer(buffers,
				LiuSwordRenderTypes.effect(FALLBACK));
		float pulse = (0.92F + Mth.sin(age * 0.42F) * 0.08F) * intensity;
		LiuSwordBeamRenderer.drawPlane(vertices, stack.last(), 0.34F * pulse, 0.9F * pulse,
				6.0F, 0xFFD34E, Math.round(112.0F * intensity));
		stack.pushPose();
		stack.translate(0.0D, 0.0D, -0.04D);
		LiuSwordBeamRenderer.drawPlane(vertices, stack.last(), 0.18F * pulse, 0.78F * pulse,
				0.0F, 0xFFFFFF, Math.round(72.0F * intensity));
		stack.popPose();
	}
}
