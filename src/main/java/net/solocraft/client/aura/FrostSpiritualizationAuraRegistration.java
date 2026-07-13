package net.solocraft.client.aura;

import net.solocraft.SololevelingMod;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraft.resources.ResourceLocation;

/** Registers the temporary frost spiritualization aura on physical clients. */
@Mod.EventBusSubscriber(
		modid = SololevelingMod.MODID,
		bus = Mod.EventBusSubscriber.Bus.MOD,
		value = Dist.CLIENT
)
public final class FrostSpiritualizationAuraRegistration {
	public static final String ID = "frost_spiritualization";

	private static final ResourceLocation BLUE_GLOW = new ResourceLocation(
			SololevelingMod.MODID,
			"textures/particle/mana_blue.png"
	);

	private FrostSpiritualizationAuraRegistration() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			if (PlayerAuraRegistry.get(ID) != null) {
				return;
			}

			PlayerAuraRegistry.register(new PlayerAuraDefinition(
					ID,
					0xF2FDFF,
					0x55CFFF,
					BLUE_GLOW,
					PlayerAuraDefinition.Facing.HORIZONTAL_CAMERA,
					0.72F,
					1.48F,
					1.08F,
					0,
					0,
					0,
					new PlayerAuraDefinition.FluidProfile(
							18,
							8,
							5,
							0.88F,
							0.68F,
							1.25F,
							1.15F,
							PlayerAuraDefinition.FluidStyle.WHITE_FLAME_HAIR
					),
					false,
					0xC8F4FF
			));
		});
	}
}
