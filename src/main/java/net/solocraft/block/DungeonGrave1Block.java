
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

public class DungeonGrave1Block extends Block {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public DungeonGrave1Block() {
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
			default -> Shapes.or(box(1, 0, 4, 15, 1, 11), box(2, 1, 5, 14, 4, 10), box(2.5, 4, 6, 13.5, 14, 9), box(4.5, 14, 6, 11.5, 19, 9), box(11.33223, 12.85355, 6, 12.73223, 15.70355, 9), box(3.62265, 19.0124, 6, 12.47265, 20.4124, 9),
					box(5.62265, 20.3624, 6, 10.47265, 22.4124, 9), box(3.26777, 12.85355, 6, 4.66777, 15.70355, 9));
			case NORTH -> Shapes.or(box(1, 0, 5, 15, 1, 12), box(2, 1, 6, 14, 4, 11), box(2.5, 4, 7, 13.5, 14, 10), box(4.5, 14, 7, 11.5, 19, 10), box(3.26777, 12.85355, 7, 4.66777, 15.70355, 10), box(3.52735, 19.0124, 7, 12.37735, 20.4124, 10),
					box(5.52735, 20.3624, 7, 10.37735, 22.4124, 10), box(11.33223, 12.85355, 7, 12.73223, 15.70355, 10));
			case EAST -> Shapes.or(box(4, 0, 1, 11, 1, 15), box(5, 1, 2, 10, 4, 14), box(6, 4, 2.5, 9, 14, 13.5), box(6, 14, 4.5, 9, 19, 11.5), box(6, 12.85355, 3.26777, 9, 15.70355, 4.66777), box(6, 19.0124, 3.52735, 9, 20.4124, 12.37735),
					box(6, 20.3624, 5.52735, 9, 22.4124, 10.37735), box(6, 12.85355, 11.33223, 9, 15.70355, 12.73223));
			case WEST -> Shapes.or(box(5, 0, 1, 12, 1, 15), box(6, 1, 2, 11, 4, 14), box(7, 4, 2.5, 10, 14, 13.5), box(7, 14, 4.5, 10, 19, 11.5), box(7, 12.85355, 11.33223, 10, 15.70355, 12.73223), box(7, 19.0124, 3.62265, 10, 20.4124, 12.47265),
					box(7, 20.3624, 5.62265, 10, 22.4124, 10.47265), box(7, 12.85355, 3.26777, 10, 15.70355, 4.66777));
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
