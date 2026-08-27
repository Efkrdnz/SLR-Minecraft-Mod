package net.solocraft.init;

import net.solocraft.block.entity.GuildComputerBlockEntity;
import net.solocraft.block.entity.InstanceDungeonKeyLoggerTileEntity;
import net.solocraft.block.entity.InstanceCoverTileEntity;
import net.solocraft.block.entity.HunterRankEvaluatorTileEntity;
import net.solocraft.block.entity.DungeonWallTileEntity;
import net.solocraft.block.entity.CustomPortalBlockEntity;
import net.solocraft.SololevelingMod;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Block;

public class SololevelingModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, SololevelingMod.MODID);
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstanceDungeonKeyLoggerTileEntity>> INSTANCE_DUNGEON_KEY_LOGGER = REGISTRY.register("instance_dungeon_key_logger",
			() -> BlockEntityType.Builder.of(InstanceDungeonKeyLoggerTileEntity::new, SololevelingModBlocks.INSTANCE_DUNGEON_KEY_LOGGER.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstanceCoverTileEntity>> INSTANCE_COVER = REGISTRY.register("instance_cover",
			() -> BlockEntityType.Builder.of(InstanceCoverTileEntity::new, SololevelingModBlocks.INSTANCE_COVER.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HunterRankEvaluatorTileEntity>> HUNTER_RANK_EVALUATOR = REGISTRY.register("hunter_rank_evaluator",
			() -> BlockEntityType.Builder.of(HunterRankEvaluatorTileEntity::new, SololevelingModBlocks.HUNTER_RANK_EVALUATOR.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DungeonWallTileEntity>> DUNGEON_WALL = REGISTRY.register("dungeon_wall", () -> BlockEntityType.Builder.of(DungeonWallTileEntity::new, SololevelingModBlocks.DUNGEON_WALL.get()).build(null));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CustomPortalBlockEntity>> CUSTOM_PORTAL = REGISTRY.register("custom_portal",
			() -> BlockEntityType.Builder.of(CustomPortalBlockEntity::new, SololevelingModBlocks.CUSTOM_PORTAL.get()).build(null));

	// â”€â”€ Guild System â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GuildComputerBlockEntity>> GUILD_COMPUTER = REGISTRY.register("guild_computer",
			() -> BlockEntityType.Builder.of(GuildComputerBlockEntity::new, SololevelingModBlocks.GUILD_COMPUTER.get()).build(null));

	private static DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}

	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		registerItemHandler(event, INSTANCE_DUNGEON_KEY_LOGGER.get());
		registerItemHandler(event, INSTANCE_COVER.get());
		registerItemHandler(event, HUNTER_RANK_EVALUATOR.get());
		registerItemHandler(event, DUNGEON_WALL.get());
		registerItemHandler(event, CUSTOM_PORTAL.get());
	}

	private static <T extends BlockEntity & WorldlyContainer> void registerItemHandler(
			RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
				(blockEntity, side) -> side == null
						? new InvWrapper(blockEntity)
						: new SidedInvWrapper(blockEntity, side));
	}
}
