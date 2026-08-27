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
 * Refuses to ship a core shader that uses a GLSL reserved word as an identifier.
 *
 * <p>This class of bug is invisible on the machine that writes it. NVIDIA's
 * compiler accepts reserved words as identifiers; AMD's and Intel's reject the
 * shader outright. A core shader that fails to compile takes the whole render
 * pipeline with it, so the game loads, plays its music, accepts clicks, and
 * draws absolutely nothing -- with no crash and no crash report.
 *
 * <p>{@code rendertype_bless_sigil.fsh} shipped with {@code float active} and
 * black-screened every player on a strict driver while working perfectly for
 * everyone testing it. Nothing but a scan catches that before release, because
 * the author's own machine says the shader is fine.
 */
public final class ShaderReservedWordRegression {
	private static final Path SHADERS = Path.of("src", "main", "resources",
			"assets", "sololeveling", "shaders");

	/**
	 * Reserved for future use by the GLSL specification. Not an exhaustive list
	 * of every keyword -- these are the ones plausible enough as a variable name
	 * that someone would actually reach for one.
	 */
	private static final String[] RESERVED = {
			"common", "partition", "active", "asm", "class", "union", "enum",
			"typedef", "template", "this", "resource", "goto", "inline", "noinline",
			"public", "static", "extern", "external", "interface", "long", "short",
			"half", "fixed", "unsigned", "superp", "input", "output", "filter",
			"sizeof", "cast", "namespace", "using", "patch", "sample", "subroutine",
			"row_major", "attribute", "varying", "packed",
	};

	private static final Pattern DECLARED = Pattern.compile(
			"\\b(?:float|int|vec2|vec3|vec4|bool|mat2|mat3|mat4|uint|double)\\s+(\\w+)\\b");
	private static final Pattern ASSIGNED = Pattern.compile("\\b(\\w+)\\s*=[^=]");

	private ShaderReservedWordRegression() {
	}

	public static void main(String[] args) throws IOException {
		List<String> offences = new ArrayList<>();
		int scanned = 0;

		try (Stream<Path> files = Files.walk(SHADERS)) {
			List<Path> sources = files
					.filter(path -> {
						String name = path.getFileName().toString();
						return name.endsWith(".fsh") || name.endsWith(".vsh");
					})
					.sorted()
					.toList();
			for (Path source : sources) {
				scanned++;
				String[] lines = Files.readString(source).replace("\r\n", "\n").split("\n");
				for (int number = 1; number <= lines.length; number++) {
					// Comments are prose and may say the word freely; this file does.
					String code = stripComment(lines[number - 1]);
					for (String word : identifiers(code)) {
						if (isReserved(word))
							offences.add(SHADERS.relativize(source) + ":" + number
									+ " uses the GLSL reserved word \"" + word + "\"");
					}
				}
			}
		}

		if (!offences.isEmpty()) {
			StringBuilder message = new StringBuilder(
					"GLSL reserved words used as identifiers. A strict driver refuses "
							+ "to compile these, which black-screens the game:\n");
			for (String offence : offences)
				message.append("  ").append(offence).append('\n');
			throw new AssertionError(message.toString());
		}

		expect(scanned > 0, "No shaders were scanned; the path is probably wrong");
		System.out.println("Shader reserved-word regression passed (" + scanned
				+ " shaders scanned).");
	}

	private static String stripComment(String line) {
		int at = line.indexOf("//");
		return at < 0 ? line : line.substring(0, at);
	}

	private static List<String> identifiers(String code) {
		List<String> found = new ArrayList<>();
		Matcher declared = DECLARED.matcher(code);
		while (declared.find())
			found.add(declared.group(1));
		Matcher assigned = ASSIGNED.matcher(code);
		while (assigned.find())
			found.add(assigned.group(1));
		return found;
	}

	private static boolean isReserved(String word) {
		for (String reserved : RESERVED)
			if (reserved.equals(word))
				return true;
		return false;
	}

	private static void expect(boolean condition, String message) {
		if (!condition)
			throw new AssertionError(message);
	}
}
