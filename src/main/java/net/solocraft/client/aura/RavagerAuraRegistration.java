package net.solocraft.client.aura;

import net.solocraft.SololevelingMod;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.resources.ResourceLocation;

/**
 * Beast-form auras for the Ravager transformations.
 *
 * <p>These are deliberately scaled-down siblings of the vessel spiritualization
 * auras rather than a separate visual system. A Ravager is a Hunter using a
 * technique, not a Monarch manifesting — so the same fluid aura reads at
 * roughly two thirds the radius for the partial form and four fifths for the
 * full form, in amber instead of the vessel palettes.</p>
 */
@EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = EventBusSubscriber.Bus.MOD,
		value = Dist.CLIENT
)
public final class RavagerAuraRegistration {
	public static final String PARTIAL_ID = "ravager_partial";
	public static final String FULL_ID = "ravager_full";
	/** Juggernaut Gigantification, registered here to share the aura setup. */
	public static final String JUGGERNAUT_ID = "juggernaut_mass";

	private static final ResourceLocation AMBER_GLOW = ResourceLocation.fromNamespaceAndPath(
			SololevelingMod.MODID, "textures/particle/auraglowred.png");

	private RavagerAuraRegistration() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			if (PlayerAuraRegistry.get(PARTIAL_ID) == null)
				PlayerAuraRegistry.register(new PlayerAuraDefinition(
						PARTIAL_ID,
						0xFFD9A0,
						0xFF8A2B,
						AMBER_GLOW,
						PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA,
						0.46F,
						1.02F,
						0.92F,
						0,
						0,
						0,
						new PlayerAuraDefinition.FluidProfile(
								11,
								5,
								3,
								0.68F,
								0.46F,
								1.05F,
								0.98F,
								PlayerAuraDefinition.FluidStyle.LIQUID_FLAME),
						false,
						0x3A2418));

			if (PlayerAuraRegistry.get(FULL_ID) == null)
				PlayerAuraRegistry.register(new PlayerAuraDefinition(
						FULL_ID,
						0xFFE7B8,
						0xFF6A12,
						AMBER_GLOW,
						PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA,
						0.60F,
						1.26F,
						1.02F,
						0,
						0,
						0,
						new PlayerAuraDefinition.FluidProfile(
								15,
								7,
								4,
								0.80F,
								0.60F,
								1.20F,
								1.10F,
								PlayerAuraDefinition.FluidStyle.LIQUID_FLAME),
						true,
						0x2E1A10));

			// Gigantification: bronze and stone rather than beast amber, and
			// wider but shorter than the Ravager forms so it reads as mass
			// rather than ferocity.
			if (PlayerAuraRegistry.get(JUGGERNAUT_ID) == null)
				PlayerAuraRegistry.register(new PlayerAuraDefinition(
						JUGGERNAUT_ID,
						0xE8C89A,
						0xB07338,
						AMBER_GLOW,
						PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA,
						0.68F,
						0.92F,
						0.72F,
						0,
						0,
						0,
						new PlayerAuraDefinition.FluidProfile(
								13,
								6,
								3,
								0.88F,
								0.52F,
								0.85F,
								0.80F,
								PlayerAuraDefinition.FluidStyle.LIQUID_FLAME),
						true,
						0x2A2018));
		});
	}
}
