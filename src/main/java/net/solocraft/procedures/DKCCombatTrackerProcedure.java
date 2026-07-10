package net.solocraft.procedures;

import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;

@Mod.EventBusSubscriber
public class DKCCombatTrackerProcedure {
	private static final String LAST_COMBAT_TICK = "solocraft_dkc_last_combat_tick";
	private static final long OUT_OF_COMBAT_TICKS = 20L * 20L;

	@SubscribeEvent
	public static void onEntityHurt(LivingHurtEvent event) {
		if (event == null || event.getEntity() == null)
			return;
		if (event.getEntity() instanceof ServerPlayer player) {
			markInCombat(player);
		}
		Entity attacker = event.getSource().getEntity();
		if (attacker instanceof ServerPlayer player) {
			markInCombat(player);
		} else if (attacker instanceof TamableAnimal tamable && tamable.getOwner() instanceof ServerPlayer owner) {
			markInCombat(owner);
		}
	}

	public static void markInCombat(ServerPlayer player) {
		player.getPersistentData().putLong(LAST_COMBAT_TICK, player.serverLevel().getGameTime());
	}

	public static boolean canEnterCastle(ServerPlayer player) {
		long lastCombatTick = player.getPersistentData().getLong(LAST_COMBAT_TICK);
		return lastCombatTick <= 0 || player.serverLevel().getGameTime() - lastCombatTick >= OUT_OF_COMBAT_TICKS;
	}

	public static void sendCombatBlockedMessage(ServerPlayer player) {
		long lastCombatTick = player.getPersistentData().getLong(LAST_COMBAT_TICK);
		long remainingTicks = Math.max(0L, OUT_OF_COMBAT_TICKS - (player.serverLevel().getGameTime() - lastCombatTick));
		long remainingSeconds = Math.max(1L, (remainingTicks + 19L) / 20L);
		player.displayClientMessage(Component.literal("\u00A74The castle rejects cowards in flight. \u00A7cWait " + remainingSeconds + "s out of combat."), true);
	}
}
