package net.solocraft.block;

import net.solocraft.init.SololevelingModParticleTypes;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** A mineable dungeon outcrop containing naturally condensed mana crystals. */
public final class ManaCrystalDepositBlock extends Block {
	private static final VoxelShape SHAPE = Shapes.or(
			Block.box(0.0D, 0.0D, 0.0D, 16.0D, 9.0D, 16.0D),
			Block.box(5.0D, 8.0D, 5.0D, 11.0D, 16.0D, 11.0D),
			Block.box(2.0D, 8.0D, 3.0D, 6.0D, 14.0D, 8.0D),
			Block.box(10.0D, 8.0D, 8.0D, 14.0D, 14.0D, 13.0D));

	public ManaCrystalDepositBlock() {
		super(BlockBehaviour.Properties.of()
				.strength(4.2F, 9.0F)
				.sound(SoundType.AMETHYST_CLUSTER)
				.lightLevel(state -> 5)
				.requiresCorrectToolForDrops()
				.noOcclusion()
				.isRedstoneConductor((state, level, pos) -> false));
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
			CollisionContext context) {
		return SHAPE;
	}

	@Override
	public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return Shapes.empty();
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
		return true;
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(18) != 0)
			return;
		double x = pos.getX() + 0.25D + random.nextDouble() * 0.5D;
		double y = pos.getY() + 0.62D + random.nextDouble() * 0.42D;
		double z = pos.getZ() + 0.25D + random.nextDouble() * 0.5D;
		level.addParticle(SololevelingModParticleTypes.MANA_BLUE.get(), x, y, z,
				0.0D, 0.012D, 0.0D);
	}

	@Override
	public void appendHoverText(ItemStack stack, BlockGetter level, List<Component> tooltip,
			TooltipFlag flag) {
		tooltip.add(Component.translatable("block.sololeveling.mana_crystal_deposit.tooltip")
				.withStyle(ChatFormatting.AQUA));
		tooltip.add(Component.translatable("block.sololeveling.mana_crystal_deposit.hint")
				.withStyle(ChatFormatting.DARK_GRAY));
	}
}
