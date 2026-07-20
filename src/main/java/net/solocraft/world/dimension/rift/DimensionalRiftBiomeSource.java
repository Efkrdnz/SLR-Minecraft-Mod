package net.solocraft.world.dimension.rift;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Angular biome source for the eight Monarch territories and central star. */
public final class DimensionalRiftBiomeSource extends BiomeSource {
	public static final Codec<DimensionalRiftBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Biome.CODEC.fieldOf("battlefield").forGetter(source -> source.battlefield),
			Biome.CODEC.fieldOf("wasteland").forGetter(source -> source.wasteland),
			Biome.CODEC.fieldOf("rift_scar").forGetter(source -> source.riftScar),
			Biome.CODEC.fieldOf("void_biome").forGetter(source -> source.voidBiome),
			Biome.LIST_CODEC.fieldOf("territories").forGetter(source -> HolderSet.direct(source.territories)),
			Codec.DOUBLE.optionalFieldOf("safe_center_radius", RiftGeometry.DEFAULT_SAFE_CENTER_RADIUS).forGetter(source -> source.safeCenterRadius),
			Codec.DOUBLE.optionalFieldOf("star_core_radius", RiftGeometry.DEFAULT_STAR_CORE_RADIUS).forGetter(source -> source.starCoreRadius),
			Codec.DOUBLE.optionalFieldOf("star_tip_radius", RiftGeometry.DEFAULT_STAR_TIP_RADIUS).forGetter(source -> source.starTipRadius),
			Codec.DOUBLE.optionalFieldOf("star_exponent", RiftGeometry.DEFAULT_STAR_EXPONENT).forGetter(source -> source.starExponent),
			Codec.DOUBLE.optionalFieldOf("scar_half_width", RiftGeometry.DEFAULT_SCAR_HALF_WIDTH).forGetter(source -> source.scarHalfWidth),
			Codec.DOUBLE.optionalFieldOf("playable_radius", RiftGeometry.DEFAULT_PLAYABLE_RADIUS).forGetter(source -> source.playableRadius)
	).apply(instance, DimensionalRiftBiomeSource::new));

	private final Holder<Biome> battlefield;
	private final Holder<Biome> wasteland;
	private final Holder<Biome> riftScar;
	private final Holder<Biome> voidBiome;
	private final List<Holder<Biome>> territories;
	private final double safeCenterRadius;
	private final double starCoreRadius;
	private final double starTipRadius;
	private final double starExponent;
	private final double scarHalfWidth;
	private final double playableRadius;

	public DimensionalRiftBiomeSource(Holder<Biome> battlefield, Holder<Biome> wasteland,
			Holder<Biome> riftScar, Holder<Biome> voidBiome, HolderSet<Biome> territories,
			double safeCenterRadius, double starCoreRadius, double starTipRadius,
			double starExponent, double scarHalfWidth, double playableRadius) {
		this.battlefield = battlefield;
		this.wasteland = wasteland;
		this.riftScar = riftScar;
		this.voidBiome = voidBiome;
		this.territories = List.copyOf(territories.stream().toList());
		if (this.territories.size() != RiftTerritory.values().length)
			throw new IllegalArgumentException("Dimensional Rift requires exactly eight ordered territory biomes");
		this.safeCenterRadius = Math.max(0.0D, safeCenterRadius);
		this.starCoreRadius = Math.max(this.safeCenterRadius, starCoreRadius);
		this.starTipRadius = Math.max(this.starCoreRadius, starTipRadius);
		this.starExponent = Math.max(0.1D, starExponent);
		this.scarHalfWidth = Math.max(0.0D, scarHalfWidth);
		this.playableRadius = Math.max(this.starTipRadius, playableRadius);
	}

	@Override
	protected Codec<? extends BiomeSource> codec() {
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes() {
		List<Holder<Biome>> biomes = new ArrayList<>(12);
		biomes.add(battlefield);
		biomes.add(wasteland);
		biomes.add(riftScar);
		biomes.add(voidBiome);
		biomes.addAll(territories);
		return biomes.stream();
	}

	@Override
	public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
		double x = quartX << 2;
		double z = quartZ << 2;
		RiftGeometry.Region region = RiftGeometry.resolve(x, z, safeCenterRadius, starCoreRadius,
				starTipRadius, starExponent, scarHalfWidth, playableRadius);
		return switch (region.type()) {
			case CENTER -> battlefield;
			case WASTELAND -> wasteland;
			case RIFT_SCAR -> riftScar;
			case VOID -> voidBiome;
			case TERRITORY -> territories.get(region.territory().ordinal());
		};
	}
}
