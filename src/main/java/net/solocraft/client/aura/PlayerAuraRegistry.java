package net.solocraft.client.aura;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registry for reusable aura recipes. Add new spiritualization presets here. */
public final class PlayerAuraRegistry {
	private static final Map<String, PlayerAuraDefinition> DEFINITIONS = new LinkedHashMap<>();
	private static final ResourceLocation GOLD_GLOW = new ResourceLocation("sololeveling", "textures/particle/glow_yellow.png");
	private static final ResourceLocation BLUE_GLOW = new ResourceLocation("sololeveling", "textures/particle/mana_blue.png");
	private static final ResourceLocation PURPLE_GLOW = new ResourceLocation("sololeveling", "textures/particle/aura_glow_purple.png");

	public static final PlayerAuraDefinition GOLIATH = register(new PlayerAuraDefinition(
			"goliath_manifestation", 0xFFF6C7, 0xC97412, GOLD_GLOW,
			PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA, 0.82F, 1.35F,
			0.72F, 0, 0, 0,
			new PlayerAuraDefinition.FluidProfile(20, 8, 6, 1.0F, 0.52F, 1.15F, 0.78F), false));

	public static final PlayerAuraDefinition RULER_BLUE = register(new PlayerAuraDefinition(
			"ruler_blue", 0xE6FAFF, 0x168DFF, BLUE_GLOW,
			PlayerAuraDefinition.Facing.CAMERA, 0.76F, 1.12F,
			1.0F, 2, 7, 5, null, true));

	public static final PlayerAuraDefinition SHADOW_FLOW = register(new PlayerAuraDefinition(
			"shadow_flow", 0xD9A7FF, 0x421080, PURPLE_GLOW,
			PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA, 0.78F, 1.22F,
			0.68F, 2, 10, 4, null, true));

	private PlayerAuraRegistry() {
	}

	public static synchronized PlayerAuraDefinition register(PlayerAuraDefinition definition) {
		if (DEFINITIONS.putIfAbsent(definition.id(), definition) != null)
			throw new IllegalArgumentException("Duplicate player aura id: " + definition.id());
		return definition;
	}

	public static PlayerAuraDefinition get(String id) {
		return DEFINITIONS.get(id);
	}

	public static Collection<PlayerAuraDefinition> values() {
		return Collections.unmodifiableCollection(DEFINITIONS.values());
	}
}
