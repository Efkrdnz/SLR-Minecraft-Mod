package net.solocraft.world.inventory;

import net.solocraft.init.SololevelingModMenus;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Server-authoritative artifact slot for one customizable boss shadow.
 *
 * <p>The visible slot mirrors the real ItemStack stored on the strongest roster
 * entry for the selected boss type. Vanilla container clicks move the item; the
 * handler persists every mutation back to the roster after independently
 * validating the type-specific whitelist.</p>
 */
public class ShadowCustomizationMenu extends AbstractContainerMenu
		implements Supplier<Map<Integer, Slot>> {
	public static final int EQUIPMENT_SLOT_X = 201;
	public static final int EQUIPMENT_SLOT_Y = 82;
	public static final int PLAYER_INVENTORY_X = 129;
	public static final int PLAYER_INVENTORY_Y = 172;
	public static final int PLAYER_HOTBAR_Y = 230;

	private static final int DATA_RANK = 0;
	private static final int DATA_LEVEL_LOW = 1;
	private static final int DATA_LEVEL_HIGH = 2;
	private static final int DATA_RANK_XP_LOW = 3;
	private static final int DATA_RANK_XP_HIGH = 4;
	private static final int DATA_RANK_XP_NEEDED_LOW = 5;
	private static final int DATA_RANK_XP_NEEDED_HIGH = 6;
	private static final int DATA_NEXT_RANK = 7;
	private static final int DATA_FLAGS = 8;
	private static final int DATA_COUNT = 9;
	private static final int FLAG_LEVEL_CAPPED = 1;
	private static final int FLAG_MAX_RANK = 1 << 1;

	public final Level world;
	public final Player entity;
	public final int x;
	public final int y;
	public final int z;
	private final String shadowType;
	private final ItemStackHandler equipment;
	private final ContainerData shadowData;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean loadingEquipment;

	public ShadowCustomizationMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		super(SololevelingModMenus.SHADOW_CUSTOMIZATION.get(), id);
		this.entity = inventory.player;
		this.world = inventory.player.level();
		BlockPos position = extraData == null ? inventory.player.blockPosition()
				: extraData.readBlockPos();
		this.x = position.getX();
		this.y = position.getY();
		this.z = position.getZ();
		this.shadowType = extraData == null ? "" : extraData.readUtf(24);

		this.loadingEquipment = true;
		this.equipment = new ItemStackHandler(1) {
			@Override
			public int getSlotLimit(int slot) {
				return 1;
			}

			@Override
			protected void onContentsChanged(int slot) {
				if (!ShadowCustomizationMenu.this.loadingEquipment
						&& !ShadowCustomizationMenu.this.world.isClientSide())
					ShadowMonarchManager.setEquipmentForDisplay(
							ShadowCustomizationMenu.this.entity,
							ShadowCustomizationMenu.this.shadowType,
							getStackInSlot(slot));
			}
		};
		if (!this.world.isClientSide()) {
			ShadowMonarchManager.prepareRosterForDisplay(this.entity);
			this.equipment.setStackInSlot(0,
					ShadowMonarchManager.equipmentForDisplay(this.entity, this.shadowType));
		}
		this.loadingEquipment = false;

		this.customSlots.put(0, this.addSlot(new SlotItemHandler(this.equipment, 0,
				EQUIPMENT_SLOT_X, EQUIPMENT_SLOT_Y) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return ShadowMonarchManager.isValidBossEquipment(
						ShadowCustomizationMenu.this.shadowType, stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		}));

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				this.addSlot(new Slot(inventory, column + (row + 1) * 9,
						PLAYER_INVENTORY_X + column * 18,
						PLAYER_INVENTORY_Y + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			this.addSlot(new Slot(inventory, column,
					PLAYER_INVENTORY_X + column * 18, PLAYER_HOTBAR_Y));
		}

		if (this.world.isClientSide()) {
			this.shadowData = new SimpleContainerData(DATA_COUNT);
		} else {
			this.shadowData = new ContainerData() {
				@Override
				public int get(int index) {
					ShadowMonarchManager.ShadowDisplayProgress progress =
							ShadowMonarchManager.progressForDisplay(
									ShadowCustomizationMenu.this.entity,
									ShadowCustomizationMenu.this.shadowType);
					return switch (index) {
						case DATA_RANK -> progress.rank();
						case DATA_LEVEL_LOW -> lowWord(progress.level());
						case DATA_LEVEL_HIGH -> highWord(progress.level());
						case DATA_RANK_XP_LOW -> lowWord(progress.rankXp());
						case DATA_RANK_XP_HIGH -> highWord(progress.rankXp());
						case DATA_RANK_XP_NEEDED_LOW -> lowWord(progress.rankXpNeeded());
						case DATA_RANK_XP_NEEDED_HIGH -> highWord(progress.rankXpNeeded());
						case DATA_NEXT_RANK -> progress.nextRank();
						case DATA_FLAGS -> (progress.levelCapped()
								? FLAG_LEVEL_CAPPED : 0)
								| (progress.maxRank() ? FLAG_MAX_RANK : 0);
						default -> 0;
					};
				}

				@Override
				public void set(int index, int value) {
				}

				@Override
				public int getCount() {
					return DATA_COUNT;
				}
			};
		}
		this.addDataSlots(this.shadowData);
	}

	@Override
	public boolean stillValid(Player player) {
		return player == this.entity && ShadowMonarchManager.isCustomizableBoss(this.shadowType)
				&& (this.world.isClientSide()
						|| ShadowMonarchManager.hasShadowForDisplay(player, this.shadowType));
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (index < 0 || index >= this.slots.size())
			return ItemStack.EMPTY;
		Slot slot = this.slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;
		ItemStack current = slot.getItem();
		ItemStack original = current.copy();
		if (index == 0) {
			if (!this.moveItemStackTo(current, 1, this.slots.size(), true))
				return ItemStack.EMPTY;
		} else {
			if (!ShadowMonarchManager.isValidBossEquipment(this.shadowType, current)
					|| !this.moveItemStackTo(current, 0, 1, false))
				return ItemStack.EMPTY;
		}
		if (current.isEmpty())
			slot.set(ItemStack.EMPTY);
		else
			slot.setChanged();
		return original;
	}

	@Override
	public Map<Integer, Slot> get() {
		return this.customSlots;
	}

	public String shadowType() {
		return this.shadowType;
	}

	public int shadowRank() {
		return this.shadowData.get(DATA_RANK);
	}

	public int shadowLevel() {
		return Math.max(1, joinedWords(this.shadowData.get(DATA_LEVEL_LOW),
				this.shadowData.get(DATA_LEVEL_HIGH)));
	}

	public int rankXp() {
		return Math.max(0, joinedWords(this.shadowData.get(DATA_RANK_XP_LOW),
				this.shadowData.get(DATA_RANK_XP_HIGH)));
	}

	public int rankXpNeeded() {
		return Math.max(1, joinedWords(
				this.shadowData.get(DATA_RANK_XP_NEEDED_LOW),
				this.shadowData.get(DATA_RANK_XP_NEEDED_HIGH)));
	}

	public int nextRank() {
		return this.shadowData.get(DATA_NEXT_RANK);
	}

	public boolean isAtLevelCap() {
		return (this.shadowData.get(DATA_FLAGS) & FLAG_LEVEL_CAPPED) != 0;
	}

	public boolean isMaxRank() {
		return (this.shadowData.get(DATA_FLAGS) & FLAG_MAX_RANK) != 0;
	}

	private static int lowWord(int value) {
		return value & 0xFFFF;
	}

	private static int highWord(int value) {
		return value >>> 16 & 0xFFFF;
	}

	private static int joinedWords(int low, int high) {
		return low & 0xFFFF | (high & 0xFFFF) << 16;
	}
}
