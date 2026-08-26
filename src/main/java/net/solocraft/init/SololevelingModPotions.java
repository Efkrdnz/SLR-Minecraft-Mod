package net.solocraft.init;

import net.solocraft.SololevelingMod;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;

public class SololevelingModPotions {
	public static final DeferredRegister<Potion> REGISTRY = DeferredRegister.create(BuiltInRegistries.POTION, SololevelingMod.MODID);
	public static final DeferredHolder<Potion, Potion> KASAKAS_VENOM = REGISTRY.register("kasakas_venom",
			() -> new Potion(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 3600, 0, false, false), new MobEffectInstance(MobEffects.POISON, 3600, 1, false, false)));
}
