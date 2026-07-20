package net.solocraft.world.dimension.rift;

/**
 * Pure coordinate math shared by world generation, biome selection, commands,
 * and mob progression. Keeping it here prevents the server and client from
 * disagreeing about which Monarch owns a position.
 */
public final class RiftGeometry {
	public static final double TWO_PI = Math.PI * 2.0D;
	public static final double SLICE_ANGLE = Math.PI / 4.0D;
	public static final double HALF_SLICE_ANGLE = Math.PI / 8.0D;

	public static final double DEFAULT_SAFE_CENTER_RADIUS = 256.0D;
	public static final double DEFAULT_STAR_CORE_RADIUS = 600.0D;
	public static final double DEFAULT_STAR_TIP_RADIUS = 1200.0D;
	public static final double DEFAULT_STAR_EXPONENT = 1.7D;
	public static final double DEFAULT_SCAR_HALF_WIDTH = 56.0D;
	public static final double DEFAULT_TERRAIN_FADE_START = 3180.0D;
	public static final double DEFAULT_PLAYABLE_RADIUS = 3500.0D;

	private RiftGeometry() {
	}

	public static double distance(double x, double z) {
		return Math.hypot(x, z);
	}

	public static double angle(double x, double z) {
		double angle = Math.atan2(z, x);
		return angle < 0.0D ? angle + TWO_PI : angle;
	}

	/** Returns the slice centered on 0, 45, 90 ... degrees. */
	public static int territoryIndex(double x, double z) {
		double shifted = positiveModulo(angle(x, z) + HALF_SLICE_ANGLE, TWO_PI);
		return Math.floorMod((int) Math.floor(shifted / SLICE_ANGLE), RiftTerritory.values().length);
	}

	public static RiftTerritory territory(double x, double z) {
		return RiftTerritory.byIndex(territoryIndex(x, z));
	}

	/** Smooth eight-point star with its points aligned to slice boundaries. */
	public static double starRadius(double angle, double coreRadius, double tipRadius, double exponent) {
		double lobe = 0.5D + 0.5D * Math.cos(8.0D * (angle - HALF_SLICE_ANGLE));
		double shaped = Math.pow(clamp(lobe, 0.0D, 1.0D), Math.max(0.1D, exponent));
		return lerp(shaped, Math.min(coreRadius, tipRadius), Math.max(coreRadius, tipRadius));
	}

	public static boolean insideStar(double x, double z, double coreRadius, double tipRadius, double exponent) {
		return distance(x, z) <= starRadius(angle(x, z), coreRadius, tipRadius, exponent);
	}

	/** Shortest block distance to either ray bordering the current slice. */
	public static double distanceToNearestBoundary(double x, double z) {
		double radius = distance(x, z);
		if (radius == 0.0D)
			return 0.0D;
		int territory = territoryIndex(x, z);
		double center = territory * SLICE_ANGLE;
		double fromCenter = Math.abs(wrapRadians(angle(x, z) - center));
		double angularDistance = Math.max(0.0D, HALF_SLICE_ANGLE - fromCenter);
		return radius * Math.sin(angularDistance);
	}

	public static Region resolveDefault(double x, double z) {
		return resolve(x, z, DEFAULT_SAFE_CENTER_RADIUS, DEFAULT_STAR_CORE_RADIUS,
				DEFAULT_STAR_TIP_RADIUS, DEFAULT_STAR_EXPONENT, DEFAULT_SCAR_HALF_WIDTH,
				DEFAULT_PLAYABLE_RADIUS);
	}

	public static Region resolve(double x, double z, double safeCenterRadius, double starCoreRadius,
			double starTipRadius, double starExponent, double scarHalfWidth, double playableRadius) {
		double radius = distance(x, z);
		if (radius > playableRadius)
			return Region.voidRegion(radius);
		if (radius <= safeCenterRadius)
			return Region.center(radius);
		if (insideStar(x, z, starCoreRadius, starTipRadius, starExponent))
			return Region.wasteland(radius);
		if (distanceToNearestBoundary(x, z) <= scarHalfWidth)
			return Region.scar(radius);
		return Region.territory(radius, territory(x, z));
	}

	/** Intended natural-spawn level for the outward progression route. */
	public static int levelForDistance(double distance) {
		double start = DEFAULT_SAFE_CENTER_RADIUS;
		double end = DEFAULT_TERRAIN_FADE_START;
		double progress = clamp((distance - start) / (end - start), 0.0D, 1.0D);
		progress = Math.pow(progress, 1.08D);
		return clamp((int) Math.round(lerp(progress, 5.0D, 100.0D)), 5, 100);
	}

	public static double smoothStep(double value) {
		double clamped = clamp(value, 0.0D, 1.0D);
		return clamped * clamped * (3.0D - 2.0D * clamped);
	}

	private static double lerp(double amount, double start, double end) {
		return start + amount * (end - start);
	}

	private static double clamp(double value, double minimum, double maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private static double positiveModulo(double value, double modulus) {
		double result = value % modulus;
		return result < 0.0D ? result + modulus : result;
	}

	private static double wrapRadians(double value) {
		return positiveModulo(value + Math.PI, TWO_PI) - Math.PI;
	}

	public enum RegionType {
		CENTER,
		WASTELAND,
		RIFT_SCAR,
		TERRITORY,
		VOID
	}

	public record Region(RegionType type, double distance, RiftTerritory territory) {
		private static Region center(double distance) {
			return new Region(RegionType.CENTER, distance, null);
		}

		private static Region wasteland(double distance) {
			return new Region(RegionType.WASTELAND, distance, null);
		}

		private static Region scar(double distance) {
			return new Region(RegionType.RIFT_SCAR, distance, null);
		}

		private static Region territory(double distance, RiftTerritory territory) {
			return new Region(RegionType.TERRITORY, distance, territory);
		}

		private static Region voidRegion(double distance) {
			return new Region(RegionType.VOID, distance, null);
		}
	}
}
