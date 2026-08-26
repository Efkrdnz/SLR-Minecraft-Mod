package net.solocraft.item;

import net.solocraft.dungeon.data.DungeonDataManager;
import net.solocraft.dungeon.DatapackGateSelectionService;
import net.solocraft.entity.DatapackGateEntity;
import net.solocraft.init.SololevelingModEntities;

import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Spawn egg for addon/datapack-authored dungeons.
 *
 * <p>The authoritative server datapack snapshot is checked before the egg is
 * allowed to create a gate. This avoids consuming the egg or leaving a dead,
 * unconfigurable gate when no valid dungeon definitions are loaded.</p>
 */
public class DatapackGateSpawnEggItem extends DeferredSpawnEggItem {
	public DatapackGateSpawnEggItem(int backgroundColor, int highlightColor) {
		super(SololevelingModEntities.DATAPACK_GATE, backgroundColor, highlightColor,
				new Item.Properties());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		if (!(context.getLevel() instanceof ServerLevel serverLevel))
			return InteractionResult.SUCCESS;
		if (DungeonDataManager.dungeonIds().isEmpty()) {
			if (context.getPlayer() != null)
				context.getPlayer().displayClientMessage(
						Component.literal("No valid datapack dungeons are loaded."), true);
			return InteractionResult.FAIL;
		}

		ItemStack stack = context.getItemInHand();
		BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
		Entity spawned = SololevelingModEntities.DATAPACK_GATE.get().spawn(serverLevel, stack,
				context.getPlayer(), spawnPos, MobSpawnType.SPAWN_EGG, true, false);
		if (!(spawned instanceof DatapackGateEntity gate))
			return InteractionResult.FAIL;

		if (context.getPlayer() instanceof net.minecraft.server.level.ServerPlayer player)
			DatapackGateSelectionService.requestOpen(player, gate);
		if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild)
			stack.shrink(1);
		return InteractionResult.CONSUME;
	}
}
