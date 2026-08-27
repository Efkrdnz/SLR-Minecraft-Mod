package net.solocraft.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Refuses to ship a HUD overlay that leaves the render pipeline tinted.
 *
 * <p>{@code RenderSystem.setShaderColor} is global. An overlay that finishes
 * without putting it back to opaque white hands that colour to whatever draws
 * next -- and because the HUD stops rendering the moment a screen opens, the
 * colour it left behind is the colour the inventory, chat and F3 screens are
 * drawn in.
 *
 * <p>{@code NewDailyQuestOverlay} restored the colour to <em>its own</em> alpha
 * rather than to white. While the warning was hidden that alpha was zero, so
 * every HUD frame ended with the pipeline at alpha zero and opening any screen
 * showed nothing at all. It was reported as a black screen, blamed on a
 * launcher, and reproduced by nobody who happened to be in combat mode -- where
 * another overlay reset the colour and hid it.
 *
 * <p>Checked as source text because the failure is a value left in a global,
 * which no unit test of the drawing code would notice.
 */
public final class OverlayRenderStateRegression {
	private static final Path OVERLAYS = Path.of("src", "main", "java", "net",
			"solocraft", "client", "screens");

	private static final Pattern SHADER_COLOR = Pattern.compile(
			"setShaderColor\\(([^;]*?)\\);", Pattern.DOTALL);

	/** Opaque white, with or without float suffixes and any spacing. */
	private static final Pattern OPAQUE_WHITE = Pattern.compile(
			"^\\s*1(?:\\.0)?[fF]?\\s*,\\s*1(?:\\.0)?[fF]?\\s*,"
					+ "\\s*1(?:\\.0)?[fF]?\\s*,\\s*1(?:\\.0)?[fF]?\\s*$");

	private OverlayRenderStateRegression() {
	}

	public static void main(String[] args) throws IOException {
		List<String> offences = new ArrayList<>();
		int scanned = 0;

		try (Stream<Path> files = Files.walk(OVERLAYS)) {
			List<Path> sources = files
					.filter(path -> path.getFileName().toString().endsWith(".java"))
					.sorted()
					.toList();
			for (Path source : sources) {
				String text = Files.readString(source).replace("\r\n", "\n");
				if (!text.contains("setShaderColor"))
					continue;
				scanned++;

				Matcher matcher = SHADER_COLOR.matcher(text);
				String last = null;
				while (matcher.find())
					last = matcher.group(1).replaceAll("\\s+", " ").trim();

				if (last != null && !OPAQUE_WHITE.matcher(last).matches())
					offences.add(source.getFileName()
							+ " ends with setShaderColor(" + trim(last) + ")");
			}
		}

		if (!offences.isEmpty()) {
			StringBuilder message = new StringBuilder(
					"HUD overlays must leave the shader colour at opaque white. These "
							+ "hand their own tint to whatever renders next, which is the "
							+ "next screen the player opens:\n");
			for (String offence : offences)
				message.append("  ").append(offence).append('\n');
			throw new AssertionError(message.toString());
		}

		expect(scanned > 0, "No overlays were scanned; the path is probably wrong");
		System.out.println("Overlay render state regression passed (" + scanned
				+ " overlays scanned).");
	}

	private static String trim(String value) {
		return value.length() <= 70 ? value : value.substring(0, 70) + "...";
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
