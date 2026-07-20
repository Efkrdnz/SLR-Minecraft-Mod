
package net.solocraft.world.inventory;

import net.solocraft.init.SololevelingModMenus;
import net.solocraft.util.ShadowMonarchManager;

import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.ContainerData;
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

public class ShadowSummonGUIMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	private static final int SHADOW_TYPE_COUNT = 13;
	private static final int SHADOW_DATA_COUNT = SHADOW_TYPE_COUNT * 2;
	public final static HashMap<String, Object> guistate = new HashMap<>();
	public final Level world;
	public final Player entity;
	public int x, y, z;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;
	private Supplier<Boolean> boundItemMatcher = null;
	private Entity boundEntity = null;
	private BlockEntity boundBlockEntity = null;
	private final ContainerData shadowData;

	public ShadowSummonGUIMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(SololevelingModMenus.SHADOW_SUMMON_GUI.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(0);
		if (this.world.isClientSide()) {
			this.shadowData = new SimpleContainerData(SHADOW_DATA_COUNT);
		} else {
			ShadowMonarchManager.prepareRosterForDisplay(this.entity);
			this.shadowData = new ContainerData() {
				@Override
				public int get(int index) {
					if (index < 0 || index >= SHADOW_DATA_COUNT)
						return 0;
					int buttonId = index % SHADOW_TYPE_COUNT;
					String type = ShadowMonarchManager.typeForSummonButton(buttonId);
					return index < SHADOW_TYPE_COUNT
							? ShadowMonarchManager.highestRankForDisplay(ShadowSummonGUIMenu.this.entity, type)
							: ShadowMonarchManager.highestLevelForDisplay(ShadowSummonGUIMenu.this.entity, type);
				}

				@Override
				public void set(int index, int value) {
				}

				@Override
				public int getCount() {
					return SHADOW_DATA_COUNT;
				}
			};
		}
		this.addDataSlots(this.shadowData);
		BlockPos pos = null;
		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);
		}
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

	public int shadowRank(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return ShadowMonarchManager.RANK_NORMAL;
		return this.shadowData.get(buttonId);
	}

	public int shadowLevel(int buttonId) {
		if (buttonId < 0 || buttonId >= SHADOW_TYPE_COUNT)
			return 1;
		return Math.max(1, this.shadowData.get(SHADOW_TYPE_COUNT + buttonId));
	}
}
