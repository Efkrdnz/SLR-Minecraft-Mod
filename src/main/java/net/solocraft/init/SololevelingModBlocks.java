package net.solocraft.init;

import net.solocraft.block.GuildComputerBlock;
import net.solocraft.block.ManaCrystalDepositBlock;
import net.solocraft.block.BeastRubbleBlock;
import net.solocraft.block.FrostCausewayBlock;
import net.solocraft.block.WoodenPassageOpenBlock;
import net.solocraft.block.UnbreakableDeepslateBlock;
import net.solocraft.block.SkeletonBlock;
import net.solocraft.block.ShadowBlock;
import net.solocraft.block.PassageWallBlock;
import net.solocraft.block.InstanceDungeonKeyLoggerBlock;
import net.solocraft.block.InstanceCoverBlock;
import net.solocraft.block.HunterRankEvaluatorBlock;
import net.solocraft.block.GolemDropBlockGemBlock;
import net.solocraft.block.GoblinSpawnerBlock;
import net.solocraft.block.EvaluatorTestBlock;
import net.solocraft.block.DungeonWallSmallBlock;
import net.solocraft.block.DungeonWallBlock;
import net.solocraft.block.DungeonToolsBlock;
import net.solocraft.block.DungeonGrave2Block;
import net.solocraft.block.DungeonGrave1Block;
import net.solocraft.block.DungeonFloorBlock;
import net.solocraft.block.DungeonBricksBlock;
import net.solocraft.block.DungeonBlockBlock;
import net.solocraft.block.DungeonBlock2Block;
import net.solocraft.block.DungeonBarrelBlock;
import net.solocraft.block.DisappearingBlockBlock;
import net.solocraft.block.DeepslateKeyblockRedBlock;
import net.solocraft.block.DeepslateKeyblockDKCBlock;
import net.solocraft.block.DeepslateKeyblockBlueBlock;
import net.solocraft.block.DeepslateKeyblockBlock;
import net.solocraft.block.CustomPortalBlock;
import net.solocraft.block.CrystalGolemSpawnerBlock;
import net.solocraft.block.CellDoorOpenBlock;
import net.solocraft.block.CellDoorClosedBlock;
import net.solocraft.SololevelingMod;

import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.world.level.block.Block;

