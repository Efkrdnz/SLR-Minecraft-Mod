package net.solocraft.world.dimension.rift;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Seed-aware terrain profile for the finite Dimensional Rift island and its
 * standalone Monarch-territory dimensions. Omitting {@code territory} keeps
 * the finite radial rift; setting it forces that profile across an infinite
 * standalone level. Vanilla child noises remain world-seed aware.
 */
public record RiftTerrainDensityFunction(
		DensityFunction baseNoise,
		DensityFunction detailNoise,
		DensityFunction caveNoise,
		double fadeStart,
		double playableRadius,
		String territory) implements DensityFunction.SimpleFunction {

	public static final KeyDispatchDataCodec<RiftTerrainDensityFunction> CODEC = KeyDispatchDataCodec.of(
			RecordCodecBuilder.mapCodec(instance -> instance.group(
					DensityFunction.HOLDER_HELPER_CODEC.fieldOf("base_noise").forGetter(RiftTerrainDensityFunction::baseNoise),
					DensityFunction.HOLDER_HELPER_CODEC.fieldOf("detail_noise").forGetter(RiftTerrainDensityFunction::detailNoise),
					DensityFunction.HOLDER_HELPER_CODEC.fieldOf("cave_noise").forGetter(RiftTerrainDensityFunction::caveNoise),
					Codec.doubleRange(256.0D, 30000000.0D).optionalFieldOf("fade_start", RiftGeometry.DEFAULT_TERRAIN_FADE_START).forGetter(RiftTerrainDensityFunction::fadeStart),
					Codec.doubleRange(512.0D, 30000000.0D).optionalFieldOf("playable_radius", RiftGeometry.DEFAULT_PLAYABLE_RADIUS).forGetter(RiftTerrainDensityFunction::playableRadius),
					Codec.STRING.optionalFieldOf("territory", "").forGetter(RiftTerrainDensityFunction::territory)
			).apply(instance, RiftTerrainDensityFunction::new)));

	public RiftTerrainDensityFunction {
		fadeStart = Math.max(RiftGeometry.DEFAULT_STAR_TIP_RADIUS, fadeStart);
		playableRadius = Math.max(fadeStart + 16.0D, playableRadius);
		territory = territory == null ? "" : territory.strip().toLowerCase().replace('-', '_').replace(' ', '_');
		RiftTerritory forcedTerritory = RiftTerritory.fromName(territory);
		if (!territory.isBlank() && forcedTerritory == null)
			throw new IllegalArgumentException("Unknown Monarch territory '" + territory + "'");
		if (forcedTerritory != null)
			territory = forcedTerritory.id();
	}

	@Override
	public double compute(FunctionContext context) {
		double x = context.blockX();
		double z = context.blockZ();
		double radius = RiftGeometry.distance(x, z);
		RiftTerritory forcedTerritory = RiftTerritory.fromName(territory);
		if (forcedTerritory == null && radius >= playableRadius)
			return -1.0D;

		double broad = Mth.clamp(baseNoise.compute(context), -1.0D, 1.0D);
		double detail = Mth.clamp(detailNoise.compute(context), -1.0D, 1.0D);
		double height = forcedTerritory == null
				? terrainHeight(RiftGeometry.resolveDefault(x, z), broad, detail)
				: territoryHeight(forcedTerritory, broad, detail);

		if (forcedTerritory == null && radius > fadeStart) {
			double fade = RiftGeometry.smoothStep((radius - fadeStart) / (playableRadius - fadeStart));
			height = Mth.lerp(fade, height, -92.0D);
		}

		double density = (height - context.blockY()) / 24.0D;
		if (context.blockY() < height - 12.0D && context.blockY() > -48) {
			double cave = Math.abs(caveNoise.compute(context));
			double carve = Mth.clamp((0.105D - cave) * 18.0D, 0.0D, 1.35D);
			density -= carve;
		}
		return Mth.clamp(density, -1.0D, 1.0D);
	}

	private static double terrainHeight(RiftGeometry.Region region, double broad, double detail) {
		return switch (region.type()) {
			case CENTER -> 82.0D + broad * 2.5D + detail;
			case WASTELAND -> 78.0D + broad * 7.0D + detail * 3.0D;
			case RIFT_SCAR -> 64.0D + broad * 6.0D - Math.abs(detail) * 9.0D;
			case VOID -> -92.0D;
			case TERRITORY -> territoryHeight(region.territory(), broad, detail);
		};
	}

	private static double territoryHeight(RiftTerritory territory, double broad, double detail) {
		return switch (territory) {
			case DESTRUCTION -> 82.0D + broad * 24.0D + Math.abs(detail) * 12.0D;
			case FROST -> 102.0D + broad * 38.0D + Math.abs(detail) * 20.0D;
			case FANGS -> 86.0D + broad * 19.0D + detail * 8.0D;
			case PLAGUES -> 70.0D + broad * 9.0D - Math.abs(detail) * 5.0D;
			case IRON_BODY -> 94.0D + broad * 30.0D + detail * 7.0D;
			case WHITE_FLAMES -> 80.0D + broad * 20.0D + Math.abs(detail) * 11.0D;
			case TRANSFIGURATION -> 94.0D + broad * 31.0D + detail * 16.0D;
			case BEGINNING -> 86.0D + broad * 23.0D + detail * 8.0D;
		};
	}

	@Override
	public DensityFunction mapAll(Visitor visitor) {
		return visitor.apply(new RiftTerrainDensityFunction(
				baseNoise.mapAll(visitor), detailNoise.mapAll(visitor), caveNoise.mapAll(visitor),
				fadeStart, playableRadius, territory));
	}

	@Override
	public double minValue() {
		return -1.0D;
	}

	@Override
	public double maxValue() {
		return 1.0D;
	}

	@Override
	public KeyDispatchDataCodec<? extends DensityFunction> codec() {
		return CODEC;
	}
}
