package net.solocraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/** Short-lived collision used by the Beast Monarch's Rubble Jaw. */
public final class BeastRubbleBlock extends Block {
	private static final int LIFETIME = 100;

	public BeastRubbleBlock() {
		super(BlockBehaviour.Properties.of()
				.mapColor(MapColor.DEEPSLATE)
				.sound(SoundType.DEEPSLATE)
				.strength(1.35F, 8.0F)
				.noLootTable());
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState previous, boolean movedByPiston) {
		super.onPlace(state, level, pos, previous, movedByPiston);
		if (!level.isClientSide())
			level.scheduleTick(pos, this, LIFETIME);
	}

	@Override
	public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getBlockState(pos).is(this))
			level.removeBlock(pos, false);
	}

	@Override
	public PushReaction getPistonPushReaction(BlockState state) {
		return PushReaction.BLOCK;
	}
}
