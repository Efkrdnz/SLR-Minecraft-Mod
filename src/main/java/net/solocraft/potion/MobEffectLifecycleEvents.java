package net.solocraft.potion;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.procedures.DomainBoostEffectExpiresProcedure;
import net.solocraft.procedures.FreezeEffectExpiresProcedure;
import net.solocraft.procedures.HasteBuffEffectExpiresProcedure;
import net.solocraft.procedures.PhysicalBuffEffectExpiresProcedure;
import net.solocraft.procedures.WillPowerEffectExpiresProcedure;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.LivingEntity;

/** Preserves legacy per-entity cleanup callbacks removed from MobEffect in 1.21. */
@EventBusSubscriber
public final class MobEffectLifecycleEvents {
	private MobEffectLifecycleEvents() {
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onEffectRemoved(MobEffectEvent.Remove event) {
		if (!event.isCanceled())
			scheduleCleanup(event.getEntity(), event.getEffect());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void onEffectExpired(MobEffectEvent.Expired event) {
		if (!event.isCanceled() && event.getEffectInstance() != null)
			scheduleCleanup(event.getEntity(), event.getEffectInstance().getEffect());
	}

	private static void scheduleCleanup(LivingEntity entity, Holder<MobEffect> effect) {
		if (entity.level().isClientSide() || entity.getServer() == null)
			return;
		entity.getServer().execute(() -> {
			if (!entity.isRemoved() && !entity.hasEffect(effect))
				cleanup(entity, effect);
		});
	}

	private static void cleanup(LivingEntity entity, Holder<MobEffect> effect) {
		if (effect.is(SololevelingModMobEffects.DOMAIN_BOOST))
			DomainBoostEffectExpiresProcedure.execute(entity);
		else if (effect.is(SololevelingModMobEffects.FREEZE))
			FreezeEffectExpiresProcedure.execute(entity.level(), entity.getX(), entity.getY(), entity.getZ());
		else if (effect.is(SololevelingModMobEffects.HASTE_BUFF))
			HasteBuffEffectExpiresProcedure.execute(entity);
		else if (effect.is(SololevelingModMobEffects.PHYSICAL_BUFF))
			PhysicalBuffEffectExpiresProcedure.execute(entity);
		else if (effect.is(SololevelingModMobEffects.WILL_POWER))
			WillPowerEffectExpiresProcedure.execute(entity.level(), entity);
		else {
			// Doom pays out when its timer completes. Routed through the shared
			// expiry dispatch so a curse consumed early by Culling, which is a
			// removal rather than an expiry, correctly forfeits the detonation.
			net.solocraft.util.CurseType curse =
					net.solocraft.util.CurseState.curseFor(effect);
			if (curse != null)
				net.solocraft.util.CurseEffectHooks.onExpired(entity, curse);
		}
	}
}
