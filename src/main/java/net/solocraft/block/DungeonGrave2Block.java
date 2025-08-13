
package net.solocraft.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Collections;

public class DungeonGrave2Block extends Block {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public DungeonGrave2Block() {
		super(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(1f, 10f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public void appendHoverText(ItemStack itemstack, BlockGetter world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			default -> Shapes.or(box(1.5, 0, 4.5, 14.5, 1, 13.5), box(2.5, 1, 5.5, 13.5, 3, 10.5), box(3, 3, 6.5, 13, 14, 9.5), box(10.83223, 12.85355, 6.5, 12.23223, 14.70355, 9.5), box(3.76777, 12.85355, 6.5, 5.16777, 14.70355, 9.5),
					box(6.16409, 13.99351, 6.5, 9.84409, 16.07351, 9.5), box(4.61471, 13.76695, 6.5, 6.61471, 15.76695, 9.5), box(9.38529, 13.76695, 6.5, 11.38529, 15.76695, 9.5));
			case NORTH -> Shapes.or(box(1.5, 0, 2.5, 14.5, 1, 11.5), box(2.5, 1, 5.5, 13.5, 3, 10.5), box(3, 3, 6.5, 13, 14, 9.5), box(3.76777, 12.85355, 6.5, 5.16777, 14.70355, 9.5), box(10.83223, 12.85355, 6.5, 12.23223, 14.70355, 9.5),
					box(6.15591, 13.99351, 6.5, 9.83591, 16.07351, 9.5), box(9.38529, 13.76695, 6.5, 11.38529, 15.76695, 9.5), box(4.61471, 13.76695, 6.5, 6.61471, 15.76695, 9.5));
			case EAST -> Shapes.or(box(4.5, 0, 1.5, 13.5, 1, 14.5), box(5.5, 1, 2.5, 10.5, 3, 13.5), box(6.5, 3, 3, 9.5, 14, 13), box(6.5, 12.85355, 3.76777, 9.5, 14.70355, 5.16777), box(6.5, 12.85355, 10.83223, 9.5, 14.70355, 12.23223),
					box(6.5, 13.99351, 6.15591, 9.5, 16.07351, 9.83591), box(6.5, 13.76695, 9.38529, 9.5, 15.76695, 11.38529), box(6.5, 13.76695, 4.61471, 9.5, 15.76695, 6.61471));
			case WEST -> Shapes.or(box(2.5, 0, 1.5, 11.5, 1, 14.5), box(5.5, 1, 2.5, 10.5, 3, 13.5), box(6.5, 3, 3, 9.5, 14, 13), box(6.5, 12.85355, 10.83223, 9.5, 14.70355, 12.23223), box(6.5, 12.85355, 3.76777, 9.5, 14.70355, 5.16777),
					box(6.5, 13.99351, 6.16409, 9.5, 16.07351, 9.84409), box(6.5, 13.76695, 4.61471, 9.5, 15.76695, 6.61471), box(6.5, 13.76695, 9.38529, 9.5, 15.76695, 11.38529));
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
		if (player.getInventory().getSelected().getItem() instanceof PickaxeItem tieredItem)
			return tieredItem.getTier().getLevel() >= 1;
		return false;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(this, 1));
	}
}
