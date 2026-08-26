package net.solocraft.api.hunter;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/**
 * How a contributed class presents itself during evaluation.
 *
 * <p>Declared in JSON under {@code data/<namespace>/sololeveling/classes}, and
 * shipping one is what makes a class rollable. A class registered without a
 * presentation still exists and can be granted, it just is not offered by the
 * Evaluator -- which is the difference between a class an addon awards itself
 * and one the System can hand you.
 *
 * <p>Requiring it also means the Evaluator can never land on a class with
 * nothing to say about itself.
 *
 * <pre>
 * {
 *   "class": "yourmod:necromancer",
 *   "description": "Authority borrowed from what is already buried.",
 *   "color": "#8FE3B0"
 * }
 * </pre>
 *
 * @param classId     the registered class this describes.
 * @param description one line the Evaluator shows beneath the class name.
 * @param color       packed ARGB, used wherever the class is themed.
 */
public record HunterClassPresentation(ResourceLocation classId, String description, int color) {

	/** Used when a file omits the colour. Neutral rather than a guess. */
	public static final int DEFAULT_COLOR = 0xFFD8D8D8;

	public HunterClassPresentation {
		Objects.requireNonNull(classId, "A presentation must name its class");
		description = description == null ? "" : description.trim();
		if (description.isEmpty())
			throw new IllegalArgumentException("Class " + classId
					+ " needs a description; the Evaluator shows it when the class is drawn");
	}
}
