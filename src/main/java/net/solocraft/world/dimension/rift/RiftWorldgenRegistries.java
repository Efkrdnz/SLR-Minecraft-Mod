package net.solocraft.world.dimension.rift;

import com.mojang.serialization.Codec;

import net.solocraft.SololevelingMod;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Serializers used by the data-driven Rift dimension JSON. */
public final class RiftWorldgenRegistries {
	private static final DeferredRegister<Codec<? extends BiomeSource>> BIOME_SOURCES =
			DeferredRegister.create(Registries.BIOME_SOURCE, SololevelingMod.MODID);
	private static final DeferredRegister<Codec<? extends DensityFunction>> DENSITY_FUNCTIONS =
			DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, SololevelingMod.MODID);

	public static final RegistryObject<Codec<? extends BiomeSource>> DIMENSIONAL_RIFT_BIOME_SOURCE =
			BIOME_SOURCES.register("dimensional_rift", () -> DimensionalRiftBiomeSource.CODEC);
	public static final RegistryObject<Codec<? extends DensityFunction>> RIFT_TERRAIN =
			DENSITY_FUNCTIONS.register("rift_terrain", () -> RiftTerrainDensityFunction.CODEC.codec());

	private RiftWorldgenRegistries() {
	}

	public static void register(IEventBus bus) {
		BIOME_SOURCES.register(bus);
		DENSITY_FUNCTIONS.register(bus);
	}
}
