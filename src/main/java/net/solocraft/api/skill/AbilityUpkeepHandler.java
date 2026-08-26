package net.solocraft.api.skill;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import net.solocraft.SololevelingMod;
import net.solocraft.api.HunterMana;
import net.solocraft.api.vessel.VesselState;

/**
 * Charges toggle abilities their upkeep, and ends the ones that go unpaid.
 *
 * <p>The mod runs this rather than the addon, so every contributed toggle drains
 * on the same cadence its own spiritualizations do -- once a second, ending the
 * moment a second cannot be paid for. An addon holding its own timer would drift
 * from that, and a hunter would have no way to predict when a form lapses.
 *
 * <p>Server-side. A client cannot decide it can still afford a transformation.
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class AbilityUpkeepHandler {
	/** Matches the cadence the mod's own upkeeps use. */
	private static final int UPKEEP_INTERVAL_TICKS = 20;

	private AbilityUpkeepHandler() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (player.tickCount % UPKEEP_INTERVAL_TICKS != 0)
			return;

		List<HunterAbility> toggles = HunterAbilityRegistry.toggles();
		if (toggles.isEmpty())
			return;

		for (HunterAbility ability : toggles) {
			if (!VesselState.isFormActive(player, ability.formId()))
				continue;

			// Creative pays nothing, so a toggle there never lapses.
			if (HunterMana.isFree(player))
				continue;

			int upkeep = ability.upkeepPerSecond();
			if (HunterMana.spend(player, upkeep))
				continue;

			HunterAbilityRegistry.deactivate(player, ability.name());
			player.displayClientMessage(Component.literal(
					HunterAbilityRegistry.displayName(ability.name()) + " lapsed: not enough mana.")
					.withStyle(ChatFormatting.RED), true);
		}
	}
}
