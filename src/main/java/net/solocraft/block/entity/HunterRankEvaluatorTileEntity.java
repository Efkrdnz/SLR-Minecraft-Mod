package net.solocraft.block.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoBlockEntity;

import net.solocraft.init.SololevelingModBlockEntities;


import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;


import java.util.stream.IntStream;
import java.util.UUID;

public class HunterRankEvaluatorTileEntity extends RandomizableContainerBlockEntity implements GeoBlockEntity, WorldlyContainer {
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(9, ItemStack.EMPTY);
	private UUID publicPulseSession;
	private int publicPulseColor = 0x3FC6FF;
	private float publicPulseIntensity;
	private int publicPulsePhase;
	private long publicPulseUntil;

	public HunterRankEvaluatorTileEntity(BlockPos pos, BlockState state) {
		super(SololevelingModBlockEntities.HUNTER_RANK_EVALUATOR.get(), pos, state);
	}

	private PlayState predicate(AnimationState event) {
		String animationprocedure = ("" + ((this.getBlockState()).getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _getip1 ? (this.getBlockState()).getValue(_getip1) : 0));
		if (animationprocedure.equals("0")) {
			return event.setAndContinue(RawAnimation.begin().thenLoop(animationprocedure));
		}
		return PlayState.STOP;
	}

	private PlayState procedurePredicate(AnimationState event) {
		String animationprocedure = ("" + ((this.getBlockState()).getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _getip1 ? (this.getBlockState()).getValue(_getip1) : 0));
		if (!animationprocedure.equals("0") && event.getController().getAnimationState() == AnimationController.State.STOPPED) {
			event.getController().setAnimation(RawAnimation.begin().thenPlay(animationprocedure));
			if (event.getController().getAnimationState() == AnimationController.State.STOPPED) {
				if (this.getBlockState().getBlock().getStateDefinition().getProperty("animation") instanceof IntegerProperty _integerProp)
					level.setBlock(this.getBlockPos(), this.getBlockState().setValue(_integerProp, 0), 3);
				event.getController().forceAnimationReset();
			}
		} else if (animationprocedure.equals("0")) {
			return PlayState.STOP;
		}
		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		data.add(new AnimationController<HunterRankEvaluatorTileEntity>(this, "controller", 0, this::predicate));
		data.add(new AnimationController<HunterRankEvaluatorTileEntity>(this, "procedurecontroller", 0, this::procedurePredicate));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	protected void loadAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		super.loadAdditional(compound, registries);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, registries);
		this.publicPulseSession = compound.hasUUID("EvaluationPulseSession")
				? compound.getUUID("EvaluationPulseSession") : null;
		this.publicPulseColor = compound.getInt("EvaluationPulseColor");
		this.publicPulseIntensity = compound.getFloat("EvaluationPulseIntensity");
		this.publicPulsePhase = compound.getInt("EvaluationPulsePhase");
		this.publicPulseUntil = compound.getLong("EvaluationPulseUntil");
	}

	@Override
	protected void saveAdditional(CompoundTag compound, HolderLookup.Provider registries) {
		super.saveAdditional(compound, registries);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, registries);
		}
		if (this.publicPulseSession != null)
			compound.putUUID("EvaluationPulseSession", this.publicPulseSession);
		compound.putInt("EvaluationPulseColor", this.publicPulseColor);
		compound.putFloat("EvaluationPulseIntensity",
				this.publicPulseIntensity);
		compound.putInt("EvaluationPulsePhase", this.publicPulsePhase);
		compound.putLong("EvaluationPulseUntil", this.publicPulseUntil);
	}

	public void setPublicPulse(UUID sessionId, int color, float intensity,
			int phase, long until) {
		this.publicPulseSession = sessionId;
		this.publicPulseColor = color & 0xFFFFFF;
		this.publicPulseIntensity = Math.max(0.0F,
				Math.min(1.0F, intensity));
		this.publicPulsePhase = Math.max(0, phase);
		this.publicPulseUntil = Math.max(0L, until);
		setChanged();
		if (this.level != null && !this.level.isClientSide())
			this.level.sendBlockUpdated(this.worldPosition, getBlockState(),
					getBlockState(), 3);
	}

	public boolean isPublicPulseOwner(UUID sessionId, long gameTime) {
		return sessionId != null && sessionId.equals(this.publicPulseSession)
				&& gameTime <= this.publicPulseUntil;
	}

	public int getPublicPulseColor() {
		return this.publicPulseColor;
	}

	public float getPublicPulseIntensity() {
		return this.publicPulseIntensity;
	}

	public int getPublicPulsePhase() {
		return this.publicPulsePhase;
	}

	public long getPublicPulseUntil() {
		return this.publicPulseUntil;
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		return this.saveWithFullMetadata(registries);
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("hunter_rank_evaluator");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return ChestMenu.threeRows(id, inventory);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Hunter Rank Evaluator");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, Direction direction) {
		return this.canPlaceItem(index, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return true;
	}
}
