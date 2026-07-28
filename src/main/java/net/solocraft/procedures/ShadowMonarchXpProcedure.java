package net.solocraft.procedures;

import net.solocraft.util.ShadowExperienceManager;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.Entity;

/**
 * Server event bridge for contribution-based shadow experience.
 */
@Mod.EventBusSubscriber
public class ShadowMonarchXpProcedure {
	@SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
	public static void onEntityDamaged(LivingDamageEvent event) {
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
