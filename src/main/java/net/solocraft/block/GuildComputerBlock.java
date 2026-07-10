package net.solocraft.block;

import net.solocraft.block.entity.GuildComputerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The Guild Computer block.
 *
 * When broken (any means — survival, creative, explosion) it drops itself
 * as a single item with all block-entity data (bound guild ID + storage
 * contents) saved inside the item's {@code BlockEntityTag}.  Placing the
 * item back restores everything automatically via Minecraft's standard
 * {@code BlockItem.updateCustomBlockEntityTag} path.
 */
public class GuildComputerBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public GuildComputerBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(SoundType.METAL)
                .strength(3.5f, 12f));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
        // Note: requiresCorrectToolForDrops() intentionally omitted so the
        // computer always drops itself regardless of tool used.
    }

    // ── BlockEntity ───────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuildComputerBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof GuildComputerBlockEntity computer)) return InteractionResult.PASS;

        NetworkHooks.openScreen((ServerPlayer) player, computer,
                buf -> computer.writeScreenOpeningData(player, buf));

        return InteractionResult.CONSUME;
    }

    // ── Drop self with full NBT (survival / loot table path) ─────────────────

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        return List.of(buildDrop(be));
    }

    /**
     * Middle-click in creative: give the computer item with its current data.
     */
    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter level,
                                       BlockPos pos, BlockState state) {
        return buildDrop(level.getBlockEntity(pos));
    }

    /**
     * Creative-mode breaking suppresses normal drops, so we fire the drop
     * manually before the block is removed.
     */
    @Override
    public void playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player.isCreative()) {
            BlockEntity be = world.getBlockEntity(pos);
            ItemStack drop = buildDrop(be);
            ItemEntity ie = new ItemEntity(world,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
            ie.setDefaultPickUpDelay();
            world.addFreshEntity(ie);
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    // ── Cleanup on replacement (no loose-item spill — everything is in NBT) ──

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos,
                         BlockState newState, boolean isMoving) {
        // If this block is being replaced by a different block, just remove the
        // tile entity.  Storage contents are already encoded in the dropped item
        // (via getDrops / playerWillDestroy), so nothing is lost.
        if (!state.is(newState.getBlock())) {
            super.onRemove(state, world, pos, newState, isMoving);
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ItemStack buildDrop(@Nullable BlockEntity be) {
        ItemStack stack = new ItemStack(this);
        if (be instanceof GuildComputerBlockEntity computer) {
            CompoundTag beTag = computer.saveWithoutMetadata();
            if (!beTag.isEmpty()) {
                stack.addTagElement("BlockEntityTag", beTag);
            }
        }
        return stack;
    }
}
