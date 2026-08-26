package net.solocraft.client.renderer;

import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Shared code-native mana-arrow mesh used by held and flying arrows.
 *
 * <p>The mesh is finite geometry rather than a beam or position-history trail:
 * a four-sided luminous shaft, crystal head, collar, and four swept fins.</p>
 */
public final class ManaArrowVisual {
	public static final ResourceLocation WHITE_TEXTURE =
			ResourceLocation.fromNamespaceAndPath("minecraft", "textures/misc/white.png");

	private ManaArrowVisual() {
	}

	public static void render(PoseStack poseStack, MultiBufferSource buffers,
			Vec3 tail, Vec3 tip, int stage, boolean locked, float intensity,
			double shaftRadius, double headLength, double headRadius,
			double finLength, double finRadius) {
		renderInternal(poseStack, buffers, tail, tip, stage, locked, intensity,
				shaftRadius, headLength, headRadius, finLength, finRadius, false);
	}

	public static void renderNocked(PoseStack poseStack, MultiBufferSource buffers,
			Vec3 tail, Vec3 tip, int stage, boolean locked, float intensity,
			double shaftRadius, double headLength, double headRadius) {
		renderInternal(poseStack, buffers, tail, tip, stage, locked, intensity,
				shaftRadius, headLength, headRadius, 0.0D, 0.0D, true);
	}

	/**
	 * Draws one section of the Spirit Bow string. The string is code-native so it
	 * can follow the nock continuously instead of snapping between texture states.
	 */
	public static void renderBowString(PoseStack poseStack, MultiBufferSource buffers,
			Vec3 start, Vec3 end, double radius) {
		Vec3 delta = end.subtract(start);
		double length = delta.length();
		if (length < 0.0001D)
			return;

		Vec3 forward = delta.scale(1.0D / length);
		Vec3 reference = Math.abs(forward.y) < 0.92D
				? new Vec3(0.0D, 1.0D, 0.0D)
				: new Vec3(1.0D, 0.0D, 0.0D);
		Vec3 right = forward.cross(reference).normalize();
		Vec3 up = right.cross(forward).normalize();

		VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
		PoseStack.Pose pose = poseStack.last();
		drawPrism(vertices, pose, start, end, right, up, radius,
				12, 62, 76, 42, 150, 174, 224);
	}

	/**
	 * Compact conventional arrow used while the Spirit Bow is drawing ordinary
	 * ammunition. It deliberately omits fins in first person to keep the nock
	 * unobtrusive.
	 */
	public static void renderPhysicalNocked(PoseStack poseStack, MultiBufferSource buffers,
			Vec3 tail, Vec3 tip, double shaftRadius, double headLength, double headRadius) {
		Vec3 delta = tip.subtract(tail);
		double length = delta.length();
		if (length < 0.0001D)
			return;

		Vec3 forward = delta.scale(1.0D / length);
		Vec3 reference = Math.abs(forward.y) < 0.92D
				? new Vec3(0.0D, 1.0D, 0.0D)
				: new Vec3(1.0D, 0.0D, 0.0D);
		Vec3 right = forward.cross(reference).normalize();
		Vec3 up = right.cross(forward).normalize();
		double safeHeadLength = Math.min(headLength, length * 0.32D);
		Vec3 headBase = tip.subtract(forward.scale(safeHeadLength));

		VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
		PoseStack.Pose pose = poseStack.last();
		drawPrism(vertices, pose, tail, headBase, right, up, shaftRadius,
				72, 42, 24, 112, 72, 34, 255);
		drawCrystalHead(vertices, pose, headBase, tip, right, up, headRadius,
				152, 164, 174, 224, 234, 240, 255);
	}

