package net.solocraft.init;

import net.solocraft.SololevelingMod;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

public class SololevelingModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, SololevelingMod.MODID);
	public static final DeferredHolder<SoundEvent, SoundEvent> SEISMICSLASH = REGISTRY.register("seismicslash", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "seismicslash")));
	public static final DeferredHolder<SoundEvent, SoundEvent> FLAGDEPLOY = REGISTRY.register("flagdeploy", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "flagdeploy")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BELLIRNG = REGISTRY.register("bellirng", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "bellirng")));
	public static final DeferredHolder<SoundEvent, SoundEvent> TELEPUSH = REGISTRY.register("telepush", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "telepush")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SLASH = REGISTRY.register("slash", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "slash")));
	public static final DeferredHolder<SoundEvent, SoundEvent> BASIC_SLASH = REGISTRY.register("basic_slash", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "basic_slash")));
	public static final DeferredHolder<SoundEvent, SoundEvent> IMPACT1 = REGISTRY.register("impact1", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "impact1")));
	public static final DeferredHolder<SoundEvent, SoundEvent> DASH = REGISTRY.register("dash", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "dash")));
	public static final DeferredHolder<SoundEvent, SoundEvent> PANELOPEN = REGISTRY.register("panelopen", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "panelopen")));
	public static final DeferredHolder<SoundEvent, SoundEvent> PANELCLOSE = REGISTRY.register("panelclose", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "panelclose")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SYSTEM_NEGATIVE = REGISTRY.register("system_negative", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "system_negative")));
	public static final DeferredHolder<SoundEvent, SoundEvent> SWORDCLASH = REGISTRY.register("swordclash", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "swordclash")));
	public static final DeferredHolder<SoundEvent, SoundEvent> ARISE = REGISTRY.register("arise", () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath("sololeveling", "arise")));
}
