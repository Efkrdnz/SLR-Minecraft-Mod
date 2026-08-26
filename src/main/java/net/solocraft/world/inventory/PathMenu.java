
package net.solocraft.world.inventory;

import net.solocraft.init.SololevelingModMenus;

import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;

public class PathMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	private static final long VALID_FLOOR_MASK = (1L << 20) - 1L;
	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private final TowerState towerState;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;

	public PathMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		this(id, inv, readOpeningData(extraData));
	}

	/** Server-side constructor. The client receives the same immutable snapshot in the opening payload. */
	public PathMenu(int id, Inventory inv, BlockPos position, TowerState towerState) {
		this(id, inv, new OpeningData(position, towerState));
	}

	private PathMenu(int id, Inventory inv, OpeningData openingData) {
		super(SololevelingModMenus.PATH.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(0);
		BlockPos pos = openingData.position();
		this.x = pos.getX();
		this.y = pos.getY();
		this.z = pos.getZ();
		this.towerState = openingData.towerState();
		this.access = ContainerLevelAccess.create(world, pos);
	}

	public static void writeOpeningData(FriendlyByteBuf buffer, BlockPos position, TowerState towerState) {
		TowerState state = towerState == null ? TowerState.EMPTY : towerState;
		buffer.writeBlockPos(position == null ? BlockPos.ZERO : position);
		buffer.writeLong(state.unlockedFloorsMask());
		buffer.writeLong(state.generatedFloorsMask());
		buffer.writeLong(state.armedTransitionsMask());
		buffer.writeVarInt(state.clearedFloors());
		buffer.writeVarInt(state.currentFloor());
		buffer.writeBoolean(state.insideDkc());
		buffer.writeBoolean(state.conquered());
		buffer.writeBoolean(state.hasRadiruCastleAccess());
		buffer.writeBoolean(state.radiruPact());
		buffer.writeBoolean(state.radiruSlaughtered());
	}

	private static OpeningData readOpeningData(FriendlyByteBuf buffer) {
		if (buffer == null)
			return new OpeningData(BlockPos.ZERO, TowerState.EMPTY);
		BlockPos position = buffer.readBlockPos();
		TowerState state = new TowerState(
				buffer.readLong(),
				buffer.readLong(),
				buffer.readLong(),
				buffer.readVarInt(),
				buffer.readVarInt(),
				buffer.readBoolean(),
				buffer.readBoolean(),
				buffer.readBoolean(),
				buffer.readBoolean(),
				buffer.readBoolean());
		return new OpeningData(position, state);
	}

	public long unlockedFloorsMask() {
		return towerState.unlockedFloorsMask();
	}

	public long generatedFloorsMask() {
		return towerState.generatedFloorsMask();
	}

	public long armedTransitionsMask() {
		return towerState.armedTransitionsMask();
	}

	public int clearedFloors() {
		return towerState.clearedFloors();
	}

	public int currentFloor() {
		return towerState.currentFloor();
	}

	public int highestUnlockedFloor() {
		return towerState.highestUnlockedFloor();
	}

	public boolean insideDkc() {
		return towerState.insideDkc();
	}

	public boolean conquered() {
		return towerState.conquered();
	}

	public boolean hasRadiruCastleAccess() {
		return towerState.hasRadiruCastleAccess();
	}

	public boolean radiruPact() {
		return towerState.radiruPact();
	}

	public boolean radiruSlaughtered() {
		return towerState.radiruSlaughtered();
	}

	public boolean isFloorUnlocked(int floor) {
		return towerState.isFloorUnlocked(floor);
	}

	public boolean isFloorGenerated(int floor) {
		return towerState.isFloorGenerated(floor);
	}

	public boolean isTransitionArmed(int floor) {
		return towerState.isTransitionArmed(floor);
	}

	public boolean isFloorCleared(int floor) {
		return towerState.isFloorCleared(floor);
	}

	@Override
	public boolean stillValid(Player player) {
		if (this.bound) {
			if (this.boundItemMatcher != null)
				return this.boundItemMatcher.get();
			else if (this.boundBlockEntity != null)
				return AbstractContainerMenu.stillValid(this.access, player, this.boundBlockEntity.getBlockState().getBlock());
			else if (this.boundEntity != null)
				return this.boundEntity.isAlive();
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		return ItemStack.EMPTY;
	}

	public Map<Integer, Slot> get() {
		return customSlots;
	}

	private record OpeningData(BlockPos position, TowerState towerState) {
		private OpeningData {
			position = position == null ? BlockPos.ZERO : position;
			towerState = towerState == null ? TowerState.EMPTY : towerState;
		}
	}

	/**
	 * Server-authored DKC state captured when the menu opens. Bit zero represents
	 * Floor 1, bit nineteen represents Floor 20.
	 */
	public record TowerState(long unlockedFloorsMask, long generatedFloorsMask,
			long armedTransitionsMask, int clearedFloors, int currentFloor,
			boolean insideDkc, boolean conquered, boolean hasRadiruCastleAccess,
			boolean radiruPact, boolean radiruSlaughtered) {
		public static final TowerState EMPTY = new TowerState(
				0L, 0L, 0L, 0, 0, false, false, false, false, false);

		public TowerState {
			unlockedFloorsMask &= VALID_FLOOR_MASK;
			generatedFloorsMask &= VALID_FLOOR_MASK;
			armedTransitionsMask &= VALID_FLOOR_MASK;
			clearedFloors = Math.max(0, Math.min(20, clearedFloors));
			currentFloor = Math.max(0, Math.min(20, currentFloor));
		}

		public boolean isFloorUnlocked(int floor) {
			return hasFloor(unlockedFloorsMask, floor);
		}

		public boolean isFloorGenerated(int floor) {
			return hasFloor(generatedFloorsMask, floor);
		}

		public boolean isTransitionArmed(int floor) {
			return hasFloor(armedTransitionsMask, floor);
		}

		public boolean isFloorCleared(int floor) {
			return floor >= 1 && floor <= clearedFloors;
		}

		public int highestUnlockedFloor() {
			return unlockedFloorsMask == 0L ? 0 : 64 - Long.numberOfLeadingZeros(unlockedFloorsMask);
		}

		private static boolean hasFloor(long mask, int floor) {
			return floor >= 1 && floor <= 20 && (mask & (1L << (floor - 1))) != 0L;
		}
	}
}
