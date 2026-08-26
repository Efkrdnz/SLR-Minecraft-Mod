package net.solocraft.world.dimension.rift;

import com.mojang.serialization.MapCodec;

import net.solocraft.SololevelingMod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Serializers used by the data-driven Rift dimension JSON. */
public final class RiftWorldgenRegistries {
	private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
			DeferredRegister.create(Registries.BIOME_SOURCE, SololevelingMod.MODID);
	private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTIONS =
			DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, SololevelingMod.MODID);

	public static final DeferredHolder<MapCodec<? extends BiomeSource>, MapCodec<? extends BiomeSource>> DIMENSIONAL_RIFT_BIOME_SOURCE =
			BIOME_SOURCES.register("dimensional_rift", () -> DimensionalRiftBiomeSource.CODEC);
	public static final DeferredHolder<MapCodec<? extends DensityFunction>, MapCodec<? extends DensityFunction>> RIFT_TERRAIN =
			DENSITY_FUNCTIONS.register("rift_terrain", () -> RiftTerrainDensityFunction.CODEC.codec());

	private RiftWorldgenRegistries() {
	}

	public static void register(IEventBus bus) {
		BIOME_SOURCES.register(bus);
		DENSITY_FUNCTIONS.register(bus);
	}
}
