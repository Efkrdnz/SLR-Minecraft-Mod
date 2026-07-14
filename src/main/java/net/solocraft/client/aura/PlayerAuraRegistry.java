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
			new PlayerAuraDefinition.FluidProfile(20, 8, 6, 1.0F, 0.52F, 1.15F, 0.78F), false,
			0xFFC542));

	public static final PlayerAuraDefinition LIU_DRAGON_SWORDS = register(new PlayerAuraDefinition(
			"liu_dragon_swords", 0xFFF4B0, 0xD68B08, GOLD_GLOW,
			PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA, 0.76F, 1.46F,
			1.2F, 0, 0, 0,
			new PlayerAuraDefinition.FluidProfile(8, 3, 2, 0.92F, 0.48F, 1.36F, 1.42F), false,
			0xFFD149));

	public static final PlayerAuraDefinition SHADOW_MONARCH_MANIFESTATION = register(new PlayerAuraDefinition(
			"shadow_monarch_manifestation", 0xC568FF, 0x10001F, PURPLE_GLOW,
			PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA, 0.92F, 1.58F,
			0.94F, 0, 0, 0,
			new PlayerAuraDefinition.FluidProfile(0, 0, 0, 1.12F, 0.92F, 1.72F, 1.12F,
					PlayerAuraDefinition.FluidStyle.SHADOW_RIFT), false));

	public static final PlayerAuraDefinition WHITE_FLAME_SPIRITUALIZATION = register(new PlayerAuraDefinition(
			"white_flame_spiritualization", 0xFFFFFF, 0x9DDCFF, BLUE_GLOW,
			PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA, 0.72F, 1.48F,
			1.18F, 0, 0, 0,
			new PlayerAuraDefinition.FluidProfile(22, 9, 6, 0.92F, 0.74F, 1.42F, 1.28F,
					PlayerAuraDefinition.FluidStyle.WHITE_FLAME_HAIR), false, 0xEAFBFF));

	public static final PlayerAuraDefinition WHITE_FLAME_DOPPELGANGER = register(new PlayerAuraDefinition(
			"white_flame_doppelganger", 0xFFFFFF, 0xBCDFFF, BLUE_GLOW,
			PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA, 1.0F, 1.28F,
			1.6F, 1, 9, 0, null, false, 0xEDF9FF));

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
