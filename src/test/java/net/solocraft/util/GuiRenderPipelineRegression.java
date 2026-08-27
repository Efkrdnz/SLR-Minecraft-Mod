package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Source-contract checks for the Minecraft 1.21 screen/background pipeline.
 *
 * <p>Unlike 1.20, {@code Screen.render} and
 * {@code AbstractContainerScreen.render} invoke {@code renderBackground}
 * themselves. Keeping the old explicit call before {@code super.render}
 * applies menu blur twice and can cover custom labels. Responsive screens may
 * keep an outer, unscaled background pass only when they suppress the nested
 * virtual call.</p>
 */
public final class GuiRenderPipelineRegression {
	private static final Path GUI = Path.of("src", "main", "java", "net",
			"solocraft", "client", "gui");
	private static final Path SCREENS = Path.of("src", "main", "java", "net",
			"solocraft", "client", "screens");

	private GuiRenderPipelineRegression() {
	}

	public static void main(String[] args) throws IOException {
		legacyBackgroundPassesAreRemovedOrGuarded();
		awakeningOddsDrawsInformationAfterTheSingleBackgroundPass();
		worldCreationHelpTextIsSharpAndFitsItsRow();
		notificationsUseTheIrisSafeLateWorldPassAndReadableTextLayers();
		skillSelectionKeepsTheMousePositionAcrossContainerSwaps();
		liuSwordVfxUsesAlphaCompositing();
	}

	private static void legacyBackgroundPassesAreRemovedOrGuarded()
			throws IOException {
		List<String> violations = new ArrayList<>();
		try (Stream<Path> sources = Files.walk(GUI)) {
			for (Path file : sources.filter(path -> path.toString().endsWith(".java"))
					.toList()) {
				String source = Files.readString(file);
				int from = 0;
				while (true) {
					int superRender = source.indexOf("super.render(", from);
					if (superRender < 0)
						break;
					from = superRender + 1;
					int renderMethod = source.lastIndexOf("public void render(", superRender);
					int explicitBackground = source.lastIndexOf("renderBackground(", superRender);
					int transparentBackground = source.lastIndexOf(
							"renderTransparentBackground(", superRender);
					if (renderMethod < 0 || (explicitBackground < renderMethod
							&& transparentBackground < renderMethod))
						continue;

					String beforeSuper = source.substring(renderMethod, superRender);
					boolean screenGuard = source.contains("!suppressNestedBackground")
							|| source.contains("!this.suppressNestedBackground");
					boolean containerSplit = source.contains(
							"if (suppressNestedBackground)")
							|| source.contains("if (this.suppressNestedBackground)");
					boolean guarded = beforeSuper.contains(
							"suppressNestedBackground = true")
							&& source.contains("void renderBackground(")
							&& (screenGuard || (containerSplit
									&& source.contains("renderBg(graphics, partialTick")));
					if (!guarded)
						violations.add(file + " retains an unguarded background-before-super pass");
				}
			}
		}
		if (!violations.isEmpty())
			throw new AssertionError(String.join(System.lineSeparator(), violations));
	}

	private static void awakeningOddsDrawsInformationAfterTheSingleBackgroundPass()
			throws IOException {
		String source = read(GUI.resolve(Path.of("worldcreation",
				"AwakeningOddsScreen.java")));
		int render = source.indexOf("public void render(");
		int superRender = source.indexOf("super.render(", render);
		int heading = source.indexOf("graphics.drawCenteredString", render);
		expectTrue(render >= 0 && superRender > render && heading > superRender,
				"Awakening labels/bar must render after Screen's single background pass");
		String renderBody = source.substring(render, source.indexOf(
				"private void renderDistributionBar", render));
		expectFalse(renderBody.contains("renderBackground("),
				"Awakening screen must not invoke the 1.21 background twice");
	}

	private static void worldCreationHelpTextIsSharpAndFitsItsRow()
			throws IOException {
		String source = read(GUI.resolve(Path.of("worldcreation",
				"SoloLevelingWorldCreationTab.java")));
		assertContains(source,
				"Individual changes switch their preset to Custom.",
				"0xFFC8D2DA",
				"0xFFB6C3CC",
				"class CrispStringWidget",
				"this.color, false");
		expectFalse(source.contains(
				"Presets update the settings below; individual changes use Custom."),
				"The world-creation footer must not be clipped with an ellipsis");
	}

	private static void notificationsUseTheIrisSafeLateWorldPassAndReadableTextLayers()
			throws IOException {
		String source = read(SCREENS.resolve(
				"SystemNotificationWorldRenderer.java"));
		assertContains(source,
				"DeferredWorldShaderRenderer.isRenderStage(event",
				"RenderLevelStageEvent.Stage.AFTER_ENTITIES",
				"DeferredWorldShaderRenderer.beginWorldPass(event)",
				"DeferredWorldShaderRenderer.worldPoseStack(event)",
				"DeferredWorldShaderRenderer.endWorldPass()",
				"ps.scale(s, -s, s)",
				"Font.DisplayMode.SEE_THROUGH",
				"Font.DisplayMode.NORMAL");
	}

	private static void skillSelectionKeepsTheMousePositionAcrossContainerSwaps()
			throws IOException {
		String restore = read(GUI.resolve("SkillScreenCursorRestore.java"));
		String slots = read(GUI.resolve("EquippedAbilitiesScreen.java"));
		String skills = read(GUI.resolve("UnlockedSkillsTab1Screen.java"));
		assertContains(restore, "GLFW.glfwSetCursorPos", "ClientTickEvent.Post",
				"MAX_WAIT_TICKS", "UnlockedSkillsTab1Screen", "EquippedAbilitiesScreen");
		assertContains(slots, "SkillScreenCursorRestore.preserveForSkillList()");
		assertContains(skills, "SkillScreenCursorRestore.preserveForSlots()");
	}

	private static void liuSwordVfxUsesAlphaCompositing() throws IOException {
		Path shader = Path.of("src", "main", "java", "net", "solocraft", "client",
				"renderer", "shader", "LiuSwordRenderTypes.java");
		Path definition = Path.of("src", "main", "resources", "assets", "sololeveling",
				"shaders", "core", "rendertype_liu_sword.json");
		String renderType = read(shader);
		String json = read(definition);
		assertContains(renderType, "EFFECT_TYPES", "TRANSLUCENT_TRANSPARENCY",
				"ADDITIVE_TRANSPARENCY is ONE, ONE");
		expectFalse(renderType.contains(".setTransparencyState(ADDITIVE_TRANSPARENCY)"),
				"Liu VFX must not use ONE, ONE blending because it bypasses vertex alpha");
		assertContains(json, "\"dstrgb\": \"1-srcalpha\"");
	}

	private static String read(Path path) throws IOException {
		return Files.readString(path);
	}

	private static void assertContains(String source, String... needles) {
		for (String needle : needles)
			expectTrue(source.contains(needle), "Missing source contract: " + needle);
	}

	private static void expectTrue(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}

	private static void expectFalse(boolean condition, String message) {
		expectTrue(!condition, message);
	}
}
