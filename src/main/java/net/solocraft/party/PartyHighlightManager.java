package net.solocraft.party;

import net.solocraft.util.EntityHighlightSystem;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Maintains private party-member outlines without broadcasting entity glow state.
 * Membership and visual preferences are always resolved by the server.
 */
@EventBusSubscriber
public final class PartyHighlightManager {
	private static final int MAX_TARGETS = 7;
	private static final Map<UUID, ViewerState> VIEWERS = new HashMap<>();

	private PartyHighlightManager() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false || !(event.getEntity() instanceof ServerPlayer viewer)
				|| viewer.tickCount % 20 != 0)
			return;
		sync(viewer);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer viewer))
			return;
		VIEWERS.remove(viewer.getUUID());
		sync(viewer);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		removeTargetFromOtherViewers(player);
		clearViewer(player, true);
		sync(player);
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		removeTargetFromOtherViewers(player);
		VIEWERS.remove(player.getUUID());
	}

	@SubscribeEvent
	public static void onServerStopping(ServerStoppingEvent event) {
		clearServer(event.getServer());
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		VIEWERS.clear();
	}

	/**
	 * Applies membership or preference changes immediately. Party actions may call
	 * this after their server-side mutation; the periodic pass remains a fallback.
	 */
	public static void syncNow(ServerPlayer viewer) {
		if (viewer != null && !viewer.hasDisconnected())
			sync(viewer);
	}

	/** Clears this viewer's private party source immediately. */
	public static void clearNow(ServerPlayer viewer) {
		if (viewer != null)
			clearViewer(viewer, true);
	}

	private static void sync(ServerPlayer viewer) {
		if (viewer == null || viewer.hasDisconnected())
			return;
		PartyService.reconcilePlayer(viewer);
		if (!PartyService.glowEnabled(viewer)) {
			clearViewer(viewer, false);
			return;
		}

		int color = PartyService.glowColor(viewer) & 0xFFFFFF;
		Map<UUID, ResourceKey<Level>> desired = desiredTargets(viewer);
		ViewerState state = VIEWERS.computeIfAbsent(viewer.getUUID(), ignored -> new ViewerState());
		boolean colorChanged = state.color != color;

		for (Map.Entry<UUID, ResourceKey<Level>> previous : List.copyOf(state.targets.entrySet())) {
			ResourceKey<Level> currentDimension = desired.get(previous.getKey());
			if (currentDimension == null || !currentDimension.equals(previous.getValue())) {
				EntityHighlightSystem.hide(viewer, previous.getKey(), previous.getValue(),
						EntityHighlightSystem.SOURCE_PARTY_MEMBERS);
			}
		}

		for (Map.Entry<UUID, ResourceKey<Level>> target : desired.entrySet()) {
			ResourceKey<Level> previousDimension = state.targets.get(target.getKey());
			if (colorChanged || previousDimension == null
					|| !previousDimension.equals(target.getValue())) {
				EntityHighlightSystem.show(viewer, target.getKey(), target.getValue(),
						EntityHighlightSystem.SOURCE_PARTY_MEMBERS, color, 0,
						EntityHighlightSystem.PRIORITY_PARTY);
			}
		}

		state.targets.clear();
		state.targets.putAll(desired);
		state.color = color;
		if (state.targets.isEmpty())
			VIEWERS.remove(viewer.getUUID());
	}

	private static Map<UUID, ResourceKey<Level>> desiredTargets(ServerPlayer viewer) {
		ResourceKey<Level> viewerDimension = viewer.level().dimension();
		Map<UUID, ResourceKey<Level>> desired = new LinkedHashMap<>();
		PartyService.onlineMembers(viewer).stream()
				.filter(member -> validTarget(viewer, member, viewerDimension))
				.sorted(Comparator.comparing(member -> member.getUUID().toString()))
				.limit(MAX_TARGETS)
				.forEach(member -> desired.put(member.getUUID(), member.level().dimension()));
		return desired;
	}

	private static boolean validTarget(ServerPlayer viewer, ServerPlayer member,
			ResourceKey<Level> viewerDimension) {
		return member != null && member != viewer && !member.getUUID().equals(viewer.getUUID())
				&& !member.hasDisconnected() && member.isAlive() && !member.isRemoved()
				&& !member.isSpectator() && member.level().dimension().equals(viewerDimension);
	}

	private static void removeTargetFromOtherViewers(ServerPlayer target) {
		MinecraftServer server = target.getServer();
		if (server == null)
			return;
		for (Map.Entry<UUID, ViewerState> entry : List.copyOf(VIEWERS.entrySet())) {
			if (entry.getKey().equals(target.getUUID()))
				continue;
			ResourceKey<Level> dimension = entry.getValue().targets.remove(target.getUUID());
			if (dimension == null)
				continue;
			ServerPlayer viewer = server.getPlayerList().getPlayer(entry.getKey());
			if (viewer != null && !viewer.hasDisconnected()) {
				EntityHighlightSystem.hide(viewer, target.getUUID(), dimension,
						EntityHighlightSystem.SOURCE_PARTY_MEMBERS);
			}
			if (entry.getValue().targets.isEmpty())
				VIEWERS.remove(entry.getKey());
		}
	}

	private static void clearViewer(ServerPlayer viewer, boolean forcePacket) {
		ViewerState removed = VIEWERS.remove(viewer.getUUID());
		if (forcePacket || removed != null)
			EntityHighlightSystem.clearSource(viewer, EntityHighlightSystem.SOURCE_PARTY_MEMBERS);
	}

	private static void clearServer(MinecraftServer server) {
		for (ServerPlayer viewer : server.getPlayerList().getPlayers())
			EntityHighlightSystem.clearSource(viewer, EntityHighlightSystem.SOURCE_PARTY_MEMBERS);
		VIEWERS.clear();
	}

	private static final class ViewerState {
		private final Map<UUID, ResourceKey<Level>> targets = new HashMap<>();
		private int color = -1;
	}
}
