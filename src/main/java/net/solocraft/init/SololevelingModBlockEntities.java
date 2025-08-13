
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.solocraft.init;

import net.solocraft.block.entity.InstanceDungeonKeyLoggerTileEntity;
import net.solocraft.block.entity.InstanceCoverTileEntity;
import net.solocraft.block.entity.HunterRankEvaluatorTileEntity;
import net.solocraft.block.entity.DungeonWallTileEntity;
import net.solocraft.block.entity.CustomPortalBlockEntity;
import net.solocraft.SololevelingMod;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;

public class SololevelingModBlockEntities {
	public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SololevelingMod.MODID);
	public static final RegistryObject<BlockEntityType<InstanceDungeonKeyLoggerTileEntity>> INSTANCE_DUNGEON_KEY_LOGGER = REGISTRY.register("instance_dungeon_key_logger",
			() -> BlockEntityType.Builder.of(InstanceDungeonKeyLoggerTileEntity::new, SololevelingModBlocks.INSTANCE_DUNGEON_KEY_LOGGER.get()).build(null));
	public static final RegistryObject<BlockEntityType<InstanceCoverTileEntity>> INSTANCE_COVER = REGISTRY.register("instance_cover",
			() -> BlockEntityType.Builder.of(InstanceCoverTileEntity::new, SololevelingModBlocks.INSTANCE_COVER.get()).build(null));
	public static final RegistryObject<BlockEntityType<HunterRankEvaluatorTileEntity>> HUNTER_RANK_EVALUATOR = REGISTRY.register("hunter_rank_evaluator",
			() -> BlockEntityType.Builder.of(HunterRankEvaluatorTileEntity::new, SololevelingModBlocks.HUNTER_RANK_EVALUATOR.get()).build(null));
	public static final RegistryObject<BlockEntityType<DungeonWallTileEntity>> DUNGEON_WALL = REGISTRY.register("dungeon_wall", () -> BlockEntityType.Builder.of(DungeonWallTileEntity::new, SololevelingModBlocks.DUNGEON_WALL.get()).build(null));
	public static final RegistryObject<BlockEntityType<?>> CUSTOM_PORTAL = register("custom_portal", SololevelingModBlocks.CUSTOM_PORTAL, CustomPortalBlockEntity::new);

	private static RegistryObject<BlockEntityType<?>> register(String registryname, RegistryObject<Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
		return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
	}
}