public class SololevelingModBlocks {
	public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(BuiltInRegistries.BLOCK, SololevelingMod.MODID);
	public static final DeferredHolder<Block, Block> DUNGEON_BLOCK = REGISTRY.register("dungeon_block", () -> new DungeonBlockBlock());
	public static final DeferredHolder<Block, Block> DUNGEON_BLOCK_2 = REGISTRY.register("dungeon_block_2", () -> new DungeonBlock2Block());
	public static final DeferredHolder<Block, Block> GOBLIN_SPAWNER = REGISTRY.register("goblin_spawner", () -> new GoblinSpawnerBlock());
	public static final DeferredHolder<Block, Block> DISAPPEARING_BLOCK = REGISTRY.register("disappearing_block", () -> new DisappearingBlockBlock());
	public static final DeferredHolder<Block, Block> UNBREAKABLE_DEEPSLATE = REGISTRY.register("unbreakable_deepslate", () -> new UnbreakableDeepslateBlock());
	public static final DeferredHolder<Block, Block> DEEPSLATE_KEYBLOCK = REGISTRY.register("deepslate_keyblock", () -> new DeepslateKeyblockBlock());
	public static final DeferredHolder<Block, Block> DEEPSLATE_KEYBLOCK_BLUE = REGISTRY.register("deepslate_keyblock_blue", () -> new DeepslateKeyblockBlueBlock());
	public static final DeferredHolder<Block, Block> DEEPSLATE_KEYBLOCK_RED = REGISTRY.register("deepslate_keyblock_red", () -> new DeepslateKeyblockRedBlock());
	public static final DeferredHolder<Block, Block> CRYSTAL_GOLEM_SPAWNER = REGISTRY.register("crystal_golem_spawner", () -> new CrystalGolemSpawnerBlock());
	public static final DeferredHolder<Block, Block> GOLEM_DROP_BLOCK_GEM = REGISTRY.register("golem_drop_block_gem", () -> new GolemDropBlockGemBlock());
	public static final DeferredHolder<Block, Block> MANA_CRYSTAL_DEPOSIT = REGISTRY.register("mana_crystal_deposit", ManaCrystalDepositBlock::new);
	public static final DeferredHolder<Block, Block> INSTANCE_DUNGEON_KEY_LOGGER = REGISTRY.register("instance_dungeon_key_logger", () -> new InstanceDungeonKeyLoggerBlock());
	public static final DeferredHolder<Block, Block> INSTANCE_COVER = REGISTRY.register("instance_cover", () -> new InstanceCoverBlock());
	public static final DeferredHolder<Block, Block> HUNTER_RANK_EVALUATOR = REGISTRY.register("hunter_rank_evaluator", () -> new HunterRankEvaluatorBlock());
	public static final DeferredHolder<Block, Block> WOODEN_PASSAGE_OPEN = REGISTRY.register("wooden_passage_open", () -> new WoodenPassageOpenBlock());
	public static final DeferredHolder<Block, Block> CELL_DOOR_CLOSED = REGISTRY.register("cell_door_closed", () -> new CellDoorClosedBlock());
	public static final DeferredHolder<Block, Block> DUNGEON_FLOOR = REGISTRY.register("dungeon_floor", () -> new DungeonFloorBlock());
	public static final DeferredHolder<Block, Block> DUNGEON_BRICKS = REGISTRY.register("dungeon_bricks", () -> new DungeonBricksBlock());
	public static final DeferredHolder<Block, Block> CELL_DOOR_OPEN = REGISTRY.register("cell_door_open", () -> new CellDoorOpenBlock());
	public static final DeferredHolder<Block, Block> DUNGEON_WALL_SMALL = REGISTRY.register("dungeon_wall_small", () -> new DungeonWallSmallBlock());
	public static final DeferredHolder<Block, Block> DUNGEON_BARREL = REGISTRY.register("dungeon_barrel", () -> new DungeonBarrelBlock());
	public static final DeferredHolder<Block, Block> DUNGEON_TOOLS = REGISTRY.register("dungeon_tools", () -> new DungeonToolsBlock());
	public static final DeferredHolder<Block, Block> SKELETON = REGISTRY.register("skeleton", () -> new SkeletonBlock());
	public static final DeferredHolder<Block, Block> DUNGEON_GRAVE_1 = REGISTRY.register("dungeon_grave_1", () -> new DungeonGrave1Block());
	public static final DeferredHolder<Block, Block> DUNGEON_GRAVE_2 = REGISTRY.register("dungeon_grave_2", () -> new DungeonGrave2Block());
	public static final DeferredHolder<Block, Block> PASSAGE_WALL = REGISTRY.register("passage_wall", () -> new PassageWallBlock());
	public static final DeferredHolder<Block, Block> DUNGEON_WALL = REGISTRY.register("dungeon_wall", () -> new DungeonWallBlock());
	public static final DeferredHolder<Block, Block> EVALUATOR_TEST = REGISTRY.register("evaluator_test", () -> new EvaluatorTestBlock());
	public static final DeferredHolder<Block, Block> CUSTOM_PORTAL = REGISTRY.register("custom_portal", () -> new CustomPortalBlock());
	public static final DeferredHolder<Block, Block> SHADOW = REGISTRY.register("shadow", () -> new ShadowBlock());
	public static final DeferredHolder<Block, Block> DEEPSLATE_KEYBLOCK_DKC = REGISTRY.register("deepslate_keyblock_dkc", () -> new DeepslateKeyblockDKCBlock());
	public static final DeferredHolder<Block, Block> FROST_CAUSEWAY = REGISTRY.register("frost_causeway", FrostCausewayBlock::new);
	public static final DeferredHolder<Block, Block> BEAST_RUBBLE = REGISTRY.register("beast_rubble", BeastRubbleBlock::new);

	// â”€â”€ Guild System â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
	public static final DeferredHolder<Block, Block> GUILD_COMPUTER = REGISTRY.register("guild_computer", () -> new GuildComputerBlock());
}
