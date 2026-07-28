package net.solocraft.dkc;

import net.solocraft.SololevelingMod;
import net.solocraft.network.DkcQuestProgressMessage;
import net.solocraft.network.SololevelingModVariables;
import net.solocraft.util.DkcQuestManager;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Produces the compact server-authoritative objective shown while Tab is held.
 * It samples at 2 Hz only for a visible DKC tracker and sends only on change.
 */
@Mod.EventBusSubscriber(modid = SololevelingMod.MODID)
public final class DkcQuestProgressTracker {
	private static final int SYNC_INTERVAL_TICKS = 10;
	private static final int MIN_PRESS_INTERVAL_TICKS = 1;
	private static final Map<MinecraftServer, Map<UUID, Snapshot>> LAST_SENT = new WeakHashMap<>();
	private static final Map<MinecraftServer, Set<UUID>> HELD = new WeakHashMap<>();
	private static final Map<MinecraftServer, Map<UUID, Long>> PRESS_ALLOWED_AT = new WeakHashMap<>();

	private DkcQuestProgressTracker() {
	}

	/** Clears the server hold/cache and closes the client's DKC tracker. */
	public static void resetPlayerState(ServerPlayer player) {
		if (player == null || player.server == null)
			return;
		unmarkHeld(player.server, player.getUUID());
		forget(player.server, player.getUUID());
		forgetPressLimit(player.server, player.getUUID());
		send(player, Snapshot.INACTIVE);
	}

	public static void tick(ServerPlayer player) {
		if (player == null || player.server == null
				|| player.tickCount % SYNC_INTERVAL_TICKS != Math.floorMod(player.getId(), SYNC_INTERVAL_TICKS)
				|| !isHeld(player))
			return;
		syncIfChanged(player);
	}

	/** Rejects duplicate/same-tick forged press edges before snapshot work. */
	public static synchronized boolean acceptPress(ServerPlayer player) {
		if (player == null || player.server == null || isHeld(player))
			return false;
		long now = player.serverLevel().getGameTime();
		Map<UUID, Long> serverLimits = PRESS_ALLOWED_AT.computeIfAbsent(player.server, ignored -> new HashMap<>());
		if (now < serverLimits.getOrDefault(player.getUUID(), 0L))
			return false;
		serverLimits.put(player.getUUID(), now + MIN_PRESS_INTERVAL_TICKS);
		return true;
	}

	/** Starts a transient hold session and responds to the server-side Tab press. */
	public static void beginTracking(ServerPlayer player) {
		if (player == null || player.server == null)
			return;
		if (markHeld(player))
			syncNow(player);
		else
			syncIfChanged(player);
	}

	private static void syncNow(ServerPlayer player) {
		Snapshot snapshot = snapshot(player);
		remember(player, snapshot);
		send(player, snapshot);
	}

	/** Releases the tiny equality cache when Tab is no longer held. */
	public static void stopTracking(ServerPlayer player) {
		if (player == null || player.server == null)
			return;
		unmarkHeld(player.server, player.getUUID());
		forget(player.server, player.getUUID());
	}

	private static void syncIfChanged(ServerPlayer player) {
		Snapshot snapshot = snapshot(player);
		if (!rememberIfChanged(player, snapshot))
			return;
		send(player, snapshot);
	}

	private static Snapshot snapshot(ServerPlayer player) {
		int floor = DkcSpatialLayout.floor(player);
		if (floor <= 0 || !player.getPersistentData().getBoolean(DkcSpatialLayout.ACTIVE_RUN_TAG))
			return Snapshot.INACTIVE;

		SololevelingModVariables.PlayerVariables vars = variables(player);
		int cleared = Mth.clamp((int) Math.floor(vars.dkc_cleared), 0, DkcFloorRegistry.LAST_FLOOR);
		String floorName = DkcFloorRegistry.name(floor);
		CompoundTag data = player.getPersistentData();
		if (!DkcRunSavedData.get(player.server).isGenerated(player, floor)) {
			return new Snapshot(true, floor, cleared, floorName, "ascent",
					"This floor is taking shape.",
					"Your progress is secured. The encounter will begin when construction finishes.", 0, 0);
		}

		if (floor == DkcRadiruManager.FLOOR && cleared >= DkcFloorRegistry.LAST_FLOOR
				&& !vars.radiru_slaughtered && DkcQuestManager.hasRadiruCastleAccess(player)) {
			return new Snapshot(true, floor, cleared, floorName, "sanctuary",
					"House Radiru remains under your protection.",
					"The training grounds are open. The ascent tower is permanently sealed.", 0, 0);
		}

		if (cleared >= floor)
			return clearedFloorSnapshot(player, vars, floor, cleared, floorName);
		if (floor == 1)
			return new Snapshot(true, floor, cleared, floorName, "boss",
					"Defeat Cerberus.", "The gatekeeper blocks the first ascension.",
					data.getBoolean("dkc_floor_1_boss_defeated") ? 1 : 0, 1);
		if (floor == DkcFloorRegistry.LAST_FLOOR)
			return throneSnapshot(data, floor, cleared, floorName);

		return waveSnapshot(data, floor, cleared, floorName);
	}

