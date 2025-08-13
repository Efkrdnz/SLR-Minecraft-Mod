package net.solocraft;

import org.joml.Matrix4f;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.GameRenderer;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;

@OnlyIn(Dist.CLIENT)
public class CustomPortalBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
	public static final ResourceLocation TEXTURE_1 = new ResourceLocation("sololeveling:textures/entities/systemvoid.png");
	private static final RenderType CUSTOM_PORTAL = RenderType.create("end_portal", DefaultVertexFormat.POSITION, VertexFormat.Mode.QUADS, 256, false, false,
			RenderType.CompositeState.builder().setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getRendertypeEndPortalShader))
					.setTextureState(RenderStateShard.MultiTextureStateShard.builder().add(TEXTURE_1, false, false).add(TEXTURE_1, false, false).build()).createCompositeState(false));

	public CustomPortalBlockEntityRenderer(BlockEntityRendererProvider.Context p_173689_) {
	}

	public void render(T p_112650_, float p_112651_, PoseStack p_112652_, MultiBufferSource p_112653_, int p_112654_, int p_112655_) {
		Matrix4f matrix4f = p_112652_.last().pose();
		this.renderCube(p_112650_, matrix4f, p_112653_.getBuffer(this.renderType()));
	}

	private void renderCube(T p_173691_, Matrix4f p_254024_, VertexConsumer p_173693_) {
		float f = this.getOffsetDown();
		float f1 = this.getOffsetUp();
		this.renderFace(p_173691_, p_254024_, p_173693_, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, Direction.SOUTH);
		this.renderFace(p_173691_, p_254024_, p_173693_, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, Direction.NORTH);
		this.renderFace(p_173691_, p_254024_, p_173693_, 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.EAST);
		this.renderFace(p_173691_, p_254024_, p_173693_, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, Direction.WEST);
		this.renderFace(p_173691_, p_254024_, p_173693_, 0.0F, 1.0F, f, f, 0.0F, 0.0F, 1.0F, 1.0F, Direction.DOWN);
		this.renderFace(p_173691_, p_254024_, p_173693_, 0.0F, 1.0F, f1, f1, 1.0F, 1.0F, 0.0F, 0.0F, Direction.UP);
	}

	private void renderFace(T p_253949_, Matrix4f p_254247_, VertexConsumer p_254390_, float p_254147_, float p_253639_, float p_254107_, float p_254109_, float p_254021_, float p_254458_, float p_254086_, float p_254310_, Direction p_253619_) {
		if (true) {
			p_254390_.vertex(p_254247_, p_254147_, p_254107_, p_254021_).endVertex();
			p_254390_.vertex(p_254247_, p_253639_, p_254107_, p_254458_).endVertex();
			p_254390_.vertex(p_254247_, p_253639_, p_254109_, p_254086_).endVertex();
			p_254390_.vertex(p_254247_, p_254147_, p_254109_, p_254310_).endVertex();
		}
	}

	protected float getOffsetUp() {
		return 0.75F;
	}

	protected float getOffsetDown() {
		return 0.375F;
	}

	protected RenderType renderType() {
		return CUSTOM_PORTAL;
	}
}
