package net.solocraft.dungeon;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.Random;

/**
 * Block theme definitions for procedurally generated dungeons.
 * Each theme has a wall, accent, rare-wall variant, floor, pillar, and light block.
 */
public enum DungeonTheme {

    STONE(
            "§8Stone Vault",
            Blocks.STONE_BRICKS,
            Blocks.CRACKED_STONE_BRICKS,
            Blocks.MOSSY_STONE_BRICKS,
            Blocks.SMOOTH_STONE,
            Blocks.CHISELED_STONE_BRICKS,
            Blocks.SEA_LANTERN
    ),
    ICE(
            "§bFrozen Crypt",
            Blocks.PACKED_ICE,
            Blocks.BLUE_ICE,
            Blocks.PACKED_ICE,
            Blocks.SMOOTH_STONE,
            Blocks.ICE,
            Blocks.SOUL_LANTERN
    ),
    NETHER(
            "§cNether Forge",
            Blocks.NETHER_BRICKS,
            Blocks.CRACKED_NETHER_BRICKS,
            Blocks.CHISELED_NETHER_BRICKS,
            Blocks.BLACKSTONE,
            Blocks.BASALT,
            Blocks.SHROOMLIGHT
    ),
    DESERT(
            "§eDesert Tomb",
            Blocks.SANDSTONE,
            Blocks.SMOOTH_SANDSTONE,
            Blocks.CHISELED_SANDSTONE,
            Blocks.SMOOTH_SANDSTONE,
            Blocks.CUT_SANDSTONE,
            Blocks.SEA_LANTERN
    ),
    MOSSY(
            "§aOvergrown Ruins",
            Blocks.MOSSY_STONE_BRICKS,
            Blocks.MOSSY_COBBLESTONE,
            Blocks.STONE_BRICKS,
            Blocks.DIRT,
            Blocks.OAK_LOG,
            Blocks.SHROOMLIGHT
    ),
    VOID(
            "§5Void Sanctum",
            Blocks.BLACKSTONE,
            Blocks.GILDED_BLACKSTONE,
            Blocks.OBSIDIAN,
            Blocks.BLACKSTONE,
            Blocks.OBSIDIAN,
            Blocks.CRYING_OBSIDIAN
    );

    /** Colour-coded display name shown in chat. */
    public final String displayName;
    /** Primary wall block. */
    public final Block wall;
    /** Secondary wall block — used for decoration variety (~25 % of walls). */
    public final Block accent;
    /** Rare wall block — used sparingly (~10 % of walls). */
    public final Block rare;
    /** Floor block (laid flat under every room and corridor). */
    public final Block floor;
    /** Pillar block (corner pillars in larger rooms). */
    public final Block pillar;
    /** Light-emitting ceiling block. */
    public final Block light;

    DungeonTheme(String displayName,
                 Block wall, Block accent, Block rare,
                 Block floor, Block pillar, Block light) {
        this.displayName = displayName;
        this.wall    = wall;
        this.accent  = accent;
        this.rare    = rare;
        this.floor   = floor;
        this.pillar  = pillar;
        this.light   = light;
    }

    /** Parse a theme by name (case-insensitive). Falls back to random if unknown. */
    public static DungeonTheme fromString(String s) {
        for (DungeonTheme t : values())
            if (t.name().equalsIgnoreCase(s)) return t;
        return random();
    }

    /** Returns a uniformly random theme. */
    public static DungeonTheme random() {
        DungeonTheme[] v = values();
        return v[new Random().nextInt(v.length)];
    }
}
