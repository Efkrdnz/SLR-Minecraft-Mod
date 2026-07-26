package net.solocraft.dkc;

import net.solocraft.network.SololevelingModVariables;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Durable, overworld-backed generation and tower-transition state for DKC runs. */
public final class DkcRunSavedData extends SavedData {
	private static final String DATA_NAME = "sololeveling_dkc_runs";
	private static final int SCHEMA_VERSION = 3;
	private static final int LAYOUT_VERSION = 6;
	private static final long VALID_BITS = (1L << DkcFloorRegistry.LAST_FLOOR) - 1L;

	private final Map<UUID, RunState> runs = new LinkedHashMap<>();

	public static DkcRunSavedData get(MinecraftServer server) {
		if (server == null)
			throw new IllegalArgumentException("A server is required.");
		return server.overworld().getDataStorage().computeIfAbsent(
				DkcRunSavedData::load, DkcRunSavedData::new, DATA_NAME);
	}

	public RunState getOrCreate(ServerPlayer player) {
		RunState existing = runs.get(player.getUUID());
		if (existing != null) {
			ensureSlot(existing);
			syncLegacyAnchor(player, existing);
			return existing;
		}

		SololevelingModVariables.PlayerVariables vars = player
				.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null)
				.orElse(new SololevelingModVariables.PlayerVariables());
		int storedFloor = Math.max(0, Math.min(DkcFloorRegistry.LAST_FLOOR,
				(int) player.getPersistentData().getDouble("dkc_current_floor")));
		boolean legacyRun = vars.dkc_started || vars.dkc_cleared > 0 || (vars.dungeoning && storedFloor > 0);
		RunState created = new RunState();
		created.slot = allocateSlot();
		created.unlockedFloors = bit(1);
		// Worlds from the legacy single-dimension implementation already treated
		// every cleared floor and its successor as reachable. Preserve that access
		// once, then let the new permit pedestal own all future transitions.
		if (legacyRun) {
			int cleared = Math.max(0, Math.min(DkcFloorRegistry.LAST_FLOOR, (int) vars.dkc_cleared));
			int highestReachable = Math.max(storedFloor, Math.min(DkcFloorRegistry.LAST_FLOOR, cleared + 1));
			for (int floor = 1; floor <= highestReachable; floor++)
				created.unlockedFloors |= bit(floor);
			for (int floor = 1; floor <= Math.min(DkcFloorRegistry.LAST_FLOOR - 1, cleared); floor++)
				created.armedTransitions |= bit(floor);
		}
		runs.put(player.getUUID(), created);
		setDirty();
		syncLegacyAnchor(player, created);
		return created;
	}

	/** Returns (and, for an online player, allocates) the collision-safe spatial slot. */
	public int slot(ServerPlayer player) {
		return getOrCreate(player).slot;
	}

	/** Returns an existing slot without creating run state for an offline owner. */
	public int slot(UUID owner) {
		RunState state = owner == null ? null : runs.get(owner);
		return state == null ? -1 : state.slot;
	}

	public boolean isUnlocked(ServerPlayer player, int floor) {
		return validFloor(floor) && (getOrCreate(player).unlockedFloors & bit(floor)) != 0L;
	}

	public boolean isGenerated(ServerPlayer player, int floor) {
		return validFloor(floor) && (getOrCreate(player).generatedFloors & bit(floor)) != 0L;
	}

	public boolean isTransitionArmed(ServerPlayer player, int floor) {
		return validFloor(floor) && (getOrCreate(player).armedTransitions & bit(floor)) != 0L;
	}

	public boolean needsCleanup(ServerPlayer player, int floor) {
		return validFloor(floor) && (getOrCreate(player).cleanupFloors & bit(floor)) != 0L;
	}

	public boolean claimTransition(ServerPlayer player, int currentFloor) {
		if (currentFloor < 1 || currentFloor >= DkcFloorRegistry.LAST_FLOOR)
			return false;
		RunState state = getOrCreate(player);
		long transitionBit = bit(currentFloor);
		long nextBit = bit(currentFloor + 1);
		if ((state.armedTransitions & transitionBit) != 0L)
			return false;
		state.armedTransitions |= transitionBit;
		state.unlockedFloors |= nextBit;
		setDirty();
		return true;
	}

	public void unlockFloor(ServerPlayer player, int floor) {
		if (!validFloor(floor))
			return;
		RunState state = getOrCreate(player);
		long next = state.unlockedFloors | bit(floor);
		if (next != state.unlockedFloors) {
			state.unlockedFloors = next;
			setDirty();
		}
	}

	public void markGenerated(ServerPlayer player, int floor) {
		if (!validFloor(floor))
			return;
		RunState state = getOrCreate(player);
		long next = state.generatedFloors | bit(floor);
		long cleanup = state.cleanupFloors & ~bit(floor);
		if (next != state.generatedFloors || cleanup != state.cleanupFloors) {
			state.generatedFloors = next;
			state.cleanupFloors = cleanup;
			setDirty();
		}
	}

	/** Keeps the existing creative DKC level tool useful with the new registry. */
	public void setDebugProgress(ServerPlayer player, int clearedFloors) {
		int cleared = Math.max(0, Math.min(DkcFloorRegistry.LAST_FLOOR, clearedFloors));
		RunState state = getOrCreate(player);
		long unlocked = 0L;
		long armed = 0L;
		for (int floor = 1; floor <= Math.min(DkcFloorRegistry.LAST_FLOOR, cleared + 1); floor++)
			unlocked |= bit(floor);
		for (int floor = 1; floor <= Math.min(DkcFloorRegistry.LAST_FLOOR - 1, cleared); floor++)
			armed |= bit(floor);
		state.unlockedFloors = unlocked;
		state.armedTransitions = armed;
		state.generatedFloors &= unlocked;
		setDirty();
	}

	@Override
	@Nonnull
	public CompoundTag save(@Nonnull CompoundTag root) {
		root.putInt("SchemaVersion", SCHEMA_VERSION);
		root.putInt("LayoutVersion", LAYOUT_VERSION);
		ListTag list = new ListTag();
		for (Map.Entry<UUID, RunState> entry : runs.entrySet()) {
			CompoundTag tag = new CompoundTag();
			tag.putUUID("Player", entry.getKey());
			tag.putInt("Slot", entry.getValue().slot);
			tag.putLong("Unlocked", entry.getValue().unlockedFloors & VALID_BITS);
			tag.putLong("Generated", entry.getValue().generatedFloors & VALID_BITS);
			tag.putLong("Armed", entry.getValue().armedTransitions & VALID_BITS);
			tag.putLong("Cleanup", entry.getValue().cleanupFloors & VALID_BITS);
			list.add(tag);
		}
		root.put("Runs", list);
		return root;
	}

	private static DkcRunSavedData load(CompoundTag root) {
		DkcRunSavedData data = new DkcRunSavedData();
		boolean layoutMatches = root.getInt("LayoutVersion") == LAYOUT_VERSION;
		boolean[] usedSlots = new boolean[DkcSpatialLayout.MAX_SLOTS];
		if (!root.contains("Runs", Tag.TAG_LIST))
			return data;
		ListTag list = root.getList("Runs", Tag.TAG_COMPOUND);
		for (int index = 0; index < Math.min(list.size(), 4096); index++) {
			CompoundTag tag = list.getCompound(index);
			if (!tag.hasUUID("Player"))
				continue;
			UUID playerId = tag.getUUID("Player");
			if (data.runs.containsKey(playerId)) {
				data.setDirty();
				continue;
			}
			RunState state = new RunState();
			int requestedSlot = tag.contains("Slot", Tag.TAG_INT) ? tag.getInt("Slot") : -1;
			if (DkcSpatialLayout.validSlot(requestedSlot) && !usedSlots[requestedSlot]) {
				state.slot = requestedSlot;
				usedSlots[requestedSlot] = true;
			} else {
				state.slot = -1;
				data.setDirty();
			}
			state.unlockedFloors = (tag.getLong("Unlocked") | bit(1)) & VALID_BITS;
			long storedGenerated = tag.getLong("Generated") & VALID_BITS;
			state.generatedFloors = layoutMatches && DkcSpatialLayout.validSlot(state.slot)
					? storedGenerated : 0L;
			state.armedTransitions = tag.getLong("Armed") & VALID_BITS;
			state.cleanupFloors = tag.getLong("Cleanup") & VALID_BITS;
			if (!layoutMatches && DkcSpatialLayout.validSlot(state.slot))
				state.cleanupFloors |= storedGenerated & bit(15);
			data.runs.put(playerId, state);
		}
		if (!layoutMatches)
			data.setDirty();
		return data;
	}

	private void ensureSlot(RunState state) {
		if (DkcSpatialLayout.validSlot(state.slot))
			return;
		state.slot = allocateSlot();
		// A replacement slot points at a different, initially empty sector. Never
		// carry generated bits over from a corrupt or duplicate persisted slot.
		state.generatedFloors = 0L;
		state.cleanupFloors = 0L;
		state.anchorSynced = false;
		setDirty();
	}

	private int allocateSlot() {
		boolean[] used = new boolean[DkcSpatialLayout.MAX_SLOTS];
		for (RunState state : runs.values())
			if (DkcSpatialLayout.validSlot(state.slot))
				used[state.slot] = true;
		for (int slot = 0; slot < used.length; slot++)
			if (!used[slot])
				return slot;
		throw new IllegalStateException("Demon King's Castle has exhausted all spatial run slots.");
	}

	private static void syncLegacyAnchor(ServerPlayer player, RunState state) {
		if (state.anchorSynced || !DkcSpatialLayout.validSlot(state.slot))
			return;
		BlockPos anchor = DkcSpatialLayout.floorOrigin(state.slot, 1);
		player.getCapability(SololevelingModVariables.PLAYER_VARIABLES_CAPABILITY, null).ifPresent(capability -> {
			if (capability.dkc_x != anchor.getX() || capability.dkc_y != anchor.getY()
					|| capability.dkc_z != anchor.getZ()) {
				capability.dkc_x = anchor.getX();
				capability.dkc_y = anchor.getY();
				capability.dkc_z = anchor.getZ();
				capability.syncPlayerVariables(player);
			}
		});
		state.anchorSynced = true;
	}

	private static long bit(int floor) {
		return 1L << (floor - 1);
	}

	private static boolean validFloor(int floor) {
		return floor >= DkcFloorRegistry.FIRST_FLOOR && floor <= DkcFloorRegistry.LAST_FLOOR;
	}

	public static final class RunState {
		private int slot = -1;
		private transient boolean anchorSynced;
		private long unlockedFloors;
		private long generatedFloors;
		private long armedTransitions;
		private long cleanupFloors;

		public long unlockedFloors() {
			return unlockedFloors;
		}

		public long generatedFloors() {
			return generatedFloors;
		}

		public long armedTransitions() {
			return armedTransitions;
		}

		public int slot() {
			return slot;
		}
	}
}
