package net.solocraft.init;

import net.solocraft.SololevelingMod;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.entity.decoration.PaintingVariant;

public class SololevelingModPaintings {
	public static final DeferredRegister<PaintingVariant> REGISTRY = DeferredRegister.create(Registries.PAINTING_VARIANT, SololevelingMod.MODID);
	public static final DeferredHolder<PaintingVariant, PaintingVariant> AHJIN = REGISTRY.register("ahjin", id -> new PaintingVariant(32, 32, id));
	public static final DeferredHolder<PaintingVariant, PaintingVariant> AHJIN_2 = REGISTRY.register("ahjin_2", id -> new PaintingVariant(32, 32, id));
}
