package net.solocraft.util;

import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Applies each shadow type's chosen outline colour to that owner's summoned
 * shadows, so a mixed field of shadows can be told apart at a glance.
 *
 * <p>Built on the same private-outline path the party system uses: outlines are
 * sent only to the owning player and never broadcast, so one player's colour
 * choices are invisible to everyone else.
 */
@EventBusSubscriber
public final class ShadowGlowManager {
	private static final Map<UUID, ViewerState> VIEWERS = new HashMap<>();

	private ShadowGlowManager() {
	}

	@SubscribeEvent
	public static void onPlayerTick(PlayerTickEvent.Post event) {
		if (false
				|| !(event.getEntity() instanceof ServerPlayer owner)
				|| owner.tickCount % 20 != 0)
			return;
		sync(owner);
	}

	@SubscribeEvent
	public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof ServerPlayer owner) {
			VIEWERS.remove(owner.getUUID());
			sync(owner);
		}
	}

	@SubscribeEvent
	public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer owner)
			VIEWERS.remove(owner.getUUID());
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(
			PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer owner) {
			clear(owner);
			sync(owner);
		}
	}

	@SubscribeEvent
	public static void onServerStopped(ServerStoppedEvent event) {
		VIEWERS.clear();
	}

	/** Re-applies colours immediately after the owner edits one. */
	public static void syncNow(ServerPlayer owner) {
		if (owner != null && !owner.hasDisconnected())
			sync(owner);
	}

	private static void sync(ServerPlayer owner) {
		if (owner == null || owner.hasDisconnected())
			return;
		ResourceKey<Level> dimension = owner.level().dimension();
		Map<UUID, Integer> desired = new LinkedHashMap<>();
		for (Map.Entry<UUID, String> summoned
				: ShadowMonarchManager.summonedShadowTypes(owner).entrySet()) {
			int color = ShadowMonarchManager.glowColor(owner, summoned.getValue());
			if (color != ShadowMonarchManager.NO_GLOW)
				desired.put(summoned.getKey(), color & 0xFFFFFF);
		}

		ViewerState state = VIEWERS.computeIfAbsent(owner.getUUID(),
				ignored -> new ViewerState());
		for (Map.Entry<UUID, Applied> previous : List.copyOf(state.applied.entrySet())) {
			Integer wanted = desired.get(previous.getKey());
			if (wanted == null || !previous.getValue().dimension.equals(dimension))
				EntityHighlightSystem.hide(owner, previous.getKey(),
						previous.getValue().dimension,
						EntityHighlightSystem.SOURCE_SHADOW_GLOW);
		}
		for (Map.Entry<UUID, Integer> target : desired.entrySet()) {
			Applied previous = state.applied.get(target.getKey());
			if (previous == null || previous.color != target.getValue()
					|| !previous.dimension.equals(dimension))
				EntityHighlightSystem.show(owner, target.getKey(), dimension,
						EntityHighlightSystem.SOURCE_SHADOW_GLOW,
						target.getValue(), 0,
						EntityHighlightSystem.PRIORITY_SHADOW_GLOW);
		}

		state.applied.clear();
		for (Map.Entry<UUID, Integer> target : desired.entrySet())
			state.applied.put(target.getKey(),
					new Applied(target.getValue(), dimension));
		if (state.applied.isEmpty())
			VIEWERS.remove(owner.getUUID());
	}

	private static void clear(ServerPlayer owner) {
		VIEWERS.remove(owner.getUUID());
		EntityHighlightSystem.clearSource(owner,
				EntityHighlightSystem.SOURCE_SHADOW_GLOW);
	}

	private record Applied(int color, ResourceKey<Level> dimension) {
	}

	private static final class ViewerState {
		private final Map<UUID, Applied> applied = new HashMap<>();
	}
}
