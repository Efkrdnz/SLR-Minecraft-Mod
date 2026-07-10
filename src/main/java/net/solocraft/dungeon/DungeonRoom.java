package net.solocraft.dungeon;

/**
 * Represents a single room inside a procedurally generated dungeon.
 *
 * <p>All coordinates are in <em>grid space</em> (not world block coordinates).
 * The top-left cell of the room's interior is {@code (gx, gz)}.
 * Wall cells added by the expansion pass are NOT included in these bounds.</p>
 */
public class DungeonRoom {

    public enum Type {
        /** Starting room — connected directly to the entry shaft. */
        ENTRY,
        /** Generic combat / exploration room. */
        NORMAL,
        /** Small room with guaranteed loot chest. */
        TREASURE,
        /** The farthest room from the entry; intended for a boss encounter. */
        BOSS
    }

    /** Top-left corner of the interior in grid coordinates. */
    public final int gx, gz;
    /** Interior dimensions (excluding surrounding wall cells). */
    public final int width, length;
    /** Room classification — affects decoration and special logic. */
    public final Type type;

    public DungeonRoom(int gx, int gz, int width, int length, Type type) {
        this.gx     = gx;
        this.gz     = gz;
        this.width  = width;
        this.length = length;
        this.type   = type;
    }

    /** Returns a copy of this room with a different type. */
    public DungeonRoom withType(Type newType) {
        return new DungeonRoom(gx, gz, width, length, newType);
    }

    /** X centre of this room in grid coordinates. */
    public int centerX() { return gx + width  / 2; }

    /** Z centre of this room in grid coordinates. */
    public int centerZ() { return gz + length / 2; }

    /**
     * Returns {@code true} if this room (with its 1-cell wall border)
     * overlaps {@code other} (with its wall border) given an extra {@code margin}.
     */
    public boolean overlaps(DungeonRoom other, int margin) {
        int ax1 = this.gx - 1 - margin,  ax2 = this.gx + this.width  + margin;
        int az1 = this.gz - 1 - margin,  az2 = this.gz + this.length + margin;
        int bx1 = other.gx - 1,           bx2 = other.gx + other.width;
        int bz1 = other.gz - 1,           bz2 = other.gz + other.length;
        return ax1 < bx2 && ax2 > bx1 && az1 < bz2 && az2 > bz1;
    }
}
