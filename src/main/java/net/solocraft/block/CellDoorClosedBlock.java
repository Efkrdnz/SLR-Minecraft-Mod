
package net.solocraft.block;

import net.solocraft.procedures.CellDoorClosedOnBlockRightClickedProcedure;

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

public class CellDoorClosedBlock extends Block {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public CellDoorClosedBlock() {
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
			default -> Shapes.or(box(0, 27, 6, 16, 30, 10), box(0, 30, 5, 16, 32, 11), box(13.5, -16, 7.5, 14.5, 27, 8.5), box(9.5, -16, 7.5, 10.5, 27, 8.5), box(5.5, -16, 7.5, 6.5, 27, 8.5), box(1.5, -16, 7.5, 2.5, 27, 8.5),
					box(0, -14, 6, 1, 27, 10), box(0, -16, 5, 1, -14, 11), box(15, -14, 6, 16, 27, 10), box(15, -16, 5, 16, -14, 11), box(10.5, 0, 8.1, 13.5, 4, 9.1));
			case NORTH -> Shapes.or(box(0, 27, 6, 16, 30, 10), box(0, 30, 5, 16, 32, 11), box(1.5, -16, 7.5, 2.5, 27, 8.5), box(5.5, -16, 7.5, 6.5, 27, 8.5), box(9.5, -16, 7.5, 10.5, 27, 8.5), box(13.5, -16, 7.5, 14.5, 27, 8.5),
					box(15, -14, 6, 16, 27, 10), box(15, -16, 5, 16, -14, 11), box(0, -14, 6, 1, 27, 10), box(0, -16, 5, 1, -14, 11), box(2.5, 0, 6.9, 5.5, 4, 7.9));
			case EAST -> Shapes.or(box(6, 27, 0, 10, 30, 16), box(5, 30, 0, 11, 32, 16), box(7.5, -16, 1.5, 8.5, 27, 2.5), box(7.5, -16, 5.5, 8.5, 27, 6.5), box(7.5, -16, 9.5, 8.5, 27, 10.5), box(7.5, -16, 13.5, 8.5, 27, 14.5),
					box(6, -14, 15, 10, 27, 16), box(5, -16, 15, 11, -14, 16), box(6, -14, 0, 10, 27, 1), box(5, -16, 0, 11, -14, 1), box(8.1, 0, 2.5, 9.1, 4, 5.5));
			case WEST -> Shapes.or(box(6, 27, 0, 10, 30, 16), box(5, 30, 0, 11, 32, 16), box(7.5, -16, 13.5, 8.5, 27, 14.5), box(7.5, -16, 9.5, 8.5, 27, 10.5), box(7.5, -16, 5.5, 8.5, 27, 6.5), box(7.5, -16, 1.5, 8.5, 27, 2.5),
					box(6, -14, 0, 10, 27, 1), box(5, -16, 0, 11, -14, 1), box(6, -14, 15, 10, 27, 16), box(5, -16, 15, 11, -14, 16), box(6.9, 0, 10.5, 7.9, 4, 13.5));
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
		CellDoorClosedOnBlockRightClickedProcedure.execute(world, x, y, z);
		return InteractionResult.SUCCESS;
	}
}
