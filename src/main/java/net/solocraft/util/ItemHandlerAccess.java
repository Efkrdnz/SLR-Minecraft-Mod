package net.solocraft.util;

import java.util.Optional;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Nullable NeoForge capability lookups exposed as Optional for legacy menus. */
public final class ItemHandlerAccess {
	private ItemHandlerAccess() {
	}

	public static Optional<IItemHandler> get(ItemStack stack) {
		return Optional.ofNullable(stack.getCapability(Capabilities.ItemHandler.ITEM));
	}

	public static Optional<IItemHandler> get(Entity entity) {
		return Optional.ofNullable(entity.getCapability(Capabilities.ItemHandler.ENTITY));
	}

	public static Optional<IItemHandler> get(BlockEntity blockEntity) {
		Level level = blockEntity.getLevel();
		if (level == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(level.getCapability(Capabilities.ItemHandler.BLOCK,
				blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null));
	}
}
