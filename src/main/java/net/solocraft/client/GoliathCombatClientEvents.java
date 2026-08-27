package net.solocraft.client;

import net.solocraft.SololevelingMod;
import net.solocraft.network.GoliathCombatMessage;
import net.solocraft.util.GoliathCombatManager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

@EventBusSubscriber(value = Dist.CLIENT)
public final class GoliathCombatClientEvents {
	private GoliathCombatClientEvents() {
	}

	@SubscribeEvent
	public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
		if (!event.isAttack())
			return;
		Minecraft minecraft = Minecraft.getInstance();
		Player player = minecraft.player;
		if (player == null || minecraft.screen != null || !GoliathCombatManager.isCombatStance(player))
			return;
		event.setCanceled(true);
		event.setSwingHand(false);
		SololevelingMod.PACKET_HANDLER.sendToServer(new GoliathCombatMessage());
	}
}
