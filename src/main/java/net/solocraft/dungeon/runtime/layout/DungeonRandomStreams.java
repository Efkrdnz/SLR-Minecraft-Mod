package net.solocraft.dungeon.runtime.layout;

import net.minecraft.util.RandomSource;

/**
 * Derives stable, independent random streams from one public dungeon seed.
 * Layout preview cannot be perturbed by level variance or encounter rolls.
 */
public final class DungeonRandomStreams {
	private static final long LEVEL_SALT = 0x5D3A_91E7_0B24_C86DL;
	private static final long LAYOUT_SALT = 0xA17C_4F29_D630_8BE5L;
	private static final long ENCOUNTER_SALT = 0xC82E_16B5_74F9_03A1L;

	private DungeonRandomStreams() {
	}

	public enum Stream {
		LEVEL,
		LAYOUT,
		ENCOUNTER
	}

	public static long seed(long rootSeed, Stream stream) {
		long salt = switch (stream) {
			case LEVEL -> LEVEL_SALT;
			case LAYOUT -> LAYOUT_SALT;
			case ENCOUNTER -> ENCOUNTER_SALT;
		};
		return mix(rootSeed ^ salt);
	}

	public static RandomSource random(long rootSeed, Stream stream) {
		return RandomSource.create(seed(rootSeed, stream));
	}

	/** SplitMix64 finalizer, also used for deterministic per-state ordering. */
	public static long mix(long value) {
		value = (value ^ (value >>> 30)) * 0xBF58_476D_1CE4_E5B9L;
		value = (value ^ (value >>> 27)) * 0x94D0_49BB_1331_11EBL;
		return value ^ (value >>> 31);
	}
}
