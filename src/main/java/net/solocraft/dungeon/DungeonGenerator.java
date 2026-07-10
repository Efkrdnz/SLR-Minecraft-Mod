package net.solocraft.dungeon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Procedural dungeon generator.
 *
 * <h3>Algorithm overview</h3>
 * <ol>
 *   <li>A 2-D integer grid is created whose size scales with {@code complexity}.</li>
 *   <li>Rooms are placed by random sampling with overlap rejection.</li>
 *   <li>A minimum spanning tree (Prim's) connects every room with corridors.</li>
 *   <li>The rooms and corridors are expanded outward by one cell to produce
 *       surrounding wall cells.</li>
 *   <li>The grid is translated into world blocks, then decorated.</li>
 *   <li>A 1-wide entry shaft with ladders is dug from the player's position
 *       down to the entry room.</li>
 * </ol>
 *
 * <h3>Room heights</h3>
 * <pre>
 *   baseY + 0          → floor  block
 *   baseY + 1 .. +4    → interior air  (INTERIOR_H = 4)
 *   baseY + 5          → ceiling block
 * </pre>
 */
public class DungeonGenerator {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** Number of interior air layers (ceiling and floor tiles are extra). */
    private static final int INTERIOR_H = 4;

    /** Total column height including floor and ceiling tiles. */
    private static final int TOTAL_H = INTERIOR_H + 2; // 6

    // Grid cell flags
    private static final int EMPTY    = 0;
    private static final int ROOM     = 1;
    private static final int CORRIDOR = 2;
    private static final int WALL     = 3;

    // ── Public entry point ────────────────────────────────────────────────────

    /**
     * Generates a procedural dungeon centred below {@code origin}.
     *
     * @param level      the server level to build in
     * @param origin     player's block position (dungeon entry spawns beneath this)
     * @param complexity 1–10  — controls room count and dungeon footprint
     * @param theme      block palette to use
     * @return a feedback message suitable for sending directly to a player
     */
    public static String generate(ServerLevel level, BlockPos origin,
                                  int complexity, DungeonTheme theme) {
        Random rng = new Random();
        int c          = Math.max(1, Math.min(10, complexity));
        int targetRooms = c * 2 + 3;          // 5 … 23
        int gridSize    = c * 22 + 40;         // 62 … 262

        // Place dungeon floor 10 blocks below the player's feet, never below Y=5
        int baseY = Math.max(5, origin.getY() - TOTAL_H - 10);

        int[][] grid = new int[gridSize][gridSize];

        // ── 1. Room placement ──────────────────────────────────────────────────
        List<DungeonRoom> rooms = placeRooms(rng, grid, gridSize, targetRooms, c);
        if (rooms.isEmpty())
            return "§cDungeon generation failed — could not place any rooms.";

        // ── 2. Minimum spanning tree ───────────────────────────────────────────
        List<int[]> edges = buildMST(rooms);

        // ── 3. Corridor carving ────────────────────────────────────────────────
        for (int[] edge : edges)
            markCorridor(grid, gridSize, rooms.get(edge[0]), rooms.get(edge[1]));

        // ── 4. Wall expansion (1-cell border around all filled cells) ──────────
        expandWalls(grid, gridSize);

        // ── 5. World offset — entry room centre aligns with origin X/Z ─────────
        int offsetX = origin.getX() - rooms.get(0).centerX();
        int offsetZ = origin.getZ() - rooms.get(0).centerZ();

        // ── 6. Build blocks ────────────────────────────────────────────────────
        buildWorld(level, grid, gridSize, baseY, offsetX, offsetZ, theme, rng);

        // ── 7. Decoration ──────────────────────────────────────────────────────
        decorateRooms(level, rooms, baseY, offsetX, offsetZ, theme, rng);

        // ── 8. Entry shaft ─────────────────────────────────────────────────────
        buildEntryShaft(level, origin, baseY, rooms.get(0), offsetX, offsetZ, theme);

        int ex = offsetX + rooms.get(0).centerX();
        int ez = offsetZ + rooms.get(0).centerZ();
        return String.format("§a%s §7generated with §e%d rooms§7.  Entry: §f%d %d %d",
                theme.displayName, rooms.size(), ex, baseY + 1, ez);
    }

    // ── Step 1: Room placement ─────────────────────────────────────────────────

    private static List<DungeonRoom> placeRooms(Random rng, int[][] grid,
                                                int gridSize, int target, int complexity) {
        List<DungeonRoom> rooms = new ArrayList<>();

        // Interior dimensions: 7–(7 + complexity/2) blocks wide and long
        int minSize  = 7;
        int maxExtra = Math.max(1, complexity / 2 + 3); // up to 8 extra at c=10

        int attempts = target * 35;

        for (int i = 0; i < attempts && rooms.size() < target; i++) {
            int w = minSize + rng.nextInt(maxExtra);
            int l = minSize + rng.nextInt(maxExtra);

            // Keep room (plus its 1-cell wall border) inside the grid
            int maxGx = gridSize - w - 2;
            int maxGz = gridSize - l - 2;
            if (maxGx < 2 || maxGz < 2) continue;

            int gx, gz;
            DungeonRoom.Type type;

            if (rooms.isEmpty()) {
                // Entry room — centred in the grid
                gx   = gridSize / 2 - w / 2;
                gz   = gridSize / 2 - l / 2;
                type = DungeonRoom.Type.ENTRY;
            } else {
                gx   = 2 + rng.nextInt(maxGx - 2);
                gz   = 2 + rng.nextInt(maxGz - 2);
                type = DungeonRoom.Type.NORMAL;
            }

            DungeonRoom candidate = new DungeonRoom(gx, gz, w, l, type);

            boolean fits = true;
            for (DungeonRoom existing : rooms) {
                if (candidate.overlaps(existing, 3)) { fits = false; break; }
            }
            if (!fits) continue;

            rooms.add(candidate);

            // Paint into grid
            for (int dx = 0; dx < w; dx++)
                for (int dz = 0; dz < l; dz++) {
                    int cx = gx + dx, cz = gz + dz;
                    if (inBounds(cx, cz, gridSize)) grid[cx][cz] = ROOM;
                }
        }

        if (rooms.size() < 2) return rooms;

        // Assign BOSS to the room farthest from entry (by squared grid distance)
        DungeonRoom entry = rooms.get(0);
        int bossIdx = 1;
        int bestDist = -1;
        for (int i = 1; i < rooms.size(); i++) {
            int d = distSq(rooms.get(i), entry);
            if (d > bestDist) { bestDist = d; bossIdx = i; }
        }
        rooms.set(bossIdx, rooms.get(bossIdx).withType(DungeonRoom.Type.BOSS));

        // Assign TREASURE to ~15 % of remaining NORMAL rooms
        for (int i = 1; i < rooms.size(); i++) {
            if (rooms.get(i).type == DungeonRoom.Type.NORMAL && rng.nextFloat() < 0.15f)
                rooms.set(i, rooms.get(i).withType(DungeonRoom.Type.TREASURE));
        }

        return rooms;
    }

    // ── Step 2: Minimum spanning tree (Prim's) ─────────────────────────────────

    /**
     * Returns a list of edges {@code [a, b]} where a and b are indices into
     * {@code rooms}. Together they form a minimum spanning tree so every room
     * is reachable from the entry room.
     */
    private static List<int[]> buildMST(List<DungeonRoom> rooms) {
        int n = rooms.size();
        boolean[] inMST = new boolean[n];
        int[]     minD  = new int[n];
        int[]     parent = new int[n];
        Arrays.fill(minD,  Integer.MAX_VALUE);
        Arrays.fill(parent, -1);
        minD[0] = 0;

        List<int[]> edges = new ArrayList<>();

        for (int iter = 0; iter < n; iter++) {
            // Pick vertex with smallest key not yet in MST
            int u = -1;
            for (int i = 0; i < n; i++)
                if (!inMST[i] && (u == -1 || minD[i] < minD[u])) u = i;

            inMST[u] = true;
            if (parent[u] != -1) edges.add(new int[]{parent[u], u});

            // Relax neighbours
            for (int v = 0; v < n; v++) {
                if (!inMST[v]) {
                    int d = distSq(rooms.get(u), rooms.get(v));
                    if (d < minD[v]) { minD[v] = d; parent[v] = u; }
                }
            }
        }
        return edges;
    }

    // ── Step 3: Corridor carving ───────────────────────────────────────────────

    /**
     * Draws a 3-wide L-shaped corridor between the centres of two rooms,
     * marking cells as CORRIDOR only if they were previously EMPTY.
     */
    private static void markCorridor(int[][] grid, int gs,
                                     DungeonRoom a, DungeonRoom b) {
        int ax = a.centerX(), az = a.centerZ();
        int bx = b.centerX(), bz = b.centerZ();

        // Horizontal leg (z = az)
        for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++)
            for (int dz = -1; dz <= 1; dz++)
                setIfEmpty(grid, gs, x, az + dz);

        // Vertical leg (x = bx)
        for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++)
            for (int dx = -1; dx <= 1; dx++)
                setIfEmpty(grid, gs, bx + dx, z);
    }

    private static void setIfEmpty(int[][] grid, int gs, int x, int z) {
        if (inBounds(x, z, gs) && grid[x][z] == EMPTY) grid[x][z] = CORRIDOR;
    }

    // ── Step 4: Wall expansion ─────────────────────────────────────────────────

    /**
     * Any EMPTY cell adjacent (including diagonals) to a ROOM or CORRIDOR cell
     * becomes a WALL cell.
     */
    private static void expandWalls(int[][] grid, int gs) {
        // Work from a copy so the expansion doesn't cascade
        int[][] copy = new int[gs][gs];
        for (int x = 0; x < gs; x++) System.arraycopy(grid[x], 0, copy[x], 0, gs);

        for (int x = 0; x < gs; x++) {
            for (int z = 0; z < gs; z++) {
                if (copy[x][z] != EMPTY) continue;
                outer:
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int nx = x + dx, nz = z + dz;
                        if (inBounds(nx, nz, gs) && copy[nx][nz] != EMPTY) {
                            grid[x][z] = WALL;
                            break outer;
                        }
                    }
                }
            }
        }
    }

    // ── Step 5: World building ─────────────────────────────────────────────────

    private static void buildWorld(ServerLevel level, int[][] grid, int gs,
                                   int baseY, int offsetX, int offsetZ,
                                   DungeonTheme theme, Random rng) {
        BlockState air   = Blocks.AIR.defaultBlockState();
        BlockState floor = theme.floor.defaultBlockState();

        for (int gx = 0; gx < gs; gx++) {
            for (int gz = 0; gz < gs; gz++) {
                int cell = grid[gx][gz];
                if (cell == EMPTY) continue;

                int wx = offsetX + gx;
                int wz = offsetZ + gz;

                if (cell == WALL) {
                    // Solid column — randomised wall variant for texture variety
                    for (int dy = 0; dy < TOTAL_H; dy++)
                        level.setBlock(new BlockPos(wx, baseY + dy, wz),
                                pickWall(theme, rng), 2);

                } else { // ROOM or CORRIDOR
                    level.setBlock(new BlockPos(wx, baseY,             wz), floor, 2);
                    for (int dy = 1; dy <= INTERIOR_H; dy++)
                        level.setBlock(new BlockPos(wx, baseY + dy,    wz), air,   2);
                    level.setBlock(new BlockPos(wx, baseY + TOTAL_H - 1, wz),
                            theme.wall.defaultBlockState(), 2);
                }
            }
        }
    }

    /**
     * Returns a wall-block state with weighted randomness:
     * ~65 % primary, ~25 % accent, ~10 % rare.
     */
    private static BlockState pickWall(DungeonTheme theme, Random rng) {
        float r = rng.nextFloat();
        if (r < 0.65f) return theme.wall.defaultBlockState();
        if (r < 0.90f) return theme.accent.defaultBlockState();
        return theme.rare.defaultBlockState();
    }

    // ── Step 6: Decoration ─────────────────────────────────────────────────────

    private static void decorateRooms(ServerLevel level, List<DungeonRoom> rooms,
                                      int baseY, int offsetX, int offsetZ,
                                      DungeonTheme theme, Random rng) {
        for (DungeonRoom room : rooms) {
            int wx0   = offsetX + room.gx;
            int wz0   = offsetZ + room.gz;
            int floorY = baseY + 1;
            int ceilY  = baseY + TOTAL_H - 1;
            int lightY = ceilY - 1;
            BlockState light = lightState(theme);

            // ── Corner pillars (shared by all room types) ────────────────────
            if (room.width >= 7 && room.length >= 7) {
                int[][] corners = {
                    { wx0 + 1,              wz0 + 1               },
                    { wx0 + room.width - 2, wz0 + 1               },
                    { wx0 + 1,              wz0 + room.length - 2  },
                    { wx0 + room.width - 2, wz0 + room.length - 2  }
                };
                BlockState pillarState = theme.pillar.defaultBlockState();
                for (int[] c : corners)
                    for (int dy = 0; dy < INTERIOR_H; dy++)
                        level.setBlock(new BlockPos(c[0], floorY + dy, c[1]), pillarState, 2);
            }

            // ── Per-type decoration ──────────────────────────────────────────
            switch (room.type) {
                case ENTRY    -> decorateEntryRoom(level, room, baseY, wx0, wz0, theme);
                case BOSS     -> decorateBossRoom (level, room, baseY, wx0, wz0, theme);
                default -> {
                    // Normal / Treasure rooms

                    // Ceiling lights every 4 blocks
                    for (int dx = 2; dx < room.width - 2; dx += 4)
                        for (int dz = 2; dz < room.length - 2; dz += 4)
                            level.setBlock(new BlockPos(wx0 + dx, lightY, wz0 + dz),
                                    light, 2);
                    // Fallback centre light
                    level.setBlock(
                            new BlockPos(wx0 + room.width / 2, lightY, wz0 + room.length / 2),
                            light, 2);

                }
            }
        }
    }

    // ── Entry room ────────────────────────────────────────────────────────────

    /**
     * Entry room: cross-shaped accent floor, corner accent tiles, a rare-block
     * landing pad at the shaft drop-point, and dense ceiling lighting.
     */
    private static void decorateEntryRoom(ServerLevel level, DungeonRoom room,
                                          int baseY, int wx0, int wz0,
                                          DungeonTheme theme) {
        int ceilY  = baseY + TOTAL_H - 1;
        int lightY = ceilY - 1;
        int w = room.width, l = room.length;
        int cx = w / 2, cz = l / 2;

        BlockState accent = theme.accent.defaultBlockState();
        BlockState rare   = theme.rare.defaultBlockState();
        BlockState light  = lightState(theme);

        // Cross arms across the full floor
        for (int dx = 0; dx < w; dx++)
            level.setBlock(new BlockPos(wx0 + dx, baseY, wz0 + cz), accent, 2);
        for (int dz = 0; dz < l; dz++)
            level.setBlock(new BlockPos(wx0 + cx, baseY, wz0 + dz), accent, 2);

        // Corner accent diamonds (2 steps in from each corner)
        int[][] corners = {
            {wx0 + 1, wz0 + 1}, {wx0 + w - 2, wz0 + 1},
            {wx0 + 1, wz0 + l - 2}, {wx0 + w - 2, wz0 + l - 2}
        };
        for (int[] c : corners)
            level.setBlock(new BlockPos(c[0], baseY, c[1]), accent, 2);

        // Rare block at centre — marks the shaft landing spot
        level.setBlock(new BlockPos(wx0 + cx, baseY, wz0 + cz), rare, 2);

        // Dense ceiling lights — every 3 blocks
        for (int dx = 1; dx < w - 1; dx += 3)
            for (int dz = 1; dz < l - 1; dz += 3)
                level.setBlock(new BlockPos(wx0 + dx, lightY, wz0 + dz), light, 2);
        // Centre ceiling spotlight directly above the shaft
        level.setBlock(new BlockPos(wx0 + cx, lightY, wz0 + cz), light, 2);
    }

    // ── Boss room ─────────────────────────────────────────────────────────────

    /**
     * Boss room: checkerboard floor, a raised central altar platform, perimeter
     * ceiling ring lights, and a spotlight over the altar.
     *
     * <pre>
     *   baseY       → checkerboard floor (accent / floor blocks)
     *   baseY + 1   → altar platform layer (pillar blocks, 3×3 or 5×5)
     *   baseY + 1   → centre of altar replaced by rare block
     *   ceilY       → perimeter ring of lights + centre spotlight
     * </pre>
     */
    private static void decorateBossRoom(ServerLevel level, DungeonRoom room,
                                         int baseY, int wx0, int wz0,
                                         DungeonTheme theme) {
        int floorY = baseY + 1;
        int ceilY  = baseY + TOTAL_H - 1;
        int lightY = ceilY - 1;
        int w = room.width, l = room.length;
        int cx = w / 2, cz = l / 2;

        BlockState accent = theme.accent.defaultBlockState();
        BlockState pillar = theme.pillar.defaultBlockState();
        BlockState rare   = theme.rare.defaultBlockState();
        BlockState light  = lightState(theme);

        // Checkerboard floor pattern
        for (int dx = 0; dx < w; dx++)
            for (int dz = 0; dz < l; dz++)
                if ((dx + dz) % 2 == 0)
                    level.setBlock(new BlockPos(wx0 + dx, baseY, wz0 + dz), accent, 2);

        // Raised altar platform — 3×3 for smaller rooms, 5×5 for larger
        int half = (w >= 11 && l >= 11) ? 2 : 1;
        for (int dx = -half; dx <= half; dx++)
            for (int dz = -half; dz <= half; dz++)
                level.setBlock(new BlockPos(wx0 + cx + dx, floorY, wz0 + cz + dz), pillar, 2);

        // Rare block at the very centre of the altar
        level.setBlock(new BlockPos(wx0 + cx, floorY, wz0 + cz), rare, 2);

        // Perimeter ring of ceiling lights (every 2 blocks along each wall)
        for (int dx = 1; dx < w - 1; dx += 2) {
            level.setBlock(new BlockPos(wx0 + dx, lightY, wz0 + 1),     light, 2);
            level.setBlock(new BlockPos(wx0 + dx, lightY, wz0 + l - 2), light, 2);
        }
        for (int dz = 1; dz < l - 1; dz += 2) {
            level.setBlock(new BlockPos(wx0 + 1,     lightY, wz0 + dz), light, 2);
            level.setBlock(new BlockPos(wx0 + w - 2, lightY, wz0 + dz), light, 2);
        }
        // Centre spotlight directly above the altar
        level.setBlock(new BlockPos(wx0 + cx, lightY, wz0 + cz), light, 2);
    }

    // ── Step 7: Entry shaft ────────────────────────────────────────────────────

    /**
     * Digs a 1-wide shaft from just above the dungeon ceiling up to the
     * player's position, lining it with ladders so the player can descend.
     */
    private static void buildEntryShaft(ServerLevel level, BlockPos origin,
                                        int baseY, DungeonRoom entry,
                                        int offsetX, int offsetZ, DungeonTheme theme) {
        int sx = offsetX + entry.centerX();
        int sz = offsetZ + entry.centerZ();

        int dungeonCeilingY = baseY + TOTAL_H - 1;   // top solid layer of dungeon
        int shaftTopY       = origin.getY();           // player's feet

        // Carve opening in dungeon ceiling
        level.setBlock(new BlockPos(sx, dungeonCeilingY, sz), Blocks.AIR.defaultBlockState(), 2);

        // Dig shaft + place ladders from just above ceiling up to player
        // Ladder FACING=WEST means it is attached to the block at sx+1 (east)
        //   and the player faces west when climbing.
        BlockState ladderState = Blocks.LADDER.defaultBlockState()
                .setValue(LadderBlock.FACING, Direction.WEST);
        BlockState wallState   = theme.wall.defaultBlockState();

        for (int y = dungeonCeilingY; y <= shaftTopY; y++) {
            // Air in the climbing column
            level.setBlock(new BlockPos(sx, y, sz), Blocks.AIR.defaultBlockState(), 2);
            // Solid attachment column (east side) — ensures the ladder has a wall
            level.setBlock(new BlockPos(sx + 1, y, sz), wallState, 2);
            // Ladder (placed last so the wall is already there)
            level.setBlock(new BlockPos(sx, y, sz), ladderState, 3);
        }

        // Light at the bottom of the shaft
        level.setBlock(new BlockPos(sx, baseY + 1, sz), lightState(theme), 2);
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private static boolean inBounds(int x, int z, int gs) {
        return x >= 0 && x < gs && z >= 0 && z < gs;
    }

    private static int distSq(DungeonRoom a, DungeonRoom b) {
        int dx = a.centerX() - b.centerX();
        int dz = a.centerZ() - b.centerZ();
        return dx * dx + dz * dz;
    }

    private static BlockState lightState(DungeonTheme theme) {
        BlockState state = theme.light.defaultBlockState();
        if (state.hasProperty(LanternBlock.HANGING))
            return state.setValue(LanternBlock.HANGING, true);
        return state;
    }
}