	private static Snapshot clearedFloorSnapshot(ServerPlayer player,
			SololevelingModVariables.PlayerVariables vars, int floor, int cleared, String floorName) {
		if (floor == DkcFloorRegistry.LAST_FLOOR) {
			String detail = !vars.radiru_slaughtered && DkcQuestManager.hasRadiruCastleAccess(player)
					? "Use the return shrine. Radiru Castle is now available from System Quests."
					: "Use the throne return shrine to leave the conquered castle.";
			return new Snapshot(true, floor, cleared, floorName, "conquered",
					"The Demon King's Castle has been conquered.", detail, 0, 0);
		}

		DkcRunSavedData runs = DkcRunSavedData.get(player.server);
		boolean armed = runs.isTransitionArmed(player, floor);
		boolean generated = runs.isGenerated(player, floor + 1);
		if (armed && generated) {
			return new Snapshot(true, floor, cleared, floorName, "ascent",
					"The tower path is open.", "Enter the tower to ascend to Floor " + (floor + 1) + ".", 0, 0);
		}
		if (armed) {
			return new Snapshot(true, floor, cleared, floorName, "ascent",
					"Floor " + (floor + 1) + " is taking shape.",
					"Your permit claim is secured. The tower will open when construction finishes.", 0, 0);
		}

		String objective = "Floor objective complete.";
		if (floor == DkcRadiruManager.FLOOR && vars.radiru_pact && !vars.radiru_slaughtered)
			objective = "Esil's Entry Permit has been granted.";
		else if (floor == DkcRadiruManager.FLOOR && vars.radiru_slaughtered)
			objective = "House Radiru has fallen.";
		return new Snapshot(true, floor, cleared, floorName, "permit", objective,
				"Claim or present an Entry Permit at the crimson tower pedestal.", 0, 0);
	}

	private static Snapshot waveSnapshot(CompoundTag data, int floor, int cleared, String floorName) {
		String prefix = "dkc_floor_" + floor;
		int required = DkcFloorRegistry.requiredKills(floor);
		int kills = Mth.clamp((int) Math.floor(data.getDouble(prefix + "_killed")), 0, required);
		boolean complete = data.getBoolean(prefix + "_complete");

		if (floor == 10 && complete) {
			return new Snapshot(true, floor, cleared, floorName, "boss",
					"Defeat Vulcan.", "The Crucible guardian has entered the battlefield.",
					data.getBoolean("dkc_floor_10_boss_defeated") ? 1 : 0, 1);
		}
		if (floor == DkcRadiruManager.FLOOR && complete) {
			return new Snapshot(true, floor, cleared, floorName, "radiru",
					"House Radiru has surrendered.",
					"Enter the castle. Right-click Esil to accept the permit - or betray House Radiru.", required, required);
		}
		if (complete) {
			return new Snapshot(true, floor, cleared, floorName, "permit",
					"Floor objective complete.", "Your Entry Permit is being issued.", required, required);
		}

		String objective = floor == DkcRadiruManager.FLOOR
				? "Overpower House Radiru's defenders."
				: floor == 10 ? "Defeat the Crucible defenders." : "Defeat the floor defenders.";
		String detail = !data.getBoolean(prefix + "_spawned")
				? "The System is initializing this floor's encounter."
				: !data.getBoolean(prefix + "_initial_spawned")
						? "The defenders are gathering..." : "";
		return new Snapshot(true, floor, cleared, floorName,
				floor == DkcRadiruManager.FLOOR ? "radiru" : "wave",
				objective, detail, kills, required);
	}

