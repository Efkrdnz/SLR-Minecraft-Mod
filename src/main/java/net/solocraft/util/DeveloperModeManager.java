package net.solocraft.util;

import net.solocraft.SololevelingMod;
import net.solocraft.network.DeveloperModeStateMessage;

import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Server-authoritative, per-player access to unreleased development previews.
 *
 * <p>The activation phrase is represented only by a one-way digest in packaged
 * code. The matching chat event is cancelled synchronously before any player
 * can receive it, while the state mutation is moved onto the server thread.</p>
 */
@EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DeveloperModeManager {
	private static final String PERSISTED_FLAG =
			"slr_secret_developer_mode";
	private static final byte[] ACTIVATION_DIGEST = HexFormat.of().parseHex(
			"a82fc3109f444ebbefe7290bb53f91d70ce4f9cdd33384a6ffa991ab5667c6b0");

	private DeveloperModeManager() {
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onServerChat(ServerChatEvent event) {
		if (!matchesActivationCode(event.getRawText()))
			return;
		event.setCanceled(true);
		ServerPlayer player = event.getPlayer();
		player.server.execute(() -> toggle(player));
	}

	public static boolean isEnabled(Entity entity) {
		if (entity == null)
			return false;
		if (entity.level().isClientSide())
			return DeveloperModeStateMessage.isClientEnabled();
		if (!(entity instanceof Player player))
			return false;
		return hasPersistedFlag(player);
	}

	public static void toggle(ServerPlayer player) {
		if (player == null)
			return;
		boolean enabled = !hasPersistedFlag(player);
		setPersistedFlag(player, enabled);
		if (!enabled) {
			VesselManager.VesselDefinition definition =
					VesselManager.currentDefinition(player);
			if (definition != null
					&& "sung_il_hwan".equals(definition.identity()))
				SungIlHwanCombatManager.resetPlayerState(player);
			ShadowMonarchManager.dismissLockedPreviewShadows(player);
		}
		DeveloperModeStateMessage.sync(player, enabled);
		JobSkillManager.syncJobSkills(player);
		JobChangeQuestManager.requestSelectionScreen(player);
		// Refresh Brigadier visibility immediately: preview-only commands use a
		// developer-state requirement and would otherwise appear only after relog.
		player.server.getCommands().sendCommands(player);
		player.displayClientMessage(Component.literal(
				"Developer testing mode " + (enabled ? "enabled" : "disabled") + ".")
				.withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.GRAY),
				true);
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (event.getEntity() instanceof ServerPlayer clone
				&& hasPersistedFlag(event.getOriginal()))
			setPersistedFlag(clone, true);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof ServerPlayer player)
			sync(player);
	}

	private static void sync(ServerPlayer player) {
		DeveloperModeStateMessage.sync(player, hasPersistedFlag(player));
		JobSkillManager.syncJobSkills(player);
	}

	private static boolean hasPersistedFlag(Player player) {
		if (player == null)
			return false;
		CompoundTag root = player.getPersistentData();
		return root.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)
				&& root.getCompound(Player.PERSISTED_NBT_TAG)
						.getBoolean(PERSISTED_FLAG);
	}

	private static void setPersistedFlag(Player player, boolean enabled) {
		CompoundTag root = player.getPersistentData();
		CompoundTag persisted = root.getCompound(Player.PERSISTED_NBT_TAG);
		if (enabled)
			persisted.putBoolean(PERSISTED_FLAG, true);
		else
			persisted.remove(PERSISTED_FLAG);
		root.put(Player.PERSISTED_NBT_TAG, persisted);
	}

	private static boolean matchesActivationCode(String rawText) {
		if (rawText == null)
			return false;
		try {
			byte[] actual = MessageDigest.getInstance("SHA-256")
					.digest(rawText.getBytes(StandardCharsets.UTF_8));
			return MessageDigest.isEqual(ACTIVATION_DIGEST, actual);
		} catch (NoSuchAlgorithmException exception) {
			SololevelingMod.LOGGER.error(
					"Unable to validate the developer-mode chat digest",
					exception);
			return false;
		}
	}
}
