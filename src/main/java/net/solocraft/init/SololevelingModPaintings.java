
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.solocraft.init;

import net.solocraft.SololevelingMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.entity.decoration.PaintingVariant;

public class SololevelingModPaintings {
	public static final DeferredRegister<PaintingVariant> REGISTRY = DeferredRegister.create(ForgeRegistries.PAINTING_VARIANTS, SololevelingMod.MODID);
	public static final RegistryObject<PaintingVariant> AHJIN = REGISTRY.register("ahjin", () -> new PaintingVariant(32, 32));
	public static final RegistryObject<PaintingVariant> AHJIN_2 = REGISTRY.register("ahjin_2", () -> new PaintingVariant(32, 32));
}
