package net.solocraft.item;

import net.solocraft.dungeon.runtime.SnowRedGateArenaManager;
import net.solocraft.init.SololevelingModEntities;
import net.solocraft.world.dimension.rift.RiftTerritory;

import net.minecraftforge.common.ForgeSpawnEggItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

public class MonarchTerritoryGateSpawnEggItem extends ForgeSpawnEggItem {
	private final RiftTerritory territory;

	public MonarchTerritoryGateSpawnEggItem(RiftTerritory territory, int backgroundColor, int highlightColor) {
		super(SololevelingModEntities.RED_GATE, backgroundColor, highlightColor, new Item.Properties());
		this.territory = territory;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		if (!(level instanceof ServerLevel serverLevel))
			return InteractionResult.SUCCESS;

		ItemStack stack = context.getItemInHand();
		BlockPos spawnPos = context.getClickedPos().relative(context.getClickedFace());
		Entity spawned = SololevelingModEntities.RED_GATE.get().spawn(serverLevel, stack, context.getPlayer(), spawnPos,
				MobSpawnType.SPAWN_EGG, true, false);
		if (spawned == null)
			return InteractionResult.FAIL;

		spawned.getPersistentData().putString(SnowRedGateArenaManager.TERRITORY_TAG, territory.id());
		if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild)
			stack.shrink(1);
		return InteractionResult.CONSUME;
	}

	@Override
	public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal("Territory: " + territory.displayName()).withStyle(ChatFormatting.DARK_RED));
		tooltip.add(Component.literal(levelRangeText()).withStyle(ChatFormatting.GRAY));
		tooltip.add(Component.literal("Wave-based red gate encounter.").withStyle(ChatFormatting.DARK_GRAY));
	}

	private String levelRangeText() {
		if (territory == RiftTerritory.FROST)
			return "Starter red gate: level 0-10.";
		return "Monarch red gate: scales to the party.";
	}
}
