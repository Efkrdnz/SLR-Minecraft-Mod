
package net.solocraft.block;

import net.solocraft.procedures.CellDoorOpenOnBlockRightClickedProcedure;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Collections;

public class CellDoorOpenBlock extends Block {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public CellDoorOpenBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1f, 10f).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
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
			default -> Shapes.or(box(0, 27, 6, 16, 30, 10), box(0, 30, 5, 16, 32, 11), box(0.8, -16, 20.8, 1.8, 27, 21.8), box(0.8, -16, 16.8, 1.8, 27, 17.8), box(0.8, -16, 12.8, 1.8, 27, 13.8), box(0.8, -16, 8.8, 1.8, 27, 9.8),
					box(0, -14, 6, 1, 27, 10), box(0, -16, 5, 1, -14, 11), box(15, -14, 6, 16, 27, 10), box(15, -16, 5, 16, -14, 11), box(0.2, 0, 17.8, 1.2, 4, 20.8));
			case NORTH -> Shapes.or(box(0, 27, 6, 16, 30, 10), box(0, 30, 5, 16, 32, 11), box(14.2, -16, -5.8, 15.2, 27, -4.8), box(14.2, -16, -1.8, 15.2, 27, -0.8), box(14.2, -16, 2.2, 15.2, 27, 3.2), box(14.2, -16, 6.2, 15.2, 27, 7.2),
					box(15, -14, 6, 16, 27, 10), box(15, -16, 5, 16, -14, 11), box(0, -14, 6, 1, 27, 10), box(0, -16, 5, 1, -14, 11), box(14.8, 0, -4.8, 15.8, 4, -1.8));
			case EAST -> Shapes.or(box(6, 27, 0, 10, 30, 16), box(5, 30, 0, 11, 32, 16), box(20.8, -16, 14.2, 21.8, 27, 15.2), box(16.8, -16, 14.2, 17.8, 27, 15.2), box(12.8, -16, 14.2, 13.8, 27, 15.2), box(8.8, -16, 14.2, 9.8, 27, 15.2),
					box(6, -14, 15, 10, 27, 16), box(5, -16, 15, 11, -14, 16), box(6, -14, 0, 10, 27, 1), box(5, -16, 0, 11, -14, 1), box(17.8, 0, 14.8, 20.8, 4, 15.8));
			case WEST -> Shapes.or(box(6, 27, 0, 10, 30, 16), box(5, 30, 0, 11, 32, 16), box(-5.8, -16, 0.8, -4.8, 27, 1.8), box(-1.8, -16, 0.8, -0.8, 27, 1.8), box(2.2, -16, 0.8, 3.2, 27, 1.8), box(6.2, -16, 0.8, 7.2, 27, 1.8),
					box(6, -14, 0, 10, 27, 1), box(5, -16, 0, 11, -14, 1), box(6, -14, 15, 10, 27, 16), box(5, -16, 15, 11, -14, 16), box(-4.8, 0, 0.2, -1.8, 4, 1.2));
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
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(this, 1));
	}

	@Override
	public InteractionResult use(BlockState blockstate, Level world, BlockPos pos, Player entity, InteractionHand hand, BlockHitResult hit) {
		super.use(blockstate, world, pos, entity, hand, hit);
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		double hitX = hit.getLocation().x;
		double hitY = hit.getLocation().y;
		double hitZ = hit.getLocation().z;
		Direction direction = hit.getDirection();
		CellDoorOpenOnBlockRightClickedProcedure.execute(world, x, y, z);
		return InteractionResult.SUCCESS;
	}
}
