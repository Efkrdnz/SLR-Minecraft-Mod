package net.solocraft.util;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/** Keeps derived player vitals and their client representation in agreement. */
public final class PlayerVitalSync {
	private static final double CREATIVE_MANA = 1_000_000.0D;

	private PlayerVitalSync() {
	}

	public static void applyDerivedAttributes(Player player) {
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(variables -> {
			AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
			if (maxHealth != null)
				maxHealth.setBaseValue(20.0D + 0.5D * variables.Vitality);

			AttributeInstance armor = player.getAttribute(Attributes.ARMOR);
			if (armor != null)
				armor.setBaseValue(0.05D * variables.Vitality);
		});
	}

	public static void restoreAfterRespawn(ServerPlayer player) {
		applyDerivedAttributes(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(variables -> {
			variables.Mana = player.isCreative() ? CREATIVE_MANA : 1000.0D + 100.0D * variables.Intelligence;
			variables.MP = variables.Mana;
			variables.Fatigue = 0.0D;
			variables.syncPlayerVariables(player);
		});
		player.setHealth(player.getMaxHealth());
		syncClientState(player);
	}

	public static void refreshClientState(ServerPlayer player) {
		applyDerivedAttributes(player);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(variables -> variables.syncPlayerVariables(player));
		syncClientState(player);
	}

	private static void syncClientState(ServerPlayer player) {
		player.connection.send(new ClientboundUpdateAttributesPacket(player.getId(), player.getAttributes().getSyncableAttributes()));
		player.connection.send(new ClientboundSetHealthPacket(player.getHealth(), player.getFoodData().getFoodLevel(), player.getFoodData().getSaturationLevel()));
	}
}
