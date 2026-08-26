package net.solocraft.api.vessel;

import java.util.Locale;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/**
 * How a contributed Monarch looks on the vessel selection screen.
 *
 * <p>Declared in JSON under {@code data/<namespace>/sololeveling/vessels}:
 *
 * <pre>
 * {
 *   "vessel": "yourmod:kaelith",
 *   "color": "#8FE3B0",
 *   "backdrop": "frost"
 * }
 * </pre>
 *
 * <p>Monarchs only. Rulers present identically to each other by design -- that
 * is what separates the two columns -- so a presentation naming a Ruler is
 * reported and skipped rather than quietly ignored.
 *
 * <p>A Monarch without a presentation keeps the neutral Monarch theming, which
 * is what every contributed Monarch used before this existed.
 *
 * @param vesselId the registered Monarch this describes.
 * @param color    packed ARGB. Drives the name, the accents, the cursor glow,
 *                 and the tint over the animated backdrop.
 * @param backdrop which shipped animation plays behind the panel.
 */
public record VesselPresentation(ResourceLocation vesselId, int color, Backdrop backdrop) {

	/** The neutral Monarch purple, unchanged from before presentations existed. */
	public static final int DEFAULT_COLOR = 0xFFB864FF;

	/**
	 * The animated backdrops the selection shader can draw.
	 *
	 * <p>These are hand-written GLSL inside the mod's core shader, so an addon
	 * picks one rather than shipping its own -- a core shader cannot be extended
	 * from outside. The chosen animation is tinted toward the declared colour,
	 * so two Monarchs sharing a backdrop still read as different.
	 */
	public enum Backdrop {
		/** Drifting shadow tendrils. The neutral Monarch default. */
		SHADOW(0),
		/** Slow crystalline drift. */
		FROST(1),
		/** Pale rising flame. */
		WHITE_FLAME(2),
		/** Restless, clawing motion. */
		BEAST(3),
		/** Fracturing collapse. */
		DESTRUCTION(4),
		/** The System's own grid, for a Monarch that reads as authority. */
		SYSTEM(5);

		private final int shaderIndex;

		Backdrop(int shaderIndex) {
			this.shaderIndex = shaderIndex;
		}

		/** The value handed to the shader's {@code CustomBackdrop} uniform. */
		public int shaderIndex() {
			return shaderIndex;
		}

		public static Backdrop parse(String raw) {
			if (raw == null || raw.isBlank())
				return SHADOW;
			try {
				return valueOf(raw.trim().toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				throw new IllegalArgumentException("backdrop \"" + raw
						+ "\" is not one of " + java.util.Arrays.toString(values()));
			}
		}
	}

	public VesselPresentation {
		Objects.requireNonNull(vesselId, "A presentation must name its vessel");
		if (backdrop == null)
			backdrop = Backdrop.SHADOW;
		// Opaque unless the file said otherwise, so a six-digit colour is not
		// silently invisible.
		if ((color & 0xFF000000) == 0)
			color |= 0xFF000000;
	}

	/** The colour as the shader wants it: three floats, no alpha. */
	public float[] shaderColor() {
		return new float[] {
				((color >> 16) & 0xFF) / 255.0F,
				((color >> 8) & 0xFF) / 255.0F,
				(color & 0xFF) / 255.0F
		};
	}
}
