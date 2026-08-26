package net.solocraft.client;

import net.solocraft.SololevelingMod;
import net.solocraft.network.LiuAttackMessage;
import net.solocraft.network.LiuChargeMessage;
import net.solocraft.util.LiuZhigangCombatManager;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

@EventBusSubscriber(value = Dist.CLIENT)
public final class LiuCombatClientEvents {
	private static boolean charging;
	private static long chargeStartedAt;
	private static int vanillaCombo;

	private LiuCombatClientEvents() {
	}

	@SubscribeEvent
	public static void onInteractionInput(InputEvent.InteractionKeyMappingTriggered event) {
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		if (player == null || minecraft.screen != null || !LiuZhigangCombatManager.isCombatStance(player))
			return;

		if (event.isAttack()) {
			if (!ModList.get().isLoaded("bettercombat"))
				sendVanillaEnhancedAttack(player);
			return;
		}

		if (event.isUseItem() && (LiuZhigangCombatManager.isMeleeWeapon(player.getMainHandItem())
				|| LiuZhigangCombatManager.isMeleeWeapon(player.getOffhandItem()))) {
			event.setCanceled(true);
			event.setSwingHand(false);
			if (!charging) {
				charging = true;
				chargeStartedAt = player.level().getGameTime();
				SololevelingMod.PACKET_HANDLER.sendToServer(new LiuChargeMessage(LiuChargeMessage.BEGIN));
			}
		}
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (false || !charging)
			return;
		Minecraft minecraft = Minecraft.getInstance();
		LocalPlayer player = minecraft.player;
		boolean valid = player != null && minecraft.screen == null
				&& LiuZhigangCombatManager.isCombatStance(player)
				&& (LiuZhigangCombatManager.isMeleeWeapon(player.getMainHandItem())
						|| LiuZhigangCombatManager.isMeleeWeapon(player.getOffhandItem()));
		if (valid && minecraft.options.keyUse.isDown())
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(new LiuChargeMessage(valid
				? LiuChargeMessage.RELEASE : LiuChargeMessage.CANCEL));
		charging = false;
	}

	public static void sendEnhancedAttack(boolean offhand, int comboIndex) {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.player == null || minecraft.screen != null
				|| !LiuZhigangCombatManager.isCombatStance(minecraft.player))
			return;
		SololevelingMod.PACKET_HANDLER.sendToServer(new LiuAttackMessage(offhand, comboIndex));
	}

	private static void sendVanillaEnhancedAttack(LocalPlayer player) {
		boolean mainhand = LiuZhigangCombatManager.isMeleeWeapon(player.getMainHandItem());
		boolean offhand = LiuZhigangCombatManager.isMeleeWeapon(player.getOffhandItem());
		if (!mainhand && !offhand)
			return;
		if (mainhand && offhand) {
			int step = Math.floorMod(vanillaCombo++, 3);
			sendEnhancedAttack(step == 1, step == 2 ? 3 : step);
			return;
		}
		sendEnhancedAttack(offhand, vanillaCombo++);
	}

	public static boolean isCharging() {
		return charging;
	}

	public static long getChargeTicks(float partialTick) {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level == null ? 0L
				: Math.max(0L, minecraft.level.getGameTime() - chargeStartedAt);
	}
}
