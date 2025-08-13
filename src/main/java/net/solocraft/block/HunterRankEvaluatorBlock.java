
package net.solocraft.block;

import net.solocraft.procedures.RankEvaluatorOnBlockRightClickedProcedure;
import net.solocraft.init.SololevelingModBlockEntities;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
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

import javax.annotation.Nullable;

import java.util.List;
import java.util.Collections;

public class HunterRankEvaluatorBlock extends BaseEntityBlock implements EntityBlock {
	public static final IntegerProperty ANIMATION = IntegerProperty.create("animation", 0, (int) 1);
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

	public HunterRankEvaluatorBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(-1, 3600000).noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	@Nullable
	@Override
	public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
		return SololevelingModBlockEntities.HUNTER_RANK_EVALUATOR.get().create(blockPos, blockState);
	}

	@Override
	public void appendHoverText(ItemStack itemstack, BlockGetter world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("After Right Clicking this block right click to \"Secretary\" NPC to get evaluated"));
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
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {

		return switch (state.getValue(FACING)) {
			default -> Shapes.or(box(-14.5, 0, -14.5, 30.5, 2, 30.5), box(-16, 0, -16, -14.5, 4.75, 32), box(30.5, 0, -16, 32, 4.75, 32), box(-14.5, 2, -16, 30.5, 4.75, -14.5), box(-14.5, 4.75, 30.5, 30.5, 7, 32), box(0.5, 2, 1, 15.5, 4, 15),
					box(-13, 2, -13, 29, 2.75, 29), box(-7.25, 2.5, -7.75, 24, 3.75, 23.25), box(7.204, 0, 3, 8.796, 16.75, 12.75), box(7.204, 0, 4, 8.796, 16.75, 12), box(7.204, 0, 1.5, 8.796, 17.5, 14.5), box(7.204, 0, 4, 8.796, 16.75, 12),
					box(7.204, 0, 3, 8.796, 16.75, 12.75), box(4, 0, 7.204, 12, 16.75, 8.796), box(1, 0, 7.204, 14.5, 17.5, 8.796), box(4, 0, 7.204, 12, 16.75, 8.796), box(-6.75, 17.75, -6.25, 22.5, 45.75, 22.75));
			case NORTH -> Shapes.or(box(-14.5, 0, -14.5, 30.5, 2, 30.5), box(30.5, 0, -16, 32, 4.75, 32), box(-16, 0, -16, -14.5, 4.75, 32), box(-14.5, 2, 30.5, 30.5, 4.75, 32), box(-14.5, 4.75, -16, 30.5, 7, -14.5), box(0.5, 2, 1, 15.5, 4, 15),
					box(-13, 2, -13, 29, 2.75, 29), box(-8, 2.5, -7.25, 23.25, 3.75, 23.75), box(7.204, 0, 3.25, 8.796, 16.75, 13), box(7.204, 0, 4, 8.796, 16.75, 12), box(7.204, 0, 1.5, 8.796, 17.5, 14.5), box(7.204, 0, 4, 8.796, 16.75, 12),
					box(7.204, 0, 3.25, 8.796, 16.75, 13), box(4, 0, 7.204, 12, 16.75, 8.796), box(1.5, 0, 7.204, 15, 17.5, 8.796), box(4, 0, 7.204, 12, 16.75, 8.796), box(-6.5, 17.75, -6.75, 22.75, 45.75, 22.25));
			case EAST -> Shapes.or(box(-14.5, 0, -14.5, 30.5, 2, 30.5), box(-16, 0, 30.5, 32, 4.75, 32), box(-16, 0, -16, 32, 4.75, -14.5), box(-16, 2, -14.5, -14.5, 4.75, 30.5), box(30.5, 4.75, -14.5, 32, 7, 30.5), box(1, 2, 0.5, 15, 4, 15.5),
					box(-13, 2, -13, 29, 2.75, 29), box(-7.75, 2.5, -8, 23.25, 3.75, 23.25), box(3, 0, 7.204, 12.75, 16.75, 8.796), box(4, 0, 7.204, 12, 16.75, 8.796), box(1.5, 0, 7.204, 14.5, 17.5, 8.796), box(4, 0, 7.204, 12, 16.75, 8.796),
					box(3, 0, 7.204, 12.75, 16.75, 8.796), box(7.204, 0, 4, 8.796, 16.75, 12), box(7.204, 0, 1.5, 8.796, 17.5, 15), box(7.204, 0, 4, 8.796, 16.75, 12), box(-6.25, 17.75, -6.5, 22.75, 45.75, 22.75));
			case WEST -> Shapes.or(box(-14.5, 0, -14.5, 30.5, 2, 30.5), box(-16, 0, -16, 32, 4.75, -14.5), box(-16, 0, 30.5, 32, 4.75, 32), box(30.5, 2, -14.5, 32, 4.75, 30.5), box(-16, 4.75, -14.5, -14.5, 7, 30.5), box(1, 2, 0.5, 15, 4, 15.5),
					box(-13, 2, -13, 29, 2.75, 29), box(-7.25, 2.5, -7.25, 23.75, 3.75, 24), box(3.25, 0, 7.204, 13, 16.75, 8.796), box(4, 0, 7.204, 12, 16.75, 8.796), box(1.5, 0, 7.204, 14.5, 17.5, 8.796), box(4, 0, 7.204, 12, 16.75, 8.796),
					box(3.25, 0, 7.204, 13, 16.75, 8.796), box(7.204, 0, 4, 8.796, 16.75, 12), box(7.204, 0, 1, 8.796, 17.5, 14.5), box(7.204, 0, 4, 8.796, 16.75, 12), box(-6.75, 17.75, -6.75, 22.25, 45.75, 22.5));
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(ANIMATION, FACING);
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

		RankEvaluatorOnBlockRightClickedProcedure.execute(world, x, y, z, entity);
		return InteractionResult.SUCCESS;
	}
}