	private static Snapshot throneSnapshot(CompoundTag data, int floor, int cleared, String floorName) {
		boolean baranDown = data.getBoolean("dkc_floor_20_baran_defeated");
		boolean kaiselinDown = data.getBoolean("dkc_floor_20_kaiselin_defeated");
		int progress = (baranDown ? 1 : 0) + (kaiselinDown ? 1 : 0);
		String detail;
		if (progress == 0)
			detail = "Baran and Kaiselin still guard the throne.";
		else if (baranDown)
			detail = "Baran defeated - Kaiselin remains.";
		else
			detail = "Kaiselin defeated - Baran remains.";
		return new Snapshot(true, floor, cleared, floorName, "boss",
				"Defeat both throne guardians.", detail, progress, 2);
	}

	private static SololevelingModVariables.PlayerVariables variables(ServerPlayer player) {
		return player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
	}

	private static void send(ServerPlayer player, Snapshot snapshot) {
		SololevelingMod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player),
				new DkcQuestProgressMessage(snapshot.active, snapshot.floor, snapshot.cleared,
						snapshot.floorName, snapshot.phase, snapshot.objective, snapshot.detail,
						snapshot.progress, snapshot.target));
	}

	private static synchronized void remember(ServerPlayer player, Snapshot snapshot) {
		LAST_SENT.computeIfAbsent(player.server, ignored -> new HashMap<>())
				.put(player.getUUID(), snapshot);
	}

	private static synchronized boolean rememberIfChanged(ServerPlayer player, Snapshot snapshot) {
		Map<UUID, Snapshot> serverCache = LAST_SENT.computeIfAbsent(player.server, ignored -> new HashMap<>());
		Snapshot previous = serverCache.put(player.getUUID(), snapshot);
		return !snapshot.equals(previous);
	}

	private static synchronized void forget(MinecraftServer server, UUID playerId) {
		Map<UUID, Snapshot> serverCache = LAST_SENT.get(server);
		if (serverCache == null)
			return;
		serverCache.remove(playerId);
		if (serverCache.isEmpty())
			LAST_SENT.remove(server);
	}

	private static synchronized boolean markHeld(ServerPlayer player) {
		return HELD.computeIfAbsent(player.server, ignored -> new java.util.HashSet<>()).add(player.getUUID());
	}

	private static synchronized boolean isHeld(ServerPlayer player) {
		Set<UUID> heldPlayers = HELD.get(player.server);
		return heldPlayers != null && heldPlayers.contains(player.getUUID());
	}

	private static synchronized void unmarkHeld(MinecraftServer server, UUID playerId) {
		Set<UUID> heldPlayers = HELD.get(server);
		if (heldPlayers == null)
			return;
		heldPlayers.remove(playerId);
		if (heldPlayers.isEmpty())
			HELD.remove(server);
	}

	private static synchronized void forgetPressLimit(MinecraftServer server, UUID playerId) {
		Map<UUID, Long> serverLimits = PRESS_ALLOWED_AT.get(server);
		if (serverLimits == null)
			return;
		serverLimits.remove(playerId);
		if (serverLimits.isEmpty())
			PRESS_ALLOWED_AT.remove(server);
	}

	@SubscribeEvent
	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		if (isHeld(player))
			syncNow(player);
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> capability.questinfo = false);
		stopTracking(player);
		forgetPressLimit(player.server, player.getUUID());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.ifPresent(capability -> capability.questinfo = false);
		stopTracking(player);
	}

	@SubscribeEvent
	public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
					.ifPresent(capability -> capability.questinfo = false);
			stopTracking(player);
			forgetPressLimit(player.server, player.getUUID());
		}
	}

	@SubscribeEvent
	public static synchronized void onServerStopped(ServerStoppedEvent event) {
		LAST_SENT.remove(event.getServer());
		HELD.remove(event.getServer());
		PRESS_ALLOWED_AT.remove(event.getServer());
	}

	private record Snapshot(boolean active, int floor, int cleared, String floorName,
			String phase, String objective, String detail, int progress, int target) {
		private static final Snapshot INACTIVE = new Snapshot(false, 0, 0, "", "", "", "", 0, 0);
	}
}
