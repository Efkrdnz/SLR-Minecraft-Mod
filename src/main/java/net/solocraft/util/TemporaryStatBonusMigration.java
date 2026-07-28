package net.solocraft.util;

import net.solocraft.init.SololevelingModMobEffects;
import net.solocraft.network.SololevelingModVariables;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.server.level.ServerPlayer;

/**
 * One-time compatibility cleanup for saves created before temporary effect
 * bonuses were derived. The old implementation wrote +30 into permanent stats
 * and expected effect removal to subtract it. A server stopped while either
 * effect was active therefore loads with that addition still baked in.
 */
@Mod.EventBusSubscriber
public final class TemporaryStatBonusMigration {
	static final String MIGRATION_RECEIPT = "slr_temporary_stat_bonus_model_v1";

	private TemporaryStatBonusMigration() {
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			migrate(player);
	}

	@SubscribeEvent
	public static void onClone(PlayerEvent.Clone event) {
		if (event.getOriginal().getPersistentData().getBoolean(MIGRATION_RECEIPT))
			event.getEntity().getPersistentData().putBoolean(MIGRATION_RECEIPT, true);
	}

	public static void migrate(ServerPlayer player) {
		if (player == null || player.getPersistentData().getBoolean(MIGRATION_RECEIPT))
			return;

		boolean legacyHaste = player.hasEffect(SololevelingModMobEffects.HASTE_BUFF.get());
		boolean legacyPhysical = player.hasEffect(SololevelingModMobEffects.PHYSICAL_BUFF.get());
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(variables -> {
			if (legacyHaste)
				variables.Speed = Math.max(0.0D,
						variables.Speed - TemporaryStatBonusManager.HASTE_BUFF_AGILITY_BONUS);
			if (legacyPhysical)
				variables.Strength = Math.max(0.0D,
						variables.Strength - TemporaryStatBonusManager.PHYSICAL_BUFF_STRENGTH_BONUS);
			if (legacyHaste || legacyPhysical)
				variables.syncPlayerVariables(player);
			player.getPersistentData().putBoolean(MIGRATION_RECEIPT, true);
		});
	}
}
