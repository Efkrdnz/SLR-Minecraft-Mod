
package net.solocraft.world.inventory;

import net.solocraft.procedures.StorePotionNewFillProcedure;
import net.solocraft.init.SololevelingModMenus;

import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;

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

/**
 * Potion shop — reworked to the Food/Weapon slot pattern: 9 display-only shop
 * slots (3 columns S/M/L × 3 rows HP/MP/FTG) filled each tick by
 * {@link StorePotionNewFillProcedure}, plus the player inventory. Buying is
 * handled by the screen (click a shop slot → StorePotionNewButtonMessage).
 */
@Mod.EventBusSubscriber
public class StorePotionNewMenu extends AbstractContainerMenu implements Supplier<Map<Integer, Slot>> {
	public final static HashMap<String, Object> guistate = new HashMap<>();

	// shop slot layout (item top-left, relative to the centred menu origin)
	public static final int[] COLS = { -26, 10, 46 };
	public static final int[] ROWS = { -56, -20, 16 };

	public final Level world;
	public final Player entity;
	public int x, y, z;
	private ContainerLevelAccess access = ContainerLevelAccess.NULL;
	private IItemHandler internal;
	private final Map<Integer, Slot> customSlots = new HashMap<>();
	private boolean bound = false;

	public StorePotionNewMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
		super(SololevelingModMenus.STORE_POTION_NEW.get(), id);
		this.entity = inv.player;
		this.world = inv.player.level();
		this.internal = new ItemStackHandler(9);
		BlockPos pos = null;
		if (extraData != null) {
			pos = extraData.readBlockPos();
			this.x = pos.getX();
			this.y = pos.getY();
			this.z = pos.getZ();
			access = ContainerLevelAccess.create(world, pos);
		}
		// 9 display-only shop slots
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				int idx = r * 3 + c;
				this.customSlots.put(idx, this.addSlot(new SlotItemHandler(internal, idx, COLS[c], ROWS[r]) {
					@Override
					public boolean mayPickup(Player entity) {
						return false;
					}

					@Override
					public boolean mayPlace(ItemStack itemstack) {
						return false;
					}
				}));
			}
		}
		// player inventory + hotbar
		for (int si = 0; si < 3; ++si)
			for (int sj = 0; sj < 9; ++sj)
				this.addSlot(new Slot(inv, sj + (si + 1) * 9, -68 + sj * 18, 52 + si * 18));
		for (int si = 0; si < 9; ++si)
			this.addSlot(new Slot(inv, si, -68 + si * 18, 112));
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		// shift-click disabled (shop slots are display-only)
		return ItemStack.EMPTY;
	}

	public Map<Integer, Slot> get() {
		return customSlots;
	}

	@SubscribeEvent
	public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
		Player entity = event.player;
		if (event.phase == TickEvent.Phase.END && entity.containerMenu instanceof StorePotionNewMenu) {
			StorePotionNewFillProcedure.execute(entity);
		}
	}
}