	private static void renderInternal(PoseStack poseStack, MultiBufferSource buffers,
			Vec3 tail, Vec3 tip, int stage, boolean locked, float intensity,
			double shaftRadius, double headLength, double headRadius,
			double finLength, double finRadius, boolean nocked) {
		Vec3 delta = tip.subtract(tail);
		double length = delta.length();
		if (length < 0.0001D)
			return;

		Vec3 forward = delta.scale(1.0D / length);
		Vec3 reference = Math.abs(forward.y) < 0.92D
				? new Vec3(0.0D, 1.0D, 0.0D)
				: new Vec3(1.0D, 0.0D, 0.0D);
		Vec3 right = forward.cross(reference).normalize();
		Vec3 up = right.cross(forward).normalize();

		double safeHeadLength = Math.min(headLength, length * 0.32D);
		double safeFinLength = Math.min(finLength, length * 0.30D);
		Vec3 headBase = tip.subtract(forward.scale(safeHeadLength));
		Vec3 shaftStart = tail.add(forward.scale(Math.min(safeFinLength * 0.22D, length * 0.06D)));

		int[] accent = accentColor(stage, locked);
		float strength = Mth.clamp(intensity, 0.0F, 1.0F);
		int alpha = Mth.clamp(Math.round(188.0F + strength * 67.0F), 0, 255);
		int finAlpha = Mth.clamp(Math.round(alpha * 0.78F), 0, 255);
		int coreRed = Mth.clamp(205 + Math.round(strength * 50.0F), 0, 255);
		int coreGreen = Mth.clamp(238 + Math.round(strength * 17.0F), 0, 255);
		int coreBlue = 255;

		VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE));
		PoseStack.Pose pose = poseStack.last();
		if (nocked) {
			drawPrism(vertices, pose, shaftStart, headBase, right, up, shaftRadius,
					14, 92, 156, 24, 126, 190, Math.min(alpha, 224));
		} else {
			drawPrism(vertices, pose, shaftStart, headBase, right, up, shaftRadius,
					accent[0], accent[1], accent[2], coreRed, coreGreen, coreBlue, alpha);
		}

		double collarLength = Math.min(safeHeadLength * 0.22D, length * 0.05D);
		Vec3 collarStart = headBase.subtract(forward.scale(collarLength));
		drawPrism(vertices, pose, collarStart, headBase, right, up,
				headRadius * 0.52D, coreRed, coreGreen, coreBlue,
				accent[0], accent[1], accent[2], alpha);

		drawCrystalHead(vertices, pose, headBase, tip, right, up, headRadius,
				accent[0], accent[1], accent[2], coreRed, coreGreen, coreBlue, alpha);
		if (!nocked && safeFinLength > 0.0001D && finRadius > 0.0001D)
			drawFins(vertices, pose, tail, forward, right, up,
					safeFinLength, finRadius, accent[0], accent[1], accent[2], finAlpha);
	}

	private static int[] accentColor(int stage, boolean locked) {
		int red;
		int green;
		if (stage >= 3) {
			red = 96;
			green = 178;
		} else if (stage == 2) {
			red = 42;
			green = 225;
		} else {
			red = 24;
			green = 198;
		}
		if (locked) {
			red = Math.min(255, red + 52);
			green = Math.min(255, green + 38);
		}
		return new int[] {red, green, 255};
	}

	private static void drawPrism(VertexConsumer vertices, PoseStack.Pose pose,
			Vec3 start, Vec3 end, Vec3 right, Vec3 up, double radius,
			int redA, int greenA, int blueA, int redB, int greenB, int blueB, int alpha) {
		Vec3[] startRing = diamondRing(start, right, up, radius);
		Vec3[] endRing = diamondRing(end, right, up, radius);
		for (int index = 0; index < 4; index++) {
			int next = (index + 1) & 3;
			boolean alternate = (index & 1) == 0;
			quad(vertices, pose,
					startRing[index], endRing[index], endRing[next], startRing[next],
					alternate ? redA : redB, alternate ? greenA : greenB,
					alternate ? blueA : blueB, alpha);
		}
	}

	private static void drawCrystalHead(VertexConsumer vertices, PoseStack.Pose pose,
			Vec3 base, Vec3 tip, Vec3 right, Vec3 up, double radius,
			int redA, int greenA, int blueA, int redB, int greenB, int blueB, int alpha) {
		Vec3[] ring = diamondRing(base, right, up, radius);
		Vec3[] tipRing = diamondRing(tip, right, up, radius * 0.045D);
		for (int index = 0; index < 4; index++) {
			int next = (index + 1) & 3;
			boolean alternate = (index & 1) == 0;
			quad(vertices, pose,
					ring[index], tipRing[index], tipRing[next], ring[next],
					alternate ? redA : redB, alternate ? greenA : greenB,
					alternate ? blueA : blueB, alpha);
		}
		quad(vertices, pose,
				tipRing[0], tipRing[1], tipRing[2], tipRing[3],
				redB, greenB, blueB, alpha);
	}

	private static void drawFins(VertexConsumer vertices, PoseStack.Pose pose,
			Vec3 tail, Vec3 forward, Vec3 right, Vec3 up, double length, double radius,
			int red, int green, int blue, int alpha) {
		Vec3[] radial = new Vec3[] {right, up, right.scale(-1.0D), up.scale(-1.0D)};
		for (Vec3 direction : radial) {
			Vec3 rootBack = tail.add(forward.scale(length * 0.04D));
			Vec3 rootFront = tail.add(forward.scale(length));
			Vec3 outerFront = tail.add(forward.scale(length * 0.62D)).add(direction.scale(radius));
			Vec3 outerBack = tail.add(forward.scale(length * 0.12D)).add(direction.scale(radius * 0.62D));
			quad(vertices, pose, rootBack, rootFront, outerFront, outerBack,
					red, green, blue, alpha);
		}
	}

	private static Vec3[] diamondRing(Vec3 center, Vec3 right, Vec3 up, double radius) {
		return new Vec3[] {
				center.add(right.scale(radius)),
				center.add(up.scale(radius)),
				center.subtract(right.scale(radius)),
				center.subtract(up.scale(radius))
		};
	}

	private static void quad(VertexConsumer vertices, PoseStack.Pose pose,
			Vec3 first, Vec3 second, Vec3 third, Vec3 fourth,
			int red, int green, int blue, int alpha) {
		Vec3 faceNormal = second.subtract(first).cross(third.subtract(first));
		if (faceNormal.lengthSqr() < 0.0000001D)
			faceNormal = new Vec3(0.0D, 1.0D, 0.0D);
		else
			faceNormal = faceNormal.normalize();
		vertex(vertices, pose, first, faceNormal, red, green, blue, alpha);
		vertex(vertices, pose, second, faceNormal, red, green, blue, alpha);
		vertex(vertices, pose, third, faceNormal, red, green, blue, alpha);
		vertex(vertices, pose, fourth, faceNormal, red, green, blue, alpha);
	}

	private static void vertex(VertexConsumer vertices, PoseStack.Pose pose,
			Vec3 point, Vec3 faceNormal, int red, int green, int blue, int alpha) {
		vertices.addVertex(pose, (float) point.x, (float) point.y, (float) point.z)
				.setColor(red, green, blue, alpha)
				.setUv(0.5F, 0.5F)
				.setOverlay(OverlayTexture.NO_OVERLAY)
				.setLight(LightTexture.FULL_BRIGHT)
				.setNormal(pose, (float) faceNormal.x, (float) faceNormal.y, (float) faceNormal.z);
	}
}
