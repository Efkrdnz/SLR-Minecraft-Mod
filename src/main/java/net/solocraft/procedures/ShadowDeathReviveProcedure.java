package net.solocraft.procedures;

import net.solocraft.entity.BeruShadowEntity;
import net.solocraft.entity.GoblinArcherShadowEntity;
import net.solocraft.entity.GoblinClubShadowEntity;
import net.solocraft.entity.GoblinMageShadowEntity;
import net.solocraft.entity.IgrisShadowEntity;
import net.solocraft.entity.KamishShadowEntity;
import net.solocraft.entity.ShadowGreenOrcEntity;
import net.solocraft.entity.ShadowHighOrcEntity;
import net.solocraft.entity.ShadowPolarBearEntity;
import net.solocraft.entity.ShadowSold1Entity;
import net.solocraft.entity.SteelFangWolfShadowEntity;
import net.solocraft.entity.TuskShadowEntity;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.CooldownManager;

import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Pays the Shadow Monarch's mana to prevent a summoned shadow's death.
 *
 * <p>A failed revival only allows the summoned entity to die. Ownership,
 * storage usage and roster progress are intentionally untouched here; the
 * authoritative roster death handler releases that summon's exact slot while
 * retaining its level, XP and rank for a later summon.</p>
 */
@EventBusSubscriber
public final class ShadowDeathReviveProcedure {
	private ShadowDeathReviveProcedure() {
	}

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event == null || event.getEntity() == null)
			return;
		tryRevive(event, event.getEntity());
	}

	public static void execute(Entity entity) {
		tryRevive(null, entity);
	}

	private static void tryRevive(LivingDeathEvent event, Entity entity) {
		if (entity == null || entity.level().isClientSide()
				|| !(entity instanceof TamableAnimal tame) || !tame.isTame()
				|| !(tame.getOwner() instanceof Player owner))
			return;
		int manaCost = revivalManaCost(entity);
		if (manaCost <= 0)
			return;
		SololevelingModVariables.PlayerVariables variables = owner
				.getCapability(
						SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY,
						null)
				.orElse(null);
		if (variables == null || variables.MP < manaCost)
			return;

		if (event != null)
			event.setCanceled(true);
		variables.MP = Math.max(0.0D, variables.MP - manaCost);
		variables.syncPlayerVariables(owner);
		if (entity instanceof LivingEntity living)
			living.setHealth(living.getMaxHealth());
		CooldownManager.set(entity, "mana_refresh",
				entity instanceof IgrisShadowEntity ? 200 : 40);
	}

	private static int revivalManaCost(Entity entity) {
		if (entity instanceof SteelFangWolfShadowEntity)
			return 100;
		if (entity instanceof ShadowSold1Entity
				|| entity instanceof ShadowPolarBearEntity)
			return 200;
		if (entity instanceof ShadowGreenOrcEntity)
			return 300;
		if (entity instanceof GoblinClubShadowEntity
				|| entity instanceof GoblinArcherShadowEntity
				|| entity instanceof GoblinMageShadowEntity
				|| entity instanceof ShadowHighOrcEntity)
			return 500;
		if (entity instanceof IgrisShadowEntity)
			return 5_000;
		if (entity instanceof TuskShadowEntity)
			return 6_000;
		if (entity instanceof BeruShadowEntity)
			return 10_000;
		if (entity instanceof KamishShadowEntity)
			return 20_000;
		return 0;
	}
}
