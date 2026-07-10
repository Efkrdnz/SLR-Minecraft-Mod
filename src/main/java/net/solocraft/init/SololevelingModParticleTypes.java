package net.solocraft.init;

import net.solocraft.SololevelingMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;

public class SololevelingModParticleTypes {
	public static final DeferredRegister<ParticleType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, SololevelingMod.MODID);
	public static final RegistryObject<SimpleParticleType> AURA_BLUE = REGISTRY.register("aura_blue", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> AURA_PURPLE = REGISTRY.register("aura_purple", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> AURA_GREEN = REGISTRY.register("aura_green", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> MANA_BLUE = REGISTRY.register("mana_blue", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> MANA_PURPLE = REGISTRY.register("mana_purple", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> MANA_GREEN = REGISTRY.register("mana_green", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> MANA_RED = REGISTRY.register("mana_red", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> PINKSLASH = REGISTRY.register("pinkslash", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> PINKSLASH_2 = REGISTRY.register("pinkslash_2", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> SLASHFURY = REGISTRY.register("slashfury", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> SLASHFURY_2 = REGISTRY.register("slashfury_2", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> SLASHFURY_3 = REGISTRY.register("slashfury_3", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> SLASHFURY_4 = REGISTRY.register("slashfury_4", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> SLASHFURY_5 = REGISTRY.register("slashfury_5", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> SHARD_PARTICLE = REGISTRY.register("shard_particle", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> GREY_SLASH_1 = REGISTRY.register("grey_slash_1", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> GREY_SLASH_2 = REGISTRY.register("grey_slash_2", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> GREY_SLASH_3 = REGISTRY.register("grey_slash_3", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> GOODSLASH_1 = REGISTRY.register("goodslash_1", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> GOOD_SLASH_2 = REGISTRY.register("good_slash_2", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> GLOW_YELLOW = REGISTRY.register("glow_yellow", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> DETECT_EYE = REGISTRY.register("detect_eye", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> DISMANTLE = REGISTRY.register("dismantle", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> CLEAVE = REGISTRY.register("cleave", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> IMPACT_1 = REGISTRY.register("impact_1", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> IMPACT_12 = REGISTRY.register("impact_12", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> IMPACT_13 = REGISTRY.register("impact_13", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> GLOW_AURA_YELLOW = REGISTRY.register("glow_aura_yellow", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> HEALING_PARTICLE = REGISTRY.register("healing_particle", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> PHYSICAL_BUFF_PARTICLE = REGISTRY.register("physical_buff_particle", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> HASTE_BUFF_PARTICLE = REGISTRY.register("haste_buff_particle", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> FIRE_PARTICLE = REGISTRY.register("fire_particle", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> GLOW_AURA_RED = REGISTRY.register("glow_aura_red", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> IMPACT_21 = REGISTRY.register("impact_21", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> IMPACT_22 = REGISTRY.register("impact_22", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> SHADOW_REVIVE = REGISTRY.register("shadow_revive", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> SHAMAN_MAGIC_PARTICLE = REGISTRY.register("shaman_magic_particle", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> WHITE_FLAMES = REGISTRY.register("white_flames", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> FIRE_PARTICLE_2 = REGISTRY.register("fire_particle_2", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> CURSE_SMOKE = REGISTRY.register("curse_smoke", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> RED_DUST_PARTICLE = REGISTRY.register("red_dust_particle", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> GOODSLASH_3 = REGISTRY.register("goodslash_3", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> BLOOD_PARTICLE_LAND = REGISTRY.register("blood_particle_land", () -> new SimpleParticleType(false));
	public static final RegistryObject<SimpleParticleType> BLOOD_PARTICLE = REGISTRY.register("blood_particle", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> GLOW_AURA_PURPLE = REGISTRY.register("glow_aura_purple", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> MAGIC_MISSILES = REGISTRY.register("magic_missiles", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> LIGHTNING_WHITE = REGISTRY.register("lightning_white", () -> new SimpleParticleType(true));
	public static final RegistryObject<SimpleParticleType> LIGHTNING_BLUE = REGISTRY.register("lightning_blue", () -> new SimpleParticleType(true));
}
