
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.solocraft.init;

import net.solocraft.SololevelingMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

public class SololevelingModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SololevelingMod.MODID);
	public static final RegistryObject<SoundEvent> SEISMICSLASH = REGISTRY.register("seismicslash", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "seismicslash")));
	public static final RegistryObject<SoundEvent> FLAGDEPLOY = REGISTRY.register("flagdeploy", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "flagdeploy")));
	public static final RegistryObject<SoundEvent> BELLIRNG = REGISTRY.register("bellirng", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "bellirng")));
	public static final RegistryObject<SoundEvent> TELEPUSH = REGISTRY.register("telepush", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "telepush")));
	public static final RegistryObject<SoundEvent> SLASH = REGISTRY.register("slash", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "slash")));
	public static final RegistryObject<SoundEvent> IMPACT1 = REGISTRY.register("impact1", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "impact1")));
	public static final RegistryObject<SoundEvent> DASH = REGISTRY.register("dash", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "dash")));
	public static final RegistryObject<SoundEvent> PANELOPEN = REGISTRY.register("panelopen", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "panelopen")));
	public static final RegistryObject<SoundEvent> PANELCLOSE = REGISTRY.register("panelclose", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "panelclose")));
	public static final RegistryObject<SoundEvent> SWORDCLASH = REGISTRY.register("swordclash", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("sololeveling", "swordclash")));
}
