package net.solocraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;

/** Temporary, server-owned road block used only by the Frost Monarch ability. */
public class FrostCausewayBlock extends Block {
	public static final IntegerProperty AGE = BlockStateProperties.AGE_7;
	/** 0-15 restores the exact water level; 16 restores air. */
	public static final IntegerProperty RETURN_LEVEL = IntegerProperty.create("return_level", 0, 16);
	public static final int RETURN_AIR = 16;
	private static final int TICK_INTERVAL = 60;

	public FrostCausewayBlock() {
		super(BlockBehaviour.Properties.of()
				.sound(SoundType.GLASS)
				.strength(-1.0F, 3600000.0F)
				.friction(0.98F)
				.lightLevel(state -> 3)
				.noOcclusion());
		registerDefaultState(stateDefinition.any()
				.setValue(AGE, 0)
				.setValue(RETURN_LEVEL, RETURN_AIR));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AGE, RETURN_LEVEL);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int age = state.getValue(AGE);
		// A refreshed block may already have an older de-duplicated tick queued. Five
		// stages guarantee a full 12 seconds from any refresh before restoration.
		if (age < 4) {
			level.setBlock(pos, state.setValue(AGE, age + 1), 2);
			level.scheduleTick(pos, this, TICK_INTERVAL);
			return;
		}
		int returnLevel = state.getValue(RETURN_LEVEL);
		BlockState replacement = returnLevel == RETURN_AIR
				? Blocks.AIR.defaultBlockState()
				: Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, returnLevel);
		level.setBlock(pos, replacement, 3);
	}

	public static void refresh(ServerLevel level, BlockPos pos, BlockState state) {
		level.setBlock(pos, state.setValue(AGE, 0), 2);
		level.scheduleTick(pos, state.getBlock(), TICK_INTERVAL);
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.BLOCK;
	}
}
