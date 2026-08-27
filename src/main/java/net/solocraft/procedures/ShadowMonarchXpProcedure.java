package net.solocraft.procedures;

import net.solocraft.util.ShadowExperienceManager;
import net.solocraft.util.ShadowMonarchManager;

import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.entity.Entity;

/**
 * Server event bridge for contribution-based shadow experience.
 */
@EventBusSubscriber
public class ShadowMonarchXpProcedure {
	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
	public static void onEntityDamaged(LivingDamageEvent.Post event) {
		ShadowExperienceManager.recordDamage(event);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event == null || event.getEntity().level().isClientSide())
			return;
		ShadowExperienceManager.awardContributions(event.getEntity());

		// Mana-stone pickup remains a finishing-shadow perk; XP itself is based
		// on every shadow's damage and does not require the final blow.
		Entity finishingShadow = ShadowExperienceManager.resolveShadow(
				event.getSource().getEntity());
		if (finishingShadow == null)
			finishingShadow = ShadowExperienceManager.resolveShadow(
					event.getSource().getDirectEntity());
		if (finishingShadow != null)
			ShadowMonarchManager.collectManaStoneDropsFromKill(
					finishingShadow, event.getEntity());
	}
}
