package net.solocraft.client;

import net.solocraft.SololevelingMod;
import net.solocraft.network.LiuAttackMessage;
import net.solocraft.network.LiuChargeMessage;
import net.solocraft.util.LiuZhigangCombatManager;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
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
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (event.phase != TickEvent.Phase.END || !charging)
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
