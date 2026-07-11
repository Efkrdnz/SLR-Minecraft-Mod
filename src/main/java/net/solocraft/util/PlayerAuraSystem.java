package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.PlayerAuraMessage;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Server API for aura state that must be visible to every tracking player. */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class PlayerAuraSystem {
	private static final String AURA_ID = "sololeveling_player_aura";
	private static final String AURA_INTENSITY = "sololeveling_player_aura_intensity";

	private PlayerAuraSystem() {
	}

	public static void setContinuous(ServerPlayer player, String auraId, float intensity) {
		CompoundTag data = player.getPersistentData();
		data.putString(AURA_ID, auraId);
		data.putFloat(AURA_INTENSITY, intensity);
		sendTracking(player, new PlayerAuraMessage(player.getId(), auraId, PlayerAuraMessage.SET, 0, intensity));
	}

	public static void clearContinuous(ServerPlayer player) {
		player.getPersistentData().remove(AURA_ID);
		player.getPersistentData().remove(AURA_INTENSITY);
		sendTracking(player, new PlayerAuraMessage(player.getId(), "", PlayerAuraMessage.CLEAR, 0, 1.0F));
	}

	public static void burst(ServerPlayer player, String auraId, int durationTicks, float intensity) {
		sendTracking(player, new PlayerAuraMessage(player.getId(), auraId, PlayerAuraMessage.BURST,
				Math.max(1, durationTicks), intensity));
	}

	private static void sendTracking(ServerPlayer player, PlayerAuraMessage message) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
	}

	private static void sendCurrent(ServerPlayer subject, ServerPlayer receiver) {
		CompoundTag data = subject.getPersistentData();
		if (!data.contains(AURA_ID))
			return;
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> receiver),
				new PlayerAuraMessage(subject.getId(), data.getString(AURA_ID), PlayerAuraMessage.SET, 0,
						data.contains(AURA_INTENSITY) ? data.getFloat(AURA_INTENSITY) : 1.0F));
	}

	@SubscribeEvent
	public static void onStartTracking(PlayerEvent.StartTracking event) {
		if (event.getEntity() instanceof ServerPlayer receiver && event.getTarget() instanceof ServerPlayer subject)
			sendCurrent(subject, receiver);
	}

	@SubscribeEvent
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			sendCurrent(player, player);
	}

	@SubscribeEvent
	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			sendCurrent(player, player);
	}
}
