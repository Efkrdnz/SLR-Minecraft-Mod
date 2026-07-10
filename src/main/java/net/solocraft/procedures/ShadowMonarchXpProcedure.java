package net.solocraft.procedures;

import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

@Mod.EventBusSubscriber
public class ShadowMonarchXpProcedure {
	private static final String SHADOW_OWNER = "sl_shadow_owner";

	@SubscribeEvent
	public static void onEntityDeath(LivingDeathEvent event) {
		if (event == null || event.getSource() == null)
			return;
		Entity attacker = event.getSource().getEntity();
		if (attacker == null)
			return;
		Player owner = null;
		if (attacker instanceof TamableAnimal tame && tame.getOwner() instanceof Player tameOwner) {
			owner = tameOwner;
		} else if (attacker.getPersistentData().hasUUID(SHADOW_OWNER) && attacker.level() instanceof ServerLevel level) {
			UUID ownerId = attacker.getPersistentData().getUUID(SHADOW_OWNER);
			owner = level.getPlayerByUUID(ownerId);
		}
		if (owner == null)
			return;
		ShadowMonarchManager.grantKillXp(owner, attacker, event.getEntity());
		ShadowMonarchManager.collectManaStoneDropsFromKill(attacker, event.getEntity());
	}
}
