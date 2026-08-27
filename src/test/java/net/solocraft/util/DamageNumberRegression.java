package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Source-level guard for the server-to-client, shader-safe damage-number path. */
public final class DamageNumberRegression {
	private static final Path MAIN = Path.of("src", "main", "java", "net", "solocraft");

	private DamageNumberRegression() {
	}

	public static void main(String[] args) throws IOException {
		String manager = read("util", "DamageNumberManager.java");
		String packet = read("network", "ShowDamageNumberMessage.java");
		String renderer = read("procedures", "RenderDamageNumberProcedure.java");
		expect(manager.contains("LivingDamageEvent.Post")
				&& manager.contains("event.getNewDamage()")
				&& manager.contains("EventBusSubscriber(modid = SololevelingMod.MODID"),
				"Damage numbers must use final server damage from the NeoForge game bus");
		expect(packet.contains("NetworkDirection.PLAY_TO_CLIENT")
				&& packet.contains("DistExecutor.unsafeRunWhenOn(Dist.CLIENT"),
				"Damage-number packets must be client-bound and client-loaded safely");
		expect(renderer.contains("DeferredWorldShaderRenderer.isRenderStage")
				&& renderer.contains("DeferredWorldShaderRenderer.beginWorldPass(event)")
				&& renderer.contains("Font.DisplayMode.NORMAL"),
				"Damage numbers must render through the shader-compatible world pass");
		billboardTextKeepsVanillaWinding(renderer);
	}

	/**
	 * Text render types cull back faces, so a billboard's scale must reverse
	 * orientation exactly as EntityRenderer#renderNameTag does: negate Y alone, for
	 * a negative determinant. Negating X as well restores a positive determinant,
	 * which flips every glyph quad away from the camera. The draw calls still run
	 * and the numbers are silently invisible, which is expensive to diagnose in
	 * game, so pin the convention here.
	 */
	private static void billboardTextKeepsVanillaWinding(String renderer) {
		expect(determinantSign(1.0F, -1.0F, 1.0F) < 0,
				"Vanilla's nameplate scale must reverse orientation");
		expect(determinantSign(-1.0F, -1.0F, 1.0F) > 0,
				"Negating X as well cancels the flip and back-faces the glyphs");
		expect(renderer.contains("poseStack.scale(scale, -scale, scale)"),
				"The damage-number billboard must negate Y alone, like vanilla nameplates");
		expect(!renderer.contains("poseStack.scale(-scale"),
				"A negated X scale culls every glyph quad");
	}

	private static float determinantSign(float x, float y, float z) {
		return x * y * z;
	}

	private static String read(String... parts) throws IOException {
		Path path = MAIN;
		for (String part : parts)
			path = path.resolve(part);
		return Files.readString(path);
	}

	private static void expect(boolean value, String message) {
		if (!value)
			throw new AssertionError(message);
	}
}
